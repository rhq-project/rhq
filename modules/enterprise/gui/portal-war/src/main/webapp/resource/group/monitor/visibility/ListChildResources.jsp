<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="childResourcesTypeKey"/>

<c:if test="${empty mode}">
   <c:set var="mode" value="currentHealth"/>
</c:if>

<hq:constant classname="org.rhq.enterprise.gui.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_AUTOGROUP" var="AUTOGROUP" />
<hq:constant classname="org.rhq.enterprise.gui.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_CLUSTER" var="CLUSTER" />
<hq:constant classname="org.rhq.enterprise.gui.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_SINGLETON" var="SINGLETON" />

<c:set var="useAvailStoplightDimensions" value="false" />
<c:if test="${useAvailStoplightDimensions}">
   <c:set var="availStoplightDimensions" value=" width=\"106\" " />
</c:if>

<c:choose>
   <c:when test="${not empty summaries.resources}">
      <c:forEach var="summary" items="${summaries.resources}">
         <div id="${summary.resource.name}_menu" class="menu">
         <table>
            <tr>
               <th><fmt:message key="resource.common.monitor.visibility.MeasurementTH"/></th>
               <th><fmt:message key="resource.common.monitor.visibility.AlertTH"/></th>
               <th><fmt:message key="resource.common.monitor.visibility.ChartTH"/></th>
            </tr>
            <c:forEach var="metricSummary" items="${summaries.metricSummaries[summary.resource.id]}">
               <tr>                  
                  <td>${metricSummary.label}</td>
                  <td>${metricSummary.alertCount}</td>
                  <td><a href="javascript:menuLayers.hide();addMetric('cg,${metricSummary.groupId},${metricSummary.definitionId}')">+</a></td>
               </tr>            
            </c:forEach>
         </table>
        </div>
   </c:forEach>

      <c:if test="${count > 5}">
         <div class="scrollable">
      </c:if>
      <display:table items="${summaries.resources}" var="summary" action="${psAction}" width="100%" cellspacing="0" cellpadding="0">
         <display:column width="1%" property="resource.id" title="&nbsp;" isLocalizedTitle="false" 
            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderInactive">
            <display:imagelinkdecorator src="/images/icon_resource.gif" border="0"/>
         </display:column>
         <display:column width="43%" property="resource.name" title="resource.service.monitor.visibility.MembersTab" 
            styleClass="ListCell" headerStyleClass="ListHeaderInactive" 
            href="/resource/common/monitor/Visibility.do?mode=${mode}&id=${summary.resource.id}"/>
            
            
            
            
            
         <display:column width="43%" property="parent.name" title="resource.service.monitor.visibility.ParentTab" 
            styleClass="ListCell" headerStyleClass="ListHeaderInactive" 
            href="/resource/common/monitor/Visibility.do?mode=${mode}&id=${summary.parent.id}"/>
            
            
            
            
         <display:column width="7%" property="availability" 
            title="resource.common.monitor.visibility.AVAILTH" 
            styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderInactive" align="center">
            <display:availabilitydecorator/> <!--  value="${summary.availability}"/> -->
         </display:column>
         <display:column width="4%" property="resource.id" 
               title="resource.common.monitor.visibility.MiniTab.More" 
               headerStyleClass="ListHeaderInactive" 
               styleClass="ListCellCheckbox" 
               href="/resource/common/monitor/Visibility.do?mode=${mode}&id=${summary.resource.id}">
            <display:imagedecorator onmouseover="menuLayers.show('${summary.resource.name}_menu', event)" 
                                    onmouseout="menuLayers.hide()" 
                                    src="/images/icon_menu_up.gif" border="0"/>
         </display:column>
      </display:table>

      <c:if test="${count > 5}">
         </div>
      </c:if>

   </c:when>
   <c:otherwise>
      <tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
   </c:otherwise>
</c:choose>

