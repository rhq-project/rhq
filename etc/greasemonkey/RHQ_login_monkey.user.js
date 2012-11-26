// ==UserScript==
// @name        RHQ login monkey
// @namespace   org.rhq.enterprise.gui.coregui
// @description Types in the credentials
// @include     *:7080/coregui/*
// @include     *:7080/coregui/#LogOut
// @grant       none
// @version     1
// ==/UserScript==

//GM_registerMenuCommand("Fill the credentials", function(e) {
//  fillTheCredentials();
//}, "c", "alt", "c" );


(function(){
document.addEventListener('keydown', function(e) {
  // pressed alt+c
  if (e.keyCode == 67 && !e.shiftKey && !e.ctrlKey && e.altKey && !e.metaKey) {
    fillTheCredentials();
  }
}, false);
})();


function fillTheCredentials() {
  document.getElementsByName("user")[0].value="rhqadmin"
  document.getElementsByName("password")[0].value="rhqadmin"
  //document.forms[0].submit() don't work, SmartGWT uses internal logic
  document.getElementsByName("password")[0].focus()
}
