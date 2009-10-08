<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<!-- ViewUserRoles.jsp -->

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>

<c:set var="widgetInstanceName" value="listUser"/>

<script type="text/javascript">
 var pageData = new Array();
 initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
 widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="selfAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="view"/>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.u}">
    <c:param name="u" value="${param.u}"/>
  </c:if>
</c:url>

<c:url var="pageAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="view"/>
  <c:if test="${not empty param.u}">
    <c:param name="u" value="${param.u}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
</c:url>

<hq:pageSize var="pageSize"/>

<html:form method="POST" action="/admin/user/RemoveRole">
<html:hidden property="u" value="${User.id}" />
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.user.RolesAssignedTo"/>
</tiles:insert>

<display:table items="${UserRole}" action="${selfAction}" paramId="u"
         paramName="User" paramProperty="id" 
         var="role" cellpadding="0" cellspacing="0" >
 <c:if test="${useroperations['MANAGE_SECURITY'] and User.id > 2}">
 <display:column property="id" 
		 title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"
   		 isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" width="1%" >
   <display:checkboxdecorator name="roles" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
 </display:column>
 </c:if>
 
 <display:column property="name" title="admin.user.list.Name" href="/admin/role/RoleAdmin.do?mode=view" paramId="r" paramProperty="id"
                 sortAttr="this.name"/>
 <display:column property="memberCount" title="admin.user.list.Members"/>
 <display:column property="description" title="common.header.Description" sortAttr="this.description" />
</display:table>

<c:if test="${useroperations['MANAGE_SECURITY'] and User.id > 2}"> <!-- user id 1 and 2 are fixed users -->
<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" value="/admin/user/UserAdmin.do?mode=addRoles"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="addToListParamName" value="u"/>
  <tiles:put name="addToListParamValue" beanName="User" beanProperty="id"/>
  <tiles:put name="pageList" beanName="UserRole"/>
  <tiles:put name="pageAction" beanName="pageAction"/>
</tiles:insert>
</c:if>

</html:form>


