package de.unirostock.sems.cbarchive.web;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;

public class UserManager {
	
	protected WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
	protected Workspace workspace = null;
	protected File workingDir = null;
	protected UserData data = null;

	public UserManager() throws IOException {
		this(null);
	}

	public UserManager( String workspaceId ) throws IOException {
		if( workspaceId != null && !workspaceId.isEmpty() ) {
			workspace = workspaceManager.getWorkspace(workspaceId);
		}
		if( workspace == null ) {
			workspace = workspaceManager.createWorkspace();
		}
		
		// updates the last-seen time stamp
		workspace.updateLastseen();
		workingDir = workspace.getWorkspaceDir();

	}

	public UserData getData() {
		return data;
	}

	public void setData(UserData data) {
		this.data = data;
	}

	public String getWorkspaceId() {
		return workspace.getWorkspaceId();
	}

	public File getWorkingDir() {
		return workingDir;
	}
	
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Lists all available archives for the user, with content <br />
	 * Similar to getArchives(true);
	 * 
	 * @return
	 */
	public List<Archive> getArchives() {
		return getArchives(true);
	}

	/**
	 * Lists all available archives for the user
	 * if {@code deepScan} is set to true, the content of the archives will be analysed 
	 * 
	 * @param deepScan
	 * @return
	 */
	public List<Archive> getArchives( boolean deepScan ) {
		List<Archive> result = new LinkedList<Archive>();
		
		Iterator<String> iter = workspace.getArchives().keySet().iterator();
		while( iter.hasNext() ) {
			String archiveId = iter.next();
			
			String name = workspace.getArchives().get(archiveId);
			File archiveFile = new File( workingDir, archiveId);
			
			// checks if archive exists
			if( archiveFile.exists() ) {
				try {
					Archive dataholder = new Archive( archiveId, name );

					// if deepScan enabled, analyse content
					if( deepScan == true ) {
						dataholder.setArchiveFile(archiveFile, workspace.lockArchive(archiveId));
						// closes it
						dataholder.close();
					}

					// adds this archive to the dataholder
					result.add(dataholder);

				} catch (Exception e) {
					LOGGER.error (e, "couldn't read combine archive: ", archiveFile);
				}
			}
			else {
				LOGGER.warn (archiveId, " is supposed to be an archive but it doesn't exist. It will get removed from the archive list.");
				iter.remove();
			}
		}
		
		return result;
	}
	
	public Archive getArchive( String archiveId ) throws FileNotFoundException, CombineArchiveWebException {
		return getArchive( archiveId, true );
	}
	
	public Archive getArchive( String archiveId, boolean deepScan ) throws CombineArchiveWebException, FileNotFoundException {

		// gets the properties Key for this archive
		String archiveName = workspace.getArchives().get(archiveId);
		// check if exists
		if( archiveName == null || archiveName.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			// get the file
			File archive = new File( workingDir.getAbsolutePath(), archiveId );
			if( archive.isFile() && archive.exists() && archive.canRead() ) {
				
				Archive archiveModel = new Archive(archiveId, archiveName, null, null);
				if( deepScan ) {
					Lock lock = workspace.lockArchive(archiveId);
					archiveModel.setArchiveFile(archive, lock );
				}
				return archiveModel;
			}
			else
				throw new FileNotFoundException("Cannot find/read combine archive file for " + archiveId);
		}
	}

	public File getArchiveFile( String archiveId ) throws FileNotFoundException {

		// gets the properties Key for this archive
		String archiveName = workspace.getArchives().get(archiveId);
		// check if exists
		if( archiveName == null || archiveName.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			// get the file
			File archive = new File( workingDir.getAbsolutePath(), archiveId );
			if( archive.isFile() && archive.exists() && archive.canRead() )
				return archive;
			else
				throw new FileNotFoundException("Cannot find/read combine archive file for " + archiveId);
		}
	}

	public void renameArchive( String archiveId, String newName ) throws IllegalArgumentException, FileNotFoundException, IOException {

		if( newName == null || newName.isEmpty() ) {
			throw new IllegalArgumentException("The new name cannot be empty!");
		}

		// gets the properties Key for this archive
		String archiveKey = workspace.getArchives().get(archiveId);
		// check if exists
		if( archiveKey == null || archiveKey.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			workspace.getArchives().put(archiveId, newName);
		}
		
		// save the settings
		workspaceManager.storeSettings();

	}

	public String createArchive( String name ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		return createArchive(name, null, null);
	}
	
	public String createArchive( String name, File existingArchive ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		return createArchive(name, existingArchive, null);
	}
	
	public String createArchive( String name, VCard creator ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		return createArchive(name, null, creator);
	}
	
