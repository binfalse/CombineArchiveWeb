package de.unirostock.sems.cbarchive.web.rest;
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
import java.net.URL;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.Archive.ReplaceStrategy;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.ImportRequest;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.OmexMetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbarchive.web.importer.Importer;

@Path( "/" )
public class ShareApi extends RestHelper {
	
	@GET
	@Path("/share/{user_path}")
	@Produces( MediaType.TEXT_PLAIN )
	public Response setUserPath( @CookieParam(Fields.COOKIE_PATH) String oldUserPath, @PathParam("user_path") String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie, @Context HttpServletRequest requestContext) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		WorkspaceHistory history = null;
		try {
			if( historyCookie != null && !historyCookie.isEmpty() ) {
				history = WorkspaceHistory.fromCookieJson(historyCookie);
			}
			
			if( history == null )
				history = new WorkspaceHistory();
			
			// puts current workspace into history
			if( history.containsWorkspace(user.getWorkspaceId()) == false )
				history.getRecentWorkspaces().add( user.getWorkspace() );
			history.setCurrentWorkspace( user.getWorkspaceId() );
			
			if( oldUserPath != null && !oldUserPath.isEmpty() && history.containsWorkspace( oldUserPath ) == false ) {
				Workspace workspace = WorkspaceManager.getInstance().getWorkspace(oldUserPath);
				if( workspace != null )
					history.getRecentWorkspaces().add( workspace );
			}
			
			history.setCurrentWorkspace( user.getWorkspaceId() );
			historyCookie = history.toCookieJson();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie ", historyCookie, e.getMessage());
		}
		
		String result = "setted " + user.getWorkspaceId();
		URI newLocation = null;
		try {
			if( requestContext != null ) {
				String uri = requestContext.getRequestURL().toString();
//				String uri = requestContext.getRequestURI();
				uri = uri.substring(0, uri.indexOf("rest/"));
				LOGGER.info("redirect sharing link ", requestContext.getRequestURL(), " to ", uri);
				newLocation = new URI( uri );
//				newLocation = new URI(requestContext.getScheme(), null, requestContext.getServerName(),
//						requestContext.getServerPort(), uri, requestContext.getQueryString(), null);
			}
			else
				newLocation = new URI("../");
			
		} catch (URISyntaxException e) {
			LOGGER.error(e, "Cannot generate relative URL to main app");
			return null;
		}
		
