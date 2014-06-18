package de.unirostock.sems.cbarchive.web.servlet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import de.unirostock.sems.cbarchive.web.dataholder.Archive;

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
	public String heartbeat() {
		
		return "ok";
	}
	
	@GET
	@Path( "/archives" )
	@Produces( MediaType.APPLICATION_JSON )
	public List<Archive> getAllArchives() {
		List<Archive> response = new LinkedList<Archive>();
		
		
		return response;
	}
	
	@GET
	@Path( "/archives/{archive_id}" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getArchive( @PathParam("archive_id") String id ) {
		
		return Response.status(500).build();
	}
	
	@PUT
	@Path( "/archives/{archive_id}" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchive( @PathParam("archive_id") String id ) {
		
		return Response.status(500).build();
	}
	
	@POST
	@Path( "/archives" )
	//@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchive() {
		
		return Response.status(500).build();
	}
	
}
