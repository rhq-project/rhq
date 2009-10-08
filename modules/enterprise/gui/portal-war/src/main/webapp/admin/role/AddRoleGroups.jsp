<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<html:form action="/admin/role/AddResourceGroups" method="POST">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleName" beanName="Role" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/admin/role/RoleGroupsForm.jsp">
  <tiles:put name="availableResGrps" beanName="AvailableResGrps"/>
  <tiles:put name="numAvailableResGrps" beanName="NumAvailableResGrps"/>
  <tiles:put name="pendingResGrps" beanName="PendingResGrps"/>
  <tiles:put name="numPendingResGrps" beanName="NumPendingResGrps"/>
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="r"/>
</html:form>
