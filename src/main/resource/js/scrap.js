



function popupMenu (someElement)
{
		
	var contextElement = document.getElementById(someElement);
	var top = ariba.Dom.absoluteTop(contextElement);
	var left = ariba.Dom.absoluteLeft(contextElement);
	
	var popupMenu = document.getElementById("mypopupMenu");
	popupMenu.style.display = "block";
	popupMenu.style.position = "absolute";
	popupMenu.style.border = "2px solid";
	popupMenu.style.top = (top + 20) + "px";
	popupMenu.style.left = left + "px";
	
}


function hidePopupMenu ()
{
	var popupMenu = document.getElementById("mypopupMenu");
	if (popupMenu) {
		popupMenu.style.display = "none";
	}	
}


function popupMenuX (someElement)
{
		
	var contextElement = document.getElementById(someElement);
	var top = ariba.Dom.absoluteTop(contextElement);
	var left = ariba.Dom.absoluteLeft(contextElement);
	
	var popupMenu = document.getElementById("popupAreaX");
	popupMenu.style.display = "block";
	popupMenu.style.position = "absolute";
	popupMenu.style.border = "2px solid";
	popupMenu.style.top = (top - 20) + "px";
	popupMenu.style.left = left + "px";
	
}


function hidePopupMenuX ()
{
	var popupMenu = document.getElementById("popupAreaX");
	if (popupMenu) {
		popupMenu.style.display = "none";
	}	
}



	function reportSize() {
	  myWidth = 0, myHeight = 0;
	  if( typeof( window.innerWidth ) == 'number' ) {
	    //Non-IE
	    myWidth = window.innerWidth;
	    myHeight = window.innerHeight;
	  } else {
	    if( document.documentElement &&
	        ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
	      //IE 6+ in 'standards compliant mode'
	      myWidth = document.documentElement.clientWidth;
	      myHeight = document.documentElement.clientHeight;
	    } else {
	      if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
	        //IE 4 compatible
	        myWidth = document.body.clientWidth;
	        myHeight = document.body.clientHeight;
	      }
	    }
	  }
	}
	function setInnerWidth(w,h){
		window.resizeTo(800,800);
		reportSize();
		var oX = 800-myWidth;
		var oY = 800-myHeight;
		window.resizeTo( (w+oX) , (h+oY) );
	}
	

	function setDimension() {
		
		 reportSize();
		var LeftNavTop = $("#LeftNav").offset().top;		
		var LeftNavHeight = $("#LeftNav").height();
		var LeftNavInnerHeight = $("#LeftNav").innerHeight();
		
		var ContentPanelTop = $("#ContentPanel").offset().top;		
		var ContentPanelTopHeight = $("#ContentPanel").height();
		var ContentPanelTopInnerHeight = $("#ContentPanel").innerHeight();

		
		var ContentViewPaginationTop = $("#ContentViewPagination").offset().top;
		
		var padder = $("#BottomPad"); padder.css("display","block");		
		var entrySize = padder.outerHeight(); padder.css("display","none"); 	
		document.getElementById("BottomPad").style.display = "none";
		
		var size1 = ContentPanelTopInnerHeight / entrySize;
		var size =  parseInt(size1);
			
			//document.getElementById('payloadid').value = "X: "+ myWidth+ " Y: "+myHeight;
		document.getElementById("windowSize").innerHTML = "X: "+ myWidth+ " Y: "+myHeight + " Size: " + size;
		document.getElementById('payloadid').value = "" + size;

		
		//var dummyClick = document.getElementById('dummyClickId');
		//var senderId = dummyClick.getAttribute('senderId');
		//alert(senderId);
		var mainForm = document.getElementsByTagName("form");
		var formId = mainForm[0].id;
		ariba.Request.senderClicked ('someveryrandomid', formId, null, null, null, null, null, null);
		//  setDocumentLocation : function (hrefString, windowName, windowAttributes) 
		//var v =  document.getElementsByTagName("form");
		//ariba.Handlers.hSubmit(v[0]);
	}
	
	
	
	

	function setDimensionOri() {
		reportSize();
		var top = document.getElementById("LeftNav").offsetTop;
		var bottom = document.getElementById("ContentViewPagination").offsetTop;
		var padder = document.getElementById("BottomPad"); padder.style.display = "block";		
		var entrySize = padder.offsetHeight; padder.style.display = "none"; 	
		var contentSize  = bottom - top - 2;
		
		if (navigator.userAgent.indexOf('Firefox') != -1) {
			entrySize = entrySize - 1;
			contentSize = contentSize + 10;
		}

		
		var size1 = contentSize / entrySize;
		var size =  parseInt(size1);
		
		
		
		
		//document.getElementById('payloadid').value = "X: "+ myWidth+ " Y: "+myHeight;
		document.getElementById("windowSize").innerHTML = "X: "+ myWidth+ " Y: "+myHeight + " Size: " + size;
		document.getElementById('payloadid').value = "" + size;
		var v =  document.getElementsByTagName("form");
		ariba.Handlers.hSubmit(v[0]);
		//alert("height: " + myHeight + "top: " + top + "bottom: " + bottom + "size: " + entrySize + "count: " + size);
		
	}
	

	
	function initDimension (str) {

		var initCalled = parseInt(str);
		
		if (ariba.Dom.IsIE6 || ariba.Dom.IsIE7 || ariba.Dom.IsIE8) {
			if (initCalled < 2) {
				setDimension();
			}
		}
		else {
			if (initCalled < 1) {
				setDimension();
			}

		}

		
	}
	
	
	
	
	function onResizeCallBack () {
		setDimension();
	}

	function test() {
		var t = ((window.getSelection && window.getSelection())
				|| (document.getSelection && document.getSelection()) || (document.selection
				&& document.selection.createRange && document.selection
				.createRange().text));
		var e = (document.charset || document.characterSet);
		if (t != '') {
			location.href = 'http://translate.google.com/translate_t?text=' + t
					+ '&hl=en&langpair=auto|zh-TW&tbb=1&ie=' + e;
		} else {
			location.href = 'http://translate.google.com/translate?u='
					+ location.href
					+ '&hl=en&langpair=auto|zh-TW&tbb=1&ie=' + e;
		}
	}
	
	
	function test2() {
		var t = ((window.getSelection && window.getSelection())
				|| (document.getSelection && document.getSelection()) || (document.selection
				&& document.selection.createRange && document.selection
				.createRange().text));
		var e = (document.charset || document.characterSet);
		location.href = 'http://cos-pc:8080/jackapp/ad/scrap?u='
					+ escape(location.href)
					+ '&hl=en&langpair=auto|zh-TW&tbb=1&ie=' + e;
	}
	
	 	
	function googleTranslateElementInit() {
	  new google.translate.TranslateElement({
	    pageLanguage: 'en'
	  }, 'google_translate_element');
	}
	

