package de.unirostock.sems.cbarchive.web.dataholder;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;

public class ArchiveEntryDataholder {
	
	private ArchiveEntry archiveEntry;
	private boolean master = false;
	
	protected String filePath;
	protected String fileName;
	protected String format;
	protected List<Element> meta = new ArrayList<Element>();
	
	public ArchiveEntryDataholder( ArchiveEntry archiveEntry ) {
		this.archiveEntry = archiveEntry;
		
		// imports data to dataholder
		filePath	= archiveEntry.getFilePath();
		fileName	= archiveEntry.getFileName();
		format		= archiveEntry.getFormat();
		for (MetaDataObject m : archiveEntry.getDescriptions ())
			meta.add( m.getXmlDescription() );
		
	}
	
	@JsonIgnore
	public ArchiveEntry getArchiveEntry() {
		return archiveEntry;
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

	public List<Element> getMeta() {
		return meta;
	}
}
