

var init;
var ContentPanelTopInnerHeight;

var BlockDataTable = new Array();
var HtmlSegmentsTable = new Array();
var PostActionCallback = {};
var BreakoutSiteName = null;

	// client side browse setting
var isImagePreviewOn = true;
var isFaviconOn = true;
var isHideReadOn;
var isExpandOn;





function scrollUp (e)
{
	stopBubbling(e);	
	$('#ContentPanel').scrollTo( 0, 1000);
}

function scrollDown (e)
{
	stopBubbling(e);	
	var bottomTarget = $('#ContentPanel div.contentViewRSSList').last().parent();
	$('#ContentPanel').scrollTo( bottomTarget, 1000);
}


function scrollCurrent (e, n)
{
	stopBubbling(e);
	var currentContent = $(n).closest(".contentViewRSSList");
	$('#ContentPanel').scrollTo(currentContent, 1000);
	return currentContent;
}

function scrollNext (e, n)
{

		// find focused content		
	var currentContent = $(n).closest(".contentViewRSSList");
	var contentList = $('#ContentPanel div.contentViewRSSList');
	var index = $(contentList).index(currentContent);

		// get next content	
	var index = $(contentList).index(currentContent);
	index = $(contentList).size() == index + 1 ? index : index + 1; 
	var nextContent = $(contentList).get(index);
	var nextContentHeader = $(nextContent).find('.contentViewRSSListHeader');

	
	var scrollToNext = function () {
		setTimeout(function () {			
			var time = $(currentContent).height() * 1.5;
			time = time > 1200 ? 1200 : time < 700 ? 700 : time; 
			$('#ContentPanel').scrollTo($(nextContent), time);
			}, 200)
		
	}
		 		
	toggleDisplay($(nextContentHeader), true, true, false, scrollToNext);
	stopBubbling(e);	
}


function scrollPrevious (e, n)
{
		// find focused content		
	var currentContent = $(n).closest(".contentViewRSSList");
	var contentList = $('#ContentPanel div.contentViewRSSList');
	var index = $(contentList).index(currentContent);

		// get previous content	
	var index = $(contentList).index(currentContent);
	index = index == 0 ? 0 : index - 1; 
	var previousContent = $(contentList).get(index);
	var previousContentHeader = $(previousContent).find('.contentViewRSSListHeader');


	var time = $(currentContent).height() * 1.5;
	time = time > 1200 ? 1200 : time < 700 ? 700 : time; 
	$('#ContentPanel').scrollTo($(previousContent), time);
	
	
	setTimeout(function () {
		toggleDisplay($(previousContentHeader), false, true, false);	
		
	}, 800)
		
	stopBubbling(e);		
}


function likeContent (e, n, recordId)
{
	$(n).fadeOut(1000);
	stopBubbling(e);
	var e = fakeEvent();
	ajaxRefresh (e, recordId, null, 'likeRssContent');

	//alert(recordId);
}

function toggleImagePreview (e)
{
	isImagePreviewOn = !isImagePreviewOn;
	$(this).find('span').html(isImagePreviewOn ? e.data.hideTxt : e.data.showTxt);
	loadPreviewImageBlock()
}


function toggleFavicon (e)
{
	isFaviconOn = !isFaviconOn;
	$(this).find('span').html(isFaviconOn ? e.data.hideTxt : e.data.showTxt);	

	$("#ContentPanel .contentViewRSSList").each(function(i) {	
		setFaviconOriginalSrc($(this));
	});	
}


function setFaviconOriginalSrc (folder)
{
	if (isFaviconOn) {
		$(folder).find("._favIconDownload").each(function(i) {
			var currentUrl = $(this).attr("src");
			var url = $(this).attr("_truesrc");
			if (url && url != currentUrl) {
				url = url.toString() == "" ? "2/rss-icon.png" : url
				$(this).attr("src", url);
			}						
		})				
	}	
}


function toggleExpandView (e)
{
	isExpandOn = !isExpandOn;	
	$(".contentViewRSSListHeader").each( 
		function(){
		    toggleDisplay(this, true, isExpandOn, true);
		}
	);		
	$(this).find('span').html(isExpandOn ? e.data.colTxt : e.data.expTxt);
}


function resetExpandView ()
{
		// undefined is only true for the first time
	if (isExpandOn != undefined) { 
		isExpandOn = false;
		var control = $('#ExpandCollapseControl');
		$(control).find('span').html("Expand");
	}	
}


function toggleHideRead (e)
{
	isHideReadOn = !isHideReadOn;	
	$("#ContentPanel .contentViewRSSList").each(function(i) {	
		var key = $(this).attr('href');
		var value = localStorage.getItem(key); 
		if (value && value == 'T' ) {
			$(this).parent().toggleClass('forceHide');
		}
	});		
	$(this).find('span').html(isHideReadOn ? e.data.showTxt : e.data.hideTxt);
}


function resetHideRead ()
{
		// undefined is only true for the first time
	if (isHideReadOn != undefined) { 
		isHideReadOn = false;
		var control = $('#HideReadControl');
		$(control).find('span').html("Hide Read");
	}	
}



function setupKeepLogin (keepLoginInfo)
{
	for (property in keepLoginInfo) {
		localStorage.setItem("keepLoginInfo." + property, keepLoginInfo[property]);	 
	}
}

function testLogin (e)
{
	var arg = '{"loginDialog_email":"jackwang65@yahoo.com","loginDialog_password":"welcome1a"}';
	ajaxRefresh (e, arg);	
}



function onEnterPressed (e)
{
   if (e.keyCode == '13') {
       e.preventDefault();
   	   $(e.data.arg).trigger('click');
   }
}






function createAccountClicked (e)
{
	var remoteServerHandlerName = e.data["_" + e.type];
	var url = "http://" + hostName + "/5/" + sessionId + "/" + uniqueClientToken;
	$('#' + e.data.dialogMeta).dialog('close');
}


