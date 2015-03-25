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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.QuotaManager;
import de.unirostock.sems.cbarchive.web.Tools;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromHttp;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;

public class HttpImporter extends Importer {
	
	private HttpClient client = null;
	
	
	private String suggestedName = Tools.DATE_FORMATTER.format( new Date() );;
	private long length = 0;
	
	public HttpImporter( String remoteUrl ) {
		this(remoteUrl, null);
	}
	
	public HttpImporter( ArchiveFromHttp archive, UserManager user ) {
		this( archive.getRemoteUrl(), user );
	}
	
	public HttpImporter( String remoteUrl, UserManager user ) {
		super(user);
		this.remoteUrl = remoteUrl;
		this.client = HttpClientBuilder.create().build();
	}
	
	public HttpImporter importRepo() throws ImporterException {
		
		if( remoteUrl == null || remoteUrl.isEmpty() )
			throw new IllegalArgumentException("remoteUrl is empty");

		checkFile();
		downloadFile();
		
		return this;
	}
	
	public void close() {
		// nothing todo
	}
	
	private boolean checkFile() throws ImporterException {
		
		try {
			HttpResponse headResponse = client.execute( new HttpHead(remoteUrl) );
			
			// check if file exists
			if( headResponse.getStatusLine().getStatusCode() != 200 ) {
				LOGGER.error( headResponse.getStatusLine().getStatusCode(), " ", headResponse.getStatusLine().getReasonPhrase(), " while check ", remoteUrl );
				throw new ImporterException(String.valueOf(headResponse.getStatusLine().getStatusCode()) + " " + headResponse.getStatusLine().getReasonPhrase() + " while check");
			}
			
			// check if file is in the Quota range
			Header contentLengthHeader = headResponse.getFirstHeader("Content-Length");
			
			// check if Content-Size header is set
			if( contentLengthHeader != null && contentLengthHeader.getValue() != null && contentLengthHeader.getValue().isEmpty() == false ) {
				length = Long.valueOf( contentLengthHeader.getValue() );
			}
			else {
				LOGGER.warn( "Remote file ", remoteUrl, " does not provide Content-Length");
				throw new ImporterException("Remote file does not provide Content-Length" );
			}
			
			// compares this header with the quota
			if( user != null)
				checkQuotas();
			
		} catch (IOException e) {
			LOGGER.error(e, "Exception while check file from ", remoteUrl);
			throw new ImporterException("Exception while check remote file", e);
		}
		
		return true;
	}
	
	private File downloadFile() throws ImporterException {
		
		try {
			tempFile = File.createTempFile(Fields.TEMP_FILE_PREFIX, ".omex");
			HttpResponse getResponse = client.execute( new HttpGet(remoteUrl) );
			
			// check if file exists
			if( getResponse.getStatusLine().getStatusCode() != 200 ) {
				LOGGER.warn( getResponse.getStatusLine().getStatusCode(), " ", getResponse.getStatusLine().getReasonPhrase(), " while download ", remoteUrl );
				throw new ImporterException(String.valueOf(getResponse.getStatusLine().getStatusCode()) + " " + getResponse.getStatusLine().getReasonPhrase() + " while download");
			}
			
			HttpEntity entity = getResponse.getEntity();
			if( entity == null ) {
				LOGGER.error("No content returned while donwloading remote file ", remoteUrl);
				throw new ImporterException( "No content returned while donwloading remote file " + remoteUrl );
			}
			
			// for name suggestions
			Header dispositionHeader = getResponse.getFirstHeader("Content-Disposition");
			if( dispositionHeader != null && dispositionHeader.getValue() != null && dispositionHeader.getValue().isEmpty() == false ) {
				// disposition header is present -> extract name
				// inline; filename=\"{0}.{1}\"
				Matcher matcher = Pattern.compile("filename=\\\"?(([a-zA-Z0-9-_\\+]+).(\\w+))\\\"?", Pattern.CASE_INSENSITIVE).matcher( dispositionHeader.getValue() );
				if( matcher.find() ) {
					suggestedName = matcher.group(1);
					for( int i = 0; i < matcher.groupCount(); i++)
						LOGGER.debug(i, ": ", matcher.group(i));
				}
			}
			else {
				// when not -> take the last part of the url
				Matcher matcher = Pattern.compile("\\/(([a-zA-Z0-9-_\\+]+).(\\w+))$", Pattern.CASE_INSENSITIVE).matcher(remoteUrl);
				if( matcher.find() ) {
					suggestedName = matcher.group(1);
					for( int i = 0; i < matcher.groupCount(); i++)
						LOGGER.debug(i, ": ", matcher.group(i));
				}
				
			}
			
			// download it
			OutputStream output = new FileOutputStream(tempFile);
			IOUtils.copy( entity.getContent(), output );
			
			// check against quota
			if( length != tempFile.length() ) {
				LOGGER.warn("Content-Length (", length, ") and downloaded length (", tempFile.length(), ") are different.");
				length = tempFile.length();
				
				checkQuotas();
			}
			
			return tempFile;
			
		} catch (IOException e) {
			LOGGER.error(e, "Exception while download file from ", remoteUrl);
			throw new ImporterException("Exception while download remote file", e);
		}
	}
	
	private void checkQuotas() throws ImporterException {
		
		// size == 0
		if( length == 0 ) {
			LOGGER.warn("The remote file is empty");
			throw new ImporterException("The remote file is empty.");
		}
		// max size for upload
		if( Fields.QUOTA_UPLOAD_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(length, Fields.QUOTA_UPLOAD_SIZE) == false ) {
			LOGGER.warn("QUOTA_UPLOAD_SIZE reached in workspace ", user.getWorkspaceId());
			throw new ImporterException("The uploaded file is to big.");
		}
		// max workspace size
		if( user != null && Fields.QUOTA_WORKSPACE_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getWorkspaceSize(user.getWorkspace()) + length, Fields.QUOTA_WORKSPACE_SIZE) == false ) {
			LOGGER.warn("QUOTA_WORKSPACE_SIZE reached in workspace ", user.getWorkspaceId());
			throw new ImporterException("The maximum size of one workspace is reached.");
		}
		// max total size
		if( user != null && Fields.QUOTA_TOTAL_SIZE != Fields.QUOTA_UNLIMITED && Tools.checkQuota(QuotaManager.getInstance().getTotalSize() + length, Fields.QUOTA_TOTAL_SIZE) == false ) {
			LOGGER.warn("QUOTA_TOTAL_SIZE reached in workspace ", user.getWorkspaceId());
			throw new ImporterException("The maximum size is reached.");
		}
		
	}

	public long getLength() {
		return length;
	}
	
	@Override
	public String getSuggestedName() {
		return suggestedName;
	}
	
}
