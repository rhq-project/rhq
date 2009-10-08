<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<html:form method="POST" action="/admin/role/AddUsers">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleKey" value="admin.role.add.users"/>
  <tiles:put name="titleName" beanName="Role" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/admin/role/RoleUsersForm.jsp">
  <tiles:put name="availableUsers" beanName="AvailableUsers"/>
  <tiles:put name="numAvailableUsers" beanName="AvailableUsers" beanProperty="totalSize"/>
  <tiles:put name="pendingUsers" beanName="PendingUsers"/>
  <tiles:put name="numPendingUsers" beanName="PendingUsers" beanProperty="totalSize"/>
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>
<html:hidden property="r"/>
</html:form>
