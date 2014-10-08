<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions'%>
<!DOCTYPE html>
<html lang="en">
<head>
	<title>CombineArchiveWeb</title>
	<script type="text/javascript">
		var RestRoot = 'rest/v1/';
	</script>
	<script type="text/javascript" src="res/js/3rd/jquery-2.0.3.min.js"></script>
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
	
	<script type="text/javascript" src="res/js/3rd/rainbow/rainbow.js"></script>
	<script type="text/javascript" src="res/js/3rd/rainbow/language/generic.js"></script>
	<script type="text/javascript" src="res/js/3rd/rainbow/language/xml.js"></script>
	<link rel="stylesheet" href="res/js/3rd/rainbow/themes/github.css" type="text/css">
	
	<script type="text/javascript" src="res/js/models.js"></script>
	<script type="text/javascript" src="res/js/js.js"></script>
	
	<link rel="stylesheet" href="res/css/css.css" type="text/css" media="all" />
	<link rel="stylesheet" href="res/css/jstree/style.css" type="text/css" />
</head>
<body>
	<header>CombineArchiveWeb</header>
	<div id="noCookie">No cookies? No content.</div>
	<div id="noJs">No JavaScript? No content.</div>
	<div id="noBrowser">Unfortunately, we're not able to support
		Internet Explorer. Since we're fighting for open systems and
		reproducibility we're unwilling to buy and install proprietary
		software that is nothing but a big bug.</div>
	<div id="templates" style="display: none;">
		<div id="template-navigation">
			
			<div class="nav-container">
				<small>The current workspace contains the following archives:</small>
				<div style="clear: both;"></div>
				<ul id="nav-workspace" style="float: left;">
					{{#	_.each(entries, function(entry) { }}
					<li><a class="mainLinks archive-link archives" data-linktype="archive" data-archiveid="{{# print(entry.id); }}" id="nav-archivelink-{{# print(entry.id); }}" title="Archive {{# print(entry.name); }} in current Workspace">{{# print(entry.name); }}</a></li>
					{{# }); }}
				</ul>
				<ul id="nav-main" style="float: right;">
					<li><a class="mainLinks command-link highlight" data-linktype="page" data-page="start-page" id="nav-startlink">start</a></li>
					<li><a class="mainLinks command-link" data-linktype="page" data-page="create-page" id="nav-createlink">create</a></li> 
				</ul>
				<div style="clear: both;"></div>
			</div>
		</div>
		<!-- **** -->
		<div id="template-start">
			<h2>About</h2>
			<p class="about-logo">
				The CombineArchiveWeb is an interface for creating and exploring <a href="http://arxiv-web3.library.cornell.edu/abs/1407.4992">COMBINE archives</a>.
				It is part of the <a href="http://sems.uni-rostock.de/cat" title="CombineArchive Toolkit">CombineArchive Toolkit</a>.
				This project was developed in the scope of <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a>
				at the <a href="http://www.uni-rostock.de/">University of Rostock</a>.
				See <a href="http://sems.uni-rostock.de/cat" title="CombineArchive Toolkit">our website</a> for further information.
			</p>
			
			<h2>Disclaimer</h2>
			<p>
				This is a web based interface to read, create, modify, and share CombineArchives. <br />
				<strong>We are not responsible for any loss of data.</strong>
			</p>
			
			<h2>Share Workspace</h2>
			<p>
				We offer basic support for collaborative working.
				All the archives that 
				To share this workspace, just distribute the following link: <br />
				<input type="text" style="width: 100%;" readonly="readonly" value="{{# print(baseUrl); }}rest/share/{{# print(history.currentWorkspace); }}" /> 
				<br />However, you should not work on the same workspace from different locations at the same time!
			</p>
			
			<h2>Workspace History</h2>
			<p>
				{{# if( _.size(history.recentWorkspaces) == 1 ) { }}
					So far, you do not have a workspace history.
					As soon as you have shared workspaces with other people you will see this history growing.<br/>
					<a href="{{# print(baseUrl); }}rest/share/new-workspace">create new workspace</a>
				{{# } else { }}
					<a href="{{# print(baseUrl); }}rest/share/new-workspace">create new workspace</a>
					<ul>
					{{# _.each( history.recentWorkspaces, function(value, key, list) { }} 
						{{# if( key != history.currentWorkspace ) { }}
						<li>
							<strong>{{# print(value); }}</strong> &nbsp;
							<a href="{{# print(baseUrl); }}rest/share/{{# print(key); }}">{{# print(key); }}</a>
						</li>
						{{# } }}
					{{# }); }}
					</ul>
				{{# } }}
			</p>
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
<!-- 				<a href="#" class="test">Hallo</a> -->
<!-- 				<a href="#" class="test2">Hallo2</a> -->
			</p>
			<h2>CreateArchive</h2>
			<p>
				<label for="newArchiveName">Name of the Archive:</label><br />
				<input type="text" id="newArchiveName" name="newArchiveName" placeholder="archive name" />
			</p>
			
			<div style="margin: 0.5em 0; float: left;">
				<input type="radio" id="newArchiveTemplate-Empty" name="newArchiveTemplate" value="empty" checked="checked" />
				<label for="newArchiveTemplate-Empty">Create an empty archive</label><br />
				
				<input type="radio" id="newArchiveTemplate-File" name="newArchiveTemplate" value="file" />
				<label for="newArchiveTemplate-File">Upload an existing archive</label><br />
				<div class="on-archive-upload">
					<!-- <input type="file" name="newArchiveExisting" size="chars" /> -->
					<div class="dropbox">
						<div class="center-button">
							<a href="#">Upload file</a>
						</div>
						<div class="file-name-display">Test...</div>
						<input type="file" name="newArchiveExisting" />
					</div><br />
				</div>
				
				<input type="radio" id="newArchiveTemplate-CellMl" name="newArchiveTemplate" value="cellml" />
				<label for="newArchiveTemplate-CellMl">Create an archive from CellMl model repository</label><br />
				<div class="on-archive-cellml">
					<input type="text" name="newArchiveCellMlLink" placeholder="link to CellML repository" /><br />
				</div>
			</div>
			<div class="loading-container">
				<div class="loading-indicator">
					<div class="icon"></div>
				</div>
			</div>
			<div class="clearer"></div>
			
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
						<a class="archive-info-delete on-not-edit" href="#">[Delete]</a>
						<a class="archive-info-save on-edit" href="#">[Save]</a>
						<a class="archive-info-cancel on-edit" href="#">[Cancel]</a>
					</div> 
				</div>
				
				<div class="archive-upload">
					<div class="dropbox">
						<div class="center-button">
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
				<div class="archive-fileexplorer">
					<div class="archive-filetree">
						<div class="edit-link">
							<a class="archive-folder-add" href="#">[New folder]</a>
						</div>
						<div class="archive-jstree"></div>
					</div>
					
					<div class="archive-explorerexpand">
						<div class="archive-explorerexpand-text">
							<div class="archive-explorerexpand-text-files"><strong>&#8679;</strong>&nbsp;files&nbsp;<strong>&#8679;</strong></div>
							<div class="archive-explorerexpand-text-meta"><strong>&#8679;</strong>&nbsp;meta&nbsp;<strong>&#8679;</strong></div>
						</div>
					</div>
					<div style="clear: both;"></div>
				</div>
				
				<div class="archive-fileinfo">
					<div>
						To select a file and show it annotated meta data move your mouse on the left red bar or tap on it.
					</div>
				</div>
				<div style="clear: left;"></div>
			</div>
		</div>
		<!-- **** -->
		<div id="template-archive-entry">
			{{# if( entry.filePath != "/" ) { }}
			<h3><i class="file-icon" style="background-image: url('res/icon/{{# print(entry.format) }}');"></i>{{# print(entry.fileName); }}</h3>
			{{# } else { }}
			<h3>Archive Meta Information</h3>
			{{# } }}
			<div class="archive-entry-frame">
				<div class="archive-entry-header">
					{{# if( entry.filePath != "/" ) { }}
					<strong>file name:</strong>
						<span class="on-not-edit text-archive-entry-filename">{{# print(entry.fileName); }}</span>
						<input type="text" class="on-edit" name="archiveEntryFileName" value="{{# print(entry.fileName); }}" placeholder="file name" /><br />
					{{# } }}
					
					<strong>file path:</strong>
						<span class="text-archive-entry-filepath">{{# print(entry.filePath); }}</span><br />
						
					{{# if( entry.filePath != "/" ) { }}
					<strong>format:</strong> <a href="{{# print(entry.format); }}" target="_blank">{{# print(entry.format); }}</a><br />
					<strong>size:</strong> {{# print( bytesToSize(entry.fileSize) ); }}<br />
					{{# } }}
					
					<strong>master:</strong>
						<span class="on-not-edit text-archive-entry-master">{{# print(entry.master == true ? 'yes' : 'no'); }}</span>
						<input type="checkbox" class="on-edit" name="archiveEntryMaster" />
					<br /><br />
					
					<div class="edit-link">
						<a class="archive-meta-omex-add on-not-edit on-edit" href="#">[Add OMEX meta]</a>
						{{# if( entry.filePath != "/" ) { }}
						<a class="archive-file-download on-not-edit" href="download/file/{{# print(archiveId + entry.filePath); }}" target="_blank">[Download]</a>
						<a class="archive-file-edit on-not-edit" href="#">[Edit]</a>
						<a class="archive-file-delete on-not-edit" href="#">[Delete]</a>
						<a class="archive-file-save on-edit" href="#">[Save]</a>
						<a class="archive-file-cancel on-edit" href="#">[Cancel]</a>
						{{# } }}
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
					<nobr>[{{# print( new XDate(modDate).toLocaleString() ); }}]</nobr>&nbsp;&nbsp;
				{{# }); }}<br />
			<strong>creators:</strong>
				{{# _.each(creators, function(vcard) { }}
					<div class="archive-meta-omex-creator-box" style="padding-left: 10px; margin: 10px 0;">
						<strong class="on-not-edit">
							<span data-field="givenName">{{# print(vcard.givenName); }}</span>
							<span data-field="familyName">{{# print(vcard.familyName); }}</span>
						</strong>
						<span class="on-not-edit archive-meta-omex-creator-orga" data-field="organization">{{# print(vcard.organization); }}</span>
						<br class="on-not-edit" />
						<span class="on-not-edit" data-field="email">{{# print(vcard.email); }}</span>
						
						<div class="on-edit">
							<strong>given name: </strong>
								<input type="text" class="on-edit" data-field="givenName" /> <br />
							<strong>family name: </strong>
								<input type="text" class="on-edit" data-field="familyName" /> <br />
							<strong>E-Mail: </strong>
								<input type="text" class="on-edit" data-field="email" /> <br />
							<strong>organization: </strong>
								<input type="text" class="on-edit" data-field="organization" /> <br />
						</div>
						
						<div class="edit-link">
							<a class="archive-meta-omex-creator-delete on-edit" href="#">[-]</a>
						</div>
					</div> 
				{{# }); }}
			<div class="edit-link">
				<a class="archive-meta-edit on-not-edit" href="#">[Edit]</a>
				<a class="archive-meta-delete on-not-edit" href="#">[Delete]</a>
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
		<div id="template-xml-meta-entry">
			<h4>XML:RDF entry</h4>
			<div class="archive-meta-xml-code">
 				<pre><code data-language="xml">{{# print(xmlString); }}</code></pre>
			</div>
			<div class="edit-link">
<!-- 				<a class="archive-meta-edit on-not-edit" href="#">[Edit]</a> -->
				<a class="archive-meta-delete on-not-edit" href="#">[Delete]</a>
<!-- 				<a class="archive-meta-save on-edit" href="#">[Save]</a> -->
<!-- 				<a class="archive-meta-cancel on-edit" href="#">[Cancel]</a> -->
			</div> 
		</div>
		
		<!-- **** -->
		<div id="template-message-success">
			<div class="message message-success" data-referring="{{# print( message.referring ); }}">
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
			<div class="message message-warning" data-referring="{{# print( message.referring ); }}">
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
			<div class="message message-error" data-referring="{{# print( message.referring ); }}">
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
		<div id="start-page" class="subPage"></div>
		
		<div id="create-page" class="subPage" style="display: none;"></div>	
		<div id="archivePage" class="subPage" style="display: none;"></div>

	</div>
	<footer>
		built and maintained by <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a> @ <a href="http://www.uni-rostock.de/">University of Rostock</a>
	</footer>
</body>
</html>
