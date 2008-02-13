<%-- start vit: delete this block --%>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<%-- end vit: delete this block --%>
<script type="text/javascript">
  var imagePath = "<html:rewrite page="/images/"/>";
</script>
<script type="text/javascript">
var pageData = new Array();
</script>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="compareMetrics"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
<hq:constant 
  classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
  symbol="COLL_TYPE_DYNAMIC"
  var="dynamic"/>
<hq:constant 
  classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
  symbol="COLL_TYPE_STATIC"
  var="static"/>
<hq:constant 
  classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
  symbol="COLL_TYPE_TRENDSUP"
  var="trendsup"/>
<hq:constant 
  classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
  symbol="COLL_TYPE_TRENDSDOWN"
  var="trendsdown"/>

<hq:constant 
    classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
    symbol="CAT_AVAILABILITY" var="availability" />
<hq:constant 
    classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
    symbol="CAT_PERFORMANCE" var="performance" />
<hq:constant 
    classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
    symbol="CAT_THROUGHPUT" var="throughput" />
<hq:constant 
    classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
    symbol="CAT_UTILIZATION" var="utilization" />
<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="MAX_KEY"
  var="max"/>
<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="MIN_KEY"
  var="min"/>
<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="AVERAGE_KEY"
  var="average"/>
<hq:constant 
  classname="org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants"
  symbol="LAST_KEY"
  var="last"/>
<!--  COMPARE METRICS TITLE Need to bring this back once we get the groupname in the params-->
<%--<c:set var="titleName" value="${CompareMetricsForm.name}" />--%>
<tiles:insert definition=".page.title.resource.generic">
  <tiles:put name="titleKey" value="resource.common.monitor.visibility.CompareMetricsTitle"/>
  <tiles:put name="titleName" beanName="titleName" />
</tiles:insert>
<html:form action="/resource/common/monitor/visibility/CompareMetrics">

<html:link href="javascript:document.CompareMetricsForm.submit()" onclick="clickLink('CompareMetricsForm', 'back')"><fmt:message 
key="resource.common.monitor.visibility.CompareMetricsReturnLink">
<fmt:param value="${TitleParam}"/></fmt:message></html:link>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.monitor.visibility.CompareMetricsTab"/>
</tiles:insert>
<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
  <tiles:put name="form" beanName="CompareMetricsForm"/>
  <tiles:put name="formName" value="CompareMetricsForm"/>
</tiles:insert>
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
<c:forEach var="category" items="${CompareMetricsForm.metrics}">
   <c:choose>
      <c:when test="${category.key == availability}">
         <c:set var="heading"
              value="resource.common.monitor.visibility.AvailabilityTH" />
      </c:when>
      <c:when test="${category.key == performance}">
         <c:set var="heading"
              value="resource.common.monitor.visibility.PerformanceTH" />
      </c:when>
      <c:when test="${category.key == throughput}">
         <c:set var="heading"
              value="resource.common.monitor.visibility.UsageTH" />
      </c:when>
      <c:when test="${category.key == utilization}">
         <c:set var="heading"
              value="resource.common.monitor.visibility.UtilizationTH" />
      </c:when>
      <c:otherwise>
         <blockquote>
         Error: unknown metric category
         <c:out value="${MetricsDisplayForm.categoryList[0]}" />
         </blockquote>
      </c:otherwise>
   </c:choose>
   <tr class="ListHeaderDark">
      <td width="1%" class="ListHeaderInactiveSorted">&nbsp;</td>
      <td width="68%" colspan="2" class="ListHeaderInactiveSorted"><fmt:message key="${heading}"/></td>
      <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.LowTH"/></td>
      <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.AvgTH"/></td>
      <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.PeakTH"/></td>
      <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.LastTH"/></td>
   </tr>

   <c:forEach var="metricList" items="${category.value}">
      <c:url var="chartUrl" value="/resource/common/monitor/Visibility.do">
         <c:param name="mode" value="chartSingleMetricMultiResource" />
         <c:param name="m" value="${metricList.key.id}" />
         <c:forEach var="rmds" items="${CompareMetricsForm.r}">
            <c:param name="r" value="${rmds}" />
         </c:forEach>
         <c:if test="${not empty groupId}">
            <c:param name="groupId" value="${groupId}"/>
         </c:if>
      </c:url>
      <tr class="ListRow">
         <td class="ListCellCheckbox">&nbsp;</td>
         <td width="1%" class="ListCellCheckbox"><a href="<c:out value="${chartUrl}" />"><html:img page="/images/icon_chart.gif" width="10" height="10" alt="" border="0"/></a></td>
         <td class="ListCell"><a href="<c:out value="${chartUrl}" />"><c:out value="${metricList.key.displayName}" /></a></td>
         <td class="ListCellRight" nowrap>&nbsp;</td>
         <td class="ListCellRight" nowrap>&nbsp;</td>
         <td class="ListCellRight" nowrap>&nbsp;</td>
         <td class="ListCellRight" nowrap>&nbsp;</td>
      </tr>
      <!-- iterate over the resources -->
      <c:forEach var="rmds" items="${metricList.value}"> <%-- MetricDisplaySummary --%>
         <c:url var="singleResourceSingleMetricChartUrl" value="/resource/common/monitor/Visibility.do">
            <c:param name="mode" value="chartSingleMetricSingleResource" />
            <c:param name="m" value="${metricList.key.id}" />
            <c:param name="id" value="${rmds.resource.id}" />
         </c:url>
         <tr class="ListRow">
            <td class="ListCellCheckbox">&nbsp;</td>
            <td width="1%" class="ListCellCheckbox">&nbsp;</td>
            <td class="ListCell">
               <a href="<c:out value="${singleResourceSingleMetricChartUrl}"/>">
                  <c:out value="${rmds.resource.name}"/>
               </a>
            </td>
            <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[min].valueFmt}" /></td>
            <c:choose>
               <c:when test="${rmds.collectionType == 0}"> <%-- 0=DYNAMIC --%>
                  <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[average].valueFmt}" /></td>
               </c:when>
               <c:otherwise>
                  <td class="ListCellRight" width="%5" nowrap> - </td>
               </c:otherwise>
            </c:choose>
            <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[max].valueFmt}" /></td>
            <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[last].valueFmt}" /></td>
            <!-- baseline not shown -->
         </tr>
      </c:forEach>
   </c:forEach>
</c:forEach>
</table>
  
<tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="useCurrentButton" value="true"/>
</tiles:insert> 
</div>
<html:link href="javascript:document.CompareMetricsForm.submit()" onclick="clickLink('CompareMetricsForm', 'back')"><fmt:message 
key="resource.common.monitor.visibility.CompareMetricsReturnLink">
<fmt:param value="${TitleParam}"/></fmt:message></html:link>

<html:hidden property="groupId"/>
<html:hidden property="category"/>
<html:hidden property="ctype"/>
<c:forEach var="r" items="${CompareMetricsForm.r}">
<input type="hidden" name="r" value="<c:out value="${r}"/>">
</c:forEach>

</html:form>

<tiles:insert definition=".page.footer"/>
  
