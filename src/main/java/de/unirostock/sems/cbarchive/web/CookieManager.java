/**
 * 
 */
package de.unirostock.sems.cbarchive.web;
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

import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


// TODO: Auto-generated Javadoc
/**
 * The Class CookieManager.
 *
 * @author martin
 */
public class CookieManager
{
	
	/** The cookies. */
	private HashMap<String, Cookie> cookies;
	
	/** The response. */
	private HttpServletResponse response;
	
	/**
	 * Instantiates a new cookie manager.
	 *
	 * @param request the request
	 * @param response the response
	 */
	public CookieManager (HttpServletRequest request, HttpServletResponse response)
	{
		this.cookies = new HashMap<String, Cookie> ();
		Cookie[] cookies = request.getCookies ();
		this.response = response;
		if (cookies != null)
		for (Cookie c : cookies)
			this.cookies.put (c.getName (), c);
	}
	
	/**
	 * Gets the cookie.
	 *
	 * @param name the name
	 * @return the cookie
	 */
	public Cookie getCookie (String name)
	{
		return cookies.get (name);
	}
	
	/**
	 * Sets the cookie.
	 *
	 * @param cookie the new cookie
	 */
	public void setCookie (Cookie cookie) {
		cookie.setPath( "/" );
		cookie.setMaxAge( Fields.COOKIE_AGE );
		
		cookies.put(cookie.getName (), cookie);
		response.addCookie (cookie);
	}
	
}
