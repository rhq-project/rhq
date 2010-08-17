var bpui;
if(typeof bpui == "undefined") {
    bpui=new Object();
}
bpui.petstoreList=new Object();

bpui.petstoreList.divName="";
bpui.petstoreList.currentCount=0;
bpui.petstoreList.category="feline01";


bpui.petstoreList.initialSetup=function() {
        // get outerdiv
        var targetDiv=document.getElementById(bpui.petstoreList.divName);
        
        // containier div
        tablex="<table><tr><td align=\"center\">";
        tablex += "<b>Java BluePrint's Pet Store Category:</b> <select size=\"1\" id=\"bpui.petstoreList.categoryList\" onchange=\"bpui.petstoreList.selectCategory()\">";
        tablex += "<option value=\"feline01\">Hairy Cat</option>";
        tablex += "<option value=\"feline02\">Groomed Cat</option>";
        tablex += "<option value=\"canine01\">Medium Dogs</option>";
        tablex += "<option value=\"canine02\">Small Dogs</option>";
        tablex += "<option value=\"avian01\">Parrot</option>";
        tablex += "<option value=\"avian02\">Exotic</option>";
        tablex += "<option value=\"fish01\">Small Fish</option>";
        tablex += "<option value=\"fish02\">Large Fish</option>";
        tablex += "<option value=\"reptile01\">Slithering Reptiles</option>";
        tablex += "<option value=\"reptile02\">Crawling Reptiles</option>";
        tablex += "</select>";
        tablex += "</td></tr><tr><td><div class=\"bpui_petstorelist_div\">";
        tablex += "</div>";
        
        // add product previous and next
        tablex += "</td></tr>";
        tablex += "<tr><td colspan=\"3\" style=\"text-align:center;\">";
        tablex += "<span class=\"bpui_petstorelist_previous\" onclick=\"bpui.petstoreList.previousProducts();\"><< PREVIOUS</span>&nbsp;&nbsp;&nbsp;&nbsp;";
        tablex += "<span class=\"bpui_petstorelist_next\" onclick=\"bpui.petstoreList.nextProducts();\">NEXT >></span><br/>";
        tablex += "</td></tr>";
        tablex += "</table>";
        targetDiv.innerHTML=tablex;
        bpui.petstoreList.setSelectedCategory();
}

bpui.petstoreList.populateData=function(datax) {
    if(typeof datax != "undefined") {
        
        // get outerdiv
        var targetDiv=document.getElementById(bpui.petstoreList.divName);
        
        // containier div
        tablex="<table><tr><td align=\"center\">";
        tablex += "<b>Java BluePrint's Pet Store Category:</b> <select size=\"1\" id=\"bpui.petstoreList.categoryList\" onchange=\"bpui.petstoreList.selectCategory()\">";
        tablex += "<option value=\"feline01\">Hairy Cat</option>";
        tablex += "<option value=\"feline02\">Groomed Cat</option>";
        tablex += "<option value=\"canine01\">Medium Dogs</option>";
        tablex += "<option value=\"canine02\">Small Dogs</option>";
        tablex += "<option value=\"avian01\">Parrot</option>";
        tablex += "<option value=\"avian02\">Exotic</option>";
        tablex += "<option value=\"fish01\">Small Fish</option>";
        tablex += "<option value=\"fish02\">Large Fish</option>";
        tablex += "<option value=\"reptile01\">Slithering Reptiles</option>";
        tablex += "<option value=\"reptile02\">Crawling Reptiles</option>";
        tablex += "</select>";
        tablex += "</td></tr><tr><td><div class=\"bpui_petstorelist_div\">";
        
        tablex += "<table class=\"bpui_petstorelist_table\">";

        // loop through product results
        for(ii=0; ii < datax.length; ii++) {
            // add row
            tablex += "<tr class=\"bpui_petstorelist_row\"><td class=\"bpui_petstorelist_cell\">";
            tablex += "<a class=\"bpui_petstorelist_image\" target=\"bppetstore\" href=\"http://localhost:8080/petstore/faces/catalog.jsp#" + 
                datax[ii].productID + "," + datax[ii].itemID + "\">";
            
            tablex += "<img src=\"http://localhost:8080/petstore/ImageServlet/" + datax[ii].imageThumbURL + "\"/>";

            tablex += "</a>";
            tablex += "</td><td class=\"bpui_petstorelist_cell\">";
            tablex += "<a class=\"bpui_petstorelist_link\" target=\"bppetstore\" href=\"http://localhost:8080/petstore/faces/catalog.jsp#" + 
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
                
        tablex += "</table></div>";
        
        // add product previous and next
        tablex += "</td></tr>";
        tablex += "<tr><td colspan=\"3\" style=\"text-align:center;\">";
        tablex += "<span class=\"bpui_petstorelist_previous\" onclick=\"bpui.petstoreList.previousProducts();\"><< PREVIOUS</span>&nbsp;&nbsp;&nbsp;&nbsp;";
        tablex += "<span class=\"bpui_petstorelist_next\" onclick=\"bpui.petstoreList.nextProducts();\">NEXT >></span><br/>";
        tablex += "</td></tr>";
        tablex += "</table>";
        targetDiv.innerHTML=tablex;
        bpui.petstoreList.setSelectedCategory();
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
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=" + bpui.petstoreList.category + "&start=" + bpui.petstoreList.currentCount + "&length=5&format=jsonp");
    bodyTag.appendChild(scriptx);
}

bpui.petstoreList.createPetstoreList=function(divName) {
    // keep divName for later references
    bpui.petstoreList.divName=divName;

    // load data from service
    bodyTag=document.getElementsByTagName("body")[0];
    scriptx=document.createElement("script");
    scriptx.setAttribute("type", "text/javascript");
    scriptx.setAttribute("src", "http://localhost:8080/petstore/catalog?command=items&pid=" + bpui.petstoreList.category + "&start=0&length=5&format=jsonp");
    bodyTag.appendChild(scriptx);
    
}
