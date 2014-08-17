package de.unirostock.sems.cbarchive.web.dataholder;


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
