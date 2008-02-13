<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="id" ignore="true"/>
<tiles:importAttribute name="groupId" ignore="true"/>
<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="view" ignore="true"/>
<tiles:importAttribute name="form" ignore="true"/>

<c:if test="${not empty id}">
   <input type="hidden" name="id" value="${id}"/>
</c:if>
<c:if test="${not empty groupId}">
   <input type="hidden" name="groupId" value="${groupId}"/>
</c:if>
<c:if test="${not empty ctype}">
   <input type="hidden" name="type" value="${ctype}"/>
</c:if>
<c:if test="${not empty type}">
   <input type="hidden" name="type" value="${type}"/>
</c:if>
<c:if test="${not empty mode}">
  <input type="hidden" name="mode" value="<c:out value="${mode}"/>"/>
</c:if>
<c:if test="${not empty view}">
  <input type="hidden" name="view" value="<c:out value="${view}"/>"/>
</c:if>

<c:choose>
  <c:when test="${not empty form}">
    <%-- used only for forms that are not MetricsDisplayForm --%>
    <tiles:importAttribute name="formName"/>
    <c:set var="readOnly" value="${form.readOnly}"/>
    <c:set var="rangeBegin" value="${form.rb}"/>
    <c:set var="rangeEnd" value="${form.re}"/>
    <c:set var="showBaseline" value="false"/>
  </c:when>
  <c:otherwise>
    <c:set var="formName" value="MetricsDisplayForm"/>
    <c:set var="readOnly" value="${MetricsDisplayForm.readOnly}"/>
    <c:set var="rangeBegin" value="${MetricsDisplayForm.rb}"/>
    <c:set var="rangeEnd" value="${MetricsDisplayForm.re}"/>
    <c:set var="highlighted" value="${MetricsDisplayForm.h}"/>
    <c:set var="showBaseline" value="${MetricsDisplayForm.showBaseline}"/>
  </c:otherwise>
</c:choose>

<hq:dateFormatter var="rb" value="${rangeBegin}"/>
<hq:dateFormatter var="re" value="${rangeEnd}"/>

<!-- Table Content -->
<table width="100%" cellspacing="0" border="0" class="MonitorToolbar">
<c:if test="${showBaseline}">
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.HighlightMetricsLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td>
            <html:select styleClass="FilterFormText" property="hv">
              <html:option value=""/>
              <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.LowValue"/>
              <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.AverageValue"/>
              <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.PeakValue"/>
              <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.LastValue"/>
            </html:select>
          </td>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.is"/></td>
          <td nowrap>
            <html:select styleClass="FilterFormText" property="hp">
              <html:option value=""/>
              <html:option value="5" key="resource.common.monitor.visibility.metricsToolbar.5%"/>
              <html:option value="10" key="resource.common.monitor.visibility.metricsToolbar.10%"/>
              <html:option value="20" key="resource.common.monitor.visibility.metricsToolbar.20%"/>
              <html:option value="30" key="resource.common.monitor.visibility.metricsToolbar.30%"/>
              <html:option value="40" key="resource.common.monitor.visibility.metricsToolbar.40%"/>
              <html:option value="50" key="resource.common.monitor.visibility.metricsToolbar.50%"/>
              <html:option value="60" key="resource.common.monitor.visibility.metricsToolbar.60%"/>
              <html:option value="70" key="resource.common.monitor.visibility.metricsToolbar.70%"/>
              <html:option value="80" key="resource.common.monitor.visibility.metricsToolbar.80%"/>
              <html:option value="90" key="resource.common.monitor.visibility.metricsToolbar.90%"/>
              <html:option value="100" key="resource.common.monitor.visibility.metricsToolbar.100%"/>
            </html:select> 
            <html:select styleClass="FilterFormText" property="ht">
              <html:option value=""/>
              <hq:optionMessageList property="highlightThresholdMenu" baseKey="resource.common.monitor.visibility.metricsToolbar" filter="true"/>
            </html:select>
          </td>
          <td><html:image property="highlight" page="/images/dash-button_go-arrow.gif" border="0"/></td>
<c:choose>
  <c:when test="${highlighted}">
          <td width="100%"><html:link href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'clear')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.ClearHighlightingBtn"/></html:link></td>
  </c:when>
  <c:otherwise>
          <td width="100%">&nbsp;</td>
  </c:otherwise>
</c:choose>
        </tr>
      </table>
    </td>
  </tr>
</c:if>
<c:choose>
  <c:when test="${readOnly}">
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.MetricDisplayRangeLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td width="100%"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.DateRange"><fmt:param value="${rb}"/><fmt:param value="${re}"/></fmt:message> <html:link href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'editRange', 'GET')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn"/></html:link> | <html:link href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'simple')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.SwitchToSimpleBtn"/></html:link></td>
          <td width="100%"><html:img page="/images/spacer.gif" width="1" height="21" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.MetricDisplayRangeLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.Last"/></td>
          <td nowrap>
            <html:select styleClass="FilterFormText" property="rn">
              <html:optionsCollection property="rnMenu"/>
            </html:select> 
            <html:select styleClass="FilterFormText" property="ru">
<!--
              <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.CollectionPoints"/>
-->
              <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.Minutes"/>
              <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.Hours"/>
              <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.Days"/>
            </html:select>
          </td>
          <td><html:image property="range" page="/images/dash-button_go-arrow.gif" border="0"/></td>
          <td width="100%"><html:link href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'advanced')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.AdvancedSettingsBtn"/></html:link></td>
        </tr>
      </table>
    </td>
  </tr>
  </c:otherwise>
</c:choose>
</table>
<!--  /  -->
