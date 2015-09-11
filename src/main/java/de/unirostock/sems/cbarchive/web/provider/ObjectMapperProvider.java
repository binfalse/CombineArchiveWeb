package de.unirostock.sems.cbarchive.web.provider;
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
		return mapper;
	}

}
