<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="crpbuWidget" value="childResourcesPerformanceByUrl"/>
<script type="text/javascript">
   initializeWidgetProperties('<c:out value="${crpbuWidget}"/>');
   crpbuWidgetProps = getWidgetProperties('<c:out value="${crpbuWidget}"/>');
</script>

<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="detailMode" ignore="true"/>
<tiles:importAttribute name="useChart" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_SMSR"
             var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_URL" var="MODE_MON_URL"/>

<%-- for v1, we don't show any charts --%>
<c:set var="useChart" value="false"/>

<c:url var="psAction" value="${selfAction}">
   <c:if test="${not empty param.so}">
      <c:param name="so" value="${param.so}"/>
   </c:if>
   <c:if test="${not empty param.sc}">
      <c:param name="sc" value="${param.sc}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.low}">
      <c:param name="low" value="${PerformanceForm.low}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.avg}">
      <c:param name="avg" value="${PerformanceForm.avg}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.peak}">
      <c:param name="peak" value="${PerformanceForm.peak}"/>
   </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
   <c:if test="${not empty param.so}">
      <c:param name="so" value="${param.so}"/>
   </c:if>
   <c:if test="${not empty param.sc}">
      <c:param name="sc" value="${param.sc}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.low}">
      <c:param name="low" value="${PerformanceForm.low}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.avg}">
      <c:param name="avg" value="${PerformanceForm.avg}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.peak}">
      <c:param name="peak" value="${PerformanceForm.peak}"/>
   </c:if>
</c:url>

<c:url var="sAction" value="${selfAction}">
   <c:if test="${not empty param.pn}">
      <c:param name="pn" value="${param.pn}"/>
   </c:if>
   <c:if test="${not empty param.ps}">
      <c:param name="ps" value="${param.ps}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.low}">
      <c:param name="low" value="${PerformanceForm.low}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.avg}">
      <c:param name="avg" value="${PerformanceForm.avg}"/>
   </c:if>
   <c:if test="${not empty PerformanceForm.peak}">
      <c:param name="peak" value="${PerformanceForm.peak}"/>
   </c:if>
</c:url>

<c:url var="iconPath" value="/images/icon_chart.gif"/>

<!-- CHILD RESOURCES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="pbuListTable">
   <tr>
      <td class="ListCell">
         <b><fmt:message key="resource.common.monitor.visibility.performance.SelectMetricInputLabel"/></b>

         <bean:define id="metricCount" name="PerformanceForm" property="metricCount"/>
         <c:choose>

            <c:when test="${metricCount eq 1}">
               ${MeasurementDef.displayName}
            </c:when>

            <c:otherwise>
               <html:select property="scheduleId" size="1" onchange="this.form.submit()">
                  <html:option key="resource.common.monitor.visibility.performance.SelectMetricInputOptionLabel"
                               value="-1" disabled="true"/>
                  <html:optionsCollection property="schedules" value="id" label="definition.displayName"/>
               </html:select>
            </c:otherwise>

         </c:choose>
      </td>
   </tr>
</table>

<div id="pbuListDiv">
   <c:choose>

      <c:when test="${MeasurementDef ne null and not empty summaries}">
         <display:table items="${summaries}" var="summary" action="${sAction}" width="100%" cellspacing="0"
                        cellpadding="0">

            <display:column width="67%" property="callDestination" title="${MeasurementDef.destinationType}"
                            isLocalizedTitle="false" sortAttr="key.callDestination"
                            styleClass="ListCell">
               <display:pathdecorator preChars="8" postChars="20" strict="true" styleClass="ListCellPopup4"/>
            </display:column>

            <display:column width="8%" property="count" title="resource.common.monitor.visibility.CallsTH"
                            sortAttr="SUM(value.count)"
                            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine"/>

            <display:column width="8%" property="minimum" title="resource.common.monitor.visibility.LowTH"
                            sortAttr="MIN(value.minimum)"
                            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
               <display:metricdecorator unit="${MeasurementDef.units}"
                                        defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
            </display:column>

            <display:column width="8%" property="average" title="resource.common.monitor.visibility.AvgTH"
                            sortAttr="SUM(value.total)/SUM(value.count)" defaultSort="true"
                            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
               <display:metricdecorator unit="${MeasurementDef.units}"
                                        defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
            </display:column>

            <display:column width="8%" property="maximum" title="resource.common.monitor.visibility.PeakTH"
                            sortAttr="MAX(value.maximum)"
                            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
               <display:metricdecorator unit="${MeasurementDef.units}"
                                        defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
            </display:column>

         </display:table>

         <tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
            <tiles:put name="widgetInstanceName" value="childResourcesPerformanceByUrl"/>
            <tiles:put name="usePager" value="true"/>
            <tiles:put name="listItems" beanName="summaries"/>
            <tiles:put name="pageSizeAction" value="${sAction}"/>
         </tiles:insert>
      </c:when>

      <c:when test="${MeasurementDef ne null}">
         <tiles:insert definition=".resource.common.monitor.visibility.noPerfs"/>
      </c:when>

   </c:choose>
</div>


<!-- / -->
