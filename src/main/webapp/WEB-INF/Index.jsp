<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions'%>
<!DOCTYPE html>
<html lang="en">
<head>
	<title>CombineArchiveWeb</title>
	<script type="text/javascript">
		var RestRoot = '${ContextPath}/rest/v1/';
	</script>
	<script type="text/javascript" src="res/js/3rd/jquery-2.0.3.min.js"></script>
<!-- 	<script type="text/javascript" src="res/js/3rd/jquery-ui-1.10.4.custom/js/jquery-ui-1.10.4.custom.min.js"></script> -->
	<script type="text/javascript" src="res/js/3rd/jquery-field-selection.js"></script>
	<script type="text/javascript" src="res/js/3rd/xdate.js"></script>
	<script type="text/javascript" src="res/js/3rd/jquery.colorbox-min.js"></script>
	
	<script type="text/javascript" src="res/js/3rd/underscore.js"></script>
	<script type="text/javascript" src="res/js/3rd/backbone-min.js"></script>
	
	<!-- Load jsTree and friends -->
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.types.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.wholerow.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.search.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.dnd.js"></script>
	
	<script type="text/javascript" src="res/js/models.js"></script>
	<script type="text/javascript" src="res/js/js.js"></script>
	
	<link rel="stylesheet" href="res/css/css.css" type="text/css" media="all" />
	<link rel="stylesheet" href="res/css/jstree/style.css" type="text/css" />
