/**
 * 
 */
package de.unirostock.sems.cbarchive.web.servlet;
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

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.QuotaManager;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebCriticalException;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;


/**
 * @author Martin Scharm
 *
 */
public class Index extends HttpServlet {

	private static final long serialVersionUID = 7678663032688543485L;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// load settings form servlet context
		ServletContext context = config.getServletContext();
		Fields.loadSettingsFromContext(context);
		
		// init Singletons
		WorkspaceManager.getInstance();
		QuotaManager.getInstance().forceAsyncScan(true);
	}
	
	@Override
	public void destroy() {
		
		LOGGER.info("Destroying Index-Servlet.");
		// store settings to disk
		WorkspaceManager.getInstance().storeSettings();
		
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
