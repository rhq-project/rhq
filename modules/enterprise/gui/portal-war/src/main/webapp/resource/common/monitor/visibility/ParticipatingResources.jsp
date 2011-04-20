<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="multiMetric" ignore="true"/>
<tiles:importAttribute name="multiResource" ignore="true"/>

<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="MAX_KEY"
      var="max"/>
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="MIN_KEY"
      var="min"/>
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="AVERAGE_KEY"
      var="average"/>
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="LAST_KEY"
      var="last"/>

<c:if test="${empty multiMetric}">
   <c:set var="multiMetric" value="false"/>
</c:if>
<c:if test="${empty multiResource}">
   <c:set var="multiResource" value="false"/>
</c:if>

<c:if test="${not multiMetric}">
   <html:hidden property="m" value="${ViewChartForm.m[0]}"/>
</c:if>

<c:set var="widgetInstanceName" value="listMetrics"/>
<script type="text/javascript">
   var pageData = new Array();
   initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
   widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<!-- PARTICIPATING RESOURCE TITLE -->
<tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" value="resource.common.monitor.visibility.chart.ParticipatingResourceTab"/>
</tiles:insert>
<!-- / -->
<c:forEach var="metricSummary" varStatus="msStatus" items="${metricSummaries}">
   <c:if test="${multiResource}">
      <c:set var="resource" value="${resources[msStatus.index]}"/>
      <html:hidden property="r" value="${resource.id}" /> <%-- supply all resources on the chart  --%>
   </c:if>
</c:forEach>
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
   <tr class="ListHeaderDark">
      <c:if test="${multiMetric}">
         <td width="1%" class="ListHeaderInactiveSorted">
            <html:img
                  page="/images/spacer.gif" width="1" height="1" alt=""
                  border="0"/>
         </td>
      </c:if>
      <td width="29%" class="ListHeaderInactiveSorted">
         <fmt:message
               key="resource.common.monitor.visibility.MetricNameTH"/>
      </td>
      <c:if test="${multiResource}">
         <td width="1%" class="ListHeaderInactiveSorted">
            <html:img
                  page="/images/spacer.gif" width="1" height="1" alt=""
                  border="0"/>
         </td>
      </c:if>
      <td width="69%" class="ListHeaderInactiveSorted">
         <fmt:message
               key="resource.common.monitor.visibility.ResourceTH"/>
      </td>
      <%--
            Don't display the metric summary min, average, max and last
            because they are confusing due to the "averaged" nature of the chart
            data. (2003/05/23 --JW)
          <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
            key="resource.common.monitor.visibility.LowTH"/></td>
          <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
            key="resource.common.monitor.visibility.AvgTH"/></td>
          <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
            key="resource.common.monitor.visibility.PeakTH"/></td>
          <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
            key="resource.common.monitor.visibility.LastTH"/></td>
      --%>
   </tr>
   <c:forEach var="metricSummary" varStatus="msStatus" items="${metricSummaries}">
      <c:choose>
         <c:when test="${multiResource}">
            <c:set var="resource" value="${resources[msStatus.index]}"/>
         </c:when>
         <c:otherwise>
            <c:set var="resource" value="${resources[0]}"/>
         </c:otherwise>
      </c:choose>
      <c:url var="resourceUrl" value="/rhq/resource/monitor/graphs.xhtml">
         <c:param name="id" value="${resource.id}"/>
      </c:url>
      <c:url var="parentResourceUrl" value="/rhq/resource/monitor/graphs.xhtml">
         <c:param name="id" value="${resource.parentResource.id}"/>
      </c:url>
      <tr class="ListRow">
         <c:if test="${multiMetric}">
            <td class="ListCellCheckbox" valign="top">
               <html:multibox
                     property="m" value="${metricSummary.definitionId}"
                     onclick="ToggleSelection(this, widgetProperties);"
                     styleClass="metricList"/>
            </td>
         </c:if>
         <c:if test="${!multiResource || (multiResource && msStatus.first)}">
            <td
                  <c:if test="${multiResource && msStatus.first}"> rowspan="
                     <c:out value='${resourcesSize}'/>
                     "
                  </c:if>
                  class="ListCellPrimary" valign="top">
               <c:out value="${metricSummary.label}"/>
            </td>
         </c:if>
         <c:choose>
            <c:when test="${multiResource}">
               <c:set var="maxResources">
                  <fmt:message key="resource.common.monitor.visibility.chart.MaxResources"/>
               </c:set>
               <c:set var="maxMessage">
                  <fmt:message key="resource.common.monitor.visibility.chart.TooManyResources">
                     <fmt:param value="${maxResources}"/>
                  </fmt:message>
               </c:set>
               <td class="ListCellLeftLineNoPadding" valign="top">
                  <html:multibox
                        property="resourceIds" value="${resource.id}"
                        onclick="ToggleSelection(this, widgetProperties, ${maxResources}, '${maxMessage}');"
                        styleClass="resourceList"/>
               </td>
               <c:set var="resCellClass" value="ListCellPrimary"/>
            </c:when>
            <c:otherwise>
               <c:set var="resCellClass" value="ListCellLeftLine"/>
            </c:otherwise>
         </c:choose>
         <c:if test="${!multiMetric || (multiMetric && msStatus.first)}">
            <td
                  <c:if test="${multiMetric && msStatus.first}"> rowspan="
                     <c:out value='${metricSummariesSize}'/>
                     "
                  </c:if>
                  class="<c:out value='${resCellClass}'/>" valign="top">
               <html:link href="${parentResourceUrl}"> 
                  <c:out value="${resource.parentResource.name}"/>
               </html:link>
               <br>
               <html:img page="/images/hierarchy.gif" width="16" height="16" alt="" border="0"/>
               <html:link href="${resourceUrl}">
                  <c:out value="${resource.name}"/>
               </html:link>
            </td>
         </c:if>
            <%--
                  Don't display the metric summary min, average, max and last
                  because they are confusing due to the "averaged" nature of the chart
                  data. (2003/05/23 --JW)
                <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[min].valueFmt}"/></td>
                <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[average].valueFmt}"/></td>
                <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[max].valueFmt}"/></td>
                <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[last].valueFmt}"/></td>
            --%>
      </tr>
   </c:forEach>
</table>

<!--  REDRAW SELECTED TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
   <tr>
      <td id="<c:out value="${widgetInstanceName}"/>RedrawTd">
         <div
               id="<c:out value="${widgetInstanceName}"/>RedrawDiv">
            <c:if
                  test="${multiMetric || multiResource}">
               <html:img
                     page="/images/tbb_redrawselectedonchart_gray.gif" width="171"
                     height="16" alt="" border="0"/>
            </c:if>
         </div>
      </td>
      <td width="100%" align="right">&nbsp;</td>
      <%--
            Don't display the metric summary min, average, max and last
            because they are confusing due to the "averaged" nature of the chart
            data. (2003/05/23 --JW)
          <td width="100%" align="right"><fmt:message
            key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
          <td><html:image property="redraw"
            page="/images/dash-button_go-arrow.gif" border="0"/></td>
      --%>
   </tr>
</table>
<!--  /  -->

</div>

<input type="Hidden" id="privateChart">
<script type="text/javascript">
   testCheckboxes('<c:out value="${widgetInstanceName}"/>');
</script>
