<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- ListUsers.jsp -->

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listUser"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<hq:pageSize var="pageSize"/>

<c:url var="pageAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>       
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<c:url var="sortAction" value="/admin/user/UserAdmin.do">
  <c:param name="mode" value="list"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<!-- FORM -->
<html:form action="/admin/user/Remove">
<c:if test="${not empty param.so}">
  <html:hidden property="so" value="${param.so}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <html:hidden property="sc" value="${param.sc}"/>
</c:if>

  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.admin.user">
    <tiles:put name="titleName" beanName="fullName"/>   
  </tiles:insert>
  <!--  /  -->

  <display:table cellspacing="0" cellpadding="0" width="100%" action="${sortAction}" items="${AllUsers}" var="user">

  <c:if test="${useroperations['MANAGE_SECURITY']}">
    <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="users" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"  suppress="${user.id == 2}" />
    </display:column>
  </c:if>
    <display:column width="20%" property="name" sortAttr="s.name"
                    title="username"
                    href="/admin/user/UserAdmin.do?mode=view" paramId="u" paramProperty="id" />
  
    <display:column width="20%" property="firstName" sortAttr="s.firstName"
                    title="admin.user.list.First" />
       
    <display:column width="20%" property="lastName" sortAttr="s.lastName"
                    title="admin.user.list.Last" />
                    
    <display:column width="20%" property="emailAddress" title="admin.user.list.Email" autolink="true" sortAttr="s.emailAddress"/>
    <display:column width="20%" property="department" title="admin.user.list.Department" sortAttr="s.department" />
  </display:table>

  <tiles:insert definition=".toolbar.list">
    <tiles:put name="listNewUrl" value="/admin/user/UserAdmin.do?mode=new"/>  
    <tiles:put name="deleteOnly"><c:out value="${!useroperations['MANAGE_SECURITY']}"/></tiles:put>
    <tiles:put name="newOnly"><c:out value="${!useroperations['MANAGE_SECURITY']}"/></tiles:put>
    <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
    
     <tiles:put name="pageList" beanName="AllUsers"/>
     <tiles:put name="pageAction" beanName="pageAction"/>

  </tiles:insert>
  
  <tiles:insert definition=".page.footer"/>

</html:form>
<!-- /  -->
