
/*-- START functions.js --*/

/*------------------------------------ start BROWSER DETECTION ------------------------------------*/
var isIE = false;
var isMacIE = false;
var isNS = false;

if (navigator.appName.indexOf("Microsoft")!=-1) {
	isIE = true;
	if (navigator.platform.indexOf("Mac")!=-1)
		isMacIE = true;
}
else
	isNS = true;

/*------------------------------------ end BROWSER DETECTION ------------------------------------*/	

/* fixes IE-Netscape DOM incompatibility
e.getAttribute("class") - works in NS, not in IE
e.getAttribute("className") - works in IE, not in NS
*/
var classStr = "class";
if (isIE)
	classStr = "className";

/*------------------------------------ start WidgetProperties ------------------------------------*/
// call this at the top of the widget (so that it's called once
// per widget)
function initializeWidgetProperties(widgetInstanceName) {
     var widgetProperties = new Array();
     widgetProperties["name"] = widgetInstanceName;
     widgetProperties["numFromSelected"] = 0; //for add/remove widgets
	 widgetProperties["numToSelected"] = 0; //for add/remove widgets
	 widgetProperties["numSelected"] = 0; //for list widgets
     pageData[widgetInstanceName] = widgetProperties;
}

// call from any js when you need all of a widget's properties
function getWidgetProperties(widgetInstanceName) {
	 return pageData[widgetInstanceName];
}

// call from any js when you need a property (ex: numFromSelected) for a
// particular widget
function getWidgetProperty(widgetInstanceName, propertyName) {
	var widgetProperties = pageData[widgetInstanceName];
	
	if (widgetProperties == null) {
	// this would only happen if the widget was not initialized
		initializeWidgetProperties(widgetInstanceName);
		widgetProperties = pageData[widgetInstanceName];
	}

     return widgetProperties[propertyName];
}

// call from any js when you need to set a property (ex:
// numFromSelected) for a particular widget
function setWidgetProperty(widgetInstanceName, propertyName, propertyValue) {
	var widgetProperties = pageData[widgetInstanceName];

	if (widgetProperties == null) {
		// this would only happen if the widget was not initialized
		intializeWidgetProperties(widgetInstanceName);
		widgetProperties = pageData[widgetInstanceName];
	}
	
	widgetProperties[propertyName] = propertyValue;
}

function clearIfAnyChecked(name) {
  var eArr = document.getElementsByTagName("input");

  for (i=0; i<eArr.length; i++) {
    if ((name != null) && (eArr[i].name != name)) {
        continue;
    }

    if (eArr[i].checked) {
      eArr[i].checked = false;
    }
  }
}

function ClearText(e) {
  if (isIE)
		e = event.srcElement;
    
  e.value="";
}

/*----------- start SHOW/HIDE DIV -----------*/
function hideDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	thisDiv.style.display = "none";
}

function showDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	thisDiv.style.display = "block";
}

function toggleDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	if ( thisDiv.style.display == "none") {
		thisDiv.style.display = "block";
	}
	else {
		thisDiv.style.display = "none";
	}
}
/*----------- end SHOW/HIDE DIV -----------*/

/*------------------------------------ end WidgetProperties ------------------------------------*/

function clickSelect(formName, inputName, inputValue) {
	var form = document.forms[formName];
	form[inputName].value = inputValue;
}

function clickAdd(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "add.x");
	document.forms[formName].appendChild(newInput);
}

function clickRemove(formName, index) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "remove.x");
	newInput.setAttribute("value", index);
	document.forms[formName].appendChild(newInput);
}

function clickAIPlatformImport(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "includeForImport.x");
	document.forms[formName].appendChild(newInput);
}

function clickAIPlatformIgnore(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "ignoreForImport.x");
	document.forms[formName].appendChild(newInput);
}

function clickLink(formName, inputName, method) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", inputName + ".x");
	document.forms[formName].appendChild(newInput);
	if (method) {
	   document.forms[formName].method = method;
	}
}

function getNumCheckedByClass(uList, className) {
    var len = uList.elements.length;
    var numCheckboxes = 0;

    for (var i = 0; i < len; i++) {
        var e = uList.elements[i];
        if (e.getAttribute(classStr)==className && e.checked) {
            numCheckboxes++;
        }
    }

    return numCheckboxes;
}

