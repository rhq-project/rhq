function roundedCorners() {
  var divs = document.getElementsByTagName('div');
  var rounded_divs = [];
  for (var i = 0; i < divs.length; i++) {
    if (/\brounded\b/.exec(divs[i].className)) {
      rounded_divs[rounded_divs.length] = divs[i];
    }
  }
  for (var i = 0; i < rounded_divs.length; i++) {
    var original = rounded_divs[i];
    
    /* Make it the inner div of the four */
    original.className = original.className.replace('rounded', '');
    //var originalId = original.id;
    //original.id = null;
    
    /* Now create the outer-most div */
    var tr = document.createElement('div');
    //tr.id = originalId;
    tr.className = 'rounded2';
    
    /* Swap out the original (we'll put it back later) */
    original.parentNode.replaceChild(tr, original);
    
    /* Create the two other inner nodes */
    var tl = document.createElement('div');
    var br = document.createElement('div');
    
    /* Now glue the nodes back in to the document */
    tr.appendChild(tl);
    tl.appendChild(br);
    br.appendChild(original);
  }
}

function loadRoundedCorners() {
/*
  var pos = navigator.userAgent.indexOf('MSIE');
  if (pos > -1) {
    var longVersion = navigator.userAgent.substring(pos + 5);
    var shortVersion = longVersion.substring(0, longVersion.indexOf(';'));
    if (shortVersion.indexOf('8') == -1) {
      return;
    }
  }
*/

  if (typeof window.onload != 'function') {
    window.onload = roundedCorners;
  } else {
    var oldMethod = window.onload;
    window.onload = function() {
      if (oldMethod) {
        oldMethod();
      }
      roundedCorners();
    }
  }
}

loadRoundedCorners();
