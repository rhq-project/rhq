<%-- Copyright 2006 Sun Microsystems, Inc.
All rights reserved. You may not modify, use, reproduce, or distribute
this software except in compliance with the terms of the License at:
http://developer.sun.com/berkeley_license.html
$Id: news.jsp,v 1.2 2006/12/14 01:04:50 yutayoshida Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>BluePrints News Page</title>
        <script type="text/javascript" src="${pageContext.request.contextPath}/faces/static/META-INF/dojo/bpcatalog/dojo.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/news.js"></script>

        <style>
            p {
                width : 70%;
                background-color : #FFEFD5;
                font-size : 80%
                }
        </style>

    </head>
    <script type="text/javascript">
        var news = new bpuinews.RSS();
        dojo.addOnLoad(function(){news.getRssInJson('${pageContext.request.contextPath}/faces/dynamic/bpui_rssfeedhandler/getRssfeed', 'https://blueprints.dev.java.net/servlets/ProjectRSS?type=news');});
    </script>
    <body>
        <jsp:include page="banner.jsp" />
        <h2><a href="http://blueprints.dev.java.net">BluePrints News</a></h2>
        <center>
            <table border="0" width="95%">
                <tr>
                    <td>
                        <button id="previous" type="button">&lt;&lt; Previous</button>
                    </td>
                    <td>
                        <button id="next" type="button">Next &gt&gt</button>
                    </td>
                </tr>
            </table>
        </center>
        <div id="news"></div>
        <jsp:include page="footer.jsp" />
    </body>
</html>
