package de.unirostock.sems.cbarchive.web.importer;

import java.io.File;

import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;

public abstract class Importer {
	
	protected File tempFile = null;
	protected UserManager user = null;
	
	public Importer( UserManager user ) {
		this.user = user;
	}
	
	public File getTempFile() {
		return tempFile;
	}
	
	public abstract Importer importRepo() throws ImporterException;
	public abstract void cleanUp();
}
