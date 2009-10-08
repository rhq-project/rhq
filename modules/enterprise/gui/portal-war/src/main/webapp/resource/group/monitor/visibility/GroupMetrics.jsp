<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="selfAction" value="/resource/group/monitor/Visibility.do?mode=resourceMetrics&groupId=${groupId}&category=COMPATIBLE"/>

<!-- TODO memberTypeLabel is passed to include the group name as {0} parameter of the message and within the
actual health data in .header.tab and childResourcesCurrentHealthByType tiles below -->
<c:set var="memberTypeLabel" value="${group.name}" />

<!-- indicator and metric data tabs-->
<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="1"/>
</tiles:insert>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
<html:form action="/resource/group/monitor/visibility/GroupMetrics">

<!-- metric data tile, using MetricDisplay.jsp in resource/common/monitor/visibility-->
<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:put name="summaries" beanName="MetricSummaries"/>
  <tiles:put name="buttonMode" value="noleft"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
  <tiles:put name="useChartMulti" value="false"/>
  <tiles:put name="useCurrent" value="true"/>
</tiles:insert>

<!-- Current Health Header message tile-->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.monitor.visibility.CurrentHealthOfCollecting"/>
  <tiles:put name="tabName" beanName="memberTypeLabel" />
</tiles:insert>

<!-- Current Health data of resources within compatible group tile -->   
<tiles:insert definition=".resource.common.monitor.visibility.childResourcesCurrentHealthByType">
  <tiles:put name="summaries" beanName="GroupMemberHealthSummaries"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>

<html:hidden property="h"/>
<html:hidden property="id" value="${groupId}"/>
<html:hidden property="groupId" value="${groupId}"/>
<html:hidden property="category"/>
</html:form>
    </td>
  </tr>
</table>
