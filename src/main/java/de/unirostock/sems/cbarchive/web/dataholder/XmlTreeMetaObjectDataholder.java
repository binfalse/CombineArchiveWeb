package de.unirostock.sems.cbarchive.web.dataholder;

import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.DefaultMetaDataObject;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;

public class XmlTreeMetaObjectDataholder extends MetaObjectDataholder {

	private Element xmltree = null;
	
	public XmlTreeMetaObjectDataholder(MetaDataObject metaObject) {
		super(metaObject);
		type = MetaObjectDataholder.TYPE_XML;
		xmltree = metaObject.getXmlDescription();
	}
	
	public XmlTreeMetaObjectDataholder(String id, String type, boolean changed) {
		super(id, type, changed);
	}
	
	public XmlTreeMetaObjectDataholder() {
		super(null, null, false);
	}

	public Element getXmltree() {
		return xmltree;
	}

	public void setXmltree(Element xmltree) {
		this.xmltree = xmltree;
	}

	@JsonIgnore
	@Override
	public void update(MetaObjectDataholder newMetaObject) {
		
		if( newMetaObject instanceof XmlTreeMetaObjectDataholder == false ) {
			// not the correct class type, check if data type is valid at least
			if( newMetaObject.type.equals(MetaObjectDataholder.TYPE_XML) == false )
				// data type is also not valid -> exception
				throw new IllegalArgumentException("Wrong data type");
		}
		
		// apply changes
		Element newXmltree = ((XmlTreeMetaObjectDataholder) newMetaObject).getXmltree();
		if( newXmltree != null )
			xmltree = newXmltree;
		
	}

	@Override
	public MetaDataObject getCombineArchiveMetaObject() {
		this.metaObject = new DefaultMetaDataObject(xmltree);
		// update the id
		generateId();
		return metaObject;
	}

}
