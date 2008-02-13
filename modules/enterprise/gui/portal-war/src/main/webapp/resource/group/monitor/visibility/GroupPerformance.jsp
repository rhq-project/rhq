<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="entityId"/>
<tiles:importAttribute name="entityType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty entityType}">
  <c:set var="entityType" value="group"/>
</c:if>

<c:set var="subTabUrl" value="/resource/${entityType}/monitor/Visibility.do?mode=performance"/>
<c:choose>
<c:when test="${not empty ctype}">
  <c:set var="selfAction" value="${subTabUrl}&eid=${entityId}&ctype=${ctype}"/>
</c:when>
<c:otherwise>
  <c:set var="selfAction" value="${subTabUrl}&eid=${entityId}"/>
</c:otherwise>
</c:choose>

<html:form action="/resource/group/monitor/visibility/GroupPerformance">

      <table width="300" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="3">
            <tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>
          </td>
        </tr>
        <tr>
          <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
          <td>
<tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.byType">
  <tiles:put name="summaries" beanName="PerfSummaries"/>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
          </td>
          <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
    <td valign="top">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="3">
      <tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
        <tiles:put name="selectedIndex" value="2"/>
        <tiles:put name="tabListName" value="perf"/>
        <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
        <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
        <tiles:put name="entityType" beanName="entityType"/>
        <c:if test="${not empty ctype}">
          <tiles:put name="autogroupResourceType" beanName="ctype"/>
        </c:if>
      </tiles:insert>
          </td>
        </tr>
        <tr>
          <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
          <td>
<tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.table">
  <tiles:put name="perfSummaries" beanName="PerfSummaries"/>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
          </td>
      	</tr>
      </table>

<html:hidden property="id"/>
<html:hidden property="category"/>
<html:hidden property="ctype"/>
<html:hidden property="rb"/>
<html:hidden property="re"/>
</html:form>
