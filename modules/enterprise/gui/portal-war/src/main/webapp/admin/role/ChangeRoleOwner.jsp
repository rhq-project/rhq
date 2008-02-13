<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<c:url var="selfAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="changeOwner"/>
  <c:param name="r" value="${Role.id}"/>
</c:url>

<html:form action="/admin/role/ChangeOwner">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleName" beanName="Role" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.changeowner.ChangeOwnerTab"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<display:table items="${AllUsers}" var="user" action="${selfAction}" width="100%" cellpadding="0" cellspacing="0">
  <display:column width="1%" property="id" isLocalizedTitle="false" styleClass="ListCell" headerStyleClass="ListHeader" >
    <display:imagebuttondecorator form="ChangeRoleOwnerForm" input="o" page="/images/fb_select.gif"/>
  </display:column>
  <display:column width="20%" property="firstName" title="admin.role.users.FirstNameTH"/> <!-- XXX: add new sortAttr for firstName -->
  <display:column width="20%" property="lastName" title="admin.role.users.LastNameTH"/> <!-- XXX: add new sortAttr for lastName -->
  <display:column width="20%" property="name" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true"/>
  <display:column width="20%" property="emailAddress" title="admin.role.users.EmailTH"/>
  <display:column width="20%" property="department" title="admin.role.users.DepartmentTH"/>
</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="noButtons" value="true"/>
  <tiles:put name="listItems" beanName="AllUsers"/>
  <tiles:put name="listSize" beanName="NumUsers"/>
  <tiles:put name="pageSizeAction" beanName="selfAction"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>  
  <tiles:put name="defaultSortColumn" value="3"/>
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="cancelOnly" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="o"/>
<html:hidden property="r"/>
</html:form>
