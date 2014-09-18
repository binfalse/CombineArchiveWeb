/**
 * 
 */
package de.unirostock.sems.cbarchive.web.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;


/**
 * @author Martin Scharm
 *
 */
public class Index extends HttpServlet {

	private static final long serialVersionUID = 7678663032688543485L;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		
		// Log Level
		LOGGER.setMinLevel (LOGGER.WARN);
		String desiredLogLevel = context.getInitParameter("LOGLEVEL");
		
		if (desiredLogLevel != null)
		{
			LOGGER.warn("Try to set log level to ", desiredLogLevel);
			
			if (desiredLogLevel.equals ("DEBUG"))
			{
				LOGGER.setMinLevel (LOGGER.DEBUG);
				LOGGER.setLogStackTrace (true);
			}
			else if (desiredLogLevel.equals ("INFO")) {
				LOGGER.setMinLevel (LOGGER.INFO);
			}
			else if (desiredLogLevel.equals ("WARN"))
				LOGGER.setMinLevel (LOGGER.WARN);
			else if (desiredLogLevel.equals ("ERROR"))
				LOGGER.setMinLevel (LOGGER.ERROR);
			else if (desiredLogLevel.equals ("NONE"))
				LOGGER.setLogToStdErr (false);
		}
		
		// Storage
		String storage = context.getInitParameter("STORAGE");
		LOGGER.error(storage);
		if( storage != null ) {
			Fields.STORAGE = new File( storage );
			Fields.SETTINGS_FILE = new File( Fields.STORAGE, Fields.SETTINGS_FILE_NAME );
			
			LOGGER.info("Setted storage to ", Fields.STORAGE);
		}
	}



	private void run (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		response.setContentType ("text/html");
		response.setCharacterEncoding ("UTF-8");
		request.setCharacterEncoding ("UTF-8");

		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		LOGGER.debug ("req: ", Arrays.toString (req));

		// gets the user class
		UserManager user = null;
		try {
			user = Tools.doLogin(request, response);
		}
		catch (CombineArchiveWebException | CombineArchiveWebCriticalException e) {
			LOGGER.error(e, "Cannot find and/or obtain working directory");
			response.sendError(500, "Cannot find and/or obtain working directory");
			return;
		}

		request.setAttribute ("user", user);
		request.setAttribute ("ContextPath", request.getContextPath ());
		request.getRequestDispatcher ("/WEB-INF/Index.jsp").forward (request, response);
	}


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		run (request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		run (request, response);
	}
}
