package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.Map;

import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;

public class XmlTreeMetaObjectDataholder extends MetaObjectDataholder {

	public static final String FIELD_XML_DESCRIPTION = "xmltree";
	
	public XmlTreeMetaObjectDataholder(MetaDataObject metaObject) {
		super(metaObject);
		type = MetaObjectDataholder.TYPE_XML;
		fields.put( FIELD_XML_DESCRIPTION, metaObject.getXmlDescription() );
	}
	
	public XmlTreeMetaObjectDataholder(MetaObjectDataholder metaObject, ArchiveEntry archiveEntry) {
		super(null);
		//TODO
	}

	@JsonIgnore
	public Element getXmlDescrition() {
		return (Element) fields.get(FIELD_XML_DESCRIPTION);
	}

	@Override
	public void update(MetaObjectDataholder newMetaObject) {
		
		if( newMetaObject instanceof XmlTreeMetaObjectDataholder == false ) {
			// not the correct class type, check if data type is valid at least
			if( newMetaObject.type.equals(MetaObjectDataholder.TYPE_XML) == false )
				// data type is also not valid -> exception
				throw new IllegalArgumentException("Wrong data type");
		}
		
		// get field map
		Map<String, Object> newFields = newMetaObject.getAny();
		
		// apply changes
		fields.put( FIELD_XML_DESCRIPTION, newFields.get(FIELD_XML_DESCRIPTION) );
		
	}

}
