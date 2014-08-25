package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;
import de.unirostock.sems.cbarchive.web.dataholder.Workspace;

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
		
		for( String archiveId : workspace.getArchives().keySet() ) {
			
			String name = workspace.getArchives().get(archiveId);
			File archiveFile = new File( workingDir, archiveId);
			
			// checks if archive exists
			if( archiveFile.exists() ) {
				try {
					Archive dataholder = new Archive( archiveId, name );

					// if deepScan enabled, analyse content
					if( deepScan == true ) {
						dataholder.setArchiveFile(archiveFile);
						// closes it
						dataholder.getArchive().close();
					}

					// adds this archive to the dataholder
					result.add(dataholder);

				}
				catch (Exception e)
				{
					LOGGER.error (e, "couldn't read combine archive: ", archiveFile);
				}
			}
			else
				LOGGER.warn (archiveId, " is supposed to be an direcetory but it doesn't exist...");
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
				return new Archive(archiveId, archiveName, deepScan == true ? archive : null);
			}
			else
				throw new FileNotFoundException("Can not find/read combine archive file for " + archiveId);
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
				throw new FileNotFoundException("Can not find/read combine archive file for " + archiveId);
		}
	}

	public void renameArchive( String archiveId, String newName ) throws IllegalArgumentException, FileNotFoundException, IOException {

		if( newName == null || newName.isEmpty() ) {
			throw new IllegalArgumentException("The new name can not be empty!");
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

	}

	public String createArchive( String name ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		return createArchive(name, null);
	}
	
	public String createArchive( String name, File existingArchive ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {
		
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
			Files.copy(existingArchive, archiveFile);
		}
		else {
			// creates and packs the new empty archive
			CombineArchive archive = new CombineArchive (archiveFile);
			archive.pack ();
			archive.close ();
		}
		
		// update the Properties
		workspace.getArchives().put(uuid, name);
		
		LOGGER.info( MessageFormat.format("Created new archive with id {0} in workspace {1}", uuid, getWorkingDir()) );

		return uuid;
	}
	
	public void deleteArchive( String archiveId ) throws IOException {
		
		File archiveFile = getArchiveFile(archiveId);
		// deletes the archive file
		if( !archiveFile.delete() )
			throw new IOException("Can not delete archive file");
		
		// removes the internal reference from the settings
		workspace.getArchives().remove(archiveId);
	}

	public void updateArchiveEntry( String archiveId, ArchiveEntryDataholder newEntryDataholder ) throws CombineArchiveWebException, IOException, TransformerException {
		
		Archive archive = getArchive(archiveId);
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
			combineArchive.close();
			throw new CombineArchiveWebException("Can not find old version of archive entry");
		}
		
		ArchiveEntryDataholder oldEntryDataholder = new ArchiveEntryDataholder(archiveEntry);
		
		for( MetaObjectDataholder newMetaObject : newEntryDataholder.getMeta() ) {

			// no changes? skip this one.
			if( newMetaObject.isChanged() == false )
				continue;

			if( newMetaObject.getId() != null && !newMetaObject.getId().isEmpty() ) {
				// check if id is existing -> update of exisiting meta entry
				
				// looking for old version
				MetaObjectDataholder oldMetaObject = null;
				for( MetaObjectDataholder iteratedMetaObject : oldEntryDataholder.getMeta() ) {

					if( iteratedMetaObject.getId().equals( newMetaObject.getId() ) ) {
						oldMetaObject = iteratedMetaObject;
						break;
					}
				}

				if( oldMetaObject == null ) {
					LOGGER.warn( MessageFormat.format("Can not find old representation of a MetaObject about file {0} in {1} with id {2}", newEntryDataholder.getFilePath(), archiveId, newMetaObject.getId()) );
					continue;
				}

				// updates the old dataholder with the new one
				oldMetaObject.update(newMetaObject);
			}
			else {
				// new meta entry
				// TODO
			}

		}
		
		// applies changes in the filename/filepath
		String newFilePath = newEntryDataholder.getFilePath();
		if( !oldEntryDataholder.getFilePath().equals(newFilePath) && newFilePath != null && !newFilePath.isEmpty() ) {
			// filePath has changed!
			
			if( !newFilePath.startsWith("/") ) {
				newFilePath = "/" + newFilePath;
			}
			
			// move it!
			combineArchive.moveEntry(oldEntryDataholder.getFilePath(), newFilePath);
		}

		combineArchive.pack();
		combineArchive.close();
	}
	

}
