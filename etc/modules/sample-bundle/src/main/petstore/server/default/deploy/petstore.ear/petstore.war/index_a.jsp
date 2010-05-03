<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: index.jsp,v 1.18 2007/03/16 15:29:15 basler Exp $ --%>
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
        <link type="text/css" rel="stylesheet" href="./tag.css"/>
        <script type="text/javascript" src="https://blueprints.dev.java.net/petstore/downloadAd.js"></script>
    </head>
    <body onload="alterDownloadAd()">
        
        <jsp:include page="banner.jsp" />
        <script>
            dojo.require("dojo.widget.FisheyeList");
            function browse(category) {
                window.location.href="${pageContext.request.contextPath}/faces/catalog.jsp?catid=" + category;
            }
            
            function alterDownloadAd() {
                if(typeof checkAdPage != "undefined") { 
                    var textx=checkAdPage();
                    if(typeof textx != "undefined") { 
                        document.getElementById("downloadAds").innerHTML=textx;
                    }
                }
            }
            
            
        </script>
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
                    <td style="vertical-align:top; width:250px; text-align:right;">
                        <div id="downloadAds">
                            <div id="downloadAds" style="text-align:center; border-style:none; width:100%;">
                                <table style="width:100%">
                                    <tr>
                                        <td style="text-align:center;">
                                            <a href="http://java.sun.com/javaee/downloads/index.jsp" target="downloads"><img style="border:none" src="./images/ad-sdk.jpg" alt="Download Java EE 5 SDK"/></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="text-align:center;">
                                            <a href="http://www.netbeans.org/downloads/index.html" target="downloads"><img style="border:none" src="./images/ad-netbeans.jpg" alt="Download Netbeans IDE"/></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="text-align:center;">
                                            <a href="http://java.com/en/download/index.jsp" target="downloads"><img style="border:none" src="./images/ad-jre.jpg" alt="Download Java SE"/></a>
                                        </td>
                                    </tr>
                                </table>
                            </div>                        
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
