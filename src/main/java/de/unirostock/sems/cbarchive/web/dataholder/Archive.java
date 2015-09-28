package de.unirostock.sems.cbarchive.web.dataholder;
/*
CombineArchiveWeb - a WebInterface to read/create/write/manipulate/... COMBINE archives
Copyright (C) 2014  SEMS Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;
import de.unirostock.sems.cbext.Formatizer;

/**
 * Dataholder/Model class for CombineArchives
 * 
 * @author Martin Peters
 *
 */
// so Jersey parses this as root dataholder, if passed to create or update or something...
@XmlAccessorType(XmlAccessType.FIELD)
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "template" )
@JsonSubTypes({
	@Type( value = Archive.class, name = Archive.TEMPLATE_PLAIN ),
	@Type( value = ArchiveFromGit.class, name = Archive.TEMPLATE_GIT ),
	@Type( value = ArchiveFromHttp.class, name = Archive.TEMPLATE_HTTP ),
	@Type( value = ArchiveFromExisting.class, name = Archive.TEMPLATE_EXISTING )
})
public class Archive implements Closeable {
	
	public static final String TEMPLATE_PLAIN		= "plain";
	public static final String TEMPLATE_HG 			= "hg";
	public static final String TEMPLATE_GIT			= "git";
	public static final String TEMPLATE_HTTP		= "http";
	public static final String TEMPLATE_EXISTING	= "existing";
	
	protected String template	= TEMPLATE_PLAIN;
	protected String id			= null;
	protected String name		= null;
	
	@JsonInclude(Include.NON_NULL) 
	protected Map<String, ArchiveEntryDataholder> entries = null;
	protected List<MetaObjectDataholder> meta = null;

	@JsonIgnore
	private CombineArchive archive	= null;
	@JsonIgnore
	private File archiveFile		= null;
	@JsonIgnore
	protected Lock lock				= null;

	public Archive(String id, String name, File file, Lock lock) throws CombineArchiveWebException {
		this.id = id;
		this.name = name;
		this.archiveFile = file;
		this.lock = lock;
		if( file != null )
			setArchiveFile(file, lock);
	}
	
	public Archive() {
		// ...
	}
	
	public Archive(String name) {
		this.name = name;
	}
	
	public Archive(String id, String name) {
		this.id = id;
		this.name = name;
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
	
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	@JsonIgnore
	public void setArchiveFile(File file, Lock lock) throws CombineArchiveWebException {
		
		if( !file.exists() || !file.isFile() ) {
			LOGGER.error( MessageFormat.format("The archive is not accesible: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not accesible");
		}
		
		this.archiveFile	= file;
		this.lock 			= lock;
		
		try {
			archive = new CombineArchive(file);
			LOGGER.debug("opening ", file.getAbsolutePath());
		} catch (IOException | JDOMException | ParseException | CombineArchiveException e) {
			LOGGER.error(e, MessageFormat.format("The archive is not parsable: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not parsable", e);
		}
		
		
		// new entry list
		this.entries = new HashMap<String, ArchiveEntryDataholder>();
		
		// gather information
		Collection<ArchiveEntry> archiveEntries = archive.getEntries();
		List<ArchiveEntry> mainEntries = archive.getMainEntries();
		
		// iterate over entries
		for( ArchiveEntry entry : archiveEntries ) {
			
			// entry information are gathered in the entry dataholder
			ArchiveEntryDataholder dataholder = new ArchiveEntryDataholder(entry);
			if( mainEntries.contains(entry) )
				dataholder.setMaster(true);
			
			// put it into the map
			this.entries.put(dataholder.getFilePath(), dataholder);
		}
		
		// add the archive meta information as root entry
		ArchiveEntryDataholder rootDataholder = new ArchiveEntryDataholder(archive, false, "/", "", null);
		rootDataholder.updateId();
		this.entries.put("/", rootDataholder );
		
	}
	
	@JsonIgnore
	public void close() throws IOException {
		if( archive != null ) {
			archive.close();
			if( lock != null )
				lock.unlock();
			LOGGER.debug("close ", archiveFile.getAbsolutePath());
		}
		else {
			if( lock != null )
				lock.unlock();
		}
	}
	
	@JsonIgnore
	public void packAndClose() throws IOException, TransformerException {
		if( archive != null ) {
			archive.pack();
			this.close();
		}
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
	public long countArchiveEntries() {
		return (long) archive.getEntries().size();
	}
	
	@JsonIgnore
	public ArchiveEntry addArchiveEntry(String fileName, Path file) throws CombineArchiveWebException, IOException {
		return addArchiveEntry(fileName, file, ReplaceStrategy.RENAME);
	}
	
	@JsonIgnore
	public ArchiveEntryDataholder getEntryById( String id ) {
		
		if( id == null || id.isEmpty() )
			return null;
		
		for( ArchiveEntryDataholder entry : entries.values() ) {
			if( entry.getId().equals(id) )
				return entry;
		}
		
		return null;
	}
	
	public enum ReplaceStrategy {
		RENAME,		// renames the new file, if the name is already taken
		REPLACE,	// replaces the old file, meta data will be copied
		OVERRIDE;	// replaces the old file, meta data will be discarded
		
		public static ReplaceStrategy fromString( String string ) {
			
			if( string == null || string.isEmpty() )
				return RENAME;
			
			ReplaceStrategy strategy = RENAME;
			string = string.toLowerCase();
			
			if( string.contains("replace") )
				strategy = ReplaceStrategy.REPLACE;
			else if( string.contains("override") )
				strategy = ReplaceStrategy.OVERRIDE;
			
			return strategy;
		}
		
	}
	
	@JsonIgnore
	public ArchiveEntry addArchiveEntry(String fileName, Path file, ReplaceStrategy strategy) throws CombineArchiveWebException, IOException {
		
		if( archive == null ) {
			LOGGER.error( "The archive was not opened" );
			throw new CombineArchiveWebException("The archive was not opened");
		}
		
		// check for blacklisted filename
		if( Tools.isFilenameBlacklisted(fileName) )
			throw new CombineArchiveWebException( 
					MessageFormat.format("The filename is blacklisted. You may not add files called {0}!", FilenameUtils.getName(fileName))
				);
		
		ArchiveEntry entry = null;
		
		if( strategy == ReplaceStrategy.RENAME || strategy == ReplaceStrategy.OVERRIDE ) {
			
			// make sure file name is not taken yet
			if( archive.getEntry(fileName) != null && strategy == ReplaceStrategy.RENAME ) {
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
			}
			
			entry = archive.addEntry( file.toFile(), fileName, Formatizer.guessFormat(file.toFile()) );
			// adds the entry to the dataholder (warning: this is probably inconsistent)
			if( entry != null ) {
				// entry information are gathered in the entry dataholder
				ArchiveEntryDataholder dataholder = new ArchiveEntryDataholder(entry);
				// put it into the map
				this.entries.put(dataholder.getFilePath(), dataholder);
			}
		}
		else if( archive.getEntry(fileName) != null && strategy == ReplaceStrategy.REPLACE ) {
			
			ArchiveEntry oldEntry = archive.getEntry(fileName);
			entry = archive.replaceFile( file.toFile(), oldEntry );
			// adds the entry to the dataholder (warning: this is probably inconsistent)
			if( entry != null ) {
				// entry information are gathered in the entry dataholder
				ArchiveEntryDataholder dataholder = new ArchiveEntryDataholder(entry);
				// put it into the map
				this.entries.put(dataholder.getFilePath(), dataholder);
			}
		}
		
		return entry;
	}


}
