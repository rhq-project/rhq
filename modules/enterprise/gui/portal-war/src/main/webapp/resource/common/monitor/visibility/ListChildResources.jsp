<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="childResourcesHealthKey" ignore="true"/>
<tiles:importAttribute name="childResourcesTypeKey" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="internal" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
   <c:set var="mode" value="currentHealth"/>
</c:if>


<c:if test="${checkboxes}">
   <c:set var="widgetInstanceName" value="childResources"/>

   <script type="text/javascript">
      initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
      widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
   </script>
</c:if>


<c:if test="${count > 5}">
   <div class="scrollable">
</c:if>
<table width="100%" border="0" cellpadding="1" cellspacing="0" id="ResourceTable">
   <tr>
      <c:if test="${not empty summaries && checkboxes}">
         <td class="ListHeaderCheckbox" width="3%"><input type="checkbox"
                                                          onclick="ToggleAllGroup(this, widgetProperties, '<c:out value="${listMembersName}"/>')"
                                                          name="listToggleAll"></td>
      </c:if>

      <td class="ListHeader" width="100" align="left">
         <BLK>
            <fmt:message key="resource.common.monitor.visibility.ResourceNameTH"/>
         </BLK>
      </td>

      <c:if test="${not empty summaries}">
         <td class="ListHeaderInactive" width="15%" align="center" nowrap>
            <fmt:message key="resource.common.monitor.visibility.TotalNumTH"/>
         </td>
         <td class="ListHeaderInactive" width="15%" align="center" nowrap>
            <fmt:message key="resource.common.monitor.visibility.AVAILTH"/>
         </td>
         <td class="ListHeaderInactive" width="15%" align="center" nowrap>
            <fmt:message key="resource.common.monitor.visibility.ActionTH"/>
         </td>
         <td class="ListHeaderInactive" width="6%">
            <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
         </td>
      </c:if>
   </tr>




   <c:forEach var="summary" items="${summaries}">

      <c:if test="${fn:length(summary.composite.resources) != 1}">
      <c:url var="url" value="/resource/common/monitor/Visibility.do">
         <c:param name="mode" value="${mode}"/>
         <c:param name="parent" value="${Resource.id}"/>
         <c:param name="type" value="${summary.composite.resourceType.id}"/>
      </c:url>

      <tr class="ListRow">
         <c:if test="${checkboxes}">
            <td class="ListCellCheckbox">
               <html:multibox property="child" value="${summary.composite.resourceType.appdefTypeKey}"
                              styleClass="${listMembersName}"/>
            </td>
         </c:if>

         <td width="1%" class="ListCell" nowrap="true">
          <c:forEach begin="1" end="${summary.composite.depth}">&nbsp;&nbsp;&nbsp;</c:forEach>
          <c:choose>
            <c:when test="${!empty summary.composite.subcategory}">
            <html:img page="/images/icon_auto-group.gif" height="10" width="11" border="0" alt=""/> <%-- "mixed group, drawer symbol"  --%>
            </c:when>
            <c:when test="${!empty summary.composite.resourceType}">
            <html:img page="/images/icon_cluster.gif" height="10" width="11" border="0" alt=""/> <%-- TODO this is shown for an autogroup --%>
            </c:when>
            <c:otherwise>
            <html:img page="/images/icon_resource.gif" height="10" width="11" border="0" alt=""/>
            </c:otherwise>
          </c:choose>