function getParent(e) {
	return e.parentNode.parentNode;
}

function getForm(e) {
	var form = "error";
	
	while (form == "error") {
		e = e.parentNode;
		if (e.tagName == "FORM")
			form = e;
	}
	
	return form;
}

function getDivName (e) {
	var divName = "error";
	
	while (divName == "error") {
		e = e.parentNode;
		if (e.tagName == "DIV")
			divName = e.getAttribute("id");
	}
	
	return divName;
}

function imageSwap(e, btnName, state) {
	var btn = e;
	btn.src = btnName + state + ".gif";
}

/* ---------------------------- start TOGGLE ---------------------------- */

function highlight(e) {
	var parent = getParent(e);
	
	if (parent) {
		parent.className = "ListRowSelected";
	}
}

function unhighlight(e) {
	var parent = getParent(e);

	if (parent) {
		parent.className = "ListRow";
	}
}

function Check(e) {
	e.checked = true;
	highlight(e);
}
	
function Clear(e) {
	e.checked = false;
	unhighlight(e);
}
/* ---------------------------- end TOGGLE ---------------------------- */

/*--------------------------- START getters for individual elements ------------------------------*/

function getNewImage (iSrc, iW, iH, iB) {
	var newImage = document.createElement("IMG");
	newImage.setAttribute("src", iSrc);
	if (iW)
    newImage.setAttribute("width", iW);
	if (iH)
    newImage.setAttribute("height", iH);
	newImage.setAttribute("border", iB);
	
	return newImage;
}

function goToSelectLocationAndRemove (e, param, base, remove) {
    var reg = new RegExp("" + remove + "=[a-zA-Z0-9]+");
    
    if ( reg.test(base) )
    {
    	base = base.replace(reg, "");
    }
    
    goToSelectLocation(e, param, base);
}

function goToSelectLocation (e, param, base) {

    var newParam = param + "=" + e.options[e.selectedIndex].value;
    var newUrl;
    
    var reg = new RegExp("" + param + "=[a-zA-Z0-9]+");
    
    if ( reg.test(base) )
    {
    	newUrl = base.replace(reg, newParam);
    }
    else
    {
        var sep = base.indexOf('?') >=0 ? '&' : '?';
        newUrl = base + sep + newParam;
    }

    window.location = newUrl;

    // var sep = base.indexOf('?') >=0 ? '&' : '?';
    // window.location = base + sep + param + "=" + e.options[e.selectedIndex].value;
}

function goToLocationSelfAndElement(param,elementName,base) {
    var sep = base.indexOf("?") >=0 ? '&' : "?";
    var val = document.forms[0].elements[elementName].value;
    window.location = base + sep + param + "=" + val;
}

/*--------------------------- END getters for individual elements ------------------------------*/

/*--------------------------- BEGIN helpers for CSS stuff ------------------------------*/
function hideFormElements() {
  for (i=0; i<document.forms.length; ++i) {
    for (j=0; j<document.forms[i].elements.length; ++j) {
      document.forms[i].elements[j].style.visibility = "hidden";
    }
  }
}

function showFormElements() {
  for (i=0; i<document.forms.length; ++i) {
    for (j=0; j<document.forms[i].elements.length; ++j) {
      document.forms[i].elements[j].style.visibility = "visible";
    }
  }
}

function whereAmI(elem, elems) { 
  for (var i = 0; i < elems.length; i++) {
    if (elems[i] == elem) { return i }
  }
}

function moveElementUp(elem, root) {
    var elems = root.getElementsByTagName("li");
    var pos = whereAmI(elem, elems);
    if (pos != 0) { 
      root.removeChild(elem);
      root.insertBefore(elem, elems[pos-1]);
    }
}

function moveElementDown(elem, root) {
    var elems = root.getElementsByTagName("li");
    var pos = whereAmI(elem, elems);
    if (pos < (elems.length - 1)) { 
      var after = elems[pos + 1];
      root.removeChild(after);
      root.insertBefore(after, elem);
    }
}
/*--------------------------- END helpers for CSS stuff ------------------------------*/

/*--------------------------- BEGIN switch content for dynamic show/hide -------------*/


