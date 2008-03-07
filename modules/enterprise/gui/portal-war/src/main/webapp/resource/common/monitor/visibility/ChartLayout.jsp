<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:choose>
<c:when test="${not empty toDashboard}">
   <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr>
         <td rowspan="3" class="PageTitle">
            <html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/>
         </td>
         <td width="100%">
            <table width="100%" border="0" cellspacing="0" cellpadding="4">
               <tr class="ListHeader">
                  <td>
                     <fmt:message key="dash.home.SavedQueries"/>
                  </td>
               </tr>
               <tr class="ListRow">
                  <td>
                     <fmt:message key="resource.common.monitor.visibility.error.ChartRemoved"/>
                  </td>
               </tr>
               <tr class="ListRow">
                  <td>
                     <html:link page="/Dashboard.do">
                        <fmt:message key="alert.current.detail.link.noresource.Rtn"/>
                     </html:link>
                  </td>
               </tr>
            </table>
         </td>
      </tr>
   </table>
</c:when>
<c:otherwise>

   <hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                symbol="MODE_MON_CHART_SMSR"
                var="MODE_MON_CHART_SMSR"/>
   <hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                symbol="MODE_MON_CHART_MMSR"
                var="MODE_MON_CHART_MMSR"/>
   <hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                symbol="MODE_MON_CHART_SMMR"
                var="MODE_MON_CHART_SMMR"/>

   <script language="JavaScript" src="<html:rewrite page="/js/chart.js"/>" type="text/javascript"></script>

   <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr>
         <td colspan="3">
            <c:choose>
               <c:when test="${param.mode == MODE_MON_CHART_MMSR}">
                  <c:set var="metricName">
                     <fmt:message key="resource.common.monitor.visibility.MultipleMetric"/>
                  </c:set>
               </c:when>
               <c:otherwise>
                  <c:set var="metricName" value="${ViewChartForm.chartName}"/>
               </c:otherwise>
            </c:choose>
            <tiles:insert definition=".page.title.resource.generic">
               <tiles:put name="titleKey" value="resource.common.monitor.visibility.SingleResourceChartPageTitle"/>
               <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
               <tiles:put name="subTitleName" beanName="metricName"/>

            </tiles:insert>
         </td>
      </tr>
      <tr>
         <td>
            <html:img page="/images/spacer.gif" width="75" height="1" alt="" border="0"/>
         </td>
         <td width="100%">
            <tiles:insert definition=".portlet.confirm"/>
            <html:form action="/resource/common/monitor/visibility/ViewChart">
               <c:choose>
                  <c:when test="${param.mode == MODE_MON_CHART_SMSR}">
                     <html:hidden property="chartName" value="${Resource.name}: ${chartedMetrics[0].metricName}"/>
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart"/>
                     &nbsp;<br>
<%--                      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs"/> 
                     &nbsp;<br>
--%>                     
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.baselinerangeparams">
                        <tiles:put name="edit" value="false"/>
                     </tiles:insert>
                     &nbsp;<br>
                  </c:when>

                  <c:when test="${param.mode == MODE_MON_CHART_MMSR}">
                     <html:hidden property="chartName" value="${Resource.name}: XXX "/>
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart">
                        <tiles:put name="multiMetric" value="true"/>
                     </tiles:insert>
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.savecharttoolbar"/>
                     &nbsp;<br>
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs">
                        <tiles:put name="multiMetric" value="true"/>
                     </tiles:insert>
                     &nbsp;<br>
                  </c:when>

                  <c:when test="${param.mode == MODE_MON_CHART_SMMR}">
                     <html:hidden property="chartName" value="${ViewChartForm.chartName}: ${chartedMetrics[0].metricName}" />
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart">
                        <tiles:put name="multiResource" value="true"/>
                     </tiles:insert>
                     &nbsp;<br>
                     <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs">
                        <tiles:put name="multiResource" value="true"/>
                     </tiles:insert>
                     &nbsp;<br>
                  </c:when>
               </c:choose>
            </html:form>
         </td>
         <td>
            <html:img page="/images/spacer.gif" width="80" height="1" alt="" border="0"/>
         </td>
      </tr>
   </table>
</c:otherwise>
</c:choose>
