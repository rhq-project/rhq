<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:set var="id" value="${param.id}"/>
<c:set var="ctype" value="${param.ctype}"/>
<c:set var="type" value="${param.type}"/>
<c:set var="parent" value="${param.parent}"/>

<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="HIGH_RANGE_KEY"
  var="high"/>
<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="LOW_RANGE_KEY"
  var="low"/>

<html>
<head>
<META Http-Equiv="Cache-Control" Content="no-cache">
<META Http-Equiv="Pragma" Content="no-cache">
<META Http-Equiv="Expires" Content="0">
<c:if test="${not empty refreshPeriod}">
   <meta http-equiv="refresh" content="<c:out value='${refreshPeriod}' />">
</c:if>

<script src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/prototype.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>

<script language="JavaScript">
  var baseUrl = "<html:rewrite page="/resource/common/monitor/visibility/IndicatorCharts.do"/>";

baseUrl += "?id=<c:out value="${id}"/>";
<c:if test="${not empty ctype}">
  baseUrl += "&ctype=<c:out value="${ctype}"/>";
</c:if>
<c:if test="${not empty groupId}">
  baseUrl += "&groupId=<c:out value="${groupId}"/>";
</c:if>
<c:if test="${not empty parent}">
  baseUrl += "&parent=<c:out value="${parent}"/>";
</c:if>

baseUrl +=  "&view=<c:out value="${view}"/>"

  // Register the remove metric chart method
  ajaxEngine.registerRequest( 'indicatorCharts', baseUrl );

  function removeMetric(token) {
    ajaxEngine.sendRequest(
        'indicatorCharts',
        'metric=' + token,
        'action=remove',
        'view=' + '<c:out value="${IndicatorViewsForm.view}"/>');
    new Effect.Fade(token);
  }

  function moveMetricUp(token) {
    ajaxEngine.sendRequest(
        'indicatorCharts',
        'metric=' + token,
        'action=moveUp',
        'view=' + '<c:out value="${IndicatorViewsForm.view}"/>');
    var root = $('root');
    var elem = $(token);
    moveElementUp(elem, root);
  }

  function moveMetricDown(token) {
    ajaxEngine.sendRequest(
        'indicatorCharts',
        'metric=' + token,
        'action=moveDown',
        'view=' + '<c:out value="${IndicatorViewsForm.view}"/>');
    var root = $('root');
    var elem = $(token);
    moveElementDown(elem, root);
  }

</script>

<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<!-- Overwrite default style -->
<style type="text/css">
   body {background: #DBE3F5 url(../../../../images/spacer.gif) no-repeat; }
</style>
</head>

<body>

<ul id="root" class="boxy">
<c:forEach var="metric" varStatus="status" items="${chartDataKeys}">


  <c:url var="chartLink" value="/resource/common/monitor/Visibility.do">
    <c:param name="m" value="${metric.definitionId}"/>
    <c:choose>
      <%-- autogroup case    --%>
      <c:when test="${ metric.childTypeId > 0}">
        <c:param name="mode" value="chartSingleMetricMultiResource"/>
        <c:param name="type" value="${metric.childTypeId}"/>
        <c:param name="parent" value="${metric.parentId}"/>
      </c:when>
      <%-- compatible group --%>
      <c:when test="${metric.groupId > 0}">
        <c:param name="mode" value="chartSingleMetricMultiResource"/>
        <c:param name="groupId" value="${metric.groupId}"/>
      </c:when>
      <%-- single resource  --%>      
      <c:otherwise>
        <c:param name="mode" value="chartSingleMetricSingleResource"/>
        <c:param name="id" value="${metric.resourceId}"/>
      </c:otherwise>
    </c:choose>
  </c:url>

  <c:url var="chartImg" value="/resource/HighLowChart">
    <c:param name="imageWidth" value="647"/>
    <c:param name="imageHeight" value="100"/>
    <c:param name="schedId" value="${metric.scheduleId}"/>
    <c:if test="${metric.groupId > 0 }">
      <c:param name="groupId" value="${metric.groupId}"/>
    </c:if>
    <c:param name="definitionId" value="${metric.definitionId}"/>
    <c:if test="${ metric.childTypeId > 0}">
      <c:param name="ctype" value="${metric.childTypeId}"/>
      <c:param name="id" value="${metric.parentId}"/>
    </c:if>
    <c:param name="now" value="${IndicatorViewsForm.timeToken}"/>
    <c:param name="measurementUnits" value="${metric.units}"/>
  </c:url>

  <li id="<c:out value="${metric.metricToken}"/>">
  <table width="650" border="0" cellpadding="2" bgcolor="#DBE3F5">
  <tr>
    <td>
      <table width="100%" border="0" cellpadding="0" cellspacing="1" style="margin-top: 1px; margin-bottom: 1px;">
        <tr>
          <td>
          <font class="BoldText">
            <a href="<c:out value="${chartLink}"/>" target="_top"><c:out value="${metric.label}"/></a>
          </font>
          <font class="FooterSmall">
             <fmt:message key="common.value.parenthesis">
               <fmt:param value="${metric.metricSource}"/>
             </fmt:message>
          </font></td>
          <td width="15%" nowrap="true"><font class="BoldText"><fmt:message key="resource.common.monitor.visibility.LowTH"/></font>: <c:out value="${metric.minMetric.valueFmt}"/></td>
          <td width="15%" nowrap="true"><font class="BoldText"><fmt:message key="resource.common.monitor.visibility.AvgTH"/></font>: <c:out value="${metric.avgMetric.valueFmt}"/></td>
          <td width="15%" nowrap="true"><font class="BoldText"><fmt:message key="resource.common.monitor.visibility.PeakTH"/></font>: <c:out value="${metric.maxMetric.valueFmt}"/></td>
          <!--  TODO don't use scheduleId for groups -->
          <td width="1%"><a href="javascript:moveMetricUp('<c:out value="${metric.metricToken}"/>')"><html:img page="/images/dash_icon_up.gif" border="0"/></a></td>
          <td width="1%"><a href="javascript:moveMetricDown('<c:out value="${metric.metricToken}"/>')"><html:img page="/images/dash_icon_down.gif" border="0"/></a></td>
          <td width="1%"><a href="javascript:removeMetric('<c:out value="${metric.metricToken}"/>')"><html:img page="/images/dash-icon_delete.gif" border="0"/></a></td>
        </tr>
        <tr>
           <td colspan="7">
              <c:out value="${metric.description}"/>
           </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<html:img src="${chartImg}" border="0"/>
</li>
</c:forEach>
</ul>

</body>

