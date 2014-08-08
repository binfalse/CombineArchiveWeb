package de.unirostock.sems.cbarchive.web.provider;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
	
	private ObjectMapper mapper = null;
	
	@Override
	public ObjectMapper getContext(Class<?> type) {
		
		if( mapper == null ) {
			mapper = new ObjectMapper();
			
		}
		
		return mapper;
	}

}
