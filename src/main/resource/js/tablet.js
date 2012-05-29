content.entryTemplateId = 'appuiTabletContentDivId';
//content.entryRowTemplateId = 'appuiMobileContentRowDivId';
//content.displayPerSection = 3;
content.noOfSections = 4;

//image.frameH = 80;
//image.frameW = 120;
//image.entryDisplayStetchToFitFrameFrom = 200;
//image.entryDisplayStetchToFitFrameTo = 300;
//image.entryDisplayAdjustToFitFrameFrom = 100;
//image.entryDisplayAdjustToFitFrameTo = 200;
//image.entryDisplaySmallFrameFrom = 80;
//image.entryDisplaySmallFrameTo = 80;




base.setFavIconSrc = function () {
	
}

//image.loadImages = function () {
	
//}

lightScrollBar.refresh = function () {
}

lightScrollBar.mouseIn = function () {
}

lightScrollBar.mouseOut = function () {
}

lightScrollBar.scroll = function () {
}

iframe.startLoading = function () {
	
}

base.modalOn = function () {
	$('body').css('overflow-y','hidden');			
	$('#ModalBG').css('display','block');
}
	
base.modalOff =	function () {
	$('body').css('overflow-y','scroll');		
	$('#ModalBG').css('display','none');
}




	// override to open new window
iframe.maxIframe = function () {
	iframe.openContentWindow(iframe.link);		
}
