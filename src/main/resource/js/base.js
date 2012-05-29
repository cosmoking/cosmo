function processButtonMeta (n)
{
	var buttonMetaDefault = {
		//icons: {primary: 'ui-icon-play'}
	}	
	
	var elements = n ?  $(n).find('[_buttonMeta]') : $('[_buttonMeta]'); 	
	 elements.each(
		function() {
			var buttonMeta ='{"' +  $(this).attr('_buttonMeta').replace(/:/g,'":"').replace(/;/g,'","').replace(/{/g,'{"').replace(/}/g,'"}') + '"}';
			buttonMeta = buttonMeta.replace(/"}"}/g,'"}}');
			buttonMeta = buttonMeta.replace(/:"{"/g,':{"');
			buttonMeta = buttonMeta.replace(/"true"/g,'true').replace(/"false"/g,'false');			
			buttonMeta = buttonMeta == '{""}' ? {} : $.parseJSON(buttonMeta);
			var meta = copyArray(buttonMetaDefault, buttonMeta);
				
			$(this).button(meta);				
		}
	)	
}


function processMenuMeta (elements)
{
	$("[_menuMeta]").each(
		function () {
			var menuMeta ='{"' +  $(this).attr('_menuMeta').replace(/:/g,'":"').replace(/;/g,'","') + '"}';
			menuMeta = menuMeta == '{""}' ? {} : $.parseJSON(menuMeta);			
			
			// add default menu class
			$(this).addClass('menu');
			
			// this places the menu to the top right at 0 0 : This fixed the problem where jquery ui position() has issue 
			// with the first placement in safari, chrom, and IE - in which it add this first relative position to it's
			//  FIRST placement // ie , if it was at 10, 15 initially it will be off that much
			$(this).css('top', 0);
			$(this).css('left', 0);			
			
			
			// get each menuItems - basically this means the buttons
			var list = $(this).find(":button, :submit, :reset, :checkbox, :radio, a, .ui-button" );
			
			// set width from the menuMeta - this basically means apply to the buttons
			list.each(function () {
				if (menuMeta.minWidth) {
					$(this).css('min-width', menuMeta.minWidth);
				}
			});
			shapeMenus(list);			
		}
	);
}

function processDialogMeta (n)
{
	var dialogMetaDefault = {
		//title: 'Account Login',
		autoOpen: false,
		closeOnEscape: true,
		dialogClass: 'dialog',
		draggable: false,
		//height: 'auto',
		//width: 'auto',
		hide: 'drop',
		show: 'drop',
		//maxHeight: false,
		//maxWidth: false,
		//minHeight: 250,
		//minWidth: 250,
		//modal: true,   //XXX jquery ui bug http://dev.jqueryui.com/ticket/4758, http://dev.jqueryui.com/ticket/3623
		position: 'center',
		resizable: false,
		//zIndex: 1000,
		stack: true,
		open: function (event) {
				PostActionCallback.Enable = false;				
			},
		close: function () {
				PostActionCallback.Enable = true;
	
			}
				
	} 
	
	var elements = n ?  $(n).find('[_dialogMeta]') : $('[_dialogMeta]'); 	
	 elements.each(
		function() {
			var dialogMeta ='{"' +  $(this).attr('_dialogMeta').replace(/:/g,'":"').replace(/;/g,'","') + '"}';
			dialogMeta = dialogMeta.replace(/"true"/g,'true').replace(/"false"/g,'false');
			dialogMeta = dialogMeta == '{""}' ? {} : $.parseJSON(dialogMeta);
			copyArray(dialogMetaDefault, dialogMeta);	
			//alert(objJsonString(dialogMeta));
			$(this).dialog(dialogMeta);

				// set tabindex
			$(this).find("a button input").each (
				function () {
					$(this).attr('tabindex','0');
				}
			);
			
				// xxx $('[_primaryAction]', this)  or $(this).find('[_primaryAction]')  
				// don't work unless attribute is assigned with value ie attr=baf. which is ugly
				// since we only want to use "attr" simply as marker
				// only way to work around is reconstruct the select string as below.  
			var primaryAction = $('#' + $(this).attr('id') + ' [_primaryAction]');
			
			$(this).bind("keypress", 
				function (e) {
				   if (e.keyCode == '13') {
				   		e.preventDefault();
				   	   $(primaryAction).trigger('click');
				   }
				}
			);			
		}
	)
}




	// This action is used during pageReload and ajax load  to parse all the meta info
	// XXX! remember to exclude ContentPanel	
