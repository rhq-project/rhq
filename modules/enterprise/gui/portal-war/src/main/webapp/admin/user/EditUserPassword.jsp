<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="fullName" value="${User.firstName} ${User.lastName}"/>
<tiles:importAttribute name="administrator"/>
<!-- FORM -->
<html:form action="/admin/user/EditPassword">  

  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.admin.user">
    <tiles:put name="titleKey" value="common.title.Edit"/>  
    <tiles:put name="titleName" beanName="fullName"/>   
  </tiles:insert>
  <!--  /  -->

  <!--  HEADER TITLE -->
  <tiles:insert definition=".header.tab">  
    <tiles:put name="tabKey" value="admin.user.changePassword.ChangePasswordTab"/>  
  </tiles:insert>

  <!--  /  -->
  <c:set var="tmpu" value="${param.u}" />
  <tiles:insert page="/admin/user/UserPasswordForm.jsp">
    <tiles:put name="userId" beanName="tmpu"/>    
    <tiles:put name="administrator" beanName="administrator"/>    
  </tiles:insert>

  <tiles:insert definition=".toolbar.empty"/>  

  <tiles:insert definition=".form.buttons"/>

  <tiles:insert definition=".page.footer"/>

</html:form>
<!-- /  -->
