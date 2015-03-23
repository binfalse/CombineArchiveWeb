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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.PersonIdent;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.Fields;
import de.unirostock.sems.cbarchive.web.UserManager;
import de.unirostock.sems.cbarchive.web.exception.ImporterException;

public abstract class Importer {
	
	protected File tempFile = null;
	protected UserManager user = null;
	
	public Importer( UserManager user ) {
		this.user = user;
	}
	
	public File getTempFile() {
		return tempFile;
	}
	
	public abstract Importer importRepo() throws ImporterException;
	public abstract void cleanUp();
	
	protected File createTempDir() throws ImporterException {
		File tempDir = null;
		
		try {
			// generate a temp dir
			tempDir = Files.createTempDirectory(Fields.TEMP_FILE_PREFIX, PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString("rwx------") )).toFile();
			if( !tempDir.isDirectory () && !tempDir.mkdirs() )
				throw new ImporterException("The temporary directories could not be created: " + tempDir.getAbsolutePath ());
			
		} catch (IOException e) {
			LOGGER.error(e, "Cannot create temporary directory");
			throw new ImporterException("Cannot create temporary directory.", e);
		}
		
		return tempDir;
	}
	
	/**
	 * Wrapper class for VCard datamodel,
	 * adding equals and hashCode for better
	 * determination in HashMaps/-Sets
	 *
	 */
	protected class ImportVCard extends VCard {
		
		public ImportVCard( PersonIdent person ) {
			super(	GitNameTransformer.getFamilyName( person.getName() ),
					GitNameTransformer.getGivenName( person.getName() ),
					person.getEmailAddress(),
					"" );
		}
		
		public ImportVCard( String hgUserString ) {
			super(	HgNameTransformer.getFamilyName( hgUserString ),
					HgNameTransformer.getGivenName( hgUserString ),
					HgNameTransformer.getEmail( hgUserString ),
					"" );
		}
		
		public int hashCode() {
			
			if( getEmail() == null && getFamilyName() == null && getGivenName() == null && getOrganization() == null )
				return 0;
			
			int hash = 1;
			hash = hash * 17 + (getEmail() == null ? 0 : getEmail().hashCode());
			hash = hash * 31 + (getFamilyName() == null ? 0 : getFamilyName().hashCode());
			hash = hash * 13 + (getGivenName() == null ? 0 : getGivenName().hashCode());
			hash = hash * 07 + (getOrganization() == null ? 0 : getOrganization().hashCode());
			
			return hash;
		}
		
		public boolean equals(Object obj) {
			
			if( (obj instanceof ImportVCard || obj instanceof VCard) && obj.hashCode() == hashCode() )
				return true;
			else
				return false;
			
		}
	}
	
	protected static class GitNameTransformer {
		protected static String getGivenName( String name ) {
			if( name == null || name.isEmpty() )
				return "";
			
			String[] splitted = splitIt(name);
			if( splitted[0] == null || splitted[0].isEmpty() )
				return "";
			else
				return splitted[0];
		}
		
		protected static String getFamilyName( String name ) {
			if( name == null || name.isEmpty() )
				return "";
			
			String[] splitted = splitIt(name);
			if( splitted[0] == null || splitted[0].isEmpty() )
				return name;
			else if( splitted.length < 2 || splitted[1] == null || splitted[1].isEmpty() )
				return "";
			else
				return splitted[1];
		}
		
		private static String[] splitIt( String name ) {
			return name.split("\\s+", 2);
		}
	}
	
	protected static class HgNameTransformer {
		
		private static Pattern namePattern = Pattern.compile("((\\w+\\s+)+)(\\w+)");
		private static Pattern mailPattern = Pattern.compile("<?(\\w+@\\w+\\.\\w+)>?");
		
		protected static String getGivenName( String name ) {
			Matcher matcher = namePattern.matcher(name);
			if( matcher.find() ) {
				String result = matcher.group(1);
				return result != null && result.isEmpty() == false ? result.trim() : "";
			}
			else
				return "";
		}
		
		protected static String getFamilyName( String name ) {
			Matcher matcher = namePattern.matcher(name);
			if( matcher.find() ) {
				for( int i = 0; i < matcher.groupCount(); i++ )
					System.out.println( matcher.group(i) );
				String result = matcher.group(3);
				return result != null && result.isEmpty() == false ? result.trim() : "";
			}
			else
				return "";
		}
		
		protected static String getEmail( String name ) {
			Matcher matcher = mailPattern.matcher(name);
			if( matcher.find() ) {
				String result = matcher.group(1);
				return result != null && result.isEmpty() == false ? result.trim() : "";
			}
			else
				return "";
		}
	}
	
}
