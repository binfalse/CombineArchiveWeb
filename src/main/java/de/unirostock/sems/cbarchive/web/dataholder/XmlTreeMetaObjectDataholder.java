package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.Utils;
import de.unirostock.sems.cbarchive.meta.DefaultMetaDataObject;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;

public class XmlTreeMetaObjectDataholder extends MetaObjectDataholder {

	private Element xmlTree = null;
	
	public XmlTreeMetaObjectDataholder(MetaDataObject metaObject) {
		super(metaObject);
		type = MetaObjectDataholder.TYPE_XML;
		xmlTree = metaObject.getXmlDescription();
	}
	
	public XmlTreeMetaObjectDataholder(String id, String type, boolean changed) {
		super(id, type, changed);
	}
	
	public XmlTreeMetaObjectDataholder() {
		super(null, null, false);
	}
	
	@JsonIgnore
	public Element getXmlTree() {
		return xmlTree;
	}
	
	@JsonIgnore
	public void setXmlTree(Element xmlTree) {
		this.xmlTree = xmlTree;
	}
	
	public String getXmlString() {
		try {
			Document doc = new Document().setRootElement(xmlTree.clone());
			return Utils.prettyPrintDocument( doc );
		} catch (IOException | TransformerException e) {
			LOGGER.error(e, "Cannot transform Xml Element to String.");
			return "";
		}
	}
	
	public void setXmlString() {
		// TODO
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
		Element newXmltree = ((XmlTreeMetaObjectDataholder) newMetaObject).getXmlTree();
		if( newXmltree != null )
			xmlTree = newXmltree;
		
	}

	@Override
	public MetaDataObject getCombineArchiveMetaObject() {
		this.metaObject = new DefaultMetaDataObject(xmlTree);
		// update the id
		generateId();
		return metaObject;
	}

}
