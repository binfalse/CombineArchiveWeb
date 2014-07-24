package de.unirostock.sems.cbarchive.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveEntryDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.MetaObjectDataholder;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;

public class UserManager {

	protected String path;
	protected UserData data = null;

	protected File workingDir;
	protected File propsFile;
	protected Properties userProps = new Properties();

	public UserManager() throws IOException {
		createUUID();
		prepareWorkingDir();
	}

	public UserManager( String path ) throws IOException {
		if( path != null && !path.isEmpty() ) {
			this.path = path;
		}
		else {
			createUUID();
		}
		prepareWorkingDir();

	}

	private String createUUID() throws IOException {

		if( Fields.STORAGE.exists() == false && Fields.STORAGE.mkdirs() == false ) {
			LOGGER.error( MessageFormat.format("Can not create storage directory: {0}", Fields.STORAGE) );
			throw new IOException( "Can not create storage directory" );
		}

		String uuid = null;
		File workingDir = null;
		while( uuid == null || workingDir.exists() ) {
			uuid = UUID.randomUUID ().toString ();
			workingDir = new File( Fields.STORAGE, uuid );
		}

		// sets it!
		this.path = uuid;
		this.workingDir = workingDir;

		return uuid;
	}

	private void prepareWorkingDir() throws IOException {

		if ( !path.matches ("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") ) {
			LOGGER.warn( MessageFormat.format("Got invalid workspace path: {0} Can't establish user class", path) );
			throw new IOException ("Invalid Workspace Path!");
		}

		workingDir = new File( Fields.STORAGE, path );
		if( !workingDir.exists() || !workingDir.isDirectory() ) {
			// maybe deleted or not existing
			if( workingDir.mkdirs() == false ) {
				// not able to create the working directory
				LOGGER.error( MessageFormat.format("Can not create working directory: {0}", workingDir) );
				throw new IOException ("Can not create working directory!");
			}
		}
		// Load the properties
		propsFile = new File( workingDir, Fields.WORKINGDIR_PROP_FILE );
		if( propsFile.exists() ) {

			InputStream stream = new FileInputStream(propsFile);
			userProps.load( stream );
			stream.close();
		}

		// update latest seen and stores them
		userProps.setProperty( Fields.PROP_LAST_SEEN, Tools.DATE_FORMATTER.format( new Date() ) );
		storeProperties();
	}

	public UserData getData() {
		return data;
	}

	public void setData(UserData data) {
		this.data = data;
	}

	public String getPath() {
		return path;
	}

	public File getWorkingDir() {
		return workingDir;
	}

	public Properties getUserProps() {
		return userProps;
	}

	protected void storeProperties() throws IOException {

		LOGGER.info ("storing user properties to " + propsFile.getAbsolutePath ());

		try {
			OutputStream stream = new FileOutputStream(propsFile);
			userProps.store(stream, null);
			stream.flush();
			stream.close();
		} catch (IOException e) {
			LOGGER.error( e, MessageFormat.format("Can not store user properties to {0}", propsFile) );
			throw e;
		}
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

		for (Object p : userProps.keySet ())
		{
			String key = (String) p;
			// iterating over all prop keys
			if( key.startsWith(Fields.PROP_ARCHIVE_PRE) ) {

				// is an archive entry
				String dir = key.substring( Fields.PROP_ARCHIVE_PRE.length() );
				String name = userProps.getProperty(key);
				File archiveFile = new File( workingDir, dir );

				// checks if archive exists
				if( archiveFile.exists() ) {
					try {
						Archive dataholder = new Archive( dir, name );

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
					LOGGER.warn (dir, " is supposed to be an direcetory but it doesn't exist...");
			}
		}

		return result;
	}
	
	public Archive getArchive( String archiveId ) throws FileNotFoundException, CombineArchiveWebException {
		return getArchive( archiveId, true );
	}
	
	public Archive getArchive( String archiveId, boolean deepScan ) throws CombineArchiveWebException, FileNotFoundException {

		// gets the properties Key for this archive
		String archiveKey = userProps.getProperty( Fields.PROP_ARCHIVE_PRE + archiveId );
		// check if exists
		if( archiveKey == null || archiveKey.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			// get the file
			File archive = new File( workingDir.getAbsolutePath(), archiveId );
			if( archive.isFile() && archive.exists() && archive.canRead() ) {
				return new Archive(archiveId, archiveKey, deepScan == true ? archive : null);
			}
			else
				throw new FileNotFoundException("Can not find/read combine archive file for " + archiveId);
		}
	}

	public File getArchiveFile( String archiveId ) throws FileNotFoundException {

		// gets the properties Key for this archive
		String archiveKey = userProps.getProperty( Fields.PROP_ARCHIVE_PRE + archiveId );
		// check if exists
		if( archiveKey == null || archiveKey.isEmpty() )
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
		String archiveKey = userProps.getProperty( Fields.PROP_ARCHIVE_PRE + archiveId );
		// check if exists
		if( archiveKey == null || archiveKey.isEmpty() )
			// if not, throw an exception!
			throw new FileNotFoundException("There is no archive in this working space with the ID " + archiveId);
		else {
			userProps.setProperty( Fields.PROP_ARCHIVE_PRE + archiveId, newName );
			storeProperties();
		}

	}

	public String createArchive( String name ) throws IOException, JDOMException, ParseException, CombineArchiveException, TransformerException {

		// generates new unique UID
		String uuid = UUID.randomUUID ().toString ();
		File archiveFile = new File (workingDir, uuid);
		while (archiveFile.exists ())
		{
			uuid = UUID.randomUUID ().toString ();
			archiveFile = new File (workingDir, uuid);
		}

		// creates and packs the new empty archive
		CombineArchive archive = new CombineArchive (archiveFile);
		archive.pack ();
		archive.close ();
		
		// update the Properties
		userProps.setProperty( Fields.PROP_ARCHIVE_PRE + uuid, name );
		storeProperties();

		LOGGER.info( MessageFormat.format("Created new archive with id {0} in workspace {1}", uuid, workingDir) );

		return uuid;
	}

	public void updateArchiveEntry( String archiveId, ArchiveEntryDataholder newEntryDataholder ) throws CombineArchiveWebException, IOException, TransformerException {

		CombineArchive combineArchive = getArchive(archiveId).getArchive();
		ArchiveEntry archiveEntry = combineArchive.getEntry( newEntryDataholder.getFilePath() );
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
				
			}

		}

		// applies changes in the filename/filepath
		// TODO delete and re-insert, if filepath has changed

		combineArchive.pack();
		combineArchive.close();
	}


}
