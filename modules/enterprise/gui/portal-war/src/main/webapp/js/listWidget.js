
/*-- START listWidget.js --*/
function ReplaceButton(divId, tdId, tdState, imageId, btnFunction)
{
	var td = document.getElementById(tdId);
	var oldDiv = document.getElementById(divId);
	
	var newDiv = document.createElement("div");
	newDiv.setAttribute("id", divId);

   var newButton = CreateImgButton(tdState, imageId, btnFunction);
   newDiv.appendChild(newButton);
	
	if (td != null)
		td.replaceChild(newDiv, oldDiv);
}

function CreateImgButton(tdState, imageId, btnFunction)
{
	var imgState = "";
	if (tdState=="off")
		imgState = "_gray";

	var imgName = imagePath + "tbb_" + imageId;

	var inputName = btnFunction;

	var imgPath = imgName + imgState + ".gif";
	var newImg = getNewImage (imgPath, false, false, "0");

	if (tdState == "on")
   {
		var newInput = document.createElement("input");
		// can't call setAttribute here because doesn't have desired
		// effect when running inside of HtmlUnit
		//newInput.setAttribute("type", "image");
		newInput.type = "image";
		newInput.setAttribute("src", imgPath);
		newInput.setAttribute("name", inputName);
		return newInput;
	}
	else
   {
		return newImg;
	}
}

function ToggleButtons(widgetInstanceName, prefix, isRemove, form) 
{   
   var imgName;
   var btnFunction;
   if (isRemove) 
   {
      imgName = "removefromlist";
      btnFunction = "remove";
   }
   else 
   {
	  var useCss = (getWidgetProperty(widgetInstanceName, "buttonType") == "css");
      imgName = (useCss) ? "uninventory" : "delete";
      btnFunction = (useCss) ? "uninventory" : "delete";
   }
  
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}


   if (numSelected == 0) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd",
                      "off", imgName, btnFunction);
		ReplaceButton(prefix + "GroupButtonDiv", prefix + "GroupButtonTd",
                      "off", "group", "group");
        if (goButtonLink != null)
            ReplaceGoButton(false);
	}
	
	else if (numSelected >= 1) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd",
                      "on", imgName, btnFunction);
		ReplaceButton(prefix + "GroupButtonDiv", prefix + "GroupButtonTd",
                      "on", "group", "group");
        if (goButtonLink!=null)
            ReplaceGoButton(true);
	}
}

function ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction) {
    if (btnFunction == "remove")
        var imgPrefix = "removeFrom";
    else if (btnFunction == "add")
        var imgPrefix = "addTo";
  
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
    
    if (numSelected < 0) {
        numSelected = getNumChecked(form);
        setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
    }
    
    var mode = "off";
    if (numSelected >= 1)
        mode = "on";

    ReplaceButton(prefix + "setBaselinesDiv", prefix + "setBaselinesTd", mode, "setBaselines", "userset");
    ReplaceButton(prefix + "enableAutoBaselinesDiv", prefix + "enableAutoBaselinesTd", mode, "enableAutoBaselines", "enable");
    ReplaceButton(prefix + imgPrefix + "FavoritesDiv", prefix + imgPrefix + "FavoritesTd", mode, imgPrefix + "Favorites", btnFunction);
    ReplaceButton(prefix + "chartSelectedMetricsDiv", prefix + "chartSelectedMetricsTd", mode, "chartselectedmetrics", "chart");
}

function ToggleButtonsCompare(widgetInstanceName, prefix, form) {
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected < 2) {
		ReplaceButton(prefix + "compareDiv", prefix + "compareTd", "off", "compareMetricsOfSelected", "compare");
	}
	
	else if (numSelected >= 2) {
            ReplaceButton(prefix + "compareDiv", prefix + "compareTd", "on", 
                "compareMetricsOfSelected", "compare");
            form.setAttribute("method","get");

	}
}

function ToggleButtonsRemoveGo(widgetInstanceName, prefix, form) {
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected == 0) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd", "off", "disablecollection", "remove");
    ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "off", "go", "ok");
	}
	
	else if (numSelected >= 1) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd", "on", "disablecollection", "remove");
    ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "on", "go", "ok");
	}
}

function ToggleButtonsGroup(widgetInstanceName, prefix, form) {
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected == 0) {
		ReplaceButton(prefix + "ApplySelectedDiv", prefix + "ApplySelectedTd", "off", "applyselectedtomembers", "apply");
    ReplaceButton(prefix + "RemoveSelectedDiv", prefix + "RemoveSelectedTd", "off", "removeselectedfrommembers", "apply");
    ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "off", "go", "ok");
	}
	
	else if (numSelected >= 1) {
		ReplaceButton(prefix + "ApplySelectedDiv", prefix + "ApplySelectedTd", "on", "applyselectedtomembers", "apply");
    ReplaceButton(prefix + "RemoveSelectedDiv", prefix + "RemoveSelectedTd", "on", "removeselectedfrommembers", "apply");
    ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "on", "go", "ok");
	}
}

