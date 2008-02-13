function replaceButtons(sel, side) {
	var index = sel.selectedIndex;

	if (!noDelete)
		replaceButton(side + "Delete", side + "Nav", "on", "_del");
	
	replaceButton(side + "Up", side + "Nav", "on", "_up");
	replaceButton(side + "Down", side + "Nav", "on", "_dn");
	
	if (index == 0)
		replaceButton(side + "Up", side + "Nav", "off", "_up");
		
	if (index == sel.length-1)
		replaceButton(side + "Down", side + "Nav", "off", "_dn");

	if (sel.length==0) {
		replaceButton(side + "Up", side + "Nav", "off", "_up");
		replaceButton(side + "Down", side + "Nav", "off", "_dn");
		if (!noDelete)
			replaceButton(side + "Delete", side + "Nav", "off", "_del");
	}
}

function replaceAddButton(sel, side) {
	var index = sel.selectedIndex;
	
	if (index==0)
		replaceButton(side + "ContentDiv", side + "ContentTd", "off", "_add");
	else
		replaceButton(side + "ContentDiv", side + "ContentTd", "on", "_add");
}

function replaceButton(divId, tdId, tdState, imgName) {
	var functName;
	if (imgName == "_up")
		functName = moveUp;
	else if (imgName == "_dn")
		functName = moveDown;
	else if (imgName == "_del")
		functName = deleteItem;
	else if (imgName == "_add")
		functName = addItem;
	
	var newDiv = document.createElement("DIV");
	newDiv.setAttribute("id", divId);
	
	var newImg;
	var fullPath = imagePath + "dash_movecontent";
	var imgState = "-on";
	if (tdState=="off")
		imgState = "-off";
	var imgName;
	var width = "20";
	var height = "20";
	
	fullPath += imgName + imgState + ".gif";
	newImg = getNewImage (fullPath, width, height, "0");

	if (tdState == "on") {
		var newA = document.createElement("A");
		newA.setAttribute("href", "#");
		newImg.setAttribute("name", divId);

		if (isIE)
			newImg.attachEvent("onclick", functName);
		else
			newImg.addEventListener("click", function(e) {functName(this)}, false);
		
		newA.appendChild(newImg);
		newDiv.appendChild(newA);
	}
	
	else
		newDiv.appendChild(newImg);
	
	var td = document.getElementById(tdId);
	var oldDiv = document.getElementById(divId);
	if (td!=null)
		td.replaceChild(newDiv,oldDiv);
}

function getSel(eSide) {
	if (eSide == "left")
		selId = "leftSel";
	else
		selId = "rightSel";
	
	return (document.getElementById(selId));
}

function getSide(e) {
	var eName = e.getAttribute("name");
	var eSide = eName.substring(0,4);
	if (eSide!="left")
		eSide="right";
	return eSide;
}

function moveUp(e) {
	if (isIE)
		e = event.srcElement;
	
	var eSide = getSide(e);
	var sel = getSel(eSide);
	var selIndex = sel.selectedIndex;
	var selText = sel[selIndex].text;
	var selValue = sel[selIndex].value;
	
	sel[selIndex].text = sel[selIndex-1].text;
	sel[selIndex].value = sel[selIndex-1].value;
	
	sel[selIndex-1].text = selText;
	sel[selIndex-1].value = selValue;
	
  sel.options[selIndex].selected = false;
  sel.options[selIndex-1].selected = true;
	replaceButtons(sel, eSide);
}

function moveDown(e) {
	if (isIE)
		e = event.srcElement;
	
	var eSide = getSide(e);
	
	var sel = getSel(eSide);
	var selIndex = sel.selectedIndex;
	var selText = sel[selIndex].text;
	var selValue = sel[selIndex].value;
	
	sel[selIndex].text = sel[selIndex+1].text;
	sel[selIndex].value = sel[selIndex+1].value;
	
	sel[selIndex+1].text = selText;
	sel[selIndex+1].value = selValue;
	
	sel.options[selIndex].selected = false;
  sel.options[selIndex+1].selected = true;
	replaceButtons(sel, eSide);
}

function deleteItem(e) {
	if (isIE)
		e = event.srcElement;
	
	var eSide = getSide(e);
	
	var sel = getSel(eSide);
	var selIndex = sel.selectedIndex;
	var selText = sel[selIndex].text;
	var selValue = sel[selIndex].value;
	
	sel[selIndex] = null;
	
	var selContent = document.getElementById(eSide + "Content");
	var len = selContent.length;
	
	selContent.options[0] = new Option("Select content...", "null");
	selContent.options[len] = new Option(selText, selValue);
	selContent.selectedIndex = 0;
	
	sel.selectedIndex = selIndex-1;
	if (sel.selectedIndex<0)
		sel.selectedIndex=0;
	replaceButtons(sel, eSide);
}

function addItem(e) {
	if (isIE)
		e = event.srcElement;
	
	var eSide = getSide(e);
	var selContent = document.getElementById(eSide + "Content");
	var selIndex = selContent.selectedIndex;
	var selText = selContent[selIndex].text;
	var selValue = selContent[selIndex].value;
	
	selContent[selIndex] = null;
	
	var selTarget = document.getElementById(eSide + "Sel");
	var len = selTarget.length;
	
	selTarget.options[len] = new Option(selText, selValue);
	selTarget.selectedIndex = selTarget.length-1;
	
	selContent.selectedIndex = selIndex-1;
	replaceButtons(selTarget, eSide);
	replaceAddButton(selContent, eSide);
}

function selectAllOptions(selId) {
  var sel = document.getElementById(selId);
  
  for(i=0; i<sel.length; i++) {
  	sel.options[i].selected = true;
  }
}