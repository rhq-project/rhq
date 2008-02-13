<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:choose>
<c:when test="${empty Resource}">
<html:link page="/Dashboard.do"><fmt:message key="alert.current.detail.link.noresource.Rtn"/></html:link>
</c:when>
<c:otherwise>

<html:link page="/rhq/resource/alert/listAlertHistory.xhtml?mode=list&id=${Resource.id}"><fmt:message key="alert.current.detail.link.Rtn"/></html:link>
</c:otherwise>
</c:choose>
<br><br>
