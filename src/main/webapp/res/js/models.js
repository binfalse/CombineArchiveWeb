// see http://backbonejs.org/

//Underscore template interpolate character to {{# ... }}
_.templateSettings =  {
		evaluate: /\{\{#(.+?)\}\}/g,
		interpolate: /\{\{([^#].*?)\}\}/g
};
// TODO

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
		'email': '',
		'organization': ''
	},
	validate: function( attrs, options ) {
		if( attrs.givenName == undefined || attrs.givenName == "" )
			return "A given name should be provided";
		
		if( attrs.givenName.length < 2 )
			return "The given name should be at least 2 characters long.";
		
		if( attrs.familyName == undefined || attrs.familyName == "" )
			return "A family name should be provided";
		
		if( attrs.familyName.length < 2 )
			return "The family name should be at least 2 characters long.";
		
		if( attrs.email !== undefined && attrs.email !== "" ) {
		
			if( attrs.email.length < 6 )
				return "Your E-Mail address is to short.";
			
			var mailRegex = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
			if( !mailRegex.test(attrs.email) )
				return "The format of your E-Mail address is invalid.";
		}
		
		if( attrs.organization !== undefined && attrs.organization !== "" ) {
			
			if( attrs.organization.length < 4 )
				return "The name of your organization be at least 4 characters.";
		}
		
	}
});

var OmexMetaModel = Backbone.Model.extend({
	urlRoot: RestRoot + 'archives/0/entries/0/meta',
	defaults: {
		"creators": [],
		"created": "",
		"modified": [],
		"type": "omex",
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
		this.template = templateCache["template-navigation"];
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
				// render the navigation bar
				console.log(response);
				self.render();
			},
			error: function(collection, response, options) {
				console.log("error: can not get archives");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					text.unshift( "Can not get archives!" );
					messageView.error( "Unable to receive archives", text );
				}
				else
					messageView.error( "Unknown Error", "Unable to receive archives" );
				console.log(response);
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
	goToCreate: function() {
		var $navElem = this.$el.find( "#nav-createlink" );
		
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
			$("#start-page").show();
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
		var oldId = this.model.get("id");
		this.model.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log("saved meta entry " + oldId + " >> " + model.get("id") + " successfully.");
				self.$el.removeClass("edit");
				self.render();
				messageView.success("Meta data successfully saved");
			},
			error: function(model, response, options) {
				console.log("error while saving meta entry");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not save meta data", text );
				}
				else
					messageView.error( "Unknown Error", "Can not save meta data" );
			}
		});
	},
	deleteModel: function() {
		if( this.model == null )
			return false;
		
		var dialog = window.confirm("Do you really want to delete this meta entry from the file? This action is final.");
		if( dialog == true ) {
			
			var self = this;
			this.model.destroy({ dataType: "text",
				success: function(model, response, options) {
					// everything ok
					console.log("delted model successfully");
					messageView.success( "Deleted meta entry successfully" );
					
					// complete rerender (quick and dirty)
					if( self.archiveEntryView !== undefined )
						self.archiveEntryView.fetch();
					else
						archiveView.fetchCollection(true);
				},
				error: function(model, response, options) {
					console.log("error while update");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Error while deleting meta entry", text );
					}
					else
						messageView.error( "Unknown Error", "Error while deleting meta entry." );
				}
			});
			
		}
		
		return false;
	}
});

/**
 * The Omex implementation
 */
