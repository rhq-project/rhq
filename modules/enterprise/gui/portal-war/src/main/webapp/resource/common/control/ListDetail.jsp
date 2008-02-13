<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="section"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>
<c:set var="entityId" value="${Resource.entityId}"/>

<c:choose>
 <c:when test="${section eq 'service'}">
 <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.service.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
 <!-- CONTROL BAR -->
 <tiles:insert definition=".tabs.resource.service.control.list.detail">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 </tiles:insert>
 </c:when>
 <c:when test="${section eq 'group'}">
 <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.group.full">
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
 <!-- CONTROL BAR -->
 <tiles:insert definition=".tabs.resource.group.control.current">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 </tiles:insert>
 </c:when>
 <c:otherwise>
  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.server.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
  <!-- CONTROL BAR -->
  <tiles:insert definition=".tabs.resource.server.control.list.detail">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>
<br>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<hq:constant symbol="CONTROL_ENABLED_ATTR" var="CONST_ENABLED" />
<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
<c:set var="curStatusTabKey" value="resource.${section}.ControlStatus.Tab"/>
<!-- CURRENT STATUS -->
<tiles:insert definition=".resource.common.control.currentStatus">
 <tiles:put name="tabKey" beanName="curStatusTabKey"/>
 <tiles:put name="section" beanName="section"/>
</tiles:insert>
<br>

<!-- QUICK CONTROL -->
<c:set var="tmpQControl" value=".resource.${section}.control.quickControl"/>
<tiles:insert beanName="tmpQControl">
 <tiles:put name="section" beanName="section"/>
</tiles:insert>

<br>

  <!-- CONTROL ACTION SCHEDULE -->
  <c:if test="${hasControlActions}">
    <c:set var="tmpScheduled" value=".resource.${section}.control.list.scheduled"/>
    <tiles:insert beanName="tmpScheduled">
      <tiles:put name="section" beanName="section"/>
    </tiles:insert>
  </c:if>

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
   <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
   </c:url>
   <c:set var="tmpMessage">
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/> <fmt:message key="resource.common.control.NotEnabled.ToEnable"/> <html:link href="${enableControlLink}"><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/></html:link> <fmt:message key="resource.common.control.NotEnabled.InInventory"/>
   </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insert definition=".portlet.notenabled">
    <tiles:put name="message" beanName="tmpMessage"/>
   </tiles:insert>

</c:otherwise>
</c:choose>

<br>

<tiles:insert definition=".page.footer"/>
