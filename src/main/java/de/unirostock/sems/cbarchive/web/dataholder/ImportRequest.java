package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.importer.Importer;

//so Jersey parses this as root dataholder, if passed to create or update or something...
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportRequest implements Serializable {
	
	private static final long serialVersionUID = 4078016514921419617L;
	
	/** Name of the CombineArchive, shown in the web front-end */
	private String archiveName = null;
	/** Type of the import source, cf. Importer. Can be null, if additionalFiles are set */
	private String type = Importer.IMPORT_HTTP;
	/** The URL pointing to the remote resource Can be null, if additionalFiles are set */
	private String remoteUrl = null;
	
	/** if set to true, webCAT sets your VCard as author of the importer CombineArchive */
	private boolean ownVCard = false;
	/** overrides the VCard, stored as cookie, for this import */
	private VCard vcard = null;
	
	/** List of additional files, to be added to the CombineArchive */
	private List<AdditionalFile> additionalFiles = null;
	
	public class AdditionalFile implements Serializable {
		
		private static final long serialVersionUID = 74409915064851457L;
		
		/** URL pointing to the remote file, which shall be added to the archive */
		private String remoteUrl = null;
		/** Path, including the file name, where the file should be placed in the archive */
		private String archivePath = null;
		/** URI representing the file format, if not set the format will be guessed */
		private URI fileFormat = null;
		
		/** List of meta information, which are added to the file */
		private List<MetaObjectDataholder> metaData = null;

		public String getRemoteUrl() {
			return remoteUrl;
		}

		public void setRemoteUrl(String remoteUrl) {
			this.remoteUrl = remoteUrl;
		}

		public String getArchivePath() {
			return archivePath;
		}

		public void setArchivePath(String archivePath) {
			this.archivePath = archivePath;
		}

		public URI getFileFormat() {
			return fileFormat;
		}

		public void setFileFormat(URI fileFormat) {
			this.fileFormat = fileFormat;
		}

		public List<MetaObjectDataholder> getMetaData() {
			return metaData;
		}

		public void setMetaData(List<MetaObjectDataholder> metaData) {
			this.metaData = metaData;
		}
		
	}
	
	/** 
	 * Default constructor
	 */
	public ImportRequest() { }
	
	/**
	 * Checks whether this import request is valid or not
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isValid() {
		
		// remote Type and/or remoteUrl is not set
		if( (type == null || type.isEmpty()) || (remoteUrl == null || remoteUrl.isEmpty()) ) {
			
			// are there some additional files? No? -> this request is useless, therefore invalid
			if( additionalFiles == null | additionalFiles.size() == 0 )
				return false;
			
		}
		// TODO
		
		return true;
	}
	
	/**
	 * Checks if an import of an already packed archive is requested
	 * (remoteUrl and type is set)
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isArchiveImport() {
		
		// remote Type and/or remoteUrl is not set
		if( (type == null || type.isEmpty()) || (remoteUrl == null || remoteUrl.isEmpty()) )
			return false;
		else
			return true;
		
	}
	
	public String getArchiveName() {
		return archiveName;
	}

	public void setArchiveName(String archiveName) {
		this.archiveName = archiveName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public boolean isOwnVCard() {
		return ownVCard;
	}

	public void setOwnVCard(boolean ownVCard) {
		this.ownVCard = ownVCard;
	}

	public VCard getVcard() {
		return vcard;
	}

	public void setVcard(VCard vcard) {
		this.vcard = vcard;
	}

	public List<AdditionalFile> getAdditionalFiles() {
		return additionalFiles;
	}

	public void setAdditionalFiles(List<AdditionalFile> additionalFiles) {
		this.additionalFiles = additionalFiles;
	}
	
}