var OmexMetaEntryView = MetaEntryView.extend({
	
	initialize: function () {
		this.template = templateCache["template-omex-meta-entry"]; 
	},
	
	events: {
		"click .archive-meta-edit": "startEdit",
		"click .archive-meta-save": "saveEdit",
		"click .archive-meta-cancel": "cancelEdit",
		"click .archive-meta-delete": "deleteModel",
		"click .archive-meta-omex-creator-add": "addCreator",
		"click .archive-meta-omex-creator-delete": "removeCreator"
	},
	startEdit: function(event) {
		this.$el.find(".archive-meta-omex-creator-box").each( function(index, boxElement) {
			$(boxElement).find("input").each( function(index, inputElement) {
				var field = $(inputElement).attr("data-field");
				var html = $(boxElement).find("span[data-field='" + field + "']").html();
				$(inputElement).val(html);
			});
		});
		
		this.$el.addClass("edit");
		
		return false;
	},
	cancelEdit: function(event) {
		this.$el.removeClass("edit");
		this.render();
		
		return false;
	},
	saveEdit: function(event) {
		this.$el.find(".error-element").removeClass("error-element");
		var creators = [];
		var error = false;
		
		this.$el.find(".archive-meta-omex-creator-box").each( function(index, boxElement) {
			var vcard = new VCardModel();
			$(boxElement).find("input").each( function(index, inputElement) {
				var elem = $(inputElement);
				vcard.set( elem.attr("data-field"), elem.val() );
			});
			
			if( vcard.isValid() )
				creators.push( vcard.toJSON() );
			else {
				error = true;
				messageView.warning( vcard.validationError );
				$(boxElement).addClass("error-element");
			}
		});
		
		console.log(creators);
		
		if( !error ) {
			// update model
			this.model.set("creators", creators);
			// push to server
			this.saveModel();
		}
		
		return false;
	},
	addCreator: function(event) {
		
		var newForm = $( templateCache["template-omex-meta-entry-creator"]( {"vcard": createView.model.toJSON() } ) );
		this.$el.find(".edit-link:last").before(newForm);
//		newForm.insertAfter(".archive-meta-omex-creator-box:last");
		
		return false;
	},
	removeCreator: function(event) {
		
		$(event.target).parent().parent().animate({opacity: 0.15, height: 0}, "500", function() {
			$(this).remove();
		});
		
		return false;
	}
});

var ArchiveEntryView = Backbone.View.extend({
	model: null,
	el: null,
	
	entryId: null,
	archiveId: null,
	metaViews: {},
	
	initialize: function () {
		this.template = templateCache["template-archive-entry"];
	},
	render: function () {
		
		if( this.model == null )
			return;
		
		var json = { "entry": this.model.toJSON(), "archiveId": this.archiveId };
		var text = this.template(json);
		this.$el.html( text );
		
		// detaches all meta entry views, if available
		_.each(this.metaViews, function(view) {
			view.leave();
		});
		
		var self = this;
		var $metaArea = this.$el.find(".archive-meta-area");
		this.metaViews = {};
		_.each(this.model.get("meta"), function(metaEntry) {
			var view = createMetaViews.call(self, metaEntry);
			view.el = $('<div></div>');
			view.render();
			var content = $('<div class="archive-meta-entry"></div>').append(view.$el);
			$metaArea.append(content);
		});
		
		function createMetaViews(entry, context) {
			var model = null;
			var view = null;
			
			switch( entry.type ) {
				case "omex":
					model = new OmexMetaModel( entry );
					model.setUrl( this.archiveId, this.entryId );
					view = new OmexMetaEntryView({ "model": model });
					view.archiveEntryView = this;
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
			},
			error: function(model, response, options) {
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not fetch archive entry information", text );
				}
				else
					messageView.error( "Unknown Error", "Can not fetch archive entry information." );
			}
		});
	},
	leave: function() {
		this.undelegateEvents();
	},
	
	events: {
		"click .archive-file-edit": "startEntryEdit",
		"click .archive-file-cancel": "cancelEntryEdit",
		"click .archive-file-save": "saveEntry",
		"click .archive-file-delete": "deleteEntry",
		"click .archive-meta-omex-add": "addOmexMeta"
	},
	
	startEntryEdit: function(event) {
		if( this.model == null )
			return false;
		
		// set edit field to correct value
		this.$el.find("input[name='archiveEntryFileName']").val( this.model.get("fileName") );
		
		// show all edit-fields
		this.$el.find(".archive-entry-header").addClass("edit");
		
		return false;
	},
	cancelEntryEdit: function(event) {
		// hide all edit fields
		this.$el.find(".archive-entry-header").removeClass("edit");
		
		return false;
	},
	saveEntry: function(event) {
		
		if( this.model == null )
			return false;
		
		var newFileName = this.$el.find("input[name='archiveEntryFileName']").val();
		if( newFileName === undefined || newFileName == null || newFileName == "" )
			return false;
		
		var newFilePath = this.model.get("filePath");
		var len = newFilePath.length - this.model.get("fileName").length;
		newFilePath = newFilePath.substring( 0, len );
		newFilePath = newFilePath + newFileName;
		
		this.model.set({ "filePath": newFilePath, 
					"fileName": newFileName });
		
		var self = this;
		this.model.save( {}, {
			success: function(model, response, options) {
				// everything ok
				console.log("updated model successfully");
				messageView.success( "Saved " + model.get("filePath") + " successfully" );
				
				self.cancelEntryEdit(null);
				self.model = model;
				
				// complete rerender (quick and dirty)
				archiveView.fetchCollection(true);
				// no complete self-re-render necessary 
//				self.$el.find(".text-archive-entry-filename").html( model.get("fileName") );
//				self.$el.find(".text-archive-entry-filepath").html( model.get("filePath") );
				
			},
			error: function(model, response, options) {
				console.log("error while update");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Error while saving archive entry", text );
				}
				else
					messageView.error( "Unknown Error", "Error while saving archive entry." );
			}
		});
	},
	deleteEntry: function(event) {
		
		if( this.model == null )
			return false;
		
		var dialog = window.confirm("Do you really want to delete this file from the archive? This action is final.");
		if( dialog == true ) {
			
			this.model.destroy({ dataType: "text",
			success: function(model, response, options) {
				// everything ok
				console.log("delted model successfully");
				messageView.success( "Deleted " + model.get("filePath") + " successfully" );
				
				// complete rerender (quick and dirty)
				archiveView.fetchCollection(true);
			},
			error: function(model, response, options) {
				console.log("error while update");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Error while deleting archive entry", text );
				}
				else
					messageView.error( "Unknown Error", "Error while deleting archive entry." );
			}
		});
			
		}
		
		return false;
	},
	addOmexMeta: function(event) {
		
		model = new OmexMetaModel();
		model.setUrl( this.archiveId, this.entryId );
		view = new OmexMetaEntryView({ "model": model });
		
		view.el = $('<div></div>');
		view.render();
		var content = $('<div class="archive-meta-entry"></div>').append(view.$el);
		this.$el.find(".archive-meta-area").append(content);
		
		// go into edit mode and add one empty vcard set
		view.startEdit();
		view.addCreator();
		
		return false;
	}
	
});

