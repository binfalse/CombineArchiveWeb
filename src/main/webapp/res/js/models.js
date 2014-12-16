// see http://backbonejs.org/

//Underscore template interpolate character to {{# ... }}
_.templateSettings =  {
		evaluate: /\{\{#(.+?)\}\}/g,
		interpolate: /\{\{([^#].*?)\}\}/g
};

var ArchiveEntryModel = Backbone.Model.extend({
	urlRoot: RestRoot + "archives/0/entries",
	defaults: {
		"filePath": "",
		"fileName": "",
		"format": "",
		"meta": {}
	},
	setArchiveId: function( archiveId ) {
		this.urlRoot = RestRoot + "archives/" + archiveId + "/entries";
	}
});

var ArchiveModel = Backbone.Model.extend({
	urlRoot: RestRoot + "archives",
	defaults: {
		"template": "plain",
		"name": "n/a"
	}
	
});

var VCardModel = Backbone.Model.extend({
	urlRoot: RestRoot + "vcard",
	defaults: {
		"givenName": "",
		"familyName": "",
		"email": "",
		"organization": ""
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
				return "Your E-Mail address is too short.";
			
			var mailRegex = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
			if( !mailRegex.test(attrs.email) )
				return "The format of your E-Mail address is invalid.";
		}
		
		if( attrs.organization !== undefined && attrs.organization !== "" ) {
			
			if( attrs.organization.length < 4 )
				return "The name of your organization be at least 4 characters.";
		}
		
	},
	isEmpty: function() {
		var empty = true;
		
		if( this.get("givenName") !== undefined && this.get("givenName") != "" )
			empty = false;
		
		if( this.get("familyName") !== undefined && this.get("familyName") !== "" )
			empty = false;
		
		if( this.get("email") !== undefined && this.get("email") !== "" )
			empty = false;
		
		if( this.get("organization") !== undefined && this.get("organization") !== "" )
			empty = false;
		
		return empty;
	}
});

var OmexMetaModel = Backbone.Model.extend({
	urlRoot: RestRoot + "archives/0/entries/0/meta",
	defaults: {
		"creators": [],
		"created": "",
		"modified": [],
		"type": "omex",
		"description": "",
		"changed": false
	},
	setUrl: function( archiveId, entryId )  {
		this.urlRoot = RestRoot + "archives/" + archiveId + "/entries/" + entryId + "/meta";
	}
});

var XmlMetaModel = Backbone.Model.extend({
	urlRoot: RestRoot + "archives/0/entries/0/meta",
	defaults: {
		"xmlString": "",
		"type": "xmltree",
		"changed": false
	},
	setUrl: function( archiveId, entryId )  {
		this.urlRoot = RestRoot + "archives/" + archiveId + "/entries/" + entryId + "/meta";
	}
});

var WorkspaceHistoryModel = Backbone.Model.extend({
	urlRoot: RestRoot + "workspaces",
	idAttribute: "workspaceId",
	defaults: {
		"workspaceId": "null",
		"name": "",
		"lastseen": "now",
		"current": false
	}
});
var WorkspaceHistoryCollection = Backbone.Collection.extend({
	model: WorkspaceHistoryModel,
	url: RestRoot + "workspaces"
});

var ArchiveCollection = Backbone.Collection.extend({
	model: ArchiveModel,
	url: RestRoot + "archives"
});

var ArchiveEntryCollection = Backbone.Collection.extend({
	model: ArchiveEntryModel,
	url: RestRoot + "archives/0/entries",
	setArchiveId: function( archiveId ) {
		this.url = RestRoot + "archives/" + archiveId + "/entries";
	}
});

// views
var NavigationView = Backbone.View.extend({
	
	collection: null,
	el: "#navigation",
	
	initialize: function () {
		this.template = templateCache["template-navigation"];
	},
	render: function() {
		var json = { "entries": this.collection.toJSON() };
		
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
				console.log("error: Cannot get archives");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					text.unshift( "Cannot get archives!" );
					messageView.error( "Unable to receive archives", text );
				}
				else
					messageView.error( "Unknown Error", "Unable to receive archives" );
				console.log(response);
			}
		});
		
	},
	selectArchive: function(archiveId, setHistory) {
		var $navElem = this.$el.find( "#nav-archivelink-" + archiveId );
		
		if( $navElem.length <= 0 )
			return false;
		
		return this.doNavigation( $navElem, setHistory );
	},
	goToPage: function(page, setHistory) {
		var $navElem = this.$el.find( "a[data-page='" + page + "']" );
		
		if( $navElem.length <= 0 )
			return false;
		
		return this.doNavigation( $navElem, setHistory );
	},
	doNavigation: function ($target, setHistory) {
		
		// hide current page
		$(".subPage").hide();
		// do highlighting
		this.$el.find(".mainLinks").removeClass("highlight");
		$target.addClass("highlight");
		
		// do the magic
		
		var linkType = $target.data("linktype");
		if( linkType == "archive" ) {
			
			var archiveId = $target.data("archiveid");
			var archiveModel = this.collection.get(archiveId);
			
			console.log( archiveId );
			
			if( archiveModel ) {
				archiveView.setArchive( archiveModel );
				if( setHistory == true )
					pageRouter.navigate("archive/" + archiveId);
				
				return true;
			}
		}
		else if( linkType == "page" ) {
			var page = $target.data("page"); 
			$( "#" + page ).show();
			
			if( setHistory == true )
				pageRouter.navigate( page.split("-")[0] );
			
			return true;
		}
		else {
			// seems to be no valid navlink
			$("#start-page").show();
			if( setHistory == true )
				pageRouter.navigate( "start" );
			
			alert("no valid link!");
		}
		
		return false;
	},
	
	events: {
		"click .mainLinks": "processNavigationEvent" 
	},
	processNavigationEvent: function(event) {
		this.doNavigation( $(event.currentTarget), true );
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
	messageId: null,
	
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
		
		if( this.messageId == undefined || this.messageId == null )
			this.messageId = "genericMetaMsg-" + this.model.get("id");
		
		var self = this;
		var oldId = this.model.get("id");
		this.model.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log("saved meta entry " + oldId + " >> " + model.get("id") + " successfully.");
				self.$el.removeClass("edit");
				self.render();
				messageView.success( undefined, "Meta data successfully saved", self.messageId );
			},
			error: function(model, response, options) {
				console.log("error while saving meta entry");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Cannot save meta data", text, self.messageId );
				}
				else
					messageView.error( "Unknown Error", "Cannot save meta data", self.messageId );
				
				self.model.fetch();
			}
		});
	},
	deleteModel: function() {
		if( this.model == null )
			return false;
		
		if( this.messageId == undefined || this.messageId == null )
			this.messageId = "genericMetaMsg-" + this.model.get("id");
		
		var dialog = window.confirm("Do you really want to delete this meta entry from the file? This action is final.");
		if( dialog == true ) {
			
			messageView.removeMessages( this.messageId );
			
			var self = this;
			this.model.destroy({ dataType: "text",
				success: function(model, response, options) {
					// everything ok
					console.log("delted model successfully");
					messageView.success( undefined, "Deleted meta entry successfully", self.messageId );
					
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
						messageView.error( "Error while deleting meta entry", text, self.messageId );
					}
					else
						messageView.error( "Unknown Error", "Error while deleting meta entry.", self.messageId );
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
		"keydown input[type='text']": "saveEdit",
		"click .archive-meta-cancel": "cancelEdit",
		"click .archive-meta-delete": "deleteModel",
		"click .archive-meta-omex-creator-add": "addCreator",
		"click .archive-meta-omex-creator-delete": "removeCreator"
	},
	startEdit: function(event, isnew) {
		this.$el.find("textarea[name='omexDescription']").val( this.$el.find("span.archive-meta-omex-description").html() );
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
		if( this.model.get("id") == undefined ) {
			this.$el.parent().remove();
		}
		else {
			this.$el.removeClass("edit");
			this.render();
			
			var messageId = "vcard-" + this.model.get("id");
			messageView.removeMessages( messageId );
		}
		
		return false;
	},
	saveEdit: function(event) {
		if (event.keyCode =! undefined && event.keyCode != null && event.keyCode != 13)
			return;
		
		this.$el.find(".error-element").removeClass("error-element");
		var creators = [];
		var error = false;
		this.messageId = "vcard-" + this.model.get("id");
		messageView.removeMessages( this.messageId );
		
		this.$el.find(".archive-meta-omex-creator-box").each( function(index, boxElement) {
			var vcard = new VCardModel();
			$(boxElement).find("input").each( function(index, inputElement) {
				var elem = $(inputElement);
				vcard.set( elem.attr("data-field"), elem.val() );
			});
			
			if( vcard.isEmpty() && createView.model != null && createView.model.isEmpty() == false )
				vcard.set( createView.model.toJSON() );
			
			if( vcard.isValid() )
				creators.push( vcard.toJSON() );
			else {
				error = true;
				messageView.warning( undefined, vcard.validationError, this.messageId );
				$(boxElement).addClass("error-element");
			}
		});
		
		console.log(creators);
		
		if( !error ) {
			// update model
			this.model.set("creators", creators);
			// set the description
			this.model.set("description", this.$el.find("textarea[name='omexDescription']").val() );
			// set type of meta data
			this.model.set("type", "omex");
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

/**
 * The XML implementation
 */
var XmlMetaEntryView = MetaEntryView.extend({
	
	initialize: function () {
		this.template = templateCache["template-xml-meta-entry"]; 
	},
	render: function () {
		
		if( this.model == null || this.template == null )
			return;
		
		var json = this.model.toJSON();
		json.xmlEscapedString = escape( json.xmlString );
		var text = this.template(json);
		this.$el.html( text );
		Rainbow.color( this.$el.get(0) );
	},
	generateBlankXml: function( filePath ) {
		
		if( filePath == null || filePath == undefined )
			filePath = "";
		
		// set some default xml for creation mode
		this.model.set("xmlString", '<?xml version="1.0" encoding="UTF-8"?>' + "\n" +
				'<rdf:Description xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" rdf:about="' + filePath + '">' + "\n" +
				'</rdf:Description>');
	},
	
	events: {
		"click .archive-meta-edit": "startEdit",
		"click .archive-meta-save": "saveEdit",
		"click .archive-meta-cancel": "cancelEdit",
		"click .archive-meta-delete": "deleteModel",
	},
	
	startEdit: function(event) {
		this.$el.find("textarea").val( this.model.get("xmlString") );
		this.$el.addClass("edit");
		
		return false;
	},
	cancelEdit: function(event) {
		
		this.messageId = "xmltree-" + this.model.get("id");
		messageView.removeMessages( this.messageId );
		
		if( this.model.get("id") == undefined ) {
			this.$el.parent().remove();
		}
		else {
			this.$el.removeClass("edit");
			this.render();
		}
		
		return false;
	},
	saveEdit: function(event) {
		
		this.messageId = "xmltree-" + this.model.get("id");
		messageView.removeMessages( this.messageId );
		
		// update the model
		this.model.set("xmlString", this.$el.find("textarea").val() );
		// set type of meta data
		this.model.set("type", "xmltree");
		// push to server
		this.saveModel();
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
			if( view != null ) {
				view.el = $("<div></div>");
				view.render();
				var content = $('<div class="archive-meta-entry"></div>').append(view.$el);
				$metaArea.append(content);
			}
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
					
				case "xmltree":
					model = new XmlMetaModel( entry );
					model.setUrl( this.archiveId, this.entryId );
					view = new XmlMetaEntryView({ "model": model });
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
					messageView.error( "Cannot fetch archive entry information", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot fetch archive entry information." );
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
		"keydown input[type='text']": "saveEntry",
		"click .archive-file-delete": "deleteEntry",
		"click .archive-meta-omex-add": "addOmexMeta",
		"click .archive-meta-xml-add": "addXmlMeta"
	},
	
	startEntryEdit: function(event) {
		if( this.model == null )
			return false;
		
		// set edit field to correct value
		this.$el.find("input[name='archiveEntryFileName']").val( this.model.get("fileName") );
		this.$el.find("input[name='archiveEntryFormat']").val( this.model.get("format") );
		if( this.model.get("master") == true ) 
			this.$el.find("input[name='archiveEntryMaster']").attr("checked", "checked");
		else
			this.$el.find("input[name='archiveEntryMaster']").removeAttr("checked");
		
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
		if (event.keyCode =! undefined && event.keyCode != null && event.keyCode != 13)
			return;
		
		if( this.model == null )
			return false;
		
		var newFileName = this.$el.find("input[name='archiveEntryFileName']").val();
		if( newFileName === undefined || newFileName == null || newFileName == "" )
			return false;
		
		var newFileMasterFlag = this.$el.find("input[name='archiveEntryMaster']").is(":checked");
		if( newFileMasterFlag !== true )
			newFileMasterFlag = false;
		
		var newFileFormat = this.$el.find("input[name='archiveEntryFormat']").val();
		if( newFileFormat === undefined || newFileFormat == null || newFileFormat == "" )
			return false;
		
		var newFilePath = this.model.get("filePath");
		var len = newFilePath.length - this.model.get("fileName").length;
		newFilePath = newFilePath.substring( 0, len );
		newFilePath = newFilePath + newFileName;
		
		this.model.set({ "filePath": newFilePath, 
					"fileName": newFileName,
					"format": newFileFormat,
					"master": newFileMasterFlag });
		
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
		
		return false;
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
		
		view.el = $("<div></div>");
		view.render();
		var content = $('<div class="archive-meta-entry"></div>').append(view.$el);
		this.$el.find(".archive-meta-area").append(content);
		
		// go into edit mode and add one empty vcard set
		view.startEdit();
		view.addCreator();
		
		return false;
	},
	addXmlMeta: function(event) {
		
		model = new XmlMetaModel();
		model.setUrl( this.archiveId, this.entryId );
		view = new XmlMetaEntryView({ "model": model });
		// generates blank Xml for creation
		view.generateBlankXml( this.model.get("filePath") );
		
		view.el = $("<div></div>");
		view.render();
		var content = $('<div class="archive-meta-entry"></div>').append(view.$el);
		this.$el.find(".archive-meta-area").append(content);
		
		// go into edit mode and add one empty vcard set
		view.startEdit();
		
		return false;
	}
	
});

var ArchiveView = Backbone.View.extend({
	
	model: null,
	collection: null,
	entryView: null,
	
	el: "#archivePage",
	
	initialize: function () {
		this.template = templateCache["template-archive"];
	},
	
	render: function( scrollValue ) {
		
		if( this.model == null || this.collection == null )
			return;
		
		var json = {
				"archive": this.model.toJSON(),
				"entries": this.collection.toJSON()
			};
		
		this.$el.html( this.template(json) );
		
		// generate json for filetree
		var data = {
				"text": "/",
				"data": this.collection.findWhere( {"filePath": "/"} ),
				"state": {opened: true},
				"icon": "res/css/folder.png",
				"type": "root",
				"children": this.generateJsTreeJson()
			};
		
		var self = this;
		// init file tree
		this.$treeEl = this.$el.find(".archive-jstree");
		this.$treeEl.jstree({
			"core": {
				"data": data,
				"check_callback": function(operation, node, node_parent, node_position, more) {
					// operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
                    // in case of 'rename_node' node_position is filled with the new node name
					
					if (operation === "move_node") {
                        return ( node_parent.parent == "#" || node_parent.original.type == "dir" || node_parent.original.type == "root") && node.original.type == "file"; // only allow moving files into directories
                    }
                    return true;  // allow all other operations
				}
			},
			"dnd": {
				"check_while_dragging": true
			},
			"plugins": ["dnd", "search"]
		});
		// work-around for these strange jstree event names
		
		this.$treeEl.on("changed.jstree", function(event, data) { self.jstreeClick.call(self, event, data); } );
		this.$treeEl.on("move_node.jstree", function(event, data) { self.jstreeMove.call(self, event, data); } );
		
		this.$el.show();
		if( scrollValue !== undefined )
			this.$treeEl.on("ready.jstree", function(event, data) { self.$el.find(".archive-filetree").scrollTop( scrollValue ); } );
	},
	
	setArchive: function( archiveModel ) {
		
		this.model = archiveModel;
		// gets all entries in for this archive
		this.fetchCollection(true);
	},
	setArchiveFile: function( fileId ) {
		
		this.hideExplorer(null, true);
		
		if( this.entryView != null )
			this.entryView.leave();
		
		// creates new view, the rest is magic ;)
		this.entryView = new ArchiveEntryView({
			el: this.$el.find(".archive-fileinfo")
		});
		this.entryView.fetch( this.model.get("id"), fileId );
	},
	
	fetchCollection: function( render, scrollValue ) {
		
		if( this.model == null )
			return;
		
		if( this.collection == null )
			this.collection = new ArchiveEntryCollection();
		
		var archiveId = this.model.get("id");
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
					self.render( scrollValue );
			},
			error: function(collection, response, options) {
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Cannot fetch archive entries", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot fetch archive entries." );
			}
		});
		
	},
	
	events: {
		// also see workaround for jsTree in render function
		"click .archive-info-edit": "startArchiveEdit",
		"click .archive-info-save": "saveArchive",
		"keydown input[name='archiveName']": "saveArchive",
		"click .archive-info-cancel": "cancelEdit",
		"click .archive-info-delete": "deleteArchive",
		
		"dragover .dropbox": "dropboxOver",
		"drop .dropbox": "dropboxDrop",
		"click .dropbox a": "dropboxClick",
		"change .dropbox input": "dropboxManual",
		"click .archive-folder-add": "addFolder",
		
//		"mouseenter .archive-fileexplorer": "showExplorer",
//		"click .archive-explorerexpand": "showExplorer",
//		"mouseleave .archive-fileexplorer": "hideExplorer"
		
		"click .archive-explorerexpand": "toggleExplorer",
		"mouseenter .archive-explorerexpand": "toggleExplorer",
		"mouseleave .archive-explorerexpand": "clearToggleTimer"
	},
	startArchiveEdit: function(event) {
		
		if( this.model == null )
			return false;
		
		// set edit field to correct value
		this.$el.find("input[name='archiveName']").val( this.model.get("name") );
		
		// show all edit-fields
		this.$el.find(".archive-info").addClass("edit");
		
		return false;
	},
	saveArchive: function(event) {
		if (event.keyCode =! undefined && event.keyCode != null && event.keyCode != 13)
			return;
		
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
					messageView.error( "Cannot save archive", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot save archive." );
			}
		});
		
		return false;
	},
	cancelEdit: function(event) {
		this.$el.find(".archive-info").removeClass("edit");
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
					pageRouter.goToPage("create");
					navigationView.fetch();
				},
				error: function(model, response, options) {
					console.log("error while deleting archive");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Cannot delete archive", text );
					}
					else
						messageView.error( "Unknown Error", "Cannot delete archive." );
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
		event.originalEvent.dataTransfer.dropEffect = "copy";
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
		// disables default click behavior
		event.stopPropagation();
		event.preventDefault();
		
		var $button = this.$el.find(".dropbox input[name='fileUpload']");
		$button.trigger("click");
		return false;
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
		
		// get current location
		var jstree = this.$treeEl.jstree(true);
		var currentNode = jstree.get_selected(true)[0];
		
		var path = "/";
		if( currentNode != undefined && currentNode.original.type != "root" ) {
			// there is a selected node and it is not the root node
			
			if( currentNode.original.type == "dir" )
				path = currentNode.original.text + "/";
			
			var par = jstree.get_node( "#" + jstree.get_parent(currentNode) );
			while( par != null && par != undefined && par != false && par.id != "#" ) {
				var npath = par.text;
				if( npath.indexOf("/", npath.length - 1) === -1 )
					npath = npath + "/";
				
				if( npath !== "/" )
					path = npath + path;
				//path = par.text + ( par.text.indexOf("/", par.text.length-1) === -1 ? "/" : "") + path;
				par = jstree.get_node( jstree.get_parent(par) );
			}
		}
		
		// Grabs the data
		var uploadTask = {
				"view": this,
				"path": path,
				"files": []
		};
		_.each(files, function(file) {
			uploadTask.files.push({
				"data": file,
				"option": null
			});
		});
		
		// show waiting stuff
		this.$el.find(".dropbox .icon").show();
		this.$el.find(".dropbox a").hide();
		// delegate stuff
		var self = this;
		setTimeout(function () { self.uploadFilesCheck(uploadTask); }, 0);
	},
	uploadFilesCheck: function(task) {
		var uploadTask = task;
		
		var result = _.every(uploadTask.files, function(file, index) {
			
			// file already processed
			if( file.option != null && file.option != undefined )
				return true;
			
			if( uploadTask.view.collection.find(function(elem) { 
				return elem.get("filePath") == uploadTask.path + file.data.name; }) ) {
				// file exists in archive
				var popupHtml = templateCache["template-dialog-exists"]({"fileName": uploadTask.path + file.data.name});
				$.prompt( popupHtml, {
					buttons: { "Rename": "rename", "Replace": "replace", "Override": "override", "Cancel": "cancel" },
					submit: function(event, value, message, fromVal) {
						uploadTask.files[index].option = value;
						// continue
						setTimeout(function () { uploadTask.view.uploadFilesCheck(uploadTask); }, 500);
					}
				});
				// breaks the loop
				return false;
			}
			else {
				// file does not exist => default value
				uploadTask.files[index].option = "default"; 
			}
			
			// continue
			return true;
		});
		
		if( result == true ) {
			// every file has passed the test
			uploadTask.view.uploadFilesDoIt(uploadTask);
		}
	},
	uploadFilesDoIt: function(uploadTask) {
		
		var formData = new FormData();
		var options = {};
		
		// adds all files to the data object (prepare to submit)
		_.each(uploadTask.files, function(file, index, list) {
			if( file.option !== "cancel" && file.option !== undefined && file.option !== null ) {
				formData.append("files[]", file.data);
				options[file.data.name] = file.option;
			}
		});
		if( _.size(options) <= 0 ) {
			// No file is selected to upload!
			uploadTask.view.$el.find(".dropbox .icon").hide();
			uploadTask.view.$el.find(".dropbox a").show();
			return false;
		}
			
		// add options and current path to formData, to upload files in the current directory
		formData.append("options", JSON.stringify(options) );
		formData.append("path", uploadTask.path);
		
		var self = uploadTask.view;
		// upload it
		$.ajax({
			"url": self.collection.url,
			"type": "POST",
			processData: false,
			contentType: false,
			data: formData,
			success: function(data) {
//				console.log(data);
				self.fetchCollection(true);
				
				// scanning for any non-critical erros.
				if( data !== undefined ) {
					_.each(data, function(element, index, list) {
						if( element.error == true )
							messageView.warning( element.filePath, element.message );
					});
				}
				else 
					messageView.error( "Unknown Error", "No response from the server!" );
				
				// not necessary to display, because complete view gets re-rendered TODO improve this!
//				this.$el.find(".dropbox .icon").hide();
//				this.$el.find(".dropbox a").show();
			},
			error: function(data) {
				self.$el.find(".dropbox .icon").hide();
				self.$el.find(".dropbox a").show();
				
				console.log(data);
				console.log("error uploading file.");
				if( data !== undefined && data.responseJSON !== undefined && data.responseJSON.status == "error" ) {
					var text = data.responseJSON.errors;
					messageView.error( "Cannot upload file", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot upload file." );
			}
		});
	},
	addFolder: function(event) {
		
		var jstree = this.$treeEl.jstree(true);
		var currentNode = jstree.get_selected(true)[0];
		var dirNode = undefined;
		
		if( currentNode == undefined )
			dirNode = jstree.get_children_dom("#")[0];
		else if( currentNode.original.type == "dir" || currentNode.original.type == "root" )
			dirNode = currentNode;
		else
			dirNode = jstree.get_parent(currentNode);
		
//		console.log(currentNode);
//		console.log(dirNode);
		var folderName = prompt("Please input new folder name", "new_folder");
		
		if( folderName == null )
			return false;
		
		// check if folder already exists
		var exists = _.find( jstree.get_children_dom(dirNode), function (element) {
			var elem = jstree.get_node(element);
			if( jstree.get_text(elem) == folderName )
				return true;
			return false;
		});
		
		// found element
		if( exists != undefined )
			return false;
		
		var nodeData = {
				text: folderName,
				type: "dir",
				icon: "res/css/folder.png",
				data: null,
				state: {
					"opened": true,
					"disabled": false,
					"selected": false
				}
		};
		jstree.create_node(dirNode, nodeData, "last", false, false);
		
		return false;
	},
	showExplorer: function(event) {
		if( this.explorerHideTimeout != undefined )
			clearTimeout( this.explorerHideTimeout );
		
		var self = this;
		this.explorerShowTimeout = setTimeout(function() {
			var el = self.$el.find(".archive-fileinfo").parent();
			var width = el.width();
			width = width - (1 + self.$el.find(".archive-explorerexpand").width());
			
			self.$el.find(".archive-fileinfo").fadeOut(200);
			self.$el.find(".archive-explorerexpand-text-files").fadeOut(200, function() {
				self.$el.find(".archive-explorerexpand-text-meta").fadeIn(200);
			});
			
			
			self.$el.find(".archive-filetree").animate({"width": width + "px"}, 500);
			self.explorerShowTimeout = undefined;
		}, 187);
	},
	hideExplorer: function(event, force) {
		if( this.explorerShowTimeout != undefined )
			clearTimeout( this.explorerShowTimeout );
		
		var self = this;
		this.explorerHideTimeout = setTimeout(function() {
			self.$el.find(".archive-explorerexpand-text-meta").fadeOut(150, function() {
				self.$el.find(".archive-explorerexpand-text-files").fadeIn(150);
			});
			
			self.$el.find(".archive-filetree").animate({"width": "0"}, 300, "linear", function() {
				self.$el.find(".archive-fileinfo").fadeIn(100);
			});
			self.explorerHideTimeout = undefined;
		}, force == true ? 0 : 187);
		
	},
	toggleExplorer: function(event, force) {
		
		var force = event.type == "click" ? true : false;
		if( this.$el.find(".archive-filetree").width() > 0 ) {
			// explorer is shown -> hide it
			this.hideExplorer(undefined, force);
		}
		else {
			// explorer is hidden -> show it
			this.showExplorer(undefined, force);
		}
	},
	clearToggleTimer: function(event) {
		
		if( this.explorerHideTimeout != undefined )
			clearTimeout( this.explorerHideTimeout );
		
		if( this.explorerShowTimeout != undefined )
			clearTimeout( this.explorerShowTimeout );
		
	},
	
	jstreeClick: function(event, data) {
		
		// directories are not yet handled
		if( data.node.original.type != "file" && data.node.original.type != "root" )
			return false;
		
		this.setArchiveFile( data.node.data.id, true );
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
		var scrollValue = this.$el.find(".archive-filetree").scrollTop();
		
		var model = this.collection.findWhere( {"id": data.node.data.id } );
		if( model.get("filePath") != path ) {
			// path has changed
			model.set("filePath", path);
			var self = this;
			model.save({}, {
				success: function(model, response, options) {
					// everything ok
					messageView.success( "file successfully moved." );
					self.fetchCollection(true, scrollValue);
				},
				error: function(model, response, options) {
					console.log("error moving file.");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Cannot move file", text );
					}
					else
						messageView.error( "Unknown Error", "Cannot move file." );
				}
				
			});
		}
		
	},
	
	generateJsTreeJson: function() {
		
		var jstreeJson = [];
		
		this.collection.each( function(entry) {
			
			if( entry.get("fileName") == null || entry.get("fileName") == "" || entry.get("fileName").length == 0 )
				// root element -> skip this (handled in render routine)
				return;
			
			var path = entry.get("filePath").substring( 0, entry.get("filePath").length - entry.get("fileName").length );
			if( path == "/" || path == "" ) {
				// file in root, just push it into the array
				jstreeJson.push({
					"text": escape(entry.get("fileName")),
					"data": entry,
					"type": "file",
					"icon": "res/icon/" + entry.get("format"),
					"state": {
						"opened": false,
						"disabled": false,
						"selected": false
					}
				});
			}
			else {
				// elment is non root, lets search for the branch it belongs to
				// split path and filter the empty parts out
				var pathSegment = _.filter( path.split("/"), function(seg) { return seg != "" && seg != " "; } );
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
						"text": escape(entry.get("fileName")),
						"data": entry,
						"type": "file",
						"icon": "res/icon/" + entry.get("format"),
						"state": {
							"opened": false,
							"disabled": false,
							"selected": false
						}
					});
					found = true;
					return currentBranch;
				}
				
				
				if( found == false ) {
					// it is not the last piece of the path
					for( var index = 0; index < currentBranch.length; index++ ) {
						currentBranchEntry = currentBranch[index];
						
						if( currentBranchEntry.type == "dir" && currentBranchEntry.text == pathSegment[pathIndex] ) {
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
							"text": escape(pathSegment[pathIndex]),
							"type": "dir",
							"icon": "res/css/folder.png",
							"state": {
								"opened": true,
								"disabled": false,
								"selected": false
							},
							"children": []
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
	file: null,
	
	el: "#create-page",
	
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
				"vcard": this.model.toJSON()
		};
		this.$el.html( this.template(json) );
		
	},
	
	events: {
		"click .save-vcard": "saveVCard",
		"click .create-archive": "createArchive",
		"keydown #newArchiveName": "createArchive",
		"click input[name='newArchiveTemplate']": "updateArchiveTemplate",
		"dragover .dropbox": "dropboxOver",
		"drop .dropbox": "dropboxDrop",
		"click .dropbox a": "dropboxClick",
		"change .dropbox input": "dropboxManual",
		"click a.test": "addMsg",
		"click a.test2": "rmvMsg"
	},
	addMsg: function(event) {
//		this.$el.find(".create-archive").attr("disabled", "disabled");
//		this.$el.find("#newArchiveName").attr("disabled", "disabled");
//		this.$el.find("input[name="newArchiveTemplate"]").attr("disabled", "disabled");
		messageView.warning("Hello", "World", "test");
		messageView.error("World", "Hello", "test");
		return false;
	},
	rmvMsg: function(event) {
		messageView.removeMessages("test");
	},
	updateArchiveTemplate: function (event) {
		var archiveTemplate = this.$el.find("input[name='newArchiveTemplate']:checked").val();
		if( archiveTemplate == undefined ) {
			// TODO
			messageView.error("Undefined archive template type");
			return false;
		}
		else if( archiveTemplate == "empty" ) {
			this.$el.find(".create-parameter").hide();
		}
		else if( archiveTemplate == "file" ) {
			this.$el.find(".create-parameter").hide();
			this.$el.find(".on-archive-upload").show();
		}
		else if( archiveTemplate == "cellml" ) {
			this.$el.find(".create-parameter").hide();
			this.$el.find(".on-archive-cellml").show();
		}
		else if( archiveTemplate == "hg" ) {
			this.$el.find(".create-parameter").hide();
			this.$el.find(".on-archive-hg").show();
		}
		else if( archiveTemplate == "http" ) {
			this.$el.find(".create-parameter").hide();
			this.$el.find(".on-archive-http").show();
		}
		else {
			// no known type of archive
			alert("unknown type");
			return false;
		}
	},
	saveVCard: function(event) {
		this.model.set("givenName", this.$el.find("input[name='userGivenName']").val() );
		this.model.set("familyName", this.$el.find("input[name='userFamilyName']").val() );
		this.model.set("email", this.$el.find("input[name='userMail']").val() );
		this.model.set("organization", this.$el.find("input[name='userOrganization']").val() );
		
		// remove old error messages
		messageView.removeMessages("ownvcard");
		this.$el.find(".error-element").removeClass("error-element");
		
		if( !this.model.isValid() ) {
			messageView.warning("Meta information invalid", this.model.validationError, "ownvcard");
			this.$el.find("p.create-vcard-box").addClass("error-element");
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
					messageView.error( "Cannot save meta data", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot save meta data." );
			}
		});
		
		return false;
	},
	createArchive: function(event) {
		if (event.keyCode =! undefined && event.keyCode != null && event.keyCode != 13)
			return;
		
		this.$el.find(".error-element").removeClass("error-element");
		
		var archiveName = this.$el.find("input[name='newArchiveName']").val();
		var archiveTemplate = this.$el.find("input[name='newArchiveTemplate']:checked").val();
		var self = this;
		
		if( archiveName == null || archiveName == undefined || archiveName == "" ) {
			messageView.error("An archive name should be provided.");
			this.$el.find("input[name='newArchiveName']").addClass("error-element");
			return false;
		}
		
		var archiveModel = new ArchiveModel({"name": archiveName}, {"collection": workspaceArchives});
		
		if( !archiveModel.isValid() ) {
			// model is not valid
			messageView.error("Archive parameter invalid", archiveModel.validationError);
			return false;
		}
		
		if( archiveTemplate == undefined ) {
			messageView.error("Undefined archive template type");
			return false;
		}
		else if( archiveTemplate == "empty" ) {
			// create new empty archive
			
			// first of all, save the VCard
			this.saveVCard();
			
			// check if there are errors in this model
			if( !this.model.isValid() ) {
				return false;
			}
			
			archiveModel.set("template", "plain");
		}
		else if( archiveTemplate == "file" ) {
			// create new archive based on a file
			// TODO check mime-type and size
			archiveModel.set("template", "existing");
			
			if( this.file == null ) {
				messageView.warning("Please select an file");
				return false;
			}
			
			// show waiting stuff
			showLoadingIndicator.call(self);
			
			var formData = new FormData();
			formData.append( "file", this.file );
			formData.append( "archive", JSON.stringify(archiveModel.toJSON()) );
			
			// upload it
			$.ajax({
				"url": archiveModel.urlRoot,
				"type": "POST",
				processData: false,
				contentType: false,
				data: formData,
				success: function(response) {
					console.log(response);
					
					// hide loading stuff and clear inputs
					hideLoadingIndicator.call(self);
					self.$el.find("input[name='newArchiveName']").val("");
					self.$el.find("input[name='newArchiveCellMlLink']").val("");
					self.$el.find(".dropbox .file-name-display").hide();
					self.file = null;
					
					if( response !== undefined ) {
						var model = new ArchiveModel( {
							"id": response.id,
							"name": response.name
						});
						console.log( model.toJSON() );
						
						messageView.success( "Archive " + model.get("name") + " successfully uploaded.");
						
						// add model to navigation collection and re-renders the view
						navigationView.collection.add([model]);
						navigationView.render();
						pageRouter.selectArchive( model.get("id") );
					}
				},
				error: function(response) {
					console.log(response);
					// hide loading stuff
					hideLoadingIndicator.call(self);
					
					console.log("error while uploading new archive");
					if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
						var text = response.responseJSON.errors;
						messageView.error( "Cannot create new archive", text );
					}
					else
						messageView.error( "Unknown Error", "Cannot create new archive." );
				}
			});
			
			return false;
		}
		else if( archiveTemplate == "cellml" ) {
			// create new archive based on a CellMl repository
			archiveModel.set("template", "hg");
			
			var link = this.$el.find("input[name='newArchiveCellMlLink']").val();
			if( !link.match(/https?:\/\/models.cellml.org\//) && !link.match(/^hg\ clone\ https?:\/\/models\.cellml\.org\//) && !link.match(/https?:\/\/models.physiomeproject.org\//) && !link.match(/^hg\ clone\ https?:\/\/models\.physiomeproject\.org\//) ) {
				messageView.error ("expected a link to a cellml or physiome repository");
				return false;
			}
			// add link to the model
			archiveModel.set ("hgLink", link);
		}
		else if( archiveTemplate == "hg" ) {
			// create new archive based on a Mercurial repository
			archiveModel.set("template", "hg");
			
			var link = this.$el.find("input[name='newArchiveHgLink']").val();
			// add link to the model
			archiveModel.set ("hgLink", link);
		}
		else if( archiveTemplate == "http" ) {
			// create new archive based on a http link
			archiveModel.set("template", "http");
			
			var link = this.$el.find("input[name='newArchiveHttpLink']").val();
			// add link to the model
			archiveModel.set ("url", link);
		}
		else {
			// no known type of archive
			messageView.error("Undefined archive template type");
			return false;
		}
		
		showLoadingIndicator.call(self);
		// push it
		archiveModel.save({}, {
			success: function(model, response, options) {
				// everything ok
				console.log("created new archive successfully.");
				messageView.success( "Archive " + model.get("name") + " successfully created.");
				
				// hide loading stuff and clear inputs
				hideLoadingIndicator.call(self);
				self.$el.find("input[name='newArchiveName']").val("");
				self.$el.find("input[name='newArchiveCellMlLink']").val("");
				self.$el.find(".dropbox .file-name-display").hide();
				self.file = null;
				
				// add model to navigation collection and re-renders the view
				navigationView.collection.add([model]);
				navigationView.render();
				pageRouter.selectArchive( model.get("id") );
			},
			error: function(model, response, options) {
				console.log("error while creating new archive");
				
				// hide loading stuff
				hideLoadingIndicator.call(self);
				
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Cannot create new archive", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot create new archive." );
			}
		});
		
		function showLoadingIndicator() {
			//loading stuff
			this.$el.find(".loading-indicator").show();
			
			// disable inputs
			this.$el.find(".create-archive").attr("disabled", "disabled");
			this.$el.find("#newArchiveName").attr("disabled", "disabled");
			this.$el.find("input[name='newArchiveTemplate']").attr("disabled", "disabled");
		}
		function hideLoadingIndicator() {
			this.$el.find(".loading-indicator").hide();
			
			// enable inputs
			this.$el.find(".create-archive").removeAttr("disabled");
			this.$el.find("#newArchiveName").removeAttr("disabled");
			this.$el.find("input[name='newArchiveTemplate']").removeAttr("disabled");
		}
	},
	dropboxOver: function(event) {
		// disables default drag'n'drop behavior
		event.stopPropagation();
		event.preventDefault();
		
		// shows nice copy icon
		event.originalEvent.dataTransfer.dropEffect = "copy";
	},
	dropboxDrop: function(event) {
		// disables default drag'n'drop behavior
		event.stopPropagation();
		event.preventDefault();
				
		// get files transmitted with drop
		var files = event.originalEvent.dataTransfer.files;
		this.stashFile(files);
		
	},
	dropboxClick: function(event) {
		// disables default click behavior
		event.stopPropagation();
		event.preventDefault();
		
		var $button = this.$el.find(".dropbox input[name='newArchiveExisting']");
		$button.trigger("click");
		return false;
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
		this.stashFile(files);
		
		// resets only this input
		$(event.target).wrap("<form>").parent("form").trigger("reset");
		$(event).unwrap();
	},
	stashFile: function(file) {
		
		if( file == null || file == undefined ) {
			this.$el.find(".dropbox .file-name-display").html("").hide();
			this.file = null;
			return false;
		}
		
		// just take the first file
		file = file[0];
		// put it on stage
		this.file = file;
		this.$el.find(".dropbox .file-name-display").html(file.name).show();
		
	}
	
});