/***********************************************
* Switch Content script -  Dynamic Drive (www.dynamicdrive.com)
* This notice must stay intact for legal use. Last updated April 2nd, 2005.
* Visit http://www.dynamicdrive.com/ for full source code
***********************************************/

var enablepersist="off" //Enable saving state of content structure using session cookies? (on/off)
var collapseprevious="no" //Collapse previously open content when opening present? (yes/no)

var contractsymbol='Hide' //HTML for contract symbol. For image, use: <img src="whatever.gif">
var expandsymbol='Show' //HTML for expand symbol.


if (document.getElementById){
document.write('<style type="text/css">')
document.write('.switchcontent{display:none;}')
document.write('</style>')
}

function getElementbyClass(rootobj, classname){
var temparray=new Array()
var inc=0
var rootlength=rootobj.length
for (i=0; i<rootlength; i++){
if (rootobj[i].className==classname)
temparray[inc++]=rootobj[i]
}
return temparray
}

function sweeptoggle(ec){
var thestate=(ec=="expand")? "block" : "none"
var inc=0
while (ccollect[inc]){
ccollect[inc].style.display=thestate
inc++
}
revivestatus()
}


function contractcontent(omit){
var inc=0
while (ccollect[inc]){
if (ccollect[inc].id!=omit)
ccollect[inc].style.display="none"
inc++
}
}

function expandcontent(curobj, cid){
var spantags=curobj.getElementsByTagName("SPAN")
var showstateobj=getElementbyClass(spantags, "showstate")
if (ccollect.length>0){
if (collapseprevious=="yes")
contractcontent(cid)
document.getElementById(cid).style.display=(document.getElementById(cid).style.display!="block")? "block" : "none"
if (showstateobj.length>0){ //if "showstate" span exists in header
if (collapseprevious=="no")
showstateobj[0].innerHTML=(document.getElementById(cid).style.display=="block")? contractsymbol : expandsymbol
else
revivestatus()
}
}
}

function revivecontent(){
contractcontent("omitnothing")
selectedItem=getselectedItem()
selectedComponents=selectedItem.split("|")
for (i=0; i<selectedComponents.length-1; i++)
document.getElementById(selectedComponents[i]).style.display="block"
}

function revivestatus(){
var inc=0
while (statecollect[inc]){
if (ccollect[inc].style.display=="block")
statecollect[inc].innerHTML=contractsymbol
else
statecollect[inc].innerHTML=expandsymbol
inc++
}
}

function get_cookie(Name) { 
var search = Name + "="
var returnvalue = "";
if (document.cookie.length > 0) {
offset = document.cookie.indexOf(search)
if (offset != -1) { 
offset += search.length
end = document.cookie.indexOf(";", offset);
if (end == -1) end = document.cookie.length;
returnvalue=unescape(document.cookie.substring(offset, end))
}
}
return returnvalue;
}

function getselectedItem(){
if (get_cookie(window.location.pathname) != ""){
selectedItem=get_cookie(window.location.pathname)
return selectedItem
}
else
return ""
}

function saveswitchstate(){
var inc=0, selectedItem=""
while (ccollect[inc]){
if (ccollect[inc].style.display=="block")
selectedItem+=ccollect[inc].id+"|"
inc++
}

document.cookie=window.location.pathname+"="+selectedItem
}

function do_onload(){
uniqueidn=window.location.pathname+"firsttimeload"
var alltags=document.all? document.all : document.getElementsByTagName("*")
ccollect=getElementbyClass(alltags, "switchcontent")
statecollect=getElementbyClass(alltags, "showstate")
if (enablepersist=="on" && ccollect.length>0){
document.cookie=(get_cookie(uniqueidn)=="")? uniqueidn+"=1" : uniqueidn+"=0" 
firsttimeload=(get_cookie(uniqueidn)==1)? 1 : 0 //check if this is 1st page load
if (!firsttimeload)
revivecontent()
}
if (ccollect.length>0 && statecollect.length>0)
revivestatus()
}

if (window.addEventListener)
window.addEventListener("load", do_onload, false)
else if (window.attachEvent)
window.attachEvent("onload", do_onload)
else if (document.getElementById)
window.onload=do_onload

if (enablepersist=="on" && document.getElementById)
window.onunload=saveswitchstate

/*--------------------------- END switch content for dynamic show/hide -------------*/


/*-- END functions.js --*/
