<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant var="PLATFORM"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="PLATFORM"/>
<hq:constant var="SERVER"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVER"/>
<hq:constant var="SERVICE"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVICE"/>

<c:set var="link" value="/ResourceHub.do"/>

<html:link href=""
           onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;">
   <%--<html:img styleClass="pagehelp" page="/images/title_pagehelp.gif" border="0" align="right"/>--%>
</html:link>
<html:link page="${link}">
   <fmt:message key="resource.hub.ResourceHubPageTitle"/>
</html:link>
&gt;
<c:choose>
   <c:when test="${ResourceHubForm.resourceCategory == PLATFORM}">
      <html:link page="${link}?resourceCategory=${PLATFORM}">
         <fmt:message key="resource.hub.filter.platform"/>
      </html:link>
   </c:when>
   <c:when test="${ResourceHubForm.resourceCategory == SERVER}">
      <html:link page="${link}?resourceCategory=${SERVER}">
         <fmt:message key="resource.hub.filter.server"/>
      </html:link>
   </c:when>
   <c:when test="${ResourceHubForm.resourceCategory == SERVICE}">
      <html:link page="${link}?resourceCategory=${SERVICE}">
         <fmt:message key="resource.hub.filter.service"/>
      </html:link>
   </c:when>
</c:choose>
