
/*-- START adminEditRole_checkAll.js --*/
function ToggleSelection(e, depends) {
	if (isIE)
		e = event.srcElement;
	
	var form = e.form;
	
	if (!e.checked) {
		var uList = e.form;
		var len = uList.elements.length;
		
		for (var i = 0; i < len; i++) {
			var elt = uList.elements[i];
			if (elt.getAttribute("type")=="checkbox" && elt.getAttribute("value")==depends) {
				elt.checked = false;
			}
		}
	}
}

function ToggleSelectionState(e, depends, state) {
	if (isIE)
		e = event.srcElement;
	
	var form = e.form;
   var uList = e.form;
	var len = uList.elements.length;
		
	for (var i = 0; i < len; i++) {
		var elt = uList.elements[i];
		if (elt.getAttribute("type")=="checkbox" && elt.getAttribute("value")==depends) {
		   if (e.checked == state) {
		      elt.checked = e.checked;
		   }
		}
	}
}

function ToggleAll(e) {
	if (isIE)
		e = event.srcElement;
	
	if (e.checked) {
		CheckAll(e);
	}
	else {
		ClearAll(e);
	}
}

function CheckAll(e) {
	var uList = e.form;
	var len = uList.elements.length;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute("type")=="checkbox") {
			e.checked = true;
		}
	}
}
	
function ClearAll(e) {
	var uList = e.form;
	var len = uList.elements.length;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute("type")=="checkbox") {
			e.checked = false;
		}
	}
}
/*-- END adminEditRole_checkAll.js --*/
