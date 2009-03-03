<%@ page contentType="text/html" %>

<%@page import="org.rhq.enterprise.installer.ServerInformation"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:loadBundle var="bundle" basename="InstallerMessages" />

   <head>
      <link rel="stylesheet" type="text/css" href="style.css" />
      <title><h:outputText value="#{bundle.welcomeTitle}" /></title>

      <meta http-equiv="Pragma"        content="no-cache" />
      <meta http-equiv="Expires"       content="-1" />
      <meta http-equiv="Cache-control" content="no-cache" />

      <%-- we only do our AJAX forwarding to the login page if fully deployed but not on the installer start page --%>
      <c:if test="<%= new ServerInformation().isFullyDeployed() && request.getRequestURI().indexOf("installer/start") == -1 %>">
      <script type="text/javascript" language="JavaScript">

         var startPage = '/Start.do';
         var xmlRequest = false;

         function doLoad()
         {
            setTimeout( "refresh()", 5*1000 )

            try
            {  
               if (window.XMLHttpRequest)
               { 
                  xmlRequest = new XMLHttpRequest(); // Firefox, Safari, ...
               } 
               else if (window.ActiveXObject)
               {
                  xmlRequest = new ActiveXObject("Microsoft.XMLHTTP"); // Internet Explorer 
               }
            }
            catch (e)
            {
               xmlRequest = false;
            }

            if ( !xmlRequest )
            {
               return false;
            }

            xmlRequest.onreadystatechange = processStateChange;
            xmlRequest.open('GET', startPage, true);
            xmlRequest.send(null);
         }

         function processStateChange()
         {
            if (xmlRequest.readyState == 4)
            {
               // 200 means page is OK, 401 means we are being asked to authenticate the user
               // in either case, it means the RHQ Console is ready
               if (xmlRequest.status == 200 || xmlRequest.status == 401)
               {
                  linkText = "${bundle.alreadyInstalledStartedLink}";
                  document.getElementById('pleasewait-image').src="/images/finished.gif";
                  document.getElementById('progressBarMessage').innerHTML='<a class="small" href="/">' + linkText + '<\/a>';
                  // this will immediately forward to the start page, uncomment if we want that functionality
                  // window.location.replace( startPage );
               }
            }
         }

         function refresh()
         {
            doLoad();
         }
      </script>
      </c:if>
      
      <script type="text/javascript" language="JavaScript">
         function popUp(url, title)
         {
            window.open(url, title, 'toolbar=0,scrollbars=1,location=1,statusbar=0,menubar=0,resizable=1');
         }
      </script>

   </head>

   <c:choose>
      <c:when test="<%= new ServerInformation().isFullyDeployed() && request.getRequestURI().indexOf("installer/start") == -1 %>">
         <body onload="doLoad()">
      </c:when>
      <c:otherwise>
         <body>
      </c:otherwise>
   </c:choose>

   <p align="center">
      <h:graphicImage url="/images/logo.png" alt="RHQ logo"/>
   </p>
   
   <h1 align="left"><h:outputText value="#{bundle.welcomeTitle}" /></h1>
