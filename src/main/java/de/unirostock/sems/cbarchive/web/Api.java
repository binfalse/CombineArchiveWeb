/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.CombineFormats;


// TODO: Auto-generated Javadoc
/**
 * The Class Api.
 *
 * @author Martin Scharm
 */
@MultipartConfig
public class Api
extends HttpServlet
{

	/**
	 * Run.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	private void run (HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException
	{
		LOGGER.setMinLevel (LOGGER.DEBUG);
		LOGGER.setLogStackTrace (true);

		response.setContentType ("application/json");
		response.setCharacterEncoding ("UTF-8");
		request.setCharacterEncoding ("UTF-8");
		
		PrintWriter out = null;
		try
		{
			out = response.getWriter ();
		}
		catch (IOException e1)
		{
			e1.printStackTrace ();
			LOGGER.error ("cannot get writer of response.");
			
			response.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		JSONObject json = new JSONObject ();
		JSONArray errors = new JSONArray ();
		//json.put ("error", e.getMessage ());
		
		
	  HttpSession session = request.getSession (true);
		CookieManager cookieMgmt = new CookieManager (request, response);
		

		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		LOGGER.debug ("req: ", Arrays.toString (req));

		User user = Tools.getUser (cookieMgmt);
		
		if (user != null)
			try
			{
				user.setWd (Tools.getWorkingDirectory (cookieMgmt));
			}
			catch (IOException e)
			{
				errors.add ("couldn't find working directory.");
				LOGGER.warn (e, "could not obtain working directory");
			}
		else
		{
			response.setStatus (HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		
		
		if (req.length >= 3 && req[2].equals ("heartbeat"))
		{
			response.setStatus (HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		
		if (req.length >= 3 && req[2].equals ("myarchives.js"))
		{
				response.setStatus (HttpServletResponse.SC_OK);
				json.put ("myarchives", user.getArchives ());
		}
		
		// TODO: delete archive
		// TODO: download files
		
		if (req.length >= 3 && req[2].equals ("createnew"))
		{
				LOGGER.info ("request for new archive");
			if (false && !user.hasInformation ())
			{
				errors.add ("you first need to fill out the form.");
			}
			else
			{
				// TODO: check max archives
				
				LOGGER.info ("creating new archive");
				String archiveName = (String) request.getParameter ("newArchiveName");
				LOGGER.debug ("new name: " + archiveName);
				if (archiveName == null || archiveName.length () < 1)
				{
					LOGGER.info ("user provided no name");
					errors.add ("no name provided");
					response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
				}
				else
				{
					response.setStatus (HttpServletResponse.SC_OK);
					try
					{
						LOGGER.info ("really creating new archive: " + archiveName);
						user.createNewArchive (archiveName);
					}
					catch (IOException | JDOMException | ParseException | CombineArchiveException | TransformerException e)
					{
						LOGGER.error (e, "couldn't create an archive");
						errors.add ("error creating archive: " + e.getMessage ());
						response.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
				}
			}
		}
		
		if (req.length >= 3 && req[2].equals ("upload.html"))
		{
			if (false && !user.hasInformation ())
			{
				errors.add ("you first need to fill out the form.");
			}
			else
			{
				// TODO: check max files and max size
				//LOGGER.info ("uploading");
				
				String archId = (String) request.getParameter ("archive");
				
				try
				{
					//LOGGER.info ("what file?");
					/*System.out.println (request.getParts ());
					System.out.println (request.getParts ().size ());
					for (Part p : request.getParts ())
					{
						System.out.println (">>> PART");
						System.out.println (p.getName ());
						System.out.println (p.getSize ());
						//System.out.println (p.getSubmittedFileName ());
						System.out.println (p.getContentType ());
						System.out.println (p.getHeaderNames ());
						System.out.println ("<<< PART");
						
					}*/
					
					
					Part filePart = request.getPart("file");
					String fileName = Tools.extractFileName (filePart);
					Path tmp = Files.createTempFile ("CombineArchiveWeb", fileName);
					filePart.write (tmp.toFile ().getAbsolutePath ());
					
					String format = Files.probeContentType (tmp);
					LOGGER.info ("user uploaded new file ", fileName, " of format ", format, " to archive ", archId);
					user.addFile (archId, tmp, fileName, format);
					/*CombineArchive ca = user.getArchive (archId);
					Part filePart = request.getPart("file");
					File f = user.getNewFile (archId, extractFileName (filePart));
					filePart.write (f.getAbsolutePath ());
					
					ArchiveEntry entry = ca.addEntry (baseDir, file, format);
					entry.addDescription (description);*/
	
					response.setStatus (HttpServletResponse.SC_NO_CONTENT);
				}
				catch (Exception e)
				{
					LOGGER.error (e, "error uploading file..");
					errors.add ("couldn't receive file: " + e.getMessage ());
					response.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
		
		
		if (errors.size () > 0)
			json.put ("error", errors);
		
		out.print (json.toJSONString ());
	}    
  
  /**
   * Extract file name.
   *
   * @param part the part
   * @return the string
   */
  protected static final String extractFileName (Part part)
  {
      String[] items = part.getHeader ("content-disposition").split (";");
      for (String s : items)
          if (s.trim ().startsWith ("filename"))
              return s.substring (s.indexOf ("=") + 2, s.length () - 1);
      return "UploadedFile-" + new SimpleDateFormat ("yyyy-MM-dd_HH-mm-ss").format (new Date ());
  }

	
	/**
	 * Archs to json.
	 *
	 * @param archs the archs
	 * @return the jSON object
	 */
	/*private static JSONObject archsToJson (HashMap<String, CombineArchive> archs)
	{
		JSONObject obj = new JSONObject ();
		for (String s : archs.keySet ())
		{
			obj.put (s, archs.get (s).toJsonDescription ());
		}
		return obj;
	}*/

	
	/**
	 * Do get.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doGet (HttpServletRequest request, HttpServletResponse response)
		throws ServletException,
			IOException
	{
		LOGGER.debug ("get request");
		run (request, response);
	}
	
	/**
	 * Do post.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doPost (HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException
	{
		LOGGER.debug ("post request");
		run (request, response);
	}
}
