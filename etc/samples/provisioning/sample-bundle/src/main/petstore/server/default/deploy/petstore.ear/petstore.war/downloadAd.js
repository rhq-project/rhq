dojo.require("dojo.io");
function checkAdPage() {
    var bindArgs = {
        // check to see if an updated page is available
        URL: "http://localhost:8080/petstore/downloadAd.txt",
        mimetype: "text/plain",
        load: returnFunctionx,
        error: ajaxBindError};

    // dispatch the request
    dojo.io.bind(bindArgs);      
}

function returnFunctionx(type, data, evt) {
    // statically setup popup for simple case
    // check return of the dojo call to make sure it is valid
    if (evt.readyState == 4) {
        if (evt.status == 200) {
            alert("data = " + data);
            document.getElementById("downloadAds").innerHTML=data;
        }
    }
}


var req;

function checkAdPageMark() {
    // calculate arrow and border image location
    req=initRequest();

    url="http://blueprints.dev.java.net/petstore/downloadAd.txt",
    req.onreadystatechange = returnFunctionxMark;
    req.open("GET", url, true);
    req.send(null);
}


function returnFunctionxMark() {
    if (req.readyState == 4) {
        if (req.status == 200) {
            alert("data = " + req.responseText);
            document.getElementById("downloadAds").innerHTML=req.responseText;
        }
    }
}

initRequest=function() {
    if (window.XMLHttpRequest) {
        return new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        return new ActiveXObject("Microsoft.XMLHTTP");
    }
}