	private String createArchive( String name, File existingArchive, VCard creator ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		
		// generates new unique UID
		String uuid = UUID.randomUUID ().toString ();
		File archiveFile = new File (workingDir, uuid);
		while (archiveFile.exists ())
		{
			uuid = UUID.randomUUID ().toString ();
			archiveFile = new File (workingDir, uuid);
		}
		
		if( existingArchive != null && existingArchive.exists() ) {
			// an archive already exists
			// check if combineArchive is valid
			CombineArchive combineArchive = new CombineArchive( existingArchive );
			combineArchive.close();
			
			// copy files
			FileUtils.copyFile(existingArchive, archiveFile);
		}
		else {
			// creates and packs the new empty archive
			CombineArchive archive = new CombineArchive (archiveFile);
			
			// adds the creator and the current date, if provided
			if( creator != null && !creator.isEmpty() ) {
				OmexDescription description = new OmexDescription( creator, new Date() );
				archive.addDescription( new OmexMetaDataObject(description) );
			}
			
			archive.pack ();
			archive.close ();
		}
		
		// update the Properties
		workspace.getArchives().put(uuid, name);
		
		LOGGER.info( MessageFormat.format("Created new archive with id {0} in workspace {1}", uuid, getWorkingDir()) );
		
		// save the settings
		workspaceManager.storeSettings();
		
		return uuid;
	}
	
	public void deleteArchive( String archiveId ) throws IOException {
		
		File archiveFile = getArchiveFile(archiveId);
		// deletes the archive file
		if( !archiveFile.delete() )
			throw new IOException("Cannot delete archive file");
		
		// removes the internal reference from the settings
		workspace.getArchives().remove(archiveId);
		
		// save the settings
		workspaceManager.storeSettings();
	}
	
	public void deleteArchiveSilent( String archiveId ) {
		
		try {
			deleteArchive(archiveId);
		} catch (IOException e) {
			LOGGER.error(e, "Cannot delete archive ", archiveId, " in workspace ", workspace.getWorkspaceId(), " silently");
		}
		
	}

	public void updateArchiveEntry( String archiveId, ArchiveEntryDataholder newEntryDataholder ) throws CombineArchiveWebException {
		
		Archive archive;
		try {
			archive = getArchive(archiveId);
		} catch (FileNotFoundException e) {
			LOGGER.error(e, "Cannot open archive with id: ", archiveId);
			throw new CombineArchiveWebException("Cannot open archive with id: " + archiveId, e);
		}
		CombineArchive combineArchive = archive.getArchive();
		ArchiveEntry archiveEntry = null;
		
		// searching for the old entry by the id
		for( ArchiveEntryDataholder entry : archive.getEntries().values() ) {
			if( entry.getId().equals(newEntryDataholder.getId()) ) {
				archiveEntry = entry.getArchiveEntry();
				break;
			}
		}
		
		if( archiveEntry == null ) {
			// was not able to find the old entry
			try {
				archive.close();
			} catch (IOException e1) {
				LOGGER.error(e1, "Cannot close archive");
			}
			throw new CombineArchiveWebException("Cannot find old version of archive entry");
		}
		
		ArchiveEntryDataholder oldEntryDataholder = new ArchiveEntryDataholder(archiveEntry);
		
		// applies changes in the filename/filepath
		String newFilePath = newEntryDataholder.getFilePath();
		if( !oldEntryDataholder.getFilePath().equals(newFilePath) && newFilePath != null && !newFilePath.isEmpty() ) {
			// filePath has changed!
			
			if( !newFilePath.startsWith("/") )
				newFilePath = "/" + newFilePath;
			
			// move it!
			try {
				combineArchive.moveEntry(oldEntryDataholder.getFilePath(), newFilePath);
			} catch (IOException e) {
				LOGGER.error(e, "Cannot move file from ", oldEntryDataholder.getFilePath(), " to ", newFilePath);
				
				try {
					archive.close();
				} catch (IOException e1) {
					LOGGER.error(e1, "Cannot close archive");
				}
				
				throw new CombineArchiveWebException("Cannot move file", e);
			}
			
			// add modified date to all omex descriptions for the root element
			for( MetaDataObject metaObject : combineArchive.getDescriptions() ) {
				if( metaObject instanceof OmexMetaDataObject )
					((OmexMetaDataObject) metaObject).getOmexDescription().getModified().add( new Date() );
			}
		}
		
		// format changed
		URI newFileFormat = newEntryDataholder.getFormat();
		if( !oldEntryDataholder.getFormat().equals(newFileFormat) && newFileFormat != null ) {
			
			oldEntryDataholder.setFormat(newFileFormat);
			oldEntryDataholder.getArchiveEntry().setFormat(newFileFormat);
			
		}
		
		// set the master flag or remove it
		if( newEntryDataholder.isMaster() )
			combineArchive.addMainEntry( archiveEntry );
		else
			combineArchive.removeMainEntry( archiveEntry );

		try {
			archive.packAndClose();
		} catch (IOException | TransformerException e) {
			LOGGER.error(e, "Cannot pack and close archive ", archiveId);
			throw new CombineArchiveWebException("Cannot pack and close archive", e);
		}
	}
	

}
