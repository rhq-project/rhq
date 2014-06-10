function httpPut() {
    var url = "/portal/sessionAccess";
    var xmlHttp = null;
    xmlHttp = new XMLHttpRequest();
    xmlHttp.open("PUT", url, false);
    xmlHttp.send(null);
    return xmlHttp;
}
/*
function initApp() {
    var oHead = document.getElementsByTagName('BODY').item(0);
    var oScript= document.createElement("script");
    oScript.type = "text/javascript";
    oScript.src="org.rhq.coregui.CoreGUI/org.rhq.coregui.CoreGUI.nocache.js";
    oHead.appendChild(oScript);
    window.alert('foo');
    //document.write("<script type='text/javascript' language='javascript' src='org.rhq.coregui.CoreGUI/org.rhq.coregui.CoreGUI.nocache.js'><\/script>");
}
*/

function initKeycloak() {    
    try {
        var response = httpPut();
    } catch (error) {
        window.alert('back-end is down. Try again?');
        setTimeout("initKeycloak();", 5000);
    }
    if (response && response.status === 200) {
        var json = JSON.parse(response.responseText);
        if (json.serverInitialized) {
            if (json.keycloak) {
                var keycloak = Keycloak('../portal/sessionAccess');
                var loadData = function () {
                    if (keycloak.idToken) {
                        window.kcReady=true;
                    } else {
                        keycloak.loadUserProfile(function() {window.kcReady=true;}, function() {
                          window.kcReady=false;
                          window.alert('unable to loak Keycloak');
                        });
                    }
                };
                keycloak.onAuthSuccess = loadData;
                var initOptions = {'onLoad':'login-required'};
                keycloak.init(initOptions);
            } else {
                window.kcReady=false;
            }
        } else {
            setTimeout("initKeycloak();", 3000);
        }
    }
}

initKeycloak();

