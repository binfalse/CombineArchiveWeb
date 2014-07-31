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
import javax.servlet.http.HttpSession;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;
import de.unirostock.sems.cbarchive.web.CookieManager;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;


/**
 * @author Martin Scharm
 *
 */
public class Index extends HttpServlet {

	private void run (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		LOGGER.setMinLevel (LOGGER.DEBUG);
		LOGGER.setLogStackTrace (true);

		response.setContentType ("text/html");
		response.setCharacterEncoding ("UTF-8");
		request.setCharacterEncoding ("UTF-8");


		HttpSession session = request.getSession (true);
		CookieManager cookieManagement = new CookieManager (request, response);


		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		LOGGER.debug ("req: ", Arrays.toString (req));

		// gets the user class
		UserManager user = null;
		try {
			user = Tools.getUser(cookieManagement);
		}
		catch (IOException e) {
			LOGGER.error(e, "Can not find and/or obtain working directory");
			response.sendError(500, "Can not find and/or obtain working directory");
			return;
		}

		if( user == null ) {
			LOGGER.error("Can not get/create user");
			response.sendError(500, "Can not get/create user");
			return;
		}

		request.setAttribute ("user", user);
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
