package de.unirostock.sems.cbarchive.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.binfalse.bflog.LOGGER;

public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 7562736436930714565L;

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
		
		// check entry points
		if( requestUrl.length >= 4 && requestUrl[2].equals("archive") ) {
			// request to download an archive from the workspace
			if( requestUrl[4] != null && !requestUrl[4].isEmpty() )
				downloadArchive(request, response, user, requestUrl[4] );
		}
		
	}
	
	private void downloadArchive(HttpServletRequest request, HttpServletResponse response, User user, String archive) {
		
		// TODO
	}

}
