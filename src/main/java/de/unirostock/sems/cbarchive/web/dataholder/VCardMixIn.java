package de.unirostock.sems.cbarchive.web.dataholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * MixIn Type for the CombineArchive VCard class
 * to add annotations for Jackson
 * 
 */
public abstract class VCardMixIn {
	@JsonIgnoreProperties(ignoreUnknown=true)
	
	@JsonIgnore
	abstract boolean isEmpty();
}
