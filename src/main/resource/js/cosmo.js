 /* assign var locally */
 FI.uniqueClientToken = FI.ut;
 FI.sessionId = FI.si;
 FI.hostName = FI.hn;
 FI.domainName = FI.dn;
 FI.bindingMarker = FI.bm;
 FI.webResourceFileIds = FI.wi;    
 FI.browserInfo = FI.bi;	
 FI.debugMode = FI.dm;
 FI.http = window.location.protocol == "https:" ? "https://" : "http://";
 FI.ws = window.location.protocol == "https:" ? "wss://" : "ws://";

(function() {


  	if (typeof console === "undefined" || typeof console.log === "undefined") {
     	console = {
     		log : function () {}
     	}
    }

    $.extend($.fn.disableTextSelect = function() {
        return this.each(function(){
            if(FI.browserInfo.firefox){//Firefox
                $(this).css('MozUserSelect','none');
            }else if(FI.browserInfo.msie){//IE
                $(this).bind('selectstart',function(){return false;});
            }else{//Opera, etc.
                $(this).mousedown(function(){return false;});
            }
        });
    });
    
	$.fn.hasHBar = function() {
	    var _elm = $(this)[0];
	    return _elm.clientWidth < _elm.scrollWidth;
	}
	
	$.fn.hasVBar = function() {
	    var _elm = $(this)[0];
 	    return _elm.clientHeight < _elm.scrollHeight;
	}
	
	$.fn.scaleTextToFit = function(resetFontSize) {
			// requires overflow-x to set auto
		var sizeLimit = 18;
		
		if (resetFontSize) {
			this.css('font-size','');			
			this.css('letter-spacing','');
			this.css('text-overflow','');
			this.css('-ms-text-overflow','');	
			this.css('overflow-x','');			
			//this.css('font-family','');
			
		}
		
		while (this.hasHBar()) {

				// if size too small - we just overflow it
			var fontsize = parseInt(this.css('font-size'));
			if (fontsize <= sizeLimit) {
				this.css('overflow-x','hidden');
				this.css('text-overflow','ellipsis');
				this.css('-ms-text-overflow','ellipsis');				
				return false;
			}
			
				
				// other first try reduce the letter-spacing, if already reduced
				// then reduce the fontsize , ideally we can try change different font too
				// ie arial < verdana ,   letter-spacing, then font-family, then font-size 
			var spacing = parseInt(this.css('letter-spacing'));
			if (spacing && spacing == -1) {
				this.css('font-size',fontsize - 1 + "px");
			}
			else {
				this.css('letter-spacing', '-1px');
			}
			
		}
	}	

	 
	$().ready(function() {

		 base.processEventMeta();
		 base.processMenuMeta();

		highlight.setAndRecord({target:$('#ShortcutPortlet .portletContent ._categoryEntry:first')});
		
		var currentPortal = $('#appuiPortalDivId h6:first');
		highlight.setAndRecord({target:currentPortal});
		
		$('#appuiCategoryPortletDivId .portletHeader h6').text(currentPortal.html() + " Categories");
		action.setHeaderOnDigest(currentPortal.html());

		 window.onbeforeunload = function(event) {
			if (iframe.checkPageBust) {					 
				return "** Sorry, apparently this content wants to leave our site **"  
			}				 
		 }

			/* do nothing for now - search hashChange . IE8 store history */
		$(window).bind( 'hashchange', function(e) { 
			e.preventDefault();
		});
		
		
		QueryAhead.init(); 
		executionTracker.init();

		//wheelControl.init(); -- use mousewheel plugin
		$(".portletContent").mousewheel(function(event, delta, deltaX, deltaY) {
			if (delta > 0) {
				$(this).scrollTo( '-=24px', { axis:'y' } );
			}
			else if (delta < 0) {
				$(this).scrollTo( '+=24px', { axis:'y' } );
			}
			return false;
		});
		 
		 
		function setWebResourceFilesToken () {
			for (var i = 0; i < FI.webResourceFileIds.length; i++) {
				var webResourceFileId = FI.webResourceFileIds[i];
				console.log(FI.webResourceFileIds[i]);

				var webResourceFileNode = $('#' + webResourceFileId); 
				if (webResourceFileNode.length !== 0 && webResourceFileNode.attr('token')) {
					console.log(webResourceFileId);
					localStorage.setItem(webResourceFileId + '.token',webResourceFileNode.attr('token'));
					localStorage.setItem(webResourceFileId,webResourceFileNode.html());
				}
			}
		} 
		setTimeout(setWebResourceFilesToken, 1);
	 

	 	//console.log(BrowserDetect.browser + ":" + BrowserDetect.versionNumber);
	    //$('#DetailCenter').disableTextSelect();//No text selection on elements with a class of 'noSelect'
	});
	
	
	
	/* embed style sample - actualy jquery may be faster
	var head = document.getElementsByTagName('head')[0],
	    style = document.createElement('style'),
	    rules = document.createTextNode('.embedStyle2 { background: blue;} .embedStyle3 { background: blue;}');
	
	style.type = 'text/css';
	if(style.styleSheet)
	    style.styleSheet.cssText = rules.nodeValue;
	else style.appendChild(rules);
	head.appendChild(style);
	*/	
	
 })();


wheelControl = {
	
	enable: true,	
	
	init: function () {

		var wheel = function (e) {
			
			
			//console.log("wheel");
			if (e.stopPropagation) {
				 e.stopPropagation();
			}
			return false;

		};
		
		if (window.addEventListener) {
			window.addEventListener('DOMMouseScroll', wheel, false);
		}
		window.onmousewheel = document.onmousewheel = wheel;		
	},
	
	enable: function () {
		wheelControl.enable = true;
	},
	
	disable: function () {
		wheelControl.enable = false;
	}
}
	
/*
 *  executionTracker tracks: 1) if exection takes beyond threshold then wait cursor shows, 2) if there is pending executino running  3) if need throttle due to user rapid clicking 
 * 
 *  For 1)
 *  Basiaclly each "click or event" can be composed of mulitple "execution context", ie - click -> ajax -> do something -> then another ajax -> end 
 *  so at beginning of each "execution context" - insert "trackPoint" and remove at the end when "execution context" completes. 
 *  This is because each "execution context" is out of order and asychrnous. The trackPoints allows any "execution context completion"
 *  to see if there are any more pending executions. If trackPoint is zero, then we know the execution is done.
 * 
 *  Now, with this, we can use "timer" to dertmine if we need to show "loader" if the exeuction has taken longer than a specific window
 * 
 *  Currently placed in - clickWrappedFunction, ajaxInternal, scrollNext ( basically any asynchrone execution like, ajax, setTimeout)
 * 
 *  For 2)
 *  Calling isRunning() check is there is an execution in progress.  The very first tracker is placed inside "clickWrappedFunction" - 
 *  this has to be the case so it tracks "all click" events. Inside, it also test if there is an execution in place so that it voids
 *  any clicks if it's the case
 * 
 *  
 *  For 3)
 *  Between execution the "time" is tracked to determine if rapid click has occured as the "time" is less than a specific amount (ie, 500ms)
 *  If it is - then show the "loader" immediates and set "throttle" to true. When this is true, the "clickWrappedFunction" would use timer to
 *  delay the execution of the "work"
 * 
 */	
executionTracker = {
	timeToShowLoader : 2000,
	element : null, 
	executionTimestamp: new Date().getTime(),
	shouldThrottle: false,
	actionMarkers: 0,
	timer: null,
	safety: null,
	init : function() {
		$('body').append("<div id='executionTracker' class='corner1' style='display:none'><span class='executionRunning cssImg spin'></span><h1 class='bold' style='font-size:15px;text-align:center;color:white'>Loading</h1></div>");
		executionTracker.element = $("#executionTracker");
	},

	addTrackPoint : function (src) {
		//console.log("show " + src + " " + executionTracker.actionMarkers);
		
			// increment tracker
		executionTracker.actionMarkers++;
		if (executionTracker.actionMarkers == 1) {
				// check if throttle is needed
			executionTracker.checkThrottle();
			if (executionTracker.shouldThrottle) {
				$(executionTracker.element).stop(true,true);
				$(executionTracker.element).css('display','block');				
			}
			else {
					// if not, display loader after window if still running
				executionTracker.timer = setTimeout(function () {
					if (executionTracker.isRunning()) {
						$(executionTracker.element).stop(true,true);				
						$(executionTracker.element).css('display','block');
						
							// safety net, if something is seriously wrong, bounce user
						executionTracker.safety = setTimeout(function() {
							if (executionTracker.isRunning()) {
								account.refresh();
							}
						}, 18000);
						
					}				
				}, executionTracker.timeToShowLoader);
			}
		}				
	},
	
	removeTrackPoint : function (src) {
			//decrement tracker
		executionTracker.actionMarkers--;
		//console.log("hide " + src + " " + executionTracker.actionMarkers);
		
			// if execution is completed
		if (executionTracker.actionMarkers == 0) {
				// clear timer and loader
			clearTimeout(executionTracker.safety);
			clearTimeout(executionTracker.timer);
	     	$(executionTracker.element).fadeOut(FI.browserInfo.msie ? 100 : 750);
		}
	},
	
		// check if in between clicks is less that 500ms - and set throttle flag
	checkThrottle : function () {
		var currentTime = new Date().getTime();
		executionTracker.shouldThrottle = (currentTime - executionTracker.executionTimestamp) < 300;
		executionTracker.executionTimestamp = currentTime;
		
		
			// make "loader image" to appear differently
		if (executionTracker.shouldThrottle) {
			$(executionTracker.element).addClass('throttle');
		}
		else {
			$(executionTracker.element).removeClass('throttle');
		}		
	},
	
		// used to see if there is an exection in progress
	isRunning : function () {
		return executionTracker.actionMarkers > 0;
	}
	
}
	

highlight = {
	group : {},
	
	setAndRecord: function (e) {
		
			// get highlight meta
		var highLightMeta = $(e.target).data('highlight'); 
			// if defined
		if (highLightMeta && highLightMeta.group) {
			var group = highlight.group[highLightMeta.group];
			if (!group) {
					// if not defined - lazy init
				highlight.group[highLightMeta.group] = {'lastElement':e.target,'lastEvent':null};
				group = highlight.group[highLightMeta.group];
			}
			
				// remove highLight from previous element, then add to current, 
				// then store elm and e for next so that it can be reset next time
			$(group.lastElement).removeClass('highLight');
			$(e.target).addClass('highLight');
			group.lastElement = e.target;
			group.lastEvent = e;
		}
	},
	
	resetGroup: function (groupNames) {
		
		for (var i = 0; i < groupNames.length; i++) {
			var group = highlight.group[groupNames[i]];
			if (group) {
				$(group.lastElement).removeClass('highLight');
			}
		}
		
	}
}


content = {
	entryTemplateId : 'appuiContentDivId', // jwl template id for rendering Content
	entryRowTemplateId : 'appuiContentRowDivId', // jwl template id for rendering row
	displayPerSection: 3, //  number of "displayed" entry per row -> has to be the same as Content.displayPerSection()
	noOfSections: 5, // number of "rows" -> has to be the same as Content.noOfSection()
					// above 4 are override by mobile.js as well as MobileContent.java
	modeType : {
		All:'All',
		Category:'Category',
		Source:'Source',
		Time:'Time',
		Liked:'Liked'		
	},
	
	sectionNames:[],
	recordIdsMap: {},
	recordIdsForDigest:{},
	viewCursor: 3,
	mode: null,
	frames: function () {
			//for source no scroll thre for ratio is 1 otherwise 2
		return content.mode === content.modeType.Source ? 1 : 2;
	}
}

