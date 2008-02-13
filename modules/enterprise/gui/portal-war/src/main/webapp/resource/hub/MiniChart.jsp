<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="eid"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="chartLink" ignore="true"/>

<c:choose>
   <c:when test="${not empty summaries}">
      <table border="0" cellpadding="0" cellspacing="2" bgcolor="#CCCCCC">
         <tr>
            <logic:iterate id="template" name="summaries">
               <c:choose>
                  <c:when test="${template.category.name eq 'AVAILABILITY'}">
                     <c:set var="url" value="/resource/AvailHealthChart"/>
                  </c:when>
                  <c:when test="${template.category.name eq 'UTILIZATION'}">
                     <c:set var="url" value="/resource/UtilizationHealthChart"/>
                  </c:when>
                  <c:otherwise>
                     <c:set var="url" value="/resource/UsageHealthChart"/>
                  </c:otherwise>
               </c:choose>

               <c:url var="healthChartUrl" value="${url}">
                  <c:param name="eid" value="${eid}"/>
                  <c:param name="tid" value="${template.id}"/>
               </c:url>

               <c:if test="${chartLink}">
                  <c:url var="healthChartLink"
                         value="/resource/common/monitor/Visibility.do">
                     <c:param name="eid" value="${eid}"/>
                     <c:param name="mode" value="chartSingleMetricSingleResource"/>
                     <c:param name="m" value="${template.id}"/>
                  </c:url>
               </c:if>

               <td>
                  <table border="0" cellpadding="0" cellspacing="0">
                     <tr>
                        <td class="MiniChartTitle">
                           <c:out value="${template.name}"/>
                        </td>
                     </tr>
                     <tr>
                        <td>
                           <c:choose>
                              <c:when test="${chartLink}">
                                 <a href="<c:out value="${healthChartLink}"/>"><img
                                       src="<c:out value="${healthChartUrl}"/>" width="200" height="100" border="0"></a>
                              </c:when>
                              <c:otherwise>
                                 <img src="<c:out value="${healthChartUrl}"/>" width="200" height="100">
                              </c:otherwise>
                           </c:choose>
                        </td>
                     </tr>
                  </table>
               </td>
            </logic:iterate>
            <c:if test="${perfSupported}">
               <td>
                  <table border="0" cellpadding="0" cellspacing="0">
                     <tr>
                        <td class="MiniChartTitle">
                           <fmt:message key="resource.common.monitor.visibility.PerformanceTH"/>
                        </td>
                     </tr>
                     <tr>
                        <td>
                           <html:img page="/resource/PerfHealthChart?eid=${eid}" width="200" height="100"/>
                        </td>
                     </tr>
                  </table>
               </td>
            </c:if>
         </tr>
      </table>
   </c:when>
   <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.NoMetricsEtc"/>
   </c:otherwise>
</c:choose>

