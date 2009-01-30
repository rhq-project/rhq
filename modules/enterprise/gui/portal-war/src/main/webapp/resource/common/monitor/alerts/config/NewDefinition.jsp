<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:form action="/alerts/New">

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.edit.NewAlertDef.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<c:if test="${not empty Resource}" >
  <html:hidden property="id" value="${Resource.id}"/>
</c:if>
<c:if test="${not empty ResourceType}" >
  <html:hidden property="type" value="${ResourceType.id}"/>
</c:if>

<tiles:insert definition=".events.config.view.nav"/>
 
<tiles:insert definition=".events.config.new.properties"/>

<tiles:insert definition=".events.config.conditions">
  <tiles:put name="formName" value="NewAlertDefinitionForm"/>
</tiles:insert>

<tiles:insert definition=".events.config.form.buttons" /> 

<tiles:insert definition=".page.footer"/>

</html:form>
