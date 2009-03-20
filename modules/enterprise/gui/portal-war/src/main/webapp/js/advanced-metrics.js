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

function validateFields() {
	var duration = document.getElementById('advancedMetricsValuesForm:durationMetricType:0').checked;
	var interval = document.getElementById('advancedMetricsValuesForm:intervalMetricType:0').checked;

	if(duration) {
		var durationValue = document.getElementById('advancedMetricsValuesForm:duration').value;
		if (isNaN(durationValue)){
			alert("Please enter a valid number");
			return false;
		} else {
			document.advancedMetricsValuesForm.submit();
			closePopupAndReloadParent();
		}
	} else if(interval) {
		var from= document.getElementById('advancedMetricsValuesForm:fromDateInputDate').value;
		var to= document.getElementById('advancedMetricsValuesForm:toDateInputDate').value;

		if(from== "" || to == "") {
			alert ("Please fill in the required dates");
			return false;
		} else {
			var fromDate = new Date(from);
			var toDate = new Date(to);
			var now = new Date();
			if(fromDate > now || toDate > now) {
				alert ("Cannot select a date in the future");
				return false;
			} else if (fromDate > toDate ) {
				alert ("From date cannot be greater than to date");
				return false;
			} else {
				document.advancedMetricsValuesForm.submit();
				closePopupAndReloadParent();
			}
		}
	}
}