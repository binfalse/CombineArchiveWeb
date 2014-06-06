/**
 * 
 */
package de.unirostock.sems.cbarchive.web;


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
}
