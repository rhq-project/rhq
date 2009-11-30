/***********************************************
* IFrame SSI script II- © Dynamic Drive DHTML code library (http://www.dynamicdrive.com)
* Visit DynamicDrive.com for hundreds of original DHTML scripts
* This notice must stay intact for legal use
***********************************************/

//Input the IDs of the IFRAMES you wish to dynamically resize to match its content height:
//Separate each ID with a comma. Examples: ["myframe1", "myframe2"] or ["myframe"] or [] for none:
var iframeids = ["perspective"];

//Should script hide iframe from browsers that don't support this script (non IE5+/NS6+ browsers. Recommended):
var iframehide = "no";

var isFirefox = (navigator.userAgent.indexOf("Firefox/") != -1);
// NOTE (ips): Original script had 16px, but 40px was what I needed to make it work on FF 3.5.3.
var extraHeight = isFirefox ? 40 : 0; // extra height in px to add to iframe in Firefox

function resizeCaller()
{
    var dyniframe = new Array();
    for (i = 0; i < iframeids.length; i++)
    {
        if (document.getElementById) {
            resizeIframe(iframeids[i]);
        }
        //reveal iframe for lower end browsers? (see var above):
        if ((document.all || document.getElementById) && iframehide == "no")
        {
            var tempobj = document.all ? document.all[iframeids[i]] : document.getElementById(iframeids[i]);
            tempobj.style.display = "block";
        }
    }
}

function resizeIframe(frameid)
{
    var currentfr = document.getElementById(frameid);
    if (currentfr && !window.opera)
    {
        currentfr.style.display = "block";
        if (currentfr.contentDocument && currentfr.contentDocument.body.offsetHeight) { // NS6+ syntax
            currentfr.height = currentfr.contentDocument.body.offsetHeight + extraHeight;
        } else if (currentfr.Document && currentfr.Document.body.scrollHeight) { // IE5+ syntax
            currentfr.height = currentfr.Document.body.scrollHeight;
        }
        if (currentfr.addEventListener) {
            currentfr.addEventListener("load", readjustIframe, false);
        } else if (currentfr.attachEvent) {
            currentfr.detachEvent("onload", readjustIframe); // Bug fix line
            currentfr.attachEvent("onload", readjustIframe);
        }
    }
}

function readjustIframe(loadevt)
{
    var crossevt = (window.event) ? event : loadevt;
    var iframeroot = (crossevt.currentTarget) ? crossevt.currentTarget : crossevt.srcElement;
    if (iframeroot) {
        resizeIframe(iframeroot.id);
    }
}

function loadintoIframe(iframeid, url)
{
    if (document.getElementById) {
        document.getElementById(iframeid).src = url;
    }
}

if (window.addEventListener) {
    window.addEventListener("load", resizeCaller, false);
} else if (window.attachEvent) {
    window.attachEvent("onload", resizeCaller);
} else {
    window.onload = resizeCaller;
}
