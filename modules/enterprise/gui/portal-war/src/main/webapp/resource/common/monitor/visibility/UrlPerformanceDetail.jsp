<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="url" ignore="true"/>
<tiles:importAttribute name="useChart" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                 symbol="MODE_MON_PERF" var="MODE_MON_PERF"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                 symbol="MODE_MON_CHART_SMSR"
                 var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                 symbol="SORTORDER_DEC" var="SORTORDER_DEC"/>

<hq:constant classname="net.hyperic.hq.rt.RtConstants" 
                 symbol="WEBSERVER" var="WEBSERVER"/>
<hq:constant classname="net.hyperic.hq.rt.RtConstants" 
                 symbol="APPSERVER" var="APPSERVER"/>
<hq:constant classname="net.hyperic.hq.rt.RtConstants" 
                 symbol="ENDUSER" var="ENDUSER"/>

<c:set var="tiers" value="${ENDUSER},${WEBSERVER},${APPSERVER}"/>

<%-- for v1, we don't show any charts --%>
<c:set var="useChart" value="false"/>

<%-- the usual sort order default is asc, but for this page we want it
  -- to be descending, so we have to set our own var --%>
<c:choose>
  <c:when test="${not empty param.so}">
    <c:set var="so" value="${param.so}"/>
  </c:when>
  <c:otherwise>
    <c:set var="so" value="${SORTORDER_DEC}"/>
  </c:otherwise>
</c:choose>

<c:url var="psAction" value="${selfAction}">
  <c:param name="so" value="${so}"/>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:param name="url" value="${url}"/>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:param name="so" value="${so}"/>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:param name="url" value="${url}"/>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="sAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:param name="url" value="${url}"/>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="iconPath" value="/images/icon_chart.gif"/>

<!-- CHILD RESOURCES CONTENTS -->
<div id="pudListDiv">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="pudListTable">
    <tr>
      <td class="ListCellHeader" colspan="7"><fmt:message key="resource.common.monitor.visibility.performance.DetailForResourcesEtc"></fmt:message></td>
    </tr>
  </table>

<c:forEach var="tier" items="${tiers}">
  <c:choose>
    <c:when test="${tier == WEBSERVER}">
      <fmt:message var="TypeTH" key="resource.common.monitor.visibility.VirtualHosts"/>
    </c:when>
    <c:when test="${tier == APPSERVER}">
      <fmt:message var="TypeTH" key="resource.common.monitor.visibility.WebApplications"/>
    </c:when>
    <c:when test="${tier == ENDUSER}">
      <fmt:message var="TypeTH" key="resource.common.monitor.visibility.EndUser"/>
    </c:when>
    <c:otherwise>
      <fmt:message var="TypeTH" key="resource.common.monitor.visibility.Unknown"/>
    </c:otherwise>
  </c:choose>

  <%-- ugly hack to get list, since ${summaries[tier]} doesn't seem
    -- to work? --%>
  <c:forEach var="s" items="${summaries}">
    <c:if test="${s.key == tier}">
      <c:set var="list" value="${s.value}"/>
    </c:if>
  </c:forEach>

  <c:choose>
    <c:when test="${empty first && not empty list}">
      <c:set var="first" value="${list}"/>
      <c:set var="colspan" value="2"/>
    </c:when>
    <c:otherwise>
      <c:set var="colspan" value="6"/>
    </c:otherwise>
  </c:choose>

  <c:if test="${not empty list}">
      <display:table items="${list}" var="summary" action="${sAction}" width="100%" cellspacing="0" cellpadding="0">
      <display:column width="34%" property="parent.name" href="/resource/service/monitor/Visibility.do?mode=${MODE_MON_PERF}&rid=${summary.parent.id}&type=${summary.parent.type}" title="${TypeTH}" isLocalizedTitle="false"/>
    <c:choose>
      <c:when test="${useChart}">
      <display:column width="1%" value="<img src=\"${iconPath}\" height=\"10\" width=\"10\" border=\"0\" alt=\"\">" href="/resource/common/monitor/Visibility.do?mode=${MODE_MON_CHART_SMSR}&rid=${rid}&type=${type}" paramId="url" paramName="summary" paramProperty="me.name" headerColspan="${colspan}" title="resource.common.monitor.visibility.URLsTH" sortAttr="24" styleClass="ListCell"/>
      <display:column width="33%" property="me.name" href="/resource/common/monitor/Visibility.do?mode=${MODE_MON_CHART_SMSR}&rid=${summary.parent.id}&type=${summary.parent.type}" paramId="url" paramName="summary" paramProperty="me.name"/>
      </c:when>
      <c:otherwise>
      <display:column width="34%" property="me.name" title="resource.common.monitor.visibility.URLsTH" sortAttr="24" styleClass="ListCell"/>
      </c:otherwise>
    </c:choose>
      <display:column width="8%" property="requestCount" title="resource.common.monitor.visibility.RequTH" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine"/>
      <display:column width="8%" property="low.total" title="resource.common.monitor.visibility.LowTH" sort="true" sortAttr="25" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
      <display:column width="8%" property="avg.total" title="resource.common.monitor.visibility.AvgTH" sort="true" defaultSort="true" sortAttr="26" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
      <display:column width="8%" property="peak.total" title="resource.common.monitor.visibility.PeakTH" sort="true" sortAttr="27" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
    </display:table>
  </c:if>

  <c:set var="list" value=""/>
</c:forEach>
</div>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listItems" beanName="first"/>
  <tiles:put name="listSize" beanName="first" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="1"/>
  <tiles:put name="noButtons" value="true"/>
</tiles:insert>
<!--  /  -->
