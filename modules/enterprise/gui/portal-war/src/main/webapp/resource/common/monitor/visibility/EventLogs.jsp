<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%
// no easy way to get the length of an array using JSTL 1.0
Object[] beans = (Object[]) request.getAttribute("timeIntervals");
int timeIntervalsLength = 0;
if (beans != null) {
  timeIntervalsLength = beans.length;
}
%>

  <tr>
    <td colspan="<%= timeIntervalsLength + 2%>" style="height: 2px;"></td>
  </tr>
  <tr style="height: 12px;">
    <td></td>
    <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
      <c:set var="count" value="${status.count}"/>
        <c:set var="icon" value="/images/no_event.gif"/>
        <c:if test="${not empty timeTick.severity}">
        <c:set var="sev" value="${timeTick.severity.ordinal}"/>
        <c:choose>
          <c:when test="${sev == 0 }">
            <c:set var="icon" value="/images/event_debug.gif"/>
          </c:when>
          <c:when test="${sev == 1 }">
            <c:set var="icon" value="/images/event_info.gif"/>
          </c:when>
          <c:when test="${sev == 2 }">
            <c:set var="icon" value="/images/event_warn.gif"/>
          </c:when>
          <c:when test="${sev == 3 }">
            <c:set var="icon" value="/images/event_error.gif"/>
           </c:when>
           <c:when test="${sev == 4 }">
            <c:set var="icon" value="/images/event_fatal.gif"/>
           </c:when>
           <c:otherwise>
            <c:set var="icon" value="/images/no_event.gif"/>
           </c:otherwise>
        </c:choose>
       </c:if>
      <td background="<c:out value="${icon}"/>" align="center" valign="middle">
      
      <c:if test="${not empty timeTick.severity}">
        <div class="eventBlock" 
         onmousedown="overlay.delayTimePopup(<c:out value="${count - 1}"/>,'<hq:dateFormatter value="${timeTick.time}"/>');showEventsDetails(<c:out value="${timeTick.time}"/>);overlay.moveOverlay(this)"></div>
      </c:if>
    </td>
    </c:forEach>
    <td align="right"><fmt:message key="resource.common.monitor.label.elc"/></td>
  </tr>
  <tr>
    <td colspan="<c:out value="${count + 2}"/>" style="height: 3px;"></td>
  </tr>

