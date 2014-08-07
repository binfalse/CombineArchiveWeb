package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.meta.omex.VCard;

/**
 * Just an wrapper class, which adds an empty constructor for Jackson
 * 
 * @author martin
 *
 */
public class VCardDataholder extends VCard {
	
	public VCardDataholder(Element element) {
		super(element);
	}

	public VCardDataholder(String familyName, String givenName, String email, String organization) {
		super(familyName, givenName, email, organization);
	}
	
	public VCardDataholder( VCard vcard ) {
		super( vcard.getFamilyName(), vcard.getGivenName(), vcard.getEmail(), vcard.getOrganization() );
	}
	
	public VCardDataholder() {
		super(null, null, null, null);
	}
	
	// Overwrites the method from super class only to annotate with @JsonIgnore
	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return super.isEmpty();
	}
	
	/**
	 * creates a list of VCard Dataholder from a simple VCard list
	 * 
	 * @param vcards
	 * @return
	 */
	public static List<VCardDataholder> convertVCardList( List<VCard> vcards ) {
		List<VCardDataholder> result = new ArrayList<VCardDataholder>();
		
		for( VCard card : vcards ) {
			result.add( new VCardDataholder(card) );
		}
		
		return result;
	}

}
