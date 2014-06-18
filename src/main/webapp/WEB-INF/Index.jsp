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
	
	<!-- Load jsTree and friends -->
	<script type="text/javascript" src="res/js/3rd/jstree.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree.types.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree.wholerow.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree.search.js"></script>
	<script type="text/javascript" src="res/js/3rd/jstree.dnd.js"></script>
	
	<script type="text/javascript" src="res/js/js.js"></script>
	<link rel='stylesheet' href='res/css/css.css' type='text/css' media='all' />
</head>
<body>
	<header> CombineArchiveWeb </header>
	<div id="noCookie">No cookies? No content.</div>
	<div id="noJs">No JavaScript? No content.</div>
	<div id="noBrowser">Unfortunately, we're not able to support
		Internet Explorer. Since we're fighting for open systems and
		reproducibility we're unwilling to buy and install proprietary
		software that is not more than a big bug.</div>
	<div id="page" style="display: none;">
		<nav>
			<ul id="nav">
				<li><a class="mainLinks highlight" id="startLink">start</a></li>
				<li><a class="mainLinks" id="createLink">create</a></li>
			</ul>
		</nav>
		<div id="startPage" class="subPage">
			This is a web based
			interface to read, created, and modify CombineArchives. We are not
			responsible for any loss of data.
		</div>
		
		<div id="createPage" class="subPage" style="display: none;">
			<h2>MetaData</h2>
			In order to maintain provenance you need to tell me who you are. This
			information will be included in the meta data of CombineArchives
			produced by you.
				<label for="userGivenName">Given Name:</label><br />
				<input type="text" id="userGivenName" name="userGivenName" placeholder="given name" /> <span id="userGivenNameAction"></span><br />
				<label for="userFamilyName">Family Name:</label><br />
				<input type="text" id="userFamilyName" name="userFamilyName" placeholder="family name" /> <span id="userFamilyNameAction"></span><br />
				<label for="userMail">E-Mail:</label><br />
				<input type="mail" id="userMail" name="userMail" placeholder="mail address" /> <span id="userMailAction"></span><br />
				<label for="userOrganization">Organization:</label><br />
				<input type="text" id="userOrganization" name="userOrganization" placeholder="organization" /> <span id="userOrganizationAction"></span><br />
			These information will be associated with a cookie in your browser.
			Thus, you do not need to remember any Password. As soon as you come
			back we'll remember you :)<br />
			If you're using a public PC you
			should make sure to clear cookies when you've finished working:
			logout.
			<h2>CreateArchive</h2>
			<div id="createArchive">
				<nav>
					<ul>
						<li><a class="createLinks highlight" id="createNew">Create new</a></li>
						<li><a class="createLinks" id="createUpload">Upload existing</a></li>
						<li><a class="createLinks" id="createCellml">Create from CellML Repository</a></li>
					</ul>
				</nav>
				Name of the archive to be created: <input type="text" id="createNewName" placeholder="name of the new workspace" /> 
				<div id="createArchiveTab" class="createTab">
					<button id="createNewButton">create new archive</button> <span id="createNewAction"></span>
				</div>
				<div id="uploadArchiveTab" class="createTab" style="display: none;">
					Upload archive: 
				</div>
				<div id="cellmlArchiveTab" class="createTab" style="display: none;">
					Link to the repo in the CellML model repository: <input type="text" id="cellmlrepo" placeholder="URL to the CellML repository"/> <button id="createCellMLButton">read repository</button>
				</div>
			</div>
		</div>
		
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

	</div>
	<footer>
		build and maintained by SEMS @ University of Rostock
	</footer>
</body>
</html>