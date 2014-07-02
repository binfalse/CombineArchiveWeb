package de.unirostock.sems.cbarchive.web.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;

@Path("v1")
public class RestApi extends Application {

	@GET
	@Path("/test")
	public Response getMsg() {

		String output = "Jersey say hello";

		return Response.status(200).entity(output).build();

	}

	@GET
	@Path("/test/{param}")
	@Produces( MediaType.APPLICATION_JSON )
	public List<String> getMsg(@PathParam("param") String param) {
		List<String> resp = new ArrayList<String>();
		//String output = "Jersey say hello: " + param;
		resp.add("Jersey say hello!");
		for( int x = 0; x < 10; x++ ) {
			resp.add( param + String.valueOf(x) );
		}

		//return Response.status(200).entity(output).build();
		return resp;
	}

	@GET
	@Path("/heartbeat")
	@Produces( MediaType.TEXT_PLAIN )
	public Response heartbeat( @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		String result = "ok " + user.getPath();
		return buildResponse(200, user).entity(result).build();
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		String result = "setted " + user.getPath();
		return buildResponse(200, user).entity(result).build();
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		// gets the list
		List<Archive> response = user.getArchives(false);
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		// gets the archive
		try {
			Archive response = user.getArchive(id);
			// build response
			return buildResponse(200, user).entity(response).build();
		} catch (FileNotFoundException | CombineArchiveWebException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} in WorkingDir {1}", id, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive!", e.getMessage() );
		}
	}

	@PUT
	@Path( "/archives/{archive_id}" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchive( @PathParam("archive_id") String id, @CookieParam(Fields.COOKIE_PATH) String userPath ) {

		return Response.status(500).build();
	}

	@POST
	@Path( "/archives" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchive( @CookieParam(Fields.COOKIE_PATH) String userPath ) {

		return Response.status(500).build();
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			Archive archive = user.getArchive(archiveId);
			archive.getArchive().close();
			return buildResponse(200, user).entity(archive.getEntries().values()).build();
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			Archive archive = user.getArchive(archiveId);
			archive.getArchive().close();
			ArchiveEntryDataholder entry = archive.getEntries().get(entryId);
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity(entry).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
		}
	}

	@PUT
	@Path( "/archives/{archive_id}/entries/{entry_id}" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchiveEntry( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {

		return Response.status(500).build();
	}

	@POST
	@Path( "/archives/{archive_id}/entries" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchiveEntry( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {

		return Response.status(500).build();
	}

	// --------------------------------------------------------------------------------
	// helper functions

	/**
	 * Generates ResponseBuilder an sets cookies, if possible
	 * 
	 * @param status
	 * @param user
	 * @return ResponseBuilder
	 */
	private ResponseBuilder buildResponse( int status, UserManager user ) {

		ResponseBuilder builder = Response.status(status);

		if( user != null ) {
			builder = builder.cookie( new NewCookie(Fields.COOKIE_PATH, user.getPath(), "/", null, null, Fields.COOKIE_AGE, false) );

			// gets the user data
			UserData data = user.getData();
			if( data != null && data.hasInformation() ) {
				// adds the cookies
				builder = builder.cookie(
						new NewCookie(Fields.COOKIE_FAMILY_NAME, data.getFamilyName(), "/", null, null, Fields.COOKIE_AGE, false),
						new NewCookie(Fields.COOKIE_GIVEN_NAME, data.getGivenName(), "/", null, null, Fields.COOKIE_AGE, false),
						new NewCookie(Fields.COOKIE_MAIL, data.getMail(), "/", null, null, Fields.COOKIE_AGE, false),
						new NewCookie(Fields.COOKIE_ORG, data.getOrganization(), "/", null, null, Fields.COOKIE_AGE, false)
						);
			}
		}

		return builder;
	}

	/**
	 * Generates an error response
	 * 
	 * @param status
	 * @param user
	 * @param errors
	 * @return Response
	 */
	private Response buildErrorResponse( int status, UserManager user, String... errors ) {

		// ResponseBuilder builder = Response.status(status);
		ResponseBuilder builder = buildResponse(status, user);

		Map<String, Object> result = new HashMap<String, Object>();
		List<String> errorList = new LinkedList<String>();

		for( String error : errors ) {
			errorList.add( error );
		}

		result.put("status", "error");
		result.put("errors", errorList);

		return builder.entity(result).build();
	}

}
