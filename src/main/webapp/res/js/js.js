var currentArchive = "";

function displayError (err)
{
	console.log ("this is an error: " + err);
}


function naviSelect (link, allLinks, div, others)
{
	link.click (function ()
	{
	others.hide ();//.each (function () {$(this).hide ()});
	div.show ();
	$("." + allLinks).removeClass ("highlight");
	link.addClass ("highlight");
	});
}


function sendFile (file, name, archiveId)
{
	var smallList = $("uploadedFileList");
	var listItem = $("<span></span>").text (name + " uploading");
	smallList.append (listItem);
	/*
	var table = $("#uploadedfiles");
	var neu = $("<tr></tr>");
	table.append (neu);
	
	var td = $("<td></td>");
	neu.append(td);
	
	var neuName = $("<code></code>").text (name);
	var neuRm = $("<a></a>").append ($("<img/>").attr ("src", contextPath+"/res/img/failed.png").attr ("alt", "remove from list"));
	td.append (neuName).append (neuRm);
	
	td = $("<td></td>");
	neu.append(td);
	var neuSize = $("<small></small>").append ($("<code></code>").text (" "+humanReadableBytes(file.size)+" "));
	td.append (neuSize);
	
	td = $("<td></td>");
	neu.append(td);
	var neuAction = $("<small></small>");
	td.append(neuAction);*/
	
	var fd = new FormData ();
	fd.append('file', file);
	fd.append('archive', archiveId);
	//fd.append('other_data', 'foo bar');
	
	//console.log (name);
	
	
	$.ajax({
		url:  "api/upload.html",
		data: fd,
		cache: false,
		contentType: false,
		processData: false,
		type: 'POST',
		success: function(response, textStatus, xhr)
		{
			console.log("success");
			console.log(response);
			console.log(textStatus);
			console.log(xhr);
			listItem.text (name + " uploaded");
			setTimeout(function () {listItem.remove ();}, 5000);
			reloadArchives ();
		},
		error: function(response, textStatus, xhr)
		{
			console.log("failed");
			console.log(response);
			displayError ("upload failed: " + textStatus);
			listItem.text (name + " uploading failed");
			setTimeout(function () {listItem.remove ();}, 5000);
		}
	});
}

function handleFileSelect (evt)
{
	evt.stopPropagation();
	evt.preventDefault();
	
	var files = evt.dataTransfer.files;
	for (var i = 0, f; f = files[i]; i++)
		sendFile (f, f.name);
}

function handleDragOver (evt)
{
	evt.stopPropagation();
	evt.preventDefault();
	evt.dataTransfer.dropEffect = 'copy';
}

function initUpload (archiveId)
{
	var inp = document.getElementById('fileupload');
	inp.addEventListener('change', function(e)
	{
		var file = this.files[0];
		var fullPath = inp.value;
		var startIndex = (fullPath.indexOf('\\') >= 0 ? fullPath.lastIndexOf('\\') : fullPath.lastIndexOf('/'));
		var filename = fullPath.substring(startIndex+1);
		sendFile (file, filename, archiveId);
		
	}, false);
	
	var dropZone = document.getElementById('dropbox');
	dropZone.addEventListener('dragover', handleDragOver, false);
	dropZone.addEventListener('drop', handleFileSelect, false);
	dropZone.addEventListener("click", 
														function (event)
														{
															inp.click ();
														}, 
													 false);
	
	
}

function prepareArchiveFileClick (link, file)
{
	link.click (function ()
	{
		$("#metaWindow").empty ();
		var infoDiv = $("<div>Path: <strong>"+file.path+"</strong><br/>File Name: <strong>"+file.fileName+"</strong><br/>Format: <strong>"+file.format+"</strong></div>");
		
		var metaTabs = $("<div class='metaTabs'></div>");
		
		$("#metaWindow").append (infoDiv).append (metaTabs);
		
		// TODO: file preview?
		// TODO: download
		// TODO: tabs with meta data
	});
}

