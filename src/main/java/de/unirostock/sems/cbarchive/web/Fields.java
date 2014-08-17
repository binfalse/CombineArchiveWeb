package de.unirostock.sems.cbarchive.web;

import java.io.File;

public class Fields {
	
	/** The Constant COOKIE_USER. Stores serialized user data on the client */
	public static final String COOKIE_USER = "combinearchivewebuser";
	
	/** The Constant COOKIE_PATH. */
	public static final String COOKIE_PATH = "combinearchiveweba";
	
	/** The Constant COOKIE_WORKSPACE_HISTORY */
	public static final String COOKIE_WORKSPACE_HISTORY = "combinearchivewebhist";

	// TODO: move this to the context.xml 
	/** The Constant STORAGE. */
	public static final File STORAGE = new File ("/tmp/CombineArchiveWebStorage");
	
	public static final File SETTINGS_FILE = new File( STORAGE, "CaWeb.settings" );

	/** The Constant COOKIE_AGE. */
	public static final int COOKIE_AGE = 60*60*24*365;

	/** The Constant MAX_ARCHIVES. */
	public static final int MAX_ARCHIVES = 10;

	/** The Constant MAX_FILES_PER_ARCHIVE. */
	public static final int MAX_FILES_PER_ARCHIVE = 20;

	/** The Constant MAX_FILE_SIZE. */
	public static final int MAX_FILE_SIZE = 1024*1024;

	public static final String WORKINGDIR_PROP_FILE = "settings.properties";
	
	public static final String PROP_ARCHIVE_PRE = "archive-";
	
	public static final String PROP_WORKSPACE_PRE ="workspace-";
	
	public static final String PROP_LASTSEEN_PRE = "workspace-lastseen-";
	
	public static final String PROP_SEPARATOR = ".";
	
	public static final String PROP_SEPARATOR_REGEX = "\\.";
	
	/** minimum time after that the settings should be stored */
	// 5 min.
	public static final long STORAGE_AGE = 60*5*1000;
	
	public static final String TEMP_FILE_PREFIX = "caweb";
	
	/** Hash Algo for generating temp ids */
	public static final String HASH_ALGO = "SHA-256";
	
	/** The charset for all http related interactions */
	public static final String CHARSET = "UTF-8";

	
}
