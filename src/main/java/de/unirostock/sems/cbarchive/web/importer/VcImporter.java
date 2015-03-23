package de.unirostock.sems.cbarchive.web.importer;
/*
CombineArchiveWeb - a WebInterface to read/create/write/manipulate/... COMBINE archives
Copyright (C) 2014  SEMS Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.JDOMException;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.LogCommand;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromHg;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbext.Formatizer;


/**
 * 
 */

/**
 * @author Martin Scharm
 *
 */
public class VcImporter
{
	/**
	 * @param archive
	 * @return true or false.
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws CombineArchiveException 
	 * @throws ParseException 
	 * @throws JDOMException 
	 * @throws CombineArchiveWebException 
	 */
	public static File importRepo (ArchiveFromHg archive) throws IOException, TransformerException, JDOMException, ParseException, CombineArchiveException, CombineArchiveWebException
	{
		String link = archive.getHgLink ();
		
		if( link == null || link.isEmpty() )
			throw new CombineArchiveWebException("The link should not be empty");
		
		
		// is it a link to nz!? (models.cellml or physiome)
		if (link.contains ("cellml.org/") || link.contains ("physiomeproject.org/"))
			link = processNzRepoLink (link);
		
		return cloneHg (link, archive);
	}
	
	public static File importRepo( String link ) throws CombineArchiveWebException, MalformedURLException, IOException, TransformerException, JDOMException, ParseException, CombineArchiveException {
		
		if( link == null || link.isEmpty() )
			throw new CombineArchiveWebException("The link should not be empty");
		
		
		// is it a link to nz!? (models.cellml or physiome)
		if (link.contains ("cellml.org/") || link.contains ("physiomeproject.org/"))
			link = processNzRepoLink (link);

		return cloneHg( link , null );
	}
	
	
	private static String processNzRepoLink (String link) throws MalformedURLException, IOException
	{
		/*
		 * 
		 * cellml feature 1:
		 * 
		 * if link starts with "hg clone", remove it.
		 * 
		 */
		if( link.toLowerCase().startsWith("hg clone ") )
			link = link.substring(9);
		
		/*
		 * 
		 * cellml feature 2:
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
			InputStream in = new URL (link).openStream();
			try
			{
				String source = IOUtils.toString (in);
				Pattern hgClonePattern = Pattern.compile ("<input [^>]*value=.hg clone ([^'\"]*). ");
				Matcher matcher = hgClonePattern.matcher (source);
				if (matcher.find())
				{
			    link = matcher.group (1);
					LOGGER.debug ("resolved exposure url to: ", link);
				}
			}
			catch (IOException e)
			{
				LOGGER.warn (e, "failed to retrieve cellml exposure source code");
			}
			finally
			{
				IOUtils.closeQuietly(in);
			}
		}
		
		/*
		 * 
		 * cellml feature 3:
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
	
	private static File cloneHg (String link, ArchiveFromHg archive) throws IOException, TransformerException, JDOMException, ParseException, CombineArchiveException, CombineArchiveWebException
	{
		// create new temp dir
		File tempDir = Files.createTempDirectory(Fields.TEMP_FILE_PREFIX, PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString("rwx------") )).toFile();
		if( !tempDir.isDirectory () && !tempDir.mkdirs() )
			throw new CombineArchiveWebException("The temporary directories could not be created: " + tempDir.getAbsolutePath ());
		
		// temp file for CombineArchive
		File archiveFile = File.createTempFile(Fields.TEMP_FILE_PREFIX, "ca-imported");
		archiveFile.delete(); // delete the tmp file, so the CombineArchive Lib will create a new file
		// create the archive
		CombineArchive ca = new CombineArchive(archiveFile);
		
		Repository repo = Repository.clone(tempDir, link);
		if( repo == null ) {	
			ca.close();
			LOGGER.error ("Cannot clone Mercurial Repository ", link, " into ", tempDir);
			throw new CombineArchiveWebException("Cannot clone Mercurial Repository " + link + " into " + tempDir);
		}
		
		List<File> relevantFiles = scanRepository(tempDir, repo);
		System.out.println ("before LogCommand");
		LogCommand logCmd = new LogCommand(repo);
		System.out.println ("after LogCommand");
		for (File cur : relevantFiles) {
			List<Changeset> relevantVersions = logCmd.execute(cur.getAbsolutePath ());
			
			ArchiveEntry caFile = ca.addEntry (
			   tempDir,
			   cur,
			   Formatizer.guessFormat (cur));
			
			// lets create meta!
			List<Date> modified = new ArrayList<Date> ();
			List<VCard> creators = new ArrayList<VCard> ();
			
			HashMap<String, VCard> users = new HashMap<String, VCard> ();
			for (Changeset cs : relevantVersions) {
				LOGGER.debug ("cs: " + cs.getTimestamp ().getDate () + " -- " + cs.getUser ());
				modified.add (cs.getTimestamp ().getDate ());
			
				String vcuser = cs.getUser ();
				String firstName = "";
				String lastName = "";
				String mail = "";
				
				String[] tokens = vcuser.split (" ");
				int lastNameToken = tokens.length - 1;
				// is there a mail address?
				if (tokens[lastNameToken].contains ("@")) {
					mail = tokens[lastNameToken];
					if (mail.startsWith ("<") && mail.endsWith (">"))
						mail = mail.substring (1, mail.length () - 1);
					lastNameToken--;
				}
				
				// search for a non-empty last name
				while (lastNameToken >= 0) {
					if (tokens[lastNameToken].length () > 0) {
						lastName = tokens[lastNameToken];
						break;
					}
					lastNameToken--;
				}
				
				// and first name of course...
				for (int i = 0; i < lastNameToken; i++) {
					if (tokens[i].length () > 0)
						firstName += tokens[i] + " ";
				}
				firstName = firstName.trim ();
				
				String userid = "[" + firstName + "] -- [" + lastName + "] -- [" + mail + "]";
				LOGGER.debug ("this is user: " + userid);
				if (users.get (userid) == null) {
					users.put (userid, new VCard (lastName, firstName, mail, null));
				}
			}
			
			for (VCard vc : users.values ())
				creators.add (vc);

			caFile.addDescription( new OmexMetaDataObject(
						new OmexDescription(creators, modified, modified.get(modified.size() - 1) )
					));
			
		}
		
		ca.pack ();
		ca.close ();
		repo.close ();
		
		// clean up the directory
		FileUtils.deleteDirectory(tempDir);
		
		// add the combine archive to the dataholder
		if( archive != null ) {
			//TODO ugly workaround with the lock. 
			archive.setArchiveFile(archiveFile, null);
			archive.getArchive().close();
		}
		
		return archiveFile;
	}
	
	
	
	private static List<File> scanRepository ( File location, Repository repo )
	{
		List<File> relevantFiles = new ArrayList<File>();

		// scans the directory recursively
		scanRepositoryDir( location, location, relevantFiles );

		return relevantFiles;
	}

	private static void scanRepositoryDir( File base, File dir, List<File> relevantFiles ) {
		
		String[] entries = dir.list();
		// nothing to scan in this dir
		if( entries == null )
			return;

		// looping throw all directory elements
		for( int index = 0; index < entries.length; index++ ) {
			File entry = new File( dir, entries[index] );

			if( entry.isDirectory() && entry.exists() && !entry.getName().startsWith(".") ) {
				// Entry is a directory and not hidden (begins with a dot) -> recursive
				scanRepositoryDir(base, entry, relevantFiles);
			}
			else if( entry.isFile() && entry.exists() ) {
				// Entry is a file -> check if it is relevant
				
				relevantFiles.add (entry);
			}

		}

	}
}
