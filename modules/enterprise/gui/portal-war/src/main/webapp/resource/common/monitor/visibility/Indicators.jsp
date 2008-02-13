<%@ page import="org.rhq.enterprise.gui.legacy.WebUser"%>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils"%>
<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="tabListName" ignore="true"/>
<tiles:importAttribute name="entityType" ignore="true"/>

<c:set var="id" value="${Resource.id}"/>
<c:set var="ctype" value="${param.type}"/>
<c:set var="view" value="${IndicatorViewsForm.view}"/>

<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="0"/>
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="Resource" beanProperty="resourceType"/>
  <tiles:put name="autogroupResourceType" beanName="ctype"/>
  <tiles:put name="entityType" beanName="entityType"/>
  <tiles:put name="tabListName" beanName="tabListName"/>
</tiles:insert>

 <c:url var="indicatorUrl" value="/resource/common/monitor/visibility/AllIndicators.do">
  <c:param name="id" value="${Resource.id}" />
  <c:if test="${not empty type}">
    <c:param name="ctype" value="${type}" />
    <c:param name="type" value="${type}" />
  </c:if>

  <c:param name="view" value="${view}"/>
  <c:param name="entityType" value="${type}"/>
</c:url>

<script src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/prototype.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/popup.js"/>" type="text/javascript"></script>

<script language="JavaScript">
var baseUrl = "<html:rewrite page="/resource/common/monitor/visibility/AllIndicators.do"/>";
baseUrl += "?id=<c:out value="${id}"/>";
<c:if test="${not empty ctype}">
  baseUrl += "&ctype=<c:out value="${ctype}"/>";
</c:if>
<c:if test="${not empty groupId}">
  baseUrl += "&groupId=<c:out value="${groupId}"/>";
</c:if>
<c:if test="${not empty parent}">
  baseUrl += "&parent=<c:out value="${parent}"/>";
</c:if>

baseUrl +=  "&view=<c:out value="${view}"/>"

<c:choose>
   <c:when test="${ProblemMetricsDisplayForm.fresh}">
      var action = "fresh";
    </c:when>
    <c:otherwise>
      var action = "refresh";
    </c:otherwise>
  </c:choose>

function addMetric(metric) {
    refreshIndicators(metric);
}

function reviewAction(option) {
    var form = document.IndicatorViewsForm;
    if (option.value == 'go') {
        form.view.disabled = false;
        form.view.value = option.text;
        form.submit();
    }
    else if (option.value == 'delete') {
        form.view.value = "";
    }
    else if (option.value == 'create') {
        form.view.disabled = false;
        return;
    }
    else if (option.value == 'update') {
        form.view.value = "<c:out value="${view}"/>";
    }
    form.view.disabled = true;
}

      function refreshIndicators(metric) {

          var indicatorDiv = document.getElementById("indicatorDiv");
          var xmlhttp = getXMLHttpRequest();
          var url = baseUrl; //'<c:out value="${indicatorUrl}" escapeXml="false"/>';
          if (metric != null) {
              url += "&action=add&addMetric=" + metric;
          } else {
              url += "&action=" + action;
          }
          xmlhttp.open('GET',url,true);
          xmlhttp.onreadystatechange=function()
            {
                if (xmlhttp.readyState==4) {
                    indicatorDiv.innerHTML = xmlhttp.responseText;
                }
            }
          xmlhttp.send(null);
          // Always use refresh after the first load
          action = "refresh";
      }

      function runRefresh() {
          refreshIndicators();
      }

      function registerRefresh() {
          <%
          WebUser u = SessionUtils.getWebUser(session);
          int refresh = u.getIntPref(WebUser.PREF_PAGE_REFRESH_PERIOD,0);
          if (refresh > 0)
            pageContext.setAttribute("refreshPeriod", String.valueOf(refresh));
          %>
          <c:if test="${not empty refreshPeriod}">
              setInterval(runRefresh,1000 * <c:out value='${refreshPeriod}'/>);
          </c:if>
      }

      onloads.push(refreshIndicators);
      onloads.push(registerRefresh);
      var eventsTime = 0;

  function initEventDetails() {
    ajaxEngine.registerRequest( 'getEventDetails', '<html:rewrite page="/resource/common/monitor/visibility/EventDetails.do"/>');
    ajaxEngine.registerAjaxElement('eventsSummary',document.getElementById('eventsSummary'));
//            ('eventsSummary');
  }


  function showEventsCallback() {
    var detail = $('eventsSummary');
    if (detail.innerHTML == "") {
      setTimeout("showEventsCallback()", 500);
    }
    else {
      var div = $('eventDetailTable');
      if (div.style.display == 'none')
        new Effect.Appear(div);
    }
  }

  function showEventsDetails(time, status) {
      initEventDetails();
    eventsTime = time;
    var detail = $('eventsSummary');
    detail.innerHTML = "";

    if (status != null)
      ajaxEngine.sendRequest( 'getEventDetails',
                              'id=<c:out value="${id}"/>',
                              'begin=' + time,
                              'status=' + status);
    else
      ajaxEngine.sendRequest( 'getEventDetails',
                              'id=<c:out value="${id}"/>',
                              'begin=' + time);
    showEventsCallback();
  }

  function hideEventDetail() {
    new Effect.Fade($('eventsSummary'));
  }

 var statusArr =
    new Array ("ALL", "ERR", "WRN", "INF", "DBG", "ALR", "CTL");

  function filterEventsDetails(status) {
    for (i = 0; i < statusArr.length; i++) {
      $(statusArr[i] + "EventsTab").className = "eventsTab";
    }
    $(status + "EventsTab").className = "eventsTabOn";

    if (status != statusArr[0])
      showEventsDetails(eventsTime, status);
    else
      showEventsDetails(eventsTime);
  }


