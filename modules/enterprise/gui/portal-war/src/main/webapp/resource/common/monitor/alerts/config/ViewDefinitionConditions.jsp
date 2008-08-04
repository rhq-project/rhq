<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="showValues" ignore="true"/>

<!-- Content Block Title: Condition -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.CondBox"/>
</tiles:insert>

<!-- Condition Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <c:forEach var="cond" items="${alertDefConditions}">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <c:if test="${! cond.first}">
      <c:choose>
      <c:when test="${conditionExpression == 'ALL'}">
      <fmt:message key="alert.config.props.CB.And"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="alert.config.props.CB.Or"/>
      </c:otherwise>
      </c:choose>
      </c:if>
      <fmt:message key="alert.config.props.CB.IfCondition"/>
    </td>
    <td width="80%" class="BlockContent">
      <c:out value="${cond.conditionText}"/>
    </td>
  </tr>
  <c:if test="${showValues}">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <fmt:message key="alert.config.props.CB.ActualValue"/>
    </td>
    <td width="80%" class="BlockContent">
      <c:choose>
      <c:when test="${not empty cond.actualValue}">
      <c:out value="${cond.actualValue}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="alert.config.props.CB.NoActualValue"/>
      </c:otherwise>
      </c:choose>
    </td>
  </tr>
  </c:if>
  </c:forEach>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.CB.DampeningRule"/></td>
    <td width="80%" class="BlockContent">
      <c:set var="howLongUnits"><fmt:message key="${'alert.config.props.CB.Enable.TimeUnit.'}${enableActionsHowLongUnits}"/></c:set>
      <c:set var="howManyUnits"><fmt:message key="${'alert.config.props.CB.Enable.TimeUnit.'}${enableActionsHowManyUnits}"/></c:set>
      <fmt:message key="${enableActionsResource}">
        <fmt:param value="${enableActionsHowLong}"/>
        <fmt:param value="${howLongUnits}"/>
        <fmt:param value="${enableActionsHowMany}"/>
        <fmt:param value="${howManyUnits}"/>
      </fmt:message>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <!-- Enablement option section -->
 
  <c:choose>
  <c:when test="${alertDef.recoveryId > 0}">
  <tr valign="top">
    <td class="BlockLabel">
      <fmt:message key="alert.config.props.CB.Recovery"/>
    </td>
    <td width="80%" class="BlockContent">
   	  <fmt:message key="alert.config.props.CB.RecoveryFor"/>
   	  <html:link page="/alerts/Config.do?mode=viewRoles&id=${Resource.id}&ad=${alertDef.recoveryId}">
   	  	<c:out value="${recoveryAlertName}" />
   	  </html:link>
  	</td>
  </tr>
  </c:when>
  </c:choose>
  
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockLabel" valign="top">
      <b>
        <fmt:message key="alert.config.props.CB.Content.ActionFilters"/>
      </b>
    </td>
    <td class="BlockContent">
      <fmt:message key="alert.config.props.CB.Content.UntilRecovered"/>
      <b>
        : <c:out value="${alertDef.willRecover}" />
      </b>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
