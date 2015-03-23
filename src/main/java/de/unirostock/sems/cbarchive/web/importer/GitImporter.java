package de.unirostock.sems.cbarchive.web.importer;

import de.unirostock.sems.cbarchive.web.UserManager;


public class GitImporter extends Importer {
	
	private String gitUrl = null;
	
	public GitImporter( String gitUrl, UserManager user ) {
		super(user);
		this.gitUrl = gitUrl;
	}
	
	public GitImporter( String gitUrl ) {
		this(gitUrl, null);
	}
	
	public String getGitUrl() {
		return gitUrl;
	}
	
	public GitImporter importRepo() {
		
		return this;
	}
	
}
