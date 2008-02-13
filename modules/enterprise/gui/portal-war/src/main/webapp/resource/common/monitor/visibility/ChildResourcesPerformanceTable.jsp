<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="perfSummaries" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="urlMode" ignore="true"/>
<tiles:importAttribute name="url" ignore="true"/>

<c:if test="${empty mode}">
   <c:set var="mode" value="url"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="MonitorBlock">
   <tr>
      <td align="left">
         <c:choose>
            <c:when test="${MeasurementDef ne null}">

               <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
                  <tiles:put name="form" beanName="PerformanceForm"/>
                  <tiles:put name="formName" value="PerformanceForm"/>
                  <tiles:put name="mode" beanName="mode"/>
                  <tiles:put name="id" value="${Resource.id}"/>
               </tiles:insert>

               <!-- Table Content -->
               <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                     <td class="MonitorChartBlock">
                        <tiles:insert definition=".resource.common.monitor.visibility.performance.controlForm"/>
                     </td>
                  </tr>
                  <tr>
                     <td class="MonitorChartCell" align="center">
                        <html:img page="/resource/PerformanceChart?imageWidth=780&imageHeight=588&perfChartType=${mode}"
                                  border="0"/>
                     </td>
                  </tr>
                  <tr>
                     <td class="MonitorChartBlock">
                        <tiles:insert page="/resource/common/monitor/visibility/ChartTimeIntervalToolbar.jsp">
                           <tiles:put name="rangeNow" beanName="PerformanceForm" beanProperty="rangeNow"/>
                           <tiles:put name="begin"><c:out value="${PerformanceForm.rbDate.time}"/></tiles:put>
                           <tiles:put name="end"><c:out value="${PerformanceForm.reDate.time}"/></tiles:put>
                           <tiles:put name="prevProperty" value="prev"/>
                           <tiles:put name="nextProperty" value="next"/>
                        </tiles:insert>
                     </td>
                  </tr>

               </table>
            </c:when>

            <c:otherwise>
               <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                     <td class="BlockContent" width="100%">
                        <html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/>
                        <i><fmt:message key="resource.common.monitor.visibility.performance.NoMetricSelected"/></i>
                     </td>
                  </tr>
                  <tr>
                     <td class="BlockBottomLine">
                        <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
                     </td>
                  </tr>
               </table>
            </c:otherwise>

         </c:choose>

      </td>
   </tr>
</table>
<!-- / -->
