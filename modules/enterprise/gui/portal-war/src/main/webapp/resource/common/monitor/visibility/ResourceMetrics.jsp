<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="id"/>

<c:set var="selfAction" value="/resource/common/monitor/Visibility.do?mode=resourceMetrics&id=${Resource.id}"/>

<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="1"/>
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="Resource" beanProperty="resourceType"/>
  <tiles:put name="autogroupResourceType" value="${param.type}"/>
  <tiles:put name="entityType" value="server"/>
  <c:if test="${perfSupported}">
    <tiles:put name="tabListName" value="perf"/>
  </c:if>
</tiles:insert>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
<html:form action="/resource/common/monitor/visibility/Metrics">
<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:put name="summaries" beanName="MetricSummaries"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
  <tiles:put name="buttonMode" value="baselines"/>
  <tiles:put name="useCurrent" value="true"/>
  <tiles:put name="id" beanName="Resource" beanProperty="id"/>
</tiles:insert>
<html:hidden property="h"/>
<html:hidden property="id"/>
<html:hidden property="category"/>
</html:form>
    </td>
  </tr>
</table>
