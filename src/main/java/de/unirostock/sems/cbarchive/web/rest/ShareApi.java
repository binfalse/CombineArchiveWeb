package de.unirostock.sems.cbarchive.web.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;

@Path( "share" )
public class ShareApi extends RestHelper {
	
	@GET
	@Path("/{user_path}")
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
	
}
