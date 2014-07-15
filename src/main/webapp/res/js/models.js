
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

var ArchiveEntryView = Backbone.View.extend({
	model: null,
	el: null,
	
	entryId: null,
	archiveId: null,
	
	initialize: function () {
		this.template = _.template( $('#template-archive-entry').html() );
	},
	render: function () {
		
		if( this.model == null )
			return;
		
		var json = this.model.toJSON();
		var text = this.template(json);
		this.$el.html( text );
	},
	fetch: function(archiveId, entryId) {
		var self = this;
		
		if( archiveId == undefined )
			archiveId = this.archiveId;
		if( entryId == undefined )
			entryId = this.entryId;
		
		if( archiveId == null || entryId == null )
			return;
		
		this.archiveId = archiveId;
		this.entryId = entryId;
		
		this.model = new ArchiveEntryModel({id: entryId});
		this.model.setArchiveId(archiveId);
		this.model.fetch({
			reset: true,
			success: function(model, response, options) {
				self.render();
			}
		});
	},
	leave: function() {
		this.undelegateEvents();
	},
	
	events: {
		
	}
	
	
	
});

var ArchiveView = Backbone.View.extend({
	
	model: null,
	collection: null,
	entryView: null,
	
	el: '#archivePage',
	
	initialize: function () {
		this.template = _.template( $('#template-archive').html() );
	},
	
	events: {
//		'changed.jstree .archive-jstree': 'jstreeClick',
//		'changed .archive-jstree': 'jstreeClick'
	},
	
	render: function() {
		
		if( this.model == null || this.collection == null )
			return;
		
		var json = {
				'archive': this.model.toJSON(),
				'entries': this.collection.toJSON()
			};
		
		this.$el.html( this.template(json) );
		
		// generate json for filetree
		
		// init file tree
		this.$treeEl = this.$el.find('.archive-jstree');
		this.$treeEl.jstree({
			'core': {
				'data': {'text': "/", 'state': {opened: true}, 'children': this.generateJsTreeJson() }
			},
			'plugins': ["dnd", "search"]
		});
		// work-around for these strange jstree event names
		var self = this;
		this.$treeEl.on('changed.jstree', function(event, data) { self.jstreeClick.call(self, event, data); } );
		
		
		this.$el.show();
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
		
	},
	
	jstreeClick: function(event, data) {
		
		console.log(data);
		
		// directories are not yet handled
		if( data.node.original.type != 'file' )
			return;
		
		if( this.entryView != null )
			this.entryView.leave();
		
		// creates new view, the rest is magic ;)
		this.entryView = new ArchiveEntryView({
			$el: this.$el.find('.archive-fileinfo'),
			el: '.archive-fileinfo'
		});
		this.entryView.fetch( this.model.get('id'), data.node.data.id );
		
	},
	
	generateJsTreeJson: function() {
		
		var jstreeJson = [];
		
		this.collection.each( function(entry) {
			
			var path = entry.get('filePath').substring( 0, entry.get('filePath').length - entry.get('fileName').length );
			if( path == '/' || path == '' ) {
				// root element, just push it into the array
				jstreeJson.push({
					'text': entry.get('fileName'),
					'data': entry,
					'type': 'file',
//					'icon': 'icon-file',
					'state': {
						'opened': false,
						'disabled': false,
						'selected': false
					}
				});
			}
			else {
				// elment is non root, lets search for the branch it belongs to
				// split path and filter the empty parts out
				var pathSegment = _.filter( path.split('/'), function(seg) { return seg != "" && seg != " "; } );
				jstreeJson = deepInject(entry, jstreeJson, pathSegment, 0);
			}
			
			function deepInject(entry, currentBranch, pathSegment, pathIndex) {
				var found = false;
				var newBranch = false;
				
				if( !_.isArray(currentBranch) ) {
					console.log("type error!");
					return [];
				}
				
				if( pathIndex == pathSegment.length) {
					// is it the last piece? -> yes -> push file and leave
					currentBranch.push({
						'text': entry.get('fileName'),
						'data': entry,
						'type': 'file',
//						'icon': 'icon-file',
						'state': {
							'opened': false,
							'disabled': false,
							'selected': false
						}
					});
					found = true;
					return currentBranch;
				}
				
				
				if( found == false ) {
					// it is not the last piece of the path
					for( var index = 0; index < currentBranch.length; index++ ) {
						currentBranchEntry = currentBranch[index];
						
						if( currentBranchEntry.type == 'dir' && currentBranchEntry.text == pathSegment[pathIndex] ) {
							// found another piece of the path
							pathIndex++;
							newBranch = currentBranchEntry;
							found = true;
							break;
						} 
					}
				}
				
				if( found == false ) {
					// branch does not exist -> create it
					newBranch = {
							'text': pathSegment[pathIndex],
							'type': 'dir',
//							'icon': 'icon-dir',
							'state': {
								'opened': true,
								'disabled': false,
								'selected': false
							},
							'children': []
						};
					console.log("new branch");
					currentBranch.push(newBranch);
					pathIndex++;
				}
				
				// another step in recursion
				newBranch.children = deepInject(entry, newBranch.children, pathSegment, pathIndex);
				console.log(newBranch.children);
				return currentBranch;
			}
		});
		
		// return it
		return jstreeJson;
	}
	
});

var CreateView = Backbone.View.extend({
	
	model: null,
	
	el: '#createPage'
	
	
});