function prepareArchiveClick (link, entry, name, archiveId)
{
	link.click (function ()
	{
		$("#archiveName").text (name);
		console.log (entry);

		$("#filesList").empty ();
		$("#metaWindow").empty ();
		
		var myFiles = {};
		var toBeSorted = [];
		for (var key in entry)
		{
			if (entry.hasOwnProperty(key))
			{
				var file = entry[key];
				var link = $("<a></a>").text (file.path);
				prepareArchiveFileClick (link, file);
				myFiles[file.path] = link;
				toBeSorted.push(file.path);
			}
		}
		toBeSorted.sort ();
		for (var i = 0; i < toBeSorted.length; i++)
		{
			$("#filesList").append (myFiles[toBeSorted[i]]).append ($("<br/>"));
		}
		
		$("#files").append ();
		
		var addDiv = $("#addFile");
		
		$("uploadedFileList").empty ();
		
		addDiv.empty ().append (
			$("<div></div>").attr ("id", "dropbox")
			.append ("Drop Files Here")
			.append ($("<br/>"))
			.append ($("<a></a>").text ("Open Dialog"))
			.append ($("<input />").attr ("type", "file").attr ("id", "fileupload").attr ("multiple", ""))
		);
		initUpload (archiveId);
		
		currentArchive = archiveId;
	});
}

function reloadArchives ()
{
	$.get ("api/myarchives.js").done (function (data)
	{
		
		
		
		var nav = $("#nav");
		$(".archives").remove ();
		
		var archs = data.myarchives;
		console.log (archs);
		//console.log (data);
		//var archs = JSON.parse (data);
		for (var key in archs)
		{
			if (archs.hasOwnProperty(key))
			{
				console.log (key);
				var name = key.substring(37);
				
				var arch = archs[key];
				
				console.log (name);
				var link = $("<a></a>").addClass ("mainLinks").attr("id", "archlink-" + key.substring (0, 36)).text (name);
				nav.append ($("<li></li>").addClass ("archives").append (link));
				naviSelect (link, "mainLinks", $("#detailsPage"), $(".subPage"));
				
				prepareArchiveClick (link, archs[key], name, key.substring (0, 36));
				
				
				
				
			}
		}
		
		if (currentArchive)
		{
			$("#archlink-" + currentArchive).click ();
		}
		
	});
}

function initNewCreation ()
{
	// create new nav
	naviSelect ($("#createNew"), "createLinks", $("#createArchiveTab"), $(".createTab"));
	naviSelect ($("#createUpload"), "createLinks", $("#uploadArchiveTab"), $(".createTab"));
	naviSelect ($("#createCellml"), "createLinks", $("#cellmlArchiveTab"), $(".createTab"));
	naviSelect ($("#createBiomodels"), "createLinks", $("#biomodelsArchiveTab"), $(".createTab"));
	
	var createNewName = $("#createNewName");
	console.log ("test");
	
	var createNewBtn = $("#createNewButton");
	createNewBtn.click (function ()
	{
		console.log ("klick");
		if (!createNewName.val ())
		{
			displayError ("no name provided");
			return;
		}
		console.log (createNewName.val ());
		
		$.post ("api/createnew", {newArchiveName: createNewName.val ()}).done (function (data) {
			console.log ("successfully created archive");
			console.log (data);
			reloadArchives ();
		}).fail (function (data) 
		{
			displayError ("error creating archive");
			console.log (data);
		});
	});
	
	var cellmlBtn = $("#createCellMLButton");
	cellmlBtn.click (function ()
	{
		if (!createNewName.val ())
		{
			displayError ("no name provided");
			return;
		}
		var repoLink = $("#cellmlrepo");
		console.log (createNewName.val () + " --> " + repoLink.val ());
	});
	
	
}

function initExportButton() {
	
	$("#exportButton").click(function () {
		// simulate link clicking
		window.location.href = "download/archive/" + currentArchive + ".omex";
		console.log("download Archive: " + currentArchive);
		//return false;
	});
	
}

function init ()
{
	$("#noJs").remove ();
	
	
	var isIE = /*@cc_on!@*/false || !!document.documentMode;
	if (!isIE)
		$("#noBrowser").remove ();
	else
		return;
	
	var jqxhr = $.post( "api/heartbeat", function() {
	})
	.done(function(response, textStatus, xhr) {
		$("#noCookie").remove ();
		$("#page").show();
	});
	
	// links
	// page nav
	naviSelect ($("#startLink"), "mainLinks", $("#startPage"), $(".subPage"));
	naviSelect ($("#createLink"), "mainLinks", $("#createPage"), $(".subPage"));
	
	reloadArchives ();
	initNewCreation ();
	initExportButton();
}

document.addEventListener("DOMContentLoaded", init, false);

