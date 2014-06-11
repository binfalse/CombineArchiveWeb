/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;


// TODO: Auto-generated Javadoc
/**
 * The Class User.
 *
 * @author Martin Scharm
 */
public class User
{
	
	/** The path. */
	private String path;
	
	/** The given name. */
	private String givenName;
	
	/** The family name. */
	private String familyName;
	
	/** The mail. */
	private String mail;
	
	/** The organization. */
	private String organization;
	
	/** The wd. */
	private File wd;
	
	/** The props. */
	private Properties props;
	
	/** The props file. */
	private File propsFile;
	
	/** The Constant ARCHIVE_KEY_PRE. */
	public static final String ARCHIVE_KEY_PRE = "archive-";
	
	/** The Constant LAST_SEEN. */
	public static final String LAST_SEEN = "last-seen";
	
	/**
	 * Instantiates a new user.
	 *
	 * @param path the path
	 * @param givenName the given name
	 * @param familyName the family name
	 * @param mail the mail
	 * @param organization the organization
	 */
	public User (String path, String givenName, String familyName, String mail,
		String organization)
	{
		super ();
		this.path = path;
		this.givenName = givenName;
		this.familyName = familyName;
		this.mail = mail;
		this.organization = organization;
		wd = null;
		propsFile = null;
		props = new Properties();
	}
	
