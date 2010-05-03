/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: main.js,v 1.9 2006/05/03 22:00:33 inder Exp $ */

var isIE;
var bodyRowText;

dojo.require("dojo.widget.FisheyeList");
init();

function browse(category) {
    window.location.href="catalog.jsp?catid=" + category;
}

function loadPetstore() {
    init();
    showMain();
}

function init() {
    if (navigator.userAgent.indexOf("IE") != -1) isIE = true;
}
