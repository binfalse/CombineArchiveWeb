package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.Tools;

public class Workspace {
	
	private String workspaceId = null;
	private String name = null;
	private Date lastseen = null;
	private File workspaceDir = null;
	private Map<String, String> archives = new HashMap<String, String>();
	
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
	
	public void updateLastseen() {
		this.lastseen = new Date();
	}
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

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

	public Map<String, String> getArchives() {
		return archives;
	}
	
}