	/**
	 * Gets the wd.
	 *
	 * @return the wd
	 */
	public File getWd ()
	{
		return wd;
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath ()
	{
		return path;
	}
	
	/**
	 * Gets the given name.
	 *
	 * @return the given name
	 */
	public String getGivenName ()
	{
		return givenName;
	}
	
	/**
	 * Gets the family name.
	 *
	 * @return the family name
	 */
	public String getFamilyName ()
	{
		return familyName;
	}
	
	/**
	 * Gets the mail.
	 *
	 * @return the mail
	 */
	public String getMail ()
	{
		return mail;
	}
	
	/**
	 * Gets the organization.
	 *
	 * @return the organization
	 */
	public String getOrganization ()
	{
		return organization;
	}
	
	
	/**
	 * Checks if there is enough information.
	 *
	 * @return true, if user provided all necessary information
	 */
	public boolean hasInformation ()
	{
		return givenName != null && familyName != null && mail != null 
			&& givenName.length () > 0 && familyName.length () > 0 && mail.length () > 5;
	}

	/**
	 * Sets the wd.
	 *
	 * @param workingDirectory the new wd
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void setWd (File workingDirectory) throws IOException
	{
		this.wd = workingDirectory;
		propsFile = new File (wd.getAbsolutePath () + File.separatorChar + "settings.properties");
		if (propsFile.exists ())
		{
			InputStream input = new FileInputStream(propsFile);
			this.props.load (input);
		}
		props.setProperty (LAST_SEEN, Tools.DATE_FORMATTER.format (new Date ()));
	}
	
	/**
	 * Creates the new archive.
	 *
	 * @param name the name
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JDOMException the jDOM exception
	 * @throws ParseException the parse exception
	 * @throws CombineArchiveException 
	 * @throws TransformerException 
	 */
	public void createNewArchive (String name) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException
	{
		// create new archive
		String tmp = UUID.randomUUID ().toString ();
		File tmpFile = new File (wd.getAbsolutePath () + File.separatorChar + tmp);
		while (tmpFile.exists ())
		{
			tmp = UUID.randomUUID ().toString ();
			tmpFile = new File (wd.getAbsolutePath () + File.separatorChar + tmp);
		}
		LOGGER.info ("new archive will be stored in " + tmpFile);
		CombineArchive archive = new CombineArchive (tmpFile);
		archive.pack ();
		archive.close ();
		
		// set properties
		props.setProperty (ARCHIVE_KEY_PRE + tmp, name);
		storeProps ();
	}
	
	/**
	 * Store props.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void storeProps () throws IOException
	{
		LOGGER.info ("storing user properties to " + propsFile.getAbsolutePath ());
		props.store (new FileOutputStream (propsFile), null);
	}
	
	/**
	 * Gets the archives.
	 *
	 * @return the archives
	 */
	public JSONObject getArchives ()
	{
		JSONObject archs = new JSONObject ();
		for (Object p : props.keySet ())
		{
			String key = (String) p;
			if (key.startsWith (ARCHIVE_KEY_PRE))
			{
				String dir = key.substring (ARCHIVE_KEY_PRE.length ());
				File archive = new File (this.wd.getAbsolutePath () + File.separatorChar + dir);
				if (archive.exists ())
				try
				{
					CombineArchive ca = new CombineArchive (archive);
					archs.put (dir + ":" + props.getProperty (key), Tools.archiveToJson (ca));
					ca.close ();
				}
				catch (Exception e)
				{
					LOGGER.error (e, "couldn't read combine archive: ", archive);
				}
				else
					LOGGER.warn (dir, " is supposed to be an direcetory but it doesn't exist...");
			}
		}
		return archs;
	}
	
	public File getArchiveFile( String archiveId ) throws FileNotFoundException {
		
		// gets the properties Key for this archive
		String archiveKey = props.getProperty( ARCHIVE_KEY_PRE + archiveId );
		// check if exists
		if( archiveKey == null || archiveKey.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			
			// gets the dir name from key string
			String dir = archiveKey.substring (ARCHIVE_KEY_PRE.length ());
			if( dir.equals(archiveId) ) {
				// is the archive we are looking for
				
				// get the file
				File archive = new File (this.wd.getAbsolutePath () + File.separatorChar + dir);
				if( archive.isFile() && archive.exists() && archive.canRead() )
					return archive;
				else
					throw new FileNotFoundException("Can not find/read combine archive file for " + archiveId);
			}
		}
		
		// Should never happen, just here to make eclipse shut up 
		return null;
	}

	/**
	 * Adds a file to an archive.
	 *
	 * @param archId the arch id
	 * @param tmp the tmp
	 * @param name the name
	 * @param format the format
	 * @throws CombineArchiveWebException the combine archive web exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JDOMException the jDOM exception
	 * @throws ParseException the parse exception
	 * @throws CombineArchiveException the combine archive exception
	 * @throws TransformerException the transformer exception
	 */
	public void addFile (String archId, Path tmp, String name, String format) throws CombineArchiveWebException, IOException, JDOMException, ParseException, CombineArchiveException, TransformerException
	{
		if (props.getProperty (ARCHIVE_KEY_PRE + archId) == null)
			throw new CombineArchiveWebException ("you do not seem to have such an archive");
		
		File file = new File (this.wd.getAbsolutePath () + File.separatorChar + archId);
		if (!file.exists ())
			throw new CombineArchiveWebException ("archive not found.");
		
		CombineArchive ca = new CombineArchive (file);
		String ourName = name;
		ArchiveEntry exists = ca.getEntry (ourName);
		while (exists != null)
		{
			ourName = name + "-" + Tools.DATE_FORMATTER.format (new Date ());
			exists = ca.getEntry (ourName);
		}
		ca.addEntry (tmp.toFile (), ourName, format);
		ca.pack ();
		ca.close ();
	}

	/**
	 * Gets the archive.
	 *
	 * @param archId the arch id
	 * @return the archive
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JDOMException the jDOM exception
	 * @throws ParseException the parse exception
	 * @throws CombineArchiveException 
	 */
	/*public CombineArchive getArchive (String archId) throws IOException, JDOMException, ParseException, CombineArchiveException
	{
		if (props.getProperty (ARCHIVE_KEY_PRE + archId) == null)
			return null;
		
		File file = new File (this.wd.getAbsolutePath () + File.separatorChar + archId);
		if (!file.exists ())
			return null;
		
		return new CombineArchive (file);
	}*/

	/**
	 * Gets the new file.
	 *
	 * @param archId the arch id
	 * @param fileName the file name
	 * @return the new file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	/*public File getNewFile (String archId, String fileName) throws IOException
	{
		if (props.getProperty (ARCHIVE_KEY_PRE + archId) == null)
			return null;
		
		File dir = new File (this.wd.getAbsolutePath () + File.separatorChar + archId);
		if (!dir.exists ())
			return null;
		
		File target = new File (dir.getAbsolutePath () + File.separatorChar + fileName);
		
		while (target.exists ())
			target = new File (dir.getAbsolutePath () + File.separatorChar + fileName + "-" + new SimpleDateFormat ("yyyy-MM-dd_HH-mm-ss-S").format (new Date ()));
		
		target.createNewFile ();
		
		return target;
	}*/
}
