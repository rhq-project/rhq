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

<html:form action="/resource/${section}/control/Edit" onsubmit="${onsubmitStr}">
<html:hidden property="rid" value="${param.rid}"/>
<html:hidden property="type" value="${param.type}"/>
<html:hidden property="bid" value="${param.bid}"/>

<c:set var="tmpTitle" value=".page.title.resource.${section}"/>
<c:set var="tmpKey" value="resource.${section}.Control.PageTitle.Edit"/>
<!--  PAGE TITLE -->
<tiles:insert beanName="tmpTitle">
  <tiles:put name="titleKey" beanName="tmpKey"/>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>

<!-- CONTROL ACTION PROPERTIES -->
<tiles:insert definition=".resource.common.control.editControlActionProperties"/>

<c:if test="${section eq 'group'}">
<br/>    
<tiles:insert definition=".resource.group.control.editControlActionBehavior"/>
</c:if>

<!-- SCHEDULE ACTION PROPERTIES -->
<br/>
<c:choose>
 <c:when test="${section eq 'group'}">
  <tiles:insert definition=".schedule">
   <tiles:put name="TheControlForm" beanName="GroupControlForm"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".schedule">
   <tiles:put name="TheControlForm" beanName="ServerControlForm"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>

<!-- OK/RESET/CANCEL -->
<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>
</html:form>
