package de.unirostock.sems.cbarchive.web.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromGit;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbext.Formatizer;


public class GitImporter extends Importer {
	
	private File tempDir = null;
	private Git repo = null;
	
	public GitImporter( ArchiveFromGit archive, UserManager user ) {
		this( archive.getRemoteUrl(), user );
	}
	
	public GitImporter( String remoteUrl, UserManager user ) {
		super(user);
		this.remoteUrl = remoteUrl;
	}
	
	public GitImporter( String remoteUrl ) {
		this(remoteUrl, null);
	}
	
	public GitImporter importRepo() throws ImporterException {
		
		if( remoteUrl == null || remoteUrl.isEmpty() )
			throw new IllegalArgumentException("remoteUrl is empty");
		
		// remove leading "git clone"
		if( remoteUrl.toLowerCase().startsWith("git clone") )
			remoteUrl = remoteUrl.substring(10);
		
		// is it a link to New Zealand!? (models.cellml or physiome)
		if (remoteUrl.contains ("cellml.org/") || remoteUrl.contains ("physiomeproject.org/"))
			remoteUrl = processNzRepoLink( remoteUrl );
		
		cloneGit();
		buildArchive();
		
		return this;
	}
	
	public void close() {
		
		repo.close();
		
		try {
			if( tempDir.exists() )
				FileUtils.deleteDirectory(tempDir);
		} catch (IOException e) {
			LOGGER.error(e, "Exception cleaning up temp dir, after importing a Git repository");
		}
		
	}
	
	private void cloneGit() throws ImporterException {
		
		// get a temp dir
		tempDir = createTempDir();
		
		try {
			// clone the repo
			repo = Git.cloneRepository()
					.setURI( remoteUrl )
					.setDirectory( tempDir )
					.setCloneSubmodules(true)		// include all submodules -> important for PMR2-Project
					.call();
			
		} catch (GitAPIException e) {
			LOGGER.error(e, "Cannot clone git repository: ", remoteUrl);
			throw new ImporterException("Cannot clone git repository: " + remoteUrl, e);
		}
		
	}
	
	private void buildArchive() throws ImporterException {
		
		if( repo == null )
			return;
		
		try {
			tempFile = File.createTempFile(Fields.TEMP_FILE_PREFIX, ".omex");
			tempFile.delete(); // delete tmp file, so CombineArchive lib will create a new archive
			CombineArchive archive = new CombineArchive(tempFile);
			
			scanRepository(tempDir, archive);
			archive.pack();
			archive.close();
			
		} catch (IOException e) {
			LOGGER.error(e, "IOException while build CombineArchive from Git Repository");
			throw new ImporterException(e);
		} catch (JDOMException | ParseException | CombineArchiveException e) {
			LOGGER.error(e, "Exception while creating empty CombineArchive");
			throw new ImporterException("Exception while creating epmty CombineArchive", e);
		} catch (TransformerException e) {
			LOGGER.error(e, "Exception while packing CombineArchive");
			throw new ImporterException("Exception while packing CombineArchive", e);
		}
	}
	
	private void scanRepository( File directory, CombineArchive archive ) throws ImporterException {
		
		String[] dirContent = directory.list();
		for(int index = 0; index < dirContent.length; index++ ) {
			File entry = new File( directory, dirContent[index] );
			
			if( entry.isDirectory() && entry.exists() && !entry.getName().startsWith(".") ) {
				// Entry is a directory and not hidden (begins with a dot) -> recursive
				scanRepository(entry, archive);
			}
			else if( entry.isFile() && entry.exists() ) {
				// Entry is a file
				// make Path relative
				Path relativePath = tempDir.toPath().relativize( entry.toPath() );
				
				// add file and scan log for omex description
				try {
					ArchiveEntry archiveEntry = archive.addEntry(tempDir, entry, Formatizer.guessFormat(entry) );
					archiveEntry.addDescription( new OmexMetaDataObject(
									getOmexForFile(relativePath)
								));
				} catch (IOException e) {
					LOGGER.error(e, "Error while adding ", relativePath, " to the CombineArchive.");
					throw new ImporterException("Error while adding \"" + relativePath.toString() + "\" to CombineArchive", e);
				}
			}
		}
		
	}
	
	private OmexDescription getOmexForFile( Path relativePath ) throws ImporterException {
		LinkedHashSet<ImportVCard> contributors = new LinkedHashSet<ImportVCard>();
		List<Date> modified = new LinkedList<Date>();
		Date created = null;
		
		try {
			Iterable<RevCommit> commits = repo.log()
											.addPath( relativePath.toString() )
											.call();
			
			for( RevCommit current : commits ) {
				// add person
				ImportVCard vcard = new ImportVCard( current.getAuthorIdent() );
				contributors.add(vcard);

				// add time stamp
				Date timeStamp = current.getAuthorIdent().getWhen(); 
				modified.add( timeStamp );
			
				// set created time stamp
				if( created == null || timeStamp.before(created) )
					created = timeStamp;
			}
			
		} catch (GitAPIException e) {
			LOGGER.error(e, "Error while getting log for file");
			throw new ImporterException("Error while getting file log", e);
		}
		
		return new OmexDescription(
					new ArrayList<VCard>( contributors ),
					modified, created
				);
	}
	
	private String processNzRepoLink (String link) throws ImporterException {
				
		/*
		 * cellml feature 1:
		 * 
		 * hg path for exposures such as 
		 * http://models.cellml.org/exposure/2d0da70d5253291015a892326fa27b7b/aguda_b_1999.cellml/view
		 * http://models.cellml.org/e/4c/goldbeter_1991.cellml/view
		 * is
		 * http://models.cellml.org/workspace/aguda_b_1999
		 * http://models.cellml.org/workspace/goldbeter_1991
		 */
		if ((link.toLowerCase().contains("cellml.org/e") || link.toLowerCase().contains("physiomeproject.org/e")))
		{
			LOGGER.debug ("apparently got an exposure url: ", link);
			InputStream in = null;
			try {
				in = new URL (link).openStream();
			} catch (IOException e1) {
				LOGGER.error("Got a malformed URL to hg clone: ", link);
				throw new ImporterException("Got a malformed URL", e1);
			}
			
			try {
				String source = IOUtils.toString (in);
				Pattern hgClonePattern = Pattern.compile ("<input [^>]*value=.git clone ([^'\"]*). ");
				Matcher matcher = hgClonePattern.matcher (source);
				if (matcher.find()) {
					link = matcher.group (1);
					LOGGER.debug ("resolved exposure url to: ", link);
				}
			} catch (IOException e) {
				LOGGER.warn (e, "failed to retrieve cellml exposure source code");
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
		
		/*
		 * cellml feature 2:
		 * 
		 * hg path for files such as 
		 * http://models.cellml.org/workspace/aguda_b_1999/file/56788658c953e1d0a6bc745b81bdb0c0c20e9821/aguda_1999_bb.ai
		 * is
		 * http://models.cellml.org/workspace/aguda_b_1999
		 */
		if ((link.toLowerCase().contains("cellml.org/workspace/") || link.toLowerCase().contains("physiomeproject.org/workspace/")) && link.toLowerCase().contains("/file/"))
		{
			LOGGER.debug ("apparently got an cellml/physiome file url: ", link);
			int pos = link.indexOf ("/file/");
			link = link.substring (0, pos);
			LOGGER.debug ("resolved file url to: ", link);
		}
		
		// now we assume it is a link to a workspace, which can be hg-cloned.
		return link;
	}
	
}
