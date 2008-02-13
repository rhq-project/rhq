<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.core.domain.util.PageOrdering" %>
<%@ page import="org.rhq.core.domain.util.OrderingField" %>
<%@ page import="java.util.List" %>
<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%-- 
   need to ignore runtime exceptions for 'id' and 'groupid'
   because this single tile is used for both contexts; thus,
   only one of them will ever be present at a time
 --%>
<tiles:importAttribute name="id" ignore="true"/>
<tiles:importAttribute name="groupId" ignore="true"/>

<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="buttonMode" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="useChart" ignore="true"/>
<tiles:importAttribute name="useChartMulti" ignore="true"/>
<tiles:importAttribute name="useCurrent" ignore="true"/>
<tiles:importAttribute name="useConfigure" ignore="true"/>
<tiles:importAttribute name="useCheckboxes" ignore="true"/>
<tiles:importAttribute name="favorites" ignore="true"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="mdsWidget" value="metricsDisplaySummary"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${mdsWidget}"/>');
mdsWidgetProps = getWidgetProperties('<c:out value="${mdsWidget}"/>');
</script>

<c:if test="${empty useChart}">
  <c:set var="useChart" value="true"/>
</c:if>
<%-- 
the group metrics page turns this off, since each metric is 
multi-resource backed 
--%>
<c:if test="${empty useChartMulti}">
  <c:set var="useChartMulti" value="true"/>
</c:if>

<%--
sometimes we don't want any left side buttons or checkboxes at all
--%>
<c:if test="${not empty buttonMode && buttonMode eq 'noleft'}">
  <c:set var="useChartMulti" value="false"/>
  <c:set var="useCheckboxes" value="false"/>
</c:if>
<c:if test="${empty buttonMode}">
  <c:set var="buttonMode" value="baselines"/>
</c:if>

<c:if test="${empty useCheckboxes}">
  <c:set var="useCheckboxes" value="true"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="MonitorBlock">
  <tr>
  <c:choose>
  <c:when test="${not empty useConfigure && not useConfigure}">
    <td width="100%">
  </c:when>
  <c:otherwise>
    <td width="50%" valign="middle">
      <fmt:message var="tmp" key="resource.common.monitor.visibility.OnlyMetricsShownForTimeRange"/>
      <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
         <tr>
            <td><c:out value="${tmp}"/></td>
         </tr>
      </table>
    </td>
<c:if test="${useCurrent}">
    <td>
    <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
      <tr>
        <td width="100%" align="right"><fmt:message key="resource.common.monitor.visibility.GetCurrentValuesLabel"/>
        <td><html:link href="javascript:location.reload()"><html:img page="/images/dash-button_go-arrow.gif" border="0"/></html:link>
        </td>
      </tr>
    </table>
    </td>
</c:if>
  </tr>
  <tr>
    <td colspan="3"/>
  </c:otherwise>
  </c:choose>

<c:choose>
  <c:when test="${not empty MetricSummaries}">

    <%
      List s = (List) pageContext.findAttribute("MetricSummaries");
      PageList l = new PageList(s, new PageControl(0,-1,new OrderingField("name", PageOrdering.ASC)));
      pageContext.setAttribute("MeasurementSummaryList",l);
	 %>
      
    <display:table items="${MeasurementSummaryList}" var="measurementSummary" onlyForProperty="valuesPresent">
      <%-- for now, checkboxes are only rendered for resources --%>
      <c:if test="${not empty id}">
        <display:column title="<input type=\"checkbox\" onclick=\"ToggleAllSelectionTwoButtons(this, mdsWidgetProps, 'availableListMember', '${buttonMode}');\" name=\"listToggleAll\">"
                        isLocalizedTitle="false" width="5%">
          <display:checkboxdecorator name="m" onclick="ToggleSelectionTwoButtons(this, mdsWidgetProps, 'availableListMember', '${buttonMode}')" styleClass="availableListMember"/>
        </display:column>
      </c:if>

      <%-- 
           SMSR chart for resources with 'id' context param;
           SMMR chart for resourceGroups with 'groupId' context param; 
       --%>
      <c:choose>
        <c:when test="${not empty id}">
          <display:column property="label" title="Name" isLocalizedTitle="false" width="30%"
                          href="/resource/common/monitor/Visibility.do?id=${Resource.id}&m=${measurementSummary.definitionId}&mode=chartSingleMetricSingleResource"/>
        </c:when>
        <c:when test="${not empty groupId}">
          <display:column property="label" title="Name" isLocalizedTitle="false" width="30%"
                          href="/resource/common/monitor/Visibility.do?groupId=${groupId}&m=${measurementSummary.definitionId}&mode=chartSingleMetricMultiResource"/>
        </c:when>
        <c:when test="${not empty param.type}">
          <display:column property="label" title="Name" isLocalizedTitle="false" width="30%"
                          href="/resource/common/monitor/Visibility.do?parent=${param.parent}&type=${param.type}&m=${measurementSummary.definitionId}&mode=chartSingleMetricMultiResource"/>
        </c:when>
        <c:otherwise>
          <td class="ListCell">
             <c:out value="Unsupported Display Type" />
          </td>
        </c:otherwise>
      </c:choose>

      <c:if test="${(not empty groupId) || (not empty param.type) }">
         <display:column property="numberCollecting" title="C.N.C." isLocalizedTitle="false" width="5%"/>
      </c:if>

      <display:column property="alertCount" title="Alerts" isLocalizedTitle="false" width="5%"/>
      <display:column property="oobCount" title="O.O.B." isLocalizedTitle="false" width="5%"/>
      <display:column property="minMetric" title="Low" isLocalizedTitle="false"/>
      <display:column property="avgMetric" title="Average" isLocalizedTitle="false"/>
      <display:column property="maxMetric" title="Peak" isLocalizedTitle="false"/>
      
      <c:choose>
        <c:when test="${not empty id}">
          <display:column property="lastMetric" title="Last" isLocalizedTitle="false"/>
          <td class="ListCellRight" width="%5" nowrap>
	         <c:out value="${metricDisplaySummary.metrics[last].valueFmt}"/>
          </td>
        </c:when>
