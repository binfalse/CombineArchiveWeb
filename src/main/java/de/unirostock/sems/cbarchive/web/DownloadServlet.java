package de.unirostock.sems.cbarchive.web;

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
		User user = null;
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
		LOGGER.debug(requestUrl);
		
		// check entry points
		if( requestUrl.length >= 4 && requestUrl[2].equals("archive") ) {
			// request to download an archive from the workspace
			if( requestUrl[3] != null && !requestUrl[3].isEmpty() )
				downloadArchive(request, response, user, requestUrl[3] );
		}
		
	}
	
	private void downloadArchive(HttpServletRequest request, HttpServletResponse response, User user, String archive) {
		
		// filters for omex extension
		//		( is just there for the browser to name the downloaded file correctly)
		if( archive.endsWith( "." + COMBINEARCHIVE_FILE_EXT ) )
			// just removes the file extension -> we get the archiveId
			archive = archive.substring(0, archive.length() - (COMBINEARCHIVE_FILE_EXT.length() + 1) );
		
		File archiveFile = null;
		String archiveName = null;
		try {
			archiveFile = user.getArchiveFile(archive);
			archiveName = user.getArchiveName(archive);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
