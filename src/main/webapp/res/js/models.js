
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
		'name': 'n/a'
	}
	
});

var VCardModel = Backbone.Model.extend({
	urlRoot: RestRoot + 'vcard',
	defaults: {
		'givenName': '',
		'familyName': '',
		'mail': '',
		'organization': ''
	}
});

var OmexMetaModel = Backbone.Model.extend({
	urlRoot: RestRoot + 'archives/0/entries/0/meta',
	defaults: {
		"type": "unknown",
		"changed": false
	},
	setUrl: function( archiveId, entryId )  {
		this.urlRoot = RestRoot + "archives/" + archiveId + "/entries/" + entryId + "/meta";
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
		
		// in case of re-render, store the selected main-button
		var $highlighted = this.$el.find(".highlight");
		var hid = null;
		if( $highlighted.length > 0 )
			hid = $highlighted.attr("id");
		
		$(this.el).html( this.template(json) );
		
		// restore selection
		if( hid != null ) {
			this.$el.find(".mainLinks").removeClass("highlight");
			this.$el.find("#" + hid).addClass("highlight");
		}
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
	selectArchive: function(archiveId) {
		var $navElem = this.$el.find( "#nav-archivelink-" + archiveId );
		
		if( $navElem.length <= 0 )
			return false;
		
		this.doNavigation( {"currentTarget": $navElem} );
		return true;
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
			
			var archiveId = $target.data("archiveid");
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
		
		return false;
	}
});

/**
 * "Abstract" Parent View for all kinds of Meta information
 */
var MetaEntryView = Backbone.View.extend({
	model: null,
	el: null,
	$el: null,
	
	template: null,
	
	initialize: function () {
		// 
	},
	render: function () {
		
		if( this.model == null || this.template == null )
			return;
		
		var json = this.model.toJSON();
		var text = this.template(json);
		this.$el.html( text );
	},
	leave: function () {
		this.undelegateEvents();
	},
	saveModel: function () {
		
		if( this.model == null )
			return false;
		
		var self = this;
		this.model.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log("saved meta entry " + self.model.get("id") + " >> " + model.get("id") + " successfully.");
				self.render();
			},
			error: function(model, response, options) {
				console.log("error while saving meta entry");
				console.log(response.responseText);
			}
		});
	}
});

/**
 * The Omex implementation
 */
var OmexMetaEntryView = MetaEntryView.extend({
	
	initialize: function () {
		this.template = _.template( $("#template-omex-meta-entry").html() ); 
	}
});

