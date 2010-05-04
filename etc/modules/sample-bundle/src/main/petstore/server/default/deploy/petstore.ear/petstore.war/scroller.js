/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: scroller.js,v 1.32 2007/01/17 18:00:09 basler Exp $ */

/**
* ImageScroller - A multipurpose item brower
* @ Author: Greg Murray
*
*/

function getApplicationContextRoot() {
    var urlArray=window.location.toString().split("/", 4);
    return "/" + urlArray[3];
}

var applicationContextRoot=getApplicationContextRoot();


function ImageScroller() {
    var _this = this;
    var initialized = false;
    // default sizes
    
    var VIEWPORT_WIDTH = 500;
    // all sizes are realitive the the viewport width
    var IMAGEPANE_WIDTH = VIEWPORT_WIDTH;
    var IMAGEPANE_HEIGHT = Math.round(VIEWPORT_WIDTH / 1.49);
    var INFOPANE_DEFAULT_HEIGHT = Math.round(VIEWPORT_WIDTH / 6.67);
    var INFOPANE_EXPAND_HEIGHT = Math.round(VIEWPORT_WIDTH / 2.86);
    var THUMB_WIDTH = Math.round(VIEWPORT_WIDTH / 5);;
    var THUMB_HEIGHT = Math.round(VIEWPORT_WIDTH / 6.67);
    
    var CHUNK_SIZE = 7;
    var PREFETCH_THRESHHOLD = 5;
    
    var IMAGE_PANE_ID = "imagePane";
    var IMAGE_PANE_BUFFER_ID = "imageBufferPane";

    var PADDING = 3;
    
    var MINIMIZE_IMG_URI = applicationContextRoot + "/images/minimize.gif";
    var MAXIMIZE_IMG_URI = applicationContextRoot + "/images/maximize.gif";
    var INDICATOR_IMG_URI = applicationContextRoot + "/images/indicator-black.gif";
    var MAXIMIZE_IMG_TOOLTIP = "Show Details";
    var MINIMIZE_IMG_TOOLTIP = "Show Less Details";
    
    // this is an array of the tiles which are divs for each thumb
    var tiles = [];
    
    var injectionPoint;
    
    // for scrolling

    var SCROLL_INCREMENT = 5;
    var INFOPANE_INCREMENT = 3;
    var tileY;
    var tileX;
    // this is the index of the image tile on the far left
    var index = 0;
    // keeps track how for the scroll has gone 
    var offset = 0;
       
    var timeout = 30; // in ms
    var isScrollingRight = false;
    var isScrollingLeft = false;
    
    // large image pane
    var imagePane;
    var imageLoadingPane;
    var loadingPane;
    // images 
    var minimizeImage;

    var indicatorImage;
    var leftButton;
    var rightButton;
    
    // infopane
    var infoPane;
    var infoTableMinimize;
    var indicatorCell;
    var minimizeLink;
    var infoPaneLoop = 0;
    var maximizing = false;
    var minimizing = false;
    var maximized = false;

    // prefetch thresh-hold
    var prefetchThreshold = 2;
    
    // a growing list of items;
    var items = [];
    // cached chunks that are already in the items array
    var loadedChunks = [];
    
    // used for debugging when debug is true
    var debug = false;
    var statusDiv;
    var status2Div;
    
    var showingBuffer = false;
    var imageBuffer;
    var imageReloadTries = 0;
    var IMG_RELOAD_RETRY_MAX = 30;
    // used for url book marking

    
    var pid;
    var currentChunk;
     // this map contains all the items 
    var map;
    // this is the main container div
    var containerDiv;
    
    this.getItems = function() {
        return map;
    }
    
    this.getScrollerItems = function() {
        return items;
    }


    this.getGroupId = function() {
        return pid;
    }
        
    this.reset = function() {
        resetTitles()
        tiles = [];
        index = 0;
        offset = 0;
        currentChunk = 0;
        items = [];
        loadedChunks = [];
    }
    
    function resetTitles() {
        for (var l = 0; l < tiles.length; l++) {
            tiles[l].parentNode.removeChild(tiles[l]);
        }
    } 
    
    // event bound to the mouseOut event of both scroll buttons
    function scrollDone() {
        isScrollingLeft = false;
        isScrollingRight = false;
    }
    
    // looping method for time out 
    function scroll() {
        if (isScrollingRight) scrollRight();
        else if (isScrollingLeft) scrollLeft();
    }
    
    
     // do the value list pre-emptive fetching
    function prefetch() {
        //printDebug("** scoller index = " + index);
        if (isScrollingRight && (index + PREFETCH_THRESHHOLD) % CHUNK_SIZE == 0) {

            if ((Math.round(index / CHUNK_SIZE)) >= currentChunk) {
                currentChunk = Math.round(index / CHUNK_SIZE) + 1;
                // fire an event
                dojo.event.topic.publish("/catalog", {type:"getChunk", id: pid, index: index, currentChunk: currentChunk});
            }
        }
    }
    
    this.setGroupId = function(id) {
        pid = id;
    }

    this.containsChunk = function(chunkId) {
        printDebug("containsChunk = " + loadedChunks);
        ret=false;
        for(ii=0; ii < loadedChunks.length; ii++) {
            if(chunkId == loadedChunks[ii]) {
                ret=true;
                break;
            }
        }
        return ret;
    }

    this.addChunk = function(chunkId) {
        loadedChunks.push(chunkId);
    }

    this.addItems = function(inItems) {
        for (var loop=0; loop < inItems.length ; loop++) {
            items.push(inItems[loop]);
            map.put(inItems[loop].id, inItems[loop]);
            createTile(inItems[loop]);
            if (loop == 0 && !loadImage) {
                showImage(inItems[loop].id);
            }   

        }
        drawTiles();
        rightButton.style.visibility="visible";
        _this.hideProgressIndicator();
    }
    
    this.showProgressIndicator = function() {
        if (indicatorImage) {
            indicatorImage.style.visibility = "visible";
        }
    }  

    this.hideProgressIndicator = function() {
        indicatorImage.style.visibility = "hidden";
    }
    
    function postImageLoad(loadIntoBuffer) {
        if (debug) {
            status2Div.innerHTML = "Try " + imageReloadTries + " " + url + " image.complete=" + imageBuffer.complete;
        }
        // keep calling this funtion until imageReloadTries < IMG_RELOAD_RETRY_MAX
	    if (!imageBuffer.complete) {
            if (imageReloadTries < IMG_RELOAD_RETRY_MAX) {
                setTimeout(function(){this.loadIntoBuffer = loadIntoBuffer;postImageLoad(loadIntoBuffer);},500);
            } else {
                this.hideProgressIndicator();
            }
            imageReloadTries = imageReloadTries + 1;
            return;
        }
        var id;

        _this.hideProgressIndicator();
        if (loadIntoBuffer) {
            imageLoadingPane.src = imageBuffer.src;
        } else {
           imagePane.src = imageBuffer.src;

        }
        // do a cross fade as long as the images aren't the same
        if (imageLoadingPane.src != imagePane.src) {
            crossFade(0,loadIntoBuffer );
        }
    }
    

    this.showImage = function(itemId) {

        _this.showProgressIndicator();
        //setTimeout(this.showProgressIndicator,0);
        var i = map.get(itemId);
        
        if (!i) {
            return;
        }
        dojo.event.topic.publish("/catalog", {type:"showingItem", id: itemId, rating: i.rating});
        // create the image pane and append the description nodes
        // asumption is that if the imagePane is not set neigher are the info children
        if (typeof imagePane == 'undefined') {
            imagePane = document.createElement("img");
            imagePane.style.width = IMAGEPANE_WIDTH + "px";
            imagePane.style.height = IMAGEPANE_HEIGHT  + "px";
            imagePane.id = IMAGE_PANE_ID;
            
            var targetElement = document.getElementById("bodySpace");
            imageLoadingPane = document.createElement("img");
            imageLoadingPane.style.position = "absolute";
            imageLoadingPane.style.visibility = "hidden";
            imageLoadingPane.style.width = IMAGEPANE_WIDTH + "px";
            imageLoadingPane.style.height = IMAGEPANE_HEIGHT  + "px";
            imageLoadingPane.id = IMAGE_PANE_BUFFER_ID;
            targetElement.appendChild(imagePane);
            targetElement.appendChild(imageLoadingPane);
            imageLoadingPane.style.left = tileX + "px";
            
            loadImage(i.image, false);
        } else {
             imageLoadingPane.style.visibility = "visible";            
             if (showingBuffer) {
                 showingBuffer = false;
             } else {
                 showingBuffer = true;
             }
             loadImage(i.image, showingBuffer);
        }
    }
    
    function loadImage(url, loadIntoBuffer) {
        imageReloadTries = 0;
        imageBuffer = new Image();
        if (loadIntoBuffer) {
            imageBuffer.src = url;
            imageLoadingPane.onLoad = setTimeout(function(){this.url=url;this.loadIntoBuffer = loadIntoBuffer;postImageLoad(loadIntoBuffer,url);},0);
        } else {
            imageBuffer.src = url;
            imageBuffer.onLoad = setTimeout(function(){this.url = url;this.loadIntoBuffer = loadIntoBuffer;postImageLoad(loadIntoBuffer,url);},0);
        }
    }
    
    function setOpacity(opacity, id) {
        var target = document.getElementById(id);
        if (typeof target.style.filter != 'undefined') {
            target.style.filter = "alpha(opacity:" + opacity + ")"; 
        } else {
            target.style.opacity = opacity/100;
        }            
    }
      
    function crossFade(count,loadIntoBuffer) {
       var percentage = Number(count);
        if (loadIntoBuffer) {
            setOpacity(100 - percentage, IMAGE_PANE_ID);
            setOpacity(percentage, IMAGE_PANE_BUFFER_ID);
        } else {
            setOpacity(100 - percentage, IMAGE_PANE_BUFFER_ID);
            setOpacity(percentage, IMAGE_PANE_ID);

        }
       if (percentage < 100) {
            percentage = percentage + 10;
            setTimeout(function(){this.loadIntoBuffer = loadIntoBuffer;this.percentage = percentage;crossFade(percentage,loadIntoBuffer);}, 25);
        }
    }
    
    
    // calling this function will result in the maximizing event being fired
    // if the pane is maximized it will asume the event want to minimize
    this.doMaximize = function() {
        if (!maximizing && !minimizing && !maximized) {
            infoPaneLoop = INFOPANE_DEFAULT_HEIGHT;
            maximizing = true;
            minimizing = false;
        } else if (!maximizing && !minimizing) {
            minimizing = true;
            maximizing = false;
        }
        setTimeout(changeInfoPane, 0);
    }
    
    // will handle either minimizing or maximing but not both
    // this method is called recursively until the maximinging 
    // or minimizing is done.
    function changeInfoPane() {
        if (maximizing) {
            maxmizeInfoPane();
        } else if (minimizing) {
            minimizeInfoPane();
        }
    }
    
    function maxmizeInfoPane() {
        if (infoPaneLoop < INFOPANE_EXPAND_HEIGHT) {
            infoPaneLoop = infoPaneLoop + INFOPANE_INCREMENT;
            var clipMe = 'rect(' + '0px,' + VIEWPORT_WIDTH +  'px,'+  infoPaneLoop +'px,' +  0 + 'px)';
            infoPane.style.clip = clipMe;
            infoPane.style.height = infoPaneLoop;
            infoPane.style.top = (tileY + (PADDING *2) + INFOPANE_DEFAULT_HEIGHT + IMAGEPANE_HEIGHT) - infoPaneLoop;
            setTimeout(changeInfoPane, 5);
        } else {
            minimizeImage.src= MINIMIZE_IMG_URI;
            minimizeLink.title = MINIMIZE_IMG_TOOLTIP;
            maximized = true;
            maximizing = false;
            minimizing = false;
        }
    }
    
    function minimizeInfoPane() {
       if (infoPaneLoop > INFOPANE_DEFAULT_HEIGHT) {
            infoPaneLoop = infoPaneLoop - INFOPANE_INCREMENT;
            var clipMe = 'rect(' + '0px,' + VIEWPORT_WIDTH +  'px,'+  infoPaneLoop +'px,' +  0 + 'px)';
            infoPane.style.clip = clipMe;
            infoPane.style.height = infoPaneLoop;
            infoPane.style.top = (tileY + (PADDING *2) + INFOPANE_DEFAULT_HEIGHT + IMAGEPANE_HEIGHT) - infoPaneLoop;
            if (debug) {
                status2Div.innerHTML = "minimize infoPaneLoop =" + infoPaneLoop +  " infopane.top=" + infoPane.style.top;
            }
            setTimeout(changeInfoPane, 5);
        } else {
            minimizeImage.src= MAXIMIZE_IMG_URI;
            minimizeLink.title = MAXIMIZE_IMG_TOOLTIP;
            maximizing = false;
            minimizing = false;
            maximized = false;
        }
    }
    
    function scrollRight() {
        isScrollingRight = true;
        if ( (index + 4) >= tiles.length) {
            // hide the rightButton
            rightButton.style.visibility="hidden";
            return;
        } else {
            leftButton.style.visibility="visible";
        }
        offset = offset - SCROLL_INCREMENT;
        drawTiles();
        setTimeout(scroll, timeout);
    }
    
    function getNext() {
        isScrollingRight = true;
        setTimeout(scroll, timeout);
    }
    

    function getPrevious () {
        isScrollingLeft = true;
        setTimeout(scroll, timeout);
    }
    
    function scrollLeft() {
        if (offset >= 0) {
            leftButton.style.visibility="hidden";
            return;
        } else {
            leftButton.style.visibility="visible";
        }
        offset = offset + SCROLL_INCREMENT;
        drawTiles();
        setTimeout(scroll, timeout);
    }
    
    function drawTiles() {
        // draw the first one if its off the screen
        // check if the far right image is out view
        var overHang;
        var temp = offset;
        index = Math.floor((offset)/THUMB_WIDTH); 
        overHang =  offset % THUMB_WIDTH;
        if (overHang < 0) {
            overHang = overHang * -1;
        }
        if (index < 0) {
            index = index * -1;
        }
        // check for next set of images
        prefetch();
        var startIndex = index;
        if (overHang > 0 && index >0) {
            startIndex = index -1;
        }
        var stopIndex = index + Math.round(VIEWPORT_WIDTH / THUMB_WIDTH);    
        if (stopIndex > tiles.length) {
            stopIndex = tiles.length;
        }
        var displayX = 0;
        for (var tl=startIndex; tl < stopIndex; tl++) {
            if (debug) {
             statusDiv.innerHTML = "overhang=" + overHang +  " startIndex=" + startIndex + " stopIndex="  + stopIndex + " offset=" + offset + " displayX=" + displayX;
            }
            if (overHang  > 0 && tl == startIndex) {
                rightButton.style.visibility="visible";
                // clip: rect(top right bottom left) - borders of the clipped area
                // clip the left
                var clipMe = 'rect(' + '0px,' + THUMB_WIDTH +  'px,'+  THUMB_HEIGHT +'px,' +  overHang + 'px)';
                tiles[tl].style.clip = clipMe;
                tiles[tl].style.left = (tileX  - overHang) + "px";
                displayX = displayX + (THUMB_WIDTH - overHang);
            } else if (tl == stopIndex -1) {
                var underHang = VIEWPORT_WIDTH - displayX ;
                if (underHang > 0 && underHang) {
                    var clipMe = 'rect(' + '0px,' + (underHang) + "px," + THUMB_HEIGHT +'px,' +  0 + 'px)';
                    tiles[tl].style.clip = clipMe;
                    tiles[tl].style.left =  tileX + (offset + (tl * THUMB_WIDTH)) + 'px';
                    tiles[tl].style.visibility = "visible";
                    // resize the previous one to its real length
                } else if (underHang < 0 && tl > 0) {
                    var clipMe = 'rect(' + '0px,' + (THUMB_WIDTH + underHang) + "px," + THUMB_HEIGHT +'px,' +  0 + 'px)';
                    tiles[tl-1].style.clip = clipMe;
                    tiles[tl-1].style.visibility = "visible";
                    tiles[tl-1].style.left = tileX + (offset + ((tl -1) * THUMB_WIDTH)) + 'px';
                } else { 
                    tiles[tl].style.left = '0px';
                    tiles[tl].style.visibility = "hidden";
                }
            } else {
                displayX = displayX + THUMB_WIDTH;
                tiles[tl].style.left = tileX + (offset + (tl * THUMB_WIDTH)) + 'px';
                tiles[tl].style.visibility = "visible";
            }	    	
        }
        if (stopIndex < tiles.length) {
            tiles[stopIndex].style.visibility = "hidden";
            tiles[stopIndex].style.left = "0px";
        }
    }

    
    this.load = function () {
        map = new Map();
        dojo.event.connect(window, "onresize", layout);
	    var loadImage;
	    
        var targetRow = document.getElementById("targetRow");
        injectionPoint = document.getElementById("injection_point");
        
        // for status output
        statusDiv = document.getElementById("status");
        status2Div = document.getElementById("status_2");

        initLayout();
        initialized = true;
    }
    
    function initLayout() {
        containerDiv = document.getElementById("CatalogBrowser");
        rightButton = document.getElementById("right_button");
        leftButton = document.getElementById("left_button");
        layout();
        leftButton.style.visibility="hidden";
        if (typeof rightButton.attachEvent != 'undefined') {
                rightButton.attachEvent('onmouseover',function(e){scrollDone();getNext();});
                rightButton.attachEvent('onmouseout',function(e){scrollDone();});
                leftButton.attachEvent('onmouseover',function(e){scrollDone();getPrevious();});
                leftButton.attachEvent('onmouseout',function(e){scrollDone();});
            } else if (typeof rightButton.addEventListener != 'undefined') {
                rightButton.addEventListener('mouseover',function(e){scrollDone();getNext();}, false);
                rightButton.addEventListener('mouseout',function(e){scrollDone();}, false);
                leftButton.addEventListener('mouseover',function(e){scrollDone();getPrevious();}, false);
                leftButton.addEventListener('mouseout',function(e){scrollDone();}, false);
            }
        createInfoPane();
    }
    
    function layout() {
        var ua = navigator.userAgent.toLowerCase();

        // this will need to be made generic depending on the thumb height
        tileY = findY(containerDiv);
        tileX = findX(containerDiv) + 4;
        var rightX = tileX + VIEWPORT_WIDTH - 20;
        rightButton.style.left = rightX  +  "px";
        var  buttonY = tileY + IMAGEPANE_HEIGHT + INFOPANE_DEFAULT_HEIGHT + 12;
        rightButton.style.top = buttonY + "px";
        leftButton.style.top = buttonY + "px";
       
        if (ua.indexOf('ie') != -1) {
            isIE = true;
        } else if (ua.indexOf('safari') != -1) {
            tileX = tileX + 8;
            timeout = 20;
        }
         drawTiles();
         if (infoPane) {
            infoPane.style.left = tileX + "px";
            if (maximized) {
            
                infoPane.style.top = (tileY + IMAGEPANE_HEIGHT  + (PADDING*2) - infoPane.style.height) + "px";
            } else {
                infoPane.style.top = (tileY + IMAGEPANE_HEIGHT  + (PADDING*2)) + "px";
            }
            if (maximized) {
                infoPaneLoop = infoPane.style.height;
            } else {
                infoPaneLoop = INFOPANE_DEFAULT_HEIGHT;
            }
         }
         if (typeof imageLoadingPane != 'undefined') {           
             imageLoadingPane.style.left = tileX;
             imageLoadingPane.style.top = tileY;
         }
    }
    
    function createInfoPane() {
	    infoPane = document.getElementById("infopane");
        infoPane.style.width = VIEWPORT_WIDTH + "px";
        // give room for 4 pixels above and below
        infoPane.style.height = (INFOPANE_DEFAULT_HEIGHT) + "px";
        // give 3px padding for a border
        infoPane.style.top = (tileY + IMAGEPANE_HEIGHT + (PADDING*2)) + "px";
        infoPane.style.left = tileX + "px";
        infoTableMinimize = document.getElementById("infopaneDetailsIcon");                 
        indicatorCell = document.getElementById("infopaneIndicator");
        indicatorCell.style.width = (10) + "px";
        indicatorImage = document.createElement("img");
        indicatorImage.className = "infopaneIndicator";
        indicatorImage.src = INDICATOR_IMG_URI;
        indicatorImage.style.visibility = "hidden";
        indicatorCell.appendChild(indicatorImage);
        minimizeLink = document.createElement("a");
        minimizeLink.className = "infopaneLink";
        minimizeLink.title = MAXIMIZE_IMG_TOOLTIP;
        minimizeImage = document.createElement("img");
        minimizeImage.src= MAXIMIZE_IMG_URI;
        minimizeLink.appendChild(minimizeImage);
        infoTableMinimize.appendChild(minimizeLink);
        
        if (typeof minimizeLink.attachEvent != 'undefined') {
            minimizeLink.attachEvent("onclick",function(e){_this.doMaximize();});
        } else {
            minimizeLink.addEventListener("click",function(e){_this.doMaximize();}, true);
        }
        var clipMe = 'rect(' + '0px,' + VIEWPORT_WIDTH +  'px,'+  INFOPANE_DEFAULT_HEIGHT +'px,' +  0 + 'px)';
        infoPane.style.clip = clipMe;
	}

    function createTile(i) {
        var div = document.createElement("div");
        div.className = "tile";
        div.id = i.id;
        var link = document.createElement("a");
        var img = document.createElement("img");
        img.title = i.name;
        img.src = i.thumbnail;
        img.className = "tileImage";
        link.appendChild(img);
        link.setAttribute("id", i.id);
        if (typeof div.attachEvent != 'undefined') {
            div.attachEvent('onclick',function(e){this.id = div.id; _this.showImage(this.id, false);});
        } else {
            link.addEventListener('click',function(e){this.id = div.id; _this.showImage(this.id, false);}, true);
        }
        div.appendChild(link);
        injectionPoint.appendChild(div);
        div.style.top = tileY + INFOPANE_DEFAULT_HEIGHT + IMAGEPANE_HEIGHT + (PADDING * 3) + "px";
        tiles.push(div);
    }
    
       
    function findY(element) {
        var t = 0;
        if (element.offsetParent) {
            while (element.offsetParent) {
                t += element.offsetTop
                element = element.offsetParent;
            }
        } else if (element.y) {
            t += element.y;
        }
        return t;
    }
    
    function findX(element) {
        var l = 0;
        if (element.offsetParent) {
            while (element.offsetParent) {
                l += element.offsetLeft
                element = element.offsetParent;
            }
        } else if (element.x)
            l += element.x;
        return l;
    }
}
