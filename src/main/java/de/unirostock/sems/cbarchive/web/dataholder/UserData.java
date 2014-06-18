package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserData {
	
	protected String givenName;
	protected String familyName;
	protected String mail;
	protected String organization;
	
	@JsonIgnore
	protected String path;
	
	public UserData() {
		super();
		this.givenName = null;
		this.familyName = null;
		this.mail = null;
		this.organization = null;
		this.path = null;
	}
	
	public UserData(String givenName, String familyName, String mail, String organization, String path) {
		super();
		this.givenName = givenName;
		this.familyName = familyName;
		this.mail = mail;
		this.organization = organization;
		this.path = path;
	}
	
	public UserData(String givenName, String familyName, String mail, String organization) {
		super();
		this.givenName = givenName;
		this.familyName = familyName;
		this.mail = mail;
		this.organization = organization;
		this.path = null;
	}

	public String getGivenName() {
		return givenName;
	}
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	public String getFamilyName() {
		return familyName;
	}
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	@JsonIgnore
	public String getPath() {
		return path;
	}
	@JsonIgnore
	public void setPath(String path) {
		this.path = path;
	}
	
	@JsonIgnore
	public String toJson() throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString( (UserData) this);
	}
	
	@JsonIgnore
	public static UserData fromJson( String json ) throws JsonParseException, JsonMappingException, IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, UserData.class);
	}
}
