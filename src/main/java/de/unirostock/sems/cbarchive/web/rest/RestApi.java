package de.unirostock.sems.cbarchive.web.rest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;

@Path("v1")
public class RestApi extends Application {

	@GET
	@Path("/heartbeat")
	@Produces( MediaType.TEXT_PLAIN )
	public Response heartbeat( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
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
			LOGGER.error(e, "Can not create user");
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( data.hasInformation() ) {
			user.setData(data);
			return buildResponse(200, user).entity(user.getData()).build();
		}
		else
			return buildErrorResponse(400, user, "insufficient user information");
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
			Archive response = user.getArchive(id, false);
			// build response
			return buildResponse(200, user).entity(response).build();
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} in WorkingDir {1}", id, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive!", e.getMessage() );
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
			LOGGER.error(e, "Can not create user");
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
			LOGGER.error(e, MessageFormat.format("Can not rename archive {0} to {3} in WorkingDir {1}", id, user.getWorkingDir(), archive.getName()) );
			return buildErrorResponse( 500, user, "Can not rename archive!", e.getMessage() );
		}
	}

	@POST
	@Path( "/archives" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response createArchive( @CookieParam(Fields.COOKIE_PATH) String userPath, Archive archive ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		if( archive == null ) {
			LOGGER.error("create archive not possible if archive == null");
			return buildErrorResponse(400, null, "no archive was transmitted" );
		}
		
		try {
			String id = user.createArchive( archive.getName() );
			archive.setId(id);
			return buildResponse(200, user).entity(archive).build();
		} catch (IOException | JDOMException | ParseException | CombineArchiveException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Can not create archive in WorkingDir {0}", user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not create archive!", e.getMessage() );
		}
			
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

			// gets result and sorts it
			List<ArchiveEntryDataholder> result = new ArrayList<ArchiveEntryDataholder>( archive.getEntries().values() );
			Collections.sort(result, new Comparator<ArchiveEntryDataholder>() {
				@Override
				public int compare(ArchiveEntryDataholder o1, ArchiveEntryDataholder o2) {
					return o2.getFileName().compareTo(o1.getFileName());
				}
			});
			
			return buildResponse(200, user).entity(result).build();
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
			ArchiveEntryDataholder entry = null;
			for( ArchiveEntryDataholder iterEntry : archive.getEntries().values() ) {
				if( iterEntry.getId().equals(entryId) ) {
					entry = iterEntry;
					break;
				}
			}
			
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
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response updateArchiveEntry( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, ArchiveEntryDataholder newEntry, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
		} catch (IOException e) {
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		try {
			user.updateArchiveEntry(archiveId, newEntry);
			
			Archive archive = user.getArchive(archiveId);
			archive.getArchive().close();
			ArchiveEntryDataholder entry = archive.getEntries().get( newEntry.getFilePath() );
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity(entry).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Can not update archive entry {0}/{1} in WorkingDir {2}", archiveId, newEntry.getFilePath(), user.getWorkingDir()) );
			return buildErrorResponse( 500, user, MessageFormat.format("Can not update archive entry {0}/{1} in WorkingDir {2}", archiveId, newEntry.getFilePath(), user.getWorkingDir()), e.getMessage() );
		}
	}

	@POST
	@Path( "/archives/{archive_id}/entries" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	public Response createArchiveEntry( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath, @FormDataParam("files[]") List<FormDataBodyPart> files,
			@CookieParam(Fields.COOKIE_USER) String userJson ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			Archive archive = user.getArchive(archiveId);
			List<ArchiveEntryDataholder> result = new LinkedList<ArchiveEntryDataholder>();
			
			for( FormDataBodyPart file : files ) {
				try {
					String fileName = file.getFormDataContentDisposition().getFileName();
//					if( !fileName.startsWith("/") )
//						fileName = "/" + fileName;
						
					// copy the stream to a temp file
					java.nio.file.Path temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, file.getFormDataContentDisposition().getFileName() );
					// write file to disk
					OutputStream output = new FileOutputStream( temp.toFile() );
					InputStream input = file.getEntityAs(InputStream.class);
					IOUtils.copy( input, output);
					
					output.flush();
					output.close();
					input.close();
					
					// add the file
					ArchiveEntry entry = archive.addArchiveEntry(fileName, temp);
					
					// add default meta information
					if( user.getData() != null && user.getData().hasInformation() == true ) {
						OmexDescription metaData = new OmexDescription(user.getData().getVCard(), new Date());
						entry.addDescription( new OmexMetaDataObject(metaData) );
					}
						
					LOGGER.info(MessageFormat.format("Successfully added file {0} to archive {1}", fileName, archiveId));
					
					// clean up
					temp.toFile().delete();
					// add to result list
					result.add( new ArchiveEntryDataholder(entry) );
				}
				catch (IOException e) {
					// TODO ???
					LOGGER.error(e, MessageFormat.format("Error while uploading/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir() ));
				}
				
			}
			
			// pack and close the archive
			archive.getArchive().pack();
			archive.getArchive().close();
			
			// return all successfully uploaded files
			return buildResponse(200, user).entity(result).build();
			
		} catch (CombineArchiveWebException | IOException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Error while uploading/adding file to archive {0} in Workspace {1}", archiveId, user.getWorkingDir() ));
			return buildErrorResponse(500, user, "Error while uploading file: " + e.getMessage() );
		}
		
	}
	
	// --------------------------------------------------------------------------------
	// Meta Object Endpoints
	
	// TODO Endpoints for meta entries!
	
	@GET
	@Path( "/archives/{archive_id}/entries/{entry_id}/meta" )
	@Produces( MediaType.APPLICATION_JSON )
	public Response getAllMetaObjects( @PathParam("archive_id") String archiveId, @PathParam("entry_id") String entryId, @CookieParam(Fields.COOKIE_PATH) String userPath ) {
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
			ArchiveEntryDataholder entry = null;
			for( ArchiveEntryDataholder iterEntry : archive.getEntries().values() ) {
				if( iterEntry.getId().equals(entryId) ) {
					entry = iterEntry;
					break;
				}
			}
			
			// check if entry exists
			if( entry != null )
				return buildResponse(200, user).entity( entry.getMeta() ).build();
			else
				return buildErrorResponse(404, user, "No such entry found");
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
				
		try {
			Archive archive = user.getArchive(archiveId);
			archive.getArchive().close();
			
			// iterate over all archive entries
			ArchiveEntryDataholder entry = null;
			for( ArchiveEntryDataholder iterEntry : archive.getEntries().values() ) {
				if( iterEntry.getId().equals(entryId) ) {
					entry = iterEntry;
					break;
				}
			}
			
			// check if entry exists
			if( entry == null )
				return buildErrorResponse(404, user, "No such entry found");
			
			// iterate over all meta entries
			MetaObjectDataholder metaObject = null;
			for( MetaObjectDataholder iterMetaObject : entry.getMeta() ) {
				if( iterMetaObject.getId().equals(metaId) ) {
					metaObject = iterMetaObject;
					break;
				}
			}
			
			// check if meta entry exists
			if( metaObject != null )
				return buildResponse(200, user).entity( metaObject ).build();
			else
				return buildErrorResponse(404, user, "No such meta entry found");
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		try {
			Archive archive = user.getArchive(archiveId);
			
			// iterate over all archive entries
			ArchiveEntryDataholder entry = null;
			for( ArchiveEntryDataholder iterEntry : archive.getEntries().values() ) {
				if( iterEntry.getId().equals(entryId) ) {
					entry = iterEntry;
					break;
				}
			}
			
			// check if entry exists
			if( entry == null ) {
				archive.getArchive().close();
				return buildErrorResponse(404, user, "No such entry found");
			}
			
			// iterate over all meta entries
			MetaObjectDataholder oldMetaObject = null;
			for( MetaObjectDataholder iterMetaObject : entry.getMeta() ) {
				if( iterMetaObject.getId().equals(metaId) ) {
					oldMetaObject = iterMetaObject;
					break;
				}
			}
			
			metaId = metaObject.getId();
			
			// check if meta entry exists
			if( oldMetaObject == null ) {
				archive.getArchive().close();
				return buildErrorResponse(404, user, "No such meta entry found");
			}
			
			// update and pack the archive
			try {
				// update the entry
				oldMetaObject.update( metaObject );
				archive.getArchive().pack();
				// force to re-generate the id, after the pack
				oldMetaObject.generateId();
			} catch( IOException | TransformerException e ) {
				LOGGER.error(e, MessageFormat.format("Can not pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Can not pack archive {0} entries in WorkingDir {1}", e.getMessage() );
			} finally {
				archive.getArchive().close();
			}
			
			return buildResponse(200, user).entity( oldMetaObject ).build();
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
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
			LOGGER.error(e, "Can not create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
				
		try {
			Archive archive = user.getArchive(archiveId);
			
			// iterate over all archive entries
			ArchiveEntryDataholder entry = null;
			for( ArchiveEntryDataholder iterEntry : archive.getEntries().values() ) {
				if( iterEntry.getId().equals(entryId) ) {
					entry = iterEntry;
					break;
				}
			}
			
			// check if entry exists
			if( entry == null ) {
				archive.getArchive().close();
				return buildErrorResponse(404, user, "No such entry found");
			}
			
			// add the description to the entry and pack the archive
			try {
				entry.addMetaEntry( metaObject );
				archive.getArchive().pack();
				// force to re-generate the id, after the pack
				metaObject.generateId();
			} catch( IOException | TransformerException e ) {
				LOGGER.error(e, MessageFormat.format("Can not pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Can not pack archive {0} entries in WorkingDir {1}", e.getMessage() );
			} finally {
				archive.getArchive().close();
			}
			
			return buildResponse(200, user).entity( metaObject ).build();
				
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Can not read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Can not read archive {0} entries in WorkingDir {1}", e.getMessage() );
		}
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
