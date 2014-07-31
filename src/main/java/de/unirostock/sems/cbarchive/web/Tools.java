/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;


// TODO: Auto-generated Javadoc
/**
 * The Class Tools.
 *
 * @author Martin Scharm
 */
public class Tools
{

	/** The Constant DATE_FORMATTER. */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss.SSS");
	
	public static UserManager doLogin( HttpServletRequest request, HttpServletResponse response ) throws CombineArchiveWebException, CombineArchiveWebCriticalException {
		// find Cookies
		HttpSession session = request.getSession (true);
		CookieManager cookieManagement = new CookieManager (request, response);

		// gets the user class
		UserManager user = null;
		try {
			user = getUser(cookieManagement);
		}
		catch (IOException e) {
			throw new CombineArchiveWebCriticalException("Can not find and/or obtain working directory", e);
		}
		
		if( user == null ) 
			throw new CombineArchiveWebException("Can not get/create user");
		
		return user;
	}

	/**
	 * Gets the user.
	 *
	 * @param cookies the cookies
	 * @return the user
	 * @throws IOException 
	 */
	public static UserManager getUser (CookieManager cookies) throws IOException
	{
		Cookie pathCookie = cookies.getCookie (Fields.COOKIE_PATH);
		if (pathCookie == null)
			return null;

		cookies.setCookie (pathCookie);

		Cookie path 		= cookies.getCookie( Fields.COOKIE_PATH );
		Cookie givenName	= cookies.getCookie( Fields.COOKIE_GIVEN_NAME );
		Cookie familyName	= cookies.getCookie( Fields.COOKIE_FAMILY_NAME );
		Cookie mail			= cookies.getCookie( Fields.COOKIE_MAIL );
		Cookie organization	= cookies.getCookie( Fields.COOKIE_ORG );
		
		UserManager user = new UserManager(pathCookie.getValue());
		UserData userData = new UserData();
		user.setData(userData);
		
		if (givenName != null)
		{
			givenName.setMaxAge (Fields.COOKIE_AGE);
			userData.setGivenName(givenName.getValue());
			cookies.setCookie (givenName);
		}
		if (familyName != null)
		{
			familyName.setMaxAge (Fields.COOKIE_AGE);
			userData.setFamilyName(familyName.getValue());
			cookies.setCookie (familyName);
		}
		if (mail != null)
		{
			mail.setMaxAge (Fields.COOKIE_AGE);
			userData.setMail(mail.getValue());
			cookies.setCookie (mail);
		}
		if (organization != null)
		{
			organization.setMaxAge (Fields.COOKIE_AGE);
			userData.setOrganization(organization.getValue());
			cookies.setCookie (organization);
		}
		
		return user;

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
	
	public static String generateHashId( String input ) {
		try {
			byte[] hash = MessageDigest.getInstance(Fields.HASH_ALGO).digest( input.getBytes() );
			return DatatypeConverter.printHexBinary(hash);
		} catch (NoSuchAlgorithmException e) {
			// As fallback send the complete String
			return input;
		}
	}
}
