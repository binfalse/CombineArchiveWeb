package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.MetaDataHolder;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.web.Tools;

public class ArchiveEntryDataholder {

	@JsonIgnore
	private ArchiveEntry archiveEntry = null;
	@JsonIgnore
	private MetaDataHolder metaDataHolder = null;
	
	private boolean master = false;

	protected String id;
	protected String filePath;
	protected String fileName;
	protected long fileSize;
	protected URI format;
	protected List<MetaObjectDataholder> meta = new ArrayList<MetaObjectDataholder>();

	public ArchiveEntryDataholder( ArchiveEntry archiveEntry ) {
		this.archiveEntry		= archiveEntry;
		this.metaDataHolder		= archiveEntry;

		// imports data to dataholder
		filePath	= archiveEntry.getFilePath();
		fileName	= archiveEntry.getFileName();
		format		= archiveEntry.getFormat();
		id			= Tools.generateHashId(filePath);
		
		try {
			fileSize	= Files.size( archiveEntry.getPath() );
		} catch (IOException e) {
			LOGGER.warn(e, "Cannot determine file size for ", filePath);
			fileSize	= 0;
		}
		
		copyMetaData(archiveEntry);
	}

	public ArchiveEntryDataholder( MetaDataHolder metaDataHolder, boolean master, String filePath, String fileName, URI format) {

		this.archiveEntry	= null;
		this.metaDataHolder	= metaDataHolder;
		
		this.master			= master;
		this.filePath		= filePath;
		this.fileName		= fileName;
		this.format			= format;

		copyMetaData(metaDataHolder);
	}

	public ArchiveEntryDataholder(boolean master, String id, String filePath, String fileName, URI format, List<MetaObjectDataholder> meta) {
		super();
		this.master = master;
		this.id = id;
		this.filePath = filePath;
		this.fileName = fileName;
		this.format = format;
		this.meta = meta;
	}

	public ArchiveEntryDataholder() {
		super();
		this.master = false;
		this.id = null;
		this.filePath = null;
		this.fileName = null;
		this.format = null;
	}

	protected void copyMetaData( MetaDataHolder metaDataHolder ) {

		meta.clear();
		for (MetaDataObject metaObject : metaDataHolder.getDescriptions ()) {
			meta.add( MetaObjectDataholder.construct(metaObject, this) );
		}

	}

	@JsonIgnore
	public void updateId() {
		id = Tools.generateHashId(filePath);
	}

	@JsonIgnore
	public ArchiveEntry getArchiveEntry() {
		return archiveEntry;
	}

	public void addMetaEntry( MetaObjectDataholder metaObject ) {

		// add the metaObject to the combineArchiveEntry...
		metaDataHolder.addDescription( metaObject.getCombineArchiveMetaObject() );
		// ...and to the internal handler
		meta.add(metaObject);

	}

	/* -------- Dataholder Getter/Setter -------- */

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public String getId() {
		return id;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
	}
	
	public URI getFormat() {
		return format;
	}
	
	public long getFileSize() {
		return fileSize;
	}

	public void setFilePath(String filePath) {
		if( archiveEntry == null )
			this.filePath = filePath;
	}

	public void setFileName(String fileName) {
		if( archiveEntry == null )
			this.fileName = fileName;
	}

	public void setFormat(URI format) {
		if( archiveEntry == null )
			this.format = format;
	}

	public List<MetaObjectDataholder> getMeta() {
		return meta;
	}
}
