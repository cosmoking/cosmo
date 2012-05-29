

function testEditorjs ()
{
	alert('testEditorjs');	
}


function editClicked (e)
{
	
		// setup callback when form is submitted (OK clicked). the arg is the event
		// the inputValues were captured during the formSubmit and passed on during the callback
		// so  that update on the browser can be done when server ajax call returns
		// basically taking the new value and update on the html. 	
	var callback = function (arg, event, inputValues, validationResult) {
			// escape twice in the formsubmitted
		var value = unescape(unescape(inputValues));
		var values = $.parseJSON(value);
		var newValue = values.editDialog_newName;
		$(arg.currentTarget).html(newValue);
	};
	setupPostFormSubmitAction(callback, e);
	
		// pass on the "args" to the form, so that when submit it gets passed to the server	
	e.data.arg =  e.data.recordMeta + "/" + e.data.recordField + "/" + e.data.recordId;

		// setup the currentName in the form
	var currentName = $(e.currentTarget).html();		
	$(e.data.dialogMeta).find("#editDialog_newName").val(currentName);	
	$(e.data.dialogMeta).find("#editDialog_currentName").val(currentName);
	
		// now everything is read open form
	formOpenClicked(e);	
}


function searchRecordFormOpenClicked (e)
{
	//var folder = $('#' + e.data.metaId);		
	//e.data.arg = folder.attr("_recordId");

	
	
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