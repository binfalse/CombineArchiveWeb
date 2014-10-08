
//the main collection with all archives in this workspace
var workspaceArchives = null;
var navigationView = null;
var archiveView = null;
var startView = null;
var createView = null;
var aboutView = null;
var messageView = null;
var templateCache = {};

function displayError (err) {
	console.log ("this is an error: " + err);
}

// http://stackoverflow.com/a/18650828/3910121
function bytesToSize(bytes) {
	if(bytes == 0) return '0 Byte';
	var k = 1000;
	var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
	var i = Math.floor(Math.log(bytes) / Math.log(k));
	return (bytes / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i];
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
			startView = new StartView();
			createView = new CreateView();
			aboutView = new AboutView();
			messageView = new MessageView();
			
			navigationView.fetch();
			
			$("#about-footer-link").click(function(event) {
				navigationView.goToPage("about-page");
				return false;
			});
			
			// show the page
			$("#page").show();
		}
	});
	
});

