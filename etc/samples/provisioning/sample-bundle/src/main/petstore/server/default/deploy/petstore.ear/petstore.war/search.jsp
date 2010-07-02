<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: search.jsp,v 1.28 2006/12/04 21:34:10 basler Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.*, com.sun.javaee.blueprints.petstore.search.*"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib prefix="ui5" uri="http://java.sun.com/blueprints/ui" %>


<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Search Page</title>
        <style>
            .itemTable {
                padding: 0.3cm;
                width: 800px;
                border-style: double; 
                border-color: darkgreen; 
            }
            .itemCell {
                border-style: solid; 
                border-color: darkgreen; 
                border-width: thin;
                padding: 5px
            }
            .tagDiv {
                border-style: groove; 
                border-color: darkgreen; 
                background-color: white;
                border-width: thick;
                padding: 5px;
                visibility: hidden;
                position:absolute;
                left:0px;
                top:0px;
                z-index: 3;
            }
        </style>
        <script type="text/javascript" src="common.js"></script>
    </head>
    <body>   
        <jsp:include page="banner.jsp" />
        <center>

            <script type="text/javascript">
                function checkAll() {
                    var elems=dojo.byId("resultsForm").elements;
                    for(ii=0; ii < elems.length; ii++) {
                        if(elems[ii].name.indexOf("mapSelectedItems") >= 0) {
                            elems[ii].checked=true;
                        }
                    }
                    return false;
                }
    
                function uncheckAll() {
                    var elems=dojo.byId("resultsForm").elements;
                    for(ii=0; ii < elems.length; ii++) {
                        if(elems[ii].name.indexOf("mapSelectedItems") >= 0) {
                            elems[ii].checked=false;
                        }
                    }
                    return false;
                }
                
                function addTags(eventx, namex, itemIdx) {
                    var xx=0;
                    var yy=0;
                    if (!eventx) var eventx=window.event;
                    if (eventx.pageX || eventx.pageY){
                        xx=eventx.pageX;
                        yy=eventx.pageY;
                    } else if (eventx.clientX || eventx.clientY) {
                        xx=eventx.clientX + document.body.scrollLeft;
                        yy=eventx.clientY + document.body.scrollTop;        
                    }
                    divId="addTags";
                    document.getElementById("addTagsItemId").value=itemIdx;
                    document.getElementById("addTagsTags").value="";
                    document.getElementById("addTagsTitle").innerHTML="<b>Add Tags to '" + namex + "'</b>";
                    document.getElementById(divId).style.left=(xx - 170) + "px";
                    document.getElementById(divId).style.top=(yy - 140) + "px";
                    document.getElementById(divId).style.visibility='visible';
                    document.getElementById("addTagsTags").focus();
                }
                
                function saveAddTags() {
                    // get data and send to controller servlet
                    itemIdx=document.getElementById("addTagsItemId").value;
                    tagsx=document.getElementById("addTagsTags").value;
                    var bindArgs = {
                        url:        "../TagServlet?itemId=" + escape(itemIdx) + "&tags=" + escape(tagsx),
                        mimetype:   "text/xml",
                        error: ajaxBindError,
                        load: function(type, data, evt){
                            // check successful response
                            if (evt.readyState == 4) {
                                if (evt.status == 200) {
                                    // get results and replace dom elements
                                    var resultx=data.getElementsByTagName("response")[0];
                                    itemIdx=resultx.getElementsByTagName("itemId")[0].childNodes[0].nodeValue;
                                    
                                    // change DOM data
                                    document.getElementById("ITEMID_TAGS_" + itemIdx).innerHTML=resultx.getElementsByTagName("tags")[0].childNodes[0].nodeValue;

                                } else if (evt.status == 204){
                                    alert("204 return");
                                }
                            }
                        }
                    };

                    dojo.io.bind(bindArgs);      
                    // make sure it was updated
                    
                    // show messages if error
                    
                    // hide popup
                    document.getElementById(divId).style.visibility='hidden';
                }
                
                function cancelAddTags() {
                    // hide popup
                    document.getElementById(divId).style.visibility='hidden';
                }
                
            </script>        
  
            <f:view>
        
                <ui5:popupTag id="pop1" xmlHttpRequestURL="../lookup.jsp?popupView=2&itemId=" 
                    elementNamePairs="name=value1,description=value2,price=value3,image=imageId">
                    <!-- Used as spaces to center the table, this could be done programmatically,
                    but browser diff, so I left it in the hands of the component user.  The image is located 
                    in the component jar so for retrieval, push it through the faces servlet -->
                    <img id="spaceImage" height="10px" width="3px" src="${pageContext.request.contextPath}/faces/static/META-INF/popup/images/spacer.gif" align="left">
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
        
                <h1>Search Page</h1>
                <h:form id="searchForm">
                    <table class="itemTable" style="width: 700px">
                        <tr>
                            <th class="itemCell">Search String</th>
                            <td class="itemCell">
                                <h:inputText size="50" id="searchString" value="#{SearchBean.searchString}"/>
                                &nbsp;&nbsp;&nbsp;Also Search Tags:<h:selectBooleanCheckbox id="searchTags" value="#{SearchBean.searchTags}"/>
                            </td>
                        </tr>
                        <tr>
                            <td  class="itemCell" align="center" colspan="2">
                                <h:commandButton action="#{SearchBean.searchAction}" id="searchSubmit" type="submit" value="Submit"/>
                                <h:commandButton id="searchReset" type="reset" value="Reset"/>
                            </td>
                        </tr>
                    </table>
                    <h:messages/>
                </h:form>
                <br/>
                     
                
                
                <h:form id="resultsForm" rendered="#{SearchBean.showResults}">
                    <table class="itemTable">
                        <tr>
                            <th class="itemCell">
                                Map
                                        <br/>
                                        <img src="../images/check_all.gif" onclick="return checkAll()"/><img src="../images/uncheck_all.gif" onclick="return uncheckAll()"/>
                                    </th>
                                    <th class="itemCell">Name</th>
                                    <th class="itemCell">Description</th>
                                    <th class="itemCell">Tags</th>
                                    <th class="itemCell">Price</th>
                                </tr>
