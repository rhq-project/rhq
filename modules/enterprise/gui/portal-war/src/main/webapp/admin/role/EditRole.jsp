<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<html:form action="/admin/role/Edit">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleName" beanName="Role" beanProperty="name"/>
</tiles:insert>

<tiles:insert page="/admin/role/RolePropertiesForm.jsp">
  <tiles:put name="role" beanName="Role"/>
  <tiles:put name="mode" value="edit"/>
</tiles:insert>

<tiles:insert page="/admin/role/RolePermissionsForm.jsp">
  <tiles:put name="perms" beanName="AllPerms"/>
</tiles:insert>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

<html:hidden property="r"/>
</html:form>
