<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="availableUsers" ignore="true"/>
<tiles:importAttribute name="numAvailableUsers" ignore="true"/>
<tiles:importAttribute name="pendingUsers" ignore="true"/>
<tiles:importAttribute name="numPendingUsers" ignore="true"/>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="addRoles"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="selfPnaAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPnpAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPsaAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPspAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPaAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="r" value="${Role.id}"/>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
</c:url>

 <c:choose>
  <c:when test="${null == availableUsers && null == pendingUsers}">
   <tiles:insert definition=".form.buttons">
    <tiles:put name="cancelOnly" value="true"/>
   </tiles:insert>
  </c:when>
  <c:otherwise>
<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="50%" valign="top">

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.users.UsersTab"/>
  <tiles:put name="useFromSideBar" value="true"/>
</tiles:insert>

    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.users.AssignToRoleTab"/>
  <tiles:put name="useToSideBar" value="true"/>
</tiles:insert>

    </td>
  </tr>
   <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${availableUsers}" var="user" action="${selfPaAction}"
                       postfix="a"
                       styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="firstName" title="admin.role.users.FirstNameTH" sortAttr="s.firstName" width="33%"/>
          <display:column property="lastName" title="admin.role.users.LastNameTH" sortAttr="s.lastName" width="33%"/>
          <display:column property="name" title="admin.role.users.UsernameTH" sortAttr="s.name" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="useFromSideBar" value="true"/>
  <tiles:put name="pageList" beanName="availableUsers"/>
  <tiles:put name="pageAction" beanName="selfPsaAction"/>
  <tiles:put name="postfix" value="a"/>
</tiles:insert>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
      <div id="AddButtonDiv" align="left"><html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/></div>
      <br>&nbsp;<br>
      <div id="RemoveButtonDiv" align="right"><html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/></div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${pendingUsers}" var="user" action="${selfPpAction}"
                       postfix="p"
                       styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="firstName" title="admin.role.users.FirstNameTH" sortAttr="s.firstName" width="33%"/>
          <display:column property="lastName" title="admin.role.users.LastNameTH" sortAttr="s.lastName" width="33%"/>
          <display:column property="name" title="admin.role.users.UsernameTH" sortAttr="s.name" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="newButtonKey" value="admin.role.users.NewUserButton"/>
  <tiles:put name="useToSideBar" value="true"/>
  <tiles:put name="pageList" beanName="pendingUsers"/>
  <tiles:put name="pageAction" beanName="selfPspAction"/>
  <tiles:put name="postfix" value="p"/>   
</tiles:insert>
    </td>
    <!-- / ADD COLUMN  -->
	
  </tr>
</table>
<!-- / SELECT & ADD -->
</c:otherwise> <%-- availableUsers != null || pendingUsers != null --%>
</c:choose>
