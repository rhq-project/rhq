<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="widgetInstanceName"/>
<tiles:importAttribute name="useConfigureButton" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty useConfigureButton}">
  <c:set var="useConfigureButton" value="true"/>
</c:if>

<!--  METRICS GET CURRENT -->
<c:if test="${useConfigureButton}">

   <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
      <tr>
         <td width="100%" align="right" nowrap>&nbsp;
            <fmt:message key="resource.common.monitor.visibility.SeeMoreMetricsLabel"/></td>

         <c:choose>
            <c:when test="${not empty groupId}">
               <c:url var="link" value="/resource/group/monitor/Config.do">
                  <c:param name="mode" value="configure"/>
                  <c:param name="groupId" value="${groupId}"/>
                  <c:param name="category" value="${category}"/>
               </c:url>
            </c:when>
            <c:when test="${not empty param.type}">
               <c:url var="link" value="/resource/common/monitor/Config.do">
                  <c:param name="mode" value="configure"/>
                  <c:param name="type" value="${param.type}"/>
                  <c:param name="parent" value="${param.parent}"/>
               </c:url>
            </c:when>
            <c:otherwise>
               <c:url var="link" value="/resource/common/monitor/Config.do">
                  <c:param name="mode" value="configure"/>
                  <c:param name="id" value="${Resource.id}"/>
                  <c:if test="${not empty param.type}">
                     <c:param name="ctype" value="${param.type}"/>
                  </c:if>
               </c:url>
            </c:otherwise>
         </c:choose>
         <td><html:link href="${link}"><html:img page="/images/dash-button_go-arrow.gif" border="0"/></html:link>
         </td>
      </tr>
   </table>
</c:if>
<!--  /  -->
