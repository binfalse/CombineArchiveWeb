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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbext.Iconizer;

public class IconServlet extends HttpServlet {

	private static final long serialVersionUID = 7167498053608057183L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
		// set charset
		response.setCharacterEncoding (Fields.CHARSET);
		request.setCharacterEncoding (Fields.CHARSET);
		
		// splitting request URL
		String requestUrl =  request.getRequestURI();
		LOGGER.debug("IconServlet request: ", requestUrl);
		
		if( requestUrl != null && !requestUrl.isEmpty() ) {
			
			try {
				response.setContentType( "image/png" );
				
				String formatString = requestUrl.substring( requestUrl.indexOf("res/icon/") + 9 );
				LOGGER.debug("format: ", formatString);
				
				URI format = new URI( formatString );
				LOGGER.info("format url: ", format);
				OutputStream output = response.getOutputStream();
				
				InputStream input = Iconizer.formatToIconStream(format);
				int size = IOUtils.copy(input, output);
				response.setContentLength(size);
				
				output.flush();
				output.close();
				input.close();
				
				response.flushBuffer();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		try {
			OutputStream output = response.getOutputStream();
			new OutputStreamWriter(output).append("Hello World");
			output.close();
			response.flushBuffer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
