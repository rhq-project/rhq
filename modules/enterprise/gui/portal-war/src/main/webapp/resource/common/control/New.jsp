<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="section" ignore="true"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>

<!-- FORM -->
<c:choose>
 <c:when test="${section eq 'group'}">
  <c:set var="onsubmitStr" value="selectAllOptions('leftSel');"/>
 </c:when>
 <c:otherwise>
  <c:set var="onsubmitStr" value=""/> 
 </c:otherwise>
</c:choose>

<html:form action="/resource/${section}/control/New" onsubmit="${onsubmitStr}">
<html:hidden property="rid" value="${param.rid}"/>
<html:hidden property="type" value="${param.type}"/>

<!--  PAGE TITLE -->
<c:choose>
 <c:when test="${section eq 'service'}">
  <tiles:insert definition=".page.title.resource.service">
   <tiles:put name="titleKey" value="resource.server.Control.PageTitle.New"/>
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  </tiles:insert>
 </c:when>
 <c:when test="${section eq 'group'}">
  <tiles:insert definition=".page.title.resource.group">
   <tiles:put name="titleKey" value="resource.group.Control.PageTitle.New"/>
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".page.title.resource.server">
   <tiles:put name="titleKey" value="resource.server.Control.PageTitle.New"/>
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>

<!-- CONTROL ACTION PROPERTIES -->
<tiles:insert definition=".resource.common.control.editControlActionProperties"/>

<c:if test="${section eq 'group'}">
<br/>    
<tiles:insert definition=".resource.group.control.editControlActionBehavior"/>
</c:if>

<!-- SCHEDULE ACTION PROPERTIES -->
<br/>
<tiles:insert definition=".schedule"/>

<!-- OK/RESET/CANCEL -->
<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>
</html:form>

