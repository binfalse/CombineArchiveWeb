package de.unirostock.sems.cbarchive.web.dataholder;

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
	
	public OmexMetaObjectDataholder(OmexMetaDataObject metaObject) {
		super(metaObject);
		
		OmexDescription omex = metaObject.getOmexDescription();
		
		type = MetaObjectDataholder.TYPE_OMEX;
		created		= omex.getCreated();
		creators	= omex.getCreators();
		modified	= omex.getModified();
	}
	
	public OmexMetaObjectDataholder(String id, String type, boolean changed) {
		super(id, type, changed);
	}
	
	public OmexMetaObjectDataholder() {
		super(null, null, false);
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
		
		this.metaObject = new OmexMetaDataObject( new OmexDescription(creators, modified, created) );
		// update the id
		generateId();
		return metaObject;
	}

}
