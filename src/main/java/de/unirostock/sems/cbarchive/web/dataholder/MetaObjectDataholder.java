package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.web.Tools;

abstract public class MetaObjectDataholder {
	
	public final static String TYPE_NOTSET = "na";
	public final static String TYPE_OMEX = "omex";
	public final static String TYPE_XML = "xmltree";
	
	
	public static MetaObjectDataholder construct( MetaDataObject metaObject ) {
		MetaObjectDataholder dataholder = null;
		
		if( metaObject instanceof OmexMetaDataObject ) {
			dataholder = new OmexMetaObjectDataholder((OmexMetaDataObject) metaObject);
		}
		else {
			dataholder = new XmlTreeMetaObjectDataholder(metaObject);
		}
		
		return dataholder;
	}
	
	@JsonIgnore
	protected MetaDataObject metaObject = null;
	
	@JsonIgnore
	protected Map<String, Object> fields = new HashMap<String, Object>();
	
	protected String id = "null";
	/** type of the meta information */
	protected String type = TYPE_NOTSET;
	/** setted from the client side, if anything changed. */
	protected boolean changed = false;
	
	public MetaObjectDataholder( MetaDataObject metaObject ) {
		this.metaObject = metaObject;
		generateId();
	}
	
	/**
	 * Updates the existing dataholder and the underlaying MetaDataObject with new information from another dataholder
	 * 
	 * @param newMetaObject
	 */
	public abstract void update( MetaObjectDataholder newMetaObject );
	
	/**
	 * Generates a temporarily id for the meta entry
	 */
	private void generateId() {
		
		Element xmlElement = metaObject.getXmlDescription();
		String xmlString = new XMLOutputter().outputString(xmlElement);
		
		id = Tools.generateHashId(xmlString);
	}

	public String getId() {
		return id;
	}

	public MetaDataObject getMetaObject() {
		return metaObject;
	}

	public String getType() {
		return type;
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	
	@JsonAnyGetter
	public Map<String, Object> getAny() {
		return fields;
	}
	@JsonAnySetter
	public void setAny(String name, Object value) {
		fields.put(name, value);
	}
	
}
