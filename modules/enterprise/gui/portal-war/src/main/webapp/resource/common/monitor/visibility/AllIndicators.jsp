<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<c:set var="id" value="${param.id}" scope="request"/>
<c:set var="ctype" value="${param.ctype}" scope="request"/>
<c:set var="parent" value="${param.parent}" scope="request"/>
<c:set var="entityType" value="${param.type}" scope="request"/> <!--  TODO fix -->
<c:set var="view" value="${param.view}" scope="request"/>
<c:set var="action" value="${param.action}" />
<c:set var="metric" value="${param.addMetric}" />
<c:set var="groupId" value="${param.groupId}" /> 

 <table width="680" border="0" cellpadding="2" bgcolor="#DBE3F5">
     <c:choose>
       <c:when test="${entityType eq 'autogroup'}">
         <tiles:insert definition=".resource.autogroup.monitor.visibility.Availability"/>
       </c:when>
       <c:otherwise>
         <tiles:insert definition=".resource.common.monitor.visibility.Availability"/>
       </c:otherwise>
     </c:choose>  
 <tr>
    <td width="680">
        <div id="charttop" style="padding-top: 1px; padding-bottom: 1px; z-index: 0;">
        <c:url var="chartUrl" value="/resource/common/monitor/visibility/IndicatorCharts.do">
          <c:if test="${not empty id}">
          <c:param name="id" value="${id}" />
          </c:if>
          <c:if test="${not empty groupId}">
          <c:param name="groupId" value="${groupId}" />
          </c:if>
          <c:if test="${not empty ctype}">
            <c:param name="ctype" value="${ctype}" />
          </c:if>
          <c:if test="${not empty parent}">
            <c:param name="parent" value="${parent}" />
          </c:if> 
          <c:param name="view" value="${view}"/>
          <c:if test="${not empty action}">
              <c:param name="action" value="${action}" />
          </c:if>         
          <c:if test="${action eq 'add'}">
            <c:param name="addMetric" value="${metric}"/>
          </c:if>  
        </c:url>
            <%-- TODO GH: Put this all in one request <tiles:insert page="${chartUrl}" controllerClass="org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.IndicatorChartsAction" />--%>
        <iframe id="chartFrame" src="<c:out value="${chartUrl}"/>"
                marginwidth="0" marginheight="0" frameborder="no"
                width="680px" height="450px"
                scrolling="auto"
                style="z-index: 0;border-style: none; height: 450px; width: 680px;active-scroll-bars {overflow-y:hidden};"></iframe>

      </div>
      <div id="chartbottom"></div>
    </td>
  </tr>
  <tr>
    <td>
      <tiles:insert definition=".resource.common.monitor.visibility.timeline">
        <c:if test="${entityType eq 'autogroup'}">
          <tiles:put name="hideLogs" value="true"/>
        </c:if>
      </tiles:insert>
    </td>
  </tr>
 
</table>
<%--
</body>
--%>
