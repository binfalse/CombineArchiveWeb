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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.Utils;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;

public class WorkspaceManager {
	
	// Singleton stuff
	
	private static volatile WorkspaceManager instance = null;
	
	public static final WorkspaceManager getInstance() {
		
		if( instance == null )
			instance = new WorkspaceManager();
		
		return instance;
	}
	
	// --------------------------------------------------------------------------------,
	
	protected Map<String, Workspace> workspaces = new HashMap<String, Workspace>();
	protected Date lastSaved = new Date();
	
	/**
	 * Default Constructor.
	 * Use getInstance()
	 */
	private WorkspaceManager() {
		// read the settings
		reloadSettings();
	}
	
	public Workspace getWorkspace( String workspaceId ) {
		updateStorage();
		return workspaces.get(workspaceId);
	}
	
	public boolean hasWorkspace( String workspaceId ) {
		return workspaces.containsKey(workspaceId);
	}
	
	public synchronized Workspace createWorkspace() throws IOException {
		
		if( Fields.STORAGE.exists() == false && Fields.STORAGE.mkdirs() == false ) {
			LOGGER.error( "Cannot create storage directory", Fields.STORAGE );
			throw new IOException( "Cannot create storage directory" );
		}

		String uuid = null;
		File workingDir = null;
		while( uuid == null || workingDir.exists() ) {
			uuid = UUID.randomUUID ().toString ();
			workingDir = new File( Fields.STORAGE, uuid );
		}
		
		Workspace workspace = new Workspace(uuid);
		workspace.setWorkspaceDir(workingDir);
		
		// create working dir
		if( workingDir.mkdirs() == false ) {
			LOGGER.error( "Cannot create working directory ", workingDir );
			throw new IOException( "Cannot create working directory" );
		}
		
		workspace.updateLastseen();
		// add to settings
		workspaces.put(uuid, workspace);
		
		return workspace;
	}
	
	public synchronized void removeWorkspace( Workspace workspace ) {
		
		if( workspace == null )
			return;
		
		// first remove it from the index
		workspaces.remove( workspace.getWorkspaceId() );
		updateStorage();
		
		// remove directory
		try {
			Utils.delete( workspace.getWorkspaceDir() );
		} catch (IOException e) {
			LOGGER.error(e, "Cannot delete Workspace directory ", workspace.getWorkspaceId() );
		}
		
	}
	
	/**
	 * Checks if the time for the store cycle exceeded and runs the store process, if necessary
	 */
	public synchronized void updateStorage() {
		
		long difference = (new Date()).getTime() - lastSaved.getTime();
//		LOGGER.info( (new Date()).getTime(), "  ", lastSaved.getTime(), "  ", difference, "  ", Fields.STORAGE_AGE );
		
		if( difference > Fields.STORAGE_AGE )
			// Scan all workspaces and than saves the settings in a separate thread
			QuotaManager.getInstance().forceAsyncScan(true);
		
	}
	
	private synchronized void reloadSettings() {
		
		if( !Fields.SETTINGS_FILE.exists() || !Fields.SETTINGS_FILE.canRead() ) {
			// in case the file cannot be read
			LOGGER.warn( "Cannot read central settings file ", Fields.SETTINGS_FILE, ". No existing Workspace will be available" );
			LOGGER.warn( "Create empty central settings file.");
			storeSettings();
			return;
		}
		
		Properties properties = null;
		try {
			// open file
			InputStream input = new FileInputStream(Fields.SETTINGS_FILE);
			// load properties
			properties = new Properties();
			properties.load(input);
			
		} catch (IOException e) {
			LOGGER.error( e, "Error while reading the central settings file." );
			return;
		}
		
		// **** begin iterating over properties 
		for( Object p : properties.keySet() ) {
			String key = (String) p;
			
			if( key.startsWith(Fields.PROP_LASTSEEN_PRE) ) {
				// last-seen field of the workspace
				
				String workspaceId = key.substring( Fields.PROP_LASTSEEN_PRE.length() );
				Workspace workspace = workspaces.get(workspaceId);
				// add workspace to the list, if necessary
				if( workspace == null ) {
					workspace = new Workspace(workspaceId);
					workspaces.put(workspaceId, workspace);
				}
				
				try {
					workspace.setLastseen( Tools.DATE_FORMATTER.parse(properties.getProperty(key)) );
				} catch (ParseException e) {
					LOGGER.warn(e, "Cannot parse date", properties.getProperty(key));
				}
				
			}
			else if( key.startsWith(Fields.PROP_WORKSPACE_PRE) ) {
				// name field of the workspace
				
				String workspaceId = key.substring( Fields.PROP_WORKSPACE_PRE.length() );
				Workspace workspace = workspaces.get(workspaceId);
				// add workspace to the list, if necessary
				if( workspace == null ) {
					workspace = new Workspace(workspaceId);
					workspaces.put(workspaceId, workspace);
				}
				
				workspace.setName( properties.getProperty(key) );
			}
			else if( key.startsWith(Fields.PROP_ARCHIVE_PRE) ) {
				// archive entry
				
				String[] parts = key.substring( Fields.PROP_ARCHIVE_PRE.length() ).split( Fields.PROP_SEPARATOR_REGEX );
				if( parts.length != 2 ) {
					LOGGER.warn("confusing properties key found ", key);
					continue;
				}
				
				Workspace workspace = workspaces.get(parts[0]);
				// add workspace to the list, if necessary
				if( workspace == null ) {
					workspace = new Workspace(parts[0]);
					workspaces.put(parts[0], workspace);
				}
				
				workspace.getArchives().put( parts[1], properties.getProperty(key) );			
			}
		}
		// **** end iterating over properties
	}
	
	/**
	 * stores the settings immediately to disk
	 * 
	 */
	public synchronized void storeSettings() {
		
		Properties properties = new Properties();
		Date savedDate = new Date();
		
		LOGGER.info("store settings to disk");
		
		for( String workspaceId : workspaces.keySet() ) {
			Workspace workspace = workspaces.get(workspaceId);
			
			// add fields for name and last-seen
			properties.setProperty( Fields.PROP_WORKSPACE_PRE + workspaceId, workspace.getName() );
			properties.setProperty( Fields.PROP_LASTSEEN_PRE + workspaceId, Tools.DATE_FORMATTER.format(workspace.getLastseen()) );
			
			// iterate over all archives
			for( String archiveId : workspace.getArchives().keySet() ) {
				String archiveName = workspace.getArchives().get(archiveId);
				properties.setProperty( Fields.PROP_ARCHIVE_PRE + workspaceId + Fields.PROP_SEPARATOR + archiveId, archiveName);
			}
		}
		
		try {
			// first write settings to temp file
			File temp = File.createTempFile( Fields.TEMP_FILE_PREFIX, "settings.conf");
			OutputStream output = new FileOutputStream( temp );
			properties.store(output, null);

			// flush'n'close
			output.flush();
			output.close();
			
			// create directories (just in case...)
			Files.createDirectories( Fields.SETTINGS_FILE.toPath().getParent() );
			
			// replace actual settings file, with newly written one
			try {
				Files.move( temp.toPath(), Fields.SETTINGS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
			}
			catch (AtomicMoveNotSupportedException e) {
				LOGGER.info(e, "Atomic move of settings file failed. Fallback to normal operation.");
				Files.move( temp.toPath(), Fields.SETTINGS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING );
			}
			
			// set time of last store to now
			lastSaved = savedDate;
		} catch (IOException e) {
			LOGGER.error(e, "Cannot write central properties file");
		}
		
	}

}
