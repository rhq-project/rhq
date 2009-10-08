<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- AddUserRoles.jsp -->

<html:form method="POST" action="/admin/user/AddUserRoles">

<tiles:insert definition=".portlet.error"/>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript">
</script>

<c:set var="widgetInstanceName" value="addRoles"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="pageAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="addRoles"/>
  <c:param name="u" value="${User.id}" />
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
</c:url>

<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="50%" valign="top">
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="admin.user.add.RolesTab"/>
      <tiles:put name="useFromSideBar" value="true"/>
     </tiles:insert>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="admin.user.add.AddToList"/>
      <tiles:put name="useToSideBar" value="true"/>
     </tiles:insert>
    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${AvailableRoles}" var="role"
                       action="${pageAction}" postfix="a"
                       styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableRole" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="name" title="header.roles" width="50%" sortAttr="r.name"/>
          <display:column property="description" title="common.header.Description" width="50%" sortAttr="r.description"/>
        </display:table>

      </div>
      <!--  /  -->

<!-- LIST ITEMS -->
<tiles:insert definition=".toolbar.new">
  <tiles:put name="useFromSideBar" value="true"/>
  <tiles:put name="pageList" beanName="AvailableRoles"/>
  <tiles:put name="pageAction" beanName="pageAction"/>
  <tiles:put name="postfix" value="a"/>
</tiles:insert>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
     <div id="AddButtonDiv" align="left">
      <html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/>
     </div>
      <br>&nbsp;<br>
     <div id="RemoveButtonDiv" align="right">
      <html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/>
     </div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${PendingRoles}" var="role"
                       action="${pageAction}" postfix="p"
                       styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingRole" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="name"  title="header.roles" width="50%" sortAttr="this.name"/>
          <display:column property="description" title="common.header.Description" width="50%" sortAttr="this.description"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="newButtonKey" value="admin.role.users.NewUserButton"/>
  <tiles:put name="useToSideBar" value="true"/>
  <tiles:put name="pageList" beanName="PendingRoles"/>
  <tiles:put name="pageAction" beanName="pageAction"/>
  <tiles:put name="postfix" value="p"/>
 </tiles:insert>

    </td>
    <!-- / ADD COLUMN  -->
	
  </tr>
</table>
<!-- / SELECT & ADD -->

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="u"/>
</html:form>