function reloadContentClicked (e)
{ 
	
	
	var isTag = $('#' + e.data.blockId).attr('_isTag') == 'true';
	var arg = (isTag ? 'F' : 'I') + "/" +  e.data.blockId + "/" +  $('#' + e.data.blockId).attr('_recordId');
	ajaxRefresh(e, arg);			
}

function subscribeClicked (e)
{ 
	var isTag = $('#' + e.data.blockId).attr('_isTag') == 'true';
	var arg = (isTag ? 'F' : 'I') + "/" +  e.data.blockId;
	ajaxRefresh(e, arg , null, 'subscribeClicked');	
}



function dateClicked (e)
{
	ActionHighlight.highlightClickedFolderAction(e);	
	var range = parseInt(e.data.range);		
	var end = new Date();	
	// 23 insteand of 24, as it would cause addtional "day" to show up
	
	var duration = 0;
	if (range == 0) {
		duration = (1000 * 60 * 60);
	}
	else if (range == 1) {
		duration = (1000 * 60 * 60 * 24); // - (60 * 1000);
	}
	else {
		duration = (range * 1000 * 60 * 60 * 24); // - (60 * 1000);
	}
	
	var start = new Date(end.getTime() - duration); 
		//e.data.arg = dateStr(start, true) + "/" + dateStr(end, true);  XXX thes format for lucene like dates ie 2010051114
		// since we switch to use cache - we now just use raw time 
	e.data.arg = start.getTime() + "/" + end.getTime();
	ajaxRefresh(e);			
}





function addSiteClicked (e)
{
	ajaxRefresh(e, null, null, null, 'addSiteClicked');	 
}











function Class_CategoryTabManager ()
{
	var that = this;
	that.lastRefreshableAction = null;
	
	this.switchFolderClicked = function (e)
	{
		e._clientAjaxCallback = function () {
			RefreshManager.clearLastRefreshableAction();
			ActionHighlight.clearFolderHighlight();
			ActionHighlight.clearTabHighlight();
			ActionHighlight.highlightCategoryFolderAction(e);
		}	
		
		e._serverCallbackContext = $(e.data.menuMeta);
		ajaxRefresh(e, null, null, 'switchFolderClicked');
	
	}    
};
var CategoryTabManager = new Class_CategoryTabManager();





function getBlockData (o)
{
	if (typeof o == 'string') {
		var blockData = BlockDataTable[o];
		if (!blockData) {
			var blockData = eval($("#" + o).attr("_blockData"));
			BlockDataTable[o] = blockData;
		}
		return blockData;
	}
	else {
		var id = $(o).attr('id');
		if (id) {
			return getBlockData(id)
		}
		return null;
	}
}

 
	//XXX refactor to combine the tagLoadCallback and tagLoadCallback2
	// this one used for tagOpenClose for both public and personl tag

function tagLoadCallback (data, node, tagDivId)
{
		var html = applyBindingTo(HtmlSegmentsTable['SearchTagsDivId'], data);
		n = $("#" + tagDivId).children().first(); // "AbstractHoverMenuMini"
		n.append(html);
		processEventMeta($(n).find('.AbstractHoverMenuMini').siblings());	
		
		
}

	// this call back is used when personalFolder is clicked and we replace use personal tags
	// should probably just hide public tags
function tagLoadCallback2 (data, n)
{
		var html = applyBindingTo(HtmlSegmentsTable['SearchTagsDivId'], data);		
		var SearchTagsDiv = $('#SearchTagsDivId'); 		
		SearchTagsDiv.html(html);
		processEventMeta(SearchTagsDiv);	
		toggleTagDisplayInit(SearchTagsDiv);
}


function toggleTagDisplayInit (n)
{
	toggleTagDisplay($(n).children());
}

function toggleTagDisplay (n)
{
	$(n).each (
		function (index) {
			var display = $(this).css('display');
			$(this).css('display', (display == 'block') ? 'none' : 'block');
		}
	);
}





function tagOpenCloseClicked (e)
{
		// load tags if not loaded
	var folder = $('#' + e.data.blockId);	
		
	var isLoaded = folder.attr("_loaded") == 'true';


	e._clientAjaxCallback = function () {

			// toggle show and hide
		toggleTagDisplay($(e.currentTarget).parent().parent().siblings());
		
			// toggle arrow image	
		$(e.currentTarget).children().first().toggleClass('ui-icon-triangle-1-s ui-icon-triangle-1-e');	
	
	
		setFaviconOriginalSrc(folder);
	
		folder.attr("_loaded", 'true');
	}

	if (!isLoaded) {
		var arg = folder.attr("id");
		e._serverCallbackContext = folder;
		ajaxRefresh(e, arg , null, 'tagOpenCloseClicked');	
	}
	else {
		e._clientAjaxCallback();
	}

	
}

function deleteTagClicked (e)
{
	var id = e.data.blockId;
	ajaxRefresh(e, id);
	$('#LeftNav  #' + e.data.blockId).remove();
}

function renameTagClicked (e)
{
	
		// setup callback when form is submitted (OK clicked). the arg is the div id of the tag as setup below
		// the inputValues were captured during the formSubmit and passed on during the callback
		// so  that rename can be done when server ajax call returns 	
	var callback = function (arg, event, inputValues, validationResult) {
			// escape twice in the formsubmitted
		var value = unescape(unescape(inputValues));
		var path = '#LeftNav #'  + arg + ' div._tagName';
		$('#LeftNav #'  + arg + ' div._tagName').first().text(value);  // NOTE FIREBOX BUG inserts a weird <a xmlns="www.wrwer> ... </a> shit
	};
	setupPostFormSubmitAction(callback, e.data.blockId);
	
		// pass on the record to the form, so that when submit it gets passed to the server	
	e.data.arg = e.data.blockId;

		// setup the currentName in the form
	var currentName = $("#" + e.data.blockId + " div._tagName").html();		
	$(e.data.dialogMeta).find("#tagRenameDialog_newName").val(currentName);	
	
		// now everything is read open form
	formOpenClicked(e);	
}


function tagSiteClicked (e)
{
	
		// pass on the record to the form, so that when submit it gets passed to the server	
	e.data.arg = e.data.blockId;


		// now everything is read open form
	formOpenClicked(e);	
}