var StartView = Backbone.View.extend({
	
	el: "#start-page",
	collection: null,
	
	initialize: function() {
		this.template = templateCache["template-start"];
		this.collection = new WorkspaceHistoryCollection();
		this.fetch();
	},
	render: function() {
		var current = this.collection.find(function( element ) {
			return element.get("current") == true;
		});
		
		var json = { "history": this.collection.toJSON(), "current": current.toJSON(), "baseUrl": location.protocol+"//"+location.host+location.pathname+(location.search?location.search:"") };
		this.$el.html( this.template(json) );
	},
	fetch: function() {
		
		if( this.collection == null )
			return;
		
		var self = this;
		this.collection.fetch({
			reset: true,
			success: function(collection, response, options) {
				self.render();
			},
			error: function(collection, response, options) {
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Cannot fetch workspace history", text );
				}
				else
					messageView.error( "Unknown Error", "Cannot fetch workspace history." );
			}
		});
		
	},
	
	events: {
		"click .start-history-rename": "renameHistoryEntry"
	},
	renameHistoryEntry: function(event) {
		
		var workspace = this.collection.get( $(event.target).attr("data-workspace-id") );
		if( workspace == null )
			return false;
		
		var newName = window.prompt( workspace.get("workspaceId"), workspace.get("name") ); 
		if( newName == null || newName == "" || newName == workspace.get("name") )
			return false;
		
		workspace.set("name", newName);
		
		messageView.removeMessages("workspace-history");
		var self = this;
		workspace.save({}, {
			success: function(model, response, options) {
				// everything ok
				self.render();
				messageView.success( undefined, "Workspace successfully renamed", "workspace-history" );
			},
			error: function(model, response, options) {
				console.log("error while renaming workspace");
				if( response.responseJSON !== undefined && response.responseJSON.status == "error" ) {
					var text = response.responseJSON.errors;
					messageView.error( "Cannot rename workspace", text, "workspace-history" );
				}
				else
					messageView.error( "Unknown Error", "Cannot rename workspace", "workspace-history" );
			}
		});
		
		return false;
	}
});

var AboutView = Backbone.View.extend({
	
	el: "#about-page",
	model: null,
	
	initialize: function() {
		this.template = templateCache["template-about"];
		this.render();
	},
	render: function() {
		this.$el.html( this.template({}) );
	},
	events: {}
	
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
	showMessage: function( type, title, text, referring ) {
		if( text == undefined && referring == undefined ) {
			text = title;
			title = undefined;
		}
		
		console.log(referring);
		
		if( referring == undefined || referring == null )
			referring == "";
		
		var json = { "message": {
			"title": title,
			"text": text,
			"referring": referring 
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
	removeMessages: function( referring ) {
		
		if( referring == undefined || referring == null || referring == "" )
			return false;
		
		this.$el.find(".message[data-referring='" + referring + "']").animate({opacity: 0.15, height: 0}, "500", function() {
			$(this).remove();
		});
		
	},
	error: function( title, text, referring ) {
		return this.showMessage("error", title, text, referring);
	},
	warning: function( title, text, referring ) {
		return this.showMessage("warning", title, text, referring);
	},
	success: function( title, text, referring ) {
		return this.showMessage("success", title, text, referring);
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

