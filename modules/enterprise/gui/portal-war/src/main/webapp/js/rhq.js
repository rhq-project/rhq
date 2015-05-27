/**
 * The theme used for all PWC (i.e. window.js) windows.
 *
 * TODO: We should define our own PWC theme that matches the L&F of the rest of
 * the app.
 */
var WINDOW_THEME = 'alphacube';

/**
 * A special value for an input that tells the server-side that the
 * corresponding value should be set to null.
 */
var NULL_INPUT_VALUE = " ";

/**
 * Set the unset status of the specified input.
 *
 * @param input
 *            an input element
 * @param unset
 *            whether or not the input should be unset
 */
function setInputUnset(input, unset) {
	clearInputValue(input);
	setInputDisabled(input, unset, true);
	input.unset = unset; // our own special property that tells us whether an
	// input is unset
	if (!unset) {
		// Set focus on the input, but only if it is a text-entry field - it doesn't make sense for radios, checkboxes,
		// etc.
		if (input.type == 'text' || input.type == 'password'
				|| input.type == 'textarea' || input.type == 'file') {
			input.focus();
		}
	}
}

/**
 * Write-protect the specified input element.
 *
 * @param input an input element
 */
function writeProtectInput(input) {
	if (input.length != null
			&& (input[0].type == 'radio' || input[0].type == 'select-multiple')) {
		// Recursively call ourself for each of the array items, which are the actual radio buttons or menu items.
		for ( var i = 0; i < input.length; i++) {
			writeProtectInput(input[i]);
		}
	} else {
		input.readonly = true;
		// NOTE: For non-text inputs (radios, checkboxes, etc), the "readonly"
		// attribute is ignored by browsers, so we
		// resort to the "disabled" attribute for these types of inputs. For the
		// text inputs, it is important to
		// *not* set the "disabled" attribute, because then the browser does not
		// allow them to receive focus, which
		// prevents tooltips from working (see
		// http://jira.jboss.com/jira/browse/JBNADM-1608).
		if (input.type != 'text' && input.type != 'password'
				&& input.type != 'textarea' && input.type != 'file') {
			setInputDisabled(input, true, true);
		}
	}
}

/**
 * Set the "title" attribute on the specified text input. If the input's value is longer than the size of the input,
 * set the title to the value. Otherwise, set the title to null.
 *
 * @param input an input element
 */
function setInputTitle(input) {
	if (input != null && input.type == 'text') {
		if (input.value != null && input.value.length > input.size) {
			input.title = input.value;
		} else {
			input.title = null;
		}
	}
}

/**
 * Clear the value of the specified input.
 *
 * @param input an input element
 */
function clearInputValue(input) {
	if (input.length != null) {
		if (input[0].type == 'radio') {
			// Recursively call ourself for each of the array items, which are the actual radio buttons.
			for ( var i = 0; i < input.length; i++) {
				clearInputValue(input[i]);
			}
		} else if (input[0].type == 'select-one'
				|| input[0].type == 'select-multiple') {
			// Recursively call ourself on the selected item.
			clearInputValue(input[input.selectedIndex]);
		}
	} else {
		switch (input.type) {
		case 'checkbox':
		case 'radio':
			input.checked = false;
			break;
		case 'select-one':
		case 'select-multiple':
			input.selected = false;
			break;
		default:
			// NOTE: We set the value to an empty string rather than null, because IE converts null to the string 'null'.
			input.value = '';
		}
	}
}

/**
 * Disable or enable the specified input element.
 *
 * @param input an input element
 * @param disabled if true, disable the input; otherwise, enable it
 * @param updateStyle whether or not the input's CSS style should be updated (i.e. grayed out if being disabled or
 *                    un-grayed-out if being enabled)
 */
function setInputDisabled(input, disabled, updateStyle) {
	if (input.length != null
			&& (input[0].type == 'radio' || input[0].type == 'select-multiple')) {
		// Recursively call ourself for each of the array items, which are the actual radio buttons or menu items.
		for ( var i = 0; i < input.length; i++) {
			setInputDisabled(input[i], disabled, updateStyle);
		}
	} else {
		input.disabled = disabled;
		if (updateStyle) {
			updateInputStyle(input);
		}
	}
}

