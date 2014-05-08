// ==UserScript==
// @name        RHQ login monkey old
// @namespace   org.rhq.coregui
// @description Types in the credentials when pressing ALT+C on the login page. This script works also for Chrome/Chromium with the extension called Tampermonkey. This version works well with the RHQ 4.10 and lower.
// @include     *:7080/coregui/*
// @include     *:7080/coregui/#LogOut
// @exclude     *:7080/coregui/login*
// @grant       none
// @author      Jiri Kremser <jkremser@redhat.com>
// @version     1.1
// ==/UserScript==

function fillTheCredentials() {
  document.getElementsByName("user")[0].value="rhqadmin";
  document.getElementsByName("password")[0].value="rhqadmin";
  //document.forms[0].submit() doesn't work, SmartGWT uses internal logic
  document.getElementsByName("password")[0].focus();
}

// register the function and the hot key
//GM_registerMenuCommand("Fill the credentials", function(e) {
//  fillTheCredentials();
//}, "c", "alt", "c" );

// fallback solution, because the function above doesn't work in all browsers
(function(){
document.addEventListener('keydown', function(e) {
  // pressed alt+c
  if (e.keyCode == 67 && !e.shiftKey && !e.ctrlKey && e.altKey && !e.metaKey) {
    fillTheCredentials();
  }
}, false);
}());
