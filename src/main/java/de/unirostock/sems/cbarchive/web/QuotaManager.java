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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;

public class QuotaManager {

	// Singleton stuff
	private static QuotaManager instance = null;
	
	/**
	 * Returns the Singleton instance
	 * @return
	 */
	public static QuotaManager getInstance() {
		if( instance == null )
			instance = new QuotaManager();

		return instance;
	}

	// --------------------------------------------------------------------------------

	protected Map<String, Long> workspaceCache = new HashMap<String, Long>();
	protected WorkspaceManager workspaceManager = null;
	protected long totalSize = 0L;

	private QuotaManager() {

		workspaceManager = WorkspaceManager.getInstance();
		forceAsyncScan(false);
	}
	
	/**
	 * Retuns the total size in bytes from all workspaces or 0L if it fails.
	 * 
	 * @return
	 */
	public long getTotalSize() {
		return totalSize;
	}
	
	/**
	 * Starts an async scan of all workspaces, also deletes workspaces which are too old
	 * @param storeSettingsAfterwards if set to true, the main properties will be stored after the scan
	 */
	public void forceAsyncScan( boolean storeSettingsAfterwards ) {
		
		new Thread( new Worker(this, storeSettingsAfterwards) ).start();
		
	}
	
	/**
	 * Returns the size in bytes of all archives together in the workspace or {@code 0L} if it fails. 
	 * @return
	 */
	public long getWorkspaceSize( Workspace workspace ) {
		
		if( workspaceCache.get(workspace.getWorkspaceId()) != null )
			return workspaceCache.get( workspace.getWorkspaceId() );

		return updateWorkspace(workspace);
	}
	
	/**
	 * Updates the size of the workspace or removes the result from the cache, if the workspace is not available anymore
	 * @param workspaceId
	 * @return
	 */
	public long updateWorkspace( String workspaceId ) {
		Workspace workspace = workspaceManager.getWorkspace(workspaceId);
		
		if( workspace == null ) {
			workspaceCache.remove(workspaceId);
			calcTotalSize();
			return 0L;
		}
		else
			return updateWorkspace( workspace );
	}

	/**
	 * Updates the size of the workspace
	 * @param workspace
	 * @return
	 */
	public long updateWorkspace( Workspace workspace ) {
		
		if( workspace == null )
			return 0L;
		if( workspaceManager.getWorkspace(workspace.getWorkspaceId()) == null ) {
			workspaceCache.remove( workspace.getWorkspaceId() );
			calcTotalSize();
			return 0L;
		}
		
		long size = scanWorkspace(workspace);
		workspaceCache.put( workspace.getWorkspaceId(), size );
		calcTotalSize();
		
		return size;
	}
	
	private long calcTotalSize() {
		long totalSize = 0L;
		for( Long workspaceSize : workspaceCache.values() )
			totalSize += workspaceSize;

		this.totalSize = totalSize;
		return totalSize;
	}

	private long scanWorkspace( Workspace workspace ) {
		long workspaceSize = 0L;
		
		if( workspace == null )
			return workspaceSize;
		
		if( !Tools.checkQuota( (new Date().getTime() - workspace.getLastseen().getTime())/1000, Fields.QUOTA_WORKSPACE_AGE ) ) {
			// this workspace is too old -> delete it
			LOGGER.warn( "Workspace ", workspace.getWorkspaceId(), " exceeded the age limit. Get deleted" );
			workspaceManager.removeWorkspace(workspace);
		}
		else {
			for( String id : workspace.getArchives().keySet() )
				workspaceSize += workspace.getArchiveSize(id);
		}
		
		return workspaceSize;
	}
	
	// --------------------------------------------------------------------------------
	
	private final Lock workerLock = new ReentrantLock(false);
	
	protected class Worker implements Runnable {
		
		private QuotaManager quotaManager;
		private boolean storeSettings;
		
		public Worker( QuotaManager quotaManager, boolean storeSettingsAfterwards ) {
			this.quotaManager = quotaManager;
			this.storeSettings = storeSettingsAfterwards;
		}
		
		@Override
		public void run() {
			
			// runs only once per time
			if( quotaManager.workerLock.tryLock() == false )
				return;
			
			LOGGER.info("start full quota scan");
			
			// scan all workspaces
			long totalSize = 0;
			long size = 0;
			Map<String, Long> cache = new HashMap<String, Long>();
			
			// clone collection in order to not get the iterator broken by some manipulations form other threads
			List<Workspace> collection = new ArrayList<Workspace>( quotaManager.workspaceManager.workspaces.values() ); 
			for( Workspace workspace : collection ) {
				
				size = quotaManager.scanWorkspace(workspace);
				if( size > 0 )
					cache.put( workspace.getWorkspaceId(), size );
				totalSize += size;
			}
			
			// tranfer the results to the main class
			quotaManager.workspaceCache = cache;
			quotaManager.totalSize = totalSize;
			
			// store settings to disk, if needed
			if( storeSettings )
				quotaManager.workspaceManager.storeSettings();
			
			LOGGER.info("finished full quota scan");
			// give dobby a sock
			quotaManager.workerLock.unlock();
		}
		
	}

}
