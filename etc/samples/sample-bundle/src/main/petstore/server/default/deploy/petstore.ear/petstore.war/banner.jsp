<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: banner.jsp,v 1.28 2006/12/19 20:23:53 yutayoshida Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/styles.css"></link>
<script type="text/javascript" src="${pageContext.request.contextPath}/faces/static/META-INF/dojo/bpcatalog/dojo.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/faces/static/META-INF/rss/rssbar.js"></script>
<link type="text/css" rel="stylesheet" href="${pageContext.request.contextPath}/faces/static/META-INF/rss/rssbar.css" />
<style type="text/css">

#rss-bar {
    margin: 0 auto 0px;
}

#rss-bar table td#rss-channel {
    background-repeat: no-repeat;
    background-position: top left;
    font-size: 14px;
    font-weight: bold;
    vertical-align: top;
    text-align: center;
    width: 254px;
}

#rss-bar table td#rss-item {
    background-repeat: no-repeat;
    font-size: 14px;
    width: 534px;
    text-align: left;
}

#rss-bar table a {
    color: white;
    text-decoration: none;
}
#rss-bar table a:hover { color: #ffff00;}

</style>
<script type="text/javascript">
    var rss = new bpui.RSS();
    dojo.addOnLoad(function(){rss.getRssInJson('${pageContext.request.contextPath}/faces/dynamic/bpui_rssfeedhandler/getRssfeed', 'https://blueprints.dev.java.net/servlets/ProjectRSS?type=news', '4', '4000', 'News from BluePrints', 'news.jsp');});
</script>

<table border="0" bordercolor="gray" cellpadding="0" cellspacing="0" bgcolor="white" width="100%">
 <tr id="injectionPoint">
  <td width="100"><a class="menuLink" href="${pageContext.request.contextPath}/faces/index.jsp""><img src="${pageContext.request.contextPath}/images/banner_logo.gif" border="0" width="70" height="70"></a></td>
  <td align="left">
   <div class="banner">Java Pet Store</div>
  </td>
  <td id="bannerRight" align="right">
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/fileupload.jsp">Seller</a> <span class="menuItem">|</span>
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/search.jsp">Search</a> <span class="menuItem">|</span>
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/catalog.jsp">Catalog</a> <span class="menuItem">|</span>
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/mapAll.jsp">Map</a> <span class="menuItem">|</span>
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/tag.jsp">Tags</a> <span class="menuItem">|</span>
    <a class="menuLink" onmouseover="this.className='menuLinkHover';" onmouseout="this.className='menuLink';" href="${pageContext.request.contextPath}/faces/index.jsp">Home</a>
  </td>
  </tr>
 </tr>
  <tr bgcolor="gray">
  <td id="menubar" align="left" colspan="3" height="25" >
    <div id="rss-bar">
    <table border="0" cellpadding="0" cellspacing="0">
        <tr>
        <td id="rss-channel" nowrap="true"></td>
        <td id="rss-item" nowrap="true"></td>
        </tr>
    </table>
    </div>
  </td>
 </tr>
 </table>


