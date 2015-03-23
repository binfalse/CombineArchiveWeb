package de.unirostock.sems.cbarchive.web.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
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
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbext.Formatizer;


public class GitImporter extends Importer {
	
	private String gitUrl = null;
	private File tempDir = null;
	private Git repo = null;
	
	public GitImporter( String gitUrl, UserManager user ) {
		super(user);
		this.gitUrl = gitUrl;
	}
	
	public GitImporter( String gitUrl ) {
		this(gitUrl, null);
	}
	
	public String getGitUrl() {
		return gitUrl;
	}
	
	public GitImporter importRepo() throws ImporterException {
		
		// remove leading "git clone"
		if( gitUrl.toLowerCase().startsWith("git clone") )
			gitUrl = gitUrl.substring(10);
		
		cloneGit();
		buildArchive();
		
		return this;
	}
	
	public void cleanUp() {
		
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
					.setURI( gitUrl )
					.setDirectory( tempDir )
					.call();
			
		} catch (GitAPIException e) {
			LOGGER.error(e, "Cannot clone git repository: ", gitUrl);
			throw new ImporterException("Cannot clone git repository: " + gitUrl, e);
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
	
}
