<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="rows"/>
<tiles:importAttribute name="useChart"/>
<tiles:importAttribute name="category"/>
<tiles:importAttribute name="buttonMode" ignore="true"/>
<tiles:importAttribute name="useCheckboxes" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty useCheckboxes}">
   <c:set var="useCheckboxes" value="true"/>
</c:if>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_SMSR"
             var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_SMMR"
             var="MODE_MON_CHART_SMMR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_MMSR"
             var="MODE_MON_CHART_MMSR"/>

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
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="BASELINE_KEY"
      var="baseline"/>
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="HIGH_RANGE_KEY"
      var="high"/>
<hq:constant
      classname="org.rhq.core.domain.measurement.ui.MetricDisplayConstants"
      symbol="LOW_RANGE_KEY"
      var="low"/>

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
      symbol="CAT_AVAILABILITY" var="availability"/>
<hq:constant
      classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
      symbol="CAT_PERFORMANCE" var="performance"/>
<hq:constant
      classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
      symbol="CAT_THROUGHPUT" var="throughput"/>
<hq:constant
      classname="org.rhq.enterprise.server.legacy.measurement.MeasurementConstants"
      symbol="CAT_UTILIZATION" var="utilization"/>

<c:set var="eid" value="${Resource.entityId.type}:${Resource.id}"/>
<c:choose>
   <c:when test="${NumChildResources <= 18}">
      <c:set var="metadataPopupHeight" value="${300 + 26 * NumChildResources}"/>
   </c:when>
   <c:otherwise>
      <c:set var="metadataPopupHeight" value="758"/>
   </c:otherwise>
</c:choose>
<c:forEach var="metricDisplaySummary" items="${rows}">
<c:url var="metadataLink" value="/resource/common/monitor/Visibility.do">
   <c:param name="mode" value="metricMetadata"/>
   <c:param name="m" value="${metricDisplaySummary.templateId}"/>
   <c:param name="eid" value="${eid}"/>
   <c:choose>
      <c:when test="${not empty childResourceType}">
         <c:param name="ctype" value="${ctype}"/>
      </c:when>
   </c:choose>
</c:url>
<c:url var="chartLink" value="/resource/common/monitor/Visibility.do">
   <c:param name="id" value="${Resource.id}"/>
   <c:param name="m" value="${metricDisplaySummary.templateId}"/>
   <c:choose>
      <c:when test="${metricDisplaySummary.availUp > 1}">
         <c:param name="mode" value="${MODE_MON_CHART_SMMR}"/>
      </c:when>
      <c:otherwise>
         <c:param name="mode" value="${MODE_MON_CHART_SMSR}"/>
      </c:otherwise>
   </c:choose>
   <c:choose>
      <%-- 
      the measurement bean doesn't know the resource type, it really
      should.... the favorites can't link to the charts properly without
      them (see pr 7223)
      --%>
      <c:when test="${not empty childResourceType}">
         <c:param name="ctype" value="${ctype}"/>
      </c:when>
   </c:choose>
</c:url>
<tr class="ListRow">
<c:if test="${useCheckboxes}">
   <td class="ListCellCheckbox">
      <html:multibox property="m"
                     onclick="ToggleSelectionTwoButtons(this, mdsWidgetProps, '${buttonMode}');"
                     value="${metricDisplaySummary.templateId}"
                     styleClass="availableListMember"/>
   </td>
</c:if>
<c:if test="${useChart}">
   <td width="1%" class="ListCellCheckbox">
      <a href="<c:out
    value="${chartLink}" />">
         <html:img
               page="/images/icon_chart.gif" width="10" height="10" alt=""
               border="0"/>
      </a>
   </td>
</c:if>
<c:choose>
   <c:when test="${useChart}">
      <td class="ListCellPrimary" nowrap><a href="<c:out value="${chartLink}" />">
         <c:out value="${metricDisplaySummary.label}"/>
      </a></td>
   </c:when>
   <c:otherwise>
      <td class="ListCellPrimary" nowrap>
         <c:out value="${metricDisplaySummary.label}"/>
      </td>
   </c:otherwise>
