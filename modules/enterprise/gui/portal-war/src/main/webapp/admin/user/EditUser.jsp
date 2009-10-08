<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="fullName" value="${User.firstName} ${User.lastName}"/>

<!-- FORM -->
<html:form action="/admin/user/Edit">

<!--  PAGE TITLE -->
<tiles:insert definition=".page.title.admin.user">
  <tiles:put name="titleKey" value="admin.user.edit"/>  
  <tiles:put name="titleName" beanName="fullName"/>   
</tiles:insert>
<!--  /  -->

<!--  HEADER TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>  
</tiles:insert>
<tiles:insert definition=".portlet.error"/>
<!--  /  -->

<tiles:insert page="/admin/user/UserForm.jsp">  
  <tiles:put name="User" beanName="User"/>
</tiles:insert>

<tiles:insert definition=".toolbar.empty"/>  

<tiles:insert definition=".form.buttons"/>
</html:form>
