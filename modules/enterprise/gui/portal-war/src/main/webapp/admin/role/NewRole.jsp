<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<html:form action="/admin/role/New">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleKey" value="admin.role.NewRolePageTitle"/>
</tiles:insert>

<tiles:insert page="/admin/role/RolePropertiesForm.jsp"/>

<tiles:insert definition=".toolbar.empty"/>

      <html:img page="/images/spacer.gif" width="1" height="15" border="0"/>

<tiles:insert page="/admin/role/RolePermissionsForm.jsp">
</tiles:insert>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer">
  <tiles:put name="msgKey" value="admin.role.new.AssignUsersEtc"/>
</tiles:insert>

</html:form>
