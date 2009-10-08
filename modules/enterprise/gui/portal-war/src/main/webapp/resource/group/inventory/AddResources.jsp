<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<html:form method="POST" action="/resource/group/inventory/AddGroupResources">
<html:hidden property="groupId"/>
<html:hidden property="category"/>

<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".page.title.resource.group">
  <tiles:put name="titleKey" value="resource.group.inventory.AddResourcesPageTitle"/>
<%--<tiles:put name="titleName" beanName="group" beanProperty="name"/>--%>
  <tiles:put name="titleName" value="FAKE GROUP TITLE" />
</tiles:insert>

<tiles:insert page="/resource/group/inventory/AddResourcesForm.jsp">
  <tiles:put name="groupId" beanName="groupId" />
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

</html:form>
