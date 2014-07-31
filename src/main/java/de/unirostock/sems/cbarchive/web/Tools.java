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
import javax.ws.rs.core.NewCookie;
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
			if( user == null ) {
				user = new UserManager();
				storeUserCookies(cookieManagement, user);
			}
		}
		catch (IOException e) {
			throw new CombineArchiveWebCriticalException("Can not find and/or obtain working directory", e);
		}
		
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

		Cookie givenName	= cookies.getCookie( Fields.COOKIE_GIVEN_NAME );
		Cookie familyName	= cookies.getCookie( Fields.COOKIE_FAMILY_NAME );
		Cookie mail			= cookies.getCookie( Fields.COOKIE_MAIL );
		Cookie organization	= cookies.getCookie( Fields.COOKIE_ORG );
		
		UserManager user = new UserManager(pathCookie.getValue());
		UserData userData = new UserData();
		user.setData(userData);
		
		if (givenName != null)
			userData.setGivenName(givenName.getValue());
		if (familyName != null)
			userData.setFamilyName(familyName.getValue());
		if (mail != null)
			userData.setMail(mail.getValue());
		if (organization != null)
			userData.setOrganization(organization.getValue());
		
		storeUserCookies(cookies, user);
		
		return user;

	}
	
	public static void storeUserCookies(CookieManager cookies, UserManager user) {
		
		cookies.setCookie( new Cookie(Fields.COOKIE_PATH, user.getPath()) );
		
		if( user.getData() != null && user.getData().hasInformation() ) {
			UserData userData = user.getData();
			cookies.setCookie(new Cookie( Fields.COOKIE_FAMILY_NAME, userData.getFamilyName() ));
			cookies.setCookie(new Cookie( Fields.COOKIE_GIVEN_NAME, userData.getGivenName() ));
			cookies.setCookie(new Cookie( Fields.COOKIE_MAIL, userData.getMail() ));
			cookies.setCookie(new Cookie( Fields.COOKIE_ORG, userData.getOrganization() ));
		}
		
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