/**
 * "Unsets" an array of input elements.
 *
 * @param inputs an array of input elements
 */
function unsetInputs(inputs) {
	for ( var i = 0; i < inputs.length; i++) {
		setInputUnset(inputs[i], true);
	}
}

function setInputsUnset(inputs, unset) {
	for ( var i = 0; i < inputs.length; i++)
		setInputUnset(inputs[i], unset);
}

function setInputsOverride(inputs, shouldOverride) {
	for ( var i = 0; i < inputs.length; i++) {
		setInputOverride(inputs[i], shouldOverride);
	}
}

function setInputOverride(input, shouldOverride) {
	input.override = shouldOverride;
	if (shouldOverride) {
		input.check();
	}
}

/**
 * Write-protects an array of input elements.
 *
 * @param inputs an array of input elements
 */
function writeProtectInputs(inputs) {
	for ( var i = 0; i < inputs.length; i++) {
		writeProtectInput(inputs[i]);
	}
}

/**
 * @param form a form element
 */
function prepareInputsForSubmission(form) {
	var inputs = Form.getElements(form);
	for ( var i = 0; i < inputs.length; i++) {
		if (inputs[i].disabled) {
			// NOTE: It is vital to enable any disabled inputs, since the browser will exclude disabled inputs from the
			// POST request.
			setInputDisabled(inputs[i], false, false);
			// Some browsers (e.g. Firefox) will automatically un-gray-out the
			// input, when the disabled property is
			// set to false, so we need to gray it out again, so it still
			// appears to be disabled.
			grayOutInput(inputs[i]);
			if (inputs[i].unset) {
				// NOTE: Set the input's value to a special string that will allow the server-side to distinguish between a
				// null (i.e. unset) value and an empty string value.
				inputs[i].value = NULL_INPUT_VALUE;
			}
		}
	}
	// Return true in case an onclick handler called us.
	return true;
}

/**
 * @param input an input element
 */
function updateInputStyle(input) {
	if (input.disabled) {
		grayOutInput(input);
	} else {
		input.style.background = '#FFFFFF';
		input.style.color = '#000000';
		input.style.border = '1px solid #A7A6AA';
	}
}

/**
 * @param input an input element
 */
function grayOutInput(input) {
	// Use the default Firefox colors, which are much more intuitive than the default IE colors.
	input.style.background = '#D6D5D9';
	input.style.color = '#000000';
	input.style.border = '1px solid #A7A6AA';
}

/**
 * Sets the values of an array of input elements to the specified value.
 *
 * @param inputs an array of input elements
 * @param value the value
 */
function setInputsToValue(inputs, value) {
	//if (confirm("Are you sure you want to set all member values to '" + value + "'?"))
	for ( var i = 0; i < inputs.length; i++)
		setElementValue(inputs[i], value);
}

/**
 * Updates the specified inputs and corresponding unset checkboxes to the specified master value / unset state.
 *
 * @param valueInputs an array of input elements for the values
 * @param unsetInputs an array of input elements for the unset checkboxes
 * @param masterValue a string representing the new value
 * @param masterUnsetState a boolean representing the new unset state
 */
function setAllValuesForOptionalProperty(valueInputs, unsetInputs, masterValue,
		masterUnsetState) {
	for ( var i = 0; i < valueInputs.length; i++) {
		setElementValue(unsetInputs[i], masterUnsetState);
		setInputUnset(valueInputs[i], masterUnsetState);
		if (!masterUnsetState)
			setElementValue(valueInputs[i], masterValue);
	}
}

/**
 * @param title the title to be displayed at the top of the modal
 * @param message the message to be displayed in the body the modal
 */
function displayMessageModal(title, message) {
	var win = new Window( {
		className : WINDOW_THEME,
		width : 350,
		height : 400,
		zIndex : 100,
		resizable : true,
		showEffect : Effect.BlindDown,
		hideEffect : Effect.SwitchOff,
		draggable : true,
		wiredDrag : true
	});
	//var escapedMessage = escape(message);
	win.getContent().innerHTML = "<div style='padding:10px'>" + message + "</div>";
	win.setTitle(title);
	win.showCenter(true); // true == modal
}

/**
 * This method is called in the onload of the body of
 * the main template to disable all conditional buttons
 */
