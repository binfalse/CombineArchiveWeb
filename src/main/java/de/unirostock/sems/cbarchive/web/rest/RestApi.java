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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.QuotaManager;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.Archive.ReplaceStrategy;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromExisting;
import de.unirostock.sems.cbarchive.web.dataholder.FetchRequest;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.StatisticData;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.exception.ArchiveEntryUploadException;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbarchive.web.exception.QuotaException;
import de.unirostock.sems.cbarchive.web.importer.Importer;
import de.unirostock.sems.cbarchive.web.provider.ObjectMapperProvider;

@Path("v1")
public class RestApi extends RestHelper {
	
	@GET
	@Path("/heartbeat")
	@Produces( MediaType.TEXT_PLAIN )
	public Response heartbeat( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie ) {
		
		// check if there is the required cookie
		if( userPath == null || userPath.isEmpty() )
		{
			String result = "not ok: missing required cookie";
			return buildResponse(400, null)
					.entity(result)
					.build();
		}
		
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		WorkspaceHistory history = null;
		try {
			if( historyCookie != null && !historyCookie.isEmpty() )
				history = WorkspaceHistory.fromCookieJson(historyCookie);
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
		}
		
		// create a new history if necessary
		if( history == null )
			history = new WorkspaceHistory();
		
		// puts current workspace into history
		if( history.containsWorkspace(user.getWorkspaceId()) == false )
			history.getRecentWorkspaces().add( user.getWorkspace() );
		history.setCurrentWorkspace( user.getWorkspaceId() );
		
		try {
			historyCookie = history.toCookieJson();
		} catch (JsonProcessingException e) {
			LOGGER.error(e, "Can not serialize the workspace history cookie to json");
			historyCookie = "";
		}
		
		String result = "ok " + user.getWorkspaceId();
		return buildResponse(200, user)
				.entity(result)
				.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
				.build();
	}

	@GET
	@Path("/heartbeat/{user_path}")
	@Produces( MediaType.TEXT_PLAIN )
	public Response heartbeatSetPath( @PathParam("user_path") String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		String result = "setted " + user.getWorkspaceId();
		return buildResponse(200, user).entity(result).build();
	}
	
	@GET
	@Path("/store_settings")
	@Produces( MediaType.TEXT_PLAIN )
	public Response storeSettings( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
//		try {
//			user = new UserManager( userPath );
//			if( userJson != null && !userJson.isEmpty() )
//				user.setData( UserData.fromJson(userJson) );
//		} catch (IOException e) {
//			LOGGER.error(e, "Cannot create user");
//			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
//		}
		
		// store the settings
		WorkspaceManager.getInstance().storeSettings();
		
		String result = "ok";
		return buildResponse(200, user).entity(result).build();
	}
	
	@GET
	@Path("/stats")
	@Produces( MediaType.APPLICATION_JSON )
	public Response getStats( @CookieParam(Fields.COOKIE_PATH) String userPath, @QueryParam("secret") String secret ) {
		// user stuff
		UserManager user = null;
		try {
			if( userPath != null && !userPath.isEmpty() )
				user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		// only fetch user stats, if they are avialable
		StatisticData stats = null;
		if( user != null )
			stats = QuotaManager.getInstance().getUserStats(user);
		else
			stats = QuotaManager.getInstance().getStats();
		
		// if secret is corret -> enable full stats
		if( secret != null && Fields.STATS_SECRET != null && secret.equals(Fields.STATS_SECRET) )
			stats.setFullStats(true);
		else if( Fields.STATS_PUBLIC == false ) {
			// stats are not public and no secret was provided
			return buildErrorResponse(400, user, "no or wrong secret was provided.", "public stats are disabled");
		}
		
		return buildResponse(200, user).entity(stats).build();
	}
	
	@GET
	@Path("/workspaces")
	@Produces( MediaType.APPLICATION_JSON )
	public Response getWorkspaces( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie ) {
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
			
			historyCookie = history.toCookieJson();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie " + historyCookie, e.getMessage());
		}
		
		return buildResponse(200, user)
				.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
				.entity(history.getRecentWorkspaces())
				.build();
	}
	