function Class_SearchDialog (init)
{
	var that = this;		
	that.varName = null;
	 	
	that.ajaxData = null;
	that.autoCompleteResults = null;
	that.focusedSearchEntryIdx = 0;
	that.focusedSearchEntry = null;
	that.searchEntrySize = 0;
	that.searchEntrySizeLimit = 100;
	that.hoverEnable = true;
	
	
		// clicked to open search dialog
	this.clickActionOnOpenSearch = function (e)
	{
		
		e._clientAjaxCallback = function () {
			if (that.focusedSearchEntry != null) {
				$(that.focusedSearchEntry).removeClass('searchEntryHighlight');
				that.focusedSearchEntry = null;
				that.hoverEnable = true;
			}			
			formOpenClicked(e);	
		};		
		
			
		if (that.ajaxData == null) {
			ajaxRefresh(e);
		}
		else {
			e._clientAjaxCallback(e);
		}
	};

		// call back upon server returned with ajax search data
	this.serverAjaxCallback = function (data) 
	{
		that.ajaxData = data;
	};
	
	
		// calls for each key on search term input
	this.keyupActionOnAutoComplete = function (e)
	{
			// lazy init 
		if (that.autoCompleteResults == null) {
			that.autoCompleteResults = $("#" + that.varName + "_autoComplete_results");
		}

			// keyboard events
		if (e.keyCode == '38') {
			that.hoverEnable = false;			
			if (--that.focusedSearchEntryIdx < 0) {
				that.focusedSearchEntryIdx = that.searchEntrySize - 1;
			}
			//console.log('up' + that.focusedSearchEntryIdx - 1);
			$(that.focusedSearchEntry).removeClass('searchEntryHighlight');
			that.focusedSearchEntry = that.autoCompleteResults.children().get(that.focusedSearchEntryIdx - 1);
			$(that.autoCompleteResults).scrollTo(that.focusedSearchEntry, {offset:-104});
			$(that.focusedSearchEntry).addClass('searchEntryHighlight');			
			//$(e.currentTarget).val($(that.focusedSearchEntry).text());
			return stopBubbling(e.originalEvent);;
		}
		if (e.keyCode == '40') {
			that.hoverEnable = false;			
			that.focusedSearchEntryIdx = ++that.focusedSearchEntryIdx  %  that.searchEntrySize;
			//console.log('down' + that.focusedSearchEntryIdx - 1);
			$(that.focusedSearchEntry).removeClass('searchEntryHighlight');
			that.focusedSearchEntry = that.autoCompleteResults.children().get(that.focusedSearchEntryIdx - 1);
			$(that.autoCompleteResults).scrollTo(that.focusedSearchEntry, {offset:-104});
			$(that.focusedSearchEntry).addClass('searchEntryHighlight');			
			//$(e.currentTarget).val($(that.focusedSearchEntry).text());
			return stopBubbling(e.originalEvent);
		}	
		if (e.keyCode == '13') {
			if (that.focusedSearchEntry != null) {
				that.clickActionOnSearchEntry(that.focusedSearchEntry ,e);
			}			
			else if (that.autoCompleteResults.children().size() == 1) {
				that.mouseOver(that.autoCompleteResults.children().get(0), e);
				that.clickActionOnSearchEntry(that.focusedSearchEntry ,e);
			}
			return stopBubbling(e.originalEvent);
		}
		if (e.keyCode == '37' || e.keyCode == '39') {
			return;
		}		
		
				
				

			// reset for each keystroke
		that.autoCompleteResults.html("");
		that.focusedSearchEntryIdx = 0;
		that.searchEntrySize = 0;
		var text = $(e.currentTarget).val();
		if (text == "") {
			return stopBubbling(e.originalEvent);
		}
		else {
			text = text.toLowerCase();
		}
		
		
			// attempt to find matches
		that.searchEntrySize = 0;
		var matchedText = '<span style="FONT-WEIGHT: bold">' + text + '</span>';
			// for each term
		for (var i = 0; i < that.ajaxData.length; i = ++i + that.arrayContextoffset) { // note that every 2 item in the array is the name, [name,id,name,id...]
		
				// each entry could have space, split the entry by space and attempt to see if each word matches
			var matchEntryText = "";
			var matchFound = false;
			var textArray = that.ajaxData[i].toLowerCase().split(" "); 
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
				var entry = '<div class="searchEntry textHeight clear textOverflow3 size12" _contextId="'+ that.getContextId(i) +'" onclick="' +  that.varName +'.clickActionOnSearchEntry(this ,event)" onMouseOver="'+ that.varName +'.mouseOver(this ,event)">' + matchEntryText + '</div>';
				that.autoCompleteResults.append(entry);
				that.searchEntrySize++;
			}

				// test of the limits - comment out for now
			//if (that.searchEntrySize > that.searchEntrySizeLimit) {
			//	break;
			//}


		}
		return stopBubbling(e.originalEvent);
	};
	
		// trigger when entry is selected 
	this.clickActionOnSearchEntry = function (node, event)
	{
		// subclass override
		
	};
	
		// 
	this.getContextId = function (index)
	{
		
	}
	

	this.mouseOver = function (node, event)
	{
		if (that.hoverEnable) {
			$(that.focusedSearchEntry).removeClass('searchEntryHighlight');
			that.focusedSearchEntry = node;
			$(that.focusedSearchEntry).addClass('searchEntryHighlight');			
			//$(e.currentTarget).val($(that.focusedSearchEntry).text());
			that.focusedSearchEntryIdx = that.autoCompleteResults.children().index(that.focusedSearchEntry) + 1;
		}
		that.hoverEnable = true;
		return stopBubbling(event);		
	};	


	init(this);
		
}