function processEventMeta (n)
{
	
	var elements = n ?  $(n).find('[_eventMeta]') : $('[_eventMeta]'); 	
	 elements.each(
		function() {
				// read attribute "_eventMeta" and convert to JSON var 
				// ie '_click:dateClicked;arg:blah' to  '{"_click":"dateClicked","arg":"blah"}'
			var eventMeta ='{"' +  $(this).attr('_eventMeta').replace(/:/g,'":"').replace(/;/g,'","').replace(/{/g,'{"').replace(/}/g,'"}') + '"}';
			eventMeta = eventMeta.replace(/"}"}/g,'"}}');
			eventMeta = eventMeta.replace(/:"{"/g,':{"');
			eventMeta = eventMeta.replace(/"true"/g,'true').replace(/"false"/g,'false');			
			eventMeta = eventMeta == '{""}' ? {} : $.parseJSON(eventMeta);


			
				// remove any "href" and replace with void  
			//$(this).attr('href','javascript:void(0)');
				
				// for each event _XXX we bind to the function ie "_click" is bind to function dateClicked();
				// XXX refactor this with menuOpen!	 				
			for (key in eventMeta) {
				if (key.charAt(0) == '_') {
					var functionName = eventMeta[key];
					if (functionName.charAt(functionName.length - 1) == '?') {
						functionName = functionName.substring(0, functionName.length - 1);
						eventMeta[key] = functionName;
						eventMeta.requireLogin = true;
					}								
					
						// will attempt to find the matching client side function if none just default to defaultEventHandler
					var theFunction = eval('typeof ' + functionName) == 'function'
						? eval(functionName)
						: defaultEventHandler;
					
					$(this).bind(key.substring(1, key.length), eventMeta, theFunction);					
				}
			}
		}
	)	
}

function defaultEventHandler (e)
{
	ajaxRefresh(e);
}	




/*
	a calledBack from ajax refresh on menu. The callbackData is an array of 
	state value which is then applied to each menuItem (ie buttons) in the order 
	it listed in the html.
	
	possible values are 'hide', 'show', 'disable', 'enable' which is then feed
	to shapeMenus
	 
	XXX: there is a bug in which a disabled button can still receive click event
	probably should assign click event to button when eventMeta is parsed
 	
*/
function reDisplayMenu (callbackData, n, menuId)
{
	var buttons = filterAttr('_buttonMeta', $(menuId).find('a'));
 	buttons.each(
 		function (i) {
 			var widget = $(this).button("widget")[0];
 			if (callbackData[i] == 'hide') {
 				$(widget).addClass('hide');
 			}
 			if (callbackData[i] == 'show') {
 				$(widget).removeClass('hide');
 			}
 			if (callbackData[i] == 'disable') {
 				$(widget).addClass('disable');
 			}
 			if (callbackData[i] == 'enable') {
 				$(widget).removeClass('disable');
 			} 			
 		}
 	);
	shapeMenus(buttons);	
}



/*
	Menu Items (ie anchor with _buttonMeta attribute) can either have 2 state declared 
	in the class (ie class="blah blah hide" or class="blah blah disable"):
	"hide" means the menu item will be hidden
	"disable" means the menu item will be shown but disabled
*/

function shapeMenus (list) {
	
	var filteredList =
			list.filter (function () {
					// re enforce display none since jquery widget tend to override
					// this have the effect of showing and hiding menu items
				if ($(this).hasClass('hide')) {
					$(this).css("display", 'none');
				}
				else {
					$(this).css("display", 'block');
				}
								
				if ($(this).hasClass('disable') && !$(this).hasClass('ui-state-disabled')) {
					$(this).button('disable');
				}	
				
				if (!$(this).hasClass('disable') && $(this).hasClass('ui-state-disabled')) {
					$(this).button('enable');
				}
						
					// return ones that are shown so that it will get properly corned from below call
				return $(this).css("display") != 'none';
			})			
			.map(function() {
				var w =  $(this).button("widget")[0];
				return w;
			});
			
	filteredList.removeClass( "ui-corner-all" ).filter( ":first" ).addClass( "ui-corner-top" );
	filteredList.filter( ":last" ).addClass( "ui-corner-bottom" );
}


