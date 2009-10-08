<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant var="COMPAT_GROUP"
             classname="org.rhq.core.domain.resource.group.GroupCategory"
             symbol="COMPATIBLE"/>
<hq:constant var="MIXED_GROUP"
             classname="org.rhq.core.domain.resource.group.GroupCategory"
             symbol="MIXED"/>

<c:set var="link" value="/GroupHub.do"/>
<%--
<html:link href=""
           onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;">
   <html:img styleClass="pagehelp" page="/images/title_pagehelp.gif" border="0" align="right"/>
</html:link> --%>
<html:link page="${link}">
   <fmt:message key="resource.hub.ResourceHubPageTitle"/>
</html:link>
&gt;
<c:choose>
   <c:when test="${GroupHubForm.groupCategory == COMPAT_GROUP}">
      <html:link page="${link}?groupCategory=${COMPAT_GROUP}">
         <fmt:message key="resource.hub.filter.compatibleGroups"/>
      </html:link>
   </c:when>
   <c:when test="${GroupHubForm.groupCategory == MIXED_GROUP}">
      <html:link page="${link}?groupCategory=${MIXED_GROUP}">
         <fmt:message key="resource.hub.filter.mixedGroups"/>
      </html:link>
   </c:when>
</c:choose>
