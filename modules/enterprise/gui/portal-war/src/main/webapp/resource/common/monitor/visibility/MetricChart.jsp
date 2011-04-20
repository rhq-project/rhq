<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="multiMetric" ignore="true"/>
<tiles:importAttribute name="multiResource" ignore="true"/>

<c:if test="${empty multiMetric}">
<c:set var="multiMetric" value="false"/>
</c:if>
<c:if test="${empty multiResource}">
<c:set var="multiResource" value="false"/>
</c:if>

<!--  METRIC CHART TITLE -->
<tiles:insert definition=".header.tab">
<tiles:put name="tabKey" value="resource.common.monitor.visibility.chart.MetricChartTab"/>
</tiles:insert>
<!--  /  -->

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="MonitorBlockContainer">
  <tr>
    <td>
      <!-- Table Content -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="MonitorChartBlock" colspan="3">
            <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chartparams">
                <tiles:put name="multiResource" beanName="multiResource"/>
                <tiles:put name="ctype" value="${ctype}"/>
            </tiles:insert>
          </td>
        </tr>
        <tr>
          <td width="50%" class="MonitorChartCell"
            rowspan="2"><html:img page="/images/spacer.gif" width="1"
            height="1" border="0"/></td>
          <td class="MonitorChartCell">
            <c:forEach var="i" varStatus="status" begin="0" end="${chartDataKeysSize - 1}">
            <c:url var="chartUrl" value="/resource/MetricChart">
            <c:param name="chartDataKey" value="${chartDataKeys[i]}"/>
            <c:param name="measurementUnits" value="${chartedMetrics[i].units}"/>
            <%-- 
            <c:param name="unitUnits" value="${chartedMetrics[i].unitUnits}"/>
            <c:param name="unitScale" value="${chartedMetrics[i].unitScale}"/>
             --%>
            <c:param name="showPeak" value="${ViewChartForm.showPeak}"/>
            <c:param name="showHighRange" value="${ViewChartForm.showHighRange}"/>
            <c:param name="showValues" value="${ViewChartForm.showValues}"/>
            <c:param name="showAverage" value="${ViewChartForm.showAverage}"/>
            <c:param name="showLowRange" value="${ViewChartForm.showLowRange}"/>
            <c:param name="showLow" value="${ViewChartForm.showLow}"/>
            <c:param name="collectionType" value="${chartedMetrics[i].collectionType}"/>
            <c:param name="showEvents" value="false"/>
            <c:param name="showBaseline" value="${ViewChartForm.showBaseline}"/>
            <c:param name="baseline" value="${ViewChartForm.baselineRaw}"/>
            <c:param name="highRange" value="${ViewChartForm.highRangeRaw}"/>
            <c:param name="lowRange" value="${ViewChartForm.lowRangeRaw}"/>
            </c:url>
            <b><fmt:message key="resource.common.monitor.visibility.chart.Metric"/></b>
            <c:out value="${chartedMetrics[i].metricName}"/><br>
            <html:img src="${chartUrl}" width="755" height="300" border="0"/>
            <c:if test="${!status.last}">&nbsp;<br><br></c:if>
            </c:forEach>
          </td>
          <td width="50%" class="MonitorChartCell"
            rowspan="2"><html:img page="/images/spacer.gif" width="1"
            height="1" border="0"/></td>
        </tr>
        <tr>
          <td class="MonitorChartBlock" colspan="3">
            <tiles:insert page="/resource/common/monitor/visibility/ChartTimeIntervalToolbar.jsp">
            <tiles:put name="rangeNow" beanName="ViewChartForm" beanProperty="rangeNow"/>
            <tiles:put name="begin" beanName="metricRange" beanProperty="begin"/>
            <tiles:put name="end" beanName="metricRange" beanProperty="end"/>
            </tiles:insert>
            &nbsp;<br>
          </td>
        </tr>
        <tr>
          <td colspan="3">
            <tiles:insert definition=".resource.common.monitor.visibility.embeddedMetricDisplayRange">
              <tiles:put name="form" beanName="ViewChartForm"/>
              <tiles:put name="formName" value="ViewChartForm"/>
            </tiles:insert>
          </td>
        </tr>
      </table>
      <!--  /  -->
    </td>
  </tr>
</table>