function ToggleSelection(e, widgetProperties, isRemove) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		form.listToggleAll.checked = false;
	}
	
	ToggleButtons(widgetInstanceName, prefix, isRemove, form);
}

function ToggleSelectionTwoButtons(e, widgetProperties, btnFunction) {
  if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
        form.listToggleAll.checked = false;
	}
	ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
}

function ToggleAllSelectionTwoButtons(e, widgetProperties, subGroup,
                                      btnFunction) {
    // First toggle all buttons
    ToggleAllRemoveGo(e, widgetProperties, subGroup);

    if (isIE)
        e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    
    ToggleTwoButtons(widgetInstanceName, widgetInstanceName, e.form,
                     btnFunction);
}

function ToggleAllCompare(e, widgetProperties) {
    subGroup="availableListMember";

    if (isIE)
		e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
    var form = e.form;

    if (e.checked) {
        CheckAll(e, widgetInstanceName, subGroup);
    }
    else {
        ClearAll(e, widgetInstanceName, subGroup);
    }

    ToggleButtonsCompare(widgetInstanceName, prefix, form);
}

function ToggleSelectionCompare(e, widgetProperties) {
    if (isIE)
		e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
	
    var form = e.form;
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
    if (e.checked) {
        highlight(e);
        setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
    }
    else {
        unhighlight(e);
        var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
        setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
    }
	
    ToggleButtonsCompare(widgetInstanceName, prefix, form);
}

function ToggleRemoveGo(e, widgetProperties) {
	if (isIE)
		e = event.srcElement;
    
    var subGroup = e.getAttribute(classStr);
    var nameAll = subGroup + 'All';
    var checkAll = document.getElementsByName(nameAll)[0];

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		checkAll.checked = false;
	}
	
	ToggleButtonsRemoveGo(widgetInstanceName, prefix, form);
}

function ToggleGroup(e, widgetProperties) {
	if (isIE)
		e = event.srcElement;
    
  var subGroup = e.getAttribute(classStr);
  var nameAll = subGroup + 'All';
  var checkAll = document.getElementsByName(nameAll)[0];

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		checkAll.checked = false;
	}
	
	ToggleButtonsGroup(widgetInstanceName, prefix, form);
}

function ToggleAll(e, widgetProperties, isRemove, subGroup) {
    if (!subGroup)
        subGroup="listMember";

    if (isIE)
        e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleButtons(widgetInstanceName, prefix, isRemove);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleButtons(widgetInstanceName, prefix, isRemove);
	}
}

function ToggleAllRemoveGo(e, widgetProperties, subGroup) {
    if (isIE)
        e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
    
    if (e.checked) {
        CheckAll(e, widgetInstanceName, subGroup);
    }
    else {
        ClearAll(e, widgetInstanceName, subGroup);
    }

    ToggleButtonsRemoveGo(widgetInstanceName, prefix);
}

function ToggleAllChart(e, widgetProperties, btnFunction, subGroup) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
  var form = e.form;
	
	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
	}
}

function ToggleAllGroup(e, widgetProperties, subGroup) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleButtonsGroup(widgetInstanceName, prefix);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleButtonsGroup(widgetInstanceName, prefix);
	}
}

function CheckAll(e, widgetInstanceName, subGroup) {
	var uList = e.form;
	var len = uList.elements.length;
	var numCheckboxes = getWidgetProperty(widgetInstanceName, "numSelected");
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)==subGroup && e.checked == false) {
			Check(e);
			numCheckboxes++;
		}
	}
	
	setWidgetProperty(widgetInstanceName, "numSelected", numCheckboxes);
}
	
function ClearAll(e, widgetInstanceName, subGroup) {
	var uList = e.form;
	var len = uList.elements.length;
  var numCheckboxes = getWidgetProperty(widgetInstanceName, "numSelected");
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)==subGroup && e.checked == true) {
			Clear(e);
      numCheckboxes--;
		}
	}
	
	setWidgetProperty(widgetInstanceName, "numSelected", numCheckboxes);
}

function getNumChecked(uList) {
	var len = uList.elements.length;
	var numCheckboxes = 0;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)=="listMember" && e.checked) {
			numCheckboxes++;
		}
	}
		
	return numCheckboxes;	
}

function testCheckboxes(functionName, widgetInstanceName, hiddenElementId, className) {
  var e = document.getElementById(hiddenElementId);
  var thisForm = e.form;

  var numChecked = getNumCheckedByClass(thisForm, className);
  setWidgetProperty(widgetInstanceName, "numSelected", numChecked);
  
  if (functionName == "ToggleButtonsCompare")
    ToggleButtonsCompare(widgetInstanceName, widgetInstanceName, thisForm);
}

function ReplaceGoButton(condition) {
  var goLink = document.getElementById("goButtonLink");
  var goImg = document.getElementById("goButtonImg");
  
  if (condition == true) {
    goLink.setAttribute("href",goButtonLink);
    goImg.setAttribute("src",imagePath + "dash-button_go-arrow.gif");
  }
  else {
    goLink.setAttribute("href", "#");
    goImg.setAttribute("src",imagePath + "dash-button_go-arrow_gray.gif");
  }
}

/*-- END listWidget.js --*/

