package de.unirostock.sems.cbarchive.web.provider;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.binfalse.bflog.LOGGER;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
	
	/** ObjectMapper Cache */
	private ObjectMapper mapper = null;
	
	public ObjectMapperProvider() {
		LOGGER.info("create new ObjectMapper");
		mapper = new ObjectMapper();
		
		// add MixIn types
		mapper.addMixInAnnotations(de.unirostock.sems.cbarchive.meta.omex.VCard.class, de.unirostock.sems.cbarchive.web.dataholder.VCardMixIn.class);
	}
	
	@Override
	public ObjectMapper getContext(Class<?> type) {
		LOGGER.info("returns ObjectMapper");
		return mapper;
	}

}
