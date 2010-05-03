var bpui;
if(typeof bpui == "undefined") {
    bpui=new Object();
}
bpui.petstoreList=new Object();

bpui.petstoreList.divName="";
bpui.petstoreList.currentCount=0;

bpui.petstoreList.populateData=function(datax) {
    if(typeof datax != "undefined") {
        
        // get outerdiv
        var targetDiv=document.getElementById(bpui.petstoreList.divName);
        
        // make sure div is clear
        targetDiv.innerHTML="";
        
        // add class to containing div
        targetDiv.setAttribute("class", "bpui_petstorelist_div")
        
        // add table
        tablex=document.createElement("table");
        tablex.setAttribute("class", "bpui_petstorelist_table")
        targetDiv.appendChild(tablex);

        // loop through product results
        for(ii=0; ii < datax.length; ii++) {
            // add row
            rowx=document.createElement("tr");
            
            // add product image with hyperlink
            colx=document.createElement("td");
            ax=document.createElement("a");
            ax.setAttribute("href", "http://localhost:8080/petstore/faces/catalog.jsp#" + datax[ii].productID + "," + datax[ii].itemID)
            ax.setAttribute("target", "bppetstore")
            ax.setAttribute("class", "bpui_petstorelist_image")
            imgx=document.createElement("img");
            imgx.setAttribute("src", "http://localhost:8080/petstore/ImageServlet/" + datax[ii].imageThumbURL);
            ax.appendChild(imgx);
            colx.appendChild(ax);
            rowx.appendChild(colx);

            // add product name with hyperlink
            colx=document.createElement("td");
            ax=document.createElement("a");
            ax.setAttribute("href", "http://localhost:8080/petstore/faces/catalog.jsp#" + datax[ii].productID + "," + datax[ii].itemID)
            ax.setAttribute("target", "bppetstore")
            ax.setAttribute("class", "bpui_petstorelist_name_link")
            spanx=document.createElement("span");
            spanx.setAttribute("class", "bpui_petstorelist_name")
            spanx.appendChild(document.createTextNode(datax[ii].name));
            ax.appendChild(spanx);
            colx.appendChild(ax);
            colx.appendChild(document.createElement("br"));
            // add product description
            spanx=document.createElement("span");
            spanx.setAttribute("class", "bpui_petstorelist_description");
            spanx.appendChild(document.createTextNode(datax[ii].description));
            colx.appendChild(spanx);
            rowx.appendChild(colx);
            
            // add product price
            colx=document.createElement("td");
            spanx=document.createElement("span");
            spanx.setAttribute("class", "bpui_petstorelist_price");
            spanx.appendChild(document.createTextNode("\$" + datax[ii].price));
            colx.appendChild(spanx);
            rowx.appendChild(colx);

            // add row to table
            tablex.appendChild(rowx);
        }
        
        // add product previous and next
        rowx=document.createElement("tr");
        colx=document.createElement("td");
        colx.setAttribute("colspan", "3");
        spanx=document.createElement("span");
        spanx.setAttribute("class", "bpui_petstorelist_previous");
        spanx.setAttribute("onclick", "bpui.petstoreList.previousProducts();");
        spanx.appendChild(document.createTextNode("<< PREVIOUS"));
        colx.appendChild(spanx);
        
        spanx=document.createElement("span");
        spanx.setAttribute("class", "bpui_petstorelist_next");
        spanx.setAttribute("onclick", "bpui.petstoreList.nextProducts();");
        spanx.appendChild(document.createTextNode("NEXT >>"));
        colx.appendChild(spanx);
        rowx.appendChild(colx);

        // add row to table
        tablex.appendChild(rowx);
    }
}


bpui.petstoreList.nextProducts=function() {
    // load data from service
    bpui.petstoreList.currentCount += 5;
    bpui.petstoreList.updateProducts();
}


bpui.petstoreList.previousProducts=function() {
    bpui.petstoreList.currentCount -= 5;
    if(bpui.petstoreList.currentCount < 0) {
        bpui.petstoreList.currentCount=0;
    }
    bpui.petstoreList.updateProducts();
}


bpui.petstoreList.updateProducts=function() {
    // load data from service
    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=feline01&start=" + bpui.petstoreList.currentCount + "&length=5&format=jsonp");
    bodyTag.appendChild(scriptx);
}



bpui.petstoreList.createPetstoreList=function(divName) {
    // keep divName for later references
    bpui.petstoreList.divName=divName;

    // load data from service
    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=feline01&start=0&length=5&format=jsonp");
    bodyTag.appendChild(scriptx);
}