function menuClicked (event)
{
	var n = this;
	
	
	var menuHideTimer = null;
	var menuOnFocus = $(event.currentTarget).attr("_menuOnFocus");	
	var tagMenu = $(event.data.menuMeta);	

		// if menu is currently on focus which is shown, then we toggle and hide it
	if (menuOnFocus) {
		$(event.currentTarget).removeAttr("_menuOnFocus");
		tagMenu.css('display', 'none');		
	}
		// else display menu
	else {
		$(event.currentTarget).attr("_menuOnFocus", 'true');
		
			// we are copying all "menu" eventData into each "menuItem" eventData
			// first iterate each event tags, and unbind any previous click event
			// reparse the eventMeta, copy event's eventMeta data
		tagMenu.find(":button, :submit, :reset, :checkbox, :radio, a, .ui-button").each (
			function () {
				var eventMeta = $(this).attr('_eventMeta');
				if (eventMeta) {
					copyArray(event.data, $(this).data('events').click[0].data);
				}						
			}
		);
			

			// if mouse is on menu dropdown, don't hide
		$(tagMenu).bind(
			"click",
			function(){
				tagMenu.css('display', 'none');
				$(event.currentTarget).removeAttr("_menuOnFocus");
			}	
		);


		
			// if mouse is on menu dropdown, don't hide
		$(tagMenu).bind(
			"mouseenter",
			function(){
				if (menuHideTimer) {
					clearTimeout(menuHideTimer);
				}
			}	
		);

			// if mouse is not on menu dropdown, hide after 100ms		
		$(tagMenu).bind(
			"mouseleave",
			function(){
				menuStartHiding = true;
				menuHideTimer = setTimeout(
					function() {
						tagMenu.css('display', 'none');
				 	 	$(event.currentTarget).removeAttr("_menuOnFocus");
					},
					200
				);
			} 
		);

			// if mouse is not on menu, hide after 100ms
		$(n).bind(
			"mouseleave",
			function(){
				menuStartHiding = true;
				menuHideTimer = setTimeout(
					function() { 
							// only hide if flag hasn't change						
						tagMenu.css('display', 'none');
				 		$(event.currentTarget).removeAttr("_menuOnFocus"); 
				 	}, 
				 	200
				);
			} 
		);
		
		tagMenu.css('display', 'block');
		tagMenu.css('position', 'fixed');
		$(tagMenu).position({ my: "left middle", at: "right middle", of: n, offset: "2 0", collision: "fit fit"});
		
			// finally we display menu		
		//var position = $(n).offset();
		//
		//tagMenu.css('top', position.top);
		//tagMenu.css('left', position.left + 20);
		

		
	}

	event.stopImmediatePropagation();	
}






function ajaxRefresh (e, arg, ctx, serverHandler, callerArgurments)
{
	
	var url = "http://" + hostName + "/5/" + sessionId + "/" + uniqueClientToken;

		// auto set PostFormSubmitAction if requireLogin is specificed. PostFormSumbitAction is 
		//  basically the call to "_click:formSubmitClicked" in a dialog. So in this case
		// if requires login the server side will check to see if login is required (ie. session.promptLogin()
		// if it does it will return the ajax response with js to bring up the login dialog. Once click
		// OK and it's valid. This then triggers the formSubmitClicked which then calls the prior function
		// that was setup by setupPostFormSubmitAction. It this case the function is (callerArgurments) which really is the initial function call.  
		// That call will then decide what it wants to do.
		// 
		// for example. switchPersonalFolder is clicked, it sess that is requiresLogin so it then call setupPostFormSubmitAction to setup a callback
		// to itself "switchPersonalFolder". server gets called and returns Login Screen. User login clicks ok and submit, it verfies it's
		// fine then calls "swtichPersonalFolder" again since it was setup prior. This time it fires again to the same server side call. 
		// THis time since it's login it will return correct ajax respoinse to switch folder
		// 
		// now say if user is logged in. This server gets call and sees that it is logged so it will just return the correct ajax response
		// with switch folder. In this case the entire User login click Ok etc action is bypassed.	
	if (e && e.data.requireLogin) {
		setupPostFormSubmitAction(callerArgurments, e);
	}
	
	var action = serverHandler != undefined 
					? serverHandler
					: e.data.serverHandler != undefined
						? e.data.serverHandler 
						: e.data["_" + e.type]; 
    
    arg = arg != undefined 
	            	? e && e.data.arg != undefined 
	            		? arg + "/" + e.data.arg
	            		: arg
	            	: e.data.arg; 			
	            	
	 
	RefreshManager.setLastRefreshableAction(action + "/" + arg);

	
	var loadingCursor = $('#loading-cursor'); // can cache this
	
	 	// additional display - don't really need it		
	setTimeout(function () {		
		if (loadingCursor.css('display') == 'block') {		
			loadingCursor.css('background-position', 'center center');
		}
	}, 500);
	
		// display loading cursor
	loadingCursor.css('background-position', '-100px -100px');
	loadingCursor.css('display','block');
	
		// workaround so that the above loading cursor can show. 
		// if call ajax directly the display won't show  
	setTimeout (function () {
		ajaxRefreshInternal (action, url, arg, ctx, e);
	}, 10);

	  		
}

