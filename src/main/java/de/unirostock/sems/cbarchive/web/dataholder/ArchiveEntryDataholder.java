package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;

public class ArchiveEntryDataholder {
	
	private ArchiveEntry archiveEntry;
	private boolean master = false;
	
	protected String filePath;
	protected String fileName;
	protected String format;
	protected List<MetaObjectDataholder> meta = new ArrayList<MetaObjectDataholder>();
	
	public ArchiveEntryDataholder( ArchiveEntry archiveEntry ) {
		this.archiveEntry = archiveEntry;
		
		// imports data to dataholder
		filePath	= archiveEntry.getFilePath();
		fileName	= archiveEntry.getFileName();
		format		= archiveEntry.getFormat();
		for (MetaDataObject metaObject : archiveEntry.getDescriptions ()) {
			meta.add( MetaObjectDataholder.construct(metaObject) );
		}
			
	}
	
	@JsonIgnore
	public ArchiveEntry getArchiveEntry() {
		return archiveEntry;
	}
	
	public MetaObjectDataholder addMetaEntry( MetaObjectDataholder metaObject ) {
		
		MetaObjectDataholder enrichedMetaObject = null;
		
		switch( metaObject.getType() ) {
		case MetaObjectDataholder.TYPE_OMEX:
			enrichedMetaObject = new OmexMetaObjectDataholder(metaObject, archiveEntry);
			break;
			
		case MetaObjectDataholder.TYPE_XML:
			enrichedMetaObject = new XmlTreeMetaObjectDataholder(metaObject, archiveEntry);
			break;
		}
		
		if( enrichedMetaObject != null ) {
			archiveEntry.addDescription(enrichedMetaObject.getMetaObject());
		}
		
		return enrichedMetaObject;
	}

	/* -------- Dataholder Getter/Setter -------- */
	
	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFormat() {
		return format;
	}

	public List<MetaObjectDataholder> getMeta() {
		return meta;
	}
}