<%
SearchBean searchBean=(SearchBean)session.getAttribute("SearchBean");
if(searchBean != null) {
    List<IndexDocument> hits=searchBean.getHits();
    if(hits != null) {
        for(IndexDocument indexDoc : hits) {
%>                    
                                <tr>
                                <td class="itemCell">
                                    <input type="checkbox" name="mapSelectedItems" value="<%= indexDoc.getUID() %>"/>                        
                                </td>
                                <td class="itemCell">
                                    <a href="${pageContext.request.contextPath}/faces/catalog.jsp?pid=<%= indexDoc.getProduct() %>&itemId=<%= indexDoc.getUID() %>"
                                       onmouseover="bpui.popup.show('pop1', event, '<%= indexDoc.getUID() %>')" onmouseout="bpui.popup.hide('pop1')">
                                        <%= indexDoc.getTitle() %>
                                    </a>
                                </td>
                                <td class="itemCell">
                                    <%= indexDoc.getSummary() %>
                                </td>
                                <td class="itemCell">
                                <span id="ITEMID_TAGS_<%= indexDoc.getUID() %>"><%= (indexDoc.getTag().equals("") ? "&nbsp;" : indexDoc.getTag()) %></span>
                                <br/><input type="button" value="Add Tags" onclick="addTags(event, '<%= indexDoc.getTitle() %>', '<%= indexDoc.getUID() %>')"/>
                            </td>
                            <td class="itemCell">
                                <%= indexDoc.getPriceDisplay() %>
                            </td>
                        </tr>
<%
        }
    }                        
}
%>
                            <tr>
                                <td colspan="5">
                                <br/>
                                <center>
                                    <table class="itemTable">
                                        <tr>
                                            <th align="right">Center Point Address:</th>
                                            <td>
                                                <h:inputText id="centerAddress" value="#{MapBean.centerAddress}" size="50"/>
                                                <br/><small><i>For example: 4140 Network Circle, Santa Clara, CA, 95054</i></small>
                                            </td>
                                        </tr>
                                        <tr>
                                            <th align="right">Area (in Miles):</th>
                                            <td>
                                                <h:inputText id="radius" value="#{MapBean.radius}" size="5"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td align="center" colspan="2">
                                                <h:commandButton action="#{MapBean.findAllByIDs}" id="mapSubmit" type="submit" 
                                                value="Map Checked Item(s)" rendered="#{SearchBean.showResults}"/>
                                            </td>
                                        </tr>
                                    </table>
                                </center>
                                <br/>
                            </td>
                        </tr>
                    </table>
                    
                    <h:messages/>
                </h:form>
                <br/><br/><br/>
                <div class="tagDiv" id="addTags">
                    <form>
                    <table>
                            <tr>
                                <td align="center">
                                    <span id="addTagsTitle"><b>Tag Title</b></span><br/><i>(seperated by spaces)</i>
                                </td>
                            </tr>
                            <tr>
                                <td align="center">
                                    <input id="addTagsTags" type="text" size="50"/>
                                </td>
                            </tr>
                            <tr>
                                <td align="center">
                                    <input type="button" value="Save" onclick="saveAddTags()"/>&nbsp;&nbsp;<input type="button" value="Cancel" onclick="cancelAddTags()"/>
                                </td>
                            </tr>
                    </table>
                    <input type="hidden" id="addTagsItemId"/>
                    </form>
                </div>
            </f:view>
        </center>
        <jsp:include page="footer.jsp" />
    </body>
</html>
