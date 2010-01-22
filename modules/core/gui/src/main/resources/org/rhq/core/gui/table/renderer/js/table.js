/**
 * @param selectAllCheckbox    the calling AllSelect object instance use to determine whether
 *                   or select or deselect all of the objects with the name selectName
 * @param selectCheckboxName name of the dom instances that should be checked / unchecked
 */
function selectAll(selectAllCheckbox, selectCheckboxName) {
	var selectedCheckboxes = document.getElementsByName(selectCheckboxName);
	for (var i = 0; i < selectedCheckboxes.length; i++) {
		var selectedCheckbox = selectedCheckboxes.item(i);
		if (!selectedCheckbox.disabled) {
			selectedCheckbox.checked = selectAllCheckbox.checked;
		}
	}
	updateButtons(selectCheckboxName);
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
function updateButtons(rowSelectorCheckboxName) {
	var selectedCheckboxCount = countSelectedCheckboxes(rowSelectorCheckboxName);
	var inputs = document.getElementsByTagName("input");
	for (var i = 0; i < inputs.length; i++) {
		var input = inputs.item(i);
        // TODO: Check if the input's type is submit, image, or reset.
        var target = input.getAttribute("target");
        if (target == rowSelectorCheckboxName) {
            var min = input.getAttribute("minimum");
            var max = input.getAttribute("maximum");
            input.disabled = (min && selectedCheckboxCount < min) ||
                             (max && selectedCheckboxCount > max);
		}
	}
}

/**
 * Returns the number of checkboxes with the given name that are currently selected
 *
 * @param checkboxName name of the dom instances that should be counted for selections
 */
function countSelectedCheckboxes(checkboxName) {
	var total = 0;
	var selectedCheckBoxes = document.getElementsByName(checkboxName);
	for (var i = 0; i < selectedCheckBoxes.length; i++) {
		if (selectedCheckBoxes.item(i).checked) {
			total += 1;
		}
	}
	return (total);
}
