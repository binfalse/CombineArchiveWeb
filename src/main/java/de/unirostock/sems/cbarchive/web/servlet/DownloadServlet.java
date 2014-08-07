package de.unirostock.sems.cbarchive.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;

public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 7562736436930714565L;
	
	public static final String COMBINEARCHIVE_FILE_EXT = "omex";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// init logger
		LOGGER.setMinLevel (LOGGER.DEBUG);
		LOGGER.setLogStackTrace (true);
		
		// set charset
		response.setCharacterEncoding ("UTF-8");
		request.setCharacterEncoding ("UTF-8");
		
		
		// login stuff
		UserManager user = null;
		try {
			user = Tools.doLogin(request, response);
		} catch (CombineArchiveWebCriticalException e) {
			LOGGER.error(e, "Exception while getting User");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} catch (CombineArchiveWebException e) {
			LOGGER.warn(e, "Exception while getting User");
			response.setStatus (HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		
		// splitting request URL
		String[] requestUrl =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		// check entry points
		if( requestUrl.length >= 4 && requestUrl[2].equals("archive") ) {
			// request to download an archive from the workspace
			if( requestUrl[3] != null && !requestUrl[3].isEmpty() )
				downloadArchive(request, response, user, requestUrl[3] );
		}
		else if( requestUrl.length >= 5 && requestUrl[2].equals("file") ) {
			
			String archive = null;
			String file = null;
			
			if( requestUrl[3] != null && !requestUrl[3].isEmpty() )
				archive = requestUrl[3];
			else
				return;
			
			StringBuilder filePath = new StringBuilder();
			for( int i = 4; i < requestUrl.length; i++ ) {
				
				if( requestUrl[i] != null && !requestUrl[i].isEmpty() ) {
					filePath.append("/");
					filePath.append( requestUrl[i] );
				}
			}
			file = filePath.toString();
			
			if( archive != null && !archive.isEmpty() && file != null && !file.isEmpty() )
				downloadFile(request, response, user, archive, file);
			
		}
		
	}
	
	private void downloadArchive(HttpServletRequest request, HttpServletResponse response, UserManager user, String archive) throws IOException {
		
		// filters for omex extension
		//		( is just there for the browser to name the downloaded file correctly)
		if( archive.endsWith( "." + COMBINEARCHIVE_FILE_EXT ) )
			// just removes the file extension -> we get the archiveId
			archive = archive.substring(0, archive.length() - (COMBINEARCHIVE_FILE_EXT.length() + 1) );
		
		File archiveFile = null;
		String archiveName = null;
		try {
			archiveFile = user.getArchiveFile(archive);
			archiveName = user.getArchive(archive, false).getName();
		} catch (FileNotFoundException | CombineArchiveWebException e) {
			LOGGER.warn(e, MessageFormat.format("FileNotFound Exception, while handling donwload request for Archive {1} in Workspace {0}", user.getWorkingDir(), archive) );
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage() );
			return;
		}
		
		// set MIME-Type to something downloadable
		response.setContentType("application/octet-stream");
		
		// set the filename of the downloaded file
		response.addHeader("Content-Disposition", 
				MessageFormat.format("inline; filename=\"{0}.{1}\"", archiveName, COMBINEARCHIVE_FILE_EXT) ); 
		
		// print the file to the output stream
		try {
			OutputStream output = response.getOutputStream();
			InputStream input = new FileInputStream(archiveFile);
			
			// copy the streams
			IOUtils.copy(input, output);
			
			// flush'n'close
			output.flush();
			output.close();
			input.close();
			
			response.flushBuffer();
		} catch (IOException e) {
			LOGGER.error(e,  MessageFormat.format("IOException, while copying streams by handling donwload request for Archive {1} in Workspace {0}", user.getWorkingDir(), archive));
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "IOException while sending the file.");
		}
	}
	
	private void downloadFile(HttpServletRequest request, HttpServletResponse response, UserManager user, String archiveId, String filePath) throws IOException {
		
		Archive archive = null;
		CombineArchive combineArchive = null;
		try {
			archive = user.getArchive(archiveId, true);
			combineArchive = archive.getArchive();
		} catch (FileNotFoundException | CombineArchiveWebException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// check if file exits in the archive
		ArchiveEntry entry = combineArchive.getEntry(filePath);
		if( entry == null ) {
			// TODO
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found in archive." );
			return;
		}
		
		// set the mime type of the response
		response.setContentType( entry.getFormat() );
		
		// set the filename of the response
		response.addHeader("Content-Disposition", 
				MessageFormat.format("inline; filename=\"{0}\"", entry.getFileName()) );
		
		// extract the file
		try {
			File tempFile = File.createTempFile( Fields.TEMP_FILE_PREFIX, entry.getFileName() );
			entry.extractFile(tempFile);
			
			OutputStream output = response.getOutputStream();
			InputStream input = new FileInputStream(tempFile);
			
			// copy the streams
			IOUtils.copy(input, output);
			
			// flush'n'close
			output.flush();
			output.close();
			input.close();
			
			response.flushBuffer();
			
			// remove the temp file
			tempFile.delete();
		}
		catch (IOException e) {
			// TODO
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found in archive." );
			return;
		}
		
	}

}