</script>

<html:form action="/resource/common/monitor/visibility/IndicatorCharts.do" method="GET" onsubmit="this.view.disabled=false">
<input type="hidden" name="id" value="<c:out value="${id}"/>">
<c:if test="${not empty ctype}">
  <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"> <%-- TODO straighten that out JBNADM-2630--%>
  <input type="hidden" name="type" value="<c:out value="${ctype}"/>">
  <input type="hidden" name="parent" value="<c:out value="${parent}"/>">
</c:if>
<c:if test="${not empty groupId}">
  <input type="hidden" name="groupId" value="<c:out value="${groupId}"/>">
</c:if>

<table width="680" cellpadding="0" cellspacing="2" border="0">
  <tr>
    <td class="ListHeaderInactive" width="100%">CHARTS</td>
  </tr>
  <tr>
    <td valign="middle" align="right" width="100%">
      <table cellspacing="2">
        <tr>
          <td>
            <fmt:message key="Filter.ViewLabel"/>
            <html:select property="action" onchange="reviewAction(this.options[this.selectedIndex]);">
              <option value="update">
                <fmt:message key="resource.common.monitor.visibility.view.Update"/>
                <c:out value="${view}"/>
              </option>
              <option value="create">
                <fmt:message key="resource.common.monitor.visibility.view.New"/>
              </option>
              <c:if test="${not empty IndicatorViewsForm.views[1]}">
              <option value="delete">
                <fmt:message key="resource.common.monitor.visibility.view.Delete"/>
                <c:out value="${view}"/>
              </option>
              <option disabled="true">
                <fmt:message key="resource.common.monitor.visibility.view.Separator"/>
              </option>
              <option disabled="true">
                <fmt:message key="resource.common.monitor.visibility.view.Goto"/>
              </option>
              <c:forEach var="viewname" items="${IndicatorViewsForm.views}">
              <option value="go"><c:out value="${viewname}"/></option>
              </c:forEach>
              </c:if>
            </html:select>
            <fmt:message key="common.label.Name"/>
            <html:text size="20" property="view" disabled="true"/>
          </td>
          <td align="right">
            <!-- Use hidden input because IE doesn't pass value of of image -->
            <input type="hidden" name="update" value="<c:out value="${view}"/>">
            <input type="image" name="submit" src="<html:rewrite page="/images/dash-button_go-arrow.gif"/>" 
               border="0" alt="Apply Chart View"/>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
  <td>
  <div style="padding-top: 1px; padding-bottom: 1px; z-index: 0; border:1px;" id="indicatorDiv">
    Loading...

  </div>
  <td>
  <tr>


</table>

</html:form>
