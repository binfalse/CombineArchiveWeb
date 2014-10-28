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

public class ArchiveFromCellMl extends Archive {
	
	protected String cellmlLink = null;
	
	public ArchiveFromCellMl() {
		super();
	}

	public ArchiveFromCellMl(String id, String name, String cellmlLink) {
		super(id, name);
		this.cellmlLink = cellmlLink;
	}
	
	public ArchiveFromCellMl(String id, String name) {
		super(id, name);
	}

	public ArchiveFromCellMl(String name) {
		super(name);
	}

	public String getCellmlLink() {
		return cellmlLink;
	}

	public void setCellmlLink(String cellmlLink) {
		this.cellmlLink = cellmlLink;
	}
	
	
}
