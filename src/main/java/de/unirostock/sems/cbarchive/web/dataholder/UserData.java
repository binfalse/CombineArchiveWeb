package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unirostock.sems.cbarchive.meta.omex.VCard;

@XmlAccessorType(XmlAccessType.FIELD)
public class UserData {
	
	protected String givenName;
	protected String familyName;
	protected String email;
	protected String organization;
	
	public UserData() {
		super();
		this.givenName = null;
		this.familyName = null;
		this.email = null;
		this.organization = null;
	}
	
	public UserData(String givenName, String familyName, String email, String organization) {
		super();
		this.givenName = givenName;
		this.familyName = familyName;
		this.email = email;
		this.organization = organization;
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
	public String getEMail() {
		return email;
	}
	public void setEMail(String email) {
		this.email = email;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	
	@JsonIgnore
	public boolean hasInformation ()
	{
		return givenName != null && familyName != null && email != null 
			&& givenName.isEmpty() == false && familyName.isEmpty() == false && email.isEmpty() == false && email.length () > 5;
	}
	
	@JsonIgnore
	public VCard getVCard() {
		return new VCard(familyName, givenName, email, organization);
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
