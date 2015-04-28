/**
 * 
 */
package de.unirostock.sems.cbarchive.web;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;


// TODO: Auto-generated Javadoc
/**
 * The Class Tools.
 *
 * @author Martin Scharm
 */
public class Tools
{

	/** The Constant DATE_FORMATTER. */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");
	
	/**
	 * Tries to obtain user instance (workspace), if fails it crates a new one
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws CombineArchiveWebException
	 * @throws CombineArchiveWebCriticalException
	 */
	public static UserManager doLogin( HttpServletRequest request, HttpServletResponse response ) throws CombineArchiveWebException, CombineArchiveWebCriticalException {
		return doLogin(request, response, true);
	}
	
	/**
	 * Tries to obtain user instance (workspace) <br>
	 * if createNew is true, it also tries to create a new user instance.
	 * 
	 * @param request
	 * @param response
	 * @param createNew
	 * @return
	 * @throws CombineArchiveWebException
	 * @throws CombineArchiveWebCriticalException
	 */
	public static UserManager doLogin( HttpServletRequest request, HttpServletResponse response, boolean createNew ) throws CombineArchiveWebException, CombineArchiveWebCriticalException {
		// find Cookies
//		HttpSession session = request.getSession (true);
		CookieManager cookieManagement = new CookieManager (request, response);

		// gets the user class
		UserManager user = null;
		try {
			user = getUser(cookieManagement);
			if( user == null && createNew == true ) {
				user = new UserManager();
				storeUserCookies(cookieManagement, user);
			}
		}
		catch (IOException e) {
			throw new CombineArchiveWebCriticalException("Cannot find and/or obtain working directory", e);
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

		Cookie userInfo		= cookies.getCookie( Fields.COOKIE_USER );

		UserManager user = new UserManager(pathCookie.getValue());
		if( userInfo != null && !userInfo.getValue().isEmpty() )
			user.setData( UserData.fromJson( userInfo.getValue() ) ); 

		storeUserCookies(cookies, user);

		return user;

	}

	public static void storeUserCookies(CookieManager cookies, UserManager user) {

		cookies.setCookie( new Cookie(Fields.COOKIE_PATH, user.getWorkspaceId()) );

		if( user.getData() != null && user.getData().hasInformation() ) {
			UserData userData = user.getData();
			try {
				cookies.setCookie(new Cookie( Fields.COOKIE_USER, userData.toJson() ));
			} catch (JsonProcessingException e) {
				LOGGER.error(e, "Cannot store cookies, due to json errors");
			}
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
	
	/**
	 * Returns false, if a quota is exceeded. Otherwise true
	 * 
	 * @param currentValue
	 * @param quota
	 * @return
	 */
	public static boolean checkQuota( long currentValue, long quota ) {
		
		// Quota is set to unlimited
		if( quota == Fields.QUOTA_UNLIMITED )
			return true;
		
		LOGGER.info(currentValue, " vs ", quota);
		
		// check if quota is exceeded
		if( currentValue >= quota )
			return false;
		else
			return true;
	}
	
	/**
	 * Generates a redirect URI to an archive
	 * 
	 * @param requestContext
	 * @param archiveId
	 * @return
	 */
	public static URI generateArchiveRedirectUri( HttpServletRequest requestContext, String archiveId ) {
		URI newLocation = null;
		try {
			if( requestContext != null ) {
				String uri = requestContext.getRequestURL().toString();
				uri = uri.substring(0, uri.indexOf("rest/"));
				LOGGER.info("redirect to ", requestContext.getRequestURL(), " to ", uri);
				newLocation = new URI( uri + "#archive/" + archiveId );
			}
			else
				newLocation = new URI( "../#archive/" + archiveId );

		} catch (URISyntaxException e) {
			LOGGER.error(e, "Cannot generate relative URL to main app");
			return null;
		}

		return newLocation;
	}
	
	/**
	 * Generates Share URI to a workspace
	 * 
	 * @param requestContext
	 * @param workspaceId
	 * @return
	 */
	public static URI generateWorkspaceRedirectUri( HttpServletRequest requestContext, String workspaceId ) {
		URI newLocation = null;
		try {
			if( requestContext != null ) {
				String uri = requestContext.getRequestURL().toString();
				uri = uri.substring(0, uri.indexOf("rest/"));
				LOGGER.info("redirect to ", requestContext.getRequestURL(), " to ", uri);
				newLocation = new URI( uri + "rest/share/" + workspaceId );
			}
			else
				newLocation = new URI( "../rest/share/" + workspaceId );

		} catch (URISyntaxException e) {
			LOGGER.error(e, "Cannot generate relative URL to main app");
			return null;
		}

		return newLocation;
	}
}
