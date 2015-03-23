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

public class ArchiveFromGit extends Archive {

	protected String gitLink = null;
	
	public ArchiveFromGit() {
		super();
		this.template = TEMPLATE_GIT;
	}
	
	public ArchiveFromGit(String id, String name, String gitLink) {
		super(id, name);
		this.gitLink = gitLink;
		this.template = TEMPLATE_GIT;
	}
	
	public ArchiveFromGit(String id, String name) {
		super(id, name);
		this.template = TEMPLATE_GIT;
	}
	
	public ArchiveFromGit(String name) {
		super(name);
		this.template = TEMPLATE_GIT;
	}

	public String getGitLink() {
		return gitLink;
	}

	public void setGitLink(String gitLink) {
		this.gitLink = gitLink;
	}
	
}
