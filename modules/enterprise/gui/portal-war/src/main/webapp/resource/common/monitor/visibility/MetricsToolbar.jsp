<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="widgetInstanceName"/>
<tiles:importAttribute name="useChartButton" ignore="true"/>
<tiles:importAttribute name="useAddButton" ignore="true"/>
<tiles:importAttribute name="useRemoveButton" ignore="true"/>
<tiles:importAttribute name="useBaselinesButtons" ignore="true"/>
<tiles:importAttribute name="useCompareButton" ignore="true"/>
<tiles:importAttribute name="useCurrentButton" ignore="true"/>
<tiles:importAttribute name="useReloadButton" ignore="true"/>
<tiles:importAttribute name="usePager" ignore="true"/>
<tiles:importAttribute name="listItems" ignore="true"/>
<tiles:importAttribute name="listSize" ignore="true"/>
<tiles:importAttribute name="pageSizeParam" ignore="true"/>
<tiles:importAttribute name="pageSizeAction" ignore="true"/>
<tiles:importAttribute name="pageNumParam" ignore="true"/>
<tiles:importAttribute name="pageNumAction" ignore="true"/>
<tiles:importAttribute name="defaultSortColumn" ignore="true"/>

<c:if test="${empty useChartButton &&
              (useAddButton || useRemoveButton || useBaselinesButtons)}">
  <c:set var="useChartButton" value="true"/>
</c:if>

<!--  METRICS TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
<c:if test="${useChartButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>chartSelectedMetricsTd"><div id="<c:out value="${widgetInstanceName}"/>chartSelectedMetricsDiv"><html:img page="/images/tbb_chartselectedmetrics_gray.gif" border="0"/></div></td>
</c:if>
<c:if test="${useBaselinesButtons}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>setBaselinesTd"><div id="<c:out value="${widgetInstanceName}"/>setBaselinesDiv"><html:img page="/images/tbb_setBaselines_gray.gif" border="0"/></div></td>
    <!--
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>enableAutoBaselinesTd"><div id="<c:out value="${widgetInstanceName}"/>enableAutoBaselinesDiv"><html:img page="/images/tbb_enableAutoBaselines_gray.gif" border="0"/></div></td>
      -->
</c:if>
<c:if test="${useAddButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>addToFavoritesTd"><div id="<c:out value="${widgetInstanceName}"/>addToFavoritesDiv"><html:img page="/images/tbb_addToFavorites_gray.gif" border="0"/></div></td>
</c:if>
<c:if test="${useRemoveButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>removeFromFavoritesTd"><div id="<c:out value="${widgetInstanceName}"/>removeFromFavoritesDiv"><html:img page="/images/tbb_removeFromFavorites_gray.gif" border="0"/></div></td>
</c:if>
<c:if test="${useCompareButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>compareTd"><div id="<c:out value="${widgetInstanceName}"/>compareDiv"><html:img page="/images/tbb_compareMetricsOfSelected_gray.gif" border="0"/></div></td>
</c:if>
<c:if test="${useCurrentButton}">
    <td width="100%" align="right"><fmt:message key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
    <td><html:image property="current" page="/images/dash-button_go-arrow.gif" border="0"/></td>
</c:if>
<c:if test="${useReloadButton}">
    <td width="100%" align="right"><fmt:message key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
    <td><a href="javascript:location.reload();"><html:img page="/images/dash-button_go-arrow.gif" border="0"/></a></td>
</c:if>
    <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
<c:if test="${usePager}">
  <tiles:insert definition=".controls.paging">
     <tiles:put name="pageList" beanName="listItems"/>
     <tiles:put name="postfix" beanName="pageSizeParam"/>
     <tiles:put name="action" beanName="pageSizeAction"/>
  </tiles:insert>
</c:if>
  </tr>
</table>
<!--  /  -->
