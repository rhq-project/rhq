<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<!--  PAGE TITLE -->
<tiles:insert definition=".page.title.admin.user">
  <tiles:put name="titleKey" value="common.title.Edit"/>
  <tiles:put name="titleName"  beanName="TitleParam"/>
</tiles:insert>
<!--  / END PAGE TITLE  -->

<!--  PAGE ERRORS -->
<tiles:insert definition=".portlet.error"/>
<!--  / END PAGE ERRORS -->

<!-- ADD USERS ROLES FORM -->
<tiles:insert definition=".admin.user.AddUserRoles"/>
<!-- / END USER ROLES FORM -->