account = {
	
	register : function (e) {
		
		$(e.data.form).find('.tips').html('');
		
		var form = $(e.data.form);
		var inputValues = {};

		form.find('input').each(function (i) {
			var type = $(this).attr('type');
			if (type.toLowerCase() == 'checkbox') {
				inputValues[$(this).attr('id')] = $(this).attr('checked'); 
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
		
		base.ajax({'action':'createAccountClicked','arg':inputValues}, e, callback);
		
		
		function callback (dataArray, e, param) {
			
			
			var validationResult = $.parseJSON(dataArray[0]);
			
			for (property in validationResult) {
				if (validationResult[property]) {
					alert("\u25CF\u0020" +validationResult[property]);
					//$(e.data.form).find('.tips').append(validationResult[property]);
				}
		  	}
		  	 	
		  	if (isEmpty(validationResult)) {
				var keepLoginArrayString = dataArray[2]; 
				account.setupKeepLogin(true, keepLoginArrayString);
				window.location.replace(FI.http + FI.hostName);
		  	}
		}			
		
	},
	
	login : function (e) {


		$(e.data.form).find('.tips').html('');
		
		
		var form = $(e.data.form);
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
		
		base.ajax({'action':'loginClicked','arg':inputValues}, e, callback);
		
		
		function callback (dataArray, e, param) {
			
			
			var validationResult = $.parseJSON(dataArray[0]);
			
			for (property in validationResult) {
				if (validationResult[property]) {
					alert("\u25CF\u0020" +validationResult[property]);
					//$(e.data.form).find('.tips').append(validationResult[property]);
				}
		  	}
		  	 	
		  	if (isEmpty(validationResult)) {
				var keepLoginArrayString = dataArray[2]; 
				account.setupKeepLogin(true, keepLoginArrayString);
				window.location.replace(FI.http + FI.hostName);
		  	}
		}		
	},
	
	refresh: function (e)
	{
		window.location.replace(FI.http + FI.hostName);
	},
	
	logout: function (e)
	{
		account.setupKeepLogin(false);
		base.ajax({'action':'logoutClicked'}, e);
		account.refresh(e);
	},
	
	
	setupKeepLogin: function (login, keepLoginArrayString)
	{
		if (login) {
			var keepLoginInfo = $.parseJSON(keepLoginArrayString).join("/");			
			localStorage.setItem("keepLoginInfo", keepLoginInfo);
		}	 
		else {
			localStorage.removeItem("keepLoginInfo");
		}
	}
}


/*
 *  Digest - all "contents" sorted by algorithm
 *  Section - contents for each "section" sorted by algorithm, could be by category, source, time, etc 
 * 
 *  Category - the "folders" or "groups" on the left hand side
 */

action = {
	
	switchPortal: function (e) {
		var arg = $(e.target).data('store').id;
		base.ajax({'action':'switchPortalClicked','arg':arg}, e, callback);
		
		function callback (dataArray, e, param) {
				var data = dataArray.shift();
				var div = $('#appuiCategoryDivId');
				page.applyBindingsToId($.parseJSON(data), div, 'appuiCategoryDivId');
				base.processEventMeta($(div));

				dataArray.shift();
				data = dataArray.shift();
				var bindings = $.parseJSON(data);
				action.applyContent(bindings, content.modeType.All);	
				action.setHeaderOnDigest($(e.target).html()); // set pretty orange header			
				highlight.setAndRecord({target:$('#ShortcutPortlet .portletContent ._categoryEntry:first')}); // set highlight
				$('#appuiCategoryPortletDivId .portletHeader h6').text($(e.target).html() + " Categories"); // set text
				lightScrollBar.refresh("#appuiCategoryPortletDivId .portletContent"); // refresh scrollbar 				
				action.clearSourcePortlet(); // clear source 
						
		};			
	},

	viewDirectContent : function (e, id) { // direct view content
		
		function callback (dataArray, e, param) {
			var data = dataArray.shift();
			var bindings = $.parseJSON(data);
			var records = page.bindingsToRecords(bindings, 'appuiContentLargeDivId');  
			var html = page.recordsToHtml(records, 'appuiContentLargeDivId');
			contentDetail.viewContent({currentTarget:html}, true);
		};			

		if (isNaN(id)) {
			base.ajax({'action':'getContentByName','arg': [id]}, e, callback);
		}
		else {
			base.ajax({'action':'getContentByIds','arg': [id]}, e, callback);
		}	
		return base.stopBubbling(e);
	},

	moreSection : function (e, moreSectionCallback) {
		
		
		var recordIdsSection = [];
		var recordIds = [];

			// viewCursor points to which sections has been rendered - this for loop goes through remaining sections and get the ids 
		for (endCursor = content.viewCursor + content.noOfSections; content.viewCursor < endCursor && content.viewCursor < content.sectionNames.length;  content.viewCursor++) {
			var sectionName = content.sectionNames[content.viewCursor];
			var ids = content.recordIdsMap[sectionName].splice(0,content.displayPerSection * content.frames()); // take at most 6 which is fine for all mode, for "Source" which only has 3, it only returns 3 
			recordIdsSection.push([sectionName, ids]);  // store the sectionName to ids mapping, so that when ajax content gets back, we can resolve it 
			recordIds = recordIds.concat(ids);
		}
		
			// refresh the "moreSectio" button
		$('.moreSection h2').html("Loading...");	
			
		action.refreshMoreSection();
			// blocks for 2sec			
		//e.data.blockDurationOn = 750;
		
			// get thid fullContents for these sections
		base.ajax({'action':'getContentByIds','arg': recordIds}, e, callback);		
		return base.stopBubbling(e);

		
		function callback (dataArray, e, param) {
			$('.moreSection h2').html("View More");	
			
			
			var data = dataArray.shift();
			var bindings = $.parseJSON(data);
			var records = page.bindingsToRecords(bindings, content.entryTemplateId);
					
				// for each record resolve it using the above mapping array table	
			for (var i = 0; i < recordIdsSection.length; i++) {
				var sectionName = recordIdsSection[i][0];
				var sectionRecords = records.splice(0, recordIdsSection[i][1].length); 					
				action.applyContentOnSection(sectionName, sectionRecords, content.frames());	
			}
						
						
			base.setFavIconSrc($("#SectionPanel"));						
			action.processImageAndEvent($("#SectionPanel"), "._contentRowSection");
			action.refreshNext($('#SectionPanel .scrollableControl'));		
			
			if (moreSectionCallback) {
				moreSectionCallback();
			}						
		};			
	},
	
	hasMoreSection : function () {
		return content.viewCursor < content.sectionNames.length;
	},
	
	refreshMoreSection : function (e) {
		$('.moreSection').css('visibility', action.hasMoreSection() ? 'visible' : 'hidden');
	},
	
	scrollPrev : function (e) {
		var control = $(e.target).closest('.scrollableControl');
		var scrollable = control.find('.scrollable');
		var config = $(scrollable).data('store');
		var index = config.index;
		var unit = parseInt(config.unit, 10);	
		index = index 
			? index >= unit
				? index - unit
				: 0
			: 0; 
		config.index = index; 
		
		if (index >= 0) {
			var prev = $(scrollable).children().eq(index);
			$(scrollable).scrollTo(prev, config.speed || 350, {onAfter : function() {
				action.refreshNext(control);				
			}});
		}
		
		return base.stopBubbling(e);
	},

	scrollNext : function (e, callback) {

		var control = $(e.target).closest('.scrollableControl');
		var scrollable = control.find('.scrollable');
		var config = $(scrollable).data('store');
		var unit = parseInt(config.unit, 10);
		var limit = $(scrollable).children().size();
		var ajaxGet = config.ajaxGet != undefined ? config.ajaxGet : true; 			

			// advance index with number of units 
		config.index = config.index ? config.index + unit : unit;
			// if new index within the "last entries", set to first of this "last entries".
			// ie, size is 16, so it's 0 - 15 valid index, if index is at 13 14 15, then
			// set index to 13 so that next prev which is 10 will show 10 11 12. 		
		config.index = config.index >= limit - unit ? limit - unit : config.index; 
	
        executionTracker.addTrackPoint("scrollNext");

		var next = $(scrollable).children().eq(config.index);
		$(scrollable).scrollTo(next, config.speed || 350,{onAfter : function() {
				// we trigger the fetch before the index hits the limit
				// ie, if limit is 30  and index at 27 and unit is 3. we will trigger a load before hit the limit
			if (config.index + unit >= limit && ajaxGet) {			
				action.getNextScrollBatch(e, callback);
			}		
			else {
				action.refreshNext(control);				
			}	
			executionTracker.removeTrackPoint("scrollNext");
		}});
		return base.stopBubbling(e);




	},
	
	getNextScrollBatch: function  (e, callback) {
		/* should probably check if there is more before fire, right now could file empty getContentByIds() requests */
		
		var control = $(e.target).closest('.scrollableControl');
		var scrollable = control.find('.scrollable');
		var config = $(scrollable).data('store');


		var recordIds = null;
		if (config.isSection) {
			var sectionName = $(e.target).closest('._contentRowSection').find('._sectionName').text();
			recordIds = content.recordIdsMap[$.trim(sectionName)] || [];
		} else {
			recordIds = content.recordIdsForDigest;
		}

		var ajaxCallback = function (dataArray) {
			var data = dataArray.shift();
			var bindings = $.parseJSON(data);
			var records = page.bindingsToRecords(bindings, config.bindingTemplate);
			if (records.length > 0) {
				var html = page.recordsToHtml(records, config.segmentTemplate || config.bindingTemplate);
				$(scrollable).append(html);
				// probably need to avoid double process already marked
				base.setFavIconSrc($(scrollable));
				action.processImageAndEvent($(scrollable));
			}
			// rightnow it was used by digest to post process the scaleTextToFit
			callback = callback || config.callback;
			if (callback) {
				callback = eval(callback);
				callback();
			}
			action.refreshNext(control);
		};

			// if no more just return with empty "data array" as returned by server		
		if (recordIds.length == 0) {
			ajaxCallback(["[]"]);	
		}
		else {
			base.ajax({'action':'getContentByIds','arg': recordIds.splice(0, content.displayPerSection * content.frames())}, e, ajaxCallback);
		}			
	},	
	
		// config.index is the "FIRST" item of the current window
	refreshNext: function (scrollableControls) {
		 $(scrollableControls).each(function(){
		 	
		 	var scrollable = $(this).find('.scrollable');
			var config = scrollable.data('store');
			var index = config.index;
			var unit = parseInt(config.unit, 10);	
			var limit = scrollable.children().size(); 			
		 					
			var nextIndex = index ? index + unit :  unit;
			var prevIndex = index ? index :  -1;
			
			var next = $(this).find('.next');
			var prev = $(this).find('.prev');  
			
			if (nextIndex >= limit) {
				next.addClass('disabled').removeClass('enabled');
			}
			else {
				next.removeClass('disabled').addClass('enabled');				
			}
			
			if (prevIndex > 0) {
				prev.removeClass('disabled').addClass('enabled');
			}
			else {
				prev.addClass('disabled').removeClass('enabled');								
			}
		 });
	},
	
	
	test : function (e) {
		alert('test');
	},
	

	scrollToTop : function (selector) {
		if (selector) {
			$(selector)[0].scrollTop = 0;
		}
		else {
			window.scrollTo(0,0);
		}
	},


	getContentForSection: function () {	
		
		if (content.mode === content.modeType.All) {
			var sectionName = $.trim($(this).text());
			$('#appuiCategoryDivId ._categoryEntry').each(function () {				
				var name = $.trim($(this).text());				
				if (name == sectionName) {
					$(this).triggerHandler('click');
					return false;
					
				}
			});
		}
		
		if (content.mode === content.modeType.Category) {
			var sectionName = $.trim($(this).text());
			$('#appuiSourceDivId ._sourceEntry').each(function () {				
				var name = $.trim($(this).text());				
				if (name == sectionName) {
					$(this).triggerHandler('click');
					return false;
					
				}
			});
		}
 
		
		
	},


	getContentForDigest: function (e) {
		highlight.resetGroup(['source']); // clear source hightlight		
		action.clearSourcePortlet(true); // clear source portlet - hide only
		action.getContentForSource(e, content.modeType.All);		
	},

	getContentForCategory: function (e) {
		highlight.resetGroup(['source']); // clear source hightlight
		var categoryId = e.categoryId || $(e.target).data('store').id;
		

			// if source is not loaded, ie (sources are stored in a div with id equal to categoryid)
		if ($('#appuiSourceDivId').children().filter('[categoryId="' + categoryId + '"]').length == 0) {
			base.ajax({'action':'getSourceForCategory','arg':categoryId}, e, callback);					
		}
		else {
			displaySources();						
		}
		
		function displaySources () {

			$('#appuiSourcePortletDivId').removeClass('hide');
			$('#appuiSourcePortletDivId .portletHeader h6').text($(e.target).html() + " Sources");

				// hide all sources and only show the ones that is matched
			$('#appuiSourceDivId').children().css('display','none');
			$('#appuiSourceDivId').children().filter('[categoryId="' + categoryId + '"]').css('display','block');
			
				// refresh scrollbar
			lightScrollBar.refresh("#appuiSourcePortletDivId .portletContent");
			
				// invoke getContentForSource, with e.currentTarget as "this"
			$.proxy(action.getContentForSource, e.target)(e, content.modeType.Category);									
		};
		
		function callback (dataArray, e, param) {
			for (var i = 0, size = dataArray.length; i < size; i = i + 2) {	
					// standard parse and apply
				var data = dataArray.shift();
				var div = $('<div/>').attr('categoryId', categoryId);
				page.applyBindingsToId($.parseJSON(data), div, 'appuiSourceDivId');
				$('#appuiSourceDivId').append(div);					
				base.processEventMeta($(div));
				base.setFavIconSrc($(div));
			}
			displaySources();
		};	
	},
		
	clearSourcePortlet : function (hideOnly) {
		$('#appuiSourcePortletDivId').addClass('hide');
		
		if (!hideOnly) {
			$('#appuiSourceDivId').html('');
		}	
	},	
		
	getContentForSource: function (e, modeSpecified) {
		content.mode = modeSpecified ? modeSpecified : content.modeType.Source;
		
		if (content.mode == content.modeType.Source) {
			highlight.resetGroup(['category']); // clear source hightlight
		}
				
		var sources = e.sources 
			? e.sources
			: $(e.target).data('store') 
				? $(e.target).data('store').sources 
				: [];

		base.ajax({'action':'getContentForSource','arg' : [content.mode].concat(sources)}, e, callback);		
		function callback (dataArray, e, param) {
			action.scrollToTop()
			var data = dataArray.shift();
			var bindings = $.parseJSON(data);
			action.applyContent(bindings, content.mode);	
			action.setHeaderOnDigest($(e.target).text()); // set pretty orange header
		};			
	},
	

	
	
		// should refine so that already processed does not get called again
	processImageAndEvent: function (rootElement, groupSelector) {
					
			// load large images (ie, digest section)		
		 image.loadImages(rootElement.find('.contentEntryLarge'), "._entryContent", "._entryDisplay", imageLargeLoadCallback, false, false);
		
			// load the regular images, 2nd half of digest and the sectional section
		if (groupSelector) {
				// perform at group selector level so that "duplicate images" does not show up for each group, ie if same row has same images like ads, only show once
 			rootElement.find(groupSelector).each (
				function () {
					image.loadImages($(this).find('.contentEntry'), "._entryContent", "._entryDisplay", imageLoadCallback, false, true);
			});
		}
		else {
				// else treat as single group, ie (click next) which is has smaller scope (ie, same group)
			image.loadImages(rootElement.find('.contentEntry'), "._entryContent", "._entryDisplay", imageLoadCallback, false, true);
		}
		
		
		
		base.processEventMeta(rootElement);  // change this to process one level only

		function imageLoadCallback ()
		{
			var imageResult = image.imageSizeFilter(this);
			if (imageResult) {
				$(imageResult.imgContainer).addClass('hasImage cssImg');
				// append for mutliple view, // if the image is currently viewable then animate otherwise don't
				$(imageResult.imgContainer).find('._entryImg').append(imageResult.img).css('display','block'); // check loading time. if takes fucking long then fade!!!!
				addVideoMarker(this);
				
			}
			else {		
				image.nextImagesLoadingQueue(this.imagesLoadingQueue);
			}
		}
		
		function imageLargeLoadCallback ()
		{
			var imageResult = image.imageSizeFilter(this);
			if (imageResult) {
					// if too small we'll just skip - 
					// this is ok since google.lookupImage() in scaleDigestHeaderToFit() will kick in in 1 sec to attemp to load if no images is there! : )
				if (imageResult.width > 306 && imageResult.height > 186 && $(imageResult.imgContainer).children().length == 0) {
					imageResult.img = $(imageResult.img).css('display','none');
					$(imageResult.imgContainer).html(imageResult.img); // for large we just set it to show just one
					addVideoMarker(this);
					$(imageResult.imgContainer).find('img').fadeIn(500);					
				}				
			}
		} 		 
		
		function addVideoMarker (imageResult) 
		{
			if (imageResult.mediaType == "OBJECT" || imageResult.mediaType == "IFRAME" ) {
				var videoMarker = '<div class="waterMark videoPlayBG corner20"></div>' + 
						'<div class="waterMarkContent videoPlayImage"></div>';
						
				$(imageResult.containingElement).find('._entryDisplay').append(videoMarker);
			}				
		}
	},
	
	applyContentOnDigest : function (records, noOfFrames) {
		
			// reset entire digestPanel html
		page.applyRecordsToId('#DigestPanel', [], 'appuiMainBodyDigestPanelDivId');
		
		var largeScroll = $('#DigestPanel .one .scrollable'); 	
		var smallScroll = $('#DigestPanel .two .scrollable');
		
			
		/* 	Used to have both scroll and each have extra frame, for now disable the second and only allow first
		  
		 	// insert 2 page(scroll)
		for (var i = 0; i < noOfFrames;i++) {
			page.applyRecordsToId(largeScroll, records.splice(0, 1), 'appuiContentLargeDivId', true);
			page.applyRecordsToId(smallScroll, records.splice(0, 3), 'appuiContentAltDivId', true);
		}
		*/
		
		page.applyRecordsToId(largeScroll, records.splice(0, 1), 'appuiContentLargeDivId', true);
		page.applyRecordsToId(smallScroll, records.splice(0, 3), 'appuiContentAltDivId', true);
		page.applyRecordsToId(largeScroll, records.splice(0, 4), 'appuiContentLargeDivId', true);

		
		action.scaleDigestHeaderToFit();
	},	
	
		// this is used as callback when click next on digest content
	scaleDigestHeaderToFit : function () {
			// scale titles to fit
		$('#DigestPanel .contentEntryLarge ._entryTitle').each(function(){
			$(this).scaleTextToFit();
		});
		
		$('#DigestPanel .contentEntryLarge ._entryDisplay').each(function(){
			
			var entryDisplay = $(this);
			
			setTimeout(function () {
				var children = entryDisplay.children();
				if (children.size() == 0) { // means no image
					var parent = entryDisplay.parent('.contentEntryLarge');
					var title = $(parent).find('._entryTitle').text();
					google.lookupImage(title, entryDisplay);				
				}
			}, 1000); 
		});		
		
		
	},
	
	setHeaderOnDigest : function (header) {
		
		//if ($.trim(header) != "Digest") {
		//	header = "Digest \u25CF " + header;
		//}
		//$('#DigestPanel .header.two').html(header);//.fadeOut(250).fadeIn(250).fadeOut(250).fadeIn(250);
		
		//.css("-moz-binding","url('/3/ellipsis.xml#ellipsis')");
	},
	
	
	applyContentOnSection : function (sectionName, records, noOfFrames) {
		
		if (records.length == 0) {
			return;
		} 
		
		var modeClass = "_mode" + content.mode; 
		
			// set third panel
		if (content.mode == content.modeType.All) {
			var categoryHtmlAsBindings = [modeClass, sectionName, page.recordsToHtml(records.splice(0, content.displayPerSection * noOfFrames), content.entryTemplateId)];
			page.applyBindingsToId(categoryHtmlAsBindings, '#SectionPanel', content.entryRowTemplateId, true);
			
			/*
			var categoryContentMap = {};
			$('#appuiCategoryDivId ._categoryEntry').each(function () {
				var categoryName = $.trim($(this).text());//.trim();
				var categorySources = $(this).data('store').sources;
				for (i = 0; i <records.length; i++) {
					var sourceId = records[i].sourceId;
					if ($.inArray(sourceId, categorySources) >= 0) {
						categoryContentMap[categoryName] = categoryName in categoryContentMap ? categoryContentMap[categoryName] : []; 
						categoryContentMap[categoryName].push(records[i]);
					}					
				}
			});
			
			for (categoryName in categoryContentMap) {
				var categoryHtmlAsBindings = [categoryName, page.recordsToHtml(categoryContentMap[categoryName], content.entryTemplateId, 6)];
				page.applyBindingsToId(categoryHtmlAsBindings, '#SectionPanel', content.entryRowTemplateId, null, true);
			}
			* */
		}
		
		else if (content.mode == content.modeType.Category) {
			var categoryHtmlAsBindings = [modeClass, sectionName, page.recordsToHtml(records.splice(0, content.displayPerSection * noOfFrames), content.entryTemplateId)];
			page.applyBindingsToId(categoryHtmlAsBindings, '#SectionPanel', content.entryRowTemplateId, true);
			/*
			var sourceContentMap = {};
			for (i = 0; i <records.length; i++) {
				var sourceId = records[i].sourceId;
				sourceContentMap[sourceId] = sourceId in sourceContentMap ? sourceContentMap[sourceId] : []; 
				sourceContentMap[sourceId].push(records[i])					
			}			
			
			$('#appuiSourceDivId ._sourceEntry').each(function () {
				var sourceId = $(this).data('store').sources[0];
				for (aSourceId in sourceContentMap) {
					if (sourceId == aSourceId) {
						var sourceName = $(this).text();
						var sourceHtmlAsBindings = [sourceName, page.recordsToHtml(sourceContentMap[sourceId], content.entryTemplateId, 6)];
						page.applyBindingsToId(sourceHtmlAsBindings, '#SectionPanel', content.entryRowTemplateId, null, true);
					}	
				}			
			});
			*/
					
		}
		
		else if (content.mode == content.modeType.Source) {
			for (i = 0; i <records.length; i++) {
				var categoryHtmlAsBindings = [modeClass, sectionName, page.recordsToHtml(records.splice(0, content.displayPerSection * noOfFrames), content.entryTemplateId)];
				page.applyBindingsToId(categoryHtmlAsBindings, '#SectionPanel', content.entryRowTemplateId, true);
			}			
		}		
		
	},
	
	applyContent: function (bindings, mode) {

			// raw data
		var sectionNames = bindings[1];
		var recordIdsList = bindings[2];
		var recordsMap = bindings[0];

			// reset mode
		content.mode = mode;
		
			// noOfFrames - 
		var noOfFrames = content.frames(); 
		
			// reset DigestPanel
		//	$('#DigestPanel').empty();
		//	$('#SectionPanel').empty();
		//$('#DigestPanel > *').remove();
		//$('#SectionPanel > *').remove();
		$('#DigestPanel').html('');
		$('#SectionPanel').html('');
		
		

			// digest
		var recordIdsForDigest = recordIdsList.pop(); // last one in array is digest
		recordIdsForDigest.splice(0, 4 * noOfFrames);	 // remove 4 * 2 from digest ids since it's already provided in recordsMap as full contents  see. Content.java
		var records = page.bindingsToRecords(recordsMap.pop(), content.entryTemplateId); // last one in map is digest
		action.applyContentOnDigest(records, noOfFrames);
		

			// section
		var records = [];
		var recordIdsMap = {};		
		for (var i = 0, size =  recordIdsList.length; i < size; i++) {
			var sectionName = $.trim(sectionNames[i]);
			recordIdsMap[sectionName] = recordIdsList[i];

				// process 
			if (i < recordsMap.length) {
				recordIdsList[i].splice(0, content.displayPerSection * noOfFrames);	// remove 3 * 2 from section ids since it's already provided in recordsMap as full contents.
				records = records.concat(page.bindingsToRecords(recordsMap[i], content.entryTemplateId));
				action.applyContentOnSection(sectionName, records, noOfFrames);
			}
		}
				
			// reset content
		content.recordIdsForDigest = recordIdsForDigest;		
		content.recordIdsMap = recordIdsMap;
		content.sectionNames = sectionNames;
		content.viewCursor = Math.min(content.noOfSections, recordIdsList.length);


		base.setFavIconSrc($("#ContentPanel"));
		action.processImageAndEvent($("#ContentPanel"), "._contentRowSection");
		action.refreshNext($('#ContentPanel .scrollableControl'));
		action.refreshMoreSection();
	}
	
	

		
}


contentDetail = {
	
	currentContent: null,


/*
 	moreContent: function (e) {
		var row = $(contentDetail.currentContent).closest('.scrollableControl');
		$(".viewContentDetailCenter .moreContentEntryRowSection").append(row);
		$(".viewContentDetailCenter .moreContentEntryRowSection").removeClass('hide');		
	},

*/	

		// basically use linkedlist so that each time when nextCOntent is called, set a back reference on the target element as 
		// data('prevContent') 
	prevContent: function (e) {
		var prevContent = $(contentDetail.currentContent).data('prevContent');
		if (prevContent) {
			$(prevContent).triggerHandler('click');
		}
	},
		

		// use existing scrollableControl to fetch new contents, since if user start using next
		// s/he is going to use it for multiple which then make sense to get batches 	
	nextContent: function (e, optionalNextTarget) {
			// get next sibling based from currentContent
		var nextTarget = optionalNextTarget || $(contentDetail.currentContent).next();		
		
			// if there is one grab it - 
		if (nextTarget.length > 0) {
			contentDetail.closeContent(e, true);
			$(nextTarget).data('prevContent', $(contentDetail.currentContent));
			$(nextTarget).triggerHandler('click');
		} 
			// else no more in the current row 
		else {
			
				// attemp to trigger "next" on current row to bring in more content
			var scrollableControl = $(contentDetail.currentContent).closest('.scrollableControl'); 			
			var scrollNext = scrollableControl.children(".next").first();
			action.getNextScrollBatch({target:scrollNext}, function () {
				nextTarget = $(contentDetail.currentContent).next();
				if (nextTarget.length > 0) { // if after fetch and there is a next..
					contentDetail.closeContent(e, true);
					$(nextTarget).data('prevContent', $(contentDetail.currentContent));
					$(nextTarget).triggerHandler('click');
				}
				else {
						// find the next scrollableContro (next row of content) - 
						// note - convert to $('#ContentPanel .scrollableControl').find(scrollableControl).next(); when use jquery 1.6
					var scrollableControls = $('#ContentPanel .scrollableControl'); 
					var idx = $.inArray(scrollableControl[0], scrollableControls);
					var nextScrollableControl = scrollableControls[idx + 1];
					
						// if there is an row, get first entry and call nextContent again with optional contentEntry
						// (supposedly we should check if there is "more" and attempt to get more rows) 
					if (nextScrollableControl) {
						var contentEntry = $(nextScrollableControl).find('.contentEntry').first();
						contentDetail.nextContent(e, contentEntry);
					}
					else {
						if (action.hasMoreSection()) {
							action.moreSection(e, contentDetail.nextContent);
						}
						else {
								// otherwise close
							contentDetail.closeContent(e, false);
						}
					}
				}
			});
		}
	},

	
	closeContent: function (e, closeContentOnly) {
		//event.preventDefault();		
		
		
	//	base.clearSelection();
	//	base.stopBubbling(e.originalEvent);	
		iframe.stopLoading();
		
		if (!closeContentOnly) {
			base.modalOff();
		}
		$(".viewContentDetailCenter").css('display','none');
		$(".viewContentDetailCenter iframe").attr('src','');
		//alert('test');
		//return false;
	},	
	
		// cache for "large" rss content	
	largeContentCache : {},
	fetchLargeContent : function (e, callback) {
		
			// create recordIds array where the first one is the one to be fetched		
		var recordIds = [$(contentDetail.currentContent).data('store').rid];
		
			// check cache
		var data = contentDetail.largeContentCache[recordIds[0]];
		
			// if non matched
		if (!data) {

				// first release previous cached ones
			contentDetail.largeContentCache = {};
			
				// read ahead next content and get their ids
			var nextTarget = $(contentDetail.currentContent).next();
			for (var j = 0; nextTarget.length > 0; j++) {
				if ($(nextTarget).data('store').large) {
					recordIds.push($(nextTarget).data('store').rid);	
				}
				if (recordIds.length > 4) { // up to 4 entries
					break;
				}
				nextTarget = $(nextTarget).next();
			}
			
				// ajax get
			base.ajax({'action':'getRssContent','arg': recordIds}, e, 
				function (dataArray, e, param) {
						// store in cache
					for (var i = 0, size = recordIds.length; i < size; i++) {
						contentDetail.largeContentCache[recordIds[i]] = dataArray.shift();	
					}
					getLargeContentCache();
			});	
		}
		else {
			 getLargeContentCache();
		}
		
		function getLargeContentCache () {
			// get the intend record from cache and return
			data = contentDetail.largeContentCache[recordIds[0]];
			data = $.parseJSON(data);					
			callback(data);
		}

		
	},
	
	viewContent: function (e, loadIframeRightAway) {

		var target = $(e.currentTarget);
		contentDetail.currentContent = target;
		
		if (target.data('store').large) {
			contentDetail.fetchLargeContent(e, show);
		}
		else {
			show();
		}
		
		function show (content) {
			
			
			//  creates http://^serverHostURL()^/#0/6/ContentViewHTML?180567 
			//  which when bookmark allows directPage to http://^serverHostURL()^/0/6/ContentViewHTML?180567
			if (!FI.debugMode && window.history && window.history.replaceState) {
				 window.history.replaceState("", "", "0/6/ContentViewHTML/" + target.data('store').rid);	
			}
			else {
				window.location.hash = "0/6/ContentViewHTML/" + target.data('store').rid
			}
			
			
			// bring up modal
			base.modalOn();
			$(".viewContentDetailCenter").css('display','block');	

			// resolve content source and apply
			var contentNode = $(".viewContentDetailCenter ._entryContent");			
			content =  content ? content : $('._entryContent', target).html();
			
			$(contentNode).addClass("invisible");
			contentNode.html(content);
			
			
				// setup and copy text and titles, checks from overrides too 			
				// ie <object type="niunews" class="hide" title="Niunews Privacy Policy" banner="Privacy"/>
			var store = target.data('store');
			var title = contentNode.find('object[type="FreshInterval"]').attr('title') ||  $('._entryTitle', target).text();
			var siteTitle = $('._entrySiteTitle', target).html(); // can override but needs the styling of the original html.. do it later
			var banner = contentNode.find('object[type="FreshInterval"]').attr('banner') || "Latest";
				// remove overrides - comment this out during debug or test mode
			contentNode.find('object[type="FreshInterval"]').remove();

			
			$(".viewContentDetailCenter ._entryTitle").html(title);
			$(".viewContentDetailCenter ._entrySiteTitle").html(siteTitle);
			$(".viewContentDetailCenter ._entrySiteTitle .hide").removeClass('hide'); // remove hide to show time
			$(".viewContentDetailCenter .banner1").html(banner);

 				

				// cleanse content

			
				// remove empty elements
			contentNode.find('p,div,span,strong,em,b,h1,h2,h3,h4,h5,h6').each (
				function () {
					if ($.trim($(this).text()) === '' && $(this).children().size() == 0) {
						$(this).remove();
					}	
					else {
						$(this).css('font-size','').css('font-family','');
					}
									
				}
			)
			
			
			
				// reset all dimension style
			contentNode.find('*').each (
				function () {
					$(this).css('width','').css('height','');
					//$(this).addClass('corner2');						
					$(this).removeAttr('width');
					$(this).removeAttr('height');
				}
			);				

			
				// reset any div that has float attribute
			contentNode.find('div[style*="float"]').each( function () {
				$(this).css('float','none');	
			});
			
			
				
				// remove class attributes
			contentNode.find('*').removeAttr('class');			


			//contentNode.find('h1,h2,h3').addClass("gradientDefaultTopLeft corner2 hPadding1");


			
				// process links with images
			contentNode.find('a').each(
				function () {

						// if link has image flag it - so that we can float to left - better looking
					var counts = $('img', this).size();
					if (counts > 0 ) {
						
							// mark this link with "linkWithImage" only if no text, this is because we float left, and with text it would look weird 							
						if ($.trim($(this).text()).length == 0) {
							$(this).addClass('linkWithImage x'); // float to left
						} 
					}

						// all links opens popup window
					var href = $(this).attr('href');
					$(this).bind('click', function (e) {
							// if it's http then open new window, otherwise bubble up (ie mailto)
						if (href.indexOf("http") == 0) {
							iframe.openContentWindow(href);
							base.stopBubbling(e);
							return false;
						}	
						else {
							return true;
						}
					});
				}
			); 


			// we don't want img followed by the br which creates weird spacing line that breaks img float left'
			contentNode.find('img + br').remove();
			contentNode.find('img + br').remove(); // twice bcos some img is actually followed by 2 brs!
			contentNode.find('a.linkWithImage + br').remove();
	       
	       	// probably should only do it for div that has image
	        contentNode.find('div + br').remove(); // some website adds br after div which moves the text to next line instead wrapping, this alows the img wrap effect. (ie. Mashable)
			
			// we don't wont more than 1 BRs in a row'
			contentNode.find('br').each (
				function () {
					var test = $(this).nextUntil(":not('br')");
					var counts = test.size();
					if (counts >= 1) { // this means we have 2 because include self 
						test.remove();
						$(this).replaceWith('<br/>');
					}
				}
			);

				// set styles for list that has images, differenite ones only image vers has text			
			contentNode.find("li:has('img')").each(
				function () {
					if ($.trim($(this).text()) == "") {
						$(this).addClass("listWithImage imageOnly");
					}
					else {
						$(this).addClass("listWithImage");
					}
					
				}	
				
			);

				// fine list that are purely list of links to style it
			contentNode.find("ul:not(:has('li.listWithImage'))").each (
				function () {
					var links = $(this).find("li:has('a')").size();
					var nonLinks = $(this).find("li:not(:has('a'))").size();
					if (links > 2 && links > nonLinks) {
						$(this).addClass("listOfLinks corner2");
					}
				}
			);

			
				// Cap the first letter
			var dom = $('.viewContentDetailCenter ._entryContent');
			//util.replaceText(dom);			
			var firstTextNode = util.firstTextNode(dom);
			if (firstTextNode) {
				var text = $.trim(firstTextNode.nodeValue);	
				//$(firstTextNode).replaceWith('<span><span class="firstLetter">'+  text.charAt(0) + '</span>'+ text.substring(1) +'&nbsp;</span>');
				$(firstTextNode).replaceWith('<span class="firstLetter">'+  text.charAt(0) + '</span>'+ text.substring(1) + '&nbsp;');
			}
			
				// for first P we don't want leading margin-top
			contentNode.find('p:first').css('margin-top','0px !important');
			
				// create image to container map - so that once image loaded we can decided if we need to add "clear" to container (***)
			imgSrcToContainerMap = {}
			
			//* http://localhost/#0/6/ContentViewHTML/132874

			contentNode.find('p,dv,dl').has('img').each (  // Note used to be p,div,dl,a change to *
				function () {		
					var container = this;						
					$('img',this).each (function () {
						var src = $(this).attr('_truesrc');
							// there could be multiple of same images in a page , hence one src url can potentially have multipe containers
							// so use array to store img to containers 
						if (imgSrcToContainerMap[src] == null) {
							imgSrcToContainerMap[src] = [];
						}								
						imgSrcToContainerMap[src].push({container:container});
					});
				}				
			)				

				// treat iframe as vide for now
			contentNode.find('iframe,object').each(function () {
				var src = $(this).attr("_truesrc");
				if (src) {
					$(this).attr('src', src);
				}
				
			});	
				
			contentNode.find('iframe').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');	

	
				// videos
			if (FI.browserInfo.firefox) {
				contentNode.find('embed').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');
				contentNode.find('object:not(:has(embed))').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');

					// if for some reason it can't be styled then hide it
				//contentNode.find('object').each (function () {
				//	if ($(this).innerWidth() == 0) {
				//		$(this).addClass('hide')
				//	}
				//});
			}
			else if (FI.browserInfo.msie) {					
				contentNode.find('embed').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');
				// IE8 object can't not be sylted for some reason
			}
			else {
				contentNode.find('embed').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');
				contentNode.find('object:not(:has(embed))').addClass('_entryDisplayFrame cssImg _entryEmbedFrame dropShadow2 corner2');
			}
			
			
			
			

				// load images and remove Ads - this needs to be again because we may be doing lazy load "full description"
				// content from server and we need to cleanse the image again 
			image.loadImages(".viewContentDetailCenter", "._entryContent", null, imageLoadCallback, true, false);
			var isFirst = true;
			function imageLoadCallback ()
			{
				var imageResult = image.imageFrameFilter(this);
				if (imageResult) {
						// remove ths later
					//setTimeout(function () {
					//	$(imageResult.img.originalImg).replaceWith(imageResult.img); // replace with the loaded one
					//}, 500)
					$(imageResult.img.originalImg).replaceWith(imageResult.img); // replace with the loaded one

						// for first image, we tag it with "first" - so we can apply special css to it. ie (float left, size etc)
					if (isFirst) {
						$(imageResult.img).addClass('first');
						isFirst = false;				
					}

					
						// if image has a container, set container.img in the map
					var src = $(imageResult.img.originalImg).attr('src');
					var containers = imgSrcToContainerMap[src];
					if (containers) {
						for (var i = 0; i < containers.length; i++) {
							var container = containers[i];						
							container.img = $(imageResult.img);
						}
					}
					
						// since img can come out of order, each time img is loaded, iterate through ALL images again to ensure no tearing effect, 
						// Note - wait AFTER one second this way "ads image" should already be removed when testing the "left" position  
						// imgSrcToContainerMap created at (***)
					setTimeout(function () {
						for (imgSrc in imgSrcToContainerMap) { // for each img src
							var containers = imgSrcToContainerMap[imgSrc];
							for (var i = 0; i < containers.length; i++) { // each img src can potentiall have multipe container in the same page, 
								var container = containers[i];							
								if (container && container.img) {	// if has container, and img is downloaded					
									var left = $(container.img).position().left;
									
									console.log(imgSrc + ": " + left);
										// check if beyond 5px instead of 0px bcos some source have img wrapped ie borders etc
										// also we skipp small images because some source have list of small images lined up horiz
									if (left > 10 && !($(container.img).hasClass('_entryDisplaySmallFrame'))) { 
										///console.log("XXX wrapping " + imgSrc);
										$(container.container).addClass('clear overflow x'); // overflow does wrap around elements, clear pushes to both ends
									}
								}
							}
						}	
							// refresh scrollbar for each dowloaded images
						lightScrollBar.refresh(".viewContentDetailCenter .viewContentDetailArea");
										
					}, 1000);
					
				}
			}				
			
				// set preview title
			$(".viewContentDetailCenter .previewiFrameCoverHeader h2").html("View full article from " + store.siteTitle);

				// scale text to fit header
			
			//$(".viewContentDetailCenter ._entryTitle").scaleTextToFit(true);

				// refresh scrollbar
			lightScrollBar.refresh(".viewContentDetailCenter .viewContentDetailArea");

			
				// reset scroll to top
			action.scrollToTop(".viewContentDetailCenter .viewContentDetailArea");
			
			
				// XXX temp work for table (craiglist car section) that is too big, not ideal as it is run after 2.5 sec and no working for IE for some reason 
				// wrap table to me at most 648px - should use transform scale thing
			contentNode.find('table').wrap('<div style="max-width:648px"></div>');
			setTimeout(function() {
				contentNode.find('table').each (function() {				
					var w = $(this).outerWidth();
					if (w > 648) {
						var ratio = 648 / w;
						$(this).css("-ms-zoom", "" + ratio + ""); // scale somehow doesn't work for IE'
						$(this).css("-moz-transform","scale(" + ratio + ", " + ratio + ")");
						$(this).css("-moz-transform-origin","0 0");
						$(this).css("-webkit-transform","scale(" + ratio + ", " + ratio + ")");
						$(this).css("-webkit-transform-origin","0 0");
					}					
				});
			}, 2000);
			
				// toggle display of the "prev" link depending if it has "prevContent" that is setup in nextContent()
			$(".viewContentDetailCenter .cmdBar .five").toggleClass('hide', !$(target).data('prevContent'));
			
			$(contentNode).removeClass("invisible");
			/*
			setTimeout(function() {
				$(contentNode).removeClass("invisible");
			}, 250);
			*/
			
				// find the first element that has tabindex and focus it - this allows keyboard scorll to work
			$('.viewContentDetailCenter *[tabindex="0"]').first()[0].focus();
			
			
			if (store.link.indexOf("http://") == 0) {
					// init iframe, also if no scrollBar we load immediately -> since it indicates of short content 	
				//iframe.loadInit(store.link, !hasScroll || loadIframeRightAway);
					// used to load iframe rightaway if short content, but turns out to costly and unreliable. for now, wait use hover over
				iframe.loadInit(store.link, loadIframeRightAway);
			}
			else {
				$(".viewContentDetailCenter ._previewFrame").addClass('hide');
			}			
			
		};		
	},	
	
}

page = {
	
	segmentsTable: [],
	bindingNameTable: [],	
	bindingAnnonationsTable: [],

		// number of bindings is expect to be the same as declared in template and must be in order
		// duplicates like ^desc^ vs ^desc!^ is expect to pass in a placeholder like "" 
	bindingsToRecords: function (bindings, pageId)
	{
		var records = [];
		var bindingNames = page.bindingNameTable[pageId];		
		for (i=0; i<bindings.length;) {
			var aRecord = {};
			for (j=0; j<bindingNames.length; j++) {
					// record fields gets set once and only once even if there is duplicates,
					// ^desc^ vs ^desc!^ , this allow server to pass one copy of the same binding
					// which is "" emptystring 
				var bindingName = bindingNames[j];
				if (!(bindingName in aRecord)) {  
					aRecord[bindingName] = bindings[i];
				} 
				i++;	
			}
			records.push(aRecord);
		}
		return records;				
	},

	bindingsToHtml: function (bindings, pageId)
	{ 
		var records = page.bindingsToRecords(bindings, pageId);
		return page.recordsToHtml(records, pageId);
	},
	
	recordsToHtml: function (records, pageId)
	{ 
		var result = '';
		var segments = page.segmentsTable[pageId];
		var bindingNames = page.bindingNameTable[pageId];
		var bindingLessTemplate = bindingNames.length == 0 ? true : false;
		var bindingAnnonations = page.bindingAnnonationsTable[pageId];
		for (iter = 0; bindingLessTemplate || iter < records.length; iter++) {	
			var record = records[iter];
			var bindingNameIdx = 0;
			for (i=0; i<segments.length; i++) {
				var value = segments[i]; 
				if (value == FI.bindingMarker) {
					var bindingName = bindingNames[bindingNameIdx];
					var bindingValue = record[bindingName];
					var bindingAnnonation = bindingAnnonations[bindingNameIdx];
					value = page.processAnnonations(bindingAnnonation, bindingValue);
					bindingNameIdx++;
				}
				result = result + value;
			}
			if (bindingLessTemplate) {
				break;
			}
		}
		return result;				
	},
	
	/*
	recordsToDom: function (records, pageId, setRecordToDomData)
	{ 
		var doms =[];
		var segments = page.segmentsTable[pageId];
		var bindingNames = page.bindingNameTable[pageId];
		var bindingLessTemplate = bindingNames.length == 0 ? true : false;
		var bindingAnnonations = page.bindingAnnonationsTable[pageId];
		for (iter = 0; bindingLessTemplate || iter < records.length; iter++) {
			var html = '';
			var record = records[iter];
			var bindingNameIdx = 0;
			for (i=0; i<segments.length; i++) {
				var value = segments[i]; 
				if (value == FI.bindingMarker) {
					var bindingName = bindingNames[bindingNameIdx];
					var bindingValue = record[bindingName];
					var bindingAnnonation = bindingAnnonations[bindingNameIdx];
					value = page.processAnnonations(bindingAnnonation, bindingValue);
					bindingNameIdx++;
				}
				html = html + value;
			}
			html = $(html);
			if (setRecordToDomData) {
				html.data('recordData', record);
			}
			doms.push(html);
			if (bindingLessTemplate) {
				break;
			}
		}
		return doms;				
	},			
	*/
	
	applyRecordsToId:	function (id, records, pageId, append) 
	{
		var idNode = typeof id == 'string' ? $(id) : id;
		var html = page.recordsToHtml(records, pageId);
		if (append) {
			idNode.append(html);
		}
		else {
			idNode.html(html);
		}
		return idNode;
	},

	applyBindingsToId:	function (bindings, id, pageId, append) 
	{
		var records = page.bindingsToRecords(bindings, pageId);
		var idNode = page.applyRecordsToId(id, records, pageId, append);
	},

	processAnnonations : function  (bindingAnnonations, bindingValue) {
		
		for(var type in bindingAnnonations) {
			var arg = bindingAnnonations[type];
			
			if (type == "fmt") {
				var rawValue = Number(bindingValue);
				if (arg == "duration") {
					return formatTime(rawValue);
				}
				if (arg == "date") {
					var d = new Date(rawValue);
		            return $.datepicker.formatDate('DD,  M d, ' , d) +  d.toLocaleTimeString();
		            //return new Date(rawValue);
		        }
		        if (arg == "text") {
						// for some reason text() doesn't filter out some markups, do twice does the trick 
					var html = '<div>' + bindingValue + '</div>';
					var html2 = '<div>' + $(html).text() + '</div>';
					return $.trim($(html2).text());
		        }
		        if (arg == "base64") {
		        	return Base64.decode(bindingValue);
		        }
			}
		}
		return bindingValue;
	}			
}



entryDisplay = {
	mouseIn : function (e) {
		
		var target = $(e.currentTarget);
		target.attr('mouseIn',1);
		
		setTimeout(function () {		
			if (target.attr('mouseIn') ==1 ) {
				var hasImage = $(e.currentTarget).hasClass('hasImage');
				var entryImage = $(e.currentTarget).find('._entryImg');
				$(entryImage).fadeOut(200);
			}
		}, 350);		
		
		
	},	

	mouseOut : function (e) {
		
		var target = $(e.currentTarget);
		target.removeAttr('mouseIn');
				
		var hasImage = $(e.currentTarget).hasClass('hasImage');
		var entryImage = $(e.currentTarget).find('._entryImg');
		$(entryImage).fadeIn(250);		
	}	
}



input = {
		 
	mask: "\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF",
		 
	keydown: function (e) {
		var target = $(e.target); 
		var value = target.val();		
		var spanLabel = $(target).siblings('label').first();		
		spanLabel.addClass('invisible');
	},	 
 

		/* only reset when text is empty */
	blur: function (e) {
		var target = $(e.target); 
		var value = target.val();		
		var spanLabel = $(target).siblings('label').first();
			// if no value put label back
		if(value.length == 0) {
			//var label = target.data('store').label;
			//target.val(label);
			spanLabel.removeClass('invisible');
			target.addClass('dim');
		}		
		else {
			target.removeClass('dim');
		}
	},
	
		/* reset text when blur */
	blurReset: function (e) {
		var target = $(e.target); 
		var value = target.val();		
		var spanLabel = $(target).siblings('label').first();

		target.val('');
		spanLabel.removeClass('invisible');
		target.addClass('dim');
	}	
		
}


lightScrollBar = {
	
		// refreshs the scrollbar - basically calls scroll which does the init and updates
	refresh: function (selector) {
		var testEvent = {currentTarget:$(selector)};
		return lightScrollBar.scroll(testEvent);		
	},
	
	mouseIn : function (e) {
		var scrollArea = $(e.currentTarget);
		var config = scrollArea.data('store');
		
		if (lightScrollBar.scroll(e)) { // if has scrollable contents
			if (config.autoHide && !FI.browserInfo.msie) {		
				//config.scrollbar.fadeIn(350);
			}
			else {
				config.scrollbar.css('display','block');
				//scrollbar.bind('click', function () { alert('hello')});
			}
		}
		else {
			config.scrollbar.css('display','none');
		}
		//base.stopBubbling(e);
	},	

	mouseOut : function (e) {
		
		var scrollArea = $(e.currentTarget);
		var config = scrollArea.data('store');
		var scrollbar = scrollArea.siblings('.scrollbar'); 
		if (config.autoHide && !FI.browserInfo.msie) {  // IE hide scrollbar in some case will reset it for some reason 
			//scrollbar.fadeOut(350);				
		}
		//base.stopBubbling(e);
	},	
	
	
	scroll : function (e) {
		
		var scrollArea = $(e.currentTarget);
		var config = scrollArea.data('store');

			// lazy init
		if (!config.init) {
				// create scrollbarHandle div inside the scrollbar div
			config.scrollbar = $('<div class="scrollbarHandle corner2"/>').appendTo(scrollArea.siblings('.scrollbar'));
			config.scrollbarContent = scrollArea.find(config.scrollbarContent);
			config.skip = false;

				// make scrollBar draggable
			$(config.scrollbar).draggable({ 
				containment: 'parent',  // parent is the scrollbar container
				scroll:false, // no fault scrolling
				cursor:'pointer',
				start: function () {
					$(config.scrollbar).addClass('opacity7');
				},
				stop : function () {
					$(config.scrollbar).removeClass('opacity7');
				},
				
				drag: function() {
						// for mozzilla don't skip as their fire rate is less
					if (FI.browserInfo.firefox || !config.skip) { 
					
						var scrollTop = scrollArea.scrollTop();  
						var scrollHeight = scrollArea.height(); 
						var contentHeight = config.scrollbarContent.outerHeight();	// height of content
						var ratio = contentHeight / scrollHeight; // scroll bar display ratio
						
							// acutal y is current current top minus the offset top
						var netY = config.scrollbar.offset().top - scrollArea.siblings('.scrollbar').offset().top;

							// times ratio to get the actual y of the scrollableCOntent						
						netY = Math.round(netY * ratio);
						$(scrollArea).scrollTo( netY + "px", { axis:'y', queue:false} );

						//console.log(config.scrollbar.offset().top + ":" + scrollArea.siblings('.scrollbar').offset().top + ":" + ratio + ":" + netY);						
					}
						// toggle skip, basically only trigger drag altentiavely
					config.skip = !config.skip;
				}
			});
			config.init = true;
		}
		  
		
		// when scroll to bottom, scrollTop + scrollHeight = contentHeight!
		var scrollTop = scrollArea.scrollTop();  
		var scrollHeight = scrollArea.height(); 
		var contentHeight = config.scrollbarContent.outerHeight();	// height of content
		var ratio = scrollHeight / contentHeight; // scroll bar display ratio

		// ratio of scrollbar to scrollableContent
		base.stopBubbling(e);
		if (ratio < 1) {
			config.offsetTop = config.offsetTop || config.scrollbar.css('margin-top'); // default offsetTop
			var scrollBarHeight  = scrollHeight * ratio; 
			var newOffsetTop = scrollTop * ratio;
			config.scrollbar.css('height', scrollBarHeight + 'px');
			config.scrollbar.css('top', newOffsetTop + parseInt(config.offsetTop) + 'px' );
			config.scrollbar.css('display','block');
			return true;
		}
		else {
			config.scrollbar.css('display','none');
			return false;
		}
		
	}		
}

/*
config.barOffsetTop
config.scrollBar
config.content
config.container
*/
 
image = {
	
		// loadImages are called both by "previewMode" and "detailMode"
		//
		// the process is done by going through all media type and attempt to download all iamges
		//  
		// the elements containing the images, 
		// selector in elements to the images, 
		// selector in elements to set the images
		// callback gets a handle to the orginalImg and loadedImg
	loadImages: function (containingElements, imagesSelector, containerSelector, callback, notPreview, loadOneOnly) {
		
		var AllImageUrls = [];
		$(containingElements).each(function(i) {	
		
			var containingElement = $(this);
			var imagesLoadingQueue = [];
			containingElement.find(imagesSelector).find(notPreview ? "img" : ",img,iframe,object").each(function(i) {

					// actual url is stored in attr "_truesrc" which is done at server side
				var url = $(this).attr("_truesrc");
				var tagName = $(this)[0].tagName;
				
				// console.log(tagName + " : " + url + " : " + $(this).html());
				
				
					// if it's img type and it's an ad - remove it
				if (tagName == "IMG" && (
					!url || // no url
					url.indexOf("http://") < 0 || // relative url
					url.indexOf("http://ad.") == 0 ||  // ad
					url.indexOf("http://ads.") == 0 ||  // ad
					url.indexOf(".ads.") > 0 || // ad
					url.indexOf("/ad-") > 0)) { // ad
						$(this).remove();
				}
				else  {
						// when it's not previewMOde (ie detailView), this code would place an loading animation cursor
						// upon loaded do the following to replace the imageloader back to loaded iamge 
						//  $(imageResult.img.originalImg).replaceWith(imageResult.img);
					if (notPreview) {
						var width = $(this).attr('width');
						var height = $(this).attr('height');
						var css = {'width': width || '24px','height': height || '24px'};	
						var loader = $('<span/>').addClass('imgLoader imgLoaderBorder').css(css).attr('src',url);					
						$(this).replaceWith(loader);
					}

						// if this image has not been loaded before
					if ($.inArray(url, AllImageUrls) < 0) {    
							// save all "images as an object" into an ImageLoadingQueue of this containing element
							// later depending on the need, it will fetch "all" or "one at time"
						var myImage = {};
						AllImageUrls.push(url);
						
							// move img to end of queue, iframe,object to beginning of loading queue
						if (tagName == "IMG") {
							imagesLoadingQueue.push(myImage);	
						}
						else {
							imagesLoadingQueue.unshift(myImage);
						}
						
						
						myImage.containingElement = containingElement;
						myImage.originalImg = notPreview ? $(loader) : $(this);
						myImage.containerSelector = containerSelector;
						myImage.onload = callback;
						myImage.onerror = image.imageFailRemove;
						myImage.onabort = image.imageFailRemove;
						myImage.notPreview = notPreview;
						myImage.imagesLoadingQueue = imagesLoadingQueue; // queue attached 
						myImage.src = url;
						myImage.mediaType = tagName;
					}
				}
			})
			
				// kick off the loading Queue
			image.nextImagesLoadingQueue(imagesLoadingQueue, loadOneOnly);			
		});		
	},		
	
		// provided a queue in loadImages, it will load one image at a time
		// this method is useful so that 
		// 1) can load one at a time. for example, if there are 10 pics of given element and we only want to fetch 1 good one for display
		//    like in the content entry. so bascailly load 1 first, and on the callback, either timeout, or image to small then it will
		//	  call this method again to load the next one an so forth, all images has a reference to the queue
		// 2) load all as in view content
	nextImagesLoadingQueue: function (imagesLoadingQueue, loadOneOnly)
	{
		if (imagesLoadingQueue) {
			for (var i = 0, size = imagesLoadingQueue.length; i < size; i++) {
				var aImage = imagesLoadingQueue.shift();
				if (aImage) {
					
						// for object and iframe - parse out the "previewImage" if there is one, otherwise call nextImagesLoadingQueue() to load next one
					if (aImage.mediaType == 'OBJECT') 
					{
							// get "html" of this object then parse out the URL - come back and deal with differnt type of object later
							// http://stackoverflow.com/questions/5201534/get-youtube-or-vimeo-thumbnails-in-one-shot-with-jquery
						var html = $(aImage.originalImg).html();
					
							// for now support Object of youtube only
							// basically parse out the youtube url	
						var youtube = "http://www.youtube.com/v";
						var begin = html.indexOf(youtube);
						if (begin >= 0) {
								// extract youtube URL
							var end = html.indexOf( '"', begin);
							var url = html.substring(begin, end);
							url = util.thumbnailURL(url);
	
							if (url) {
								aImage.src = url;
							}
							else {
								image.nextImagesLoadingQueue(imagesLoadingQueue, loadOneOnly);
								return;
							}
						}
						else {
							image.nextImagesLoadingQueue(imagesLoadingQueue, loadOneOnly);
							return;
						}
					}
					
					if (aImage.mediaType == 'IFRAME') 
					{
							// get iframe's actual src and get it's preview Image
						var url = util.thumbnailURL($(aImage.originalImg).attr('_truesrc'));
						if (url) {
							aImage.src = url;
						}
						else {
							image.nextImagesLoadingQueue(imagesLoadingQueue, loadOneOnly);
							return;
						}
					}
					
						// load actual image
					var myImage = new Image();
					myImage.containingElement = aImage.containingElement;
					myImage.originalImg = aImage.originalImg;
					myImage.containerSelector = aImage.containerSelector;
					myImage.onload = aImage.onload;
					myImage.onerror = aImage.onerror;
					myImage.onabort = aImage.onabort;
					myImage.notPreview = aImage.notPreview;
					myImage.imagesLoadingQueue = aImage.imagesLoadingQueue;
					myImage.mediaType = aImage.mediaType;
					myImage.src = aImage.src;

				}
				if (loadOneOnly) {
					return;
				}		
			}
		}
	},
	
	imageFailRemove: function ()
	{
		this.originalImg.remove();
		image.nextImagesLoadingQueue(this.imagesLoadingQueue, false);
	},
	
	frameH : 114,
	frameW : 186,
	
		// resize for the "preview"
	imageSizeFilter: function (imageObj)
	{
		if (!imageObj) {
			return;
		}
		
		var h = image.frameH;
		var w = image.frameW;
		var r = w / h;
		
		var imageResult = image.imageAdFilter(imageObj);
		if (imageResult) {
			imageObj = imageResult.img;
			var imgContainer = imageObj.imgContainer || imageObj.containingElement.find(imageObj.containerSelector);
			
				// if both w and h are smaller than frame
			if (imageObj.width < w && imageObj.height < h) {				
				if (h - imageObj.height > 10) {	// if h different is beyond 20 px than stretch to 104 which is 10px border
					var img = '<div class="corner2 gradientDefault" style="display:block;float:left;height:'+ image.frameH + 'px;width:' + image.frameW +  'px"><img class="corner2" src="' + imageObj.src + '" style="position:relative;top:50%;margin-top:-' + ((image.frameH - 10)/2) +'px;display:block;height:' + (image.frameH - 10) + 'px;width:auto"/></div>'
				}
				else {	// else just center it
					var img = '<div class="corner2 gradientDefault" style="display:block;float:left;height:'+ image.frameH + 'px;width:' + image.frameW +  'px"><img class="corner2" src="' + imageObj.src + '" style="position:relative;top:50%;margin-top:-'+ imageObj.height / 2  +'px;display:block;height:'+ imageObj.height + 'px;width:'+ imageObj.width +'px"/></div>'
				}
			}
			else {
					// determine if image is wide or slim then stretch accordingly
				var ratio = imageObj.width / imageObj.height;
				
				var img = ratio < r
  					? '<img src="' + imageObj.src + '" style="width:' +  $(imgContainer).width() + 'px"/>' 	
  					: '<img src="' + imageObj.src + '" style="height:' +  $(imgContainer).height() + 'px"/>'
				
				
/*				
					? imageObj.width < 186 
						? '<img src="' + imageObj.src + '" style="min-width:186px"/>'
						: '<img src="' + imageObj.src + '" style="max-width:186px"/>'	
					: imageObj.height < 114 
						? '<img src="' + imageObj.src + '" style="min-height:114px"/>'
						: '<img src="' + imageObj.src + '" style="max-height:114px"/>';
*/						
			}
			

			return {'imgContainer':imgContainer,'img':img,'width':imageObj.width,'height':imageObj.height};
		}
		return; 		
	},
	
		// removes ads
	imageAdFilter: function (imageObj)
	{
		if (!imageObj) {
			return;
		}
		
		
		var height = imageObj.height;
		var width = imageObj.width;
		var remove = false;
		if (width == 1 &&  height == 1) {
			remove = true;
		}	
		if (width < 64 || height < 60 ) {
			remove = true;
		}
		if (width > (height * 4)) {
			remove = true;
		}

		if (remove) {
			if (imageObj.originalImg) {
				imageObj.originalImg.remove();
			}
			return;
		}
		return {'imgContainer':null,'img':imageObj};
	},
	
	entryDisplayStetchToFitFrameFrom: 404,
	entryDisplayStetchToFitFrameTo : 626,
	entryDisplayAdjustToFitFrameFrom: 147,
	entryDisplayAdjustToFitFrameTo: 404,
	entryDisplaySmallFrameFrom: 80,
	entryDisplaySmallFrameTo: 80,
	
		// frames it - not used
	imageFrameFilter: function (imageObj)
	{
		if (!imageObj) {
			return;
		}
		
		
		var imageResult = image.imageAdFilter(imageObj);
		if (imageResult) {
			imageObj = imageResult.img;
			
			if ($(imageObj).attr('src')) {
				$(imageObj).addClass("_entryDisplayFrame cssImg dropShadow2");
			}
				
			var width = imageObj.width;
			var height = imageObj.height;
			if (width >= image.entryDisplayStetchToFitFrameFrom && width < image.entryDisplayStetchToFitFrameTo) { // for this range upto max-width _entryDisplayFrame
				$(imageObj).addClass("_entryDisplayStetchToFitFrame");
			}

			if (width >= image.entryDisplayAdjustToFitFrameFrom  && width < image.entryDisplayAdjustToFitFrameTo) { // for this range we set to 186  
				$(imageObj).addClass("_entryDisplayAdjustToFitFrame"); 
			}
			
			if (width <= image.entryDisplaySmallFrameFrom && height <= image.entryDisplaySmallFrameTo) { // just small don't do anything'
				$(imageObj).addClass("_entryDisplaySmallFrame");
			}
			else {
				$(imageObj).addClass("corner2");			
			}
			
			
			return {'imgContainer':null,'img':imageObj};			
		}
		return ;
	},


		// frames image in the DetailPanel
	frameImage: function (images)
	{
		var imageArrays = $.type(images) === "string"
			? $(images).find("img")
			: images;
			
						
		imageArrays.each(function(i) {
			
			if ($(this).attr('src')) {
				$(this).addClass("_entryDisplayFrame cssImg");
			}
			
			var width = this.width;
			if (width >= 400  && width <= 600) {
				$(this).addClass("_entryDisplayStetchToFitFrame");
				//console.log(width + " is enlarged!");				
			}
			
		})
				
		if (imageArrays.length == 1) {			
			var img = $(imageArrays[0]);
					// does auto enlarge, if , smaller at 2.5x then double size
			var width =  img.innerWidth();
			var height =  img.innerHeight();
			var maxWidth = img.css('max-width').replace('px','');
			var maxHeight = img.css('max-height').replace('px','');
			if (width * 2.5 < maxWidth && height * 2.5 < maxHeight) {
				var newWidth = width * 2 + "px";
				img.css('width', newWidth);
			}
		}				
	}	
	
}


base = {
	

	
	
	setFavIconSrc: function (folder) {
		
			// IE8 and FF3.6 does not support background image size. So we can't scale to 12px or 16px as we can do so with regular img
			// therefore, we replace the <div> background image, to be <img> src before going throught the set favicon phase.
			// in that face it basically replaces the image src to _truesrc
			
		if (FI.browserInfo.msie || (FI.browserInfo.firefox && FI.browserInfo.firefox <= 3.6)) {
			$(folder).find("._favicon").replaceWith(function() {
				var iconClass = $(this).hasClass('icon12') ? 'icon12' : 'icon16';
				return "<img src='2/rss-icon.png' _truesrc='" + $(this).attr("_truesrc") + "' class='" + iconClass + " opacity6 _favicon' style='margin-right:5px'/>";
			});
		}
		
		
		$(folder).find("._favicon").each(function(i) {
			var faviconURL = $(this).attr("_truesrc");
			if (faviconURL) {
				var defaultURL = $(this).attr("src");
				if (faviconURL != defaultURL) {
					faviconURL = faviconURL.toString() == "" ? "2/rss-icon.png" : faviconURL
	
					var ele = $(this);		
					var favicon = new Image();
					favicon.onload = function () {
						ele.attr("src", faviconURL);
						ele.css("background","url('" + faviconURL + "') no-repeat");
					};
					favicon.src = faviconURL;
				}
			}						
		})					
	},

	modalOn:	function () {
			// firefox will reset ALL scrollbars if attempt to hide/unhide the BODY scrollbar - hence comment out
		if (!FI.browserInfo.firefox) {
			$('body').css('overflow-y','hidden');			
		}
		$('body').css('width','1278px');
		$('#ModalBG').css('display','block');
	},
	
	modalOff:	function () {
		$('body').css('width','1260px');
		if (!FI.browserInfo.firefox) {
			$('body').css('overflow-y','scroll');		
		}		
		$('#ModalBG').css('display','none');
		
	},
	
	blockDurationOn:	function (duration) {
		$('#ModalBG').addClass("transparent");
		$('#ModalBG').css('display','block');
		setTimeout (function() {
			$('#ModalBG').removeClass("transparent");
			$('#ModalBG').css('display','none');
			base.modalOff();
		}, duration);
	},

	
	zoom: function (e, event, requireFloat) {
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
		        	margin: 'auto',
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
		  
			$(div).position(
				{my: "center center",
				 at: "center center",
				 of: $('#ModalBG'),
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

		   base.modalOn();
		   base.stopBubbling(event);
	},
	
	ajax: function (param, e, callback) {

		
		param.url = FI.http + FI.hostName + "/5/" + FI.sessionId + "/" + FI.uniqueClientToken;

		
		param.action = param.action != undefined 
						? param.action
						: e.data.serverHandler != undefined
							? e.data.serverHandler 
							: e.data["_" + e.type]; 


		if ($.isArray(param.arg)) {
			param.arg = param.arg.join('/');			
		}

	    
	    param.arg = param.arg != undefined 
		            	? e && e.data && e.data.arg != undefined 
		            		? param.arg + "/" + e.data.arg
		            		: param.arg
		            	: e.data.arg; 			
		            	
		
	
		var loadingCursor = $('#loading-cursor'); // can cache this
		
		
			// display loading cursor
		//loadingCursor.css('background-position', '-100px -100px');
		//loadingCursor.css('display','block');
		if (e && e.data && e.data.blockDurationOn) {
			base.blockDurationOn(e.data.blockDurationOn);
		}
		
			// workaround so that the above loading cursor can show. 
			// if call ajax directly the display won't show  
		//setTimeout (function () {
			base.ajaxInternal (param, e, callback);
		//}, 10);
	},
	
	
	ajaxInternal: function (param, e, callback) {
	
		executionTracker.addTrackPoint("ajaxInternal");
		$.ajax({
			async: true,
			type: 'GET',  // POST works, but remember to append "data" portion below to here
			url: param.url + "/" + param.action + (param.arg == undefined ? "" : "/" + param.arg),
			contentType: 'charset=UTF8',
			/* context: e, somehow IE8 has problem with this - the fn.extend when copies the options- some property can not be copied*/ 
			dataFilter: function (data, type) {
				//console.log("data...");
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
			beforeSend: function (jqXHR, settings) {
				//jqXHR.setRequestHeader("Testing", 'XXX'); enject my own header
			},
			
			success : function (dataArray, textStatus, XMLHttpRequest) {
				//console.log("success...");
				if (callback) {
					callback(dataArray, e, param);
				}
				executionTracker.removeTrackPoint("ajaxInternal");					
			 },
			error : function (jqXHR, textStatus, errorThrown) {
				//console.log("error...");
				executionTracker.removeTrackPoint("ajaxInternal");				
			} 
		});						
	},
	
	stopBubbling: function (event)
	{	
		var e = event == undefined ? window.event : event;
		if (!e) {
			return false;
		}
		
		e.cancelBubble = true;
			// sometimes IE craps so try/catch
		try {
			if (e.preventDefault) {
				e.preventDefault();
			}		
			if (e.stopImmediatePropagation) {
				e.stopImmediatePropagation();
			}
			if (e.stopPropagation) {
				 e.stopPropagation();
			}
		}
		catch (err) {
		}
		return false;
	},
	
	clearSelection: function ()
	{
	    if(document.selection && document.selection.empty) {
	        document.selection.empty();
	    } else if(window.getSelection) {
	        var sel = window.getSelection();
	        sel.removeAllRanges();
	    }
	},	
	


	processEventMeta : function (n)
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
				
					
				try {		
					eventMeta = eventMeta == '{""}' ? {} : $.parseJSON(eventMeta);
				}
				catch (err) {
					alert('invalid eventMeta:' + eventMeta);
				}
	
	
				
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
							: base.defaultEventHandler;
						
							
							// wrap the function to track clicks - should extends this to other event
						if (key == '_click') {
							var clickWrappedFunction = function (e) {
								e.preventDefault();


									// do not allow multiple "user" clicks,  e.originalEvent detects from user verus manual "triggered" event
								if (executionTracker.isRunning() && e.originalEvent) {
									e.preventDefault();
									console.log("early exit");
									return false;
								}

									// register trackPoint - result of this call allows as to see if we need throttling..
								executionTracker.addTrackPoint("clickFunction");
								setTimeout(function() {
									try {
											// actual execution
										if (!FI.debugMode && window.history && window.history.replaceState) {
											window.history.replaceState("", "", "/");	
										}
										else {
											window.location.hash = 'home';
										}
										
										
										//console.log("invoke function");
										
										$.proxy(theFunction, e.target)(e);
										highlight.setAndRecord(e);
										
									} 
									catch (err) {
										console.log(err)
									}
									finally {
										executionTracker.removeTrackPoint("clickFunction");										
									}
									// if throttling - delay execution
								}, executionTracker.shouldThrottle ? 1000 : 0);
							}			
						}	

						$(this).bind(key.substring(1, key.length), eventMeta, clickWrappedFunction || theFunction);

						
							// once process remove it so we don't accidently process if this method is called repeatedly on the
							// same element
							// disable text select
						
						
							// for click we add cursor class
						if (key == '_click') {
							$(this).disableTextSelect();
							$(this).addClass('cursor');
						}
							
											
					}
				}
				$(this).removeAttr('_eventMeta');
			}
		)
	},
	
	defaultEventHandler : function (e) {
		base.ajax({}, e);	
	},
	

 	processMenuMeta: function (elements)
	{
		$("[_menuMeta]").each(
			function () {
				var menuMeta ='{"' +  $(this).attr('_menuMeta').replace(/:/g,'":"').replace(/;/g,'","') + '"}';
				menuMeta = menuMeta == '{""}' ? {} : $.parseJSON(menuMeta);			
				
				// add default menu class
				$(this).addClass('hoverMenu corner1 dropShadow1 bg3 opacity8 hide');
				
				
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
			}
		);
	},
	

	menuClicked: function  (event)
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
						300
					);
				} 
			);
	

			tagMenu.css('display', 'block');
			tagMenu.css('position', 'fixed');
			$(tagMenu).position({ my: "left top", at: "center bottom", of: n, offset: "-25px 0", collision: "fit fit"});
			
				// finally we display menu		
			//var position = $(n).offset();
			//
			//tagMenu.css('top', position.top);
			//tagMenu.css('left', position.left + 20);
			
	
			
		}
	
		event.stopImmediatePropagation();	
	}	
}


