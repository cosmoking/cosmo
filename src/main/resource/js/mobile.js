content.entryTemplateId = 'appuiMobileContentDivId';
content.entryRowTemplateId = 'appuiMobileContentRowDivId';
content.displayPerSection = 2;
content.noOfSections = 4;

image.frameH = 80;
image.frameW = 120;
image.entryDisplayStetchToFitFrameFrom = 200;
image.entryDisplayStetchToFitFrameTo = 300;
image.entryDisplayAdjustToFitFrameFrom = 100;
image.entryDisplayAdjustToFitFrameTo = 200;
image.entryDisplaySmallFrameFrom = 80;
image.entryDisplaySmallFrameTo = 80;

executionTracker.timeToShowLoader = 200; 




function pullDownAction () {
}

function pullUpAction () {
	action.moreSection();	
}


(function() {
 	document.addEventListener(
 		'DOMContentLoaded', 	
 		function () {
 			
 			/* Pull down stuff
			pullDownEl = document.getElementById('pullDown');
			pullDownOffset = pullDownEl.offsetHeight;
			pullUpEl = document.getElementById('pullUp');	
			pullUpOffset = pullUpEl.offsetHeight;
 			*/
 			
 			
			FI.ContentPanel = new iScroll($('#ContentPanel')[0], {hideScrollbar:true,vScrollbar:false,useTransition:true,checkDOMChanges:false});
			
			/* Pull down stuff
				,topOffset: pullDownOffset,
				onRefresh: function () {
					if (pullDownEl.className.match('loading')) {
						pullDownEl.className = '';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = 'Pull down to refresh...';
					} else if (pullUpEl.className.match('loading')) {
						pullUpEl.className = '';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = 'Pull up to load more...';
					}
				},
				onScrollMove: function () {
					if (this.y > 75 && !pullDownEl.className.match('flip')) {
						pullDownEl.className = 'flip';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = 'Release to refresh...';
						this.minScrollY = 0;
					} else if (this.y < 75 && pullDownEl.className.match('flip')) {
						pullDownEl.className = '';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = 'Pull down to refresh...';
						this.minScrollY = -pullDownOffset;
					} else if (this.y < (this.maxScrollY - 75) && !pullUpEl.className.match('flip')) {
						pullUpEl.className = 'flip';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = 'Release to refresh...';
						this.maxScrollY = this.maxScrollY;
					} else if (this.y > (this.maxScrollY + 75) && pullUpEl.className.match('flip')) {
						pullUpEl.className = '';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = 'Pull up to load more...';
						this.maxScrollY = pullUpOffset;
					}
				},
				onScrollEnd: function () {
					if (pullDownEl.className.match('flip')) {
						pullDownEl.className = 'loading';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = 'Loading...';				
						pullDownAction();	// Execute custom function (ajax call?)
					} else if (pullUpEl.className.match('flip')) {
						pullUpEl.className = 'loading';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = 'Loading...';				
						pullUpAction();	// Execute custom function (ajax call?)
					}
				}}); 
			*/
			
			FI.ContentDetailPanel = new iScroll($('.viewContentDetailCenter .viewContentDetailArea')[0], {hideScrollbar:false,vScrollbar:true,useTransition:true,checkDOMChanges:false});
			
				// hide address bar 
			setTimeout(function () {window.scrollTo(0,1);},	100);
		},
		false);
 })();
 
 



sup = {
	action : {
		applyContentOnSection : action.applyContentOnSection,
		applyContent : action.applyContent,
		moreSection : action.moreSection
	}
}
 
 

action.scrollToTop = function (selector) {
		// fixme - should check to selector
	if (selector) {
		FI.ContentDetailPanel.scrollTo(0,0,0);
	}
	else {
		FI.ContentPanel.scrollTo(0,0.0);
	}
}
 
lightScrollBar.refresh = function (selector) {
	if (".viewContentDetailCenter .viewContentDetailArea" == selector) {
		setTimeout (function () {FI.ContentDetailPanel.refresh();}, 0);
	}			
}

base.modalOn = function () {
} 
 
base.modalOff = function () {
} 

action.refreshNext = function () {
	
}

base.setFavIconSrc = function () {
	
}

contentDetail.closeContent = function (e, closeContentOnly) {
	if (!closeContentOnly) {
		$(".viewContentDetailCenter").css('display','none');
	}
}	


	// override to only show in source mode for now
action.applyContentOnDigest = function (records, noOfFrames) {

 	
	if (content.mode == content.modeType.Source) {
			// here instead of using "DigestPanel"'s content row as in MainBody.jwl - reuse mobile row tempalte "content.entryRowTemplateId"
		var modeClass = "_mode" + content.mode;
		var sectionName = "digest";
		for (i = 0; i <records.length; i++) {
			var categoryHtmlAsBindings = [modeClass, sectionName, page.recordsToHtml(records.splice(0, 4), content.entryTemplateId)];
			page.applyBindingsToId(categoryHtmlAsBindings, '#DigestPanel', content.entryRowTemplateId, true);
		}			
	}	
}
 


// override to first call super then refresh the section row with iscroll
action.applyContentOnSection = function (sectionName, records, noOfFrames) {
	sup.action.applyContentOnSection(sectionName, records, noOfFrames);
	
		// only create row scroll if not mode Source
	if (content.mode != content.modeType.Source) {
	
			// refresh newly inserted scroll - (ie last())
		var scrollable = $('#SectionPanel .scrollableControl').last(); 
		var scroll = new iScroll($(scrollable)[0], {
					snap: true,
					useTransition:true,
					momentum: false,
					hScrollbar: false,
					checkDOMChanges: true /* when inserted this gets refreshed auto */
				 });
			// bind to element for later use
		$(scrollable).data('scroll', scroll);
		
	}
		// refresh
	setTimeout(function () { FI.ContentPanel.refresh(); }, 0);
	
}



// override to first set the template ids then delegate to super
action.applyContent = function (bindings, mode) {

		// applying new content - destory old row scrollers
	$('.mobileHScrollableControl').each (function() {
		var scroll = $(this).data('scroll');
		if (scroll) {
			scroll.destroy();
			$(this).data('scroll', null);
		}
	}); 
	
	sup.action.applyContent(bindings, mode);
}

// override to simply call super
action.moreSection = function () {
	sup.action.moreSection();
}

