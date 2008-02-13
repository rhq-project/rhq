<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:insert definition=".events.config.view.conditions">
  <tiles:put name="showValues" value="false"/>
</tiles:insert>

<c:if test="${not canEditConditions}">
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ErrorField">
  <tr>
    <td><fmt:message key="alert.config.props.CB.CanNotEditConditions"/></td>
  </tr>
</table>
</c:if>
<c:if test="${canEditConditions}">
  <c:if test="${not empty Resource || not empty ResourceType}">
  <tiles:insert definition=".toolbar.edit">
  <c:choose>
    <c:when test="${not empty Resource}">
    <tiles:put name="editUrl">/alerts/Config.do?mode=editConditions&id=<c:out value="${Resource.id}"/>&ad=<c:out value="${alertDef.id}"/></tiles:put>
    </c:when>
    <c:otherwise>
    <tiles:put name="editUrl">/alerts/Config.do?mode=editConditions&type=<c:out value="${ResourceType.id}"/>&ad=<c:out value="${alertDef.id}"/></tiles:put>
    </c:otherwise>
  </c:choose>
  </tiles:insert>
</c:if>
</c:if>
<br>