function disableConditionalButtons() {
	var buttons = document.getElementsByTagName("input");
	var i;
	var button;
	for (i = 0; i < buttons.length; i++) {
		button = buttons.item(i);
		var selectTarget = button.getAttribute("target");
		if (selectTarget != null) {
			/*
			 * assume it should be disabled at first, and then run the button through
			 * the regular update mechanism to see if it should be re-enabled; if the
			 * page is being loaded for the first time, the updateButtons method will
			 * be a no-op, otherwise it will analyze which select items were enabled
			 * before the page refresh and re-enable button as appropriate according
			 * to their min/max processing rules.
			 */
			button.disabled = true;
			updateButtons(selectTarget);
		}
	}
}

/**
 * @param thisObj    the calling AllSelect object instance use to determine whether
 *                   or select or deselect all of the objects with the name selectName
 * @param selectName name of the dom instances that should be checked / unchecked
 */
function selectAll(thisObj, selectName) {
	var selects = document.getElementsByName(selectName);
	var i;
	var select;
	for (i = 0; i < selects.length; i++) {
		select = selects.item(i);
		if (select.disabled) {
			continue;
		}
		if (thisObj.checked) {
			select.checked = true;
		} else {
			select.checked = false;
		}
	}
	updateButtons(selectName);
}

/**
 * This method will be called either:
 *   1) directly as a result of checking or unchecking a single Select component, or
 *   2) as the final task of the selectAll method
 *
 * @param selectName name of the dom instances that are checked or unchecked which will determine
 *                   whether or not the conditional buttons on the page should be enabled / disabled
 */
// TODO: Make this robust enough to properly maintain CSS style when the button
// is disabled (ips, 08/31/07).
function updateButtons(targetName) {
	var count = countSelected(targetName);
	var buttons = document.getElementsByTagName("input");
	var i;
	var button;
	for (i = 0; i < buttons.length; i++) {
		button = buttons.item(i);
		if (button.getAttribute("target") != null
				&& button.getAttribute("target") == targetName) {
			var low = button.getAttribute("low");
			var high = button.getAttribute("high");
			if (high != null) {
				if (low <= count && count <= high) {
					button.disabled = false;
				} else {
					button.disabled = true;
				}
			} else {
				if (low <= count) {
					button.disabled = false;
				} else {
					button.disabled = true;
				}
			}
		}
	}
}

/**
 * Returns the number of checkboxes with the given name that are currently selected
 *
 * @param selectName name of the dom instances that should be counted for selections
 */
function countSelected(selectName) {
	var total = 0;
	var selectElts = document.getElementsByName(selectName);
	var i;
	for (i = 0; i < selectElts.length; i++) {
		if (selectElts.item(i).checked) {
			total = total + 1;
		}
	}
	return (total);
}

function setFoot() {
	var conH;
	var winH;
	var footerH = 28;
	var browserH = 88;
	if (isIE) {
		conH = document.body.scrollHeight;
		winH = document.body.clientHeight;
	} else {
		conH = document.height;
		winH = window.innerHeight;
	}
	var myHeight = winH - conH - footerH + browserH;
	if (myHeight > 60) {
		var footerSpacer = document.getElementById("footerSpacer");
		footerSpacer.setAttribute('height', myHeight);
	}
}

function openAbout(windowTitle) {
	var content = $('about').innerHTML;
	// NOTE: The PWC docs say the 'closable' option defaults to true, but this
	// does not appear to be the case.
	var windowOptions = {
		className : WINDOW_THEME,
		title : windowTitle,
		width : 296,
		height : 164,
		closable : true,
		minimizable : false,
		maximizable : false,
		resize : false,
		draggable : false,
		effectOptions : {
			duration : 0.25
		}
	};
	Dialog.alert(content, windowOptions);
}

/**
 * Sends a click event to the anchor element with the specified id.
 * See http://wiki.apache.org/myfaces/JavascriptWithJavaServerFaces.
 *
 * @param anchorId the id of an anchor element
 */
function clickAnchor(anchorId) {
	var anchor = document.getElementById(anchorId);
	if (document.createEvent) {
		var event = document.createEvent('MouseEvents');
		event.initEvent('click', true, false);
		anchor.dispatchEvent(event);
	} else if (document.createEventObject) {
		anchor.fireEvent('onclick');
	}
}

