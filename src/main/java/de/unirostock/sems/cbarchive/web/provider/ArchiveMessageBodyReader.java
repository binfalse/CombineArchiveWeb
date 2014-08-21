package de.unirostock.sems.cbarchive.web.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromExisting;

public class ArchiveMessageBodyReader implements MessageBodyReader<ArchiveFromExisting> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == ArchiveFromExisting.class;
	}

	@Override
	public ArchiveFromExisting readFrom(Class<ArchiveFromExisting> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

}
