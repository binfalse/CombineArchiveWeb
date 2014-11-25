package de.unirostock.sems.cbarchive.web.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "stackTrace", "cause", "localizedMessage", "suppressed" })
public class ArchiveEntryUploadException extends CombineArchiveWebException {
	
	private static final long serialVersionUID = 9041379974534017455L;

	private String filePath = null;
	private final boolean error = true;
	
	public ArchiveEntryUploadException(String message) {
		super(message);
	}

	public ArchiveEntryUploadException() {
		super();
	}

	public ArchiveEntryUploadException(Throwable cause) {
		super(cause);
	}
	
	public ArchiveEntryUploadException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ArchiveEntryUploadException(String message, String filePath) {
		this(message);
		this.filePath = filePath;
	}
	
	public ArchiveEntryUploadException(String message, Throwable cause, String filePath) {
		this(message, cause);
		this.filePath = filePath;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public boolean isError() {
		return error;
	}
}
