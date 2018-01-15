package de.unirostock.sems.cbarchive.web.dataholder;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;

public class Workspace {
	
	private String workspaceId = null;
	private String name = null;
	private Date lastseen = null;
	private boolean current = false;
	@JsonIgnore
	private File workspaceDir = null;
	@JsonIgnore
	private Map<String, String> archives = new HashMap<String, String>();
	@JsonIgnore
	private Map<String, ReentrantLock> locks = new HashMap<String, ReentrantLock>();
	
	public Workspace(String workspaceId, String name) {
		super();
		this.workspaceId = workspaceId;
		
		if( name == null || name.isEmpty() )
			this.name = "Workspace " + Tools.DATE_FORMATTER.format( new Date() );
		else
			this.name = name;
	}

	public Workspace(String workspaceId) {
		this(workspaceId, null);
	}

	public Workspace() {
		this(null, null);
	}
	
	@JsonIgnore
	public void updateLastseen() {
		this.lastseen = new Date();
	}
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
	
	@JsonIgnore
	public File getWorkspaceDir() throws IOException {
		
		if( (workspaceDir == null || !workspaceDir.exists()) && workspaceId != null && !workspaceId.isEmpty() ) {
			
			workspaceDir = new File( Fields.STORAGE, workspaceId );
			if( !workspaceDir.exists() || !workspaceDir.isDirectory() ) {
				// maybe deleted or not existing
				if( workspaceDir.mkdirs() == false ) {
					// not able to create the working directory
					LOGGER.error( "Cannot create working directory.", workspaceDir );
					throw new IOException ("Cannot create working directory!");
				}
			}
			
		}
		
		return workspaceDir;
	}

	@JsonIgnore
	public void setWorkspaceDir(File workspaceDir) {
		this.workspaceDir = workspaceDir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Date getLastseen() {
		return lastseen;
	}

	public void setLastseen(Date lastseen) {
		this.lastseen = lastseen;
	}
	
	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	@JsonIgnore
	public Map<String, String> getArchives() {
		return archives;
	}
	
	/**
	 * Returns the size in bytes of one archive in this workspace or {@code 0L} if it fails.
	 *
	 * @param archiveId the archive id
	 * @return the archive size
	 */
	@JsonIgnore
	public long getArchiveSize( String archiveId ) {
		
		long size = 0L;
		File workspaceDir = null;
		
		// get workspace dir
		try {
			workspaceDir = getWorkspaceDir();
		} catch (IOException e) {
			LOGGER.error(e, "Cannot calc Archive size!");
			return 0L;
		}
		
		File archiveFile = new File(workspaceDir, archiveId);
		if( archiveFile.exists() )
			size += archiveFile.length();
		else
			LOGGER.warn("archive ", archiveId, " in workspace ", workspaceId, " does not exists.");
		
		return size;
	}
	
	@JsonIgnore
	public synchronized Lock lockArchive( String archiveId ) throws CombineArchiveWebException {
		
		if( archives.containsKey(archiveId) == false )
			throw new CombineArchiveWebException("No such archive.");
		
		ReentrantLock archiveLock = null;
		synchronized (this) {
			archiveLock = locks.get(archiveId);
			if( archiveLock == null ) {
				archiveLock = new ReentrantLock(true);
				locks.put(archiveId, archiveLock);
			}
		}
		
		try {
			if( archiveLock.tryLock( Fields.LOCK_ARCHIVE_TIMEOUT, TimeUnit.SECONDS ) == false )
				throw new CombineArchiveWebException("Lock timeout.");
		} catch (InterruptedException e) {
			throw new CombineArchiveWebException("Lock interrupted.", e);
		}
		
		return archiveLock;
	}
}