var searchSiteDialog = new Class_SearchDialog(
	function (that) {
	
		that.arrayContextoffset = 0;		
		that.varName = "searchSiteDialog";
		
			// trigger when entry is selected 
		that.clickActionOnSearchEntry = function (node, event)
		{
			var e = fakeEvent();
			e._clientAjaxCallback = function () {
				formCancelClicked(e);
			};				
			
			e.data.dialogMeta = "#searchSiteDialog";
			var contextId =  $(node).attr('_contextId');		
			ajaxRefresh(e, contextId, null, 'tagClicked');
			return stopBubbling(event);
			
		};			
		
		that.getContextId = function (index)
		{
			return index;
		};
			
	}
);


var searchFolderDialog = new Class_SearchDialog(
	function (that) {	
		
		that.arrayContextoffset = 1;
		that.varName = "searchFolderDialog";
		
			// trigger when entry is selected 
		that.clickActionOnSearchEntry = function (node, event)
		{
			var e = fakeEvent();
			e.data.blockId =  $(node).attr('_contextId');
			e.data.dialogMeta = "#searchFolderDialog";
			tagClicked(e);
			formCancelClicked(e);
			return stopBubbling(event);	
		};
		
		that.getContextId = function (index)
		{
			return that.ajaxData[index + 1];
		};
				
	}
);


var searchTitleDialog = new Class_SearchDialog(
	function (that) {	
		
		that.arrayContextoffset = 0;
		that.varName = "searchTitleDialog";
		
			// trigger when entry is selected 
		that.clickActionOnSearchEntry = function (node, event)
		{
			var e = fakeEvent();
			e.data.blockId =  $(node).attr('_contextId');
			var title = $(node).text();
			e.data.dialogMeta = "#searchTitleDialog";
			ajaxRefresh(e, title, null, 'searchTitleClicked');
			formCancelClicked(e);
			return stopBubbling(event);	
		};
		
		that.getContextId = function (index)
		{
			return that.ajaxData[index + 1];
		};
	}
);


var searchContentDialog = new Class_SearchDialog(
	function (that) {	
		
		that.arrayContextoffset = 0;
		that.varName = "searchContentDialog";
		
			// trigger when entry is selected 
		that.clickActionOnSearchEntry = function (node, event)
		{
			var e = fakeEvent();
			e.data.blockId =  $(node).attr('_contextId');
			var content = $(node).text();
			e.data.dialogMeta = "#searchContentDialog";
			ajaxRefresh(e, content, null, 'searchContentClicked');
			formCancelClicked(e);
			return stopBubbling(event);	
		};
		
		that.getContextId = function (index)
		{
			return that.ajaxData[index + 1];
		};
	}
);




function addFolderClicked (e)
{
	formOpenClicked(e);	
}


function detailTagClicked (e)
{
	var folder = $('#' + e.data.blockId);		
	e.data.arg = folder.attr("_recordId");

	
	
	e._clientAjaxCallback = function () {
	
			// replace empty info with Not Available 
		$(e.data.dialogMeta).find("div.inputText").each(
			function() {
				var textValue = $.trim($(this).html()); 
				if (textValue == '') {
					$(this).html("Not Available");
				}
			});
		
		
			// image load tester 
		$(e.data.dialogMeta).find("img").each(function() {
			var img = $(this); 
			var url = $(this).attr("_truesrc");
			if (url) {
				var myImage = new Image();
				myImage.onload = function () { 
					img.attr('src', url);
					img.css('display', 'block');
					img.toggleClass('imgLoader');
				};
				myImage.src = url;
				
			}
			else {
				$(this).parent().html("Not Available");
			}
		});	
		
		formOpenClicked(e);	
	
	};
	

	ajaxRefresh(e);	
}

ActionHighlight = {
	
	highlightText : null,
	highlightFolderBg : null,
	highlightTabBg : null,
	
	clearFolderHighlight : function ()
	{		
		if (ActionHighlight.highlightFolderBg) {
			$(ActionHighlight.highlightFolderBg).css('background-color','');
			$(ActionHighlight.highlightText).css('font-weight','normal');			
		}
	},
	
	clearTabHighlight : function ()
	{		
		if (ActionHighlight.highlightTabBg) {
			$(ActionHighlight.highlightTabBg).css('background-color','');
			$(ActionHighlight.highlightTabBg).css('font-weight','normal');			
		}	
	},
	
	highlightClickedFolderAction  :  function (e)
	{
		if (e.currentTarget != undefined) {
			var bg = $(e.currentTarget).closest('.AbstractHoverMenuMini');
			var text = $(e.currentTarget).find('._tagName')
			ActionHighlight.clearFolderHighlight();
			ActionHighlight.highlightFolderBg = bg;
			$(ActionHighlight.highlightFolderBg).css('background-color','#e4f1fb');
			ActionHighlight.highlightText = text;
			$(ActionHighlight.highlightText).css('font-weight','bold');			
		}			
	}, 
	
	highlightCategoryFolderAction : function (e)
	{
		if (e.currentTarget != undefined) {
			var bg = $(e.currentTarget);
			ActionHighlight.clearTabHighlight();
			ActionHighlight.highlightTabBg = bg;
			$(ActionHighlight.highlightTabBg).css('background-color','#e4f1fb');
			$(ActionHighlight.highlightTabBg).css('font-weight','bold');			
		}						
	},
	
	highlightCategoryFolderByName : function (tabName)
	{
		var e = {};
		e.currentTarget = $('#CategoryTabDivId #' + tabName); 
		ActionHighlight.highlightCategoryFolderAction(e);
	}	
	
}




