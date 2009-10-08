var SELECT;
var WIDTH;
var SIDE;

function getSide() {
	return SIDE;
}

function setSide(side) {
  SIDE = side;
}

function replaceButtons(sel, side) {
	if (getSide()!=null) {
    var oldSide = getSide();
    var oldSel = document.getElementsByName(getSide())[0];
    var oldIndex = oldSel.selectedIndex;
    
    if (oldSide != side && oldIndex != -1) {
      oldSel[oldIndex].selected=false;
      
      replaceButton(oldSide + "Up", oldSide + "Nav", "off", "_up");
  		replaceButton(oldSide + "Down", oldSide + "Nav", "off", "_dn");
      replaceButton(oldSide + "Delete", oldSide + "Nav", "off", "_del");
    }
  }
  
  setSide(side);
  SELECT = sel;
  var index = sel.selectedIndex;

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

function moveUp(e) {
	if (isIE)
		e = event.srcElement;

  var sel = SELECT;
  var selIndex = sel.selectedIndex;
  var selText = sel[selIndex].text;
  var selValue = sel[selIndex].value;
  
  sel[selIndex].text = sel[selIndex-1].text;
  sel[selIndex].value = sel[selIndex-1].value;
  
  sel[selIndex-1].text = selText;
  sel[selIndex-1].value = selValue;
  
  sel.options[selIndex].selected = false;
  sel.options[selIndex-1].selected = true;
  replaceButtons(sel, getSide());
}

function moveDown(e) {
  if (isIE)
    e = event.srcElement;
  
  var sel = SELECT;
  var selIndex = sel.selectedIndex;
  var selText = sel[selIndex].text;
  var selValue = sel[selIndex].value;
  
  sel[selIndex].text = sel[selIndex+1].text;
  sel[selIndex].value = sel[selIndex+1].value;
  
  sel[selIndex+1].text = selText;
  sel[selIndex+1].value = selValue;
  
  sel.options[selIndex].selected = false;
  sel.options[selIndex+1].selected = true;
  replaceButtons(sel, getSide());
}

function deleteItem(e) {
  if (isIE)
    e = event.srcElement;
  
  var sel = SELECT;
  var selIndex = sel.selectedIndex;
  
  sel[selIndex] = null;
  
  sel.selectedIndex = selIndex-1;
  if (sel.selectedIndex<0)
    sel.selectedIndex=0;
  replaceButtons(sel, getSide());
}

function addItem(side) {
	setSide(side);
  var textBox = document.getElementById(side + "Content");
  textBox.focus();
  textBox.select();
	var textValue = textBox.value;
	
	var selTarget = document.getElementsByName(side)[0];
  
	var len = selTarget.length;
	
	selTarget.options[len] = new Option(textValue, textValue);
	if (selTarget.length > 1)
    selTarget.options[selTarget.length-2].selected = false;
  selTarget.options[selTarget.length-1].selected = true;
	
  replaceButtons(selTarget, side);
}

function selectAllOptions() {
  var selArr = document.getElementsByTagName("select");

  if (selArr!=null) {
    for (var i = 0; i < selArr.length; i++) {
      var e = selArr[i];
      if (e.getAttribute(classStr) == "multiSelect") {
        for(var k=0; k<e.length; k++) {
        	e.options[k].selected = true;
        }
      }
    }
  }
}