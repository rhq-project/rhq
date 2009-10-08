
/*-- START dashboard.js --*/
function resizeToCorrectWidth() {
  var footers = document.getElementsByName("footer");
  if (footers.length == 2) {
    var divArr = document.getElementsByName("containerDiv");
    for (i=0; i<divArr.length; i++) {
      divArr[i].setAttribute("style", "");
    }
    
    var eArr = document.getElementsByName("specialTd");
    for (i=0; i<eArr.length; i++) {
      if (eArr[i].getAttribute("width") == "100%") {
        eArr[i].setAttribute("width", "25%");
      }
    }
  }
}
/*-- END dashboard.js --*/