<%-- TODO comment in again when we know how to calculate it JBNADM-2626>        
        <c:when test="${not empty groupId}">
          <display:column property="summaryMetric" title="Summary" isLocalizedTitle="false"/>
        </c:when>
--%>        
        <c:otherwise>
          <td class="ListCell">
             <c:out value="?" />
          </td>
        </c:otherwise>
      </c:choose>
    </display:table>
  </c:when>
  <c:otherwise>
    <tiles:insert definition=".resource.common.monitor.visibility.noMetrics">
      <c:if test="${not empty favorites}">
        <tiles:put name="favorites" beanName="favorites"/>
      </c:if>
    </tiles:insert>
  </c:otherwise>
</c:choose>

     <%-- This tile should probably be between the metrical and trait data --%>
 <tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
   <tiles:put name="widgetInstanceName" beanName="mdsWidget"/>
     <c:choose>
       <c:when test="${buttonMode eq 'add'}">
         <tiles:put name="useAddButton" value="true"/>
       </c:when>
       <c:when test="${buttonMode eq 'remove'}">
         <tiles:put name="useRemoveButton" value="true"/>
       </c:when>
       <c:when test="${buttonMode eq 'baselines'}">
         <tiles:put name="useBaselinesButtons" value="false"/>
         <tiles:put name="useAddButton" value="false"/>
         <tiles:put name="useRemoveButton" value="false"/>
       </c:when>
       <c:when test="${buttonMode eq 'noleft'}">
         <tiles:put name="useAddButton" value="false"/>
         <tiles:put name="useRemoveButton" value="false"/>
         <tiles:put name="useBaselinesButtons" value="false"/>
       </c:when>
     </c:choose>
     <c:if test="${not empty id}">
       <tiles:put name="useChartButton" beanName="useChartMulti"/>
     </c:if>

  </tiles:insert>  

      <%-- now the traits --%>
<c:choose>
   <c:when test="${not empty MetricSummariesTrait}">
      <% 
        List s = (List) pageContext.findAttribute("MetricSummariesTrait");
        PageList l = new PageList(s, new PageControl(0,-1,new OrderingField("name", PageOrdering.ASC)));
        pageContext.setAttribute("MeasurementSummaryListTrait",l);
      %>
      <display:table items="${MeasurementSummaryListTrait}" var="measurementSummary">
         <display:column property="label" title="Trait name" isLocalizedTitle="false"
                         href="/resource/common/monitor/Visibility.do?id=${Resource.id}&m=${measurementSummary.definitionId}&mode=showTraitHistory"
                         width="35%"/>

         <display:column property="alertCount" title="Alerts" 
                        isLocalizedTitle="false" width="5%"/>

        <display:column property="value" title="Value" isLocalizedTitle="false"/>
        <display:column title="Last changed"
            property="timestamp"
            width="20%"
            isLocalizedTitle="false" nowrap="true" >
            <display:datedecorator format="dd MMM yy, HH:mm:ss Z"/>
        </display:column>    
      </display:table>
   </c:when>
   <c:otherwise>
     <c:if test="${not empty id}">
       <tiles:insert definition=".resource.common.monitor.visibility.noMetrics">
         <tiles:put name="traits" value="true"/>
         <c:if test="${not empty favorites}">
           <tiles:put name="favorites" beanName="favorites"/>
         </c:if>
       </tiles:insert>
     </c:if>
   </c:otherwise>
</c:choose>

     




<c:if test="${useCheckboxes}">
<script type="text/javascript">
  clearIfAnyChecked('m');
</script>
</c:if>
