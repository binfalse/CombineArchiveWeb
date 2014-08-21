package de.unirostock.sems.cbarchive.web.dataholder;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

public class ArchiveFromExisting extends Archive {
	
	@FormDataParam("file")
	protected FormDataBodyPart file = null;
	
	public ArchiveFromExisting() {
		super();
	}
	
	public ArchiveFromExisting(String id, String name, FormDataBodyPart file) {
		super(id, name);
		this.file = file;
	}

	public ArchiveFromExisting(String id, String name) {
		super(id, name);
	}

	public ArchiveFromExisting(String name) {
		super(name);
	}

	public FormDataBodyPart getFile() {
		return file;
	}

	public void setFile(FormDataBodyPart file) {
		this.file = file;
	}
	
}