function hidediv(elementId) {
	document.getElementById(elementId).style.visibility = 'hidden';
	document.getElementById(elementId).style.display = 'none';
}

function showdiv(elementId) {
	document.getElementById(elementId).style.visibility = 'visible';
	document.getElementById(elementId).style.display = 'block';
}

function clickRadio(radioName, valueToClick) {
	var radioSet = document.getElementsByName(radioName);
	var i;
	for (i = 0; i < radioSet.length; i++) {
		if (radioSet.item(i).value == valueToClick) {
			radioSet.item(i).click();
		}
	}
}

function ignoreEnterKey(evt) {
	var e = (evt) ? evt : window.event;
	var charCode = (e.charCode) ? e.charCode
			: ((e.which) ? e.which : e.keyCode);
	if (charCode == 13 || charCode == 3) {
		e.returnValue = false;
		e.cancel = true;
		return false;
	} else {
		return true;
	}

}

function updateDependent(e, dep, disableValue) {
	if (isIE) {
		e = event.srcElement;
	}

	var depElement = document.getElementById(dep);
	if (e.value == disableValue) {
		depElement.readonly = true;
		depElement.disabled = true;
		depElement.checked = false;

	} else {
		depElement.readonly = false;
		depElement.disabled = false;
	}
}

function clickAlreadySelectedElements() {
	var allForms = document.forms;
	for (i = 0; i < allForms.length; i++) {
		var form = allForms[i];
		for (j = 0; j < form.elements.length; j++) {
			var element = form.elements[j];
			if (element.type == 'radio' || element.type == 'select-multiple'
					|| element.type == 'select-one') {
				if (element.checked) {
					element.click();
				}
			}
		}
	}
}

function addWindowOnLoadEvent(newMethod) {
	if (typeof window.onload != 'function') {
		window.onload = newMethod;
	} else {
		var oldMethod = window.onload;
		window.onload = function() {
			if (oldMethod) {
				oldMethod();
			}
			newMethod();
		}
	}
}

function addWindowOnResizeEvent(newMethod) {
	if (typeof window.onresize != 'function') {
		window.onresize = newMethod;
	} else {
		var oldMethod = window.onresize;
		window.onresize = function() {
			if (oldMethod) {
				oldMethod();
			}
			newMethod();
		}
	}
}

function getElementCrossBrowser(elementId) {
	var elementResult;
	if (document.getElementById) {
		elementResult = document.getElementById(elementId);
	} else if (document.all) {
		elementResult = document.all[elementId];
	}
	return elementResult;
}

function getElementLeftPos(e) {
	var xPos = e.offsetLeft;
	var tempEl = e.offsetParent;
	while (tempEl != null) {
		xPos += tempEl.offsetLeft;
		tempEl = tempEl.offsetParent;
	}
	return xPos;
}

function getElementTopPos(e) {
	var yPos = e.offsetTop;
	var tempEl = e.offsetParent;
	while (tempEl != null) {
		yPos += tempEl.offsetTop;
		tempEl = tempEl.offsetParent;
	}
	return yPos;
}

function keepCentered(elementId) {
	var elementToBeCentered = getElementCrossBrowser(elementId);
	var browserWidth = getBrowserWidth();
	var browserHeight = getBrowserHeight();

	var x = (browserWidth / 2) - (elementToBeCentered.offsetWidth / 2);
	var y = (browserHeight / 2) - (elementToBeCentered.offsetHeight / 2);

	// alert('keepCentered for ' + elementId + ' @ (' + x + ',' + y + ')');
	elementToBeCentered.style.top = (y + 'px');
	elementToBeCentered.style.left = (x + 'px');
}

function sizeAppropriately(elementId) {
	var elementToBeSized = getElementCrossBrowser(elementId);
	elementToBeSized.style.width = '600px';

	var browserHeight = getBrowserHeight();
	var height = browserHeight - 200;
	// alert('sizeAppropriately for ' + elementId + ' @ height of ' + height);
	elementToBeSized.style.height = (height + 'px');
}

