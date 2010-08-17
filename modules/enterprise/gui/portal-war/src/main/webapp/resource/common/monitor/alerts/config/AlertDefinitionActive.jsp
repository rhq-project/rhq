<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="alertDef"/>

<c:choose>
  <c:when test="${alertDef.enabled}">
    <td width="30%" class="BlockContent">
      <html:img page="/images/icon_available_green.gif" width="12" height="12" border="0"/>
      <fmt:message key="alert.config.props.PB.ActiveYes"/>
    </td>
  </c:when>
    <c:otherwise>
    <td width="30%" class="BlockContent">
    <html:img page="/images/icon_available_red.gif" width="12" height="12" border="0"/>
    <fmt:message key="alert.config.props.PB.ActiveNo"/>
    &nbsp;
    <c:if test="${not alertDef.deleted && not empty alert.id}">
        <a href="/alerts/RenableAlertDefinition.do?id=${Resource.id}&a=${alert.id}&mode=${param.mode}&ad=${alertDef.id}">click to re-enable</a>
    </c:if>
    </td>
  </c:otherwise>
</c:choose>
