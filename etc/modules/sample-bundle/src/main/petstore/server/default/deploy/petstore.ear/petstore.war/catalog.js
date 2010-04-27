/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: catalog.js,v 1.21 2007/01/17 22:16:44 basler Exp $ */

var ac;
var is;
var controller;
//var debug=true;

function getApplicationContextRoot() {
    var urlArray=window.location.toString().split("/", 4);
    return "/" + urlArray[3];
}

var applicationContextRoot=getApplicationContextRoot();

function initCatalog() {
    ac = new AccordionMenu();
    is = new ImageScroller();
    is.load();
    controller = new CatalogController();
    // wire in a listener for the rating component
    dojo.event.connect("before", bpui.rating, "doClick", controller, "modifyState");
    controller.initialize();    
}

function CatalogController() {
  dojo.event.topic.subscribe("/catalog", this, handleEvent);
  
  // this object structure contains a list of the products and chunks that have been loaded
  var pList = new ProductList();
  
  var CHUNK_SIZE=7;
  var initalRating;
  var initalItem;
  var originalURL;
  
  // using this for some browsers that do not support innerHTML
  var useDOMInjection = false;
  
  var infoName = document.getElementById("infopaneName");
  var infoRating = document.getElementById("infopaneRating");
  var infoPrice = document.getElementById("infopanePrice");
  var infoBuyNow = document.getElementById("infopaneBuyNow");
  var infoShortDescription =  document.getElementById("infopaneShortDescription");
  var infoDescription =  document.getElementById("infopaneDescription"); 
  // for paypal
  var buyNowAmount = document.getElementById("buyNow1_amount");
  var buyNowItemName = document.getElementById("buyNow1_item_name");
  
  function handleEvent(args) {
      if (args.type == "showingItem") {
        // update the id on the ratring component
        if (typeof bpui != 'undefined') {
            var groupId = is.getGroupId();
      	    window.location.href= originalURL +  "#" + groupId + "," + args.id;
            if (typeof bpui.rating != 'undefined') {
                // update the rating
                bpui.rating.state["rating"].bindings["itemId"]=args.id;
                bpui.rating.modifyDisplay("rating", args.rating, true);
                // get the currrent item
                showItemDetails(args.id);
            } else {
                initalItem = args.id;
                initalRating = args.rating;         
            }
        } else {
            // things haven't been loaded to set the inital rating
            initalItem = args.id;
            initalRating = args.rating;
        }
      } else if (args.type == "getChunk") {
          populateItems(args.id, args.index, args.currentChunk, false);

      } else if (args.type == "showItemDetails") {
          showProductDetails(args.productId, args.itemId);

      }  else if (args.type == "showProducts") {
          is.reset();
          populateItems(args.productId, 0, 0, true);
      }
  }
  
  function showItemDetails(id) {
      var i = is.getItems().get(id);
      setNodeText(infoName, i.name + "<br/><a href='javascript:controller.disableItem(&quot;" + id + "&quot;,&quot;" + i.name + "&quot;)'><font size='-1' color='white'><i>Flag as inappropriate</i></font></a>");
      setNodeText(infoPrice, i.price);
      setNodeText(infoShortDescription, i.shortDescription);
      setNodeText(infoDescription, i.description);
      // update the paypal
      buyNowAmount.value = i.price;
      buyNowItemName.value = i.name;
  }
  
  function setNodeText(t, text) {
      if (useDOMInjection) {
          t.lastChild.nodeValue = text;
      } else {
          t.innerHTML = text;
      }
  }
  
  this.initialize = function() {
        // check whether the innerHTML changes can be used in the infopane
      infoName.innerHTML = "&nbsp;";
      if (!useDOMInjection && infoName.innerHTML != "&nbsp;") {
        useDOMInjection = true;

        infoName.appendChild(document.createTextNode("Name"));
        infoPrice.appendChild(document.createTextNode("$0.00"));
        infoShortDescription.appendChild(document.createTextNode("<description>"));
        infoDescription.appendChild(document.createTextNode("<description>"));
      }
      
      var ratingInstance = bpui.rating.state["rating"];
      ratingInstance.grade = initalRating;
      bpui.rating.state["rating"].bindings["itemId"]=initalItem;
      bpui.rating.modifyDisplay("rating", initalRating, true);
      loadAccordion();
  }
  
 
  this.modifyState = function(arg, rating) {
      var itemId = initalItem;
      if (typeof  bpui.rating.state["rating"].bindings["itemId"] != 'undefined') {
          itemId = bpui.rating.state["rating"].bindings["itemId"]; 
      }
      // set the cached rating to the new rating that was set.
      is.getItems().get(itemId).rating  = rating;
  }
  

  function loadAccordion () {
        // go out and get the categories
        // this should be made more geric
        var bindArgs = {
            url:  applicationContextRoot + "/catalog?command=categories&format=json",
            mimetype: "text/json",
            load: function(type,json) {
               ac.load(json);
               processURLParameters();
             },
             error: ajaxBindError
        };
        dojo.io.bind(bindArgs);
    }
    
    // this needs to happen after we have loaded the accordion data
    function processURLParameters() {
        originalURL = decodeURIComponent(window.location.href);
        var params = {};
        // look for the params
        if (originalURL.indexOf("#") != -1) {
            var qString = originalURL.split('#')[1];
            var args = qString.split(',');
            originalURL = originalURL.split('#')[0];
            ac.loadCategoryItem(args[0], args[1]);
            return;
    	} else if (originalURL.indexOf("?") != -1) {
            var qString = originalURL.split('?')[1];
            // get rid of any bookmarking stuff
            if (qString.indexOf("#") != -1) {
                qString = qString.split('#')[0];
                originalURL = originalURL.split('#')[0];
                window.location.href = originalURL;
            }
            ps = qString.split('&');
            // now go through and create the params map as an object literal
            for (var i in ps) {
                var t = ps[i].split('=');
                params[t[0]] = t[1];
            }
            // first check for the item in product        
            if (typeof params.itemId != 'undefined' && typeof params.pid != 'undefined') {
                ac.loadCategoryItem(params.pid, params.itemId);
        	// next if there is a catid definition then do it
            } else if (typeof params.catid != 'undefined') {
                ac.showCategory(params.catid);
            }
        } else {
            // nothing is selected
            ac.showFirstCategory();
        }
    }
  


    function showProductDetails(pid, itemId) {
        is.reset();
        is.showProgressIndicator();
        var bindArgs = {
            url:  applicationContextRoot + "/catalog?command=itemInChunk&pid=" + pid + "&itemId=" + itemId + "&length=" + CHUNK_SIZE,
            mimetype: "text/xml",
            load: function(type,data,postProcessHandler) {
               processProductData(data,true, pid, itemId);
               showItemDetails(itemId);
               is.doMaximize();
             },
             error: ajaxBindError 
        };
        dojo.io.bind(bindArgs);          
    }



    function populateItems(pid, index, neededChunk, showImage) {
        is.showProgressIndicator(); 
        is.setGroupId(pid);
        printDebug("populateItems - need to make sure displaying - pid=" + pid + " Chunk=" +  neededChunk);

        // check to see if relevant scroller page is already loaded
        if(!is.containsChunk(pid + "_" + neededChunk)) {

            // not loaded, so see if it is in the cache
            if (pList.hasChunk(pid, neededChunk)) {
                // in cache, so add chunk to scroller
                printDebug("**** adding chunk from cache - pid=" + pid + " Chunk=" +  neededChunk);
                is.addChunk(pid + "_" + neededChunk);
                is.addItems(pList.getChunk(pid, neededChunk));
                
                // show first image if you have it
                if(showImage && is.getScrollerItems().length > 0) {
                    is.showImage(is.getScrollerItems()[0].id);
                }

            } else {
                // not in cache so load it
                startRetIndex=(neededChunk * CHUNK_SIZE);

                printDebug("**** retrieving chunk from server - pid=" + pid + " currentIndex=" + index + " startIndex=" + startRetIndex + " Chunk=" +  neededChunk);
                var bindArgs = {
                    url:  applicationContextRoot + "/catalog?command=items&pid=" + pid + "&start=" + startRetIndex + "&length=" + CHUNK_SIZE,
                    mimetype: "text/xml",
                    load: function(type,data,postProcessHandler) {
                        processProductData(data, showImage, pid, null, neededChunk);
                    },
                    error: ajaxBindError
                };
                dojo.io.bind(bindArgs);
            }
        } else {
            printDebug("*** items already showing");
        }

    }


   function processProductData(responseXML, showImage, pid, iId, chunkId) {
        var items = [];
        var count = responseXML.getElementsByTagName("item").length;
        for (var loop=0; loop < count ; loop++) {

            var item = responseXML.getElementsByTagName("item")[loop];
            var itemId =  item.getElementsByTagName("id")[0].firstChild.nodeValue;
            var name =  item.getElementsByTagName("name")[0].firstChild.nodeValue;
            var thumbURL = item.getElementsByTagName("image-tb-url")[0].firstChild.nodeValue;
            var imageURL = item.getElementsByTagName("image-url")[0].firstChild.nodeValue;
            var description = item.getElementsByTagName("description")[0].firstChild.nodeValue;
            var price = item.getElementsByTagName("price")[0].firstChild.nodeValue;
            var rating = item.getElementsByTagName("rating")[0].firstChild.nodeValue;
            var shortDescription;
            if (description.length > 71) {
                shortDescription = description.substring(0,71) + "...";
            } else {
                shortDescription = description;
            }
            var i = {id: itemId, name: name, image: imageURL, thumbnail: thumbURL, shortDescription: shortDescription, description: description, price:price, rating: rating};
            items.push(i);
        }

        // cache the chunks 
        pList.addChunk(pid, chunkId, items);
        is.addItems(items);
        is.addChunk(pid + "_" + chunkId);

        if (showImage && iId == null) {
            is.setGroupId(pid);
            is.showImage(items[0].id);
        } else {
            is.setGroupId(pid);
            is.showImage(iId);
        }
        is.hideProgressIndicator();
    }
    
    function ProductList() {
        var _plist = this;
        var map = new Map();
        
        this.addChunk = function(pid, chunkNumber, items) {
            map.put(pid + "_" + chunkNumber, items, true);  
        }
        
        this.getChunk = function(pid, chunkNumber) {
            return map.get(pid + "_" + chunkNumber);  
        }
        
        this.hasChunk = function(pid, chunkNumber) {
            return (map.get(pid + "_" + chunkNumber) != null);  
        }
        
        this.contents = function() {
            return map.contents();
        }
    }


  this.disableItem=function(itemId, itemName) {
        // go out and get the categories
        // this should be made more geric
        if (confirm("Are you sure you want to effectively remove this item from Petstore?")) {
            var bindArgs = {
                url:  applicationContextRoot + "/catalog?command=disable&id=" + itemId,
                mimetype: "text/xml",
                load: function(type,json) {
                    //alert("The item named '" + itemName + "' has been disabled!");
                    pList = new ProductList();
                    is.reset();
                    populateItems(is.getGroupId(), 0, 0, true);
                 },
                 error: ajaxBindError
            };
            dojo.io.bind(bindArgs);
        }
    }


}


