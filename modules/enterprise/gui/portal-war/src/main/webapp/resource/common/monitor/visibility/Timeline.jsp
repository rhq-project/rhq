<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="hideLogs" ignore="true"/>



<div id="overlay" class="overlay"></div>

<table cellpadding="0" cellspacing="0" border="0" width="100%">
  <%--
  <tr>
    <td class="ListHeaderInactive">Events</td>
  </tr>
  <tr>
    <td>

  --%>
  <c:if test="${not hideLogs}">
    <tiles:insert page="/resource/common/monitor/visibility/EventLogs.jsp"/>
  </c:if>
  <tr>
    <td width="10">
      <div id="timetop"></div>
      <html:img page="/images/timeline_ll.gif" height="10"/> 
    </td>
    <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
      <c:set var="count" value="${status.count}"/>
    <td width="9">
      <div id="timePopup_<c:out value="${count - 1}"/>" onmouseover="overlay.delayTimePopup(<c:out value="${count - 1}"/>,'<hq:dateFormatter value="${timeTick.time}"/>')" onmousedown="overlay.curTime = '<hq:dateFormatter value="${timeTick.time}"/>'; overlay.moveOverlay(this)" onmouseout="overlay.curTime = null">
      <html:img page="/images/timeline_off.gif" height="10" width="9" onmouseover="imageSwap(this, imagePath + 'timeline', '_on')" onmouseout="imageSwap(this, imagePath +  'timeline', '_off');" onmousedown="imageSwap(this, imagePath +  'timeline', '_down')"/> 
      </div>
    </td>
    </c:forEach>
    <td width="100%">
      <html:img page="/images/timeline_lr.gif" height="10"/> 
    </td>
  </tr>
  <tr>
    <td></td>
    <td colspan="<c:out value="${count / 2}"/>" valign="top">
      <hq:dateFormatter value="${timeIntervals[0].time}"/>
      <div id="timePopup" class="timepopup" onmousedown="overlay.hideTimePopup()"></div>
      <div style="height: 16px;"></b>
    </td>
    <td colspan="<c:out value="${count / 2}"/>" align="right" valign="top">
      <hq:dateFormatter value="${timeIntervals[count - 1].time}"/>
    </td>
    <td></td>
  </tr>
  <tr>
    <td colspan="<c:out value="${count + 2}"/>" valign="top">
      <a name="eventDetail"></a>
      <div id="eventDetailTable"
           style="position: relative; height: 230px; display: none; ">
      <div class="eventDetails">
      <table cellspacing="0" width="100%">
        <tr>
          <td id="ALLEventsTab" width="10%" class="eventsTabOn" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('ALL')" class="black"><fmt:message key="resource.common.monitor.label.events.All"/></a>
          </td>
          <td id="ERREventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('ERR')" class="red"><fmt:message key="resource.common.monitor.label.events.Error"/></a>
          </td>
          <td id="WRNEventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('WRN')" class="yellow"><fmt:message key="resource.common.monitor.label.events.Warn"/></a>
          </td>
          <td id="INFEventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('INF')" class="green"><fmt:message key="resource.common.monitor.label.events.Info"/></a>
          </td>
          <td id="DBGEventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('DBG')" class="green"><fmt:message key="resource.common.monitor.label.events.Debug"/></a>
          </td>
          <td id="ALREventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('ALR')" class="red"><fmt:message key="resource.common.monitor.label.events.Alert"/></a>
          </td>
          <td id="CTLEventsTab" width="10%" class="eventsTab" nowrap="true">
            <a href="#eventDetail" onclick="filterEventsDetails('CTL')" class="navy"><fmt:message key="resource.common.monitor.label.events.Control"/></a>
          </td>
          <td valign="top" style="text-align: right; border-bottom: solid; border-width: 1px; border-color: #000000;">
            <html:img page="/images/dash-icon_delete.gif"
                      onclick="new Effect.Fade($('eventDetailTable'))"/>
          </td>
        </tr>
        <tr>
          <td colspan="8"> 
            <div id="eventsSummary"  style="height:200px; overflow:auto;">Timeline here</div>
          </td>
        </tr>
      </table>
      </div>
      </div>
    </td>
  </tr>
<%--
</table>
</td>
</tr>
--%>
</table>

