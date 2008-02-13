<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_SMSR"
             var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_SMMR"
             var="MODE_MON_CHART_SMMR"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
             symbol="MODE_MON_CHART_MMSR"
             var="MODE_MON_CHART_MMSR"/>

<c:if test="${not empty availabilityMetrics}">
   <tr>
      <td><font class="BoldText">
         <fmt:message key="resource.common.monitor.visibility.availability.value">
            <fmt:param value="${availMetricsAttr}"/>
         </fmt:message>
      </font>
         <font class="FooterSmall">
            <fmt:message key="resource.common.monitor.visibility.availability.timeframeShown"/>
         </font>
      </td>
   </tr>
   <tr>
      <td>
         <table cellpadding="0" cellspacing="0" border="0">
            <tr>
               <td width="8">
                  <html:img page="/images/timeline_ul.gif" height="10"/>
               </td>
               <c:forEach var="avail" items="${availabilityMetrics}" varStatus="status">
                  <td width="9">
                     <div onmousedown="overlay.moveOverlay(this);overlay.showTimePopup(<c:out value="${status.count - 1}"/>,'<hq:dateFormatter value="${avail.timestamp}"/>')">
                        <c:choose>
                           <c:when test="${avail.value == 1}">
                              <html:img page="/images/timeline_green.gif" height="10" width="9" border="0" title="UP"/>
                           </c:when>
                           <c:when test="${avail.value == 0 && avail.known}">
                              <html:img page="/images/timeline_red.gif" height="10" width="9" border="0" title="DOWN"/>
                           </c:when>
                           <c:otherwise>
                              <html:img page="/images/timeline_unknown.gif" height="10" width="9" border="0"
                                        title="UNKNOWN"/>
                           </c:otherwise>
                        </c:choose>
                     </div>
                  </td>
               </c:forEach>
               <td width="10" align="left">
                  <html:img page="/images/timeline_ur.gif" height="10"/>
               </td>
            </tr>
         </table>
      </td>
   </tr>
</c:if>