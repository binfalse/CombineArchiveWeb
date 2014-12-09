
var PageRouter = Backbone.Router.extend({
	
	routes: {
		"":								"start",
		"start":						"start",
		"create":						"create",
		"about":						"about",
		"archive/:archiveId(/:fileId)":	"archive"
	},
	
	initialize: function(options) {
		
		// init views
		workspaceArchives = new ArchiveCollection();
		navigationView = new NavigationView({ collection: workspaceArchives });
		archiveView = new ArchiveView();
		startView = new StartView();
		createView = new CreateView();
		aboutView = new AboutView();
		messageView = new MessageView();
		
		navigationView.fetch();
		Backbone.history.start();
	},
	
	// Helper functions
	selectArchive: function( archiveId ) {
		return this.navigate("archive/" + archiveId);
	},
	goToPage: function( page ) {
		return this.navigate( page );
	},
	
	// Route functions
	start: function() {
		navigationView.goToPage("start-page");
	},
	create: function() {
		navigationView.goToPage("create-page");
	},
	about: function() {
		navigationView.goToPage("about-page");
	},
	archive: function(archiveId, fileId) {
		navigationView.selectArchive(archiveId);
	}
	
});