</c:choose>
<c:choose>
   <%-- used for favorites --%>
   <c:when test="${MetricsDisplayForm.showMetricSource}">
      <td class="ListCellLeftLineNoPadding" width="10%" align="center">
         <c:out value="${metricDisplaySummary.metricSource}" default="&nbsp;" escapeXml="false"/>
      </td>
   </c:when>
   <c:otherwise>
      <!-- metric source not shown -->
   </c:otherwise>
</c:choose>
<c:choose>
   <c:when test="${MetricsDisplayForm.showNumberCollecting}">
      <td class="ListCellLeftLineNoPadding" align="center">
         <c:out value="${metricDisplaySummary.availUp}"/>
      </td>
   </c:when>
   <c:otherwise>
      <!-- number collecting not shown -->
   </c:otherwise>
</c:choose>
<td class="ListCellLeftLineNoPadding" align="center">
   <c:out value="${metricDisplaySummary.alertCount}"/>
</td>
<c:choose>
   <c:when test="${MetricsDisplayForm.showBaseline}">
      <td class="ListCellRight" nowrap>
      <span class="MonitorMetricsValue">
      <c:choose>
         <c:when test="${MetricsDisplayForm.displayBaseline}">
            <c:out value="${metricDisplaySummary.metrics[baseline].valueFmt}"/>
         </c:when>
         <c:when test="${MetricsDisplayForm.displayHighRange}">
            <c:out value="${metricDisplaySummary.metrics[high].valueFmt}"/>
         </c:when>
         <c:when test="${MetricsDisplayForm.displayLowRange}">
            <c:out value="${metricDisplaySummary.metrics[low].valueFmt}"/>
         </c:when>
      </c:choose>
      </span>
      </td>
   </c:when>
   <c:otherwise>
      <!-- baseline not shown -->
   </c:otherwise>
</c:choose>
<c:choose>
   <c:when test="${metricDisplaySummary.designated && category == availability}">
      <td class="ListCellRight" width="%5" nowrap> &nbsp; </td>
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[average].valueFmt}"/>
      </td>
      <td class="ListCellRight" width="%5" nowrap> &nbsp; </td>
      <td class="ListCellCheckboxLeftLine" width="%5" nowrap>
         <c:choose>
            <c:when test="${metricDisplaySummary.metrics[last].value == 1}">
               <html:img page="/images/icon_available_green.gif" width="12" height="12" alt="" border="0"
                         align="middle"/>
            </c:when>
            <c:when test="${metricDisplaySummary.metrics[last].value == 0}">
               <html:img page="/images/icon_available_red.gif" width="12" height="12" alt="" border="0" align="middle"/>
            </c:when>
            <c:otherwise>
               <html:img page="/images/icon_available_yellow.gif" width="12" height="12" alt="" border="0"
                         align="middle"/>
            </c:otherwise>
         </c:choose>
      </td>
   </c:when>
   <c:when test="${metricDisplaySummary.collectionType == dynamic}">
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[min].valueFmt}"/>
      </td>
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[average].valueFmt}"/>
      </td>
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[max].valueFmt}"/>
      </td>
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[last].valueFmt}"/>
      </td>
   </c:when>
   <c:otherwise>
      <td class="ListCellRight" width="%5" nowrap> -</td>
      <td class="ListCellRight" width="%5" nowrap> -</td>
      <td class="ListCellRight" width="%5" nowrap> -</td>
      <td class="ListCellRight" width="%5" nowrap>
         <c:out value="${metricDisplaySummary.metrics[last].valueFmt}"/>
      </td>
      <!-- baseline not shown -->
   </c:otherwise>
</c:choose>
<td width="1%" class="ListCellCheckbox">
   <a href=""
      onclick="window.open('<c:out value="${metadataLink}" />','_metricMetadata','width=800,height=<c:out value="${metadataPopupHeight}" />,scrollbars=yes,toolbar=no,left=80,top=80,resizable=yes'); return false;">
      <html:img
            page="/images/icon_info.gif" width="11" height="11" alt=""
            border="0"/>
   </a>
</td>
</tr>
</c:forEach>