var ArchiveEntryView = Backbone.View.extend({
	model: null,
	el: null,
	
	entryId: null,
	archiveId: null,
	metaViews: {},
	
	initialize: function () {
		this.template = _.template( $('#template-archive-entry').html() );
	},
	render: function () {
		
		if( this.model == null )
			return;
		
		var json = this.model.toJSON();
		var text = this.template(json);
		this.$el.html( text );
		
		// detaches all meta entry views, if available
		_.each(this.metaViews, function(view) {
			view.leave();
		});
		
		var $metaArea = this.$el.find(".archive-meta-area");
		this.metaViews = {};
		_.each(this.model.get("meta"), function(metaEntry) {
			var view = createMetaViews(metaEntry);
			view.$el = $("<div class='archive-meta-entry' />");
			view.render();
			
			$metaArea.append(view.$el);
		});
		
		function createMetaViews(entry) {
			var model = null;
			var view = null;
			
			switch( entry.type ) {
				case "omex":
					model = new OmexMetaModel( entry );
					model.setUrl( this.archiveId, this.entryId );
					view = new OmexMetaEntryView({ "model": model });
					break;
				default:
					break;
			}
			
			return view;
		}
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
		//TODO
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
			"core": {
				"data": {"text": "/", "state": {opened: true}, "children": this.generateJsTreeJson() },
				"check_callback": true
			},
			"plugins": ["dnd", "search"]
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
	
	events: {
		// also see workaround for jsTree in render function
		'click .archive-info-edit': 'startArchiveEdit',
		'click .archive-info-save': 'saveArchive',
		'click .archive-info-cancel': 'cancelEdit',
		'click .archive-info-download': 'downloadArchive',
		'dragover .dropbox': 'dropboxOver',
		'drop .dropbox': 'dropboxDrop',
		'click .dropbox a': 'dropboxClick',
		'change .dropbox input': 'dropboxManual'
	},
	startArchiveEdit: function(event) {
		
		if( this.model == null )
			return false;
		
		// set edit field to correct value
		this.$el.find("input[name='archiveName']").val( this.model.get("name") );
		
		// show all edit-fields
		this.$el.find('.archive-info').addClass('edit');
		
		return false;
	},
	saveArchive: function(event) {
		if( this.model == null )
			return false;
		
		var newName = this.$el.find("input[name='archiveName']").val();
		if( newName === undefined || newName == null || newName == "" )
			return false;
		
		this.model.set({"name": newName});
		var self = this;
		
		this.model.save( {}, {
			success: function(model, response, options) {
				// everything ok
				console.log("updated model successfully");
				self.$el.find(".archive-info").removeClass("edit");
				self.model = model;
				// no complete self-re-render necessary 
				self.$el.find(".text-archive-name").html( model.get("name") );
				navigationView.render();
			},
			error: function(model, response, options) {
				console.log("error while update");
				console.log(response.responseText);
			}
		});
		
		return false;
	},
	cancelEdit: function(event) {
		this.$el.find('.archive-info').removeClass('edit');
		return false;
	},
	dropboxOver: function(event) {
		// disables default drag'n'drop behavior
		event.stopPropagation();
		event.preventDefault();
		
		// shows nice copy icon
		event.originalEvent.dataTransfer.dropEffect = 'copy';
	},
	dropboxDrop: function(event) {
		// disables default drag'n'drop behavior
		event.stopPropagation();
		event.preventDefault();
		
		if( this.collection == null )
			this.collection = new ArchiveEntryCollection();
		
		// get files transmitted with drop
		var files = event.originalEvent.dataTransfer.files;
		this.uploadFiles(files);
		
	},
	dropboxClick: function(event) {
		var $button = this.$el.find(".dropbox input[name='fileUpload']");
		$button.trigger("click");
	},
	dropboxManual: function(event) {
		// disables default drag'n'drop behavior
		event.stopPropagation();
		event.preventDefault();
		
		if( this.collection == null )
			this.collection = new ArchiveEntryCollection();
		
		// get files transmitted with drop
		console.log(event);
		var files = event.target.files;
		this.uploadFiles(files);
	},
	uploadFiles: function(files) {
		
		// form data object to push to server
		var formData = new FormData();
		var self = this;
		
		// adds all files to the data object
		_.each(files, function(file, index, list) {
			console.log(file);
			formData.append("files[]", file);
		});
		
		// show waiting stuff
		this.$el.find(".dropbox .icon").show();
		this.$el.find(".dropbox a").hide();
		
		// upload it
		$.ajax({
			"url": this.collection.url,
			"type": "POST",
			processData: false,
			contentType: false,
			data: formData,
			success: function(data) {
				console.log(data);
				self.fetchCollection(true);
				// not necessary to display, because complete view gets re-rendered
//				this.$el.find(".dropbox .icon").hide();
//				this.$el.find(".dropbox a").show();
			},
			error: function(data) {
				console.log(data);
			}
		});
	},
	downloadArchive: function(event) {
		window.location.href = "download/archive/" + this.model.get("id") + ".omex";
		console.log("download Archive: " + this.model.get("id"));
		
		return false;
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
	
	el: '#createPage',
	
	initialize: function() {
		var text = $('#template-create').html();
		this.template = _.template( text );
		
		if( this.model == null ) {
			this.model = new VCardModel();
			var self = this;
			this.model.fetch({
				success: function( model, reponse, options ) {
					self.render();
				}
			});
		}
	},
	render: function() {
		
		if( this.model == null )
			return;
		
		var json = {
				'vcard': this.model.toJSON()
		};
		this.$el.html( this.template(json) );
		
	},
	
	events: {
		'click .save-vcard': 'saveVCard',
		'click .create-archive': 'createArchive'
	},
	saveVCard: function(event) {
		this.model.set('givenName', this.$el.find("input[name='userGivenName']").val() );
		this.model.set('familyName', this.$el.find("input[name='userFamilyName']").val() );
		this.model.set('mail', this.$el.find("input[name='userMail']").val() );
		this.model.set('organization', this.$el.find("input[name='userOrganization']").val() );
		
		// TODO
		if( !this.model.isValid() ) {
			//alert( this.model.validationError );
		}
		
		this.model.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log(response);
			},
			error: function(model, response, options) {
				console.log(response);
			}
		});
		
		return false;
	},
	createArchive: function(event) {
		var archiveName = this.$el.find("input[name='newArchiveName']").val();
		var archiveTemplate = this.$el.find("input[name='newArchiveTemplate']:checked").val();
		
		// first of all, save the VCard
		saveVCard();
		
		// check if there are errors in this model
		if( !this.model.isValid() ) {
			// TODO
			return false;
		}
		
		var archiveModel = new ArchiveModel({'name': archiveName}, {'collection': workspaceArchives});
		
		if( archiveTemplate == undefined ) {
			// TODO
			alert("undefined type");
			return false;
		}
		else if( archiveTemplate == "empty" ) {
			// create new empty archive
			// nothing else do to...
		}
		else if( archiveTemplate == "file" ) {
			// create new archive based on a file
			// TODO make file upload and stuff...
		}
		else if( archiveTemplate == "cellml" ) {
			// create new archive based on a CellMl repository
			// TODO get url and stuff
		}
		else {
			// no known type of archive
			alert("unknown type");
			return false;
		}
		
		if( !archiveModel.isValid() ) {
			// model is not valid
			alert( "error: " + archiveModel.validationError );
			return false;
		}
		
		// push it
		archiveModel.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log("created new archive successfully.");
				// add model to navigation collection and re-renders the view
				navigationView.collection.add([model]);
				navigationView.render();
				navigationView.selectArchive( model.get("id") );
			},
			error: function(model, response, options) {
				console.log("error while creating new archive");
				console.log(response.responseText);
			}
		});
	}
	
	
});