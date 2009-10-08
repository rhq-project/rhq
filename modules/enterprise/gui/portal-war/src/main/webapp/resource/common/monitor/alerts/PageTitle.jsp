<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="titleKey" ignore="true"/>

<c:choose>
<c:when test="${empty Resource}">
<tiles:insert definition=".page.title.events.noresource">
  <tiles:put name="titleKey" value="alert.current.detail.noresource.PageTitle"/>
  <tiles:put name="titleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${Platform == Resource.resourceType.category}">
<tiles:insert definition=".page.title.events.platform">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${Server == Resource.resourceType.category}">
<tiles:insert definition=".page.title.events.server">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${Service == Resource.resourceType.category}">
<tiles:insert definition=".page.title.events.service">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<%--<c:when test="${4 == Resource.resourceType.category}">
<tiles:insert definition=".page.title.events.application">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${5 == Resource.resourceType.category}">
<tiles:insert definition=".page.title.events.group">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>--%>
</c:choose>