javascript: 
	var header = function ()
	{
		var head = document.getElementsByTagName("head")[0].childNodes;
		var content='{"WEBSITE":{"value":"' + escape(location.href) + '"}';
		var collected = {}; 
		collected.count = 1;
		for (var i = 0; i<head.length ; i++) {
			content = add(collected, content, head[i], "TITLE");
			content = add(collected, content, head[i], "LINK", "type", "application/rss+xml");
			content = add(collected, content, head[i], "LINK", "type", "application/atom+xml"); 
			content = add(collected, content, head[i], "META", "name", "keywords");
			content = add(collected, content, head[i], "META", "name", "description");
			content = add(collected, content, head[i], "META", "name", "Keywords");
			content = add(collected, content, head[i], "META", "name", "Description");			
		};
		content += '}';
		return content;
	};
		
	var add = function (collected, c, n, takeTag, attr, attrValue) {
		if (n.tagName && n.tagName == takeTag) {	
			if (attr && n.getAttribute(attr) != attrValue) {
				return c;
			}
			c += (collected.count++ > 0 ? ',' : '') + '"' + takeTag + collected.count + '":{';
				
			for (var j=0;j<n.attributes.length;j++) {
				var attrName = n.attributes[j].name; 
				c += (j > 0 ? ',' : '') + '"' +  attrName + '":"' + escape(n.attributes[j].nodeValue) + '"';
			}
			if (n.text) {
				c += '"value":"' + escape(n.text) + '"';
			} 			
			c += "}";						
		}		
		return c;
	};
		
		// remember to match the server host !
	var w=window.open("http://localhost/0/6&"+escape(header()), "niunews","toolbar=no,location=no,status=no,width=400,height=450");		
