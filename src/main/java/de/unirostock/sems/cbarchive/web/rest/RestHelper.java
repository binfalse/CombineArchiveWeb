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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Providers;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;

/**
 * Some helping methods for dealing with rest responses
 * 
 * @author martinP
 *
 */
public abstract class RestHelper extends Application {
	
	/**
	 * Access to the provider registered to this applications
	 */
	@Context
	Providers providers;
	
	/**
	 * Generates ResponseBuilder an sets cookies, if possible
	 * 
	 * @param status
	 * @param user
	 * @return ResponseBuilder
	 */
	protected ResponseBuilder buildResponse( int status, UserManager user ) {

		ResponseBuilder builder = Response.status(status);

		if( user != null ) {
			builder = builder.cookie( new NewCookie(Fields.COOKIE_PATH, user.getWorkspaceId(), "/", null, null, Fields.COOKIE_AGE, false) );

			// gets the user data
			UserData data = user.getData();
			if( data != null && data.hasInformation() ) {
				// adds the cookies
				try {
					builder = builder.cookie(
							new NewCookie(Fields.COOKIE_USER, data.toJson(), "/", null, null, Fields.COOKIE_AGE, false) );
				} catch (JsonProcessingException e) {
					LOGGER.error(e, "Unable to set user cookies, due to Json encoding error.");
				}
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
	protected Response buildErrorResponse( int status, UserManager user, String... errors ) {

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
	
	/**
	 * Generates an error response
	 * 
	 * @param status
	 * @param user
	 * @param errors
	 * @return Response
	 */
	protected Response buildTextErrorResponse( int status, UserManager user, String... errors ) {

		// ResponseBuilder builder = Response.status(status);
		ResponseBuilder builder = buildResponse(status, user);

		StringBuilder result = new StringBuilder("An error occured, while processing the request:\n");
		for( String error : errors ) {
			result.append("  * ");
			result.append(error);
			result.append("\n");
		}

		return builder.entity(result.toString()).build();
	}

}
