<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
    response.setHeader("Pragma","no-cache"); //HTTP 1.0
    response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>

<c:set var="showUpAndDown" value="true" scope="request"/>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr><td valign="top" class="DashboardPadding">
        <tiles:insert definition="<%= request.getParameter("portlet")%>"/>
    </td></tr>
</table>
