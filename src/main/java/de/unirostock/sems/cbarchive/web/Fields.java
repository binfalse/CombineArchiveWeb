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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import de.binfalse.bflog.LOGGER;

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
	
	/** Blacklist of file names */
	public static final Set<String> FILENAME_BLACKLIST = Collections.unmodifiableSet(
			new HashSet<String>( Arrays.asList( new String[] {"metadata.rdf", "manifest.xml"} ) )
		);

	/** The Constant COOKIE_AGE. */
	public static final int COOKIE_AGE = 60*60*24*365;

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
	
	/** default buffer size for copying stuff */
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	
	/** Timeout for locks on archives */
	public static final long LOCK_ARCHIVE_TIMEOUT = 5;
	
	/** Default name for new combine archives */
	public static final String NEW_ARCHIVE_NAME = "New CombineArchive";
	
	/** Link to the sedML WebTools for starting a simulation */
	public static String SEDML_WEBTOOLS_URL = null;
	
	/** Link to a feedback form */
	public static String FEEDBACK_URL = "https://sems.uni-rostock.de/trac/combinearchive-web/newticket?from=WEBCAT-INTERFACE";
	
	/** Link to CaRo (CombineArchive to Research Object) Converter */
	/* http://caro.web/caro?from= */
	public static String CARO_URL = null;
	
	/** Link to RoCa (Research Object to CombineArchive) Converter */
	/* http://caro.web/roca?from= */
	public static String ROCA_URL = null;
	
	/** max time for caching statistic data */
	public static long STATS_MAX_AGE = 180;
	
	/** switch the open stats page on or off, also limits the access to the rest-endpoint, when set to false */
	public static boolean STATS_PUBLIC = true;
	
	/** is set to something != null, it enables the full access to all stats, when added as URL-Parameter */
	public static String STATS_SECRET = null;
	
	/** current version of webCAT, automatically read from src/main/resources/version.pref and set via maven resource filtering */
	public static String CURRENT_VERSION = "[head version]";
	
	/** determines, if current build is a release build. see CURRENT_VERSION */
	public static boolean CURRENT_VERSION_IS_RELEASE = false;
	
	/** allows to share the entire workspace history, this does not effect the UI, but enables the /share/history endpoint,
	 * which is used by the migration PHP script. Disabled by default. */
	public static boolean ALLOW_SHARING_HISTORY = false;
	
	// ------------------------------------------------------------------------
	// Quotas

	/** Value for unlimited quota. Not a quota by itself */
	public static final long QUOTA_UNLIMITED = 0L;

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

	// ------------------------------------------------------------------------
	// Loading Fields from servlet context

	public static void loadSettingsFromContext( ServletContext context ) {
		
		// Log Level
		LOGGER.setMinLevel (LOGGER.WARN);
		String desiredLogLevel = context.getInitParameter("LOGLEVEL");

		if (desiredLogLevel != null) {
			LOGGER.warn("Setting log level to ", desiredLogLevel);

			if (desiredLogLevel.equals ("DEBUG")) {
				LOGGER.setMinLevel (LOGGER.DEBUG);
				LOGGER.setLogStackTrace (true);
			}
			else if (desiredLogLevel.equals ("INFO"))
				LOGGER.setMinLevel (LOGGER.INFO);
			else if (desiredLogLevel.equals ("WARN"))
				LOGGER.setMinLevel (LOGGER.WARN);
			else if (desiredLogLevel.equals ("ERROR"))
				LOGGER.setMinLevel (LOGGER.ERROR);
			else if (desiredLogLevel.equals ("NONE"))
				LOGGER.setLogToStdErr (false);
		}
		
		// Version
		InputStream versionPropStream = context.getResourceAsStream("/WEB-INF/classes/version.properties");
		if(versionPropStream != null ) {
			try {
				Properties versionProp = new Properties();
				versionProp.load( versionPropStream );
				versionPropStream.close();
				
				CURRENT_VERSION = versionProp.getProperty("version", CURRENT_VERSION);
				CURRENT_VERSION_IS_RELEASE = versionProp.getProperty("release", "false").equals("true") ? true : false; 
			} catch (IOException e) {
				LOGGER.warn(e, "Could not read version resource. No version will be shown.");
			}
		}
		
		LOGGER.info("Starting webCAT ", CURRENT_VERSION, CURRENT_VERSION_IS_RELEASE ? "" : " non release build");
		
		// Storage
		String storage = context.getInitParameter("STORAGE");
		if( storage != null ) {
			Fields.STORAGE = new File( storage );
			Fields.SETTINGS_FILE = new File( Fields.STORAGE, Fields.SETTINGS_FILE_NAME );

			LOGGER.info("Set storage to ", Fields.STORAGE);
		}
		
		// sedML WebTools
		String sedMlWebTools = context.getInitParameter("SEDML_WEBTOOLS");
		if( sedMlWebTools != null && sedMlWebTools.isEmpty() == false )
			Fields.SEDML_WEBTOOLS_URL = sedMlWebTools;
		else
			Fields.SEDML_WEBTOOLS_URL = null;
		
		// CaRo Url
		String caroUrl = context.getInitParameter("CARO_URL");
		if( caroUrl != null && caroUrl.isEmpty() == false )
			Fields.CARO_URL = caroUrl;
		else
			Fields.CARO_URL = null;
		
		// CaRo Url
		String rocaUrl = context.getInitParameter("ROCA_URL");
		if( rocaUrl != null && rocaUrl.isEmpty() == false )
			Fields.ROCA_URL = rocaUrl;
		else
			Fields.ROCA_URL = null;
		
		// max stats age
		STATS_MAX_AGE = parseLong( context.getInitParameter("MAX_STATS_AGE"), STATS_MAX_AGE );
		
		// public stats
		String statsPublic = context.getInitParameter("STATS_PUBLIC");
		if( statsPublic != null && statsPublic.isEmpty() == false ) {
			statsPublic = statsPublic.toLowerCase();
			if( statsPublic.equals("true") || statsPublic.equals("1") )
				STATS_PUBLIC = true;
			else
				STATS_PUBLIC = false;
		}
		
		String statsSecret = context.getInitParameter("STATS_SECRET");
		if( statsSecret != null && statsSecret.isEmpty() == false )
			STATS_SECRET = statsSecret;
		else
			STATS_SECRET = null;
		
		// feedback Url
		String feedbackUrl = context.getInitParameter("FEEDBACK_URL");
		if( feedbackUrl != null && feedbackUrl.isEmpty() == true )
			// disable feedback button
			Fields.FEEDBACK_URL = null;
		else if( feedbackUrl != null && feedbackUrl.isEmpty() == false )
			// set another URL
			Fields.FEEDBACK_URL = feedbackUrl;
		
		// History sharing
		String allowSharingHistory = context.getInitParameter("ALLOW_SHARING_HISTORY");
		if( allowSharingHistory != null && allowSharingHistory.isEmpty() == false ) {
			allowSharingHistory = allowSharingHistory.toLowerCase();
			if( allowSharingHistory.equals("true") || allowSharingHistory.equals("1") )
				ALLOW_SHARING_HISTORY = true;
			else
				ALLOW_SHARING_HISTORY = false;
		}
		
		// Quotas
		
		QUOTA_TOTAL_SIZE		= parseQuotaFromString( context.getInitParameter("QUOTA_TOTAL_SIZE") );
		QUOTA_WORKSPACE_SIZE	= parseQuotaFromString( context.getInitParameter("QUOTA_WORKSPACE_SIZE") );
		QUOTA_WORKSPACE_AGE		= parseQuotaFromString( context.getInitParameter("QUOTA_WORKSPACE_AGE") );
		QUOTA_ARCHIVE_SIZE		= parseQuotaFromString( context.getInitParameter("QUOTA_ARCHIVE_SIZE") );
		QUOTA_ARCHIVE_LIMIT		= parseQuotaFromString( context.getInitParameter("QUOTA_ARCHIVE_LIMIT") );
		QUOTA_FILE_LIMIT		= parseQuotaFromString( context.getInitParameter("QUOTA_FILE_LIMIT") );
		QUOTA_UPLOAD_SIZE		= parseQuotaFromString( context.getInitParameter("QUOTA_UPLOAD_SIZE") );
		
		
		LOGGER.info ("configured: ",
			"\nLoglevel: ", desiredLogLevel,
			"\nFields.STORAGE: ", Fields.STORAGE,
			"\nFields.SETTINGS_FILE: ", Fields.SETTINGS_FILE,
			"\nQUOTA_TOTAL_SIZE: ", QUOTA_TOTAL_SIZE,
			"\nQUOTA_WORKSPACE_SIZE: ", QUOTA_WORKSPACE_SIZE,
			"\nQUOTA_WORKSPACE_AGE: ", QUOTA_WORKSPACE_AGE,
			"\nQUOTA_ARCHIVE_SIZE: ", QUOTA_ARCHIVE_SIZE,
			"\nQUOTA_ARCHIVE_LIMIT: ", QUOTA_ARCHIVE_LIMIT,
			"\nQUOTA_FILE_LIMIT: ", QUOTA_FILE_LIMIT, 
			"\nQUOTA_UPLOAD_SIZE: ", QUOTA_UPLOAD_SIZE
			);
		
	}
	
	private static long parseQuotaFromString( String string ) {
		return parseLong(string, QUOTA_UNLIMITED);
	}
	
	private static long parseLong( String string, long defaultValue ) {
		
		if( string == null || string.isEmpty() )
			return defaultValue;
		
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			LOGGER.warn("Bad number format in the context settings: ", string);
			return defaultValue;
		}
	}

}
