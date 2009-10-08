<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- ListRoles.jsp -->

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listRoles"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<hq:pageSize var="pageSize"/>

<c:url var="pageAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="sortAction" value="/admin/role/RoleAdmin.do">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<html:form method="POST" action="/admin/role/Remove.do">

<tiles:insert definition=".page.title.admin.role">
  <tiles:put name="titleKey" value="admin.role.ListRolesPageTitle"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<display:table items="${AllRoles}" var="role" action="${sortAction}" width="100%" cellspacing="0" cellpadding="0">
  <c:if test="${useroperations['MANAGE_SECURITY']}">
     <display:column property="id"
                     title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"
                     isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" width="1%">
       <display:checkboxdecorator name="r" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
  </display:column>
  </c:if>
  <display:column width="25%" property="name" title="admin.role.list.NameTH"
                              href="/admin/role/RoleAdmin.do?mode=view" paramId="r" paramProperty="id"
                              sortAttr="r.name" />
  <display:column width="10%" property="memberCount" title="admin.role.list.MembersTH" />
  <display:column width="65%" property="description" title="common.header.Description" sortAttr="r.description"/> 
</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" value="/admin/role/RoleAdmin.do?mode=new"/>
  <tiles:put name="deleteOnly"><c:out value="${!useroperations['MANAGE_SECURITY']}"/></tiles:put>
  <tiles:put name="newOnly"><c:out value="${!useroperations['MANAGE_SECURITY']}"/></tiles:put>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="defaultSortColumn" value="r.name"/>
  <tiles:put name="pageList" beanName="AllRoles"/>
  <tiles:put name="pageAction" beanName="pageAction"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

</html:form>
