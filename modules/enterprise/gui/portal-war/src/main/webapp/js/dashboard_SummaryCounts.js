
/*-- START dashboard_SummaryCounts.js --*/
function checkParent(e) {
  if (e.checked == false) {
    var parentGroup = e.getAttribute(classStr);
    var endPosition = parentGroup.indexOf("Parent");
    var group = parentGroup.substring(0, endPosition);
    
    var uList = e.form;
    var len = uList.elements.length;
    
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];

      if (e.getAttribute(classStr)==group)
        e.checked=false;
      if (e.getAttribute(classStr)==(group + "CheckAll"))
        e.checked=false;
    }
  }
}

function checkChild(e) {
  var group = e.getAttribute(classStr);
  var uList = e.form;
  var len = uList.elements.length;
  
  if (e.checked == true) {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)==(group + "Parent"))
        e.checked=true;
    }
  }
    
  else {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)==(group + "CheckAll"))
        e.checked=false;
    }
  }
}

function ToggleAll(e, group) {
  var uList = e.form;
  var len = uList.elements.length;
  
  if (e.checked == true) {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];

      if (e.getAttribute(classStr)== group)
        e.checked=true;
      if (e.getAttribute(classStr)==(group + "Parent"))
        e.checked=true;
    }
  }
  
  else {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)== group)
        e.checked=false;
    }
  }
}
/*-- END dashboard_SummaryCounts.js --*/

