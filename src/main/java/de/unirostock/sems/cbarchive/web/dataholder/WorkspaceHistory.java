package de.unirostock.sems.cbarchive.web.dataholder;

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

public class WorkspaceHistory {
	
	private List<String> recentWorkspaces = new ArrayList<String>();
	@JsonInclude(Include.NON_NULL) 
	private String currentWorkspace = null;
	
	public WorkspaceHistory(List<String> recentWorkspaces, String currentWorkspace) {
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

	public List<String> getRecentWorkspaces() {
		return recentWorkspaces;
	}

	public void setRecentWorkspaces(List<String> recentWorkspaces) {
		this.recentWorkspaces = recentWorkspaces;
	}
	
	/**
	 * Removes all not existing workspaces from the history
	 */
	@JsonIgnore
	public void cleanUpHistory() {
		WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

		Iterator<String> iter = recentWorkspaces.iterator();
		while( iter.hasNext() ) {
			String elem = iter.next();
			if( workspaceManager.hasWorkspace(elem) == false )
				iter.remove();
		}
			
	}
	
	@JsonIgnore
	public String toCookieJson() throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString( (List<String>) recentWorkspaces );
		
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
		
		return new WorkspaceHistory(recent, null);
	}
	
	
}
