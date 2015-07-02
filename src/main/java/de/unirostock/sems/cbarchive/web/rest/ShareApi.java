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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.QuotaManager;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.Archive.ReplaceStrategy;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.ImportRequest;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.OmexMetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.dataholder.WorkspaceHistory;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;
import de.unirostock.sems.cbarchive.web.importer.Importer;
import de.unirostock.sems.cbarchive.web.provider.ObjectMapperProvider;

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

		LOGGER.info("got import request for: ", remoteUrl);

		if( remoteUrl == null || remoteUrl.isEmpty() ) {
			LOGGER.warn("empty remote url provided");
			return buildTextErrorResponse(400, user, "empty remote url provided");
		}

		// check maximum archives
		if( Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildTextErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}

		if( remoteType == null || remoteType.isEmpty() )
			remoteType = Importer.IMPORT_HTTP;
		else
			remoteType = remoteType.toLowerCase();


		String archiveId = null;
		Importer importer = null;
		try {

			importer = Importer.getImporter(remoteType, remoteUrl, user);
			importer.importRepo();

			if( archiveName == null || archiveName.isEmpty() )
				archiveName = importer.getSuggestedName();

			// add archive to workspace
			archiveId = user.createArchive( archiveName, importer.getTempFile() );

		} catch (ImporterException e) {
			LOGGER.warn(e, "Cannot import remote archive!");
			return buildTextErrorResponse(400, user, e.getMessage(), "URL: " + remoteUrl);
		} catch (IOException | JDOMException | ParseException
				| CombineArchiveException | TransformerException e) {

			LOGGER.error(e, "Cannot read downloaded archive");
			return buildTextErrorResponse(400, user, "Cannot read/parse downloaded archive", e.getMessage(), "URL: " + remoteUrl);
		} finally {
			if( importer != null && importer.getTempFile() != null && importer.getTempFile().exists() )
				importer.getTempFile().delete();

			importer.close();
		}
		
		// redirect to workspace
		ImportRequest req = new ImportRequest();
		req.setArchiveName(archiveName);
		return buildSuccesResponse(user, req, archiveId, requestContext);
	}

	@POST
	@Path("/import")
	@Produces( MediaType.TEXT_PLAIN )
	@Consumes( MediaType.APPLICATION_JSON )
	public Response importRemoteArchive( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson, ImportRequest request, @Context HttpServletRequest requestContext ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildTextErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		if( request == null || request.isValid() == false )
			return buildTextErrorResponse(400, user, "import request is not set properly");

		// check maximum archives
		if( Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildTextErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}

		String archiveId = null;
		try {
			archiveId = importOrCreateArchive(user, request, null);
		} catch (ImporterException e) {
			return buildTextErrorResponse(400, user, e.getMessage(), e.getCause().getMessage());
		}

		try ( Archive archive = user.getArchive(archiveId) ) {
			// set own VCard
			if( request.isOwnVCard() )
				setOwnVCard(user, request, archive);

			try {
				// import additional files
				if( request.getAdditionalFiles() != null && request.getAdditionalFiles().size() > 0 ) {
					addAdditionalFiles(user, request, archive, null);
				}
			}
			catch (ImporterException e) {
				// catch quota-exceeding-exception (Logging already done)
				user.deleteArchiveSilent(archiveId);
				return buildTextErrorResponse(507, user, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : null );
			}

			archive.getArchive().pack();

		} catch (IOException | CombineArchiveWebException e) {
			LOGGER.error(e, "Cannot open/pack newly created archive");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Cannot open newly created archive: ", e.getMessage() );
		} catch (ImporterException e) {
			LOGGER.error(e, "Something went wrong with the extended import");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Error while applying additional data to the archive");
		} catch (TransformerException e) {
			LOGGER.error(e, "Something went wrong while packing the archive");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Something went wrong while packing the archive");
		}
		
		
		// redirect to workspace
		return buildSuccesResponse(user, request, archiveId, requestContext);
	}

	@POST
	@Path( "/import" )
	@Produces( MediaType.TEXT_PLAIN )
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	public Response uploadArchive( @CookieParam(Fields.COOKIE_PATH) String userPath, @CookieParam(Fields.COOKIE_USER) String userJson, @Context HttpServletRequest requestContext,
			@FormDataParam("request") String serializedRequest, @FormDataParam("archive") FormDataBodyPart archiveFile, @FormDataParam("additionalFile") List<FormDataBodyPart> additionalFiles ) {
		// user stuff
		UserManager user = null;
		try {
			user = new UserManager( userPath );
			if( userJson != null && !userJson.isEmpty() )
				user.setData( UserData.fromJson(userJson) );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create user");
			return buildTextErrorResponse(500, null, "user not creatable!", e.getMessage() );
		}

		ImportRequest request = null;
		ObjectMapper mapper = null;
		try {

			mapper = ((ObjectMapperProvider) providers.getContextResolver(ObjectMapper.class, MediaType.WILDCARD_TYPE)).getContext( null );
			request = mapper.readValue(serializedRequest, ImportRequest.class);

			if( request == null ) {
				LOGGER.error("Cannot deserialize import request");
				return buildTextErrorResponse(500, user, "Cannot deserialize import request");
			}
			else if( request.isValid() == false )
				return buildTextErrorResponse(400, user, "import request is not set properly");

		} catch (IOException e) {
			LOGGER.error(e, "Cannot deserialize import request");
			return buildTextErrorResponse(500, user, "Cannot deserialize import request", e.getMessage());
		}

		// check maximum archives
		if( Tools.checkQuota( user.getWorkspace().getArchives().size(), Fields.QUOTA_ARCHIVE_LIMIT) == false ) {
			LOGGER.warn("QUOTA_ARCHIVE_LIMIT reached in workspace ", user.getWorkspaceId());
			return buildTextErrorResponse(507, user, "Maximum number of archives in one workspace reached!");
		}
		
		String archiveId = null;
		try {
			archiveId = importOrCreateArchive(user, request, archiveFile);
		} catch (ImporterException e) {
			return buildTextErrorResponse(400, user, e.getMessage(), e.getCause().getMessage());
		}

		try ( Archive archive = user.getArchive(archiveId) ) {
			// set own VCard
			if( request.isOwnVCard() )
				setOwnVCard(user, request, archive);

			try {
				// import additional files
				if( request.getAdditionalFiles() != null && request.getAdditionalFiles().size() > 0 ) {
					addAdditionalFiles(user, request, archive, additionalFiles);
				}
			}
			catch (ImporterException e) {
				// catch quota-exceeding-exception (Logging already done)
				user.deleteArchiveSilent(archiveId);
				return buildTextErrorResponse(507, user, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : null );
			}

			archive.getArchive().pack();

		} catch (IOException | CombineArchiveWebException e) {
			LOGGER.error(e, "Cannot open/pack newly created archive");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Cannot open newly created archive: ", e.getMessage() );
		} catch (ImporterException e) {
			LOGGER.error(e, "Something went wrong with the extended import");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Error while applying additional data to the archive");
		} catch (TransformerException e) {
			LOGGER.error(e, "Something went wrong while packing the archive");
			user.deleteArchiveSilent(archiveId);
			return buildTextErrorResponse(500, user, "Something went wrong while packing the archive");
		}
		
		// redirect to workspace
		return buildSuccesResponse(user, request, archiveId, requestContext);
	}

	private String importOrCreateArchive( UserManager user, ImportRequest request, FormDataBodyPart archiveFile ) throws ImporterException {
		
		String archiveId = null;
		
		if( archiveFile != null ) {
			// import via post data
			java.nio.file.Path temp = null;
			try {
				// write uploaded file to temp
				// copy the stream to a temp file
				temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, archiveFile.getFormDataContentDisposition().getFileName() );
				// write file to disk
				OutputStream output = new FileOutputStream( temp.toFile() );
				InputStream input = archiveFile.getEntityAs(InputStream.class);
				long uploadedFileSize = IOUtils.copy( input, output);
				
				output.flush();
				output.close();
				input.close();
				
				// quota stuff
				// max size for upload
				if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(uploadedFileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
					LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The new archive is to big.");
				}
				// max workspace size
				if( user != null && Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + uploadedFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
					LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The maximum size of the workspace is reached, while importing new archive.");
				}
				// max total size
				if( user != null && Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + uploadedFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
					LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The maximum size is reached, while importing new archive.");
				}
				
				// set default name, if necessary
				if( request.getArchiveName() == null || request.getArchiveName().isEmpty() )
					request.setArchiveName( Fields.NEW_ARCHIVE_NAME );
				
				archiveId = user.createArchive( request.getArchiveName(), temp.toFile());
			} catch (IOException e) {
				
			} catch (JDOMException | ParseException | CombineArchiveException | TransformerException e) {
				
			} finally {
				if( temp != null && temp.toFile().exists() )
					temp.toFile().delete();
			}
		}
		else if( request.isArchiveImport() ) {
			// try to import (if requested)
			Importer importer = null;
			try {

				importer = Importer.getImporter(request.getType(), request.getRemoteUrl(), user);
				importer.importRepo();

				if( request.getArchiveName() == null || request.getArchiveName().isEmpty() )
					request.setArchiveName( importer.getSuggestedName() );

				// add archive to workspace
				archiveId = user.createArchive( request.getArchiveName(), importer.getTempFile() );

			} catch (ImporterException e) {
				LOGGER.warn(e, "Cannot import remote archive!");
				throw new ImporterException("Cannot import remote archive", e);
			} catch (IOException | JDOMException | ParseException
					| CombineArchiveException | TransformerException e) {

				LOGGER.error(e, "Cannot read downloaded archive");
				throw new ImporterException("Cannot read downlaoded archive. URL: " + request.getRemoteUrl(), e);
			} finally {
				if( importer != null && importer.getTempFile() != null && importer.getTempFile().exists() )
					importer.getTempFile().delete();

				importer.close();
			}
		}
		else {
			// just create an empty archive

			// set default name, if necessary
			if( request.getArchiveName() == null || request.getArchiveName().isEmpty() )
				request.setArchiveName( Fields.NEW_ARCHIVE_NAME );

			try {
				archiveId = user.createArchive( request.getArchiveName() );
			} catch (IOException | JDOMException | ParseException
					| CombineArchiveException | TransformerException e) {

				LOGGER.error(e, "Cannot create new archive");
				throw new ImporterException("Cannot create new archive.", e);
			}
		}
		
		return archiveId;

	}

	private void setOwnVCard( UserManager user, ImportRequest request, Archive archive ) throws ImporterException {

		VCard vcard = request.getVcard();

		// check if any VCard info is available
		// (either from cookies of from the request it self)
		if( vcard == null ) {
			if( user.getData() != null ) 
				vcard = user.getData().getVCard();
			else 
				throw new ImporterException("No vcard information provided for archive annotation");
		}

		// get root element
		ArchiveEntryDataholder root = archive.getEntries().get("/");
		if( root != null ) {
			// look for existing omex meta information
			OmexMetaObjectDataholder omexMeta = null;
			for( MetaObjectDataholder meta : root.getMeta() )
				if( meta instanceof OmexMetaObjectDataholder ) {
					omexMeta = (OmexMetaObjectDataholder) meta;
					break;
				}

			if( omexMeta != null ) {
				// if there is already an omex entry,
				// just add our VCard and add a modified date
				omexMeta.getCreators().add(vcard);
				omexMeta.getModified().add( new Date() );
			}
			else {
				// create a new dataholder and fill it with some information
				omexMeta = new OmexMetaObjectDataholder();
				omexMeta.setCreators( new ArrayList<VCard>(1) );
				omexMeta.setModified( new ArrayList<Date>(1) );
				omexMeta.setCreated( new Date() );

				omexMeta.getCreators().add(vcard);
				omexMeta.getModified().add( new Date() );

				root.addMetaEntry(omexMeta);
			}
		}
	}

	private void addAdditionalFiles( UserManager user, ImportRequest request, Archive archive, List<FormDataBodyPart> uploadedFiles ) throws ImporterException {

		for( ImportRequest.AdditionalFile addFile : request.getAdditionalFiles() ) {
			java.nio.file.Path temp = null;
			try {
				URI remoteUri = new URI( addFile.getRemoteUrl() );

				// copy the stream to a temp file
				temp = Files.createTempFile( Fields.TEMP_FILE_PREFIX, FilenameUtils.getBaseName(remoteUri.toString()) );
				// write file to disk
				OutputStream output = new FileOutputStream( temp.toFile() );
				InputStream input = null;
				
				String protocol = remoteUri.getScheme().toLowerCase();
				if( protocol.equals("http") || protocol.equals("https") ) {
					input = remoteUri.toURL().openStream();
				}
				else if( protocol.equals("post") && uploadedFiles != null && uploadedFiles.size() > 0 ) {
					// use a file from the post
					String fileName = remoteUri.getAuthority();
					for( FormDataBodyPart file : uploadedFiles ) {
						if( file.getFormDataContentDisposition().getFileName().equals(fileName) ) {
							input = file.getEntityAs(InputStream.class);
							break;
						}
					}
				}
				else
					throw new ImporterException("Unknown protocol " + protocol + " while adding "  + remoteUri.toString());
				
				if( input == null )
					throw new ImporterException("Cannot open stream to import file: " + remoteUri.toString());
				
				long downloadedFileSize = IOUtils.copy( input, output );

				output.flush();
				output.close();
				input.close();

				// quota stuff
				// max size for upload
				if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(downloadedFileSize, Fields.QUOTA_UPLOAD_SIZE) == false ) {
					LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The additional file is to big: " + addFile.getRemoteUrl());
				}
				// max archive size
				if( user != null && Fields.QUOTA_ARCHIVE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(user.getWorkspace().getArchiveSize(archive.getId()) + downloadedFileSize, Fields.QUOTA_ARCHIVE_SIZE) == false ) {
					LOGGER.warn("QUOTA_ARCHIVE_SIZE reached in workspace ", user.getWorkspaceId(), " while trying to adv import archive");
					throw new ImporterException("The maximum size of the archive is reached, while adding " + addFile.getRemoteUrl());
				}
				// max workspace size
				if( user != null && Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + downloadedFileSize, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
					LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The maximum size of the workspace is reached, while adding " + addFile.getRemoteUrl());
				}
				// max total size
				if( user != null && Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + downloadedFileSize, Fields.QUOTA_TOTAL_SIZE) == false ) {
					LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
					throw new ImporterException("The maximum size is reached, while adding " + addFile.getRemoteUrl());
				}

				String path = addFile.getArchivePath();
				if( path == null || path.isEmpty() )
					path = FilenameUtils.getBaseName( remoteUri.toString() );
				// remove leading slash
				if( path.startsWith("/") )
					path = path.substring(1);

				// add it
				ArchiveEntry entry = archive.addArchiveEntry(path, temp, ReplaceStrategy.RENAME);

				// set file format uri
				if( addFile.getFileFormat() != null )
					entry.setFormat( addFile.getFileFormat() );

				// add all meta data objects
				if( addFile.getMetaData() != null )
					for( MetaObjectDataholder meta : addFile.getMetaData() ) {
						entry.addDescription( meta.getCombineArchiveMetaObject() );
					}

			} catch (URISyntaxException e) {
				LOGGER.error(e, "Wrong defined remoteUrl");
				throw new ImporterException("Cannot parse remote URL: " + addFile.getRemoteUrl(), e);
			} catch(IOException | CombineArchiveWebException e) {
				LOGGER.error(e, "Cannot download an additional file. ", addFile.getRemoteUrl());
				throw new ImporterException("Cannot download and add an additional file: " + addFile.getRemoteUrl(), e);
			} finally {
				if( temp != null && temp.toFile().exists() )
					temp.toFile().delete();
			}
		}

	}
	
	private Response buildSuccesResponse( UserManager user, ImportRequest request, String archiveId, HttpServletRequest requestContext ) {
		
		Map<String, String> response = new HashMap<String, String>();
		response.put( "workspaceId", user.getWorkspaceId() );
		response.put( "shareLink", Tools.generateWorkspaceRedirectUri(requestContext, user.getWorkspaceId()).toString() );
		response.put( "archiveId", archiveId );
		response.put( "archiveName", request.getArchiveName() );
		
		StringBuilder respString = new StringBuilder();
		for( String key : response.keySet() ) {
			respString.append(key);
			respString.append(": ");
			respString.append( response.get(key) );
			respString.append("\n");
		}
		
		return buildResponse(302, user).entity( respString.toString() ).location( Tools.generateArchiveRedirectUri(requestContext, archiveId) ).build();
	}

}