/*
function Class_ActionHighlight ()
{
	that = this;
	that.highlightText;
	that.highlightFolderBg;
	that.highlightTabBg;
	
	that.clearFolderHighlight = function ()
	{		
		a.c();
		if (that.highlightFolderBg) {
			$(that.highlightFolderBg).css('background-color','');
			$(that.highlightText).css('font-weight','normal');			
		}
	}
	
	that.clearTabHighlight = function ()
	{		
		if (that.highlightTabBg) {
			$(that.highlightTabBg).css('background-color','');
			$(that.highlightTabBg).css('font-weight','normal');			
		}	
	}	
	
	that.highlightClickedFolderAction  = function (e)
	{
		if (e.currentTarget != undefined) {
			var bg = $(e.currentTarget).closest('.AbstractHoverMenuMini');
			var text = $(e.currentTarget).find('._tagName')
			that.clearFolderHighlight();
			that.highlightFolderBg = bg;
			$(that.highlightFolderBg).css('background-color','#e4f1fb');
			that.highlightText = text;
			$(that.highlightText).css('font-weight','bold');			
		}			
	} 
	
	that.highlightCategoryFolderAction = function (e)
	{
		if (e.currentTarget != undefined) {
			var bg = $(e.currentTarget);
			that.clearTabHighlight();
			that.highlightTabBg = bg;
			$(that.highlightTabBg).css('background-color','#e4f1fb');
			$(that.highlightTabBg).css('font-weight','bold');			
		}						
	}	
	
	that.highlightCategoryFolderByName = function (tabName)
	{
		var e = {};
		e.currentTarget = $('#CategoryTabDivId #' + tabName); 
		ActionHighlight.highlightCategoryFolderAction(e);
	}
}
var ActionHighlight = new Class_ActionHighlight();


*/


function popularSiteClicked (e)
{
	ActionHighlight.highlightClickedFolderAction(e);
	ajaxRefresh(e);
}

function tagClicked (e)
{
	ActionHighlight.highlightClickedFolderAction(e); 
	var folder = $('#' + e.data.blockId);		
	var recordId = folder.attr("_recordId");
	var isTag = folder.attr("_isTag") == 'true';
	var isLoaded = folder.attr("_loaded") == 'true';
	var ids = "";	


	e._clientAjaxCallback = function () {		
		if (isTag) {						
			$("#" + e.data.blockId).find("[_recordId]").each(
				function (i) {
					var id = $(this).attr("_recordId"); 
					ids = ids + id + "/" ;
				}
			)
			ids = ids.substring(0, ids.length - 1);
		}
		else {
			ids = ids + recordId;		
		}
		
			// most clear otherwise it gets called twice. see notes at ajaxRefresh3.clientAjaxCallback
		e._clientAjaxCallback = null;
		ajaxRefresh(e, ids, null, 'tagClicked');			
	}


	if (isTag && !isLoaded) {
		var arg = e.data.blockId;		
		e._serverCallbackContext = $(e.currentTarget).parent().parent().parent();
		folder.attr("_loaded","true");		
		ajaxRefresh(e, arg, null , 'tagOpenCloseClicked');					
	}
	else {
		e._clientAjaxCallback();
	}
}


function postProcessRssContent () {

		// reset the padding so that scroll disappears, do this first so that the "_offset" would be correct
	$('#BottomPad').css('height', '0');

	loadPreviewImageBlock();
	var index = 0;
	var contentPositionTop = $('#ContentPanel').position().top;
	
	$("#ContentPanel .contentViewRSSList").each(function(i) {	
		setFaviconOriginalSrc($(this));
		
			// takes longDesc and extract text and insert to shortDesc (ie. the preview)
		var contentViewRSSListHeader = $(this).children(".contentViewRSSListHeader");
		var longDesc = $(this).find("._longDesc:first");
		var shortDesc = longDesc.text();		
		shortDesc = trim(shortDesc);
		contentViewRSSListHeader.find("._shortDesc").attr('title', shortDesc).first().html(shortDesc);

			// the index of this item in the list		
		$(this).attr('_index', index++);
		
			// calculates how far this item is from the top - used for padding
			
		$(this).attr('_offset', $(this).offset().top - contentPositionTop);
		
		
	});
	
		// history color change or hide
	$("#ContentPanel .contentViewRSSList").each(function(i) {	
		var key = $(this).attr('href');
		var value = localStorage.getItem(key); 
		if (value && value == 'T' ) {
			$(this).children('.contentViewRSSListHeader').css('color','SlateBlue');
		}
	});
	

	
		// reset Toggle to collapse
	resetExpandView();
		// reset hide read
	resetHideRead();
		// flash the "result" counts
	$("#SearchResultTotal").css('font-weight', 'bold').fadeOut(300).fadeIn(300).fadeOut(300).fadeIn(300).fadeOut(300).fadeIn(300).fadeOut(300).fadeIn(300);	

};


function loadPreviewImageBlock ()
{
	
		// this does the image priview prep. basically upon refresh we grab all the RssList, 
		// fetch all the img src for each entry and start download the images, each
		// new image created has it's attribute 'name' set to the id of the entry, so that 
		// when image is loaded the callback to addImageToPreview will insert it into preview block
		// base on the associating image
	if (isImagePreviewOn) {
		var AllImageUrls = [];
		$("#ContentPanel .contentViewRSSList").each(function(i) {	
			var contentViewRSSListHeader = $(this).children(".contentViewRSSListHeader");
				// download images
			$(this).find("._longDesc img").each(function(i) {
				var currentUrl = $(this).attr("src");
				var url = $(this).attr("_truesrc");
				if (url && url != currentUrl) {
					if ($.inArray(url, AllImageUrls) < 0) {
						AllImageUrls.push(url);
						var myImage = new Image();
						myImage.contentViewRSSListHeader = contentViewRSSListHeader;
						myImage.onload = imageOnloadCallback;
						myImage.src = url;
					}
				}
			})
		});
	}
}

function inPreviewImageBlock (n, e)
{
	toggleHeaderRssIcon(n, true);
	if (isImagePreviewOn) {
		var previewBlock = $(n).find("._previewImageBlock");
		var width = $(n).width();
		width = parseInt(width * 0.75) + 'px';
		previewBlock.css('max-width', width);
		previewBlock.position({ my: "center top", at: "center bottom", of: $(n), offset: "0 10", collision: "fit flip"}); 	
		previewBlock.css('visibility', 'visible');
	}
	stopBubbling(e);
}

function toggleHeaderRssIcon (n, showSite)
{
	var isExpand = $(n).attr('_isExpand');
	if (isExpand == 'false') {
		if (showSite) {
			$(n).find('div._headerIcons img._feedIcon').addClass('forceHide');
			$(n).find('div._headerIcons img._siteIcon').removeClass('forceHide');
		}
		else {
			$(n).find('div._headerIcons img._feedIcon').removeClass('forceHide');
			$(n).find('div._headerIcons img._siteIcon').addClass('forceHide');
		}
	}
}

