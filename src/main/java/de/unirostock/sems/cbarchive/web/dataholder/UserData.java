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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Tools;

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
	
	/**
	 * Checks if the current user is contained in the given list.
	 *
	 * @param list the list
	 * @return true, if this vcard exists in list
	 */
	@JsonIgnore
	public boolean isContained( List<VCard> list ) {
		
		return Tools.containsVCard(list, getVCard());
	}
	
	@JsonIgnore
	public String toJson() throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString( (UserData) this);
		
		json = Base64.encodeBase64URLSafeString( json.getBytes() );
		return json;
	}
	
	@JsonIgnore
	public static UserData fromJson( String json ) throws JsonParseException, JsonMappingException, IOException {
		
		if( json == null )
			return null;
		
		json = new String( Base64.decodeBase64(json) );
		
		ObjectMapper mapper = new ObjectMapper();
		UserData result = mapper.readValue(json, UserData.class);
		return result;
	}
	
}
