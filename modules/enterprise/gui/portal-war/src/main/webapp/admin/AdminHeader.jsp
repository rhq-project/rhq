<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img styleClass="pagehelp" page="/images/title_pagehelp.gif" border="0" align="right"/></html:link>
<html:link page="/Admin.do"><fmt:message key="admin.admin.AdministrationTitle"/></html:link>
<tiles:importAttribute name="location" ignore="true"/>
<c:if test="${not empty location}">
&gt;
  <c:choose>
    <c:when test="${location eq 'header.users'}">
      <c:url var="parent" value="/admin/user/UserAdmin.do">
        <c:param name="mode" value="list"/>
      </c:url>
    </c:when>
    <c:when test="${location eq 'header.roles'}">
      <c:url var="parent" value="/admin/role/RoleAdmin.do">
        <c:param name="mode" value="list"/>
      </c:url>
    </c:when>
    <c:when test="${location eq 'admin.home.ServerConfig'}">
      <c:url var="parent" value="/admin/config/Config.do">
        <c:param name="mode" value="edit"/>
      </c:url>
    </c:when>
    <c:when test="${location eq 'admin.home.ResourceTemplates'}">
      <c:url var="parent" value="/admin/config/EditDefaults.do">
        <c:param name="mode" value="monitor"/>
      </c:url>
    </c:when>
    <c:when test="${location eq 'admin.home.ResourceAlerts'}">
      <html:link page="/admin/config/EditDefaults.do?mode=monitor">
      <fmt:message key="admin.home.ResourceTemplates"/>
      </html:link>
      &gt;
    </c:when>
  </c:choose>

  <a href="<c:out value="${parent}"/>"><fmt:message key="${location}"/></a>
  <c:if test="${not empty param.mode && param.mode ne 'list'}">
    &gt;
    <a href="javascript: window.location.reload()"><fmt:message key="admin.mode.${param.mode}"/></a>
  </c:if>
</c:if>
