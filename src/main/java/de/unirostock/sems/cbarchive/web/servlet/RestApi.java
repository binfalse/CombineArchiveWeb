package de.unirostock.sems.cbarchive.web.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
	
}
