package de.unirostock.sems.cbarchive.web.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
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

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.HttpImporter;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;

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
			
			if( history.getRecentWorkspaces().containsKey(user.getWorkspaceId()) == false )
				history.getRecentWorkspaces().put( user.getWorkspaceId(), user.getWorkspace().getName() );
			
			if( oldUserPath != null && !oldUserPath.isEmpty() && history.getRecentWorkspaces().containsKey( oldUserPath ) == false ) {
				Workspace workspace = WorkspaceManager.getInstance().getWorkspace(oldUserPath);
				if( workspace != null )
					history.getRecentWorkspaces().put( oldUserPath, workspace.getName() );
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
	public Response downloadRemoteArchive(@CookieParam(Fields.COOKIE_PATH) String userPath, @Context HttpServletRequest requestContext, @QueryParam("remote") String remoteUrl, @QueryParam("name") String archiveName) {
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
		
		HttpImporter importer = null;
		String archiveId = null;
		try {
			importer = new HttpImporter(remoteUrl, user);
			importer.importRepo();
			
			if( archiveName == null || archiveName.isEmpty() )
				archiveName = importer.getSuggestedName();
			
			archiveId = user.createArchive( archiveName, importer.getTempFile() );
			
		} catch (ImporterException e) {
			return buildTextErrorResponse(400, user, e.getMessage(), "URL: " + remoteUrl);
		}  catch (IOException | JDOMException | ParseException
				| CombineArchiveException | TransformerException e) {
			
			LOGGER.error(e, "Can not read downloaded archive");
			return buildTextErrorResponse(400, user, "Can not read/parse downloaded archive", e.getMessage(), "URL: " + remoteUrl);
		} finally {
			if( importer.getTempFile() != null)
				importer.getTempFile().delete();
		}
		
		// redirect to workspace
		URI newLocation = null;
		try {
			if( requestContext != null ) {
				String uri = requestContext.getRequestURL().toString();
				uri = uri.substring(0, uri.indexOf("rest/"));
				LOGGER.info("redirect to ", requestContext.getRequestURL(), " to ", uri);
				newLocation = new URI( uri );
			}
			else
				newLocation = new URI("../");
			
		} catch (URISyntaxException e) {
			LOGGER.error(e, "Cannot generate relative URL to main app");
			return null;
		}
		
		return buildResponse(302, user).entity(archiveId + "\n" + archiveName).location(newLocation).build();
	}
	
}
