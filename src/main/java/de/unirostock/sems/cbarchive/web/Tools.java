/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;


// TODO: Auto-generated Javadoc
/**
 * The Class Tools.
 *
 * @author Martin Scharm
 */
public class Tools
{

	/** The Constant COOKIE_PATH. */
	public static final String COOKIE_PATH = "combinearchiveweba";

	/** The Constant COOKIE_GIVEN_NAME. */
	public static final String COOKIE_GIVEN_NAME = "combinearchivewebb";

	/** The Constant COOKIE_FAMILY_NAME. */
	public static final String COOKIE_FAMILY_NAME = "combinearchivewebc";

	/** The Constant COOKIE_MAIL. */
	public static final String COOKIE_MAIL = "combinearchivewebd";

	/** The Constant COOKIE_ORG. */
	public static final String COOKIE_ORG = "combinearchivewebe";

	// TODO: move this to the context.xml 
	/** The Constant STORAGE. */
	public static final File STORAGE = new File ("/tmp/CombineArchiveWebStorage");

	/** The Constant COOKIE_AGE. */
	public static final int COOKIE_AGE = 60*60*24*365;

	/** The Constant MAX_ARCHIVES. */
	public static final int MAX_ARCHIVES = 10;

	/** The Constant MAX_FILES_PER_ARCHIVE. */
	public static final int MAX_FILES_PER_ARCHIVE = 20;

	/** The Constant MAX_FILE_SIZE. */
	public static final int MAX_FILE_SIZE = 1024*1024;

	/** The Constant DATE_FORMATTER. */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss.SSS");


	public static User doLogin( HttpServletRequest request, HttpServletResponse response ) throws CombineArchiveWebException, CombineArchiveWebCriticalException {
		// find Cookies
		HttpSession session = request.getSession (true);
		CookieManager cookieManagement = new CookieManager (request, response);

		// gets the user class
		User user = getUser(cookieManagement);
		
		// user exits
		if (user != null)
			// set WorkingDirectory
			try {
				user.setWd( getWorkingDirectory(cookieManagement) );
			}
			catch (IOException e) {
				throw new CombineArchiveWebException("Could not find and obtain working directory", e);
			}
		else
		{
			throw new CombineArchiveWebCriticalException("Can not get/create user");
		} 


		return user;
	}

	/**
	 * Creates the user.
	 *
	 * @param cookies the cookies
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void createUser (CookieManager cookies) throws IOException
	{
		createWorkingDirectory (cookies);
	}

	/**
	 * Gets the user.
	 *
	 * @param cookies the cookies
	 * @return the user
	 */
	public static User getUser (CookieManager cookies)
	{
		Cookie c = cookies.getCookie (COOKIE_PATH);
		if (c == null)
			return null;

		c.setMaxAge (COOKIE_AGE);
		cookies.setCookie (c);


		Cookie givenName = cookies.getCookie (COOKIE_PATH);
		Cookie familyName = cookies.getCookie (COOKIE_PATH);
		Cookie mail = cookies.getCookie (COOKIE_PATH);
		Cookie organization = cookies.getCookie (COOKIE_PATH);

		if (givenName != null)
		{
			givenName.setMaxAge (COOKIE_AGE);
			cookies.setCookie (givenName);
		}
		if (familyName != null)
		{
			familyName.setMaxAge (COOKIE_AGE);
			cookies.setCookie (familyName);
		}
		if (mail != null)
		{
			mail.setMaxAge (COOKIE_AGE);
			cookies.setCookie (mail);
		}
		if (organization != null)
		{
			organization.setMaxAge (COOKIE_AGE);
			cookies.setCookie (organization);
		}

		return new User (c.getValue (),
				givenName == null ? null : givenName.getValue (),
						familyName == null ? null : familyName.getValue (),
								mail == null ? null : mail.getValue (),
										organization == null ? null : organization.getValue ()
				);

	}

	/**
	 * Creates the working directory.
	 *
	 * @param cookies the cookies
	 * @return the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static File createWorkingDirectory (CookieManager cookies) throws IOException
	{
		if (!STORAGE.exists () && !STORAGE.mkdirs ())
		{
			LOGGER.error ("cannot create storage : " + STORAGE);
			throw new IOException ("cannot create storage : " + STORAGE);
		}

		String tmp = UUID.randomUUID ().toString ();
		while (new File (STORAGE + "/" + tmp).exists ())
			tmp = UUID.randomUUID ().toString ();

		File wd = new File (STORAGE + "/" + tmp);

		if (!wd.mkdirs ())
		{
			LOGGER.error ("cannot create working directory: " + wd);
			throw new IOException ("cannot create working directory: " + wd);
		}
		Cookie c = new Cookie (COOKIE_PATH, tmp);
		c.setMaxAge (COOKIE_AGE);
		c.setPath ("/");
		cookies.setCookie (c);


		return wd;
	}

	/**
	 * Gets the working directory.
	 *
	 * @param cookies the cookies
	 * @return the working directory
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static File getWorkingDirectory (CookieManager cookies) throws IOException
	{
		Cookie c = cookies.getCookie (COOKIE_PATH);
		if (c == null)
			throw new IOException ("no cookie");

		String tmp = c.getValue ();
		if (!tmp.matches ("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
			throw new IOException ("invalid file name");

		File wd = new File (STORAGE + "/" + tmp);
		if (!wd.exists () || !wd.isDirectory ())
		{
			// maybe deleted at some point
			if (!wd.mkdirs ())
				throw new IOException ("path not found");
		}

		return wd;
	}



	/**
	 * Archive to json.
	 *
	 * @param ca the ca
	 * @return the jSON object
	 */
	public static JSONObject archiveToJson (CombineArchive ca)
	{
		JSONObject descr = new JSONObject ();
		ArchiveEntry master = ca.getMainEntry ();

		for (ArchiveEntry e : ca.getEntries ())
		{
			JSONObject entryDescr = new JSONObject ();
			entryDescr.put ("path", e.getFilePath ());
			entryDescr.put ("fileName", e.getFileName ());
			entryDescr.put ("format", e.getFormat ());

			if (e == master)
				entryDescr.put ("master", "true");

			JSONArray meta = new JSONArray ();
			for (MetaDataObject m : e.getDescriptions ())
				meta.add (m.getXmlDescription ());

			entryDescr.put ("meta", meta);
			descr.put (e.getFilePath (), entryDescr);
		}

		return descr;
	}


	/**
	 * Extract file name.
	 *
	 * @param part the part
	 * @return the string
	 */
	public static final String extractFileName (Part part)
	{
		if (part != null)
		{
			String header = part.getHeader ("content-disposition");
			if (header != null)
			{
				LOGGER.debug ("content-disposition not null: ", header);
				String[] items = header.split (";");
				for (String s : items)
				{
					LOGGER.debug ("current disposition: ", s);
					if (s.trim ().startsWith ("filename"))
						return s.substring (s.indexOf ("=") + 2, s.length () - 1);
				}
			}
		}
		else
			LOGGER.debug ("file part seems to be null -> cannot extract file name.");
		return "UploadedFile-" + DATE_FORMATTER.format (new Date ());
	}
}
