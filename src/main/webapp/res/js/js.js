
//the main collection with all archives in this workspace
var workspaceArchives = null;
var navigationView = null;
var archiveView = null;
var createView = null;
var messageView = null;
var templateCache = {};

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
	
	// read in and compile all tempates
	$("#templates").children().each( function(index, element) {
		var $el = $(element);
		var id = $el.attr("id");
		var html = $el.html();
		
		if( id != undefined && html != undefined && html.length > 1)
			templateCache[ id ] = _.template(html);
		$el.remove();
	});
	
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
			
			navigationView.fetch();
			
			// show the page
			$("#page").show();
		}
	});
	
});

