<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- Don't insert the sub-tiles if there is no alert and no alertDef. --%>
<c:if test="${not empty alert and not empty alertDef}">

<html:form action="/alerts/RemoveAlerts">
<html:hidden property="alerts" value="${a}"/>
<html:hidden property="id" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.resourceType.category.name}"/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.current.detail.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".events.alert.view.properties"/>

&nbsp;<br>
<tiles:insert definition=".events.config.view.conditions">
  <tiles:put name="showValues" value="true"/>
</tiles:insert>

&nbsp;<br>
<tiles:insert definition=".events.alert.view.notifications"/>

&nbsp;<br>
<tiles:insert definition=".events.alert.view.controlaction"/>

<tiles:insert definition=".form.buttons.deleteCancel"/>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".page.footer"/>

</html:form>

</c:if>
