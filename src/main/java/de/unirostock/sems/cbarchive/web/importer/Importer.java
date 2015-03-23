package de.unirostock.sems.cbarchive.web.importer;
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
