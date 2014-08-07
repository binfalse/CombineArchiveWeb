package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;

public class OmexMetaObjectDataholder extends MetaObjectDataholder {
	
	private List<VCardDataholder> creators = null;
	private Date created = null;
	private List<Date> modified = null;
	
	public OmexMetaObjectDataholder(OmexMetaDataObject metaObject) {
		super(metaObject);
		
		OmexDescription omex = metaObject.getOmexDescription();
		
		type = MetaObjectDataholder.TYPE_OMEX;
		created		= omex.getCreated();
		creators	= VCardDataholder.convertVCardList( omex.getCreators() );
		modified	= omex.getModified();
	}
	
	public OmexMetaObjectDataholder(MetaObjectDataholder metaObject, ArchiveEntry archiveEntry) {
		super(null);
		
		// TODO
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
	
	public List<VCardDataholder> getCreators() {
		return creators;
	}

	public List<Date> getModified() {
		return modified;
	}
	
	public void setCreators(List<VCardDataholder> creators) {
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
		List<VCardDataholder> newCreators = ((OmexMetaObjectDataholder) newMetaObject).getCreators();
		if( newCreators != null && newCreators.size() > 0 )
			creators = newCreators;
		
		// got modified today.
		getModified().add( new Date() );
	}

}