var ArchiveView = Backbone.View.extend({
	
	model: null,
	collection: null,
	entryView: null,
	
	el: '#archivePage',
	
	initialize: function () {
		this.template = templateCache["template-archive"];
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
		var data = {
				"text": "/",
				"data": this.collection.findWhere( {"filePath": "/"} ),
				"state": {opened: true},
				"type": "root",
				"children": this.generateJsTreeJson()
			};
		
		// init file tree
		this.$treeEl = this.$el.find('.archive-jstree');
		this.$treeEl.jstree({
			"core": {
				"data": data,
				"check_callback": function(operation, node, node_parent, node_position, more) {
					// operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
                    // in case of 'rename_node' node_position is filled with the new node name
					
					if (operation === "move_node") {
                        return (node_parent.original.type == "dir" || node_parent.original.type == "root") && node.original.type == "file"; // only allow moving files into directories
                    }
                    return true;  //allow all other operations
				}
			},
			"dnd": {
				"check_while_dragging": true
			},
			"plugins": ["dnd", "search"]
		});
		// work-around for these strange jstree event names
		var self = this;
		this.$treeEl.on('changed.jstree', function(event, data) { self.jstreeClick.call(self, event, data); } );
		this.$treeEl.on("move_node.jstree", function(event, data) { self.jstreeMove.call(self, event, data); } );
		
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
			},
			error: function(collection, response, options) {
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not fetch archive entries", text );
				}
				else
					messageView.error( "Unknown Error", "Can not fetch archive entries." );
			}
		});
		
	},
	
	events: {
		// also see workaround for jsTree in render function
		"click .archive-info-edit": "startArchiveEdit",
		"click .archive-info-save": "saveArchive",
		"click .archive-info-cancel": "cancelEdit",
		"click .archive-info-delete": "deleteArchive",
		"dragover .dropbox": "dropboxOver",
		"drop .dropbox": "dropboxDrop",
		"click .dropbox a": "dropboxClick",
		"change .dropbox input": "dropboxManual",
		"click .archive-folder-add": "addFolder"
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
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not save archive", text );
				}
				else
					messageView.error( "Unknown Error", "Can not save archive." );
			}
		});
		
		return false;
	},
	cancelEdit: function(event) {
		this.$el.find('.archive-info').removeClass('edit');
		return false;
	},
	deleteArchive: function(event) {
		
		var dialog = window.confirm("Do you really want to delete the entire archive? This action is final.");
		
		if( dialog == true ) {
			this.model.destroy({ dataType: "text",
				success: function(model, response, options) {
					// everything ok
					console.log("deleted archive successfully");
//					messageView.success("Deleted archive successfully");
					navigationView.goToCreate();
					navigationView.fetch();
				},
				error: function(model, response, options) {
					console.log("error while deleting archive");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Can not delete archive", text );
					}
					else
						messageView.error( "Unknown Error", "Can not delete archive." );
				}
			});
		}
		
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
	addFolder: function(event) {
		
		var jstree = this.$treeEl.jstree(true);
		var currentNode = jstree.get_selected(true)[0];
		var dirNode = undefined;
		
		if( currentNode == undefined )
			dirNode = jstree.get_children_dom("#")[0];
		else if( currentNode.original.type == "dir" )
			dirNode = currentNode;
		else
			dirNode = jstree.get_parent(currentNode);
		
		console.log(currentNode);
		console.log(dirNode);
		var folderName = prompt("Please input new folder name", "new_folder");
		
		if( folderName == null )
			return false;
		
		var nodeData = {
				text: folderName,
				type: "dir",
				data: null,
				state: {
					'opened': true,
					'disabled': false,
					'selected': false
				}
		};
		jstree.create_node(dirNode, nodeData, "last", false, false);
		
		return false;
	},
	
	jstreeClick: function(event, data) {
		
		console.log(data);
		
		// directories are not yet handled
		if( data.node.original.type != 'file' && data.node.original.type != 'root' )
			return false;
		
		if( this.entryView != null )
			this.entryView.leave();
		
		// creates new view, the rest is magic ;)
		this.entryView = new ArchiveEntryView({
			el: this.$el.find(".archive-fileinfo")
		});
		this.entryView.fetch( this.model.get('id'), data.node.data.id );
		
	},
	jstreeMove: function(event, data) {
		
		console.log(data);
		var jstree = this.$treeEl.jstree(true);
		
		var path = data.node.text;
		var par = jstree.get_node(data.parent);
		while( par != null && par != undefined && par.id != "#" ) {
			var npath = par.text;
			if( npath.indexOf("/", npath.length - 1) === -1 )
				npath = npath + "/";
			
			path = npath + path;
			//path = par.text + ( par.text.indexOf("/", par.text.length-1) === -1 ? "/" : "") + path;
			par = jstree.get_node( jstree.get_parent(par) );
		}
		console.log(path);
		
		var model = this.collection.findWhere( {"id": data.node.data.id } );
		if( model.get("filePath") != path ) {
			// path has changed
			model.set("filePath", path);
			var self = this;
			model.save({}, {
				success: function(model, response, options) {
					// everything ok
					messageView.success( "file successfully moved." );
					self.fetchCollection(true);
				},
				error: function(model, response, options) {
					console.log("error moving file.");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Can not move file", text );
					}
					else
						messageView.error( "Unknown Error", "Can not move file." );
				}
				
			});
		}
		
	},
	
	generateJsTreeJson: function() {
		
		var jstreeJson = [];
		
		this.collection.each( function(entry) {
			
			if( entry.get('fileName') == null || entry.get('fileName') == '' || entry.get('fileName').length == 0 )
				// root element -> skip this (handled in render routine)
				return;
			
			var path = entry.get('filePath').substring( 0, entry.get('filePath').length - entry.get('fileName').length );
			if( path == '/' || path == '' ) {
				// file in root, just push it into the array
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
	
	el: '#create-page',
	
	initialize: function() {
		this.template = templateCache["template-create"];
		
		if( this.model == null ) {
			this.model = new VCardModel();
			var self = this;
			this.model.fetch({
				success: function( model, reponse, options ) {
					self.render();
				},
				error: function(model, response, options) {
					console.log("error getting own VCard");
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
		'click .create-archive': 'createArchive',
		'keydown #newArchiveName': 'createArchive',
		"click a.test": "addMsg"
	},
	addMsg: function(event) {
		messageView.success("Hello World");
		messageView.error("Error");
		messageView.warning("Halfdkas");
		messageView.success("sldfjfs");
		return false;
	},
	saveVCard: function(event) {
		this.model.set('givenName', this.$el.find("input[name='userGivenName']").val() );
		this.model.set('familyName', this.$el.find("input[name='userFamilyName']").val() );
		this.model.set('email', this.$el.find("input[name='userMail']").val() );
		this.model.set('organization', this.$el.find("input[name='userOrganization']").val() );
		
		if( !this.model.isValid() ) {
			messageView.warning("Meta information invalid", this.model.validationError);
			return false;
		}
		
		this.model.save({}, {
			success: function(model, response, options) {
				// everything ok
				messageView.success( "MetaData successfully saved." );
			},
			error: function(model, response, options) {
				console.log("error saving own VCard");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not save meta data", text );
				}
				else
					messageView.error( "Unknown Error", "Can not save meta data." );
			}
		});
		
		return false;
	},
	createArchive: function(event) {
		if (event.keyCode && event.keyCode != 13)
			return;
		
		var archiveName = this.$el.find("input[name='newArchiveName']").val();
		var archiveTemplate = this.$el.find("input[name='newArchiveTemplate']:checked").val();
		
		// first of all, save the VCard
		this.saveVCard();
		
		// check if there are errors in this model
		if( !this.model.isValid() ) {
			return false;
		}
		
		var archiveModel = new ArchiveModel({'name': archiveName}, {'collection': workspaceArchives});
		
		if( archiveTemplate == undefined ) {
			// TODO
			messageView.error("Undefined archive template type");
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
				messageView.success( "Archive " + model.get("name") + " successfully created.");
				// add model to navigation collection and re-renders the view
				navigationView.collection.add([model]);
				navigationView.render();
				navigationView.selectArchive( model.get("id") );
			},
			error: function(model, response, options) {
				console.log("error while creating new archive");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Can not create new archive", text );
				}
				else
					messageView.error( "Unknown Error", "Can not create new archive." );
			}
		});
	}
	
});

var StartView = Backbone.View.extend({
	
	el: "#start-page",
	
	initialize: function() {
		this.template = templateCache["template-start"];
		this.render();
	},
	render: function() {
		this.$el.html( this.template({}) );
	},
	
	events: {
		
	}
});

var MessageView = Backbone.View.extend({
	
	el: "#message-bar",
	
	initialize: function() {
		this.templateSuccess = templateCache["template-message-success"];
		this.templateWarning = templateCache["template-message-warning"];
		this.templateError	 = templateCache["template-message-error"];
	},
	render: function() {
		// not used?
	},
	showMessage: function( type, title, text ) {
		if( text == undefined ) {
			text = title;
			title = undefined;
		}
		
		var json = { "message": {
			"title": title,
			"text": text
		}};
		
		var template = null;
		if( type == "success" )
			template = this.templateSuccess;
		else if( type == "warning" )
			template = this.templateWarning;
		else 
			template = this.templateError;
		
		var html = template(json);
		var messageBlock = $(html).hide().appendTo(this.$el).fadeIn("quick");
		// show success messages just for 2 seconds
		if( type == "success" ) {
			messageBlock.delay( 2000 ).animate({opacity: 0.15, height: 0}, "700", function() {
				$(this).remove();
			});
		}
		
	},
	error: function( title, text ) {
		return this.showMessage("error", title, text);
	},
	warning: function( title, text ) {
		return this.showMessage("warning", title, text);
	},
	success: function( title, text ) {
		return this.showMessage("success", title, text);
	},
	
	events:  {
		"click .message-button-close": "closeMessage",
	},
	closeMessage: function(event) {
		event.preventDefault();
		
		console.log(event);
		$(event.target).parent().parent().animate({opacity: 0.15, height: 0}, "500", function() {
			$(this).remove();
		});
		return false;
	}
});

