<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="showHostPlatform" ignore="true"/>
<tiles:importAttribute name="errKey" ignore="true"/>
<tiles:importAttribute name="tabKey"/>
<tiles:importAttribute name="hostResourcesHealthKey"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<c:if test="${checkboxes}">
  <c:set var="widgetInstanceName" value="hostResources"/>
  <script type="text/javascript">
    initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
    widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
  </script>
</c:if>

<c:forEach var="summary" items="${summaries}" varStatus="status">
  <div id="<c:out value="${summary.resourceTypeId}_${summary.resourceId}_menu"/>" class="menu">
    <ul>
      <li><div class="BoldText"><fmt:message key="${hostResourcesHealthKey}"/> <fmt:message key="resource.common.monitor.visibility.TypeTH"/></div>
      <c:out value="${summary.resourceTypeName}"/>
      </li>
    <c:if test="${showHostPlatform}">
      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.HostPlatformTH"/></div>
      <html:link page="/resource/platform/monitor/Visibility.do?mode=${param['mode']}&eid=${summary.parentResourceTypeId}:${summary.parentResourceId}"><c:out value="${summary.parentResourceName}" default="PARENT RESOURCE NAME NOT SET"/></html:link>
    </c:if>
      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.USAGETH"/></div>
        <%--<hq:metric metric="${summary.throughput}" unit="none"  defaultKey="resource.common.monitor.visibility.performance.NotAvail" />--%>
      </li>
    </ul>
  </div>

  <c:set var="count" value="${status.count}"/>
</c:forEach>

<hq:pageSize var="pageSize"/>
<!--  HOST RESOURCES CONTENTS -->
<c:choose>
  <c:when test="${count > 5}">
    <div id="hostResourcesDiv" class="scrollable">
  </c:when>
  <c:otherwise>
    <div id="hostResourcesDiv">
  </c:otherwise>
</c:choose>
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr class="ListHeaderLight">
    <c:if test="${not empty summaries && checkboxes}">
    <td class="ListHeaderCheckbox" width="3%"><input type="checkbox" onclick="ToggleAllGroup(this, widgetProperties, '<c:out value="${listMembersName}"/>')" name="listToggleAll"></td>
    </c:if>

    <td width="100%" class="ListHeader"><BLK><fmt:message key="${tabKey}"/></BLK></td>
    <c:if test="${not empty summaries}">
    <td width="20%" class="ListHeaderCheckbox"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></td>
    <td class="ListHeaderInactive" width="6%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <!--
    <td width="8%" class="ListHeaderCheckbox"><fmt:message key="resource.common.monitor.visibility.PERFTH"/></td>
  -->
    </c:if>
  </tr>

    <c:forEach var="summary" items="${summaries}">
  <tr class="ListRow">
  <c:url var="url" value="/resource/${summary.resourceEntityTypeName}/monitor/Visibility.do?mode=${mode}&eid=${summary.resourceTypeId}:${summary.resourceId}"/>
    <c:if test="${checkboxes}">
    <td class="ListCellCheckbox" width="3%"><html:multibox property="host" value="${summary.resourceTypeId}:${summary.resourceId}" styleClass="${listMembersName}"/></td>
    </c:if>

    <td class="ListCell"><a href="<c:out value="${url}"/>"><c:out value="${summary.resourceName}"/></a></td>
    <td class="ListCellCheckbox">
      <html:img page="/resource/Availability?eid=${summary.resourceTypeId}:${summary.resourceId}" width="12" height="12" alt="" border="0"/>
    </td>
    <td class="ListCellCheckbox">
      <a href="<c:out value="${url}"/>"><html:img page="/images/icon_menu_down.gif" onmouseover="menuLayers.show('${summary.resourceTypeId}_${summary.resourceId}_menu', event)" onmouseout="menuLayers.hide()" border="0"/></a>
    </td>
  </tr>

    </c:forEach>
</table>

<c:if test="${empty summaries}">
  <c:if test="${empty errKey}">
    <c:set var="errKey" value="resource.common.monitor.visibility.NoHealthsEtc" />
  </c:if>
  <tiles:insert definition=".resource.common.monitor.visibility.HostHealthError">
    <tiles:put name="errKey" beanName="errKey" />
  </tiles:insert>
</c:if>
</div>
