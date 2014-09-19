package de.unirostock.sems.cbarchive.web;

import java.io.File;

public class Fields {
	
	/** The Constant COOKIE_USER. Stores serialized user data on the client */
	public static final String COOKIE_USER = "combinearchivewebuser";
	
	/** The Constant COOKIE_PATH. */
	public static final String COOKIE_PATH = "combinearchiveweba";
	
	/** The Constant COOKIE_WORKSPACE_HISTORY */
	public static final String COOKIE_WORKSPACE_HISTORY = "combinearchivewebhist";
	
	public static final String SETTINGS_FILE_NAME = "CaWeb.settings";
	
	/** The Constant STORAGE. */
	public static File STORAGE = new File ("/tmp/CombineArchiveWebStorage");
	
	public static File SETTINGS_FILE = new File( STORAGE, SETTINGS_FILE_NAME );

	/** The Constant COOKIE_AGE. */
	public static final int COOKIE_AGE = 60*60*24*365;

	/** The Constant MAX_FILE_SIZE. */
	public static final int MAX_FILE_SIZE = 1024*1024;

	public static final String PROP_ARCHIVE_PRE = "archive-";
	
	public static final String PROP_WORKSPACE_PRE ="workspace-";
	
	public static final String PROP_LASTSEEN_PRE = "workspace-lastseen-";
	
	public static final String PROP_SEPARATOR = ".";
	
	public static final String PROP_SEPARATOR_REGEX = "\\.";
	
	/** minimum time after that the settings should be stored in ms */
	// 5 min.
	public static final long STORAGE_AGE = 60*5*1000;
	
	/** Prefix for temp files */
	public static final String TEMP_FILE_PREFIX = "caweb";
	
	/** Hash Algo for generating temp ids */
	public static final String HASH_ALGO = "SHA-256";
	
	/** The charset for all http related interactions */
	public static final String CHARSET = "UTF-8";

	// ------------------------------------------------------------------------
	// Quotas
	
	/** Value for unlimited quota. Not a quota by itself */
	public static final long QUOTA_UNLIMITED = 0;
	
	/** Maximum size in bytes of all workspaces together. 0 means unlimited */
	public static long QUOTA_TOTAL_SIZE = QUOTA_UNLIMITED;
	
	/** Maximum size in bytes of all archives in one workspaces together. 0 means unlimited */
	public static long QUOTA_WORKSPACE_SIZE = QUOTA_UNLIMITED;
	
	/** Maximum time in seconds a workspace can be left unused, before deletion. 0 means unlimited */ 
	public static long QUOTA_WORKSPACE_AGE = QUOTA_UNLIMITED;
	
	/** Maximum size of one archive in bytes. 0 means unlimited */ 
	public static long QUOTA_ARCHIVE_SIZE = QUOTA_UNLIMITED;
	
	/** Maximum number of archives in a workspace. 0 means unlimited */
	public static long QUOTA_ARCHIVE_LIMIT = QUOTA_UNLIMITED;
	
	/** Maximum number of files in an archive. 0 means unlimited */
	public static long QUOTA_FILE_LIMIT = QUOTA_UNLIMITED;
	
	/** Maximum file size for uploads. 0 means unlimited */
	public static long QUOTA_UPLOAD_SIZE = QUOTA_UNLIMITED;
	
}
