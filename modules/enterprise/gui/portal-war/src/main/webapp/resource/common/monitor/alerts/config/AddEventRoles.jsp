<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
<c:choose>
<c:when test="${AvailableRoles == null}">
<!-- error occured -->
<tiles:insert page="/common/NoRights.jsp"/>
</c:when>
<c:otherwise>

<html:form method="POST" action="/alerts/config/AddRoles">

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.edit.AddNotifications"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/resource/common/monitor/alerts/config/DefinitionRolesForm.jsp">
  <tiles:put name="availableRoles" beanName="AvailableRoles"/>
  <tiles:put name="numAvailableRoles" beanName="AvailableRoles" beanProperty="totalSize"/>
  <tiles:put name="pendingRoles" beanName="PendingRoles"/>
  <tiles:put name="numPendingRoles" beanName="PendingRoles" beanProperty="totalSize"/>
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>
<html:hidden property="ad"/>
<html:hidden property="id"/>
<html:hidden property="type"/>

</html:form>

</c:otherwise>
</c:choose>
