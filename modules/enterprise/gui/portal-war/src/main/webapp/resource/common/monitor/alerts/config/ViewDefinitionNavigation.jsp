<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:choose>

  <c:when test="${not empty Resource}">
    <html:link page="/rhq/resource/alert/listAlertDefinitions.xhtml?id=${Resource.id}"><fmt:message key="alert.config.props.ReturnLink"/></html:link>
  </c:when>
  
  <c:when test="${not empty ResourceType}">
    <html:link page="/rhq/admin/listAlertTemplates.xhtml?type=${ResourceType.id}"><fmt:message key="alert.config.props.ReturnTemplateLink"/></html:link>
  </c:when>
  
</c:choose>
<br><br>
