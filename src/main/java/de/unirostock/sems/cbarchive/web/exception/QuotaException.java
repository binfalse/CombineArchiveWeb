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

package de.unirostock.sems.cbarchive.web.exception;



public class QuotaException extends CombineArchiveWebException {

	private static final long serialVersionUID = 5379704954830465478L;
	
	/** string which is safe to show to user */
	private String userMessage = null;

	public QuotaException() {
		super();
	}

	public QuotaException(String message, Throwable cause) {
		super(message, cause);
	}

	public QuotaException(String msg) {
		super(msg);
	}

	public QuotaException(Throwable cause) {
		super(cause);
	}
	
	public QuotaException(String msg, String userMessage) {
		super(msg);
		this.userMessage = userMessage;
	}
	
	public QuotaException(String msg, String userMessage, Throwable cause) {
		super(msg, cause);
		this.userMessage = userMessage;
	}
	
	/**
	 * Returns the user message, which is safe to show to end user
	 * 
	 * @return String
	 */
	public String getUserMessage() {
		return userMessage;
	}
	
}
