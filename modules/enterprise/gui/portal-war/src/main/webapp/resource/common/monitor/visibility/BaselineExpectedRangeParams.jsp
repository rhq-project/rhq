<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
<tiles:put name="tabKey"
value="resource.common.monitor.visibility.chart.MetricBaselineAndExpectedRangeTab"/>
</tiles:insert>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <!-- baseline -->
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.BaselineLabel"/></td>
    <td class="BlockContent">
      <c:choose>
      <c:when test="${editBaseline}">
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.OldValue"/>
      <span class="MonitorMetricsValue">
      <c:choose>
      <c:when test="${not empty ViewChartForm.baseline}">
      <c:out value="${ViewChartForm.baseline}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NotYetSet"/>
      </c:otherwise>
      </c:choose>
      </span>
    </td>
  </tr>
  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <td class="BlockContent">
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NewValue"/>
      <span class="MonitorMetricsValue">
      <c:choose>
      <c:when test="${not empty ViewChartForm.newBaseline}">
      <c:out value="${ViewChartForm.newBaseline}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NotYetSet"/>
      </c:otherwise>
      </c:choose>
      </span>
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'saveBaseline')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.SaveValueLink"/></html:link>
      | <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'cancelBaseline')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.CancelLink"/></html:link>
      </c:when>
      <c:otherwise>
      <span class="MonitorMetricsValue">
      <c:choose>
      <c:when test="${not empty ViewChartForm.baseline}">
      <c:out value="${ViewChartForm.baseline}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NotYetSet"/>
      </c:otherwise>
      </c:choose>
      </span>
      <c:if test="${not empty canManageMeasurements}">
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'changeBaseline')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.ChangeValueLink"/></html:link>
      </c:if>
      </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <!-- high range -->
  <tr>
    <td class="BlockLabel"><fmt:message key="resource.common.monitor.visibility.chart.baseline.ExpectedRangeLabel"/></td>
    <logic:messagesPresent property="highRange">
    <td class="ErrorField">
    </logic:messagesPresent>
    <logic:messagesNotPresent property="highRange">
    <td class="BlockContent">
    </logic:messagesNotPresent>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.HighRangeLabel"/>
      <span class="MonitorMetricsValue">
      <c:choose>
      <c:when test="${editHighRange}">
      <html:text property="highRange" size="15"/>
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'saveHighRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.SaveValueLink"/></html:link>
      | <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'cancelHighRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.CancelLink"/></html:link>
      <logic:messagesPresent property="highRange">
      <br>-- <html:errors property="highRange"/>
      </logic:messagesPresent>
      </c:when>
      <c:otherwise>
      <c:choose>
      <c:when test="${not empty ViewChartForm.highRange}">
      <c:out value="${ViewChartForm.highRange}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NotYetSet"/>
      </c:otherwise>
      </c:choose>
      <c:if test="${not empty canManageMeasurements}">
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'changeHighRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.ChangeValueLink"/></html:link>
      </c:if>
      </c:otherwise>
      </c:choose>
      </span>
    </td>
  </tr>
  <!-- low range -->
  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <logic:messagesPresent property="lowRange">
    <td class="ErrorField">
    </logic:messagesPresent>
    <logic:messagesNotPresent property="lowRange">
    <td class="BlockContent">
    </logic:messagesNotPresent>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.LowRangeLabel"/>
      <span class="MonitorMetricsValue">
      <c:choose>
      <c:when test="${editLowRange}">
      <html:text property="lowRange" size="15"/>
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'saveLowRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.SaveValueLink"/></html:link>
      | <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'cancelLowRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.CancelLink"/></html:link>
      <logic:messagesPresent property="lowRange">
      <br>-- <html:errors property="lowRange"/>
      </logic:messagesPresent>
      </c:when>
      <c:otherwise>
      <c:choose>
      <c:when test="${not empty ViewChartForm.lowRange}">
      <c:out value="${ViewChartForm.lowRange}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.chart.baseline.NotYetSet"/>
      </c:otherwise>
      </c:choose>
      <c:if test="${not empty canManageMeasurements}">
      - <html:link href="javascript:document.forms[0].submit();" onclick="clickLink('ViewChartForm', 'changeLowRange')"><fmt:message
      key="resource.common.monitor.visibility.chart.baseline.ChangeValueLink"/></html:link>
      </c:if>
      </c:otherwise>
      </c:choose>
      </span>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