util = {
	thumbnailURL: function (url) {
		if (!url) {
			return null;
		}
		
		var token = "http://www.youtube.com/embed/";
		if (url.indexOf(token) == 0) {
			url = (url.split('&')[0]).split('?')[0];
			var id = url.substring(token.length);
			return "http://i2.ytimg.com/vi/" + id + "/hqdefault.jpg";
			
			//http://i2.ytimg.com/vi/f4vTqozhDWo/hqdefault.jpg
			//http://img.youtube.com/vi/f4vTqozhDWo/0.jpg
		}
		
		var token1 = "http://www.youtube.com/v/";
		if (url.indexOf(token1) == 0) {
			url = (url.split('&')[0]).split('?')[0];
			var id = url.substring(token1.length);
			return "http://i2.ytimg.com/vi/" + id + "/hqdefault.jpg";
			
			//http://i2.ytimg.com/vi/f4vTqozhDWo/hqdefault.jpg
			//http://img.youtube.com/vi/f4vTqozhDWo/0.jpg
		}
		
		
		var token2 = "//www.viddler.com/embed/";
		if (url.indexOf(token2) == 0) {
			var id2 = url.split('/?')[0].substring(token2.length);
						
			return "http://cdn-thumbs.viddler.com/thumbnail_2_" + id2 + ".jpg";
		}
		return false;
	},
	
	scrollbarWidth: function () {
	
	    var inner = document.createElement('p');  
	    inner.style.width = "100%";  
	    inner.style.height = "200px";  
	  
	    var outer = document.createElement('div');  
	    outer.style.position = "absolute";  
	    outer.style.top = "0px";  
	    outer.style.left = "0px";  
	    outer.style.visibility = "hidden";  
	    outer.style.width = "200px";  
	    outer.style.height = "150px";  
	    outer.style.overflow = "hidden";  
	    outer.appendChild (inner);  
	  
	    document.body.appendChild (outer);  
	    var w1 = inner.offsetWidth;  
	    outer.style.overflow = 'scroll';  
	    var w2 = inner.offsetWidth;  
	    if (w1 == w2) w2 = outer.clientWidth;  
	  
	    document.body.removeChild (outer);  
	  
	    return (w1 - w2);  

	},		
	
	replaceText : function (elems) {
		var elem;
		for ( var i = 0; elems[i]; i++ ) {
			elem = elems[i];
			if ( elem.nodeType === 3 || elem.nodeType === 4 && elem.nodeValue) { // Get the text from text nodes and CDATA nodes
				var a = elem.nodeValue; 
				elem.nodeValue = elem.nodeValue.replace(/&#60/g,"<").replace(/&#62;/g,">");
			} else if ( elem.nodeType !== 8 ) { // Traverse everything else, except comment nodes
				util.replaceText(elem.childNodes);
			}
		}
		return;		
	},
	
	
	firstText : function (elems, result) {
		var elem;
		for ( var i = 0; elems[i]; i++ ) {
			elem = elems[i];
			if ( elem.nodeType === 3 || elem.nodeType === 4 && elem.nodeValue) { // Get the text from text nodes and CDATA nodes
				if ($.trim(elem.nodeValue).length > 0) {
					result.push(elem);
					return;
				}
			} else if ( elem.nodeType !== 8 ) { // Traverse everything else, except comment nodes
				util.firstText( elem.childNodes, result);
				if (result.length > 0) {
					return;
				}
			}
		}
		return;
	},
	
	firstTextNode : function (elems) {
		result = [];
		util.firstText($(elems), result);
		return result[0];
	}
	
}

	
iframe = {

	checkPageBust: false, // check if we allow leaving page, while loading iframe we won't since it could be iframe bust, but after user can leave page
	link: null, // page to be loaded in iframe
	
		// setup, the iframe link , and if we wish to load immediately after init
	loadInit: function (link, loadImmediately) {
		iframe.link = link;
		$(".viewContentDetailCenter div.previewiFrameCover").removeClass('opacity0');
		$(".viewContentDetailCenter div.previewiFrameCoverHeader").css('display','block');			
		if (loadImmediately) {
			iframe.startLoading();
		}
	},
	
		// xxx, IE8 for some reason must have "background img" in order to block the events from iframe using cover
		// so here we use opacity0 to hide and unhide the imgLoader 
	startLoading: function () {
		var src = $(".viewContentDetailCenter iframe").attr('src');
		if (src != iframe.link) { // if not loaded yet
			iframe.checkPageBust = true;
			$(".viewContentDetailCenter iframe").attr('src', iframe.link);
			$(".viewContentDetailCenter div.previewiFrameCoverHeader h2").html("Loading Site...")
			$(".viewContentDetailCenter div.previewiFrameCoverHeader").fadeOut(2000);
			setTimeout(function() {
				iframe.checkPageBust = false;	// assume 5 sec already loads remote frame's js and any iframe bust are caught, there after user can leave the page
			}, 5000);
		}
	},
		
		// clean up basically
	stopLoading: function ()
	{
		iframe.minIframe();		
		$(".viewContentDetailCenter iframe").attr('src', '');
		iframe.loadCompleted();					
	},
	
		// iframe onLoad callback, allow frame bust, and remove loading gif
	loadCompleted: function (e)
	{
		var src = $(".viewContentDetailCenter iframe").attr('src');
		if (src && src.length > 0) {		
			iframe.checkPageBust = false;
			$(".viewContentDetailCenter div.previewiFrameCover").addClass('opacity0');
		}
	},

		// maximze the frame and remove the cover
	maxIframe: function (n)
	{
		var frame = $(".viewContentDetailCenter iframe");
		$(".viewContentDetailCenter div.previewiFrameCover").css('display','none');
		$(".viewContentDetailCenter .cmdBar").addClass('alt');
		$(".viewContentDetailCenter .cmdBar .three").removeClass('hide');		
		frame.removeClass('previewiFrame');
		frame.addClass('previewiFrameMax');
	},

		// minimize the frame to orginal, and cover back
	minIframe: function (n)
	{
		var frame = $(".viewContentDetailCenter iframe");
		$(".viewContentDetailCenter div.previewiFrameCover").css('display','block');
		$(".viewContentDetailCenter .cmdBar").removeClass('alt');
		$(".viewContentDetailCenter .cmdBar .three").addClass('hide');
		frame.removeClass('previewiFrameMax');
		frame.addClass('previewiFrame');
	},

	
	openContentIFrame: function (node, event, link) {
		
		BreakoutSiteName = "blah blah";
		var currentContent = scrollCurrent(event, node);
	
			// show iframe hide desc
		var iframe = $(currentContent).find('._contentIFrame');
		$(currentContent).find('._longDesc').addClass('forceHide');
	
			// calculate height for max display	
		var currentContentHeight = $('#ContentPanel').innerHeight();
		var iFrameToContentHeight = $(iframe).position().top - $(currentContent).position().top;
		iFrameToContentHeight = (currentContentHeight - iFrameToContentHeight); 
		
		
			// hide the scroll while loading - will show after site is loaded
		$('#ContentPanel').css('overflow-y','hidden');
		$(iframe).css('height', iFrameToContentHeight + "px").removeClass('hide');
	
		
		setTimeout (function () {
			clearIFrameLoader();
		}, 5000);
		
			// open iframe
		iFrameToContentHeight = iFrameToContentHeight + 17; // +17 to hide horiz bar
		var iframeHtml = '<div class="_clearIFrameLoader" style="position:absolute;width:100%;height:' + iFrameToContentHeight + 'px;background-image:url(2/ajax-loader4.gif);background-repeat:no-repeat;background-position: center center"></div>'	
		iframeHtml += '<div><IFRAME onload="clearIFrameLoader(this)" src="' + link + '" frameborder="0" WIDTH="100%"  height="'+ iFrameToContentHeight + '"/></div>';
		$(iframe).html(iframeHtml);
		
		
		return false;
	}, 

	openNewWindow: function ()
	{
		window.open(iframe.link, "_somewondi");
	},
	
	openContentWindow: function  (link) 
	{
			// should consider use jquery
		var winX = (document.all)?window.screenLeft:window.screenX;
		var winY = (document.all)?window.screenTop:window.screenY;
	
			// x offset,width is  2 96 2
			// y offset,height is 15 85 10
		
		var left = winX + $(window).width() * 0.03;
		var top =  winY +  $(window).height() * 0.15;
		//alert (" W " + $(window).height() + " H" + $(window).width() + " TOP ");  
		
		var newWindow = window.open(link, "_blank","scrollbars=yes,dependent=no,resizable=yes,toolbar=no,location=yes,menubar=yes,status=no,height=" + ($(window).height() * 0.80) + ",width=" + ($(window).width() * 0.94) + ", top=" + top +", left=" + left);
		
		
		if (navigator.userAgent.indexOf('Chrome/') > 0) {
			newWindow.blur();
			newWindow.focus();
		}	
	}
}	
	

QueryAhead = {
		textTable: null,//[['abc',1],['aaa',2],['efg',3],['eee',4],['axsdfbc',1],['asdfaa',2],['esdfsdffg',3],['esdfsdfee',4]],
		contextTable: null,
		display: null,
		input:null,
		focusedSearchEntryIdx: 0,
		focusedSearchEntry: null,
		matchedEntries: 0,
		matchedEntriesLimit: 50,
		hoverEnable: true,
		closeImage:null,
		
		init: function () {
			QueryAhead.input = $('#QueryAheadInput');
			QueryAhead.closeImage =  $('#QueryAheadInputClose');
			
			if (FI.browserInfo.msie) {
				QueryAhead.input.bind('keypress',QueryAhead.ieSpecialKeyCapture);
			}
			
			
			
			//QueryAhead.input.bind('focusout', function(e) {
			//	QueryAhead.close(e);
			//});
			
		},
		ieSpecialKeyCapture: function (e) {
			if (e.keyCode == 13) {
				return base.stopBubbling(e);
			}
		},
		
			// default click on input
	    clickActionOnOpenSearch : function (e)
	    {
		
			QueryAhead.closeImage.css('display','block');
			   

  

			function callback (dataArray, e, param) {
				var data = $.parseJSON(dataArray[0]);	
				QueryAhead.textTable = data[0];
				QueryAhead.contextTable = data[1];
					// pre process text into lowercase and split based on spaces
				var textTable = QueryAhead.textTable;
			    for (var i = 0, size = textTable.length; i < size; i++) {
		            textTable[i] = textTable[i].toLowerCase().split(" ");
		        } 								
			};			


	        if (QueryAhead.textTable == null) {
	            base.ajax({'action':'searchSiteDialogClicked'}, e, callback);
	        }
	        	        
			return base.stopBubbling(e);
	    },
	    
	    close: function (e, keepSearchTerm)
	    {
        	$('#QueryAheadDropDown').css('display','none');
  
    		
    		if (!keepSearchTerm) {
    			QueryAhead.input.val('');
   				QueryAhead.input[0].focus();
    		}
        	return base.stopBubbling(e.originalEvent);
	    },
	    
	    keyupActionOnAutoComplete: function (e)
	    {
	            // lazy init 
	        if (QueryAhead.display == null) {
	            QueryAhead.display = $("#QueryAheadDropDown .portletContent .queryEntries");
	        }


	            // keyboard events up
	        if (e.keyCode === 38) {
	            QueryAhead.hoverEnable = false;            
	            if (--QueryAhead.focusedSearchEntryIdx < 0) {
	                QueryAhead.focusedSearchEntryIdx = QueryAhead.queryEntrySize - 1;
	            }
	            //console.log('up' + QueryAhead.focusedSearchEntryIdx - 1);
	            $(QueryAhead.focusedSearchEntry).removeClass('queryEntryHighlight');
	            QueryAhead.focusedSearchEntry = QueryAhead.display.children().get(QueryAhead.focusedSearchEntryIdx - 1);
	            //$(QueryAhead.display).scrollTo(QueryAhead.focusedSearchEntry, {offset:-104});
	            $(QueryAhead.focusedSearchEntry).addClass('queryEntryHighlight');            
	            //$(e.currentTarget).val($(QueryAhead.focusedSearchEntry).text());
	            return;
	            //return;
	        }
	        	// down
	        if (e.keyCode === 40) {
	            QueryAhead.hoverEnable = false;            
	            QueryAhead.focusedSearchEntryIdx = ++QueryAhead.focusedSearchEntryIdx  %  QueryAhead.queryEntrySize;
	            //console.log('down' + QueryAhead.focusedSearchEntryIdx - 1);
	            $(QueryAhead.focusedSearchEntry).removeClass('queryEntryHighlight');
	            QueryAhead.focusedSearchEntry = QueryAhead.display.children().get(QueryAhead.focusedSearchEntryIdx - 1);
	            //$(QueryAhead.display).scrollTo(QueryAhead.focusedSearchEntry, {offset:-104});
	            $(QueryAhead.focusedSearchEntry).addClass('queryEntryHighlight');            
	            //$(e.currentTarget).val($(QueryAhead.focusedSearchEntry).text());
	            return;
	        }    
	        	//enter
	        if (e.keyCode === 13) {
	            if (QueryAhead.focusedSearchEntry != null) {
	                QueryAhead.clickActionOnSearchEntry(QueryAhead.focusedSearchEntry ,e);
	            }            
	            else if (QueryAhead.display.children().size() > 0) {
	                QueryAhead.mouseOver(QueryAhead.display.children().get(0), e);
	                QueryAhead.clickActionOnSearchEntry(QueryAhead.focusedSearchEntry ,e);
	            }
	            return base.stopBubbling(e.originalEvent);
	        }
	        
	        	// esc
	        if (e.keyCode === 27) {
	        	return QueryAhead.close(e);
	        }        
	        
	        
	        if (e.keyCode === 37 || e.keyCode === 39) {
	            return;
	        }
	        
	        
	        
	            // reset for each keystroke
	        QueryAhead.display.html("");
	        QueryAhead.focusedSearchEntryIdx = 0;
	        QueryAhead.queryEntrySize = 0;
	        var text = $(e.currentTarget).val();
	        if (text == "") {	        	
	        		// if empty clear scrollbar
				var testEvent = {currentTarget:$(QueryAhead.display).parent()};
				lightScrollBar.mouseIn(testEvent);
	            return;
	        }
	        else {
	            text = text.toLowerCase();
	        }
	        
	        
	            // attempt to find matches
	        QueryAhead.queryEntrySize = 0;
	        var matchedText = '<span class="bold colorBlack alignTop">' + text + '</span>';
	            // for each term
	        for (var i = 0; i < QueryAhead.textTable.length; i++) {
	        
	                // each entry could have space, split the entry by space and attempt to see if each word matches
	            var matchEntryText = "";
	            var matchFound = false;
	            var textArray = QueryAhead.textTable[i]; 
	            for (var j = 0; j < textArray.length; j++) {
	                if (j > 0) {
	                    matchEntryText = matchEntryText + " ";  // restore spacing
	                }
	                var indexOf = textArray[j].indexOf(text);
	                if (indexOf == 0) {         
	                    matchEntryText = matchEntryText + matchedText + textArray[j].substr(text.length, textArray[j].length - text.length);
	                    matchFound = true; 
	                }            
	                else {
	                    matchEntryText = matchEntryText + textArray[j];  // restore entry 
	                }
	            }
	            
	            if (matchFound) {
	                var entry = '<h6 ctxId = "' +  i + '" class="queryEntry hFit vFit txtOverflow color8 shadow1 line" onClick="QueryAhead.clickActionOnSearchEntry(this ,event)" onMouseOver="QueryAhead.mouseOver(this ,event)">' + matchEntryText + '</h6>';
	                QueryAhead.display.append(entry);
	                QueryAhead.queryEntrySize++;
	            }

	            if (QueryAhead.queryEntrySize >= QueryAhead.matchedEntriesLimit) {
	                break;
	            }
	        }
	        
	        if (QueryAhead.queryEntrySize > 0) {
				$('#QueryAheadDropDown').css('display','block');
	        }

				// update scrollbar
			var testEvent = {currentTarget:$(QueryAhead.display).parent()};
			lightScrollBar.mouseIn(testEvent);
	        
	        return base.stopBubbling(e.originalEvent);
	    },
	    
	    clickActionOnSearchEntry: function (node, event)
	    {
	    	var text = $(node).text();
	    	QueryAhead.input.val(text);
	    	var ctxId = $(node).attr('ctxId');	    
	    	action.getContentForSource({sources:[ctxId],target:node});
	    	
	        
	        if (QueryAhead.focusedSearchEntry != null) {
                $(QueryAhead.focusedSearchEntry).removeClass('queryEntryHighlight');
                QueryAhead.focusedSearchEntry = null;
                QueryAhead.hoverEnable = true;
            }            
	    	
	    	
	    	return QueryAhead.close(event, true);
	    },
	    
	    getContextId: function (index)
	    {
	        
	    },
	    
	    mouseOver: function (node, event)
	    {
	        if (QueryAhead.hoverEnable) {
	            $(QueryAhead.focusedSearchEntry).removeClass('queryEntryHighlight');
	            QueryAhead.focusedSearchEntry = node;
	            $(QueryAhead.focusedSearchEntry).addClass('queryEntryHighlight');            
	            //$(e.currentTarget).val($(QueryAhead.focusedSearchEntry).text());
	            QueryAhead.focusedSearchEntryIdx = QueryAhead.display.children().index(QueryAhead.focusedSearchEntry) + 1;
	        }
	        QueryAhead.hoverEnable = true;
	        return base.stopBubbling(event);        
	    }
	}


	

/**
*
*  Base64 encode / decode
*  http://www.webtoolkit.info/
*
**/
 
Base64 = {
 
	// private property
	_keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
 
	// public method for encoding
	encode : function (input) {
		var output = "";
		var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
		var i = 0;
 
		input = Base64._utf8_encode(input);
 
		while (i < input.length) {
 
			chr1 = input.charCodeAt(i++);
			chr2 = input.charCodeAt(i++);
			chr3 = input.charCodeAt(i++);
 
			enc1 = chr1 >> 2;
			enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
			enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
			enc4 = chr3 & 63;
 
			if (isNaN(chr2)) {
				enc3 = enc4 = 64;
			} else if (isNaN(chr3)) {
				enc4 = 64;
			}
 
			output = output +
			this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
			this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);
 
		}
 
		return output;
	},
 
	// public method for decoding
	decode : function (input) {
		var output = "";
		var chr1, chr2, chr3;
		var enc1, enc2, enc3, enc4;
		var i = 0;
 
		input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
 
		while (i < input.length) {
 
			enc1 = this._keyStr.indexOf(input.charAt(i++));
			enc2 = this._keyStr.indexOf(input.charAt(i++));
			enc3 = this._keyStr.indexOf(input.charAt(i++));
			enc4 = this._keyStr.indexOf(input.charAt(i++));
 
			chr1 = (enc1 << 2) | (enc2 >> 4);
			chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
			chr3 = ((enc3 & 3) << 6) | enc4;
 
			output = output + String.fromCharCode(chr1);
 
			if (enc3 != 64) {
				output = output + String.fromCharCode(chr2);
			}
			if (enc4 != 64) {
				output = output + String.fromCharCode(chr3);
			}
 
		}
 
		output = Base64._utf8_decode(output);
 
		return output;
 
	},
 
	// private method for UTF-8 encoding
	_utf8_encode : function (string) {
		string = string.replace(/\r\n/g,"\n");
		var utftext = "";
 
		for (var n = 0; n < string.length; n++) {
 
			var c = string.charCodeAt(n);
 
			if (c < 128) {
				utftext += String.fromCharCode(c);
			}
			else if((c > 127) && (c < 2048)) {
				utftext += String.fromCharCode((c >> 6) | 192);
				utftext += String.fromCharCode((c & 63) | 128);
			}
			else {
				utftext += String.fromCharCode((c >> 12) | 224);
				utftext += String.fromCharCode(((c >> 6) & 63) | 128);
				utftext += String.fromCharCode((c & 63) | 128);
			}
 
		}
 
		return utftext;
	},
 
	// private method for UTF-8 decoding
	_utf8_decode : function (utftext) {
		var string = "";
		var i = 0;
		var c = c1 = c2 = 0;
 
		while ( i < utftext.length ) {
 
			c = utftext.charCodeAt(i);
 
			if (c < 128) {
				string += String.fromCharCode(c);
				i++;
			}
			else if((c > 191) && (c < 224)) {
				c2 = utftext.charCodeAt(i+1);
				string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
				i += 2;
			}
			else {
				c2 = utftext.charCodeAt(i+1);
				c3 = utftext.charCodeAt(i+2);
				string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
				i += 3;
			}
 
		}
 
		return string;
	}
 
}	

