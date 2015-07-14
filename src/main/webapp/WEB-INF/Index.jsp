<%@page import="de.unirostock.sems.cbarchive.web.Fields"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions'%>
<!DOCTYPE html>
<!-- CombineArchiveWeb - a WebInterface to read/create/write/manipulate/... COMBINE archives -->
<!-- Copyright (C) 2014  SEMS Group

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. -->
<html lang="en">
<head>
	<title>CombineArchiveWeb</title>
	<script type="text/javascript">
		var RestRoot = 'rest/v1/';
		var SedMlWebToolsUrl = '<%= Fields.SEDML_WEBTOOLS_URL %>';
	</script>
	<script type="text/javascript" src="res/js/3rd/jquery-2.0.3.min.js"></script>
	<script type="text/javascript" src="res/js/3rd/xdate.js"></script>
	
	<script type="text/javascript" src="res/js/3rd/underscore-min.js"></script>
	<script type="text/javascript" src="res/js/3rd/backbone-min.js"></script>
	
	<!-- Load jsTree and friends -->
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.types.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.wholerow.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.search.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree/jstree.dnd.js"></script>
	
	<!-- rainbow syntax highlighter -->
	<script type="text/javascript" src="res/js/3rd/rainbow/rainbow.js"></script>
	<script type="text/javascript" src="res/js/3rd/rainbow/language/xml.js"></script>
	<link rel="stylesheet" href="res/js/3rd/rainbow/themes/pastie.css" type="text/css">
	
	<!-- popup framwork -->
	<script type="text/javascript" src="res/js/3rd/jquery-impromptu.min.js"></script>
	<link rel="stylesheet" href="res/css/jquery-impromptu.min.css" type="text/css">
	
	<!-- main scripts -->
	<% int scriptVersion = 2; %>
	<script type="text/javascript" src="res/js/models.js?version=<%= scriptVersion %>"></script>
	<script type="text/javascript" src="res/js/router.js?version=<%= scriptVersion %>"></script>
	<script type="text/javascript" src="res/js/js.js?version=<%= scriptVersion %>"></script>
	
	<link rel="stylesheet" href="res/css/css.css?version=<%= scriptVersion %>" type="text/css" media="all" />
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
	<% if( Fields.FEEDBACK_URL != null ) { %>
		<div id="feedback">
			<a href="<%= Fields.FEEDBACK_URL %>" title="feedback"></a>
		</div>
	<% } %>
	<div id="templates" style="display: none;">
		<div id="template-navigation">
			
			<div class="nav-container">
				<small>The current workspace contains the following archives:</small>
				<div style="clear: both;"></div>
				<ul id="nav-workspace" style="float: left; width: 70%;">
					{{#	_.each(entries, function(entry) { }}
					<li><a class="mainLinks archive-link archives" data-linktype="archive" data-archiveid="{{# print(entry.id); }}" id="nav-archivelink-{{# print(entry.id); }}" title="Archive {{# print(escape(entry.name)); }} in current Workspace">{{# print(escape(entry.name)); }}</a></li>
					{{# }); }}
				</ul>
				<ul id="nav-main" style="float: right; width: 29%;">
					<li><a class="mainLinks command-link highlight" data-linktype="page" data-page="start-page" id="nav-startlink">start</a></li>
					<li><a class="mainLinks command-link" data-linktype="page" data-page="about-page" id="nav-aboutlink">about</a></li>
					<li><a class="mainLinks command-link" data-linktype="page" data-page="create-page" id="nav-createlink">create</a></li> 
				</ul>
				<div style="clear: both;"></div>
			</div>
		</div>
		<!-- **** -->
		<div id="template-start">
			
			<h2>Share Workspace</h2>
			<p>
				We offer basic support for collaborative working.
				All the archives that 
				To share this workspace, just distribute the following link: <br />
				<div>
					<input type="text" style="display: block; float: left; width: calc(100% - 6.5em);" readonly="readonly" value="{{# print(baseUrl); }}rest/share/{{# print(current.workspaceId); }}" />
					<div class="edit-link" style="float: right;"> 
						<a href="mailto:?subject=Share%20CombineArchiveWeb%20Workspace:%20{{# print(encodeURIComponent( current.name )); }}&body=To%20take%20a%20look%20into%20the%20'{{# print(encodeURIComponent( current.name )); }}'%20Workspace,%20visit%20following%20link:%0D%0A%0D%0A{{# print(encodeURIComponent( baseUrl + 'rest/share/' + current.workspaceId )); }}">[Share]</a>
					</div>
					<div style="clear: both;"></div>
				</div>
				<br />However, you should not work on the same workspace from different locations at the same time!
			</p>
			
			<h2>Workspace History</h2>
			<p>
				{{# if( _.size(history) == 1 ) { }}
					So far, you only have one workspace in your history.
					As soon as you have shared workspaces with other people you will see this history growing.<br/>
				{{# } }}
				<div class="edit-link">
					<a href="{{# print(baseUrl); }}rest/share/new-workspace">[Create new workspace]</a>
				</div>
				<ul class="start-history-list" style="margin-top: 0;">
				{{# _.each( history, function(element, index, list) { }} 
					<li class="edit-object {{# element.current == true ? print('current-workspace') : print(''); }}">
						<span>
							{{# if( element.current != true ) { }}
								<a href="{{# print(baseUrl); }}rest/share/{{# print(element.workspaceId); }}">{{# print(escape(element.name)); }}</a>
							{{# } else { }}
								{{# print(escape(element.name)); }}
							{{# } }}
						</span>
						<span class="edit-link">
							<a class="start-history-rename" data-workspace-id="{{# print(element.workspaceId); }}" href="#">[Rename]</a>
							<a class="start-history-delete" data-workspace-id="{{# print(element.workspaceId); }}" href="#">[Delete]</a>
							<a href="mailto:?subject=Share%20CombineArchiveWeb%20Workspace:%20{{# print(encodeURIComponent( element.name )); }}&body=To%20take%20a%20look%20into%20the%20'{{# print(encodeURIComponent( element.name )); }}'%20Workspace,%20visit%20following%20link:%0D%0A%0D%0A{{# print(encodeURIComponent( baseUrl + 'rest/share/' + element.workspaceId )); }}">[Share]</a>
						</span>
					</li>
				{{# }); }}
				</ul>
				
			</p>
			
			<h2>Disclaimer</h2>
			<p>
				This is a web based interface to read, create, modify, and share CombineArchives. <br />
				<strong>We are not responsible for any loss of data.</strong>
			</p>
			
			<div class="stats-div"></div>
		</div>
		<!-- **** -->
		<div id="template-stats">
			<h2>Statistics</h2>
			<div class="stats-par">
				
				{{# if( stats.generated != undefined ) { }}
				<div class="stats-line stat-generated">
					<div class="attribute-name">timestamp:</div>
					<div class="attribute-detail">{{# print( new XDate(stats.generated).toLocaleString() ); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
			
				{{# if( stats.averageArchiveCount != undefined && !isNaN(stats.averageArchiveCount) ) { }}
				<div class="stats-line stat-averageArchiveCount">
					<div class="attribute-name">average archives per workspace:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.averageArchiveCount*100)/100 ); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.averageArchiveCountQuota != undefined && !isNaN(stats.averageArchiveCountQuota) ) { }}
				<div class="stats-line stat-averageArchiveCountQuota">
					<div class="attribute-name">average usage of 'archives per workspace' quota:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.averageArchiveCountQuota*10000)/100 ); }}%</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.averageWorkspaceAge != undefined && !isNaN(stats.averageWorkspaceAge) ) { }}
					{{# var avgAge = stats.averageWorkspaceAge/3600; }}
				<div class="stats-line stat-averageWorkspaceAge">
					<div class="attribute-name">average age of workspace:</div>
					<div class="attribute-detail">{{# print( Math.round(avgAge*100)/100 ); }}h</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.averageWorkspaceSize != undefined && !isNaN(stats.averageWorkspaceSize) ) { }}
				<div class="stats-line stat-averageWorkspaceSize">
					<div class="attribute-name">average size per workspace:</div>
					<div class="attribute-detail">{{# print( bytesToSize(Math.round(stats.averageWorkspaceSize*100)/100) ); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.averageWorkspaceSizeQuota != undefined && !isNaN(stats.averageWorkspaceSizeQuota) ) { }}
				<div class="stats-line stat-averageWorkspaceSizeQuota">
					<div class="attribute-name">average usage of 'size per workspace' quota:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.averageWorkspaceSizeQuota*10000)/100 ); }}%</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.totalArchiveCount != undefined && !isNaN(stats.totalArchiveCount) ) { }}
				<div class="stats-line stat-totalArchiveCount">
					<div class="attribute-name">total number of hosted archives:</div>
					<div class="attribute-detail">{{# print(stats.totalArchiveCount); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.totalSize != undefined && !isNaN(stats.totalSize) ) { }}
				<div class="stats-line stat-totalSize">
					<div class="attribute-name">total size of all hosted archives:</div>
					<div class="attribute-detail">{{# print( bytesToSize(stats.totalSize) ); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.totalSizeQuota != undefined && !isNaN(stats.totalSizeQuota) ) { }}
				<div class="stats-line stat-totalSizeQuota">
					<div class="attribute-name">usage of 'total size' quota:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.totalSizeQuota*10000)/100 ); }}%</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.totalWorkspaceCount != undefined && !isNaN(stats.totalWorkspaceCount) ) { }}
				<div class="stats-line stat-totalWorkspaceCount">
					<div class="attribute-name">total number of hosted workspaces:</div>
					<div class="attribute-detail">{{# print(stats.totalWorkspaceCount); }}</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.userArchiveCountQuota != undefined && !isNaN(stats.userArchiveCountQuota) ) { }}
				<div class="stats-line stat-userArchiveCountQuota">
					<div class="attribute-name">usage of 'archives per workspace' quota:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.userArchiveCountQuota*10000)/100 ); }}%</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				{{# if( stats.userWorkspaceSizeQuota != undefined && !isNaN(stats.userWorkspaceSizeQuota) ) { }}
				<div class="stats-line stat-userWorkspaceSizeQuota">
					<div class="attribute-name">usage of 'workspace size' quota:</div>
					<div class="attribute-detail">{{# print( Math.round(stats.userWorkspaceSizeQuota*10000)/100 ); }}%</div>
					<div class="clearer"></div>
				</div>
				{{# } }}
				
				<div class="clearer"></div>		
			</div>
		</div>		
		<!-- **** -->
		<div id="template-create">
			<h2>MetaData</h2>
			<p>
				In order to maintain provenance you need to tell me who you are. This
				information will be included in the meta data of CombineArchives
				produced by you.
			</p>
			<p class="create-vcard-box">
				<label for="userGivenName">Given Name:</label><br />
				<input type="text" id="userGivenName" name="userGivenName" placeholder="given name" value="{{# print(escape(vcard.givenName)); }}"/><br />
				<label for="userFamilyName">Family Name:</label><br />
				<input type="text" id="userFamilyName" name="userFamilyName" placeholder="family name" value="{{# print(escape(vcard.familyName)); }}" /><br />
				<label for="userMail">E-Mail:</label><br />
				<input type="mail" id="userMail" name="userMail" placeholder="email address" value="{{# print(escape(vcard.email)); }}" /><br />
				<label for="userOrganization">Organization:</label><br />
				<input type="text" id="userOrganization" name="userOrganization" placeholder="organization" value="{{# print(escape(vcard.organization)); }}" />
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
				<label for="newArchiveTemplate-Empty">Create an <b>empty archive</b></label><br />
				
				<input type="radio" id="newArchiveTemplate-File" name="newArchiveTemplate" value="file" />
				<label for="newArchiveTemplate-File"><b>Upload</b> an existing archive</label><br />
				<div class="create-parameter on-archive-upload">
					<div class="dropbox">
						<div class="center-button">
							<a href="#">Upload file</a>
						</div>
						<div class="file-name-display">Test...</div>
						<input type="file" name="newArchiveExisting" />
					</div><br />
				</div>
				
				<input type="radio" id="newArchiveTemplate-CellMl" name="newArchiveTemplate" value="cellml" />
				<label for="newArchiveTemplate-CellMl">Create an archive from <b>CellMl model repository</b></label><br />
				<div class="create-parameter on-archive-cellml">
					<input type="text" name="newArchiveCellMlLink" placeholder="link to CellML repository" size="50" /><br />
				</div>
				
				<input type="radio" id="newArchiveTemplate-Git" name="newArchiveTemplate" value="git" />
				<label for="newArchiveTemplate-Git">Create an archive from a <b>Git</b> repository</label><br />
				<div class="create-parameter on-archive-git">
					<input type="text" name="newArchiveGitLink" placeholder="link to Git repository" size="50" /><br />
				</div>
				
				<input type="radio" id="newArchiveTemplate-Http" name="newArchiveTemplate" value="http" />
				<label for="newArchiveTemplate-Http">Import a remote archive via <b>HTTP</b></label><br />
				<div class="create-parameter on-archive-http">
					<input type="text" name="newArchiveHttpLink" placeholder="HTTP link to the CombineArchive" size="50" /><br />
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
		<div id="template-about">
			
			<h2>About</h2>
			<p class="about-logo">
				The CombineArchiveWeb is an interface for creating and exploring <a href="http://arxiv-web3.library.cornell.edu/abs/1407.4992">COMBINE archives</a>.
				It is part of the <a href="http://sems.uni-rostock.de/cat" title="CombineArchive Toolkit">CombineArchive Toolkit</a>.
				This project was developed in the scope of <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a>
				at the <a href="http://www.uni-rostock.de/">University of Rostock</a>.
				See <a href="http://sems.uni-rostock.de/cat" title="CombineArchive Toolkit">our website</a> for further information.
			</p>
			
			<h2>License</h2>
			<p>
				This program is free software: you can redistribute it and/or modify
				it under the terms of the GNU General Public License as published by
				the Free Software Foundation, either version 3 of the License, or
				(at your option) any later version.
			</p>
			<p>
				This program is distributed in the hope that it will be useful,
				but WITHOUT ANY WARRANTY; without even the implied warranty of
				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
				GNU General Public License for more details.
			</p>
			<p>
				You should have received a copy of the GNU General Public License
				along with this program.  If not, see <a href="http://www.gnu.org/licenses/" target="_blank">http://www.gnu.org/licenses/</a>.
    		</p>
			
		</div>
		<!-- **** -->
		<div id="template-archive">
			
			<div class="archive-headarea">	
				<div class="archive-info">
					<div class="attribute-name">id:</div>
					<div class="attribute-detail"><span class="text-archive-id">{{# print(archive.id); }}</span></div>
					<div class="attribute-name">name:</div>
					<div class="attribute-detail">
						<span class="on-not-edit text-archive-name">{{# print(escape(archive.name)); }}</span>
						<input type="text" class="on-edit" name="archiveName" value="{{# print(escape(archive.name)); }}" placeholder="archive name" />
					</div>
						
					<div class="edit-link">
						<% if( Fields.SEDML_WEBTOOLS_URL != null && Fields.SEDML_WEBTOOLS_URL.isEmpty() == false ) { %>
						<a class="archive-info-simulate on-not-edit" href="{{# print(SedMlWebToolsUrl); print(baseUrl); }}download/archive/{{# print(workspace.workspaceId); }}/{{# print(archive.id); }}.omex" target="_blank">[Simulate]</a>
						<% } %>
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
							<div class="archive-explorerexpand-text-files">files</div>
							<div class="archive-explorerexpand-text-meta">meta</div>
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
			<h3><i class="file-icon" style="background-image: url('res/icon/{{# print(entry.format) }}');"></i>{{# print(escape(entry.fileName)); }}</h3>
			{{# } else { }}
			<h3>Archive Meta Information</h3>
			{{# } }}
			<div class="archive-entry-frame">
				<div class="archive-entry-header">
					{{# if( entry.filePath != "/" ) { }}
					<div class="attribute-name">file name:</div>
					<div class="attribute-detail">
						<span class="on-not-edit text-archive-entry-filename">{{# print(escape(entry.fileName)); }}</span>
						<input type="text" class="on-edit" name="archiveEntryFileName" value="{{# print(escape(entry.fileName)); }}" placeholder="file name" />
					</div>
					{{# } }}
					
					<div class="attribute-name">file path:</div>
					<div class="attribute-detail">
						<span class="text-archive-entry-filepath">{{# print(escape(entry.filePath)); }}</span>
					</div>
						
					{{# if( entry.filePath != "/" ) { }}
					<div class="attribute-name">format:</div>
					<div class="attribute-detail">
						<span class="on-not-edit"><a class="text-archive-entry-format" href="{{# print(entry.format); }}" target="_blank">{{# print(entry.format); }}</a></span>
						<input type="text" class="on-edit" name="archiveEntryFormat" value="{{# print(entry.format); }}" placeholder="file format" />
					</div>
					<div class="attribute-name">size:</div>
					<div class="attribute-detail">{{# print( bytesToSize(entry.fileSize) ); }}</div>
					{{# } }}
					
					<div class="attribute-name">master:</div>
					<div class="attribute-detail">
						<span class="on-not-edit text-archive-entry-master">{{# print(entry.master == true ? 'yes' : 'no'); }}</span>
						<input type="checkbox" class="on-edit" name="archiveEntryMaster" />
					</div>
					<br />
					
					<div class="edit-link">
						<a class="archive-meta-omex-add on-not-edit on-edit" href="#">[Add OMEX meta]</a>
						<a class="archive-meta-xml-add on-not-edit on-edit" href="#">[Add RDF/XML meta]</a>
						{{# if( entry.filePath != "/" ) { }}
						<a class="archive-file-download on-not-edit" href="download/file/{{# print(archiveId + escape(entry.filePath)); }}" target="_blank">[Download]</a>
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
			<div class="attribute-name">created:</div>
			<div class="attribute-detail">{{# print( new XDate(created).toLocaleString() ); }}</div>
			
			<div class="attribute-name">modified:</div>
			<div class="attribute-detail">
				{{# _.each(modified, function(modDate) { }}
					<nobr>[{{# print( new XDate(modDate).toLocaleString() ); }}]</nobr>&nbsp;&nbsp;
				{{# }); }}
				&nbsp;
			</div>
			<div style="clear: left;"></div>
			<div class="attribute-name">description:</div>
			<div class="attribute-detail">
				<span class="on-not-edit archive-meta-omex-description">{{# print(escape(description, true)); }}</span>
				<textarea class="on-edit" name="omexDescription">{{# print(description); }}</textarea>
				&nbsp;
			</div>
			<div class="attribute-name-br">creators:</div>
			<div class="attribute-detail-br">
				{{# _.each(creators, function(vcard) { }}
					<div class="archive-meta-omex-creator-box" style="padding-left: 10px; margin: 10px 0;">
						<strong class="on-not-edit">
							<span data-field="givenName">{{# print(escape(vcard.givenName)); }}</span>
							<span data-field="familyName">{{# print(escape(vcard.familyName)); }}</span>
						</strong>
						<span class="on-not-edit archive-meta-omex-creator-orga" data-field="organization">{{# print(escape(vcard.organization)); }}</span>
						<br class="on-not-edit" />
						<span class="on-not-edit" data-field="email">{{# print(escape(vcard.email)); }}</span>
						
						<div class="on-edit">
							<div class="attribute-name">given name:</div>
							<div class="attribute-detail"><input type="text" class="on-edit" data-field="givenName" /></div>
							<div class="attribute-name">family name:</div>
							<div class="attribute-detail"><input type="text" class="on-edit" data-field="familyName" /></div>
							<div class="attribute-name">E-Mail:</div>
							<div class="attribute-detail"><input type="text" class="on-edit" data-field="email" /></div>
							<div class="attribute-name">organization:</div>
							<div class="attribute-detail"><input type="text" class="on-edit" data-field="organization" /></div>
						</div>
						
						<div class="edit-link">
							<a class="archive-meta-omex-creator-delete on-edit" href="#">[-]</a>
						</div>
					</div> 
				{{# }); }}
			</div>
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
				<div class="attribute-name">given name:</div>
				<div class="attribute-detail">
					<input type="text" class="on-edit" data-field="givenName" placeholder="{{# print(escape(vcard.givenName)); }}" />
				</div>
				<div class="attribute-name">family name:</div>
				<div class="attribute-detail">
					<input type="text" class="on-edit" data-field="familyName" placeholder="{{# print(escape(vcard.familyName)); }}" />
				</div>
				<div class="attribute-name">E-Mail:</div>
				<div class="attribute-detail">
					<input type="text" class="on-edit" data-field="email" placeholder="{{# print(escape(vcard.email)); }}" />
				</div>
				<div class="attribute-name">organization:</div>
				<div class="attribute-detail">
					<input type="text" class="on-edit" data-field="organization" placeholder="{{# print(escape(vcard.organization)); }}" />
				</div>
				
				<div class="edit-link">
					<a class="archive-meta-omex-creator-delete on-edit" href="#">[-]</a>
				</div>
			</div> 
		</div>
		<!-- **** -->
		<div id="template-dialog-exists">
			<p>
				<strong>{{# print(escape(fileName)); }}</strong> already exists.
			</p>
			<ul>
				<li><strong>Rename</strong> the new file</li>
				<li><strong>Replace</strong> the old file, meta data will be copied</li>
				<li><strong>Override</strong> the old file, meta data will be destroyed</li>
			</ul>
		</div>
		<!-- **** -->
		<div id="template-xml-meta-entry">
			<h4>RDF/XML entry</h4>
			<div class="archive-meta-xml-code">
 				<pre class="on-not-edit"><code data-language="xml">{{# print(xmlEscapedString); }}</code></pre>
 				<textarea class="on-edit archive-meta-xml-text" >{{# print(xmlEscapedString); }}</textarea>
			</div>
			<div class="edit-link">
				<a class="archive-meta-edit on-not-edit" href="#">[Edit]</a>
				<a class="archive-meta-delete on-not-edit" href="#">[Delete]</a>
				<a class="archive-meta-save on-edit" href="#">[Save]</a>
				<a class="archive-meta-cancel on-edit" href="#">[Cancel]</a>
			</div> 
		</div>
		<!-- **** -->
		<div id="template-message-success">
			<div class="message message-success" data-referring="{{# print( message.referring ); }}">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(escape(message.title)); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(escape(txt)); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( escape(message.text) ); }}</span>
				{{# } }}
			</div>
		</div>
		<div id="template-message-warning">
			<div class="message message-warning" data-referring="{{# print( message.referring ); }}">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(escape(message.title)); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(escape(txt)); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( escape(message.text) ); }}</span>
				{{# } }}
			</div>
		</div>
		<div id="template-message-error">
			<div class="message message-error" data-referring="{{# print( message.referring ); }}">
				<div class="message-buttons">
					<a class="message-button-close" href="#">[x]</a>
				</div>
				
				{{# if( message.title ) { }}
						<strong>{{# print(escape(message.title)); }}: </strong>
				{{#	} }}
				
				{{# if( _.isArray(message.text) ) { }}
					<ul>
					{{# _.each(message.text, function(txt) { }}
						<li>{{# print(escape(txt)); }}</li>
					{{# }); }}
					</ul>
				{{# } }}
				
				{{# if( !_.isArray(message.text) ) { }}
						<span>{{# print( escape(message.text) ); }}</span>
				{{# } }}
			</div>
		</div>
		<!-- **** -->
	</div>


	<div id="page" style="display: none;">
		<nav id="navigation"></nav>
		<div id="message-bar"></div>
		<div id="start-page" class="subPage"></div>
		
		<div id="about-page" class="subPage" style="display: none;"></div>
		<div id="create-page" class="subPage" style="display: none;"></div>	
		<div id="archivePage" class="subPage" style="display: none;"></div>
	</div>
	<footer>
		built by <a href="http://sems.uni-rostock.de/" title="Simulation Experiment Management for Systems Biology">SEMS</a> @ <a href="http://www.uni-rostock.de/" title="University of Rostock">University of Rostock</a>
		&nbsp;|&nbsp;<a id="about-footer-link" href="#" title="About page">About</a>
	</footer>
</body>
</html>
