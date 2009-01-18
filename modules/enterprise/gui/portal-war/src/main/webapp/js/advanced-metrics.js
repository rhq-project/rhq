function toggleControls(elementId) {
	var element = document.getElementById(elementId);
	if (element.disabled = true) {
		enableElement(element);
	} else {
		disableElement(element);
	}
}

function disableElement(elementId) {
	document.getElementById(elementId).disabled = true;
}

function enableElement(elementId) {
	document.getElementById(elementId).disabled = false;
}

function uncheckRadio(elementName) {
	var elementsList = document.getElementsByName(elementName);
	for (i = 0; i < elementsList.length; i++) {
		elementsList[i].checked = false;
	}
}

function checkRadio(elementName) {
	var elementsList = document.getElementsByName(elementName);
	for (i = 0; i < elementsList.length; i++) {
		elementsList[i].checked = true;
	}
}

function checkUncheckRadio(elementName1, elementName2) {
	checkRadio(elementName1);
	uncheckRadio(elementName2);
}

function closePopupAndReloadParent() {
	window.opener.location.href = window.opener.location.href;
	if (window.opener.progressWindow) {
		window.opener.progressWindow.close();
	}
	window.opener.location.reload();
	window.opener.focus();
	window.close();
}