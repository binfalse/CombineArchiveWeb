package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;

public class OmexMetaObjectDataholder extends MetaObjectDataholder {
	
	public static final String FIELD_CREATED = "created";
	public static final String FIELD_CREATORS = "creators";
	public static final String FIELD_MODIFIED = "modified";
	
	public OmexMetaObjectDataholder(OmexMetaDataObject metaObject) {
		super(metaObject);
		
		OmexDescription omex = metaObject.getOmexDescription();
		
		type = MetaObjectDataholder.TYPE_OMEX;
		fields.put(FIELD_CREATED, omex.getCreated());
		fields.put(FIELD_CREATORS, omex.getCreators());
		fields.put(FIELD_MODIFIED, omex.getModified());
	}
	
	public OmexMetaObjectDataholder(MetaObjectDataholder metaObject, ArchiveEntry archiveEntry) {
		super(null);
		
		// TODO
	}

	@JsonIgnore
	public Date getCreated() {
		return (Date) fields.get(FIELD_CREATED);
	}
	
	@JsonIgnore
	public List<VCard> getCreators() {
		return (List<VCard>) fields.get(FIELD_CREATORS);
	}

	@JsonIgnore
	public List<Date> getModified() {
		return (List<Date>) fields.get(FIELD_MODIFIED);
	}

	@Override
	public void update(MetaObjectDataholder newMetaObject) {
		if( newMetaObject instanceof OmexMetaObjectDataholder == false ) {
			// not the correct class type, check if data type is valid at least
			if( newMetaObject.type.equals(MetaObjectDataholder.TYPE_OMEX) == false )
				// data type is also not valid -> exception
				throw new IllegalArgumentException("Wrong data type");
		}
		
		// get field map
		Map<String, Object> newFields = newMetaObject.getAny();
		
		// check type for creators list
		try {
			List<Map<String, String>> tempVcard = (List<Map<String, String>>) newFields.get(FIELD_CREATORS);
			List<VCard> newCreators = new ArrayList<VCard>();
			
			for( Map<String, String> stringVCard : tempVcard ) {
				newCreators.add( new VCard(
						stringVCard.get("familyName"),
						stringVCard.get("givenName"),
						stringVCard.get("email"),
						stringVCard.get("organization")
					) );
			}
			
			// apply
			fields.put(FIELD_CREATORS, newCreators);
		}
		catch (ClassCastException e) {
			LOGGER.error(e, "Can not cast string VCard Object into List/Map");
			return;
		}
		
		// got modified today.
		getModified().add( new Date() );
	}

}
