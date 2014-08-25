package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

public class WorkspaceHistory {
	
	private Map<String, String> recentWorkspaces = new HashMap<String, String>();
	@JsonInclude(Include.NON_NULL) 
	private String currentWorkspace = null;
	
	public WorkspaceHistory(Map<String, String> recentWorkspaces, String currentWorkspace) {
		super();
		this.recentWorkspaces = recentWorkspaces;
		this.currentWorkspace = currentWorkspace;
		
		cleanUpHistory();
	}

	public WorkspaceHistory() {
		super();
	}

	public String getCurrentWorkspace() {
		return currentWorkspace;
	}

	public void setCurrentWorkspace(String currentWorkspace) {
		this.currentWorkspace = currentWorkspace;
	}

	public Map<String, String> getRecentWorkspaces() {
		return recentWorkspaces;
	}

	public void setRecentWorkspaces(Map<String, String> recentWorkspaces) {
		this.recentWorkspaces = recentWorkspaces;
	}
	
	/**
	 * Removes all not existing workspaces from the history
	 */
	@JsonIgnore
	public void cleanUpHistory() {
		WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

		Iterator<String> iter = recentWorkspaces.keySet().iterator();
		while( iter.hasNext() ) {
			String elem = iter.next();
			if( workspaceManager.hasWorkspace(elem) == false )
				iter.remove();
		}
			
	}
	
	@JsonIgnore
	public String toCookieJson() throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString( (List<String>) new ArrayList<String>( recentWorkspaces.keySet() ) );
		
		json = Base64.encodeBase64URLSafeString( json.getBytes() );
		return json;
	}
	
	@JsonIgnore
	public static WorkspaceHistory fromCookieJson( String json ) throws JsonParseException, JsonMappingException, IOException {
		
		if( json == null )
			return null;
		
		json = new String( Base64.decodeBase64(json) );
		
		ObjectMapper mapper = new ObjectMapper();
		List<String> recent = mapper.readValue(json, new TypeReference<List<String>>(){} );
		
		WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
		WorkspaceHistory result = new WorkspaceHistory();
		
		// looking up the names
		for( String elem : recent ) {
			Workspace workspace = workspaceManager.getWorkspace(elem);
			if( workspace != null )
				result.getRecentWorkspaces().put(workspace.getWorkspaceId(), workspace.getName());
		}
		
		return result;
	}
	
	
}