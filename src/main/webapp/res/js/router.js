
var PageRouter = Backbone.Router.extend({
	
	routes: {
		"":								"start",
		"start":						"start",
		"create":						"create",
		"about":						"about",
		"archive/:archiveId(/:fileId)":	"archive"
	},
	
	initialize: function(options) {
		
		Backbone.history.start();
	},
	
	start: function() {
		
	},
	create: function() {
		
	},
	about: function() {
		
	},
	archive: function(archiveId, fileId) {
		
	}
	
});