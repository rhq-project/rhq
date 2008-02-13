<%@ page language="java" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="problems" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="hideTools" ignore="true"/>

<!-- Toobar -->
<c:if test="${not hideTools}">
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td><html:image page="/images/fb_addResourcesApply.gif" border="0" onmouseover="imageSwap(this, imagePath + 'fb_addResourcesApply', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_addResourcesApply', '');" onmousedown="imageSwap(this, imagePath +  'fb_addResourcesApply', '_down')"/>
    <html:hidden property="fresh" value="false"/>
    </td>
  </tr>
</table>
</c:if>

<c:choose>
  <c:when test="${not empty problems}">   
    <c:set var="id" value="${Resource.id}" />

    <c:forEach var="metric" items="${problems}" varStatus="status">
      <c:set var="resourceType" value="${metric.problemType}"/>
      <c:url var="metadataLink" value="/resource/common/monitor/Visibility.do">
        <c:param name="mode" value="metricMetadata"/>
        <c:param name="m" value="${metric.scheduleId}"/>
        <c:param name="id" value="${Resource.id}"/>
        <c:choose>
          <c:when test="${not empty ctype}">
            <c:param name="ctype" value="${ctype}"/>
          </c:when>
          <c:otherwise>
            <c:if test="${id != metric.resourceId}">
              <c:param name="ctype" value="${metric.resourceId}"/>
            </c:if>
          </c:otherwise>
        </c:choose>
      </c:url>

      <c:set var="metadataPopupHeight" value="326"/>

    <!-- Here are the menu layers. Give each a unique id and a class of menu -->
      <div id="metric_menu_<c:out value="${metric.scheduleId}"/>" class="menu">
        <ul>
          <li>
            <div class="BoldText">
            <%-- <c:choose>
              <c:when test="${metric.single}">--%>
                <fmt:message key="resource.common.monitor.visibility.problemMetric.Type">
                  <fmt:param value="${metric.problemType}"/>
                </fmt:message>
              <%--</c:when>
              <c:otherwise>
                <fmt:message key="resource.common.monitor.visibility.problemMetric.TypeCount">
                  <fmt:param value="${metric.problemType}"/>
                  <fmt:param value="${metric}"/>
                </fmt:message>
              </c:otherwise>
            </c:choose>--%>
          </div>
        <c:if test="${metric.startTime != null}">
  	      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.problemMetric.Began"/></div>
          <hq:dateFormatter value="${metric.startTime.time}"/></li>
        </c:if>
        <hr>
        <c:url var="chartLink" value="/resource/common/monitor/Visibility.do">
          <c:param name="m" value="${metric.scheduleId}"/>
          <%--<c:choose>
            <c:when test="${metric.single}">--%>
              <c:param name="mode" value="chartSingleMetricSingleResource"/>
              <c:param name="eid" value="${metric.resourceId}"/>
            <%--</c:when>
            <c:otherwise>
              <c:param name="mode" value="chartSingleMetricMultiResource"/>
              <c:param name="id" value="${Resource.id}"/>
              <c:param name="ctype" value="${metric.resourceId}"/>
            </c:otherwise>
          </c:choose>--%>
        </c:url>
       <%-- <c:choose>
          <c:when test="${metric.single}">--%>
            <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${metric.resourceId},${metric.scheduleId}')"/>
         <%-- </c:when>
          <c:otherwise>
            <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${resource.Id},${metric.scheduleId},${metric.appdefKey}')"/>
          </c:otherwise>
        </c:choose>--%>
        <li>
        <a href="<c:out value="${scriptUrl}"/>"><fmt:message key="resource.common.monitor.visibility.problemMetric.ChartMetric"/></a>
  	    <html:link href="${chartLink}"><fmt:message key="resource.common.monitor.visibility.problemMetric.FullChart"/></html:link>
        </li>
        <hr>
  	    <li><html:link href="" onclick="window.open('${metadataLink}','_metricMetadata','width=800,height=${metadataPopupHeight},scrollbars=yes,toolbar=no,left=80,top=80,resizable=yes'); return false;">
          <fmt:message key="resource.common.monitor.visibility.problemMetric.MetricData"/></html:link></li>
      </ul>
    </div>
    <c:set var="count" value="${status.count}"/>
  </c:forEach>

  <c:if test="${count > 7}">
    <div class="scrollable">
  </c:if>

  <table width="100%" border="0" cellpadding="1" cellspacing="0">
    <tr class="tableRowHeader">
      <th class="ListHeaderInactive">
        <html:select property="showType" onchange="document.ProblemMetricsDisplayForm.submit()">
          <html:option value="1"><fmt:message key="resource.common.monitor.visibility.MiniTab.Problems"/></html:option>
          <html:option value="2"><fmt:message key="resource.common.monitor.visibility.MiniTab.All"/></html:option>
        </html:select>
      </th>
      <th class="ListHeaderInactive" width="15%"><fmt:message key="resource.common.monitor.visibility.MiniTab.OOB"/></th>
      <th class="ListHeaderInactive" width="15%"><fmt:message key="resource.common.monitor.visibility.MiniTab.Alerts"/></th>
      <th class="ListHeaderInactive" width="4%"><fmt:message key="resource.common.monitor.visibility.MiniTab.More"/></th>
    </tr>
  <c:forEach var="metric" items="${problems}">
    <%--<c:choose>
      <c:when test="${metric.single}">--%>
        <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${metric.resourceId},${metric.scheduleId}');menuLayers.hide()"/>
     <%-- </c:when>
    <c:otherwise>
      <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${eid},${metric.scheduleId},${metric.resourceId}')"/>
    </c:otherwise>
    </c:choose>--%>

    <c:if test="${resourceType != metric.problemType}">
      <c:set var="resourceType" value="${metric.problemType}"/>
      <tr>
        <td class="ListCellSelected" colspan="4"><c:out value="${resourceType}"/></td>
      </tr>
    </c:if>

    <tr>
      <td class="ListCell"><c:out value="${metric.name}"/></td>
      <td class="ListCell" align="center"><c:out value="${metric.oobCount}"/></td>
      <td class="ListCell" align="center"><c:out value="${metric.alertCount}"/></td>
      <td class="ListCell">
      <a href="<c:out value="${scriptUrl}"/>"><html:img page="/images/icon_menu.gif" onmouseover="menuLayers.show('metric_menu_${metric.scheduleId}', event)" onmouseout="menuLayers.hide()" border="0"/></a>
      </td>
    </tr>
  </c:forEach>
  </table>
  <c:if test="${count > 7}">
    </div>
  </c:if>
  </c:when>
  <c:otherwise>
    <table class="table" width="100%" border="0" cellspacing="0" cellpadding="1">
      <tr class="tableRowHeader">
        <th class="ListHeaderInactive">
          <html:select property="showType" onchange="document.ProblemMetricsDisplayForm.submit()">
            <html:option value="1"><fmt:message key="resource.common.monitor.visibility.MiniTab.Problems"/></html:option>
            <html:option value="2"><fmt:message key="resource.common.monitor.visibility.MiniTab.All"/></html:option>
          </html:select>
        </th>
      </tr>
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="resource.common.monitor.visibility.no.problems.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>