function toggleCmdBar (node, event, link)
{
	var node = $(node).closest(".contentViewRSSList");

	var isExpand = $(node).attr('_isExpand');
	if (isExpand == 'false') {
		openContentWindow(event, link);
	}
	else {
		var a = $(node).find('._commandBar');
		a.toggleClass('hide');

	}
	stopBubbling(event);
	return false;
}

function outPreviewImageBlock (n, e)
{
	toggleHeaderRssIcon(n, false);
	if (isImagePreviewOn) {
		var previewBlock = $(n).find("._previewImageBlock");
		previewBlock.css('visibility', 'hidden');

	}
	stopBubbling(e);
}

function imageOnloadCallback ()
{	
	var height = this.height;
	var width = this.width;
	
		// likely an ad
	if (width < 64 || height < 60 ) {
		return;
	}
		// likely an http://ads.pheedo.com
		// probably want to remvoe 
	if (width > (height * 4)) {
		return;
	}

		// likely an http://ads.pheedo.com
		// probably want to remvoe 
	if (width == 1 &&  height == 1) {
		return;
	}
	
	
	var contentViewRSSListHeaderRssIcon =  this.contentViewRSSListHeader.find("div._headerIcons img.rssIcon");
	contentViewRSSListHeaderRssIcon.css("opacity", "1.0");
	

		// append new image to previewImageBlock
	var previewBlock = this.contentViewRSSListHeader.find("._previewImageBlock");
	var img = '<div class="previewImageWrapper"><img class="previewImage" src="' + this.src + '"/></div>';		
	previewBlock.append(img);
	
	var count = $(previewBlock).find('img').size();
	if (count == 1 && height >= 80) {
		 $(previewBlock).find('img').css('height', '120px');
	}
	else {
		$(previewBlock).find('img').css('height', '80px');
	}
}






function toggleDisplay (node, notAnimate, forceToggle, notRead, clientAjaxCallback)
{
	node = $(node).parent();

	var shortDesc = node.find("._shortDesc");

	var info = node.find("._info");
	var navBar = node.find('._navBar');
	var commandBar = node.find('._commandBar');
	var imagePreview = node.find('._previewImageBlock');	

	var rssContent = node.find("._rssContent");
	var longDesc = node.find("._longDesc");
	var contentIFrame = node.find("._contentIFrame");

		// if forceToggle (ie click EXPAND/LIST) then use forceToggle value otherwise use current display state
	var show = forceToggle == undefined 
		?  rssContent.css('display') == 'none' 
		: forceToggle;
			
	if (show) {

  			// sets the padding so that entires on the bottom looks fine
  		$('#BottomPad').css('height', $(node).attr('_offset') + 'px');
		
			// with manual created event as below some data are not present in the node hence this 2 is not in the clientAjaxCallback 
		toggleHeaderRssIcon(node, false);		
		$(node).attr('_isExpand', true);	
			

		var e = fakeEvent();
		e._clientAjaxCallback = function () {

			// start showing the imgs
			longDesc.find('img').each(
				function() {
				    var originalsrc = $(this).attr('_truesrc');
				    if (originalsrc) {
							// may want to use the "ajax loader gif"
				    	$(this).attr('src', originalsrc);
				    }
				}
			);

			longDesc.find('*').css('lineHeight', '1.5em');
			longDesc.find('a').each(
				function () {
					var href = $(this).attr('href');
					//$(this).attr('href','#');
					$(this).attr('onClick', "openContentIFrame(this, event, '" + href + "');return false");
				}
			); 
			
			
			shortDesc.css('display','none');
			info.css('display','block');
			navBar.css('display', 'block');			
			imagePreview.css('display','none');
			contentIFrame.css('display', 'block');	

			if (!notRead) {
				var header = node.children('.contentViewRSSListHeader'); 
				header.css('color','SlateBlue');
				var href = node.attr('href');
				
				var alreadyRead = localStorage.getItem(href); 
				if (!(alreadyRead && alreadyRead == 'T' )) {						
					localStorage.setItem(node.attr('href'), 'T');
					var recordId = href.substring(1, href.length);
					ajaxRefresh (e, recordId, null, 'readRssContent');					
				}		
			}
	
			if (!notAnimate) {
				rssContent.show(300);
				info.find('div._siteTitle').fadeOut(500).fadeIn(500).fadeOut(500).fadeIn(500).fadeOut(500).fadeIn(500);
			}
			else {
				rssContent.css('display','block');
			}

			rssContent.removeClass('forceHide');			
			if (clientAjaxCallback) {
				clientAjaxCallback();
			}
		}


			// fetch RssContent if this is a BIG content		
		var href = node.attr('href');
		if (href.charAt(href.length - 1) == 'L') {
			var recordId = href.substring(1, href.length - 1);
			var nodeId = node.attr('id');
			node.attr('href', '#' + recordId); // clear the L
			ajaxRefresh (e, recordId + "/" + nodeId, null, 'getRssContent');
		}
		else {
			e._clientAjaxCallback();			
		}
	}
	else {
		imagePreview.css('display','block');
		node.attr('_isExpand', false);
		
		
		rssContent.css('display','none');			    
		shortDesc.css('display', 'inline');
		info.css('display','none');	
		navBar.css('display', 'none');
		commandBar.css('display','none');
	}

		
	return false;	
}


function updateReadCount ()
{
	$('#ClearHistoryId').html(localStorage.length);	
}


function clearHistory ()
{
	var keys = new Array();
	for (var i = 0; i < localStorage.length; i++) {
		var key = localStorage.key(i);
		if (key.charAt(0) == '#') {
			keys.push(key);
		}
	}
	
		// use clear() - need to first dump out the existing ones the put back
	for (var i = 0; i < keys.length; i++) {
		localStorage.removeItem(keys[i]);		
	}
	
	$("#ContentPanel .contentViewRSSListHeader").each(function(i) {	
		$(this).css('color','MidnightBlue');
	});
	
}


