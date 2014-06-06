/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;


/**
 * @author Martin Scharm
 *
 */
public class Index
extends HttpServlet
{
	private void run (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		LOGGER.setMinLevel (LOGGER.DEBUG);
		LOGGER.setLogStackTrace (true);

		response.setContentType ("text/html");
		response.setCharacterEncoding ("UTF-8");
		request.setCharacterEncoding ("UTF-8");
		
		
	  HttpSession session = request.getSession (true);
		CookieManager cookieMgmt = new CookieManager (request, response);
		

		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		LOGGER.debug ("req: ", Arrays.toString (req));
		
		User user = Tools.getUser (cookieMgmt);
		if (user == null)
			Tools.createUser (cookieMgmt);
		request.setAttribute ("user", user);
		
		if (user != null)
			try
			{
				user.setWd (Tools.getWorkingDirectory (cookieMgmt));
			}
			catch (IOException e)
			{
				LOGGER.warn (e, "could not obtain working directory");
				user.setWd (Tools.createWorkingDirectory (cookieMgmt));
			}
		
		request.getRequestDispatcher ("/WEB-INF/Index.jsp").forward (request, response);
	}

	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet (HttpServletRequest request, HttpServletResponse response)
		throws ServletException,
			IOException
	{
		run (request, response);
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost (HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException
	{
		run (request, response);
	}
}
