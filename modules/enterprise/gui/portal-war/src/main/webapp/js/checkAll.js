
/*-- START checkAll.js --*/
function ToggleSelection(e) {
	if (isIE)
		e = event.srcElement;
	
	var form = e.form;
	
	if (!e.checked) {
		var group = e.getAttribute(classStr);
		var uList = e.form;
		var len = uList.elements.length;
	
		for (var i = 0; i < len; i++) {
			var e = uList.elements[i];
			if (e.getAttribute(classStr)==(group + "Parent")) {
				e.checked=false;
			}
		}
	}
}

function ToggleAll(e) {
	if (isIE)
		e = event.srcElement;
  var groupName = getGroupName(e);
	
	if (e.checked) {
		CheckAll(e, groupName);
	}
	else {
		ClearAll(e, groupName);
	}
}

function CheckAll(e, groupName) {
	var uList = e.form;
	var len = uList.elements.length;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)==groupName) {
			e.checked = true;
		}
	}
}
	
function ClearAll(e, groupName) {
	var uList = e.form;
	var len = uList.elements.length;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)==groupName) {
			e.checked = false;
		}
	}
}

function getGroupName(e) {
  var parentGroup = e.getAttribute(classStr);
  var endPosition = parentGroup.indexOf("Parent");
  var group = parentGroup.substring(0, endPosition);
  
  return group;
}
/*-- END checkAll.js --*/

