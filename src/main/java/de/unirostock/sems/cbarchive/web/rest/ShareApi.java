package de.unirostock.sems.cbarchive.web.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;

@Path( "share" )
public class ShareApi extends RestHelper {
	
	@GET
	@Path("/{user_path}")
	@Produces( MediaType.TEXT_PLAIN )
	public Response setUserPath( @CookieParam(Fields.COOKIE_PATH) String oldUserPath, @PathParam("user_path") String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		String result = "setted " + user.getWorkspaceId();
		URI newLocation = null;
		try {
			newLocation = new URI("../");
		} catch (URISyntaxException e) {
			LOGGER.error(e, "Can not generate relative URL to main app");
			return null;
		}
		
		return buildResponse(302, user).entity(result).location(newLocation).build();
		
	}
	
}
