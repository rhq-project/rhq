<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" src="<html:rewrite page="/js/popup.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

<c:set var="groupId" value="${groupId}"/>
<c:set var="view" value="${param.view}"/>
<c:set var="mode" value="${param.mode}"/>
<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<c:choose>
  <c:when test="${mode == 'currentHealth'}">
    <c:set var="isCurrentHealth" value="true"/>
  </c:when>
  <c:when test="${mode == 'resourceMetrics'}">
    <c:set var="isResourceMetrics" value="true"/>
  </c:when>
  <c:when test="${mode == 'performance'}">
    <c:set var="isPerformance" value="true"/>
  </c:when>
</c:choose>

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<%--<c:set var="entityId" value="${Resource.entityId}"/>--%>
<c:set var="selfAction" value="/resource/group/monitor/Visibility.do?mode=currentHealth&groupId=${groupId}&category=COMPATIBLE}"/>
<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<tiles:insert definition=".page.title.resource.group.full">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<c:choose>
    <c:when test="${ canControl }">
        <tiles:insert definition=".tabs.resource.group.monitor.visibility">
          <tiles:put name="groupId" value="${groupId}"/>
        </tiles:insert>
    </c:when>
    <c:otherwise>
        <tiles:insert definition=".tabs.resource.group.monitor.visibility.nocontrol">
          <tiles:put name="groupId" value="${groupId}"/>
        </tiles:insert>
    </c:otherwise>
</c:choose>            

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<table width="100%" class="MonitorBlockContainer">
  <tr>
    <td valign="top">
<c:choose>
  <c:when test="${isPerformance}">
    <tiles:insert page="/resource/group/monitor/visibility/GroupPerformance.jsp">
      <tiles:put name="entityId" beanName="entityId"/>
    </tiles:insert>
  </c:when>
  <c:otherwise>
    <c:choose>
      <c:when test="${isCurrentHealth}">
        <html:form
            action="/resource/common/monitor/visibility/SelectResources.do">
          <input type="hidden" name="groupId" value="<c:out value="${groupId}"/>">
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>">
          </c:if>
          <html:hidden property="mode"/>
          <tiles:insert page="/resource/group/monitor/visibility/CurrentHealthResources.jsp">
            <tiles:put name="mode" beanName="mode"/>
            <tiles:put name="showProblems" value="true"/>
          </tiles:insert>
        </html:form>
      </c:when>
      <c:when test="${isResourceMetrics}">
        <html:form action="/resource/group/monitor/visibility/FilterGroupMetrics">
          <input type="hidden" name="groupId" value="<c:out value="${groupId}"/>">
          <tiles:insert page="/resource/group/monitor/visibility/CurrentHealthResources.jsp">
            <tiles:put name="mode" beanName="mode"/>
          </tiles:insert>
        </html:form>
      </c:when>
    </c:choose>
    </td>
    <td valign="top" width="100%">
    <c:choose>
      <c:when test="${isCurrentHealth}">
        <tiles:insert page="/resource/common/monitor/visibility/Indicators.jsp">
          <c:if test="${perfSupported}">
            <tiles:put name="tabListName" value="perf"/>
          </c:if>
          <tiles:put name="entityType" value="group"/>
        </tiles:insert>
      </c:when>
      <c:when test="${isResourceMetrics}">
        <tiles:insert page="/resource/group/monitor/visibility/GroupMetrics.jsp">
          <tiles:put name="groupId" beanName="groupId"/>
        </tiles:insert>
      </c:when>
      <c:otherwise>
        <c:out value="${mode}"/>
      </c:otherwise>
    </c:choose>
    </td>
  </tr>
  <tr>
    <td colspan="2">
      <html:form method="GET" action="/resource/common/monitor/visibility/MetricsControl">
        <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
          <tiles:put name="form" beanName="MetricsControlForm"/>
          <tiles:put name="formName" value="MetricsControlForm"/>
          <tiles:put name="mode" beanName="mode"/>
          <tiles:put name="groupId" beanName="groupId"/>
          <c:if test="${not empty view}">
            <tiles:put name="view" beanName="view"/>
          </c:if>
       </tiles:insert>
     </html:form>
  </c:otherwise>
</c:choose>
    </td>
  </tr>
</table>

<tiles:insert definition=".page.footer"/>