function toggleCheckBox (e)
{
	var checkbox = $(e.data.arg);
	if ($(checkbox).attr("checked")) {
		$(checkbox).removeAttr("checked");
	}
	else {
		$(checkbox).attr("checked","true");				
	}
	return false;
}





function fontHeight (pa) {
	pa= pa || document.body;
	var who= document.createElement('div');
	var atts= {fontSize:'11px',padding:'0.5em',position:'absolute',lineHeight:'1.5em',visibility:'hidden'};
	for(var p in atts){
		who.style[p]= atts[p];
	}
	who.appendChild(document.createTextNode('M'));
	pa.appendChild(who);
	//var fs= [who.offsetWidth,who.offsetHeight];
	var fontHeight = who.offsetHeight; 
	pa.removeChild(who);
	//alert(who.offsetHeight);
	//return fs;
	return fontHeight; 
}	


function toggleFontSize (e) {
	var max = 18;
	var min = 10;
	
	var content = $("#ContentPanel");
	var fontSize = content.css("fontSize").replace("px","");
    fontSize = parseInt(fontSize);
    
    if (e.data.isIncrease) {
        if (fontSize < max) {
        	content.css("fontSize",  (fontSize + 2) + "px");	
        }
    }
    else {
        if (fontSize > min) {
        	content.css("fontSize",  (fontSize - 2) + "px");	
        }
    }
    //setDimension();
}

function toggleFontFamily () {
	
	var fontFamilies = new Array("Verdana","Arial","Courier-New", "Georgia", "Times-New-Roman", "Tahoma");
	var content = $("#ContentPanel");
	var currentFont = content.css("fontFamily").split(",")[0];
	
	for (i = 0; i < fontFamilies.length; i++) {
		if (fontFamilies[i].toLowerCase() == currentFont.toLowerCase()) {
			var newFont = fontFamilies[(i + 1) % fontFamilies.length];
			content.css("fontFamily",  newFont);				
			$("#FontFamily div").text(newFont);
			return;
		}
	}
}
	
	
function clearIFrameLoader (n)
{
	$('._clearIFrameLoader').css('display','none');
	$('#ContentPanel').css('overflow-y','scroll');
	BreakoutSiteName = null;
	
}	

function openContentIFrame (node, event, link) {
	
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
}   


	
function openContentWindow (event, link) 
{

		// should consider use jquery
	var winX = (document.all)?window.screenLeft:window.screenX;
	var winY = (document.all)?window.screenTop:window.screenY;

		// x offset,width is  10 80 10
		// y offset,height is 15 75 10
	
	var left = winX + $(window).width() * 0.10;
	var top =  winY +  $(window).height() * 0.15;
	//alert (" W " + $(window).height() + " H" + $(window).width() + " TOP ");  
	
	
	
	var newWindow = window.open(link, "test","scrollbars=yes,dependent=no,resizable=yes,toolbar=no,location=no,menubar=no,status=no,height=" + ($(window).height() * 0.75) + ",width=" + ($(window).width() * 0.80) + ", top=" + top +", left=" + left);
	
	
	if (navigator.userAgent.indexOf('Chrome/') > 0) {
		newWindow.blur();
		newWindow.focus();
	}	
	stopBubbling(event);	
}



function initSlider () {

	var LeftNav;
	var LeftNavWidth;	
	
	var ContentPanel;
	var ContentPanelWidth;
	var ContentPanelLeft;
	
	var ContentViewControl;
	var ContentViewControlWidth;
	var ContentViewControlLeft;
	
	var ContentViewPagination;	
	var ContentViewPaginationWidth;
	var ContentViewPaginationLeft;
	

		LeftNav = $("#LeftNav");
		LeftNavWidth = 15;		
		
		ContentPanel = $("#ContentPanel");
		ContentPanelWidth= 79.5;
		ContentPanelLeft = 20.5;

		ContentViewControl = $("#ContentViewControl");
		ContentViewControlWidth = 79.5;
		ContentViewControlLeft = 20.5;
		
		ContentViewPagination = $("#ContentViewPagination");
		ContentViewPaginationWidth = 79.5;
		ContentViewPaginationLeft = 20.5;
		
		$("#slider").slider({
				range: 'min',
				min: 0,
				max: LeftNavWidth + ContentPanelWidth,
				animate: false, 
				value: LeftNavWidth,
				step: 0.001,
				slide: function(event, ui) {
					var delta = ui.value - LeftNavWidth;
					LeftNav.css("width", (LeftNavWidth + delta) + "%");
					ContentPanel.css("width", (ContentPanelWidth - delta) + "%").css("left", (ContentPanelLeft + delta) + "%");
					ContentViewControl.css("width", (ContentViewControlWidth - delta) + "%").css("left", (ContentViewControlLeft + delta) + "%");
					ContentViewPagination.css("width", (ContentViewPaginationWidth - delta) + "%").css("left", (ContentViewPaginationLeft + delta) + "%");					
				}
			});
}



