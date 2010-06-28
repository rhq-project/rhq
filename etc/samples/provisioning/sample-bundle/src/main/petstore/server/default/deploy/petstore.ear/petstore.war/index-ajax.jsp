<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: index.jsp,v 1.17 2007/03/15 23:19:46 basler Exp $ --%>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.*, com.sun.javaee.blueprints.petstore.model.CatalogFacade, com.sun.javaee.blueprints.petstore.model.Tag"%>

<%
try {
    CatalogFacade cf = (CatalogFacade)config.getServletContext().getAttribute("CatalogFacade");
    List<Tag> tags=cf.getTagsInChunk(0, 12);
    // since top 20 come from database or desending refCount order, need to reorder by tag name
    Collections.sort(tags, new Comparator() {
        public int compare(Object one, Object two) {
             int cc=((Tag)two).getTag().compareTo(((Tag)one).getTag());
             return (cc < 0 ? 1 : cc > 0 ? -1 : 0);
        }
    });    
%>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
        <title>Java Pet Store Reference Application</title>
        <script type="text/javascript" src="./common.js"></script>
        <link type="text/css" rel="stylesheet" href="./tag.css"/>
        <script type="text/javascript">
                //var djConfig = {isDebug: true };
        </script>
    </head>
    <body>
        
        <jsp:include page="banner.jsp" />
        <script>
            dojo.require("dojo.widget.FisheyeList");
            dojo.require("dojo.io.*");
            dojo.require("dojo.io.ScriptSrcIO");
            //transport: "ScriptSrcTransport",
            //url: "http://localhost:8080/petstore/downloadAd.json",

            function browse(category) {
                window.location.href="${pageContext.request.contextPath}/faces/catalog.jsp?catid=" + category;
            
            }
            

            function debugProperties(namex) {
                var listx="";
                var ob=namex;
                for(xx in ob) {
                    listx += xx + " = " + ob[xx] + "<br/>"
                }
                //document.write(listx);
                alert(listx);
            }            
            
            function checkAdPage() {
                var bindArgs = {
                    //url: "https://blueprints.dev.java.net/petstore/downloadAd.json",
                    //url: "http://localhost:8080/petstore/downloadAd.json",
                    url: "http://search.yahooapis.com/ImageSearchService/V1/imageSearch?appid=YahooDemo&query=Madonna&output=json&callback=ws_results",
                    transport: "ScriptSrcTransport",
                    jsonParamName: "callback",
                    mimetype: "text/json",
                    load: function(type, data, event, kwArgs) { 
                        /* type will be "load", data will be response data,  event will null, and kwArgs are the keyword arguments used in the dojo.io.bind call. */ 
                        alert("load = " + data.ResultSet.totalResultsAvailable);
                        //debugProperties(data);
                        //document.getElementById("downloadAds").innerHTML=data.downloadxx;

                        
                    },
                    error: function(type, data, event, kwArgs) { 
                        /* type will be "error", data will be response data,  event will null, and kwArgs are the keyword arguments used in the dojo.io.bind call. */ 
                        alert("error");
                    },
                    timeout: function() { 
                        /* Called if there is a timeout */
                        alert("timeout");
                    },
                    timeoutSeconds: 10};
                // dispatch the request
                dojo.io.bind(bindArgs);      
            }
            
            function callback() {
                alert("callback");
            }

            function returnFunctionx(type, data, evt) {
                // statically setup popup for simple case
                // check return of the dojo call to make sure it is valid
                if (evt.readyState == 4) {
                    if (evt.status == 200) {
                        alert("data = " + data.downloadxx);
                        //document.getElementById("downloadAds").innerHTML=data;
                        document.getElementById("downloadAds").innerHTML=data.downloadxx;
                    }
                }
            }            

            function testit() {
                testx={download:"test", textx:"it"};
                alert("test = " + testx.download);
            }
            
            
        </script>
            <span onclick="checkAdPage();">test it</span>
        
        
            <table bgcolor="white">
                <tr>
                    <td valign="top">
                        <div class="outerbar" style="width: 200px">
                            
                            <div dojoType="FisheyeList"
                                 itemWidth="170" itemHeight="50"
                                 itemMaxWidth="340" itemMaxHeight="100"
                                 orientation="vertical"
                                 effectUnits="2"
                                 itemPadding="10"
                                 attachEdge="top"
                                 labelEdge="bottom"
                                 enableCrappySvgSupport="false">
                                
                                <div dojoType="FisheyeListItem" onClick="browse('Dogs');" 
                                     iconsrc="${pageContext.request.contextPath}/images/dogs_icon.gif">
                                </div>
                                
                                <div dojoType="FisheyeListItem" onClick="browse('Cats');"
                                     iconsrc="${pageContext.request.contextPath}/images/cats_icon.gif">
                                </div>
                                
                                <div dojoType="FisheyeListItem" onClick="browse('Birds');"
                                     iconsrc="${pageContext.request.contextPath}/images/birds_icon.gif">
                                </div>
                                
                                <div dojoType="FisheyeListItem" onClick="browse('Fish');"
                                     iconsrc="${pageContext.request.contextPath}/images/fish_icon.gif">
                                </div>
                                
                                <div dojoType="FisheyeListItem" onClick="browse('Reptiles');"
                                     iconsrc="${pageContext.request.contextPath}/images/reptiles_icon.gif">
                                </div>
                            </div>
                            
                        </div>
                    </td>
                    <td valign="top" style="width: 400px">
                        <div id="bodyCenter">
                            <table valign="top" id="bodyTable" border="0">
                                <tr>
                                    <td>
                                        <map name="petmap">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Birds')" 
                                                  alt="Birds" 
                                                  coords="72,2,280,250">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Fish')" 
                                                  alt="Fish" 
                                                  coords="2,180,72,250">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Dogs')" 
                                                  alt="Dogs" 
                                                  coords="60,250,130,320">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Reptiles')" 
                                                  alt="Reptiles" 
                                                  coords="140,270,210,340">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Cats')" 
                                                  alt="Cats" 
                                                  coords="225,240,295,310">
                                            <area onmouseover="javascript:this.style.cursor='pointer';" onclick="browse('Birds')" 
                                                  alt="Birds" 
                                                  coords="280,180,350,250">
                                        </map>
                                        
                                        <img src="${pageContext.request.contextPath}/images/splash.gif" 
                                             alt="Pet Selection Map"
                                             usemap="#petmap" 
                                             width="350" 
                                             height="355" 
                                             border="0">
                                    </td>
                                </tr>
                            </table>
                            
                        </div>
                    </td>
                    <td style="vertical-align:top;">
                        <div style="border-style: double; width:100%;">
                            <table border="0">
                                <tr>
                                    <th colspan="2" style="text-align:center">Most Popular Tags</th>
                                </tr>
                                <tr>
<%
    String style=null;
    int refx=0, ii=0;
    for(Tag tag : tags) {
        refx=tag.getRefCount() / 5;
        if(refx >= 3) {
            style="xxlarge";
        } else if(refx == 2) {
            style="xlarge";
        } else if(refx == 1) {
            style="large";
        } else {
            style="medium";
        }

        if((ii % 2) == 0) out.println("</tr>\n<tr>");
        out.println("<td class='tagCell' style='text-align: center'><a href='./tag.jsp?tag=" + 
            tag.getTag() + "'><span class='" + style +"'>" +  tag.getTag() + "</span></a></td>");
        ii++;
    }
%>
                                </tr>
                            </table>
                        </div>
                    </td>
                    <td valign="top" style="vertical-align:top; width:250px;">
                        <div id="downloadAds">
                            <jsp:include page="download.jsp" />
                        </div>
                    </td>
                </tr>
            </table>
        
        <div style="position: absolute; visibility: hidden;z-index:5" id="menu-popup">
            <table id="completeTable" class="popupTable" ></table>
        </div>
        
        <br/><br/>
        <jsp:include page="footer.jsp" />
    </body>
</html>
<%
    } catch(Exception e) {
        e.printStackTrace();
    }
%>