function ajaxRefreshInternal (action, url, arg, ctx, e)
{
	$.ajax({
		  async: true,
		  type: 'GET',
		  url: url,
		  contentType: 'charset=UTF8',
		  data: action + (arg == undefined ? "" : "/" + arg),
		  context: e,
		  dataFilter: function (data, type) {
				var dataArray = new Array();
				var markerSize = 10;
				var startpos = 0;
				while (startpos < data.length) {
					var boundaryMarker = data.substring(startpos, startpos + markerSize); 
					var nextBoundary = data.indexOf(boundaryMarker, startpos + markerSize);
					dataArray.push(data.substring(startpos + markerSize, nextBoundary));
					startpos = nextBoundary < 0 ? data.length : nextBoundary + markerSize; 
				}
				return dataArray;
				
				},
		  success : function (dataArray, textStatus, XMLHttpRequest) {
				for (var i = 0, size = dataArray.length; i < size; i = i + 2) {	
					var data = dataArray.shift();					
					var script = dataArray.shift();
					if (script) {
						try {
							eval(script);
						}
						catch (err) {
							alert(err);
						}
					}
		  		}
		  		
		  			// This callBack is strictly used to allow sequence of calls to go in order when ajax call
		  			// is made. When some method makes call to ajaxRefresh() to fetch data follows by a method a()
		  			// to process that data. It' highly the case the call a() will be invoked before the ajaxRefresh()
		  			// gets the data back from server. hence in this case assign a() to _clientAjaxCallback and invoke
		  			// ajaxRefresh() as the last call in the method. AND when data is retreived and the "success" is called
		  			// whe ajaxRefresh is done then the follow up method a() will then be invoked. hence the correct order
		  			
		  			// note, if this._clientAjaxCallback function is calling ajaxRefresh(e) inside then it NEEDS to 
		  			// set "e._clientAjaxCallback = null" before call ajaxRefresh(e) otherwise the call back will be called twice
		  			// i.e.   
		  			//
		  			//	e.clientCallback = function () {
		  			//  	..
		  			//		e._clientAjaxCalback = null;
		  			//		ajaxRefresh(e);
		  			//	}
		  		if (this._clientAjaxCallback) {
		  			this._clientAjaxCallback();
		  				// this is so that within same method they may be more than one
		  				// ajaxrefresh and we don't want accidentlly call twice.		  				
		  			this._clientAjaxCallback = null;
		  			
		  		}		  		
				$('#loading-cursor').fadeOut(200);		  		
		  }
		  
		}
	);				
}






function showFormError (form, entrySelector, message) {
		
	var entry = $(entrySelector);
	var entryLabel = ""; 
	
		// if it's input we are updating the label text
	if (entry[0].localName == 'input') {

			// store current label to the attr so later when clearFormError we can restore it
		entryLabel = form.find("label[for='" +  entry.attr("id") + "']");		
	}
		// else update the text pointed by the entrySelector (ie p.tips for general error)
	else {
		entryLabel = entry;		
	}


		// store current message for later restore	
	var entryLabelText = $(entryLabel).html();
	$(entryLabel).attr('_entryLabelText', entryLabelText ? entryLabelText : ' ');
		// set error message and style	
	entryLabel.addClass('error-text');
	$(entryLabel).html(message);		
	entryLabel.fadeOut(1000).fadeIn(1000);	
}

