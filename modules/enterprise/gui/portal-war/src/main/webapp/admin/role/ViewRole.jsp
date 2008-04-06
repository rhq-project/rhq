<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- ViewRole.jsp -->

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="userWidgetInstanceName" value="assignedUsers"/>
<c:set var="groupWidgetInstanceName" value="assignedGroups"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${userWidgetInstanceName}"/>');
userWidgetProperties = getWidgetProperties('<c:out value="${userWidgetInstanceName}"/>');
initializeWidgetProperties('<c:out value="${groupWidgetInstanceName}"/>');
groupWidgetProperties = getWidgetProperties('<c:out value="${groupWidgetInstanceName}"/>');
</script>

<c:url var="selfPuAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="view"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pnu}">
    <c:param name="pnu" value="${param.pnu}"/>
  </c:if>
  <c:if test="${not empty param.psu}">
    <c:param name="psu" value="${param.psu}"/>
  </c:if>
  <c:if test="${not empty param.sou}">
    <c:param name="sou" value="${param.sou}"/>
  </c:if>
  <c:if test="${not empty param.scu}">
    <c:param name="scu" value="${param.scu}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
</c:url>

<c:url var="selfPgAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="view"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pnu}">
    <c:param name="pnu" value="${param.pnu}"/>
  </c:if>
  <c:if test="${not empty param.psu}">
    <c:param name="psu" value="${param.psu}"/>
  </c:if>
  <c:if test="${not empty param.sou}">
    <c:param name="sou" value="${param.sou}"/>
  </c:if>
  <c:if test="${not empty param.scu}">
    <c:param name="scu" value="${param.scu}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
</c:url>

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleName" beanName="Role" beanProperty="name"/>
</tiles:insert>

<tiles:insert page="/admin/role/RolePropertiesForm.jsp">
  <tiles:put name="mode" value="view"/>
  <tiles:put name="role" beanName="Role"/>
</tiles:insert>

<tiles:insert page="/admin/role/RolePermissionsForm.jsp">
  <tiles:put name="mode" value="view"/>
</tiles:insert>

<c:if test="${useroperations['MANAGE_SECURITY'] and not Role.fsystem}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" value="/admin/role/RoleAdmin.do?mode=edit"/>
  <tiles:put name="editParamName" value="r"/>
  <tiles:put name="editParamValue" beanName="Role" beanProperty="id"/>
</tiles:insert>
</c:if>
<br>

<html:form method="POST" action="/admin/role/RemoveUsers">

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.users.AssignedUsersTab"/>
</tiles:insert>

<display:table items="${RoleUsers}" var="user" action="${selfPuAction}"
               postfix="u"
               width="100%" cellpadding="0" cellspacing="0">
  <c:if test="${useroperations['MANAGE_SECURITY']}">
    <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, userWidgetProperties, true)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="u" onclick="ToggleSelection(this, userWidgetProperties, true)" styleClass="listMember"/>
    </display:column>
  </c:if>
  <display:column href="/admin/user/UserAdmin.do?mode=view" paramId="u" paramProperty="id"
                  width="34%" property="name" title="admin.role.users.UsernameTH" sortAttr="u.name" />
  <display:column width="33%" property="firstName" title="admin.role.users.FirstNameTH" sortAttr="u.firstName" /> <!-- XXX sort turned off, can't get it to work -->
  <display:column width="33%" property="lastName" title="admin.role.users.LastNameTH" sortAttr="u.lastName" /> <!-- XXX sort turned off, can't get it to work -->
</display:table>

<c:if test="${useroperations['MANAGE_SECURITY']}">
<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" value="/admin/role/RoleAdmin.do?mode=addUsers"/>
  <tiles:put name="widgetInstanceName" beanName="userWidgetInstanceName"/>
  <tiles:put name="addToListParamName" value="r"/>
  <tiles:put name="addToListParamValue" beanName="Role" beanProperty="id"/>
  <tiles:put name="pageList" beanName="RoleUsers"/>
  <tiles:put name="pageAction" beanName="selfPuAction"/>
  <tiles:put name="postfix" value="u"/>
</tiles:insert>
</c:if>

<html:hidden property="r"/>
</html:form>
<br>

<c:if test="${not Role.fsystem}">
<html:form method="POST" action="/admin/role/RemoveResourceGroups">

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.groups.AssignedGroupsTab"/>
</tiles:insert>

<display:table items="${RoleResGrps}" var="group" action="${selfPgAction}"
               postfix="g"
               width="100%" cellpadding="0" cellspacing="0">
  <c:if test="${useroperations['MANAGE_SECURITY']}">
    <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, groupWidgetProperties, true)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="g" onclick="ToggleSelection(this, groupWidgetProperties, true)" styleClass="listMember"/>
    </display:column>
  </c:if>
  <display:column width="25%" property="name" href="/resource/group/Inventory.do?mode=view&groupId=${group.id}" title="common.header.Group"
                  sortAttr="r.name"/>
  <display:column width="75%" property="description" title="common.header.Description"/>
</display:table>

<c:if test="${useroperations['MANAGE_SECURITY']}">
<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" value="/admin/role/RoleAdmin.do?mode=addGroups"/>
  <tiles:put name="widgetInstanceName" beanName="groupWidgetInstanceName"/>
  <tiles:put name="addToListParamName" value="r"/>
  <tiles:put name="addToListParamValue" beanName="Role" beanProperty="id"/>
  <tiles:put name="pageList" beanName="RoleResGrps"/>
  <tiles:put name="pageAction" beanName="selfPgAction"/>
  <tiles:put name="postfix" value="g"/>
</tiles:insert>
</c:if>

<tiles:insert definition=".page.return">
  <tiles:put name="returnUrl" value="/admin/role/RoleAdmin.do?mode=list"/>
  <tiles:put name="returnKey" value="admin.role.view.ReturnToRoles"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="r"/>
</html:form>
</c:if>
