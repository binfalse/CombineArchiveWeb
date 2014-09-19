package de.unirostock.sems.cbarchive.web.rest;

import java.io.FileNotFoundException;
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
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.VcImporter;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromCellMl;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromExisting;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.provider.ObjectMapperProvider;

@Path("v1")
public class RestApi extends RestHelper {
	
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
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		String result = "ok " + user.getWorkspaceId();
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
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}
		
		// store the settings
		WorkspaceManager.getInstance().storeSettings();
		
		String result = "ok";
		return buildResponse(200, user).entity(result).build();
	}
	
	@GET
	@Path("/workspaces")
	@Produces( MediaType.APPLICATION_JSON )
	public Response getCurrentWorkspaceId( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_WORKSPACE_HISTORY) String historyCookie ) {
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
			
			history.setCurrentWorkspace( user.getWorkspaceId() );
			historyCookie = history.toCookieJson();
			
		} catch (IOException e) {
			LOGGER.error(e, "Error parsing workspace history cookie ", historyCookie);
			return buildErrorResponse(500, user, "Error parsing workspace history cookie " + historyCookie, e.getMessage());
		}
		
		return buildResponse(200, user)
				.cookie( new NewCookie(Fields.COOKIE_WORKSPACE_HISTORY, historyCookie, "/", null, null, Fields.COOKIE_AGE, false) )
				.entity(history)
				.build();
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
			String id = user.createArchive( archive.getName(), user.getData().getVCard() );
			archive.setId(id);
			
			if( archive instanceof ArchiveFromCellMl ) {
				LOGGER.debug( ((ArchiveFromCellMl) archive).getCellmlLink() );
				try
				{
					//Archive arch = user.getArchive (id);
					if (!VcImporter.importRepo ((ArchiveFromCellMl) archive, id, user))
						throw new CombineArchiveWebException ("importing cellml repo failed");
				}
				catch (CombineArchiveWebException e)
				{
					LOGGER.error (e, "cannot create archive");
					return buildErrorResponse( 500, user, "Cannot create archive!", e.getMessage() );
				}
			}
			
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
			// archive is generated from an existing file
			// delegate to the original Endpoint
			return createArchive(userPath, archive, userJson);
		}
		
		// check maximum archives
		if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}
		
		try {
			// check for mime type
			// TODO
			
			// write uploaded file to temp
			// copy the stream to a temp file
			java.nio.file.Path temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, file.getFormDataContentDisposition().getFileName() );
			// write file to disk
			OutputStream output = new FileOutputStream( temp.toFile() );
			InputStream input = file.getEntityAs(InputStream.class);
			long uploadedFileSize = IOUtils.copy( input, output);
			
			output.flush();
			output.close();
			input.close();
			
			// max size for upload
			if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(uploadedFileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
				LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The uploaded file is to big.");
			}
			// max workspace size
			if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(user.getWorkspace().getWorkspaceSize() + uploadedFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
				LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The maximum size of one workspace is reached.");
			}
			// max total size
			if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(WorkspaceManager.getInstance().getTotalSize() + uploadedFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
				LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
				// remove temp file
				temp.toFile().delete();
				return buildErrorResponse(507, user, "The maximum size is reached.");
			}
			
			// creates a existing archive in the working space (check is included)
			String id = user.createArchive( archive.getName(), temp.toFile() );
			archive.setId(id);
			
			// remove temp file
			temp.toFile().delete();
			
			return buildResponse(200, user).entity(archive).build();
			
		} catch (IOException | JDOMException | ParseException | CombineArchiveException | TransformerException e) {
			LOGGER.error(e, MessageFormat.format("Cannot create archive in WorkingDir {0}", user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot create archive!", e.getMessage() );
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
			archive.getArchive().close();

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
			archive.getArchive().close();
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

	@POST
	@Path( "/archives/{archive_id}/entries" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	public Response createArchiveEntry( @PathParam("archive_id") String archiveId, @CookieParam(Fields.COOKIE_PATH) String userPath, @FormDataParam("files[]") List<FormDataBodyPart> files,
			@FormDataParam("path") String path ,@CookieParam(Fields.COOKIE_USER) String userJson ) {
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
		
		try {
			Archive archive = user.getArchive(archiveId);
			List<ArchiveEntryDataholder> result = new LinkedList<ArchiveEntryDataholder>();
			
			// adds ending slash
			if( path == null || path.isEmpty() )
				path = "/";
			else if( !path.endsWith("/") )
				path = path + "/";
			
			for( FormDataBodyPart file : files ) {
				
				// TODO see below
				// check maximum archives
				if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
					LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
					return buildErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
				}
				
				try {
					String fileName = file.getFormDataContentDisposition().getFileName();
					// remove leading slash
					if( fileName.startsWith("/") )
						fileName = fileName.substring(1);
						
					// copy the stream to a temp file
					java.nio.file.Path temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, file.getFormDataContentDisposition().getFileName() );
					// write file to disk
					OutputStream output = new FileOutputStream( temp.toFile() );
					InputStream input = file.getEntityAs(InputStream.class);
					long uploadedFileSize = IOUtils.copy( input, output);
					
					output.flush();
					output.close();
					input.close();
					
					// TODO some mechanism, which does not stop the whole process and returns the already processed files
					
					// max size for upload
					if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(uploadedFileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
						LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						temp.toFile().delete();
						return buildErrorResponse(507, user, "The uploaded file is to big.");
					}
					// max files in one archive
					// TODO
					if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED && Tools.checkQuota(archive.countArchiveEntries() + 1, Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
						LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
						// remove temp file
						temp.toFile().delete();
						return buildErrorResponse(507, user, "The maximum size of one archive is reached.");
					}
					// max archive size
					if( Fields.QUOTA_ARCHIVE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(user.getWorkspace().getArchiveSize(archiveId) + uploadedFileSize, Fields.QUOTA_ARCHIVE_SIZE) == false ) {
						LOGGER.warn("QUOTA_ARCHIVE_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						temp.toFile().delete();
						return buildErrorResponse(507, user, "The maximum size of one archive is reached.");
					}
					// max workspace size
					if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(user.getWorkspace().getWorkspaceSize() + uploadedFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
						LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						temp.toFile().delete();
						return buildErrorResponse(507, user, "The maximum size of one workspace is reached.");
					}
					// max total size
					if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(WorkspaceManager.getInstance().getTotalSize() + uploadedFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
						LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
						// remove temp file
						temp.toFile().delete();
						return buildErrorResponse(507, user, "The maximum size is reached.");
					}
					
					// add the file in the currently selected path
					ArchiveEntry entry = archive.addArchiveEntry(path + fileName, temp);
					
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
			ArchiveEntry archiveEntry = null;
			
			// searching for the old entry by the id
			for( ArchiveEntryDataholder entry : archive.getEntries().values() ) {
				if( entry.getId().equals(entryId) ) {
					archiveEntry = entry.getArchiveEntry();
					break;
				}
			}
			
			if( archiveEntry == null ) {
				return buildErrorResponse(404, user, "Cannot find archive entry"); 
			}
			
			// removes the entry and pack/closes the archive
			try {
				boolean result = combineArchive.removeEntry(archiveEntry);
				combineArchive.pack();
				
				if( result )
					return buildResponse(200, user).entity("ok").build();
				else
					return buildErrorResponse(500, user, "Cannot remove meta description");
			} catch (TransformerException e) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot delete meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				combineArchive.close();
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
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.getArchive().close();
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
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot create meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				archive.getArchive().close();
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
			MetaObjectDataholder metaObject = null;
			for( MetaObjectDataholder iterMetaObject : entry.getMeta() ) {
				if( iterMetaObject.getId().equals(metaId) ) {
					metaObject = iterMetaObject;
					break;
				}
			}
			
			// check if meta entry exists
			if( metaObject == null ) {
				archive.getArchive().close();
				return buildErrorResponse(404, user, "No such meta entry found");
			}
			
			// removes the meta entry and pack/closes the archive
			try {
				boolean result = entry.getArchiveEntry().removeDescription( metaObject.getMetaObject() );
				combineArchive.pack();
				
				if( result )
					return buildResponse(200, user).entity("ok").build();
				else
					return buildErrorResponse(500, user, "Cannot remove meta description");
			} catch (TransformerException e) {
				LOGGER.error(e, MessageFormat.format("Cannot pack archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
				return buildErrorResponse( 500, user, "Cannot delete meta info", "Cannot pack archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
			} finally {
				combineArchive.close();
			}
			
		} catch (CombineArchiveWebException | IOException e) {
			LOGGER.error(e, MessageFormat.format("Cannot read archive {0} entries in WorkingDir {1}", archiveId, user.getWorkingDir()) );
			return buildErrorResponse( 500, user, "Cannot read archive " + archiveId + " entries in WorkingDir " + user.getWorkingDir().toString(), e.getMessage() );
		}
	}
	
}
