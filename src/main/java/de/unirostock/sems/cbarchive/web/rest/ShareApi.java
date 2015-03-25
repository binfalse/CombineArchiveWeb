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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbarchive.web.importer.GitImporter;
import de.unirostock.sems.cbarchive.web.importer.HgImporter;
import de.unirostock.sems.cbarchive.web.importer.HttpImporter;
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
		File tempFile = null;
		try {
			
			Importer importer = Importer.getImporter(remoteType, remoteUrl, user);
			importer.importRepo();
			
			if( archiveName == null || archiveName.isEmpty() )
				archiveName = importer.getSuggestedName();
			
			// add archive to workspace
			archiveId = user.createArchive( archiveName, tempFile );
			
			importer.close();
			
		} catch (ImporterException e) {
			LOGGER.warn(e, "Cannot import remote archive!");
			return buildTextErrorResponse(400, user, e.getMessage(), "URL: " + remoteUrl);
		} catch (IOException | JDOMException | ParseException
				| CombineArchiveException | TransformerException e) {
			
			LOGGER.error(e, "Cannot read downloaded archive");
			return buildTextErrorResponse(400, user, "Cannot read/parse downloaded archive", e.getMessage(), "URL: " + remoteUrl);
		} finally {
			if( tempFile != null && tempFile.exists() )
				tempFile.delete();
		}
		
		// redirect to workspace
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
		
		return buildResponse(302, user).entity(archiveId + "\n" + archiveName).location(newLocation).build();
	}
	
}
