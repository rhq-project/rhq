/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: engine.js,v 1.10 2006/05/03 22:00:32 inder Exp $ */

function Engine () {

  /**
   * 
   * Load template text aloing with an associated script
   * 
   * Argument p properties are as follows:
   *
   * url :              Not required but used if you want to get the template from
   *                    something other than the injection serlvet. For example if
   *                    you want to load content directly from a a JSP or HTML file.
   *       
   * p.template :       Not required if you specficy a url property Otherewise this
   *                    is the name of the template file.
   *
   * p.initFunction:    Not required. This function or function pointer will be called
   *                    after the template text and script are loaded. The result of 
   *                    the evaluated script will be accessible in the context of
   *                    this function.
   *
   * p.injectionPoint:  Not required. This is the id of an element into. If this is
   *                    not specfied a div will be created under the roon node of
   *                    the document and the template will be injected into it.
   *                    Content is injected by setting the innerHTML property
   *                    of an element to the template text.
   */
  this.inject = function (p) {
        var targetUrl;
        if (!p.url) targetUrl = "controller?command=content&target=/" + p.template;
        else targetUrl = p.url;
        var templateArgs = {
            url:  targetUrl,
            mimetype: "text/html",
            load: function(type, data) {
               //if no parent is given append to the document root
               var nData = includeEmbeddedResources(data, p.initFunction );
               if (!p.injectionPoint) {
                    var injectionPoint = document.createElement("div");
                    injectionPoint.innerHTML = nData;
                    document.firstChild.appendChild(injectionPoint);
               } else {
                    p.injectionPoint.innerHTML = nData;
               }
               if (p.script) {
                  // now load the associated JavaScript
                  loadScript(p.script,p.initFunction);
               }
            }
        };
        dojo.io.bind(templateArgs);
  }

  function loadScript(targetURL,callback) {
        var templateArgs = {
           url:  targetURL,
            mimetype: "text/plain",
            load: callback
        };
        dojo.io.bind(templateArgs);
  }
  
  /**
   * If were returning an text document remove any script in the
   * the document and add it to the global scope using a time out.
   */
  function includeEmbeddedResources(target,  initFunction) {
    var bodyText = "";
    var embeddedScripts = [];
    var embeddedStyles = [];
    var scriptReferences = [];
    var styleReferences = [];
    var styles = [];
    // recursively go through and weed out the scripts
    // TODO: Use some better REGEX processing
    // TODO: Also support single quotes
    while (target.indexOf("<script") != -1) {
            var realStart = target.indexOf("<script");
            var scriptSourceStart = target.indexOf("src=", (realStart));
            var scriptElementEnd = target.indexOf(">", realStart);
            var end = target.indexOf("</script>", (realStart)) + "</script>".length;
            if (realStart != -1 && scriptSourceStart != -1) {
                var scriptSourceName;
                var scriptSourceLinkStart= scriptSourceStart + 5;
                var scriptSourceLinkEnd= target.indexOf("\"", (scriptSourceLinkStart + 1));
                if (scriptSourceStart < scriptElementEnd) {
                    scriptSourceName = target.substring(scriptSourceLinkStart, scriptSourceLinkEnd);
                    // prevent multiple inclusions of dojo.js. 
                    // there is no way you would get to this point without dojo being included
                    if (scriptSourceName.indexOf("dojo.js") == -1) {
                        scriptReferences.push(scriptSourceName);
                    }
                }
            }
            // now remove the script body
           var scriptBodyStart =  scriptElementEnd + 1;
           var sBody = target.substring(scriptBodyStart, end - "</script>".length);
           if (sBody.length > 0) {
              embeddedScripts.push(sBody);
           }
           //remove script
           target = target.substring(0, realStart) + target.substring(end, target.length);
      }

      while (target.indexOf("<style") != -1) {
            var realStart = target.indexOf("<style");
            var styleElementEnd = target.indexOf(">", realStart);
            var end = target.indexOf("</style>", (realStart)) ;
             var styleBodyStart =  styleElementEnd + 1;
             var sBody = target.substring(styleBodyStart, end);
           if (sBody.length > 0) {
              embeddedStyles.push(sBody);
           }
           //remove style
           target = target.substring(0, realStart) + target.substring(end + "</style>".length, target.length);
        }
        // get the links    
        while (target.indexOf("<link") != -1) {
            var realStart = target.indexOf("<link");
            var styleSourceStart = target.indexOf("href=", (realStart));
            var styleElementEnd = target.indexOf(">", realStart) +1;
            if (realStart != -1 && styleSourceStart != -1) {
                var styletSourceName;
                var styleSourceLinkStart= styleSourceStart + 6;
                var styleSourceLinkEnd= target.indexOf("\"", (styleSourceLinkStart + 1));
                if (styleSourceStart < styleElementEnd) {
                    styleSourceName = target.substring(styleSourceLinkStart, styleSourceLinkEnd);
                    styleReferences.push(styleSourceName);
                }
                //remove style
                target = target.substring(0, realStart) + target.substring(styleElementEnd, target.length);
            }
        }
        
        var head = document.getElementsByTagName("head")[0];
        
        // inject the links
        for(var loop = 0; loop < styleReferences.length; loop++) {
            var link = document.createElement("link");
            link.href = styleReferences[loop];
            link.type = "text/css";
            link.rel = "stylesheet";
            head.appendChild(link);
        }
        
        var stylesElement;
        if (embeddedStyles.length > 0) {
            stylesElement = document.createElement("style");
            stylesElement.type="text/css";
            var stylesText;
            for(var loop = 0; loop < embeddedStyles.length; loop++) {
                stylesText = stylesText + embeddedStyles[loop];
            }
            if (document.styleSheets[0].cssText) {
               document.styleSheets[0].cssText = document.styleSheets[0].cssText + stylesText;
            } else {
                stylesElement.appendChild(document.createTextNode(stylesText));
                head.appendChild(stylesElement);
            }
        }
                
        scriptLoader(scriptReferences, 0, function() {
           this.embeddedScripts = embeddedScripts;
            // evaluate the embedded javascripts in the order they were added
            // consider using an onload handler
            for(var loop = 0; loop < embeddedScripts.length; loop++) {
            //alert("evaluating " + embeddedScripts[loop]);
                var script = embeddedScripts[loop];
                // append to the script a method to call the scriptLoaderCallback
                eval(script);
                if (loop == (embeddedScripts.length -1)) {
                    initFunction();
                }
            }
        });
   
        return target;
    }
    
   
    /**
     * Load the scripts in order and load them one after on another
     */
    function scriptLoader(scripts, index, callbackFunction) {
        var head = document.getElementsByTagName("head").item(0);
        var scriptElement = document.createElement("script");
        scriptElement.id = "c_script_" + index;
        scriptElement.type = "text/javascript";
        
        var loadHandler = function () {
            if (index < scripts.length &&  index != scripts.length -1) {
                scriptLoader(scripts, ++index, callbackFunction);
            } else {
                callbackFunction();
            }
        }
        if (typeof scriptElement.onreadystatechange != 'undefined') {
            scriptElement.onreadystatechange = function () {
		        if (this.readyState == 'loaded') {
		    	    loadHandler();
	            }
		     }; 
        }
        scriptElement.onload = loadHandler;
        
        // Safari not seeing the onload event and does not support the onreadystate
        if (navigator.userAgent.toLowerCase().indexOf("safari") != -1) {
            scriptElement.src = scripts[index];
            setTimeout(loadHandler, 0);
        }
        head.appendChild(scriptElement);
        setTimeout("document.getElementById('c_script_" + index + "').src ='" + scripts[index] + "'", 0);

        scriptElement = null;
        head = null;
    }
            
  /**
   * If were returning an XML document remove any script in the
   * the document and add it to the global scope using a time out.
   */
  function includeEmbeddedScripts(xmlDocument) {
    var items = new Array();
    var xmlDocument = document.getElementsByTagName("script");
  
    for(var loop = 0; loop < targets.length; loop++) {
        var children = targets[loop].childNodes;
        var iScript = "";
        for(var innerLoop = 0; innerLoop < children.length; innerLoop++) {
            iScript += children[innerLoop].data;
        }
        items.add(iScript);
        children[loop].parentNode.removeChild(children[loop]);
    }
    for(var loop = 0; loop < items.length; loop++) {
        setTimeout(items[loop],0);
    }
  }
}
