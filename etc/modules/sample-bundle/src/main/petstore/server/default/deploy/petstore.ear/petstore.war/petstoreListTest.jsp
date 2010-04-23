<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>jsonp test page</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/bp_petstorelist.css"></link>
        <script type="text/javascript" src="${pageContext.request.contextPath}/bp_petstorelist.js"></script>
        <script type="text/javascript">
            var petstoreList;
            function init() {
                petstoreList=new bpui.petstoreList.createPetstoreList("petstoreListDiv");
            }
        </script>
    </head>
    <body onload="init()">
        <h1>jsonp test page</h1>
        
        <div id="petstoreListDiv"></div>
    </body>
</html>