function getBrowserWidth() {
	var myWidth = 0;
	if (typeof (window.innerWidth) == 'number') {
		// Non-IE
		myWidth = window.innerWidth;
	} else if (document.documentElement
			&& (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
		// IE 6+ in 'standards compliant mode'
		myWidth = document.documentElement.clientWidth;
	} else if (document.body
			&& (document.body.clientWidth || document.body.clientHeight)) {
		// IE 4 compatible
		myWidth = document.body.clientWidth;
	}
	return myWidth;
}

function getBrowserHeight() {
	var myHeight = 0;
	if (typeof (window.innerWidth) == 'number') {
		// Non-IE
		myHeight = window.innerHeight;
	} else if (document.documentElement
			&& (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
		// IE 6+ in 'standards compliant mode'
		myHeight = document.documentElement.clientHeight;
	} else if (document.body
			&& (document.body.clientWidth || document.body.clientHeight)) {
		// IE 4 compatible
		myHeight = document.body.clientHeight;
	}
	return myHeight;
}

function changeComboBox(selectElementId, value) {
	var selectElement = getElementCrossBrowser(selectElementId);
	var i;
	selectElement[0].selected = true;
	for (i = 0; i < selectElement.length; i++) {
		if (selectElement[i].value == value) {
			selectElement[i].selected = true;
		}
	}
}

function manageStartDivs(component) {
	if (component.value == "immediate" ) {
		hidediv('recur');
		hidediv('end');
	} else {
		showdiv('recur');
		hidediv('end');
	}
}

function manageRecurDivs(component) {
	if (component.value == "never" ) {
		hidediv('end');
	} else {
		showdiv('end');
	}
}

function hideInitialDivs() {
	hidediv('recur');
	hidediv('end');
}
/**
* set attribute to given value for element specified by ID
* @param elementId element ID
* @param attribute name
* @param value - attribute value to set
*/
var setAttribute = function(elementId, attribute, value) {
 var el = document.getElementById(elementId);
 if (el && el != null) {
   el.setAttribute(attribute, value);
 } else {
   console.log('Failed to set ['+attribute+'='+value+'] : node id=[' + elementId + '] not found');
 }
}
/**
* set 'action' attributes to elements based on given actionMap. If actionMap is for example
* {elementID:'foo'}, then attribute 'action' is going to be set to 'foo' for element with id 'elementID'
*
* @param actionMap map<elementId,actionValue>
*/
var setFormActions = function(actionMap) {
    for (key in actionMap) {
        if (actionMap.hasOwnProperty(key)) {
            setAttribute(key,'action',actionMap[key]);
        }
    }
}

/**
 * WindowResizeTracker can be used by scripts that need to keep track
 * of the current window/page sizes and their changes.
 * This variable is a "singleton", don't create new instances of it using "new",
 * but rather use directly this variable and its properties and methods.
 */
var WindowResizeTracker = {

	/** The current window and page sizes */
	currentSizes : {
		pageWidth : 0,
		pageHeight : 0,
		windowWidth : 0,
		windowHeight : 0
	},

	/** How the window/page size changed since last window resize event. */
	currentDeltas : {
		pageWidth : 0,
		pageHeight : 0,
		windowWidth : 0,
		windowHeight : 0
	},

	/** The function passed to this method will be invoked on every window resize and load event. */
	addListener : function(method) {
		if (!WindowResizeTracker.listeners) {
			WindowResizeTracker.listeners = [];
		}

		WindowResizeTracker.listeners.push(method);
	},

	init : function() {
		WindowResizeTracker.currentSizes = WindowUtilities.getPageSize();
		if (WindowResizeTracker.listeners) {
			WindowResizeTracker.listeners.each( function(listener) {
				listener();
			});
		}
		Event.observe(window, "resize", WindowResizeTracker._fire);
	},

	_fire : function() {
		var newSizes = WindowUtilities.getPageSize();
		for ( var i in newSizes) {
			WindowResizeTracker.currentDeltas[i] = newSizes[i]
					- WindowResizeTracker.currentSizes[i];
		}
		WindowResizeTracker.currentSizes = newSizes;

		if (WindowResizeTracker.listeners) {
			WindowResizeTracker.listeners.each( function(listener) {
				listener();
			});
		}
	}
};

// initialize the resize tracker
Event.observe(window, "load", WindowResizeTracker.init);
