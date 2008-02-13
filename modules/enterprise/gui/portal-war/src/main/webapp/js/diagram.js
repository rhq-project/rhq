/*-- START diagram.js --*/

function toggleDiagram(eId) {
  var thisDiv = document.getElementById(eId);
  
  if ( thisDiv.style.visibility == "hidden") {
    showDiagram(eId);
  } else {
    hideDiagram(eId);
  }
}

function hideDiagram(eId) {
  var thisDiv = document.getElementById(eId);
  
  thisDiv.style.visibility = "hidden";  
  showFormElements();
  
}

function showDiagram(eId) {
  var thisDiv = document.getElementById(eId);

  thisDiv.style.visibility = "visible";
  hideFormElements();
}

// We register this body.onclick handler within this javascript file
// so that the onclick happens only on pages that have the diagram.
// This function should work in all IE and Gecko-based browsers.
function bodyClicked(e) {
  if (!e) {
    if (window.event) {
      e = window.event;
    }
  }

  var target = null;
  if (e.target) {
    target = e.target;
  } else if (e.srcElement) {
    target = e.srcElement;
  }

  if ( !target || ('navMapIcon' != target.name && 'navMapImage' != target.name) ) {
    hideDiagram('diagramDiv');
  }
}
document.body.onclick = bodyClicked;

/*-- END diagram.js --*/
