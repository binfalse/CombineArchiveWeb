package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.Utils;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;

// so Jackson can parse json into childs of this abstract class
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type" )
@JsonSubTypes({
	@Type(value = OmexMetaObjectDataholder.class, name = MetaObjectDataholder.TYPE_OMEX ),
	@Type(value = XmlTreeMetaObjectDataholder.class, name = MetaObjectDataholder.TYPE_XML )
})
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
	
	protected String id = "null";
	/** type of the meta information */
	protected String type = TYPE_NOTSET;
	/** setted from the client side, if anything changed. */
	protected boolean changed = false;
	
	public MetaObjectDataholder( MetaDataObject metaObject ) {
		this.metaObject = metaObject;
		generateId();
	}
	
	public MetaObjectDataholder(String id, String type, boolean changed) {
		super();
		this.id = id;
		this.type = type;
		this.changed = changed;
	}
	
	/**
	 * Updates the existing dataholder and the underlaying MetaDataObject with new information from another dataholder
	 * 
	 * @param newMetaObject
	 * @throws CombineArchiveWebException 
	 */
	@JsonIgnore
	public abstract void update( MetaObjectDataholder newMetaObject ) throws CombineArchiveWebException;
	
	/**
	 * Generates a CombineArchive MetaObject, which can be easily added to an ArchvieEntry
	 *  
	 * @return
	 */
	@JsonIgnore
	public abstract MetaDataObject getCombineArchiveMetaObject();
	
	/**
	 * Generates a temporarily id for the meta entry
	 */
	@JsonIgnore
	public void generateId() {
		
		Element xmlElement = metaObject.getXmlDescription();
		String xmlString = null;
		try {
			Document doc = new Document();
			doc.setRootElement(xmlElement.clone());
			xmlString = Utils.prettyPrintDocument( doc );
		} catch (IOException | TransformerException e) {
			LOGGER.error(e, "Can't generate xml from meta object to generate meta id");
			return;
		}
		
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
	
}
