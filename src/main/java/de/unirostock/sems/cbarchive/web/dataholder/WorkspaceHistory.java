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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unirostock.sems.cbarchive.web.WorkspaceManager;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;

public class WorkspaceHistory {
	
	private List<Workspace> recentWorkspaces = null;
	@JsonInclude(Include.NON_NULL) 
	private String currentWorkspace = null;
	
	public WorkspaceHistory(List<Workspace> recentWorkspaces, String currentWorkspace) {
		super();
		this.recentWorkspaces = recentWorkspaces;
		if( this.recentWorkspaces == null )
			this.recentWorkspaces = new ArrayList<Workspace>();
		
		this.currentWorkspace = currentWorkspace;
		if( this.currentWorkspace == null || this.currentWorkspace.isEmpty() ) {
			// finding the current
			this.currentWorkspace = null;
			for( Workspace elem : this.recentWorkspaces ) {
				if( this.currentWorkspace == null && elem.isCurrent() )
					this.currentWorkspace = elem.getWorkspaceId();
				else if( this.currentWorkspace != null && elem.isCurrent() )
					elem.setCurrent(false);
			}
		}
		else {
			setCurrentWorkspace(this.currentWorkspace);
		}
			
		cleanUpHistory();
	}

	public WorkspaceHistory() {
		this( null, null );
	}
	
	public WorkspaceHistory( List<Workspace> recentWorkspaces ) {
		this( recentWorkspaces, null );
	}

	public String getCurrentWorkspace() {
		return currentWorkspace;
	}

	public void setCurrentWorkspace(String currentWorkspace) {
		
		if( this.currentWorkspace.equals(currentWorkspace) )
			return;
		
		this.currentWorkspace = currentWorkspace;
		
		// finding the current
		for( Workspace elem : this.recentWorkspaces ) {
			if( elem.getWorkspaceId().equals(this.currentWorkspace) )
				elem.setCurrent(true);
			else
				elem.setCurrent(false);
		}
	}

	public List<Workspace> getRecentWorkspaces() {
		return recentWorkspaces;
	}

	/**
	 * Removes all not existing workspaces from the history
	 */
	@JsonIgnore
	public void cleanUpHistory() {
		WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

		Iterator<Workspace> iter = recentWorkspaces.iterator();
		while( iter.hasNext() ) {
			Workspace elem = iter.next();
			if( workspaceManager.hasWorkspace(elem.getWorkspaceId()) == false )
				iter.remove();
		}
			
	}
	
	@JsonIgnore
	public boolean containsWorkspace(String workspaceId) {
		
		if( currentWorkspace.equals(workspaceId) )
			return true;
		
		for( Workspace elem : recentWorkspaces) {
			if( workspaceId.equals(elem.getWorkspaceId()) )
				return true;
		}
		
		return false;
	}
	
	public Workspace getWorkspace(String workspaceId) throws CombineArchiveWebException {
		
		for( Workspace elem : recentWorkspaces) {
			if( workspaceId.equals(elem.getWorkspaceId()) )
				return elem;
		}
		
		throw new CombineArchiveWebException("No such workspace available");
	}
	
	@JsonIgnore
	public String toCookieJson() throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString( recentWorkspaces );
		
		json = Base64.encodeBase64URLSafeString( json.getBytes() );
		return json;
	}
	
	@JsonIgnore
	public static WorkspaceHistory fromCookieJson( String json ) throws JsonParseException, JsonMappingException, IOException {
		
		if( json == null )
			return null;
		
		json = new String( Base64.decodeBase64(json) );
		
		ObjectMapper mapper = new ObjectMapper();
		List<Workspace> recent = mapper.readValue(json, new TypeReference<List<Workspace>>(){} );
		List<Workspace> result = new ArrayList<Workspace>();
		
		WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
		
		
		// looking up the names
		for( Workspace elem : recent ) {
			Workspace workspace = workspaceManager.getWorkspace(elem.getWorkspaceId());
			if( workspace != null )
				result.add(workspace);
		}
		
		return new WorkspaceHistory(result);
	}
	
	
}
