
//the main collection with all archives in this workspace
var workspaceArchives = null;
var navigationView = null;
var archiveView = null;
var createView = null;
var messageView = null;

function displayError (err)
{
	console.log ("this is an error: " + err);
}

$(document).ready(function () {
	
	$("#noJs").remove ();
	
	var isIE = /*@cc_on!@*/false || !!document.documentMode;
	if (!isIE)
		$("#noBrowser").remove ();
	else
		return;
	
	// heatbeat
	$.get( RestRoot + "heartbeat", function(data) {
		if( data.substring(0, 2) == "ok" ) {
			// remove cookie note
			$("#noCookie").remove ();

			// fetch archives
			workspaceArchives = new ArchiveCollection();
			navigationView = new NavigationView({ collection: workspaceArchives });
			archiveView = new ArchiveView();
			createView = new CreateView();
			messageView = new MessageView();
			messageView.success("Hello World!");
			
			navigationView.fetch();
			
			// show the page
			$("#page").show();
		}
	});
	
});

