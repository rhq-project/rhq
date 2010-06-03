<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: mapAll.jsp,v 1.14 2006/12/06 22:44:37 basler Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Map Viewer Page</title>
    </head>        
    <body>
        <jsp:include page="banner.jsp" />
        <center>
        <f:view>

            <h:form id="form1">
                <table border="1" cellpadding="5" cellspacing="5" style="border-style:double; width:600px; border-color:darkgreen; padding:5px">
                    <tr>
                        <td colspan=3>
                        <i>Select the category whose items will be displayed in a Google map based on the Seller's address.  
                        Optionally, enter a "Center Point Address" and "Area" to limit the search to a specific 
                        area around the center point.</i>
                        </td>
                    </tr>
                    <tr>
                        <th align="right">Select Category to Map:</th>
                        <td align="center" colspan=2>
                            <h:selectOneRadio value="#{MapBean.category}" required="true">
                                <f:selectItems value="#{MapBean.categories}"/>
                            </h:selectOneRadio>
                        </td>
                    </tr>
                    <tr>
                        <th align="right">Center Point Address:</th>
                        <td>
                            <h:inputText id="centerAddress" value="#{MapBean.centerAddress}" size="70"/>
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
                        <td  colspan="2" align="center">
                            <h:commandButton action="#{MapBean.findAllByCategory}" id="submitCat" type="submit" value="Map Category"/>
                        </td>
                    </tr>

                </table>
                <h:messages/>
            </h:form>
        </f:view>
        &nbsp;&nbsp;&nbsp;  
        </center>
        <jsp:include page="footer.jsp" />
    </body>
</html>
