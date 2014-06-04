// ==UserScript==
// @name        RHQ login monkey
// @namespace   org.rhq.coregui
// @description Types in the credentials when pressing ALT+C on the login page. This script works also for Chrome/Chromium with the extension called Tampermonkey. This script works with the new login screen that was added to RHQ 4.11
// @include     *:7080/coregui/login*
// @grant       none
// @author      Jiri Kremser <jkremser@redhat.com>
// @version     1.2
// ==/UserScript==

function fillTheCredentials() {
  // new UI
  document.getElementById('inputUsername').value="rhqadmin";
  document.getElementById('inputPassword').value="rhqadmin";
  document.getElementById('loginForm').submit();
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