	@GET
	@Path("/workspaces/{workspace_id}")
	@Produces( MediaType.APPLICATION_JSON )
	public Response getSingleWorkspace( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie, @PathParam("workspace_id") String requestedWorkspace ) {
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
			
			historyCookie = history.toCookieJson();
			Workspace workspace = history.getWorkspace(requestedWorkspace);
			
			return buildResponse(200, user)
					.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
					.entity(workspace)
					.build();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie " + historyCookie, e.getMessage());
		} catch (CombineArchiveWebException e) {
			LOGGER.error(e, "Cannot find requested workspace in history ", requestedWorkspace);
			return buildErrorResponse(500, user, "Cannot find requested workspace in history " + requestedWorkspace, e.getMessage());
		}
	}
	
	@PUT
	@Path("/workspaces/{workspace_id}")
	@Produces( MediaType.APPLICATION_JSON )
	public Response updateWorkspace( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie, @PathParam("workspace_id") String workspaceId, Workspace workspace ) {
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
			
			if( workspaceId == null || workspaceId.isEmpty() )
				workspaceId = workspace.getWorkspaceId();
			
			Workspace oldWorkspace = history.getWorkspace(workspaceId);
			oldWorkspace.setName( workspace.getName() );
			
			historyCookie = history.toCookieJson();
			
			return buildResponse(200, user)
					.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
					.entity(oldWorkspace)
					.build();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie " + historyCookie, e.getMessage());
		} catch (CombineArchiveWebException e) {
			LOGGER.error(e, "Cannot find requested workspace in history ", workspaceId);
			return buildErrorResponse(500, user, "Cannot find requested workspace in history " + workspaceId, e.getMessage());
		}
	}
	
	@DELETE
	@Path("/workspaces/{workspace_id}")
	@Produces( MediaType.TEXT_PLAIN )
	public Response deleteWorkspace( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie, @PathParam("workspace_id") String workspaceId ) {
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
			
			if( workspaceId == null || workspaceId.isEmpty() ) {
				LOGGER.error("No workspace id was provided for deletion");
				return buildErrorResponse(400, user, "No workspace ID provided");
			}
			
			// removes workspace
			history.removeWorkspaceFromHistory(workspaceId);
			
			// removed the current workspace?
			if( user.getWorkspaceId().equals(workspaceId) ) {
				String newCurrent = history.getRecentWorkspaces().size() > 0 ? history.getRecentWorkspaces().get(0).getWorkspaceId() : "new-workspace";
				user = new UserManager(newCurrent);
			}
			
			historyCookie = history.toCookieJson();
			return buildResponse(200, user)
					.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
					.entity("ok")
					.build();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie " + historyCookie, e.getMessage());
		}
	}
	
	// --------------------------------------------------------------------------------
	// own VCard
	
	@GET
	@Path("/vcard")
	@Produces( MediaType.APPLICATION_JSON )
	public Response getOwnVcard( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( user.getData() == null )
			user.setData( new UserData() );
		
		return buildResponse(200, user).entity(user.getData()).build();
	}
	
	@POST
	@Path("/vcard")
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateOwnVcard( @CookieParam(Fields.COOKIE_PATH) String userPath, UserData data ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( data.hasInformation() ) {
			user.setData(data);
			return buildResponse(200, user).entity(user.getData()).build();
		}
		else {
			LOGGER.warn("User ", user.getWorkspaceId(), " has provided insufficient information to update vCard");
			return buildErrorResponse(400, user, "insufficient user information");
		}
	}
	
	// --------------------------------------------------------------------------------
	// archives

	@GET
	@Path( "/archives" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getAllArchives( @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		// gets the list
		List<Archive> response = user.getArchives(false);
		// sorts the list of the archives
		Collections.sort(response, new Comparator<Archive>() {
			@Override
			public int compare(Archive o1, Archive o2) {
				return o1.getName().toLowerCase().compareTo( o2.getName().toLowerCase() );
			}
		});
		
		// build response
		return buildResponse(200, user).entity(response).build();
	}

	@GET
	@Path( "/archives/{archive_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getArchive( @PathParam("archive_id") String id, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		// gets the archive
		try {
			Archive response = user.getArchive(id, false);
			// build response
			return buildResponse(200, user).entity(response).build();
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} in WorkingDir {1}", id, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive!", e.getMessage() );
		}
	}

	@PUT
	@Path( "/archives/{archive_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchive( @PathParam("archive_id") String id, @CookieParam(Fields.COOKIE_PATH) String userPath, Archive archive ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( archive == null ) {
			LOGGER.error("update archive not possible if archive == null");
			return buildErrorResponse(400, null, "no archive was transmitted" );
		}
		
		try {
			user.renameArchive(id, archive.getName() );
			// gets archive with all entries
			archive = user.getArchive(id, false);
			return buildResponse(200, user).entity(archive).build();
		} catch (IllegalArgumentException | IOException | CombineArchiveWebException e) {
			LOGGER.error(e, MessageFormat.format("Cannot rename archive {0} to {3} in WorkingDir {1}", id, user.getWorkingDir(), archive.getName()) );
			return buildErrorResponse( 500, user, "Cannot rename archive!", e.getMessage() );
		}
	}

	@POST
	@Path( "/archives" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchive( @CookieParam(Fields.COOKIE_PATH) String userPath, Archive archive, @CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( archive == null ) {
			LOGGER.error("create archive not possible if archive == null");
			return buildErrorResponse(400, null, "no archive was transmitted" );
		}
		
		// check maximum archives
		if( Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}
		
		try {
			if( Importer.isImportable(archive) ) {
				
				File archiveFile = null;
				try {
					// import stuff
					Importer importer = Importer.getImporter(archive, user);
					archiveFile = importer.importRepo().getTempFile();
					importer.close();
					
					long repoFileSize = archiveFile.length();
					// max workspace size
					if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + repoFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
						LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						archiveFile.delete();
						return buildErrorResponse(507, user, "The maximum size of one workspace is reached.");
					}
					// max total size
					if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + repoFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
						LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						archiveFile.delete();
						return buildErrorResponse(507, user, "The maximum size is reached.");
					}
					
					String id = user.createArchive( archive.getName(), archiveFile, archive.isIncludeVCard() && user.hasData() ? user.getData().getVCard() : null );
					archive.setId(id);
					
				}
				catch (ImporterException e) {
					LOGGER.error (e, "Cannot import archive.");
					return buildErrorResponse( 500, user, "Cannot import archive!", e.getMessage() );
				} finally {
					if( archiveFile != null && archiveFile.exists() )
						archiveFile.delete();
				}
			}
			else {
				// Ordinary creation (only include VCard, if checkbox is checked)
				String id = user.createArchive( archive.getName(), archive.isIncludeVCard() && user.hasData() ? user.getData().getVCard() : null );
				archive.setId(id);
			}
			
			// trigger quota update
			QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
			
			return buildResponse(200, user).entity(archive).build();
			
		} catch (IOException | JDOMException | ParseException | CombineArchiveException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Cannot create archive in WorkingDir {0}", user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot create archive!", e.getMessage() );
		}
			
	}
	
	@POST
	@Path( "/archives" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	public Response createArchiveFromMultipart( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson, @FormDataParam("archive") String serializedArchive, @FormDataParam("file") FormDataBodyPart file ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		// maps the Archive dataholder manually
		Archive archive = null;
		try {
			ObjectMapper mapper = ((ObjectMapperProvider) providers.getContextResolver(ObjectMapper.class, MediaType.WILDCARD_TYPE)).getContext( null );
			archive = mapper.readValue(serializedArchive, Archive.class);
			
			if( archive == null ) {
				LOGGER.error("create archive not possible if archive == null");
				return buildErrorResponse(400, null, "no archive was transmitted" );
			}
		} catch (IOException e) {
			LOGGER.error(e, "Cannot parse archive information!");
			return buildErrorResponse(500, user, "Cannot parse archive information!");
		}
		
		if( archive instanceof ArchiveFromExisting == false ) {
			// archive is not generated from an existing file
			// delegate to the original Endpoint
			return createArchive(userPath, archive, userJson);
		}
		
		// check maximum archives
		if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}
		
		java.nio.file.Path temp = null;
		try {
			// check for mime type
			// TODO
			
			// figuring out a good temp file name (seems like URIs don't like Brackets) 
			String uploadedFileName = file.getFormDataContentDisposition().getFileName();
			uploadedFileName = Tools.cleanUpFileName(uploadedFileName);
			
			// write uploaded file to temp
			temp = Tools.writeStreamToTempFile( uploadedFileName, file.getEntityAs(InputStream.class) );
			long uploadedFileSize = temp.toFile().length();
					
			// max size for upload
			if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(uploadedFileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
				LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The uploaded file is to big.");
			}
			// max workspace size
			if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + uploadedFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
				LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The maximum size of one workspace is reached.");
			}
			// max total size
			if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + uploadedFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
				LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The maximum size is reached.");
			}
			
			// creates a existing archive in the working space (check is included)
			String id = user.createArchive( archive.getName(), temp.toFile(), archive.isIncludeVCard() && user.hasData() ? user.getData().getVCard() : null );
			archive.setId(id);
			
			// trigger quota update
			QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
			
			return buildResponse(200, user).entity(archive).build();
			
		} catch (IOException | JDOMException | ParseException | CombineArchiveException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Cannot create archive in WorkingDir {0}", user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot create archive!", e.getMessage() );
		} finally {
			// remove temp file
			if( temp != null )
				temp.toFile().delete();
		}
			
	}
	
	@DELETE
	@Path( "/archives/{archive_id}" )
	@Produces( MediaType.TEXT_PLAIN )
	public Response deleteArchive( @PathParam("archive_id") String id, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			user.deleteArchive(id);
		} catch (IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot delete archive {1} in WorkingDir {0}", user.getWorkingDir(), id) );
			return buildErrorResponse( 500, user, "Cannot delete archive!", e.getMessage() );
		}	
		
		// trigger quota update
		QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
		
		// just return a HTTP ok
		return buildResponse(200, user).entity("ok").build();
	}

	// --------------------------------------------------------------------------------
	// archive entries

	@GET
	@Path( "/archives/{archive_id}/entries" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getAllArchiveEntries( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			Archive archive = user.getArchive(archiveId);
			archive.close();

			// gets result and sorts it
			List<ArchiveEntryDataholder> result = new ArrayList<ArchiveEntryDataholder>( archive.getEntries().values() );
			Collections.sort(result, new Comparator<ArchiveEntryDataholder>() {
				@Override
				public int compare(ArchiveEntryDataholder o1, ArchiveEntryDataholder o2) {
					return o1.getFileName().toLowerCase().compareTo(o2.getFileName().toLowerCase());
				}
			});
			
			return buildResponse(200, user).entity(result).build();
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}

	}

	@GET
	@Path( "/archives/{archive_id}/entries/{entry_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getArchiveEntry( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			Archive archive = user.getArchive(archiveId);
			archive.close();
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity(entry).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}

	@PUT
	@Path( "/archives/{archive_id}/entries/{entry_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchiveEntry( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, ArchiveEntryDataholder newEntry, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			user.updateArchiveEntry(archiveId, newEntry);
			
			Archive archive = user.getArchive(archiveId);
			archive.close();
			ArchiveEntryDataholder entry = archive.getEntries().get( newEntry.getFilePath() );
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity(entry).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot update archive entry {0}/{1} in WorkingDir {2}", archiveId, newEntry.getFilePath(), user.getWorkingDir()) );
			return buildErrorResponse( 500, user, MessageFormat.format("Cannot update archive entry {0}/{1} in WorkingDir {2}", archiveId, newEntry.getFilePath(), user.getWorkingDir()), e.getMessage() );
		}
	}
	
	@SuppressWarnings("resource")
	@POST
	@Path( "/archives/{archive_id}/entries" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchiveEntry( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson,
										List<FetchRequest> requestList ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		// TODO
		Archive archive = null;
		LinkedList<Object> result = new LinkedList<Object>();
		try {
			archive = user.getArchive(archiveId);
			
			// check maximum files in archive -> is the limit already reached, without uploading anything new?
			if( Fields.QUOTA_FILE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( archive.countArchiveEntries(), Fields.QUOTA_FILE_LIMIT) == false ) {
				LOGGER.warn("QUOTA_FILE_LIMIT reached in workspace ", user.getWorkspaceId());
				return buildErrorResponse(507, user, "The max amount of files in one archive is reached.");
			}
			
			CloseableHttpClient client = HttpClientBuilder.create().build();
			for( FetchRequest request : requestList ) {
				
				if( request == null || request.isValid() == false ) {
					LOGGER.error("Got invalid fetch request, to add file from remote url");
					result.add( new ArchiveEntryUploadException("Got invalid fetch request,  to add file from remote url.") );
				}
				
				String fileName = null;
				HttpGet getRequest = new HttpGet(request.getRemoteUrl());
				CloseableHttpResponse response = client.execute( getRequest );
				
				// check for valid response
				if( response.getStatusLine().getStatusCode() != 200 ) {
					LOGGER.warn("Cannot fetch remote file.", request.getRemoteUrl(), "Status: ", response.getStatusLine().getStatusCode());
					result.add( new ArchiveEntryUploadException(MessageFormat.format("Cannot fetch remote file. {1} Status: {0}", response.getStatusLine().getStatusCode(), request.getRemoteUrl()), request.getPath() ));
				}
				
				// try to find sufficient file name
				fileName = Tools.suggestFileNameFromHttpResponse(getRequest, response);
				LOGGER.debug("Suggested name for fetched file is ", fileName);
				
				// clean up name
				fileName = Tools.cleanUpFileName(fileName);
				LOGGER.debug("Suggested and cleaned name for fetched file is ", fileName);
				
				// check content length
				Header contentLengthHeader = response.getFirstHeader("Content-Length");
				long contentLength = 0;
				if( contentLengthHeader != null && contentLengthHeader.getValue() != null && contentLengthHeader.getValue().isEmpty() == false ) {
					contentLength = Long.valueOf( contentLengthHeader.getValue() );
				} else {
					contentLength = response.getEntity().getContentLength();
				}
				
				// check for all quotas
				try {
					Tools.checkQuotasOrFail(contentLength, archive, user);
				}
				catch (QuotaException e) {
					getRequest.abort();
					result.add( new ArchiveEntryUploadException(e.getUserMessage(), request.getPath() + fileName) );
					continue;
				}
				
				// determine most limiting quota
				String limitError = null;
				long limit = Long.MAX_VALUE;
				if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_UPLOAD_SIZE < limit ) {
					limit = Fields.QUOTA_UPLOAD_SIZE;
					limitError = "The fetched file is to big.";
				}
				if( Fields.QUOTA_ARCHIVE_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_ARCHIVE_SIZE - user.getWorkspace().getArchiveSize(archiveId) < limit ) {
					limit = Fields.QUOTA_ARCHIVE_SIZE - user.getWorkspace().getArchiveSize(archiveId);
					limitError = "The maximum size of one archive is reached.";
				}
				if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_WORKSPACE_SIZE - QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) < limit ) {
					limit = Fields.QUOTA_WORKSPACE_SIZE - QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace());
					limitError = "The maximum size of one workspace is reached.";
				}
				if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_TOTAL_SIZE - QuotaManager.getInstance().getTotalSize() < limit ) {
					limit = Fields.QUOTA_TOTAL_SIZE - QuotaManager.getInstance().getTotalSize();
					limitError = "The maximum size is reached.";
				}
				
				
				// download the file
				java.nio.file.Path tempFile = Files.createTempFile(Fields.TEMP_FILE_PREFIX, fileName);
				FileOutputStream fileOutput = new FileOutputStream(tempFile.toFile());
				long copied = Tools.copyStream( response.getEntity().getContent() , fileOutput, limit == Long.MAX_VALUE ? 0 : limit );
				
				// quota was exceeded afterwards
				if( copied >= limit ) {
					LOGGER.error("Exceeded quota while download: ", limitError, "in workspace ", user.getWorkspaceId());
					result.add( new ArchiveEntryUploadException(limitError, request.getPath() + fileName) );
					
					getRequest.abort();
					response.close();
					tempFile.toFile().delete();
					continue;
				}
				
				// override flag
				ReplaceStrategy strategy = ReplaceStrategy.fromString( request.getStrategy() ); 
						
				// add the file in the currently selected path
				ArchiveEntry entry = archive.addArchiveEntry(request.getPath() + fileName, tempFile, strategy);
				
				// add default meta information
				Tools.addOmexMetaData(entry,
						user.getData() != null && user.getData().hasInformation() ? user.getData().getVCard() : null,
						MessageFormat.format("Derived from: {0}", request.getRemoteUrl()),
						true);
				
				LOGGER.info(MessageFormat.format("Successfully fetched and added file {0} to archive {1}", fileName, archiveId));
				
				// clean up
				tempFile.toFile().delete();
				// add to result list
				result.add( new ArchiveEntryDataholder(entry) );
			}
			
			synchronized (archive) {
				// pack and close the archive
				archive.packAndClose();
				archive = null;
			}
			
			// trigger quota update
			QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
			// return all successfully uploaded files
			return buildResponse(200, user).entity(result).build();
			
		} catch (CombineArchiveWebException | IOException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Error while fetching/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir() ));
			return buildErrorResponse(500, user, "Error while fetching file: " + e.getMessage() );
		} finally {
			try {
				if( archive != null )
					archive.close();
			}
			catch (IOException e) {
				LOGGER.error(e, "Final closing of archive caused exception");
			}
		}
		
	}
	
	@SuppressWarnings("resource")
	@POST
	@Path( "/archives/{archive_id}/entries" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	public Response createArchiveEntry( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath, @FormDataParam("files[]") List<FormDataBodyPart> files,
			@FormDataParam("options") String optionString, @FormDataParam("path") String path, @CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( files == null ) {
			LOGGER.error("No files were uploaded!");
			return buildErrorResponse(400, user, "No files were uploaded!");
		}
		
		Map<String, String> options = null;
		try {
			ObjectMapper mapper = ((ObjectMapperProvider) providers.getContextResolver(ObjectMapper.class, MediaType.WILDCARD_TYPE)).getContext( null );
			options = mapper.readValue(optionString, new TypeReference<Map<String, String>>() {} );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot parse options String.");
			return buildErrorResponse(500, user, "Cannot read options string.");
		}
		
		Archive archive = null;
		try {
			archive = user.getArchive(archiveId);
			List<Object> result = new LinkedList<Object>();
			
			// adds ending slash
			if( path == null || path.isEmpty() )
				path = "/";
			else if( !path.endsWith("/") )
				path = path + "/";
			
			// check maximum files in archive -> is the limit already reached, without uploading anything new?
			if( Fields.QUOTA_FILE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( archive.countArchiveEntries(), Fields.QUOTA_FILE_LIMIT) == false ) {
				LOGGER.warn("QUOTA_FILE_LIMIT reached in workspace ", user.getWorkspaceId());
				return buildErrorResponse(507, user, "The max amount of files in one archive is reached.");
			}
			
			for( FormDataBodyPart file : files ) {
				String fileName = null;
				try {
					fileName = file.getFormDataContentDisposition().getFileName();
					// remove leading slash
					if( fileName.startsWith("/") )
						fileName = fileName.substring(1);
					
					// figuring out a good temp file name (seems like URIs don't like Brackets) 
					String uploadedFileName = file.getFormDataContentDisposition().getFileName();
					uploadedFileName = Tools.cleanUpFileName(uploadedFileName);
					
					// copy the stream to a temp file
					java.nio.file.Path temp = Tools.writeStreamToTempFile( uploadedFileName, file.getEntityAs(InputStream.class) ); 
					long uploadedFileSize = temp.toFile().length();
					
					// check for all quotas
					try {
						Tools.checkQuotasOrFail(uploadedFileSize, archive, user);
					}
					catch (QuotaException e) {
						// remove temp file
						temp.toFile().delete();
						result.add( new ArchiveEntryUploadException(e.getUserMessage(), path + fileName) );
						continue;
					}
					
					// override flag
					String opt = (options != null) ? options.get( file.getFormDataContentDisposition().getFileName() ) : null;
					ReplaceStrategy strategy = ReplaceStrategy.fromString(opt); 
							
					// add the file in the currently selected path
					ArchiveEntry entry = archive.addArchiveEntry(path + fileName, temp, strategy);
					
					// add default meta information
					Tools.addOmexMetaData(entry,
							user.getData() != null && user.getData().hasInformation() ? user.getData().getVCard() : null,
							true);
					
					LOGGER.info(MessageFormat.format("Successfully added file {0} to archive {1}", fileName, archiveId));
					
					// clean up
					temp.toFile().delete();
					// add to result list
					result.add( new ArchiveEntryDataholder(entry) );
				}
				catch (CombineArchiveWebException | IOException e) {
					LOGGER.error(e, MessageFormat.format("Error while uploading/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir() ));
					String message = e.getMessage();
					if( message == null || message.isEmpty() )
						message = MessageFormat.format("Error while uploading/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir());
					
					result.add( new ArchiveEntryUploadException(message, path + fileName) );
					continue;
				}
				
			}
			
			synchronized (archive) {
				// pack and close the archive
				archive.packAndClose();
				archive = null;
			}
			
			// trigger quota update
			QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
			
			// return all successfully uploaded files
			return buildResponse(200, user).entity(result).build();
			
		} catch (CombineArchiveWebException | IOException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Error while uploading/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir() ));
			return buildErrorResponse(500, user, "Error while uploading file: " + e.getMessage() );
		} finally {
			try {
				if( archive != null )
					archive.close();
			}
			catch (IOException e) {
				LOGGER.error(e, "Final closing of archive caused exception");
			}
		}
		
	}
	
	@DELETE
	@Path( "/archives/{archive_id}/entries/{entry_id}" )
	@Produces( MediaType.TEXT_PLAIN )
	public Response deleteArchiveEntry( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		// getting the archive
		try {
			Archive archive = user.getArchive(archiveId);
			CombineArchive combineArchive = archive.getArchive();
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			if( entry == null ) {
				return buildErrorResponse(404, user, "Cannot find archive entry"); 
			}
			
			// removes the entry and pack/closes the archive
			try {
				boolean result = combineArchive.removeEntry( entry.getArchiveEntry() );
				combineArchive.pack();
				
				// trigger quota update
				QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );
				
				if( result )
					return buildResponse(200, user).entity("ok").build();
				else {
					LOGGER.error("Cannot move meta description for entry ", entryId, " in Archive ", archiveId, " in Workspace ", user.getWorkspaceId());
					return buildErrorResponse(500, user, "Cannot remove meta description");
				}
			} catch (TransformerException e) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot delete meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.close();
			}
			
		} catch (FileNotFoundException | CombineArchiveWebException e) {
			LOGGER.warn(e, "Cannot find archive to delete an entry");
			return buildErrorResponse(404, user, "Cannot find archive", e.getMessage());
		} catch (IOException e) {
			LOGGER.warn(e, "Cannot delete archive entry");
			return buildErrorResponse(404, user, "Cannot delete archive entry", e.getMessage());
		}
	}
	
	// --------------------------------------------------------------------------------
	// Meta Object Endpoints
	
	@GET
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getAllMetaObjects( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			Archive archive = user.getArchive(archiveId);
			archive.close();
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity( entry.getMeta() ).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}
	
	@GET
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta/{meta_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getMetaObject( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @PathParam("meta_id") String metaId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
				
		try {
			Archive archive = user.getArchive(archiveId);
			archive.close();
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry == null )
				return buildErrorResponse(404, user, "No such entry found");
			
			// iterate over all meta entries
			MetaObjectDataholder metaObject = entry.getMetaById(metaId);
			
			// check if meta entry exists
			if( metaObject != null )
				return buildResponse(200, user).entity( metaObject ).build();
			else
				return buildErrorResponse(404, user, "No such meta entry found");
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}
	
	@PUT
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta/{meta_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateMetaObject( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @PathParam("meta_id") String metaId, @CookieParam(Fields.COOKIE_PATH) String userPath, MetaObjectDataholder metaObject ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			Archive archive = user.getArchive(archiveId);
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry == null ) {
				archive.close();
				return buildErrorResponse(404, user, "No such entry found");
			}
			
			// get meta
			MetaObjectDataholder oldMetaObject = entry.getMetaById(metaId);
			metaId = metaObject.getId();
			
			// check if meta entry exists
			if( oldMetaObject == null ) {
				archive.close();
				return buildErrorResponse(404, user, "No such meta entry found");
			}
			
			// update and pack the archive
			try {
				// update the entry
				oldMetaObject.update( metaObject );
				archive.getArchive().pack();
				// force to re-generate the id, after the pack
				oldMetaObject.generateId();
			} catch( CombineArchiveWebException e ) {
				// Something went wrong while the update process
				LOGGER.error(e, "Not able to update the meta element.");
				return buildErrorResponse(400, user, "Not able to update the meta element.", e.getMessage());
			} catch( IOException | TransformerException e ) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.close();
			}
			
			return buildResponse(200, user).entity( oldMetaObject ).build();
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}
	
	@POST
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createMetaObject( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath, MetaObjectDataholder metaObject ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
				
		try {
			Archive archive = user.getArchive(archiveId);
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry == null ) {
				archive.close();
				return buildErrorResponse(404, user, "No such entry found");
			}
			
			// add the description to the entry and pack the archive
			try {
				entry.addMetaEntry( metaObject );
				archive.getArchive().pack();
				// force to re-generate the id, after the pack
				metaObject.generateId();
			} catch( IOException | TransformerException e ) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot create meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.close();
			}
			
			return buildResponse(200, user).entity( metaObject ).build();
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive ", archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}
	
	@DELETE
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta/{meta_id}" )
	@Produces( MediaType.TEXT_PLAIN )
	public Response deleteMetaObject( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @PathParam("meta_id") String metaId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			Archive archive = user.getArchive(archiveId);
			CombineArchive combineArchive = archive.getArchive();
			ArchiveEntryDataholder entry = archive.getEntryById(entryId);
			
			// check if entry exists
			if( entry == null ) {
				archive.close();
				return buildErrorResponse(404, user, "No such entry found");
			}
			
			// iterate over all meta entries
			MetaObjectDataholder metaObject = entry.getMetaById(metaId);
			
			// check if meta entry exists
			if( metaObject == null ) {
				archive.close();
				return buildErrorResponse(404, user, "No such meta entry found");
			}
			
			// removes the meta entry and pack/closes the archive
			try {
				boolean result = false;
				result = entry.getMetaDataHolder().removeDescription( metaObject.getMetaObject() );
					
				combineArchive.pack();
				
				if( result )
					return buildResponse(200, user).entity("ok").build();
				else {
					LOGGER.error("Cannot remove meta description for entry ", entryId, " in Archive ", archiveId, " in Workspace ", user.getWorkspaceId());
					return buildErrorResponse(500, user, "Cannot remove meta description");
				}
			} catch (TransformerException e) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot delete meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.close();
			}
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}

	@PUT
	@Path( "/archives/{archive_id}/enrich" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response enrichArchive ( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if (Fields.SEDML_WEBTOOLS_API_URL == null || Fields.SEDML_WEBTOOLS_API_URL.isEmpty ()) {
			LOGGER.info("link to SEDML webtools invalid");
			return buildErrorResponse(503, null, "not supported at this instance");
		}
		
		File archiveFile = null;
		Archive archive = null;
		LinkedList<Object> result = new LinkedList<Object>();
		try {
			archiveFile = user.getArchiveFile(archiveId);
			archive = user.getArchive(archiveId);
			// check maximum files in archive -> is the limit already reached, without uploading anything new?
			if( Fields.QUOTA_FILE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( archive.countArchiveEntries(), Fields.QUOTA_FILE_LIMIT) == false ) {
				LOGGER.warn("QUOTA_FILE_LIMIT reached in workspace ", user.getWorkspaceId());
				return buildErrorResponse(507, user, "The max amount of files in one archive is reached.");
			}
			
			HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart("file", new FileBody(archiveFile))
                    .build();
		    HttpPost request = new HttpPost(Fields.SEDML_WEBTOOLS_API_URL);
		    request.setEntity(entity);
		    
		    CloseableHttpClient client = HttpClientBuilder.create().build();
		    CloseableHttpResponse response = client.execute(request);
		    

			// check if file exists
			if( response.getStatusLine().getStatusCode() != 200 ) {
				LOGGER.warn( response.getStatusLine().getStatusCode(), " ", response.getStatusLine().getReasonPhrase(), " while enriching ", archiveId, " at ", Fields.SEDML_WEBTOOLS_API_URL);
				return buildErrorResponse(502, null, "SEDML webtools failed to execute simulation at " + Fields.SEDML_WEBTOOLS_API_URL);
			}
			
			HttpEntity results = response.getEntity();
			if( results == null ) {
				LOGGER.error("No content returned while donwloading simulation resutls from SEDML webtools at ", Fields.SEDML_WEBTOOLS_API_URL);
				return buildErrorResponse(502, null, "No content returned while donwloading simulation resutls from SEDML webtools at " + Fields.SEDML_WEBTOOLS_API_URL);
			}
			
			Header contentLengthHeader = response.getFirstHeader("Content-Length");
			long contentLength = 0;
			if( contentLengthHeader != null && contentLengthHeader.getValue() != null && contentLengthHeader.getValue().isEmpty() == false ) {
				contentLength = Long.valueOf( contentLengthHeader.getValue() );
			} else {
				contentLength = response.getEntity().getContentLength();
			}
			
			// check for all quotas
			try {
				Tools.checkQuotasOrFail(contentLength, archive, user);
			}
			catch (QuotaException e) {
				request.abort();
				response.close();
				LOGGER.error(e, "quota error");
				return buildErrorResponse(507, null, "quota error: " + e.getMessage ());
			}
			

			// determine most limiting quota
			String limitError = null;
			long limit = Long.MAX_VALUE;
			if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_UPLOAD_SIZE < limit ) {
				limit = Fields.QUOTA_UPLOAD_SIZE;
				limitError = "The fetched file is to big.";
			}
			if( Fields.QUOTA_ARCHIVE_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_ARCHIVE_SIZE - user.getWorkspace().getArchiveSize(archiveId) < limit ) {
				limit = Fields.QUOTA_ARCHIVE_SIZE - user.getWorkspace().getArchiveSize(archiveId);
				limitError = "The maximum size of one archive is reached.";
			}
			if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_WORKSPACE_SIZE - QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) < limit ) {
				limit = Fields.QUOTA_WORKSPACE_SIZE - QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace());
				limitError = "The maximum size of one workspace is reached.";
			}
			if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Fields.QUOTA_TOTAL_SIZE - QuotaManager.getInstance().getTotalSize() < limit ) {
				limit = Fields.QUOTA_TOTAL_SIZE - QuotaManager.getInstance().getTotalSize();
				limitError = "The maximum size is reached.";
			}
			
			

			// download the file
			java.nio.file.Path tempFile = Files.createTempFile(Fields.TEMP_FILE_PREFIX, "simulation-result");
			FileOutputStream fileOutput = new FileOutputStream(tempFile.toFile());
			long copied = Tools.copyStream( response.getEntity().getContent() , fileOutput, limit == Long.MAX_VALUE ? 0 : limit );
			
			// poor man's heuristic: quota was exceeded afterwards
			if( copied >= limit ) {
				request.abort();
				response.close();
				tempFile.toFile().delete();
				
				LOGGER.error("Exceeded quota while download: ", limitError, "in workspace ", user.getWorkspaceId());
				return buildErrorResponse(507, null, "Exceeded quota: " + limitError);
			}
			
			// override flag
			ReplaceStrategy strategy = ReplaceStrategy.OVERRIDE; 
			File tempResult = File.createTempFile(Fields.TEMP_FILE_PREFIX, "simulation-result");
			
			CombineArchive simulationResults = new CombineArchive(tempFile.toFile());
			// add simulation results
			for (ArchiveEntry entry : simulationResults.getEntries ()) {
				// skip the archive itself
				if (entry.getFormat().toString().contains("/omex"))
					continue;
				// skip the model
				if (entry.getFormat().toString().contains("/sbml"))
					continue;
				// skip the sedml script
				if (entry.getFormat().toString().contains("/sed-ml"))
					continue;
				
				entry.extractFile(tempResult);
				
				String filename = "/results/sedml-webtools/" + entry.getFileName();
				ArchiveEntry newEntry = archive.addArchiveEntry(filename, tempResult.toPath(), strategy);
				newEntry.setFormat(entry.getFormat());
				
				// add default meta information
				Tools.addOmexMetaData(newEntry,
						user.getData() != null && user.getData().hasInformation() ? user.getData().getVCard() : null,
						"Simulation results from SEDML webtools at " + Fields.SEDML_WEBTOOLS_API_URL,
						true);
				
				LOGGER.info(MessageFormat.format("Successfully added simulation results file {0} to archive {1}", filename, archiveId));
				
				// clean up
				tempFile.toFile().delete();
				// add to result list
				result.add( new ArchiveEntryDataholder(entry) );
			}
			
			tempResult.delete();
			tempFile.toFile().delete();

			synchronized (archive) {
				// pack and close the archive
				archive.packAndClose();
				archive = null;
			}

			// trigger quota update
			QuotaManager.getInstance().updateWorkspace( user.getWorkspace() );

			// return all successfully uploaded files
			return buildResponse(200, user).entity(result).build();
			
		} catch (CombineArchiveWebException | CombineArchiveException | ParseException | JDOMException | IOException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Cannot enrich archive {0} in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, MessageFormat.format("Cannot enrich archive {0} in WorkingDir {1}", archiveId, user.getWorkingDir()), e.getMessage() );
		} finally {
			try {
				if( archive != null )
					archive.close();
			}
			catch (IOException e) {
				LOGGER.error(e, "Final closing of archive caused exception");
			}
		}
	}
	
}
