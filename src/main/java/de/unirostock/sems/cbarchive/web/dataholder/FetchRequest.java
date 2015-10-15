package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonIgnore;

// so Jersey parses this as root dataholder, if passed to create or update or something...
@XmlAccessorType(XmlAccessType.FIELD)
public class FetchRequest implements Serializable {

	private static final long serialVersionUID = -2444137273939226465L;
	
	private String path = "/";
	private String remoteUrl = null;
	
	public FetchRequest(String path, String remoteUrl) {
		super();
		this.path = path;
		this.remoteUrl = remoteUrl;
	}
	
	public FetchRequest() {
		super();
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}
	
	@JsonIgnore
	public boolean isValid() {
		
		if( remoteUrl == null || remoteUrl.isEmpty() )
			return false;
		
		if( path == null || path.isEmpty() )
			return false;
		
		return true;
	}
	
}
