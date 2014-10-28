/**
 * 
 */
package de.unirostock.sems.cbarchive.web.exception;
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


/**
 * The Class CombineArchiveWebException representing errors happening in the web interface.
 *
 * @author Martin Scharm
 */
public class CombineArchiveWebException
	extends Exception
{
	private static final long	serialVersionUID	= -5549201053053553658L;

	/**
	 * Instantiates a new combine archive web exception.
	 *
	 * @param msg the msg
	 */
	public CombineArchiveWebException (String msg)
	{
		super (msg);
	}

	public CombineArchiveWebException() {
		super();
	}

	public CombineArchiveWebException(String message, Throwable cause) {
		super(message, cause);
	}

	public CombineArchiveWebException(Throwable cause) {
		super(cause);
	}
	
}
