package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.jdom2.JDOMException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;

/**
 * Dataholder/Model class for CombineArchives
 * 
 * @author Martin Peters
 *
 */
// so Jersey parses this as root dataholder, if passed to create or update or something...
@XmlAccessorType(XmlAccessType.FIELD)
public class Archive {
	protected String id;
	protected String name;
	
	@JsonInclude(Include.NON_NULL) 
	protected Map<String, ArchiveEntryDataholder> entries = null;
	
	protected List<MetaObjectDataholder> meta = null;

	@JsonIgnore
	private CombineArchive archive;
	@JsonIgnore
	private File archiveFile;

	public Archive(String id, String name, File file) throws CombineArchiveWebException {
		this.id = id;
		this.name = name;
		this.archiveFile = file;
		if( file != null )
			setArchiveFile(file);

	}
	
	public Archive() {
		this.id = null;
		this.name = null;
		this.archive = null;
		this.archiveFile = null;
	}
	
	public Archive(String name) {
		this.id = null;
		this.name = name;
		this.archive = null;
		this.archiveFile = null;
	}
	
	public Archive(String id, String name) {
		this.id = id;
		this.name = name;
		this.archive = null;
		this.archiveFile = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Map<String, ArchiveEntryDataholder> getEntries() {
		return entries;
	}

	@JsonIgnore
	public void setArchiveFile(File file) throws CombineArchiveWebException {
		
		if( !file.exists() || !file.isFile() ) {
			LOGGER.error( MessageFormat.format("The archive is not accesible: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not accesible");
		}
		
		this.archiveFile = file;

		try {
			archive = new CombineArchive(file);
		} catch (IOException | JDOMException | ParseException | CombineArchiveException e) {
			LOGGER.error(e, MessageFormat.format("The archive is not parsable: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not parsable", e);
		}
		
		
		// new entry list
		this.entries = new HashMap<String, ArchiveEntryDataholder>();
		
		// gather information
		Collection<ArchiveEntry> archiveEntries = archive.getEntries();
		ArchiveEntry mainEntry = archive.getMainEntry();
		
		// iterate over entries
		for( ArchiveEntry entry : archiveEntries ) {
			
			// entry information are gathered in the entry dataholder
			ArchiveEntryDataholder dataholder = new ArchiveEntryDataholder(entry);
			if( entry == mainEntry )
				dataholder.setMaster(true);
			
			// put it into the map
			this.entries.put(dataholder.getFilePath(), dataholder);
		}
		
		// add the archive meta information as root entry
		ArchiveEntryDataholder rootDataholder = new ArchiveEntryDataholder(archive, false, "/", "", "");
		rootDataholder.updateId();
		this.entries.put("/", rootDataholder );
		
	}
	
	@JsonIgnore
	public File getArchiveFile() {
		return archiveFile;
	}
	
	@JsonIgnore
	public CombineArchive getArchive() {
		return archive;
	}
	
	@JsonIgnore
	public ArchiveEntry addArchiveEntry(String fileName, Path file) throws CombineArchiveWebException, IOException {
		
		if( archive == null ) {
			LOGGER.error( "The archive was not opened" );
			throw new CombineArchiveWebException("The archive was not opened");
		}
		
		// make sure file name is not taken yet
		String altFileName = fileName;
		int i = 1;
		while( archive.getEntry(altFileName) != null ) {
			i++;
			int extensionPoint = fileName.lastIndexOf( '.' );
			String extension = fileName.substring( extensionPoint );
			String pureName = fileName.substring( 0, extensionPoint );
			
			altFileName = pureName + "-" + String.valueOf(i) + extension;
		}
		fileName = altFileName;
		
		// add the entry
		return archive.addEntry(file.toFile(), fileName, Files.probeContentType(file));
	}


}