portlet = {
	showDropDown: function (e) {

		var portlet = $(e.data.portletId);
		var menuHideTimer = null;
		var menuOnFocus = portlet.attr("_menuOnFocus");	
		var init = portlet.attr("_init");
		portlet.css('position', 'absolute');		
	
		if (!init) {
			
			portlet.attr("_init",true);
			
				// if mouse is on menu dropdown, don't hide
			portlet.bind(
				"click",
				function(){
					portlet.addClass('hide');	
					portlet.removeAttr("_menuOnFocus");
				}	
			);
	
	
			
				// if mouse is on menu dropdown, don't hide
			portlet.bind(
				"mouseenter",
				function(){
					if (menuHideTimer) {
						clearTimeout(menuHideTimer);
					}
				}	
			);
	

				// if mouse is not on menu dropdown, hide after 100ms		
			portlet.bind(
				"mouseleave",
				function(){
					menuStartHiding = true;
					menuHideTimer = setTimeout(
						function() {
							portlet.addClass('hide');	
					 	 	portlet.removeAttr("_menuOnFocus");
						},
						300
					);
				} 
			);
		}
	
			// if menu is currently on focus which is shown, then we toggle and hide it
		if (menuOnFocus) {
			portlet.removeAttr("_menuOnFocus");
			portlet.addClass('hide');		
		}
			// else display menu
		else {
			portlet.attr("_menuOnFocus", 'true');
			portlet.removeClass('hide');
			portlet.position({ my: "left top", at: "left bottom", of: e.currentTarget, offset: "0 0", collision: "fit fit"});	
			
		
			
				// finally we display menu		
			//var position = $(n).offset();
			//
			//tagMenu.css('top', position.top);
			//tagMenu.css('left', position.left + 20);
			
	
			
		}
	
		return base.stopBubbling(e);
	
	}
	
	
}