</head>
<body>
	<header> CombineArchiveWeb </header>
	<div id="noCookie">No cookies? No content.</div>
	<div id="noJs">No JavaScript? No content.</div>
	<div id="noBrowser">Unfortunately, we're not able to support
		Internet Explorer. Since we're fighting for open systems and
		reproducibility we're unwilling to buy and install proprietary
		software that is nothing but a big bug.</div>
	<div id="templates" style="display: none;">
		<div id="template-navigation">
			<ul id="nav">
				<li><a class="mainLinks highlight" data-linktype="page" data-page="startPage" id="nav-startlink">start</a></li>
				<li><a class="mainLinks" data-linktype="page" data-page="createPage" id="nav-createlink">create</a></li> 
				{{#	_.each(entries, function(entry) { }}
				<li><a class="mainLinks archives" data-linktype="archive" data-archiveid="{{# print(entry.id); }}" id="nav-archivelink-{{# print(entry.id); }}" >{{# print(entry.name); }}</a></li>
				{{# }); }}
			</ul>
		</div>
		<!-- **** -->
		<div id="template-create">
			<h2>MetaData</h2>
			<p>
				In order to maintain provenance you need to tell me who you are. This
				information will be included in the meta data of CombineArchives
				produced by you.
			</p>
			<p>
				<label for="userGivenName">Given Name:</label><br />
				<input type="text" id="userGivenName" name="userGivenName" placeholder="given name" value="{{# print(vcard.givenName); }}"/><br />
				<label for="userFamilyName">Family Name:</label><br />
				<input type="text" id="userFamilyName" name="userFamilyName" placeholder="family name" value="{{# print(vcard.familyName); }}" /><br />
				<label for="userMail">E-Mail:</label><br />
				<input type="mail" id="userMail" name="userMail" placeholder="email address" value="{{# print(vcard.email); }}" /><br />
				<label for="userOrganization">Organization:</label><br />
				<input type="text" id="userOrganization" name="userOrganization" placeholder="organization" value="{{# print(vcard.organization); }}" />
			</p>
			<p>
				<input type="button" class="save-vcard" value="Save" />
				<a href="#" class="test">Hallo</a>
			</p>
			<h2>CreateArchive</h2>
			<p>
				<label for="newArchiveName">Name of the Archive:</label><br />
				<input type="text" id="newArchiveName" name="newArchiveName" placeholder="archive name" />
			</p>
			
			<p>
				<input type="radio" id="newArchiveTemplate-Empty" name="newArchiveTemplate" value="empty" checked="checked" />
				<label for="newArchiveTemplate-Empty">Create an empty archive</label><br />
				
				<input type="radio" id="newArchiveTemplate-File" name="newArchiveTemplate" value="file" />
				<label for="newArchiveTemplate-File">Upload an existing archive</label><br />
				
				<input type="radio" id="newArchiveTemplate-CellMl" name="newArchiveTemplate" value="cellml" />
				<label for="newArchiveTemplate-CellMl">Create an archive from CellMl model repository</label>
			</p>
			<p>
				<input type="button" class="create-archive" value="Create Archive" />
			</p>
		</div>
		<!-- **** -->
		<div id="template-archive">
			
			<div class="archive-headarea">	
				<div class="archive-info">
					<strong>id:</strong>
						<span class="text-archive-id">{{# print(archive.id); }}</span><br />
					<strong>name:</strong>
						<span class="on-not-edit text-archive-name">{{# print(archive.name); }}</span>
						<input type="text" class="on-edit" name="archiveName" value="{{# print(archive.name); }}" placeholder="archive name" /><br />
						
					<div class="edit-link">
						<a class="archive-info-download on-not-edit" href="download/archive/{{# print(archive.id); }}.omex">[Download]</a>
						<a class="archive-info-edit on-not-edit" href="#">[Edit]</a>
						<a class="archive-info-save on-edit" href="#">[Save]</a>
						<a class="archive-info-cancel on-edit" href="#">[Cancel]</a>
					</div> 
				</div>
				
				<div class="archive-upload">
					<div class="dropbox">
						<div>
							<a href="#">Upload files</a>
							<div class="icon"> </div>
						</div>
						<input type="file" name="fileUpload" multiple="multiple" />
					</div>
				</div>
				<div style="clear: left;"></div>
			</div>
			
			<h2>Archive Content</h2>
			
			<div class="archive-contentarea">
				<div class="archive-filetree">
					<div>
						<div class="archive-jstree"></div>
					</div>
				</div>
				
				<div class="archive-fileinfo">
					<div>
						Wir bereiten nun den Versand Ihrer Sendung vor. Sie erhalten eine E-Mail von uns, wenn sie versandt wurde. Sie können jetzt noch eine Stornierung anfordern, falls Sie das möchten.
					</div>
				</div>
				<div style="clear: left;"></div>
			</div>
		</div>
		<!-- **** -->
		<div id="template-archive-entry">
			<h3>{{# print(entry.fileName); }}</h3>
			<div class="archive-entry-frame">
				<div class="archive-entry-header">
					<strong>file name:</strong>
						<span class="on-not-edit text-archive-entry-filename">{{# print(entry.fileName); }}</span>
						<input type="text" class="on-edit" name="archiveEntryFileName" value="{{# print(entry.fileName); }}" placeholder="file name" /><br />
					<strong>file path:</strong>
						<span class="text-archive-entry-filepath">{{# print(entry.filePath); }}</span><br />
					<strong>format:</strong> {{# print(entry.format); }}<br />
					<strong>master:</strong> {{# print(entry.master == true ? 'yes' : 'no'); }}<br /><br />
					
					<div class="edit-link">
						<a class="archive-file-download on-not-edit" href="download/file/{{# print(archiveId + entry.filePath); }}">[Download]</a>
						<a class="archive-file-edit on-not-edit" href="#">[Edit]</a>
						<a class="archive-file-save on-edit" href="#">[Save]</a>
						<a class="archive-file-cancel on-edit" href="#">[Cancel]</a>
					</div> 
				</div>
				
				<div class="archive-meta-area"></div>
			</div>
		</div>
		<!-- **** -->
		<div id="template-omex-meta-entry">
			<h4>OMEX entry</h4>
			<strong>created:</strong> {{# print( new XDate(created).toLocaleString() ); }}<br />
			<strong>modified:</strong>
				{{# _.each(modified, function(modDate) { }}
					{{# print( new XDate(modDate).toLocaleString() + "; &nbsp;&nbsp;" ); }}
				{{# }); }}<br />
			<strong>creators:</strong>
				{{# _.each(creators, function(vcard) { }}
					<div class="archive-meta-omex-creator-box" style="padding-left: 10px; margin: 10px 0;">
						<strong>given name: </strong>
							<span class="on-not-edit" data-field="givenName">{{# print(vcard.givenName); }}</span>
							<input type="text" class="on-edit" data-field="givenName" /> <br />
						<strong>family name: </strong>
							<span class="on-not-edit" data-field="familyName">{{# print(vcard.familyName); }}</span>
							<input type="text" class="on-edit" data-field="familyName" /> <br />
						<strong>E-Mail: </strong>
							<span class="on-not-edit" data-field="email">{{# print(vcard.email); }}</span>
							<input type="text" class="on-edit" data-field="email" /> <br />
						<strong>organization: </strong>
							<span class="on-not-edit" data-field="organization">{{# print(vcard.organization); }}</span>
							<input type="text" class="on-edit" data-field="organization" /> <br />
						
						<div class="edit-link">
							<a class="archive-meta-omex-creator-delete on-edit" href="#">[-]</a>
						</div>
					</div> 
				{{# }); }}
			<div class="edit-link">
				<a class="archive-meta-edit on-not-edit" href="#">[Edit]</a>
				<a class="archive-meta-omex-creator-add on-edit" href="#">[+]</a>
				<a class="archive-meta-save on-edit" href="#">[Save]</a>
				<a class="archive-meta-cancel on-edit" href="#">[Cancel]</a>
			</div> 
		</div>
		<!-- **** -->
		<div id="template-omex-meta-entry-creator">
			<div class="archive-meta-omex-creator-box" style="padding-left: 10px; margin: 10px 0;">
				<strong>given name: </strong>
					<span class="on-not-edit" data-field="givenName">{{# print(vcard.givenName); }}</span>
					<input type="text" class="on-edit" data-field="givenName" placeholder="{{# print(vcard.givenName); }}" /> <br />
				<strong>family name: </strong>
					<span class="on-not-edit" data-field="familyName">{{# print(vcard.familyName); }}</span>
					<input type="text" class="on-edit" data-field="familyName" placeholder="{{# print(vcard.familyName); }}" /> <br />
				<strong>E-Mail: </strong>
					<span class="on-not-edit" data-field="email">{{# print(vcard.email); }}</span>
					<input type="text" class="on-edit" data-field="email" placeholder="{{# print(vcard.email); }}" /> <br />
				<strong>organization: </strong>
					<span class="on-not-edit" data-field="organization">{{# print(vcard.organization); }}</span>
					<input type="text" class="on-edit" data-field="organization" placeholder="{{# print(vcard.organization); }}" /> <br />
				
				<div class="edit-link">
					<a class="archive-meta-omex-creator-delete on-edit" href="#">[-]</a>
				</div>
			</div> 
		</div>
		<!-- **** -->
		<div id="template-message-success">
			<div class="message message-success">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(message.title); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(txt); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( message.text ); }}</span>
				{{# } }}
			</div>
		</div>
		<div id="template-message-warning">
			<div class="message message-warning">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(message.title); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(txt); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( message.text ); }}</span>
				{{# } }}
			</div>
		</div>
		<div id="template-message-error">
			<div class="message message-error">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(message.title); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(txt); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( message.text ); }}</span>
				{{# } }}
			</div>
		</div>
		<!-- **** -->
	</div>


	<div id="page" style="display: none;">
		<nav id="navigation"></nav>
		<div id="message-bar"></div>
		<div id="startPage" class="subPage">
			<p>
				This is a web based interface to read, created, and modify CombineArchives. <br />
				<strong>We are not responsible for any loss of data.</strong>
			</p>
		</div>
		
		<div id="createPage" class="subPage" style="display: none;"></div>	
		<div id="archivePage" class="subPage" style="display: none;"></div>

	</div>
	<footer>
		built and maintained by <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a> @ University of Rostock
	</footer>
</body>
</html>
