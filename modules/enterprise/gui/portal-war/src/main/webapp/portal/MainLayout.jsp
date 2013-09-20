<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts-menu.sf.net/tag" prefix="menu" %>
<%@ taglib uri="http://richfaces.org/a4j" prefix="a4j" %>
<html:html>
   <head>
      <meta http-equiv="Expires" content="-1">
      <meta http-equiv="Pragma" content="no-cache">
      <meta http-equiv="Max-Age" content="0">
      <meta http-equiv="Cache-Control" content="no-cache">

      <link rel="stylesheet" type="text/css" media="screen" href="/portal/css/win.css"/>
      <link rel="stylesheet" type="text/css" media="screen" href="/portal/css/layout.css"/>

      <script type="text/javascript" src="/portal/js/functions.js"></script>
      <script type="text/javascript" src="/portal/js/prototype.js"></script>
      <script type="text/javascript" src="/portal/js/window.js"></script>
      <script type="text/javascript" src="/portal/js/effects.js"></script>
      <!--<script type="text/javascript" src="/portal/js/debug.js"> </script>-->
      <script type="text/javascript" src="/portal/js/rhq.js"></script>

      <link href="/portal/css/theme/default.css" rel="stylesheet" type="text/css"/>
      <link href="/portal/css/theme/alphacube.css" rel="stylesheet" type="text/css"/>
      <link href="/portal/css/theme/debug.css" rel="stylesheet" type="text/css"/>
      <link rel="stylesheet" type="text/css" media="screen" href="/portal/css/menu.css"/>

      <link rel="icon" type="image/png" href="/portal/images/favicon.png"/>
      <link rel="apple-touch-icon" href="/portal/images/favicon.png"/>

      <tiles:insert attribute="head"/>
      <title>
         <fmt:message key="${portal.name}">
            <c:if test="${not empty TitleParam}">
               <fmt:param value="${TitleParam}"/>
            </c:if>
            <c:if test="${not empty TitleParam2}">
               <fmt:param value="${TitleParam2}"/>
            </c:if>
         </fmt:message>
      </title>

      <script type="text/javascript" src="/portal/js/ajax.js"></script>

      <!--
      <script type="text/javascript">
         var onloads = new Array();
         function bodyOnLoad()
         {
            for (var i = 0; i < onloads.length; i++)
               onloads[i]();
         }
      </script>
      -->
   </head>
   <body>

      <!--spinder 4/20/11: commenting for better iframing for coregui. Portal.war full view with menus no longer maintained. -->
      <!--<tiles:insert attribute="header">
         <tiles:put name="breadcrumb" beanName="breadcrumb"/>
         <tiles:put name="location" beanName="location"/>
      </tiles:insert>-->

      <div id="content-full">
	     <!--<div id="Breadcrumb">
	        <tiles:insert attribute="breadcrumb">
	           <tiles:put name="location" beanName="location"/>
	        </tiles:insert>
	     </div>-->
	     <tiles:insert attribute='body'/>
      </div>
   </body>
</html:html>
