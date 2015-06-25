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
import de.unirostock.sems.cbarchive.web.dataholder.StatisticData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;

public class QuotaManager {

	// Singleton stuff
	private static volatile QuotaManager instance = null;
	
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
	
	protected Thread workerThread = null;
	protected long workerExecutionTime = 0L;
	
	protected StatisticData stats = null;
	protected Date statsTimestamp = null;

	private QuotaManager() {

		workspaceManager = WorkspaceManager.getInstance();
		//forceAsyncScan(false);
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
		
		if( (workerThread == null || workerThread.isAlive() == false) && workerLock.tryLock() ) {
			workerThread = new Thread( new Worker(this, storeSettingsAfterwards) );
			workerLock.unlock();
			workerThread.start();
		}
		
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
	
	public StatisticData getStats() {
		
		// if cache is ok
		if( stats == null || statsTimestamp == null || (new Date().getTime() - statsTimestamp.getTime() + workerExecutionTime)/1000 > Fields.MAX_STATS_AGE )
			generateStats();
		
		LOGGER.debug( "return stats, generated at ", stats.getGenerated() );
		return stats;
	}
	
	public StatisticData getUserStats(UserManager user) {
		StatisticData stats = getStats().clone();
		
		if( user != null ) {
			// add user stats
			if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED )
				stats.setUserWorkspaceSizeQuota( (double) getWorkspaceSize( user.getWorkspace() ) / (double) Fields.QUOTA_WORKSPACE_SIZE );
			if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED)
				stats.setUserArchiveCountQuota( (double) user.getArchives().size() / (double) Fields.QUOTA_ARCHIVE_LIMIT );
		}
		
		return stats;
	}
	
	private void generateStats() {
		
		LOGGER.debug( "start generation of new stats, old are generated at: ", stats.getGenerated() );
		
		// start new thread
		forceAsyncScan(false);
		
		// wait for the thread to finish
		try {
			workerThread.join( workerExecutionTime > 5 ? workerExecutionTime * 2 : 250 );
		} catch (InterruptedException e) {
			LOGGER.debug(e, "Aborted waiting for background task");
		}
		
		LOGGER.debug( "stop waiting for generation, new stats are from: ", stats.getGenerated() );
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
			long startTime = new Date().getTime();
			
			// runs only once per time
			if( quotaManager.workerLock.tryLock() == false )
				return;
			
			LOGGER.info("start full quota scan");
			
			// scan all workspaces
			long totalSize = 0L;
			long totalArchiveCount = 0L;
			long totalWorkspaceAge = 0L;
			long workspaceCount = 0L;
			Map<String, Long> cache = new HashMap<String, Long>();
			Date now = new Date();
			long nowTime = now.getTime();
			
			// clone collection in order to not get the iterator broken by some manipulations form other threads
			List<Workspace> collection = new ArrayList<Workspace>( quotaManager.workspaceManager.workspaces.values() ); 
			for( Workspace workspace : collection ) {
				
				long size = quotaManager.scanWorkspace(workspace);
				if( size > 0 ) {
					cache.put( workspace.getWorkspaceId(), size );
					
					totalSize += size;
					totalArchiveCount += workspace.getArchives().size();
					totalWorkspaceAge += (nowTime - workspace.getLastseen().getTime())/1000;
					workspaceCount++;
				}
			}
			
			// generate stats
			StatisticData stats = new StatisticData();
			stats.setGenerated(now);
			
			stats.setTotalSize(totalSize);
			stats.setTotalWorkspaceCount(workspaceCount);
			stats.setTotalArchiveCount(totalArchiveCount);
			stats.setAverageWorkspaceSize( (double) totalSize / (double) workspaceCount );
			stats.setAverageArchiveCount( (double) totalArchiveCount / (double) workspaceCount );
			stats.setAverageWorkspaceAge( (double) totalWorkspaceAge / (double) workspaceCount );
			
			if( Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED )
				stats.setAverageWorkspaceSizeQuota( stats.getAverageWorkspaceSize() / (double) Fields.QUOTA_WORKSPACE_SIZE );
			if( Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED )
				stats.setTotalSizeQuota( (double) totalSize / (double) Fields.QUOTA_TOTAL_SIZE );
			if( Fields.QUOTA_ARCHIVE_LIMIT != Fields.QUOTA_UNLIMITED )
				stats.setAverageArchiveCountQuota( stats.getAverageArchiveCount() / (double) Fields.QUOTA_ARCHIVE_LIMIT );
			
			stats.setMaxStatsAge( Fields.MAX_STATS_AGE );
			
			// tranfer the results to the main class
			quotaManager.workspaceCache = cache;
			quotaManager.totalSize = totalSize;
			quotaManager.stats = stats;
			quotaManager.statsTimestamp = now;
			
			// store settings to disk, if needed
			if( storeSettings )
				quotaManager.workspaceManager.storeSettings();
			
			// save duration of execution
			quotaManager.workerExecutionTime = new Date().getTime() - startTime;
			LOGGER.info("finished full quota scan in ", quotaManager.workerExecutionTime, "ms");
			// give dobby a sock
			quotaManager.workerLock.unlock();
		}
		
	}

}
