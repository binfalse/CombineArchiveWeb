package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
public class StatisticData implements Serializable, Cloneable {
	
	private static final long serialVersionUID = -3935699299597297002L;
	
	public static final double IGNORE_DOUBLE = -1.0f;
	public static final long IGNORE_LONG = -1L;
	
	/** total number of workspaces hosted on this instance */
	private long workspaceCount = IGNORE_LONG;
	/** total size in bytes of all workspaces combined */
	private long totalSize = IGNORE_LONG;
	/** average size per workspace */
	private double sizePerWorkspace = IGNORE_DOUBLE;
	/** average amount of archives per workspace */
	private double archivesPerWorkspace = IGNORE_DOUBLE;
	/** average age of the workspaces in seconds */
	private double avgWorkspaceAge = IGNORE_DOUBLE;
	
	/** relative usage of the total size quota (0.0 - 1.0) */
	private double totalQuota = IGNORE_DOUBLE;
	/** average usage of space per Workspace quota (0.0 - 1.0) */
	private double avgWorkspaceSizeQuota = IGNORE_DOUBLE;
	/** average usage of archives per workspace (0.0 - 1.0) */
	private double avgArchiveCountQuota = IGNORE_DOUBLE;
	
	/** relative usage of space for the current workspace (0.0 - 1.0) */
	private double workspaceSizeQuota = IGNORE_DOUBLE;
	/** relative usage of archives per workspace quota (0.0 - 1.0) */
	private double archiveCountQuota = IGNORE_DOUBLE;
	
	/** Timestamp of generation */
	private Date generated = new Date();
	
	public StatisticData() {}

	public long getWorkspaceCount() {
		return workspaceCount;
	}

	public void setWorkspaceCount(long workspaceCount) {
		this.workspaceCount = workspaceCount;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public double getSizePerWorkspace() {
		return sizePerWorkspace;
	}

	public void setSizePerWorkspace(double sizePerWorkspace) {
		this.sizePerWorkspace = sizePerWorkspace;
	}

	public double getTotalQuota() {
		return totalQuota;
	}

	public void setTotalQuota(double totalQuota) {
		this.totalQuota = totalQuota;
	}

	public double getAvgWorkspaceSizeQuota() {
		return avgWorkspaceSizeQuota;
	}

	public void setAvgWorkspaceSizeQuota(double avgWorkspaceSizeQuota) {
		this.avgWorkspaceSizeQuota = avgWorkspaceSizeQuota;
	}

	public double getAvgArchiveCountQuota() {
		return avgArchiveCountQuota;
	}

	public void setAvgArchiveCountQuota(double avgArchiveCountQuota) {
		this.avgArchiveCountQuota = avgArchiveCountQuota;
	}

	public double getAvgWorkspaceAge() {
		return avgWorkspaceAge;
	}

	public void setAvgWorkspaceAge(double d) {
		this.avgWorkspaceAge = d;
	}

	public double getWorkspaceSizeQuota() {
		return workspaceSizeQuota;
	}

	public void setWorkspaceSizeQuota(double workspaceSizeQuota) {
		this.workspaceSizeQuota = workspaceSizeQuota;
	}

	public double getArchiveCountQuota() {
		return archiveCountQuota;
	}

	public void setArchiveCountQuota(double archiveCountQuota) {
		this.archiveCountQuota = archiveCountQuota;
	}

	public Date getGenerated() {
		return generated;
	}

	public void setGenerated(Date generated) {
		this.generated = generated;
	}

	public double getArchivesPerWorkspace() {
		return archivesPerWorkspace;
	}

	public void setArchivesPerWorkspace(double archivesPerWorkspace) {
		this.archivesPerWorkspace = archivesPerWorkspace;
	}
	
	@Override
	public StatisticData clone() {
		StatisticData clone = new StatisticData();
		
		clone.archiveCountQuota = archiveCountQuota;
		clone.archivesPerWorkspace = clone.archivesPerWorkspace;
		clone.avgArchiveCountQuota = avgArchiveCountQuota;
		clone.avgWorkspaceAge = avgWorkspaceAge;
		clone.avgWorkspaceSizeQuota = avgWorkspaceSizeQuota;
		clone.sizePerWorkspace = sizePerWorkspace;
		clone.totalQuota = totalQuota;
		clone.totalSize = totalSize;
		clone.workspaceCount = workspaceCount;
		clone.workspaceSizeQuota = workspaceSizeQuota;
		
		clone.generated = new Date(generated.getTime());
		
		return clone;
	}
	
}