<%--         </td>

         <td class="ListCell">--%>
            <c:choose>
               <c:when test="${empty summary.composite.resourceType}">
                  <c:out value="${summary.composite.subcategory.displayName}"/>
               </c:when>
               <c:otherwise>
                  <a href="<c:out value="${url}" />">
                     <c:out value="${summary.composite.resourceType.name}"/>
                  </a>
               </c:otherwise>
            </c:choose>
         </td>
         <td class="ListCellCheckbox">
            <c:out value="${summary.composite.memberCount}" default="0"/>
         </td>
         <td class="ListCellCheckbox">
            <c:choose>
               <c:when test="${summary.composite.availability == 0}">
                  <html:img page="/images/icon_available_red.png" border="0"
                            width="15" height="15"/>
               </c:when>
               <c:when test="${summary.composite.availability == 1}">
                  <html:img page="/images/icon_available_green.png" border="0"
                            width="15" height="15"/>
               </c:when>
               <c:when test="${summary.composite.availability < 1 && summary.composite.availability > 0}">
                  <html:img page="/images/icon_available_yellow.png" border="0"
                            width="15" height="15"/>
               </c:when>
               <c:otherwise>
                  <html:img page="/images/icon_available_grey.png" border="0"
                            width="15" height="15"/>
               </c:otherwise>
            </c:choose>
         </td>
         <!-- JS: action column -->
        <td class="ListCellCheckbox">
          <c:if test="${empty summary.composite.subcategory}">
           <div id="${summary.composite.resourceType.name}_menu" class="menu">
            <table>
               <tr>
                  <th><fmt:message key="resource.common.monitor.visibility.MeasurementTH"/></th>
                  <th><fmt:message key="resource.common.monitor.visibility.AlertTH"/></th>
                  <th><fmt:message key="resource.common.monitor.visibility.ChartTH"/></th>
               </tr>
               <c:forEach var="metricSummary" items="${summary.metricSummaries}">
                  <tr>
                     <td>${metricSummary.label}</td>
                     <td>${metricSummary.alertCount}</td>
                     <td>
                       <a href="javascript:menuLayers.hide();addMetric('ag,${metricSummary.parentId},${metricSummary.definitionId},${metricSummary.childTypeId}')">+</a>
                     </td>
                  </tr>
               </c:forEach>
            </table>
            </div> <%-- action popup  --%>
            <a href="<c:out value="${url}"/>">
               <html:img page="/images/icon_menu.gif" height="14" width="11"
                         onmouseover="menuLayers.show('${summary.composite.resourceType.name}_menu', event)"
                         onmouseout="menuLayers.hide()" border="0"/>
            </a>
          </c:if>
          <c:if test="${!empty summary.composite.subcategory}">
            <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
          </c:if>
         </td>
      </tr>
      </c:if>

      <%-- ***********  single resource case starts here  ********** --%>
      <c:if test="${fn:length(summary.composite.resources) == 1}">
         <c:forEach items="${summary.composite.resources}" var="res">
            <tr>

      <c:url var="url" value="/resource/common/monitor/Visibility.do">
         <c:param name="mode" value="${mode}"/>
         <c:param name="id" value="${res.resource.id}"/>
      </c:url>

      <tr class="
         <c:choose>
            <c:when test="${summary.composite.mainResource}">ListRowSelected</c:when>
            <c:otherwise>ListRow</c:otherwise>
         </c:choose>
         ">
         <c:if test="${checkboxes}">
            <td class="ListCellCheckbox"></td>
         </c:if>

         <td width="1%" class="ListCell" nowrap="true">
          <c:forEach begin="1" end="${summary.composite.depth}">&nbsp;&nbsp;&nbsp;</c:forEach>
            <html:img page="/images/icon_resource.gif" height="10" width="11" border="0" alt=""/>
            <a href="<c:out value="${url}" />">
               <c:out value="${res.resource.name}"/>
            </a>
         </td>
         <td class="ListCellCheckbox">1</td>
         <td class="ListCellCheckbox">
            <c:choose>
               <c:when test="${empty res.availability}">
                  <html:img page="/images/icon_available_grey.png" border="0"
                            width="15" height="15"/>                  
               </c:when>
               <c:when test="${res.availability == 'DOWN'}">
                  <html:img page="/images/icon_available_red.png" border="0"
                            width="15" height="15"/>
               </c:when>
               <c:when test="${res.availability == 'UP'}">
                  <html:img page="/images/icon_available_green.png" border="0"
                            width="15" height="15"/>
               </c:when>
            </c:choose>
         </td>
         <!-- JS: action column -->
         <td class="listCellCheckbox" align="center">
                  <%--<c:if test="${summary.composite.mainResource}">--%>
         <div id="${summary.composite.resourceType.name}_menu" class="menu">
         <table>
            <tr>
               <th><fmt:message key="resource.common.monitor.visibility.MeasurementTH"/></th>
               <th><fmt:message key="resource.common.monitor.visibility.AlertTH"/></th>
               <th><fmt:message key="resource.common.monitor.visibility.ChartTH"/></th>
            </tr>
            <c:forEach var="metricSummary" items="${summary.metricSummaries}">
               <tr>
                  <td>${metricSummary.label}</td>
                  <td>${metricSummary.alertCount}</td>
                  <td>
                     <c:choose>
                        <c:when test="${metricSummary.parentId > 0}">
                           <a href="javascript:menuLayers.hide();addMetric('ag,${metricSummary.parentId},${metricSummary.definitionId},${metricSummary.childTypeId}')">+</a>
                        </c:when>
                        <c:otherwise>
                           <a href="javascript:menuLayers.hide();addMetric('${summary.composite.resources[0].resource.id},${metricSummary.scheduleId}')">+</a>
                        </c:otherwise>
                     </c:choose>
                  </td>
                  
               </tr>
            </c:forEach>
         </table>
         </div> <%-- action popup  --%>
      <%--</c:if>--%>
<%--            <c:choose>
               <c:when test="${not empty summary.composite.resourceType.metricDefinitions}">
--%>               
                  <a href="<c:out value="${url}"/>">
                     <html:img page="/images/icon_menu.gif" height="14" width="11" alt=""
                         onmouseover="menuLayers.show('${summary.composite.resourceType.name}_menu', event)"
                         onmouseout="menuLayers.hide()" border="0"/>
                  </a>
<%--                  
               </c:when>
               <c:otherwise>&nbsp;</c:otherwise>
            </c:choose>
--%>            
         </td>
          </tr>
         </c:forEach>
      </c:if>

   </c:forEach>

</table>

<c:if test="${count > 5}">
   </div>
</c:if>
<!-- / -->

<c:if test="${empty summaries}">
   No child resources
</c:if>