function Class_ImportSites ()
{
	var that = this;	
	that.folderTree = null;
	that.linksCounter = null; 	 
		
	this.openFileUpload = function (e) {
		var uploader = new qq.FileUploader({
		    // pass the dom node (ex. $(selector)[0] for jQuery users)
		    element: document.getElementById('uploadBookmarkDialog_file'),
		    // path to server-side upload script
		    action: '/7/' + sessionId + '/' + uniqueClientToken + '/uploadUserBookmarkClicked',
		    // when upload is complete
		    onComplete: that.onFileUploadComplete
		});
		
		
		$('#bookmarkImporterDialog_ok').css('display','none');		
		formOpenClicked(e);		
	};


	this.onFileUploadComplete = function (id, fileName, result) {
		    	
    		// hide file upload dialog
    	e = fakeEvent();    		
    	e.data.dialogMeta = '#uploadBookmarkDialog';
    	formCancelClicked(e);
    	e.data.dialogMeta = '#bookmarkImporterDialog';	    	
    		
    	e._clientAjaxCallback = function () {
			that.folderTree = {Default:[]}; 
			that.linksCounter = {count:0};
			var currentFolder;			
			var dialog = $('#bookmarkImporterDialog_html'); 
			
				// build the map of  tag : [links]

			var fileFormat = null;
    		dialog.find('*').each(
    			function () {
    				var tagName = this.tagName;
    				if (fileFormat == null && tagName == "OPML") {
    					fileFormat = "OPML";
    				}
    				
					if (fileFormat == "OPML") {    				    				 	    			
						if (tagName == 'OUTLINE') {
							var feedURLStr = $(this).attr('XMLURL');
							var title = $(this).attr('TEXT');	    				
							if (feedURLStr == undefined) {
								var folderName = title;
								if (!that.folderTree[folderName]) {
									that.folderTree[folderName] = [];	
								}
								currentFolder = that.folderTree[folderName];
							}						
							else {
								var folder = currentFolder ?  currentFolder : that.folderTree.Default;
								var linkElement = $('<span>').html(title);
								folder.push({element:linkElement, feedURL:feedURLStr});
								that.linksCounter.count++;
							}
						}
					}
					
					else {
						if (tagName == 'H3') {
							var folderName = $(this).html();
							if (!that.folderTree[folderName]) {
								that.folderTree[folderName] = [];	
							}
							currentFolder = that.folderTree[folderName];
						}						
						if (tagName == 'A') {
							var folder = currentFolder ?  currentFolder : that.folderTree.Default;		
							var linkElement = $('<span>').html($(this).html()).css('white-space','nowrap');					
							folder.push({element:linkElement, feedURL:$(this).attr('href')});
							that.linksCounter.count++;
						}						
					}						    				
    			}
    		);

			
				// for each link - show on the page and lookup the feed!

			dialog.html('');
    		for (folder in that.folderTree) {
    			dialog.append('<br/>').append('<span><strong>' + folder + '</strong></span><br/><br/>');
    			
    			for(var i=0; i < that.folderTree[folder].length; i++) {
    				var link = that.folderTree[folder][i].feedURL;  					    					
	    			that.lookupFeed(link, that.folderTree[folder][i], dialog);
    				dialog.append(that.folderTree[folder][i].element).append('<br/>');
    			}
    		}


    		/*
    		$('#bookmarkImporterDialog_html').find('a').each(
    			function () {
    				var link = $(this).attr('href');
    				
    				if (link.indexOf('http://') == 0) {		    				
    					$(this).css('background-color', link.indexOf('http://') == 0 ? 'green': 'red');
	    				//google.feeds.lookupFeed(link, uploadFeed);
	    				lookupFeed(link, this);
    				}
    			}
    		);
    		*/
    		
    		
    			// open the import page now
			formOpenClicked(e);
			that.testLookupFeedDone();
    	}	
    	
    	ajaxRefresh(e, fileName, null, 'fetchFile');
    };

		// while loop to test when all lookup is done
	this.testLookupFeedDone = function  ()
	{
		if (that.linksCounter.count > 0) {
			console.log(that.linksCounter.count);
			setTimeout (that.testLookupFeedDone, 1000);						
		}
		else {
			
				// show button when done
			console.log('done!!!');
			$('#bookmarkImporterDialog_ok').css('display','block');
		}
	}			
		
	this.lookupFeed = function (link, entry, dialog)
	{
			// if not an valid url skip, and decrement count
		if (link.indexOf('http://') != 0) {
			that.linksCounter.count--;
			return;
		}			
							
		var data = {q:link,v:'1.0'};	
		$.ajax({
			  async: true,
			  type: 'GET',
			  data: data,
			  context: entry,
			  url: 'http://ajax.googleapis.com/ajax/services/feed/lookup',
			  dataType :'jsonp',		  
			  success : function (json, textStatus, XMLHttpRequest) {
	    	    console.log('lookup feed for url: ' + link);	    						    						  	
	         	if( json.responseStatus == 200) {         		
	         		this.feedURL = json.responseData.url;         		
	         		console.log('>> found feed: ' + this.feedURL + 'for link: ' + link);
	         		
		    		$(this.element).css('color', 'blue');
	         	}		  	
	         	else {
	         		$(this.element).css('color', 'green');
	         		//console.log('>> feed not found: ' + json.responseDetails);
	         	}
	         	
	         		// decrement count and scroll to it
				that.linksCounter.count--;			
				dialog.scrollTo(this.element, 1, {offset:-40});
				
			  }		  
		});						
	};


	this.bookmarkImportClicked = function (e)
	{
	   var result = [];
	   for (folder in that.folderTree) {
	   		result.push(folder);
			for(var i=0; i < that.folderTree[folder].length; i++) {
				if (that.folderTree[folder][i].feedURL != null) {
					result.push(that.folderTree[folder][i].feedURL);
				}  					    					
		   	}  
	   }   
	   
	   e._clientAjaxCallback = function () {
		   formCancelClicked(e);
	   }
	   			
	   ajaxRefresh(e, result.toString(), null, 'importBookmark');
	}		
	
}

var importSites = new Class_ImportSites();



function Class_RefreshManager ()
{
	var that = this;
	that.lastRefreshableAction = null;
	
    this.setLastRefreshableAction = function (action)
    {
    		// track these 2 actions for now
    	if (action.indexOf("tagClicked") == 0 || action.indexOf("dateClicked") == 0) {
			that.lastRefreshableAction = action;    		
    	}
    };
    
    
    this.clearLastRefreshableAction = function ()
    {
    	that.lastRefreshableAction = null;	
    };
 
 
	this.refreshFolderClicked = function (e)
	{
		ajaxRefresh(e, null, null, "refreshFolderClicked");		
	} 
    
    
    this.refreshClicked = function (e)
    {
    	that.lastRefreshableAction;    	
		ajaxRefresh(e, that.lastRefreshableAction, null, "refreshClicked");    	
    };
};
var RefreshManager = new Class_RefreshManager();




		
