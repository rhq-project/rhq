<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.categories"/></td>
  </tr>
  <tr> 
    <td class="ListCell" width="50%" nowrap="true"> 
      <html:multibox property="filter" value="0"/>
      <fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></td>
    <td class="ListCell" width="50%" nowrap="true"> 
      <html:multibox property="filter" value="1"/>
      <fmt:message key="resource.common.monitor.visibility.UtilizationTH"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="2"/>
      <fmt:message key="resource.common.monitor.visibility.UsageTH"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="3"/>
      <fmt:message key="resource.common.monitor.visibility.PerformanceTH"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.valueTypes"/></td>
  </tr>
  <tr> 
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="4"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.dynamic"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="5"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsup"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="6"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsdown"/></td>
    <td class="ListCell" nowrap="true"> 
      <html:multibox property="filter" value="7"/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.static"/></td>
  </tr>
</table>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td class="ListCell" align="right"> 
      <fmt:message key="resource.hub.search.KeywordSearchLabel"/>
      <html:text property="keyword" size="20"/>
    </td>
    <td class="ListCell" align="left">
      <html:image property="filterSubmit" page="/images/dash-button_go-arrow.gif" border="0"/>
    </td>
  </tr>
</table>
