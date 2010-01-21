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