function clearFormError (form)
{
	form.find('*').each(function () {
		var entrylabelText = $(this).attr('_entryLabelText');
		if (entrylabelText) {
			$(this).html(entrylabelText);
		}	
	})	
	form.find('*').removeClass('error-text');
}

function formSubmitClicked (e)
{
	
	var form = $(e.data.dialogMeta);	
	clearFormError(form);
		// formSubmited imples no more open forms so setting to false, the implication of this is so that	
		// post action only gets invoked when there are no more open forms.	
	PostActionCallback.Enable = true;   
	
	
		// ...?loginClicked/{"loginDialog_email":"jackwang65@yahoo.com","loginDialog_password":"we"}
		// NOTE!! the values are escaped twice!!
	if (e.data.jsonFormEncode) {
		var inputValues = {};
		form.find('input').each(function (i) {
			var type = $(this).attr('type');
			if (type.toLowerCase() == 'checkbox') {
				inputValues[$(this).attr('id')] = $(this).attr('value'); 
			}
			else {	
				inputValues[$(this).attr('id')] = escape($(this).val()); // escape here and below to work for UTF8!!
			} 		
		});	
		form.find('textarea').each(function (i) {
			var disabled = $(this).attr('disabled');
			var readonly = $(this).attr('readonly');
			if (!(disabled || readonly)) {
				inputValues[$(this).attr('id')] = escape($(this).val()); 
			}
		});	

 
		inputValues = escape(objJsonString(inputValues));
	}
		// ...?loginClicked/jackwang65@yahoo.com/we	
	else {
		var inputValues = "";
		form.find('input').each(function (i) {
			inputValues += i > 0 ? "/" + escape($(this).val()) : escape($(this).val()); 		
		});	
		inputValues = escape(inputValues);
	}

	
	e._clientAjaxCallback = function () {
		var validationResult = $.parseJSON(form.attr('_ajaxData'));
		for (property in validationResult) {
			if (validationResult[property]) {
				showFormError(form, property, validationResult[property]);
			}
	  	}
	  	 	
	  	if (isEmpty(validationResult)) {
			formCancelClicked(e, form);
				
				// call only when No outstanding form is open and that no callback is in progress
			if (PostActionCallback.Enable && 
			 	!PostActionCallback.callbackInProgress &&
			 	PostActionCallback && 
			 	PostActionCallback.callback ) {
				
				PostActionCallback.callbackInProgress = true;
				PostActionCallback.callback(PostActionCallback.arg, e, inputValues, validationResult);
				PostActionCallback.callbackInProgress = false;
			}
	  	}				
	}
	
		// it's possible this formSubmitClicked is subject to "?" param which inicates require login hence
		// we are passing args to prep for that. when that happens , the result of this call would 
		// trigger event on "openLoginDialog" which opens up the login dialog. At same time the remaining logic 
		// would still get run and exits itself with empty validation and "PostActionCallback.Enable" being true. the user
		// will be present with login screen at that point. when it does click ok, it will come back to this same
		// place, at that time, if validation passess and PostActionCallback.Enable is false, the call back will kick in.
		// which does the actual action. On a different scenario if user has already logged in the validation result
		// will come back and goes through the validation.   
		
	ajaxRefresh(e, inputValues, null, null, "formSubmitClicked");
 

}


function formCancelClicked (e, explicitForm) 
{
		// explicitForm fixes the problem when e.data gets wiped for artifical trigger() event
		// in that case, we pass it explicitForm
	var form = explicitForm == undefined ? $(e.data.dialogMeta) : explicitForm;
	clearFormError(form);				

		// clear the focus and active state of the primaryAction for next invocation if any 
	$('#' + $(form).attr('id') + ' [_primaryAction]').removeClass('ui-state-active');	// see above _primaryAction for this selector
	form.dialog('close');
}


