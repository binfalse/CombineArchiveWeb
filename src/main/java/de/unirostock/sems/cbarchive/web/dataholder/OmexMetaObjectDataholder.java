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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;

public class OmexMetaObjectDataholder extends MetaObjectDataholder {
	
	private List<VCard> creators = null;
	private Date created = null;
	private List<Date> modified = null;
	private String description = null;
	
	public OmexMetaObjectDataholder(OmexMetaDataObject metaObject) {
		super(metaObject, null);
		
		OmexDescription omex = metaObject.getOmexDescription();
		type = MetaObjectDataholder.TYPE_OMEX;
		created		= omex.getCreated();
		creators	= omex.getCreators();
		modified	= omex.getModified();
		description = omex.getDescription();
	}
	
	public OmexMetaObjectDataholder(String id, String type, String description, boolean changed) {
		super(id, type, changed);
		type = MetaObjectDataholder.TYPE_OMEX;
		this.description = description;
	}
	
	public OmexMetaObjectDataholder(String id, String type, boolean changed) {
		super(id, type, changed);
		type = MetaObjectDataholder.TYPE_OMEX;
	}
	
	public OmexMetaObjectDataholder() {
		super(null, null, false);
		type = MetaObjectDataholder.TYPE_OMEX;
	}

	public Date getCreated() {
		return created;
	}
	
	public List<VCard> getCreators() {
		return creators;
	}

	public List<Date> getModified() {
		return modified;
	}
	
	public void setCreators(List<VCard> creators) {
		this.creators = creators;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public void setModified(List<Date> modified) {
		this.modified = modified;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonIgnore
	@Override
	public void update(MetaObjectDataholder newMetaObject) {
		if( newMetaObject instanceof OmexMetaObjectDataholder == false ) {
			// not the correct class type, check if data type is valid at least
			if( newMetaObject.type.equals(MetaObjectDataholder.TYPE_OMEX) == false )
				// data type is also not valid -> exception
				throw new IllegalArgumentException("Wrong data type");
		}
		
		// set new creators list
		List<VCard> newCreators = ((OmexMetaObjectDataholder) newMetaObject).getCreators();
		if( newCreators != null && newCreators.size() > 0 ) {
			creators.clear();
			creators.addAll(newCreators);
		}
		
		// got modified today.
		getModified().add( new Date() );
		
		// set description
		description = ((OmexMetaObjectDataholder) newMetaObject).getDescription();
		((OmexMetaDataObject) metaObject).getOmexDescription().setDescription( description );
	}

	@Override
	public MetaDataObject getCombineArchiveMetaObject() {
		
		// fill with some default data, if necessary
		if( created == null )
			created = new Date();
		
		if( modified == null )
			modified = new ArrayList<Date>();
		
		if( modified.isEmpty() )
			modified.add( new Date() );
		
		if( description != null && description.isEmpty() )
			description = null;
		
		this.metaObject = new OmexMetaDataObject( new OmexDescription(creators, modified, created, description) );
		// update the id
		generateId();
		return metaObject;
	}

}
