/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: rss.js,v 1.3 2006/05/31 19:13:03 basler Exp $ */

dojo.require("dojo.io.*");

if (bpui = 'undefined') {
	bpui = new Object();
}

bpui.RSS = function() {
    var jsonData = null;
    var rssItemNum = 0;
    var currentItem = 0;
    var rssTimeout = null;
    var itemIntervalMsec = 2000;
    var rssFadeoutCounter = 110;
    var rssFadeinCounter = 110;
    var itemIntervalId = 0;
    
    this.getRssInJson = function (uri, number) {
        rssItemNum = number;
        var encodedURI = encodeURI(bpui.contextRoot + "/faces/dynamic/bpui_rssfeedhandler/getRssfeed?style=json&itemNumber="+number+"&url="+uri);
        //alert("encoded url=" + encodedURI);
        var bindArgs = {
                    url: encodedURI,
                    mimetype: "text/json",
                    load: function (type, data, http) {
                        handleJsonRss(data);
                        for (var key in data.channel.item) {
                            dojo.debug("ITEM Title ", key, ":", data.channel.item[key].title);
                            dojo.debug("ITEM LInk ", key, ":", data.channel.item[key].link);
                        }
                    },
                    error: function (t, e) {
                        dj_debug("ERROR : " + e.message);
                    }
        }
        dojo.io.bind(bindArgs);
        return false;
    }

    function handleJsonRss(json) {
        jsonData = json;
        // setting top title and link
        generateHref(json.channel.title, json.channel.link, "rss-channel");
        // setting items
        generateHref(json.channel.item[0].title, json.channel.item[0].link, "rss-item");
        var aNodes = document.getElementById("rss-item").getElementsByTagName("a");
        dojo.event.connect(aNodes[0], "onmouseover", "pauseCycle");
        dojo.event.connect(aNodes[0], "onmouseout", "resumeCycle");
        cycleRss();
    }

    function generateHref (title, link, nodeId) {
        var node = document.getElementById(nodeId);
        var aNode = document.createElement("a");
        aNode.setAttribute("href", link);
        aNode.appendChild(document.createTextNode(title));
        if (node.hasChildNodes()) {
            node.removeChild(node.firstChild);
        }
        node.appendChild(aNode);
    }

    function cycleRss () {
        itemIntervalId = setTimeout(replaceItem, itemIntervalMsec);
    }
    
    
    function replaceItem() {
        if (itemIntervalId) {
            clearTimeout(itemIntervalId);
        }
        // fadeout the current item and pop the next one in.
        var cItem = document.getElementById("rss-item");
        dojo.fx.html.fadeOut(cItem.getElementsByTagName('a')[0], 500);
        var waitId = setTimeout(function(waitId) {
            clearTimeout(waitId);
            //cItem.removeChild(cItem.firstChild);
            if (currentItem < (rssItemNum -1)) {
                currentItem += 1;
            } else {
                currentItem = 0;
            }
            generateHref(jsonData.channel.item[currentItem].title, jsonData.channel.item[currentItem].link, "rss-item");
            // attach event for onmouseover(pause) and onmouseout(resume)
            var aNodes = cItem.getElementsByTagName("a");
            dojo.event.connect(aNodes[0], "onmouseover", "pauseCycle");
            dojo.event.connect(aNodes[0], "onmouseout", "resumeCycle");
            cycleRss();}, 500);
    }

    function pauseCycle(evt) {
        if (itemIntervalId) {
            clearTimeout(itemIntervalId);
        }
    }

    function resumeCycle (evt) {
        cycleRss();
    }

    bpui.getContextRoot = function() {
        var urlArray=window.location.toString().split("/", 4);
        return "/" + urlArray[3];
    }

    bpui.contextRoot = bpui.getContextRoot();
}

