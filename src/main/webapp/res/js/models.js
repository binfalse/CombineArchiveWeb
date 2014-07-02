
//Underscore template interpolate character to {{# ... }}
_.templateSettings =  {
		evaluate: /\{\{#(.+?)\}\}/g,
		interpolate: /\{\{([^#].*?)\}\}/g
};
var RestRoot = 'http://localhost:8080/CombineArchiveWeb/rest/v1/';

var ArchiveEntryModel = Backbone.Model.extend({
	urlRoot: RestRoot + 'archives/0/entries',
	defaults: {
		'filePath': '',
		'fileName': '',
		'format': '',
		'meta': {}
	},
	setArchiveId: function( archiveId ) {
		this.urlRoot = RestRoot + 'archives/' + archiveId + '/entries';
	}
});

var ArchiveModel = Backbone.Model.extend({
	urlRoot: RestRoot + 'archives',
	defaults: {
		'id': '000',
		'name': 'n/a'//,
//		'entries': new ArchiveEntryModel()
	}
	
});

var ArchiveCollection = Backbone.Collection.extend({
	model: ArchiveModel,
	url: RestRoot + 'archives'
});

var ArchiveEntryCollection = Backbone.Collection.extend({
	model: ArchiveEntryModel,
	url: RestRoot + 'archives/0/entries',
	setArchiveId: function( archiveId ) {
		this.url = RestRoot + 'archives/' + archiveId + '/entries';
	}
});

// views
var NavigationView = Backbone.View.extend({
	
	collection: null,
	el: '#navigation',
	
	initialize: function () {
		var templateText = $('#template-navigation').html();
//		console.log(templateText);
		this.template = _.template(templateText);
	},
	render: function() {
		var json = { 'entries': this.collection.toJSON() };
//		console.log(json);
		$(this.el).html( this.template(json) );
	},
	fetch: function() {
		if( this.collection == null )
			return;
		
		var self = this;
		
		this.collection.fetch({
			reset: false,
			success: function(collection, response, options) {
				console.log("ok");
				self.render();
			},
			error: function(collection, response, options) {
				console.log("error: can not get archives");
			}
		});
		
	},
	
	events: {
		'click .mainLinks': 'doNavigation' 
	},
	doNavigation: function (event) {
		
		// hide current page
		$('.subPage').hide();
		// do highlighting
		var $target = $(event.currentTarget);
		this.$el.find(".mainLinks").removeClass("highlight");
		$target.addClass("highlight");
		
		// do the magic
		
		var linkType = $target.data("linktype");
		if( linkType == "archive" ) {
			
			var archiveId = $(event.currentTarget).data("archiveid");
			var archiveModel = this.collection.get(archiveId);
			
			console.log( archiveId );
			
			if( archiveModel )
				archiveView.setArchive( archiveModel );
		}
		else if( linkType == "page" ) {
			$( "#" + $target.data("page") ).show();
		}
		else {
			// seems to be no valid navlink
			$("#startPage").show();
			alert("no valid link!");
		}
	}
});

var ArchiveView = Backbone.View.extend({
	
	model: null,
	collection: null,
	
	el: '#archivePage',
	
	initialize: function () {
		var templateText = $('#template-archive').html();
		console.log(templateText);
		this.template = _.template(templateText);
	},
	
	render: function() {
		
		if( this.model == null || this.collection == null )
			return;
		
		var json = {
				'archive': this.model.toJSON(),
				'entries': this.collection.toJSON()
			};
		
		$(this.el).html( this.template(json) );
		$(this.el).show();
	},
	
	setArchive: function( archiveModel ) {
		
		this.model = archiveModel;
		// gets all entries in for this archive
		this.fetchCollection(true);
		
		
	},
	
	fetchCollection: function( render ) {
		
		if( this.model == null )
			return;
		
		if( this.collection == null )
			this.collection = new ArchiveEntryCollection();
		
		var archiveId = this.model.get('id');
		var self = this;
		
		this.collection.setArchiveId( archiveId );
		this.collection.fetch({
			reset: true,
			success: function(collection, response, options) {
				// set archiveId for every model
				collection.forEach(function (element, index, list) {
					element.setArchiveId( archiveId );
				});
				// render 
				if( render == true )
					self.render();
			}
		});
		
	}
	
});

var CreateView = Backbone.View.extend({
	
	model: null,
	
	el: '#createPage'
	
	
});