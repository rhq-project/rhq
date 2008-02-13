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
    <td background="<html:rewrite page="/images/no_event.gif"/>" align="center" valign="middle">
      <c:if test="${timeTick.events > 0}">
      <div class="eventBlock" onmouseover="this.style.backgroundColor='#0000ff'" onmouseout="this.style.backgroundColor='#003399'" onmousedown="overlay.delayTimePopup(<c:out value="${count - 1}"/>,'<hq:dateFormatter value="${timeTick.time}"/>');showEventsDetails(<c:out value="${timeTick.time}"/>);overlay.moveOverlay(this)"></div>
      </c:if>
    </td>
    </c:forEach>
    <td align="right"><fmt:message key="resource.common.monitor.label.elc"/></td>
  </tr>
  <tr>
    <td colspan="<c:out value="${count + 2}"/>" style="height: 3px;"></td>
  </tr>

