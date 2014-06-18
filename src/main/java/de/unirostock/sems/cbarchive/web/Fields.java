package de.unirostock.sems.cbarchive.web;

import java.io.File;

public class Fields {
	
	/** The Constant COOKIE_USER. Stores serialized user data on the client */
	public static final String COOKIE_USER = "combinearchivewebuser";
	
	/** The Constant COOKIE_PATH. */
	public static final String COOKIE_PATH = "combinearchiveweba";

//	/** The Constant COOKIE_GIVEN_NAME. */
//	public static final String COOKIE_GIVEN_NAME = "combinearchivewebb";
//
//	/** The Constant COOKIE_FAMILY_NAME. */
//	public static final String COOKIE_FAMILY_NAME = "combinearchivewebc";
//
//	/** The Constant COOKIE_MAIL. */
//	public static final String COOKIE_MAIL = "combinearchivewebd";
//
//	/** The Constant COOKIE_ORG. */
//	public static final String COOKIE_ORG = "combinearchivewebe";

	// TODO: move this to the context.xml 
	/** The Constant STORAGE. */
	public static final File STORAGE = new File ("/tmp/CombineArchiveWebStorage");

	/** The Constant COOKIE_AGE. */
	public static final int COOKIE_AGE = 60*60*24*365;

	/** The Constant MAX_ARCHIVES. */
	public static final int MAX_ARCHIVES = 10;

	/** The Constant MAX_FILES_PER_ARCHIVE. */
	public static final int MAX_FILES_PER_ARCHIVE = 20;

	/** The Constant MAX_FILE_SIZE. */
	public static final int MAX_FILE_SIZE = 1024*1024;
	
}
