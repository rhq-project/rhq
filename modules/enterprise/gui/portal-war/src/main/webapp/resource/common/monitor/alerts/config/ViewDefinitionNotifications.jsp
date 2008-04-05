<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:url var="viewRolesUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewRoles"/>
  <c:choose>
  <c:when test="${not empty Resource}">
    <c:param name="id" value="${Resource.id}"/>
  </c:when>
  <c:otherwise>
    <c:param name="type" value="${ResourceType.id}"/>
  </c:otherwise>
  </c:choose>
  <c:param name="ad" value="${alertDef.id}"/>
</c:url>
<c:url var="viewUsersUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewUsers"/>
  <c:choose>
  <c:when test="${not empty Resource}">
    <c:param name="id" value="${Resource.id}"/>
  </c:when>
  <c:otherwise>
    <c:param name="type" value="${ResourceType.id}"/>
  </c:otherwise>
  </c:choose>
  <c:param name="ad" value="${alertDef.id}"/>
</c:url>
<c:url var="viewOthersUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewOthers"/>
  <c:choose>
  <c:when test="${not empty Resource}">
    <c:param name="id" value="${Resource.id}"/>
  </c:when>
  <c:otherwise>
    <c:param name="type" value="${ResourceType.id}"/>
  </c:otherwise>
  </c:choose>
  <c:param name="ad" value="${alertDef.id}"/>
</c:url>
<c:url var="viewSnmpUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewSnmp"/>
  <c:choose>
  <c:when test="${not empty Resource}">
    <c:param name="id" value="${Resource.id}"/>
  </c:when>
  <c:otherwise>
    <c:param name="type" value="${ResourceType.id}"/>
  </c:otherwise>
  </c:choose>
  <c:param name="ad" value="${alertDef.id}"/>
</c:url>


<tiles:insert definition=".events.config.view.notifications.tabs">
  <tiles:put name="viewRolesUrl" beanName="viewRolesUrl"/>
  <tiles:put name="viewUsersUrl" beanName="viewUsersUrl"/>
  <tiles:put name="viewOthersUrl" beanName="viewOthersUrl"/>
  <tiles:put name="viewSnmpUrl" beanName="viewSnmpUrl"/>
</tiles:insert>

<%--
  I don't particularly *WANT* to use JSP-RT stuff here, but there
  seems to be no better choice since the struts tiles:insert does
  not yet take EL.
--%>
<% String notificationsTile = null; %>
<c:choose>
<c:when test="${param.mode == 'viewRoles'}">
<% notificationsTile = ".events.config.view.notifications.roles"; %>
<c:set var="formAction" value="/alerts/RemoveRoles"/>
<c:set var="selfUrl" value="${viewRolesUrl}"/>
<c:set var="addMode" value="addRoles"/>
<c:set var="defaultSortColumn" value="0"/>
</c:when>
<c:when test="${param.mode == 'viewUsers'}">
<% notificationsTile = ".events.config.view.notifications.users"; %>
<c:set var="formAction" value="/alerts/RemoveUsers"/>
<c:set var="selfUrl" value="${viewUsersUrl}"/>
<c:set var="addMode" value="addUsers"/>
<c:set var="defaultSortColumn" value="2"/>
</c:when>
<c:when test="${param.mode == 'viewOthers'}">
<% notificationsTile = ".events.config.view.notifications.others"; %>
<c:set var="formAction" value="/alerts/RemoveOthers"/>
<c:set var="selfUrl" value="${viewOthersUrl}"/>
<c:set var="addMode" value="addOthers"/>
<c:set var="defaultSortColumn" value="0"/>
</c:when>
<c:when test="${param.mode == 'viewSnmp'}">
<% notificationsTile = ".events.config.view.notifications.snmp"; %>
<c:set var="formAction" value="/alerts/SetSnmpProps"/>
<c:set var="selfUrl" value="${viewSnmpUrl}"/>
<c:set var="defaultSortColumn" value="0"/>
</c:when>
<c:otherwise>
<%-- do nothing --%>
</c:otherwise>
</c:choose>
<script language="JavaScript" src="<html:rewrite page='/js/listWidget.js'/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="list"/>
<script language="JavaScript" type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
<!-- FORM -->
<html:form action="${formAction}">
<html:hidden property="ad" value="${alertDef.id}"/>
<c:choose>
<c:when test="${not empty Resource}">
  <html:hidden property="id" value="${Resource.id}"/>
</c:when>
<c:otherwise>
  <html:hidden property="type" value="${ResourceType.id}"/>
</c:otherwise>
</c:choose>

<%-- I have to use an RT-expr here.  Yuck.  See comment above. --%>
<tiles:insert definition="<%=notificationsTile%>">
  <tiles:put name="selfUrl" beanName="selfUrl"/>
</tiles:insert>

<%-- if the attributes are not available, we can't display this tile: an error probably occurred --%>
<c:if test="${param.mode != 'viewSnmp'}">
  <c:choose>
    <c:when test="${null == notifyList || empty listSize}">
      <!-- permission error occured -->
      <fmt:message key="alert.config.error.no.permission"/>
    </c:when>
    <c:otherwise>
      <c:if test="${not empty Resource}">
        <hq:authorization permission="MANAGE_ALERTS">
          <tiles:insert definition=".toolbar.addToList">
            <tiles:put name="addToListUrl"><c:out value="/alerts/Config.do?mode=${addMode}&id=${Resource.id}&ad=${alertDef.id}"/></tiles:put>
            <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
            <tiles:put name="pageList" beanName="notifyList"/>
            <tiles:put name="pageAction" beanName="selfUrl"/>
          </tiles:insert>
        </hq:authorization>
      </c:if>
      <c:if test="${not empty ResourceType}">
        <hq:authorization permission="MANAGE_SETTINGS">
          <tiles:insert definition=".toolbar.addToList">
            <tiles:put name="addToListUrl"><c:out value="/alerts/Config.do?mode=${addMode}&type=${ResourceType.id}&ad=${alertDef.id}"/></tiles:put>
            <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
            <tiles:put name="pageList" beanName="notifyList"/>
            <tiles:put name="pageAction" beanName="selfUrl"/>
          </tiles:insert>
        </hq:authorization>
      </c:if>
    </c:otherwise>
  </c:choose>
</c:if>

</html:form>

<!-- / FORM -->
<br>
