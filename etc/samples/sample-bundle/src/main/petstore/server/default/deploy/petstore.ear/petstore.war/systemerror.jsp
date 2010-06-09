<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: systemerror.jsp,v 1.3 2007/01/09 19:02:12 basler Exp $ --%>
<%@ page isErrorPage="true" %>
<%@ page import="com.sun.javaee.blueprints.petstore.util.PetstoreUtil, java.util.logging.Level" %>
<%
PetstoreUtil.getLogger().log(Level.INFO, "Encountered a Runtime Exception and being transferred the the systemerror page", exception);
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
        <title>Java Pet Store Reference Application: System Error Page</title>
    </head>
    <body>
        
        <jsp:include page="banner.jsp" />
        
        <h2>System Error !</h2>
        <p>We had problems processing your request. An exception has been caught, 
        so perhaps your application was not set up or deployed properly.</p>

        The Exception that was thrown is:<b> <%= exception.toString() %></b>.  The server log will contain the stack trace of the exception.
        
        <p><a href="${pageContext.request.contextPath}/faces/index.jsp">Go back to sample application home</a></p>
        <br/><br/>
        <jsp:include page="footer.jsp" />
        
    </body>
</html>
