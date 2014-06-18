package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;

import org.jdom2.JDOMException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.CombineArchiveWebException;

/**
 * Dataholder/Model class for CombineArchives
 * 
 * @author Martin Peters
 *
 */
public class Archive {
	protected String id;
	protected String name;

	@JsonIgnore
	private CombineArchive archive;

	public Archive(String id, String name, File file) throws CombineArchiveWebException {
		this.id = id;
		this.name = name;
		setArchiveFile(file);

		//archive.getMainEntry().getDescriptions().get(0).getAbout();
	}

	public Archive(String id, String name) {
		this.id = id;
		this.name = name;
		this.archive = null;
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
	
	@JsonIgnore
	public void setArchiveFile(File file) throws CombineArchiveWebException {

		if( !file.exists() || !file.isFile() ) {
			LOGGER.error( MessageFormat.format("The archive is not accesible: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not accesible");
		}

		try {
			archive = new CombineArchive(file);
		} catch (IOException | JDOMException | ParseException | CombineArchiveException e) {
			LOGGER.error(e, MessageFormat.format("The archive is not parsable: {0}", file.getAbsolutePath()) );
			throw new CombineArchiveWebException("The archive is not parsable");
		}

	}
	
	@JsonIgnore
	public CombineArchive getArchive() {
		return archive;
	}


}