footer = {
	show: function (e)
	{
		var footerName = e.data.name;
		window.location.replace(FI.http + FI.hostName + "/0/6/ContentViewHTML/" + footerName);
	} 
}

mail = {
	send: function (e){
		
		/* BUG if subject has &amp; then it would fail  ie  blah blah & blah*/ 
		var recordId = contentDetail.currentContent.data('store').rid;
		var link = FI.http + FI.domainName + "/0/6/ContentViewHTML/" + recordId;
		var errorMsg = "here here here is the error error error error";
		var subject = $(".viewContentDetailCenter ._entryTitle").text();
		var body_message = "More content in: " + link;
		var mailto_link = 'mailto:'+ "" +'?subject='+subject+'&body='+body_message;
		win = window.open(mailto_link,'emailWindow');
		if (win && win.open &&!win.closed) win.close();
		}
}

google = {
	
	lookupImage: function (text, imgContainer)
	{
							
		var data = {q:text,v:'1.0',imgsz:'medium|large','rsz':4};	
		$.ajax({
			  async: true,
			  type: 'GET',
			  data: data,
			  context: imgContainer,
			  url: 'http://ajax.googleapis.com/ajax/services/search/images',
			  dataType :'jsonp',		  
			  success : function (json, textStatus, XMLHttpRequest) {
	         	if( json.responseStatus == 200) { 
	         		for (var key in json.responseData.results) {
	         			var result = json.responseData.results[key];
	         			var imgSrc = unescape(result.url);
	         			
	         			//console.log(result.url + ' : ' + imgSrc);
	         			
		         		var imgObj = {'src':imgSrc,'height':result.height,'width':result.width,'imgContainer':imgContainer};
		         		var resultImage = image.imageSizeFilter(imgObj);
		         		if (resultImage) {
		         			
   							var testImg = new Image();
							testImg.onload = function () {
								resultImage.img = $(resultImage.img).css('display','none');
				         		$(imgContainer).html(resultImage.img);
				         		$(imgContainer).find('img').fadeIn(300);
				         		// console.log('putting in');
							};
							testImg.src = imgSrc;
			         		return;
			         	}
			         	//console.log('tried one more...!');
	         		}
        		
	         	}		  	
	    	    
			  }		  
		});						
	}	
}

	
$(function(){
    $.extend($.fn.disableTextSelect = function() {
        return this.each(function(){
            if(FI.browserInfo.firefox){//Firefox
                $(this).css('MozUserSelect','none');
            }else if(FI.browserInfo.msie){//IE
                $(this).bind('selectstart',function(){return false;});
            }else{//Opera, etc.
                $(this).mousedown(function(){return false;});
            }
        });
    });
    //$('.noSelect').disableTextSelect();//No text selection on elements with a class of 'noSelect'
});
	
	
