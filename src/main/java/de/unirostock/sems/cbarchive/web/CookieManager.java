/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

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
