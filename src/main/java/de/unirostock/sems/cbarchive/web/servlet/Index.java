/**
 * 
 */
package de.unirostock.sems.cbarchive.web.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;


/**
 * @author Martin Scharm
 *
 */
public class Index extends HttpServlet {

	private static final long serialVersionUID = 7678663032688543485L;

	private void run (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		LOGGER.setMinLevel (LOGGER.WARN);
		String desiredLogLevel = getServletContext().getInitParameter("LOGLEVEL");
		if (desiredLogLevel != null)
		{
			if (desiredLogLevel.equals ("DEBUG"))
			{
				LOGGER.setMinLevel (LOGGER.DEBUG);
				LOGGER.setLogStackTrace (true);
			}
			else if (desiredLogLevel.equals ("INFO"))
				LOGGER.setMinLevel (LOGGER.INFO);
			else if (desiredLogLevel.equals ("WARN"))
				LOGGER.setMinLevel (LOGGER.WARN);
			else if (desiredLogLevel.equals ("ERROR"))
				LOGGER.setMinLevel (LOGGER.ERROR);
			else if (desiredLogLevel.equals ("NONE"))
				LOGGER.setLogToStdErr (false);
		}

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
			LOGGER.error(e, "Can not find and/or obtain working directory");
			response.sendError(500, "Can not find and/or obtain working directory");
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