function formOpenClicked (e)
{
		// if there is an open dialog already opened specified 
		// then we close it first before open the actual one
	if (e.data.closeDialogMeta) {
		var form = $(e.data.closeDialogMeta);	
		clearFormError(form);
		$(form).dialog('close');		
	}

		// refactor this with menuclicked	 
		// push current event data to the dialog's buttons
	var dialog = $(e.data.dialogMeta);	
	dialog.find(":button, :submit, :checkbox, :radio, a, .ui-button").each (
		function () {
			var eventMeta = $(this).attr('_eventMeta');
			if (eventMeta) {
				copyArray(e.data, $(this).data('events').click[0].data);
			}						
		}
	);
	
		// clearn inputs before form is opened.
		// however, if skipClearInputs means we don't clear the fields, this is in case
		// where inputs are pre- populated before it's opened
	if (e.data.skipClearInputs == undefined || !e.data.skipClearInputs) {
		var form = $(e.data.dialogMeta);		
		form.find('input').val('');
	}		
	
	
	dialog.dialog('open');

		// due to animation need to set an delay then set the focus		
	setTimeout(	function() { 
		$('#' + $(dialog).attr('id') + ' [_primaryFocus]').focus();	// see above _primaryAction for this selector
	}, 500); 	
}



function setupPostFormSubmitAction(aFunction, arg)
{
     //var functionName = arguments.callee.toString().substr('function '.length);        
     //functionName =  functionName.substr(0, functionName.indexOf('('));   
     
     	// setup a post action, and only setup when this is not a returning call from previous postAction
     	// the callBackInProgress is set true when it was already invoked.
     if (PostActionCallback && !PostActionCallback.callbackInProgress) {
		 PostActionCallback.callback = typeof aFunction == 'string' ? eval(aFunction) : aFunction;
		 
		 	// if it's string - copy reference else - shallow copy the object
		 PostActionCallback.arg = typeof arg == 'string' ? arg : $.extend({}, arg);
     }
}



function stopBubbling (event)
{	
	var e = event == undefined ? window.event : event;
	e.cancelBubble = true;
	if (e.stopPropagation) {
		 e.stopPropagation();
	}
	return false;
}


function fakeEvent ()
{
	//var e = $.Event();
	var e = {}; e.data = {};
	return e;
	
}




function zoom (e, event, requireFloat)
{
  

  $('#foo').remove();

  var elm;
  if (e.currentTarget != undefined) {
  	elm = e.currentTarget;
  }  
  else {
  	elm = e;
  }	
	
  

  if (requireFloat) {
  	 $(elm).css('float', 'left');
  }
	
  var offset = $(elm).offset();
  var currentWidth = $(elm).outerWidth(true); 	
  var currentHeight = $(elm).outerHeight(true);
  

  if (requireFloat) {
  	 $(elm).css('float', '');
  }
  
  

  
  
  
  var div = $('<div/>')  
        .attr('id', 'foo')  
        .addClass('roundCorner')
        .css({
        	opacity: 0.4,
        	position: 'absolute',  
            height: currentHeight,  
            width: currentWidth,
            top: offset.top,
            left: offset.left,
            border: '1px solid black', 
            backgroundColor: 'white',
            zIndex: 1000
        });  
  
  $('BODY').append(div); 
  
  var finalWidth = 450;
  var finalHeight = 350;	
	
  var xOffset = currentWidth < finalWidth ? "-" + ((finalWidth - currentWidth) / 2) : ((currentWidth - finalWidth) / 2);
  var yOffset = currentHeight < finalHeight ? "-" + ((finalHeight - currentHeight) / 2) : ((currentHeight - finalHeight) / 2);
	
 //$(this).css('position','absolute');	
 //$(this).css('top', offset.top);
 //$(this).css('left', offset.left);
 //$(this).css('z-index','2000');
 
  var speed = 1.0;
  //$('#zoomOverlay').css('display','block');
  
	$(div).position(
		{my: "center center",
		 at: "center center",
		 of: $('#zoomOverlay'),
		 offset: xOffset + " " + yOffset,
		 collision: "none none",
		 using: function(to){
			$(div).animate(
				{top : to.top,
				left : to.left,
				width: finalWidth,
				height: finalHeight,
				opacity: 0.8},
				250 * speed,
				'linear',
				function(){
					$(this).animate(
						{opacity: 1.0},
						100 * speed,
						'linear',
						function(){
							$(this).addClass('dropShadow');
	  	  				}
	  	  			);
	  	  		}
	  	  	);
	 	 }}
	 );
 
   stopBubbling(event);
 
}



