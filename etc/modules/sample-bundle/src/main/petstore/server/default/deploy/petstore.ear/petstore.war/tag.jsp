<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: tag.jsp,v 1.11 2007/01/17 18:00:09 basler Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.*, com.sun.javaee.blueprints.petstore.model.CatalogFacade, com.sun.javaee.blueprints.petstore.model.Tag"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib prefix="ui5" uri="http://java.sun.com/blueprints/ui" %>


<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Tag Page</title>
        <script type="text/javascript" src="./common.js"></script>
        <link type="text/css" rel="stylesheet" href="./tag.css"/>
<%
try {
    CatalogFacade cf = (CatalogFacade)config.getServletContext().getAttribute("CatalogFacade");
    List<Tag> tags=cf.getTagsInChunk(0, 90);
    // since top 20 come from database or desending refCount order, need to reorder by tag name
    Collections.sort(tags, new Comparator() {
        public int compare(Object one, Object two) {
             return ((Tag)one).getTag().compareTo(((Tag)two).getTag());
        }
    });    
%>
    <script language="javascript">
        function retrieveItems(tag) {
            var bindArgs = {
                // url when using the jsp to serve the ajax request
                url: "../tagItemLookup.jsp?tag=" + escape(tag),
                mimetype: "text/xml",
                load: returnFunctionx,
                error: ajaxBindError};

            // dispatch the request
            dojo.io.bind(bindArgs);      
        }
        
        
        function returnFunctionx(type, data, evt) {
            // statically setup popup for simple case
            var componentId="displayItems";
            // check return of the dojo call to make sure it is valid
            if (evt.readyState == 4) {
                if (evt.status == 200) {
                    // get results and replace dom elements
                    var itemsx=data.getElementsByTagName("item");
                    display="<table class='itemTable'><tr><td class='itemCell' align='center' colspan='4'><h2>Tag: " + 
                        data.getElementsByTagName("tag")[0].childNodes[0].nodeValue + 
                        "</h2></td></tr><tr><th class='itemCell'>Name</th><th class='itemCell'>Description</th><th class='itemCell'>Tags</th><th class='itemCell'>Price</th></tr>"
                    for(ii=0; ii < itemsx.length; ii++) {
                        display += "<tr>";
                        display +="<td class='itemCell'><a href='./catalog.jsp?pid="+ itemsx[ii].getElementsByTagName("productID")[0].childNodes[0].nodeValue +"&itemId=" + 
                            itemsx[ii].getElementsByTagName("itemID")[0].childNodes[0].nodeValue + "' onmouseover='bpui.popup.show(&quot;pop1&quot;, event, &quot;" + 
                            itemsx[ii].getElementsByTagName("itemID")[0].childNodes[0].nodeValue + "&quot;)' onmouseout='bpui.popup.hide(&quot;pop1&quot;)'>" + 
                            itemsx[ii].getElementsByTagName("name")[0].childNodes[0].nodeValue +"</a></td>";
                        display +="<td class='itemCell'>" + itemsx[ii].getElementsByTagName("description")[0].childNodes[0].nodeValue +"</td>";
                        display +="<td class='itemCell'>" + itemsx[ii].getElementsByTagName("tags")[0].childNodes[0].nodeValue +"</td>";
                        display +="<td class='itemCell' style='text-align: right'>" + itemsx[ii].getElementsByTagName("price")[0].childNodes[0].nodeValue +"</td>";
                        display +="</tr>";
                    }
                    display += "</table>";
                    document.getElementById(componentId).innerHTML=display;
                    document.getElementById(componentId).style.visibility='visible';
                } else if (evt.status == 204){
                    alert("204 return");
                }
            }
        }
    
        
        function checkQueryString() {
            <!-- add script to check for tag in query string, if exists then retrieve data -->
            var iPos=window.location.href.indexOf("?tag=");
            if(iPos > -1) {
                // have tag so retrieve items
                retrieveItems(window.location.href.substr(iPos + 5));
            }
        }

    </script>
    </head>
    <body onload="checkQueryString();">   
        <jsp:include page="banner.jsp" />
        <f:view>
            
                <ui5:popupTag id="pop1" xmlHttpRequestURL="../lookup.jsp?popupView=2&itemId=" 
                    elementNamePairs="name=value1,description=value2,price=value3,image=imageId">
                    <!-- Used as spaces to center the table, this could be done programmatically,
                    but browser diff, so I left it in the hands of the component user.  The image is located 
                    in the component jar so for retrieval, push it through the faces servlet -->
                    <img id="spaceImage" height="10px" width="10px" src="${pageContext.request.contextPath}/faces/static/META-INF/popup/images/spacer.gif" align="left">
                    <table border="0" width="270px" bgcolor="#ffffff" cellpadding="5" cellspacing="5">
                        <tr>
                            <td align="left" valign="top"><b>Name:</b>
                            <span id="value1">Loading Data...</span></td>
                        </tr>
                        <tr>
                            <td align="left" valign="top"><b>Seller Address:</b>
                            <span id="value2">Loading Data...</span></td>
                        </tr>
                        <tr>
                            <td align="left" valign="top"><b>Price:</b>
                            <span id="value3">Loading Data...</span></td>
                        </tr>
                        <tr>
                            <td colspan="2" align="center"><img name="image" id="imageId" src="" 
                            alt="[Loading Image...]" border="2"/><br/><br/></td>
                        </tr>
                    </table> 
                </ui5:popupTag>            
            <center>
                <h1>Tag Page</h1>
                <table border="0">
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
        
        if((ii % 6) == 0) out.println("</tr>\n<tr>");
        //out.println("<td class='tagCell'><span onclick=\"retrieveItems('" + tag.getTag() + "')\" class='" + style +"'>" +  tag.getTag() + "</span> (" + tag.getRefCount() + ")</td>");
        out.println("<td class='tagCell'><span onclick=\"retrieveItems('" + tag.getTag() + "')\" class='" + style +"'>" +  tag.getTag() + "</span></td>");
        ii++;
    }
%>
                    </tr>
                </table>
                <div id="displayItems" class="items">
                    
                </div>
            </center>
        </f:view>
        <br/><br/><br/><br/>
        <jsp:include page="footer.jsp" />
        
    </body>
</html>

<%
    } catch(Exception e) {
        e.printStackTrace();
    }
%>
