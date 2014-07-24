<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions'%>
<html>
<head>
	<title>CombineArchiveWebWeb</title>
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
		software that is not more than a big bug.</div>

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
				<input type="text" id="userGivenName" name="userGivenName" placeholder="given name" value="{{# print(vcard.givenName); }}"/> <span id="userGivenNameAction"></span><br />
				<label for="userFamilyName">Family Name:</label><br />
				<input type="text" id="userFamilyName" name="userFamilyName" placeholder="family name" value="{{# print(vcard.familyName); }}" /> <span id="userFamilyNameAction"></span><br />
				<label for="userMail">E-Mail:</label><br />
				<input type="mail" id="userMail" name="userMail" placeholder="mail address" value="{{# print(vcard.mail); }}" /> <span id="userMailAction"></span><br />
				<label for="userOrganization">Organization:</label><br />
				<input type="text" id="userOrganization" name="userOrganization" placeholder="organization" value="{{# print(vcard.organization); }}" /> <span id="userOrganizationAction"></span>
			</p>
			<p>
				<input type="button" class="save-vcard" value="Save" />
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
			<h3>{{# print(fileName); }}</h3>
			<div style="padding-left: 10px;">
				<strong>file path:</strong> {{# print(filePath); }}<br />
				<strong>format:</strong> {{# print(format); }}<br />
				<strong>master:</strong> {{# print(master == true ? 'yes' : 'no'); }}<br /><br />
				
				<div class="edit-link">
					<a class="archive-file-download on-not-edit" href="#">[Download]</a>
					<a class="archive-file-edit on-not-edit" href="#">[Edit]</a>
					<a class="archive-file-save on-edit" href="#">[Save]</a>
					<a class="archive-file-cancel on-edit" href="#">[Cancel]</a>
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
					<p style="padding-left: 10px;">
						<strong>given name:</strong> {{# print(vcard.givenName); }}<br />
						<strong>family name:</strong> {{# print(vcard.familyName); }}<br />
						<strong>E-Mail:</strong> {{# print(vcard.email); }}<br />
						<strong>organization:</strong> {{# print(vcard.organization); }}<br />
					</p> 
				{{# }); }}
			<div class="edit-link">
				<a class="archive-meta-edit on-not-edit" href="#">[Edit]</a>
				<a class="archive-meta-save on-edit" href="#">[Save]</a>
				<a class="archive-meta-cancel on-edit" href="#">[Cancel]</a>
			</div> 
		</div>
	</div>


	<div id="page" style="display: none;">
		<nav id="navigation"></nav>
		
		<div id="startPage" class="subPage">
			<p>
				This is a web based interface to read, created, and modify CombineArchives. <br />
				<strong>We are not responsible for any loss of data.</strong>
			</p>
		</div>
		
		<div id="createPage" class="subPage" style="display: none;"></div>
		
		<div id="detailsPage" class="subPage" style="display: none;">
			<h2 id="archiveName"></h2>
			<div id="files">
				<div id="filesList"></div>
				<div id="metaWindow"></div>
				<div class="clearer"></div>
			</div>
			<h3>Upload Files</h3>
			<small>files won't be overwritten, we'll rename them in case of a conflict</small>
			<div id="addFile"></div>
			<div><small id="uploadedFileList"></small></div>
			<div id="export">
				<button id="exportButton">export as CombineArchive</button>
			</div>
		</div>
		
		<div id="archivePage" class="subPage" style="display: none;"></div>

	</div>
	<footer>
		build and maintained by <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a> @ University of Rostock
	</footer>
</body>
</html>