		return buildResponse(302, user)
				.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
				.entity(result).location(newLocation).build();
		
	}
	
	@GET
	@Path("/import")
	@Produces( MediaType.TEXT_PLAIN )
	public Response downloadRemoteArchive(@CookieParam(Fields.COOKIE_PATH) String userPath, @Context HttpServletRequest requestContext, @DefaultValue("http") @QueryParam("remote") String remoteUrl, @QueryParam("name") String archiveName, @QueryParam("type") String remoteType) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildTextErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		LOGGER.info(remoteUrl);
		
		if( remoteUrl == null || remoteUrl.isEmpty() ) {
			LOGGER.warn("empty remote url provided");
			return buildTextErrorResponse(400, user, "empty remote url provided");
		}
		
		if( remoteType == null || remoteType.isEmpty() )
			remoteType = Importer.IMPORT_HTTP;
		else
			remoteType = remoteType.toLowerCase();
		
		
		String archiveId = null;
		Importer importer = null;
		try {
			
			importer = Importer.getImporter(remoteType, remoteUrl, user);
			importer.importRepo();
			
			if( archiveName == null || archiveName.isEmpty() )
				archiveName = importer.getSuggestedName();
			
			// add archive to workspace
			archiveId = user.createArchive( archiveName, importer.getTempFile() );
			
		} catch (ImporterException e) {
			LOGGER.warn(e, "Cannot import remote archive!");
			return buildTextErrorResponse(400, user, e.getMessage(), "URL: " + remoteUrl);
		} catch (IOException | JDOMException | ParseException
				| CombineArchiveException | TransformerException e) {
			
			LOGGER.error(e, "Cannot read downloaded archive");
			return buildTextErrorResponse(400, user, "Cannot read/parse downloaded archive", e.getMessage(), "URL: " + remoteUrl);
		} finally {
			if( importer != null && importer.getTempFile().exists() )
				importer.getTempFile().delete();
			
			importer.close();
		}
		
		// redirect to workspace
		return buildResponse(302, user).entity(archiveId + "\n" + archiveName).location( generateRedirectUri(requestContext, archiveId) ).build();
	}
	
	@POST
	@Path("/import")
	@Produces( MediaType.TEXT_PLAIN )
	public Response importRemoteArchive( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson, ImportRequest request, @Context HttpServletRequest requestContext ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildTextErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( request == null || request.isValid() == false )
			return buildTextErrorResponse(400, user, "import request is not set properly");
		
		
		String archiveId = null;
		
		// try to import (if requested)
		if( request.isArchiveImport() ) {
			Importer importer = null;
			try {
				
				importer = Importer.getImporter(request.getType(), request.getRemoteUrl(), user);
				importer.importRepo();
				
				if( request.getArchiveName() == null || request.getArchiveName().isEmpty() )
					request.setArchiveName( importer.getSuggestedName() );
				
				// add archive to workspace
				archiveId = user.createArchive( request.getArchiveName(), importer.getTempFile() );
				
			} catch (ImporterException e) {
				LOGGER.warn(e, "Cannot import remote archive!");
				return buildTextErrorResponse(400, user, e.getMessage(), "URL: " + request.getRemoteUrl() );
				
			} catch (IOException | JDOMException | ParseException
					| CombineArchiveException | TransformerException e) {
				
				LOGGER.error(e, "Cannot read downloaded archive");
				return buildTextErrorResponse(400, user, "Cannot read/parse downloaded archive", e.getMessage(), "URL: " + request.getRemoteUrl() );
			} finally {
				if( importer != null && importer.getTempFile().exists() )
					importer.getTempFile().delete();
				
				importer.close();
			}
		}
		else {
			// just create an empty archive
			
			// set default name, if necessary
			if( request.getArchiveName() == null || request.getArchiveName().isEmpty() )
				request.setArchiveName( Fields.NEW_ARCHIVE_NAME );
			
			try {
				archiveId = user.createArchive( request.getArchiveName() );
			} catch (IOException | JDOMException | ParseException
					| CombineArchiveException | TransformerException e) {
				
				LOGGER.error(e, "Cannot create new archive");
				return buildTextErrorResponse(400, user, "Cannot create new archive", e.getMessage() );
			}
		}
		
		try ( Archive archive = user.getArchive(archiveId) ) {
			// TODO quota
			
			// set own VCard
			if( request.isOwnVCard() ) {
				setOwnVCard(user, request, archive);
			}
			
			// import additional files
			if( request.getAdditionalFiles() != null && request.getAdditionalFiles().size() > 0 ) {
				addAdditionalFiles(user, request, archive);
			}
				
			
		} catch (IOException | CombineArchiveWebException e) {
			LOGGER.error(e, "Cannot open newly created archive");
			return buildTextErrorResponse(500, user, "Cannot open newly created archive: ", e.getMessage() );
		} catch (ImporterException e) {
			LOGGER.error(e, "Something went wrong with the extended import");
			return buildTextErrorResponse(500, user, "Error while applying additional data to the archive");
		}
		
		// redirect to workspace
		return buildResponse(302, user).entity(archiveId + "\n" + request.getArchiveName()).location( generateRedirectUri(requestContext, archiveId) ).build();
	}
	
	private void setOwnVCard( UserManager user, ImportRequest request, Archive archive ) throws ImporterException {
		
		VCard vcard = request.getVcard();
		
		// check if any VCard info is available
		// (either from cookies of from the request it self)
		if( vcard == null ) {
			if( user.getData() != null ) 
				vcard = user.getData().getVCard();
			else 
				throw new ImporterException("No vcard information provided for archive annotation");
		}
		
		// get root element
		ArchiveEntryDataholder root = archive.getEntries().get("/");
		if( root != null ) {
			// look for existing omex meta information
			OmexMetaObjectDataholder omexMeta = null;
			for( MetaObjectDataholder meta : root.getMeta() )
				if( meta instanceof OmexMetaObjectDataholder ) {
					omexMeta = (OmexMetaObjectDataholder) meta;
					break;
				}
			
			if( omexMeta != null ) {
				// if there is already an omex entry,
				// just add our VCard and add a modified date
				omexMeta.getCreators().add(vcard);
				omexMeta.getModified().add( new Date() );
			}
			else {
				// create a new dataholder and fill it with some information
				omexMeta = new OmexMetaObjectDataholder();
				omexMeta.setCreators( new ArrayList<VCard>(1) );
				omexMeta.setModified( new ArrayList<Date>(1) );
				omexMeta.setCreated( new Date() );
				
				omexMeta.getCreators().add(vcard);
				omexMeta.getModified().add( new Date() );
				
				root.addMetaEntry(omexMeta);
			}
		}
	}
	
	private void addAdditionalFiles( UserManager user, ImportRequest request, Archive archive ) {
		
		for( ImportRequest.AdditionalFile addFile : request.getAdditionalFiles() ) {
			java.nio.file.Path temp = null;
			try {
				URL remoteUrl = new URL( addFile.getRemoteUrl() );
				
				// copy the stream to a temp file
				temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, FilenameUtils.getBaseName(remoteUrl.toString()) );
				// write file to disk
				OutputStream output = new FileOutputStream( temp.toFile() );
				InputStream input = remoteUrl.openStream();
				long uploadedFileSize = IOUtils.copy( input, output);
				
				output.flush();
				output.close();
				input.close();
				
				String path = addFile.getArchivePath();
				if( path == null || path.isEmpty() )
					path = FilenameUtils.getBaseName( remoteUrl.toString() );
				// remove leading slash
				if( path.startsWith("/") )
					path = path.substring(1);
				
				// add it
				ArchiveEntry entry = archive.addArchiveEntry(path, temp, ReplaceStrategy.RENAME);
				// add all meta data objects
				for( MetaObjectDataholder meta : addFile.getMetaData() ) {
					entry.addDescription( meta.getCombineArchiveMetaObject() );
				}
				
			} catch(IOException | CombineArchiveWebException e) {
				LOGGER.error(e, "Cannot download an additional file.", addFile.getRemoteUrl());
			} finally {
				if( temp != null )
					temp.toFile().delete();
			}
		}
		
	}
	
	private URI generateRedirectUri( HttpServletRequest requestContext, String archiveId ) {
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
	
}
