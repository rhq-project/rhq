<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="multiResource" ignore="true"/>
<c:if test="${empty multiResource}">
   <c:set var="multiResource" value="false"/>
</c:if>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE"/>
<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<c:choose>
   <c:when test="${not empty groupId}">
      <html:hidden property="groupId" value="${groupId}"/>
   </c:when>
   <c:when test="${not empty parent}">
      <html:hidden property="parent" value="${parent}"/>
      <html:hidden property="type" value="${type}"/>
   </c:when>
   <c:otherwise>
      <html:hidden property="id" value="${Resource.id}"/>
   </c:otherwise>
</c:choose>

<html:hidden property="ctype"/>
<html:hidden property="mode" value="${param.mode}"/>

<c:forEach var="mid" items="${ViewChartForm.origM}">
   <html:hidden property="origM" value="${mid}"/>
</c:forEach>

<table width="100%" cellpadding="3" cellspacing="0" border="0">
<tr>
   <td colspan="6" class="BlockBottomLine">
      <html:img
            page="/images/spacer.gif" width="1" height="1" border="0"/>
   </td>
</tr>
<tr>
   <td width="30" rowspan="4">
      <html:img
            page="/images/spacer.gif" width="30" height="1" border="0"/>
   </td>
   <td width="125">
      <html:hidden property="showValues"/>
      <input type="checkbox" name="showValuesCB"
      <c:if test="${ViewChartForm.showValues}"> checked</c:if>
                                                onclick="javascript:checkboxToggled('showValuesCB', 'showValues');">
      <html:img page="/images/icon_actual.gif" width="11"
                height="11" border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.Actual"/>
   </td>
   <td width="125">
      <html:hidden property="showPeak"/>
      <input type="checkbox" name="showPeakCB"
      <c:if test="${ViewChartForm.showPeak}"> checked</c:if>
                                              onclick="javascript:checkboxToggled('showPeakCB', 'showPeak');">
      <html:img page="/images/icon_peak.gif" width="11" height="11"
                border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.Peak"/>
   </td>
   <td width="125">
      <html:hidden property="showHighRange"/>
      <input type="checkbox" name="showHighRangeCB"
      <c:if test="${ViewChartForm.showHighRange}"> checked</c:if>
      <c:if test="${empty ViewChartForm.highRange}"> disabled</c:if>
                                                     onclick="javascript:checkboxToggled('showHighRangeCB', 'showHighRange');">
      <html:img page="/images/icon_highrange.gif" width="11"
                height="11" border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.HighRange"/>
   </td>
   <td rowspan="4">
      <html:img page="/images/spacer.gif" width="30" height="1" border="0"/>
   </td>
   <td rowspan="4" valign="top">

      <table border="0">
         <tr>
            <td class="LinkBox">
               <c:if test="${not multiResource}">
                  <c:url var="alertLink" value="/alerts/Config.do">
                     <c:param name="mode" value="new"/>
                     <c:param name="id" value="${Resource.id}"/>
                     <c:param name="metricId" value="${param.m}"/>
                  </c:url>
                  <html:link href="${alertLink}">
                     <fmt:message key="resource.common.monitor.visibility.NewAlertLink"/>
                     <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
                  </html:link>
                  <br>
               </c:if>
               <html:hidden property="saveChart" value="false"/>
               <html:link href="."
                          onclick="ViewChartForm.saveChart.value='true'; ViewChartForm.submit(); return false;">
                  <fmt:message key="resource.common.monitor.visibility.SaveChartToDash"/>
                  <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
               </html:link>
               <br>

               <c:if test="${not empty back}">
                  <html:link page="${back}">
                     <fmt:message key="resource.common.monitor.visibility.Back2Resource"/>
                     <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
                  </html:link>
               </c:if>

            </td>
         </tr>
      </table>

   </td>
</tr>
<tr>
   <td>
      <c:if test="${canControl}">
         <html:hidden property="showEvents"/>
         <input type="checkbox" name="showEventsCB"
         <c:if test="${ViewChartForm.showEvents}"> checked</c:if>
                                                   onclick="javascript:checkboxToggled('showEventsCB', 'showEvents');">
         <html:img page="/images/icon_controlactions.gif"
                   width="11" height="11" border="0"/>
         <fmt:message
               key="resource.common.monitor.visibility.chart.ControlActions"/>
      </c:if>
   </td>
   <td>
      <html:hidden property="showAverage"/>
      <input type="checkbox" name="showAverageCB"
      <c:if test="${ViewChartForm.showAverage}"> checked</c:if>
                                                 onclick="javascript:checkboxToggled('showAverageCB', 'showAverage');">
      <html:img page="/images/icon_average.gif" width="11" height="11"
                border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.Average"/>
   </td>
   <td>
      <html:hidden property="showBaseline"/>
      <input type="checkbox" name="showBaselineCB"
      <c:if test="${ViewChartForm.showBaseline}"> checked</c:if>
      <c:if test="${empty ViewChartForm.baseline}"> disabled</c:if>
                                                    onclick="javascript:checkboxToggled('showBaselineCB', 'showBaseline');">
      <html:img page="/images/icon_baseline.gif" width="11"
                height="11" border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.Baseline"/>
   </td>
</tr>
<tr>
   <td>&nbsp;</td>
   <td>
      <html:hidden property="showLow"/>
      <input type="checkbox" name="showLowCB"
      <c:if test="${ViewChartForm.showLow}"> checked</c:if>
                                             onclick="javascript:checkboxToggled('showLowCB', 'showLow');">
      <html:img page="/images/icon_low.gif" width="11" height="11"
                border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.Low"/>
   </td>
   <td>
      <html:hidden property="showLowRange"/>
      <input type="checkbox" name="showLowRangeCB"
      <c:if test="${ViewChartForm.showLowRange}"> checked</c:if>
      <c:if test="${empty ViewChartForm.lowRange}"> disabled</c:if>
                                                    onclick="javascript:checkboxToggled('showLowRangeCB', 'showLowRange');">
      <html:img page="/images/icon_lowrange.gif" width="11"
                height="11" border="0"/>
      <fmt:message
            key="resource.common.monitor.visibility.chart.LowRange"/>
   </td>
</tr>
<tr>
   <td colspan="3">
      <html:image page="/images/fb_redraw.gif" property="redraw" border="0"
                  onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');"
                  onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');"
                  onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')" tabindex="1" accesskey="r"/>
   </td>
</tr>
<tr>
   <td colspan="6" class="BlockBottomLine">
      <html:img
            page="/images/spacer.gif" width="1" height="1" border="0"/>
   </td>
</tr>
</table>
<script language="JavaScript" type="text/javascript">
   <!--
   document.forms["ViewChartForm"].elements["showValuesCB"].focus();
   // -->
</script>
