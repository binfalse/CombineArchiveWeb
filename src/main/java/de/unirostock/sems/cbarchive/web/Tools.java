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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.meta.MetaDataHolder;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.exception.QuotaException;


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

		UserManager user = null;
		if( WorkspaceManager.getInstance().hasWorkspace( pathCookie.getValue() ) ) {
			// workspace exists
			user = new UserManager(pathCookie.getValue());
			// parse vCard info
			if( userInfo != null && !userInfo.getValue().isEmpty() )
				user.setData( UserData.fromJson( userInfo.getValue() ) ); 

			storeUserCookies(cookies, user);
		}

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
	 * Checks for all quotas required to add/update an file within a CombineArchive. If a quota is exceeded it fails by throwing an QuotaException.
	 *  
	 * @param fileSize of the uploaded file
	 * @param archive
	 * @param user
	 * @throws QuotaException
	 */
	public static void checkQuotasOrFail( long fileSize, Archive archive, UserManager user ) throws QuotaException {
		
		// max size for upload
		if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && fileSize > 0 && Tools.checkQuota(fileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
			LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
			throw new QuotaException("QUOTA_UPLOAD_SIZE reached in workspace " + user.getWorkspaceId(), "The fetched file is to big.");
		}
		
		// max files in one archive
		if( Fields.QUOTA_FILE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota(archive.countArchiveEntries() + 1, Fields.QUOTA_FILE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_FILE_LIMIT reached in workspace ", user.getWorkspaceId());
			throw new QuotaException("QUOTA_FILE_LIMIT reached in workspace " + user.getWorkspaceId(), "The max amount of files in one archive is reached.");
		}
		
		// max archive size
		if( Fields.QUOTA_ARCHIVE_SIZE != Fields.QUOTA_UNLIMITED && fileSize > 0 && Tools.checkQuota(user.getWorkspace().getArchiveSize( archive.getId() ) + fileSize, Fields.QUOTA_ARCHIVE_SIZE) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_SIZE reached in workspace ", user.getWorkspaceId());
			throw new QuotaException("QUOTA_ARCHIVE_SIZE reached in workspace " + user.getWorkspaceId(), "The maximum size of one archive is reached.");
		}
		
		// max workspace size
		if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && fileSize > 0 && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + fileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
			LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
			throw new QuotaException("QUOTA_WORKSPACE_SIZE reached in workspace " + user.getWorkspaceId(), "The maximum size of one workspace is reached.");
		}
		
		// max total size
		if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && fileSize > 0 && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + fileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
			LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
			throw new QuotaException("QUOTA_TOTAL_SIZE reached in workspace " + user.getWorkspaceId(), "The maximum size is reached.");
		}
		
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

	/**
	 * checks whether a filename is blacklisted or not
	 * 
	 * @param filename
	 * @return true if filename is blacklisted
	 */
	public static boolean isFilenameBlacklisted( String filename ) {

		if( filename == null || filename.isEmpty() )
			return true;

		if( Fields.FILENAME_BLACKLIST.contains( FilenameUtils.getName(filename) ) ) 
			return true;

		return false;
	}

	/**
	 * Adds current date as modification and adds the creator, if not done yet, to every Omex description
	 * Also creates new Omex description, if create is set to true and only if necessary
	 * 
	 * @param entity
	 * @param creator
	 * @param create
	 */
	public static void addOmexMetaData(MetaDataHolder entity, VCard creator, boolean create) {
		addOmexMetaData(entity, creator, null, create);
	}
	
	/**
	 * Adds current date as modification and an additional description, as well as the creator,
	 * if not done yet, to every Omex description. Also creates new Omex description,
	 * if create is set to true and only if necessary
	 * 
	 * @param entity
	 * @param creator
	 * @param additionalDescription
	 * @param create
	 */
	public static void addOmexMetaData(MetaDataHolder entity, VCard creator, String additionalDescription, boolean create) {

		int added = 0;
		// save some checks
		if( creator != null && creator.isEmpty() )
			creator = null;
		
		if( additionalDescription != null && additionalDescription.isEmpty() )
			additionalDescription = null;

		// add modified date and own VCard to all omex descriptions for the root element
		for( MetaDataObject metaObject : entity.getDescriptions() ) {
			if( metaObject instanceof OmexMetaDataObject ) {
				OmexDescription meta = ((OmexMetaDataObject) metaObject).getOmexDescription();

				meta.getModified().add( new Date() );
				if( creator != null && !containsVCard(meta.getCreators(), creator) )
					// creator is set and not in Omex right now
					meta.getCreators().add(creator);
				if( additionalDescription != null )
					// add additional description to meta (e.g. "derived from")
					meta.setDescription( meta.getDescription() + "\n" + additionalDescription );
					
				added++;
			}
		}

		if( added == 0 && create == true ) {
			// meta was added to non entry -> create
			OmexDescription meta = new OmexDescription();
			meta.getModified().add( meta.getCreated() );
			if( creator != null )
				meta.getCreators().add( creator );
			if( additionalDescription != null )
				meta.setDescription(additionalDescription);

			// attach to entity
			entity.addDescription( new OmexMetaDataObject(meta) );
		}

	}

	/**
	 * Checks if the given VCard exists already in the Collection
	 * 
	 * @param collection
	 * @param vcard
	 * @return
	 */
	public static boolean containsVCard( Collection<VCard> collection, VCard vcard ) {

		if( collection == null )
			return vcard == null;
		else if( vcard == null )
			return false;

		for( VCard current : collection )
			if( areVCardEqual(current, vcard) )
				return true;

		return false;
	}

	/**
	 * Compares 2 VCards and returns true if both are identical in means of String.equal() or if both are null
	 * 
	 * @param vcard1
	 * @param vcard2
	 * @return
	 */
	public static boolean areVCardEqual( VCard vcard1, VCard vcard2 ) {

		if( vcard1 == vcard2 || (vcard1 == null && vcard2 == null) )
			return true;
		else if( vcard1 == null || vcard2 == null )
			return false;
		return	(vcard1.getGivenName() == null ? vcard2.getGivenName() == null : vcard1.getGivenName().equals( vcard2.getGivenName() )) &&
				(vcard1.getFamilyName() == null ? vcard2.getFamilyName() == null : vcard1.getFamilyName().equals( vcard2.getFamilyName() )) &&
				(vcard1.getEmail() == null ? vcard2.getEmail() == null : vcard1.getEmail().equals( vcard2.getEmail() )) &&
				(vcard1.getOrganization() == null ? vcard2.getOrganization() == null : vcard1.getOrganization().equals( vcard2.getOrganization() ));
	}

	/**
	 * Copies an InputStream into an OutputStream and closes all streams afterwards.
	 * Stops at max length.
	 * 
	 * @param input
	 * @param output
	 * @param maxLength
	 * @return
	 * @throws IOException
	 */
	public static long copyStream( InputStream input, OutputStream output, long maxLength ) throws IOException {

		byte[] buffer = new byte[Fields.DEFAULT_BUFFER_SIZE];
		long copied = 0;
		int red = 0;
		while( (red = input.read(buffer)) != -1 ) {
			output.write(buffer, 0, red);

			copied = copied + red;
			// abort, if maxLength is reached
			if( maxLength > 0 && copied > maxLength )
				break;
		}

		input.close();
		output.flush();
		output.close();

		return copied;
	}
	
	/**
	 * writes a input stream entirely into a newly created temp file.
	 * Closes all streams afterwards
	 * 
	 * @param tempFileName
	 * @param input
	 * @return Path to temp file
	 * @throws IOException
	 */
	public static Path writeStreamToTempFile(String tempFileName, InputStream input) throws IOException {

		// copy the stream to a temp file
		Path temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, tempFileName );
		// write file to disk
		OutputStream output = new FileOutputStream( temp.toFile() );
		IOUtils.copy( input, output);

		output.flush();
		output.close();
		input.close();
		
		return temp;
	}

	/**
	 * Suggests a filename for the queried file, base on the Content-Disposition Header field or the URL
	 * 
	 * @param request
	 * @param response
	 * @return A name suggestion or null
	 */
	public static String suggestFileNameFromHttpResponse( HttpRequest request, HttpResponse response ) {
		return suggestFileNameFromHttpResponse(request.getRequestLine().getUri(), response);
	}

	/**
	 * Suggests a filename for the queried file, base on the Content-Disposition Header field or the URL
	 * 
	 * @param request
	 * @param response
	 * @return A name suggestion or null
	 */
	public static String suggestFileNameFromHttpResponse( String remoteUrl, HttpResponse response ) {

		// try to evaluate name from url
		String urlName = FilenameUtils.getName( remoteUrl );

		// try to extract file name from http header
		String headerName = suggestFileNameFromHttpResponse(response);

		return headerName != null && headerName.isEmpty() == false ? headerName : urlName;
	}

	/**
	 * Suggests a filename for the queried file, base on the Content-Disposition Header field.
	 * 
	 * @param request
	 * @param response
	 * @return A name suggestion or null
	 */
	public static String suggestFileNameFromHttpResponse( HttpResponse response ) {

		// try to extract name from HttpHeader
		Header dispositionHeader = response.getFirstHeader("Content-Disposition");
		if( dispositionHeader != null && dispositionHeader.getValue() != null && dispositionHeader.getValue().isEmpty() == false ) {
			// disposition header is present -> extract name
			HeaderElement[] fileNameHeaderElements = dispositionHeader.getElements();
			if( fileNameHeaderElements.length > 0 ) {
				NameValuePair fileNamePair = fileNameHeaderElements[0].getParameterByName("filename");
				String suggestedName = fileNamePair != null ? fileNamePair.getValue() : null;
				LOGGER.debug("Extracted filename ", suggestedName, " from Http header");

				return suggestedName;
			}
		}

		return null;
	}
	
	/**
	 * removes all non alpha-numeric symbols from a file name
	 * 
	 * @param fileName
	 * @return
	 */
	public static String cleanUpFileName( String fileName ) {
		return fileName.replaceAll("[^A-Za-z0-9\\.]", "_");
	}

}
