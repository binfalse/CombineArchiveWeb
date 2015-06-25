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
	private long totalWorkspaceCount = IGNORE_LONG;
	/** total size in bytes of all workspaces combined */
	private long totalSize = IGNORE_LONG;
	/** total number of archives hosted on this instance */
	private long totalArchiveCount = IGNORE_LONG;
	/** average size per workspace */
	private double averageWorkspaceSize = IGNORE_DOUBLE;
	/** average amount of archives per workspace */
	private double averageArchiveCount = IGNORE_DOUBLE;
	/** average age of the workspaces in seconds */
	private double averageWorkspaceAge = IGNORE_DOUBLE;
	
	/** relative usage of the total size quota (0.0 - 1.0) */
	private double totalSizeQuota = IGNORE_DOUBLE;
	/** average usage of space per Workspace quota (0.0 - 1.0) */
	private double averageWorkspaceSizeQuota = IGNORE_DOUBLE;
	/** average usage of archives per workspace (0.0 - 1.0) */
	private double averageArchiveCountQuota = IGNORE_DOUBLE;
	
	// user stats
	/** relative usage of space for the current workspace (0.0 - 1.0) */
	private double userWorkspaceSizeQuota = IGNORE_DOUBLE;
	/** relative usage of archives per workspace quota (0.0 - 1.0) */
	private double userArchiveCountQuota = IGNORE_DOUBLE;
	
	// server stuff
	private long maxStatsAge = IGNORE_LONG;
	
	/** Timestamp of generation */
	private Date generated = new Date();
	
	public StatisticData() {}
	
	public long getTotalWorkspaceCount() {
		return totalWorkspaceCount;
	}

	public void setTotalWorkspaceCount(long totalWorkspaceCount) {
		this.totalWorkspaceCount = totalWorkspaceCount;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public double getAverageWorkspaceSize() {
		return averageWorkspaceSize;
	}

	public void setAverageWorkspaceSize(double averageWorkspaceSize) {
		this.averageWorkspaceSize = averageWorkspaceSize;
	}

	public double getAverageArchiveCount() {
		return averageArchiveCount;
	}

	public void setAverageArchiveCount(double averageArchiveCount) {
		this.averageArchiveCount = averageArchiveCount;
	}

	public double getAverageWorkspaceAge() {
		return averageWorkspaceAge;
	}

	public void setAverageWorkspaceAge(double averageWorkspaceAge) {
		this.averageWorkspaceAge = averageWorkspaceAge;
	}

	public double getTotalSizeQuota() {
		return totalSizeQuota;
	}

	public void setTotalSizeQuota(double totalSizeQuota) {
		this.totalSizeQuota = totalSizeQuota;
	}

	public double getAverageWorkspaceSizeQuota() {
		return averageWorkspaceSizeQuota;
	}

	public void setAverageWorkspaceSizeQuota(double averageWorkspaceSizeQuota) {
		this.averageWorkspaceSizeQuota = averageWorkspaceSizeQuota;
	}

	public double getAverageArchiveCountQuota() {
		return averageArchiveCountQuota;
	}

	public void setAverageArchiveCountQuota(double averageArchiveCountQuota) {
		this.averageArchiveCountQuota = averageArchiveCountQuota;
	}

	public double getUserWorkspaceSizeQuota() {
		return userWorkspaceSizeQuota;
	}

	public void setUserWorkspaceSizeQuota(double userWorkspaceSizeQuota) {
		this.userWorkspaceSizeQuota = userWorkspaceSizeQuota;
	}

	public double getUserArchiveCountQuota() {
		return userArchiveCountQuota;
	}

	public void setUserArchiveCountQuota(double userArchiveCountQuota) {
		this.userArchiveCountQuota = userArchiveCountQuota;
	}

	public Date getGenerated() {
		return generated;
	}

	public void setGenerated(Date generated) {
		this.generated = generated;
	}
	
	public long getTotalArchiveCount() {
		return totalArchiveCount;
	}

	public void setTotalArchiveCount(long totalArchiveCount) {
		this.totalArchiveCount = totalArchiveCount;
	}
	
	public long getMaxStatsAge() {
		return maxStatsAge;
	}

	public void setMaxStatsAge(long maxStatsAge) {
		this.maxStatsAge = maxStatsAge;
	}

	@Override
	public StatisticData clone() {
		StatisticData clone = new StatisticData();
		
		clone.userArchiveCountQuota = userArchiveCountQuota;
		clone.averageArchiveCount = averageArchiveCount;
		clone.averageArchiveCountQuota = averageArchiveCountQuota;
		clone.averageWorkspaceAge = averageWorkspaceAge;
		clone.averageWorkspaceSizeQuota = averageWorkspaceSizeQuota;
		clone.averageWorkspaceSize = averageWorkspaceSize;
		clone.totalSizeQuota = totalSizeQuota;
		clone.totalSize = totalSize;
		clone.totalWorkspaceCount = totalWorkspaceCount;
		clone.totalArchiveCount = totalArchiveCount;
		clone.userWorkspaceSizeQuota = userWorkspaceSizeQuota;
		
		clone.maxStatsAge = maxStatsAge;
		clone.generated = new Date(generated.getTime());
		
		return clone;
	}
	
}
