<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="fullName" value="${User.firstName} ${User.lastName}"/>

<!-- FORM -->
<html:form action="/admin/user/New">

<!--  PAGE TITLE -->
<tiles:insert definition=".page.title.admin.user">
  <tiles:put name="titleKey" value="admin.user.NewUser"/>  
  <tiles:put name="titleName" beanName="fullName"/>   
</tiles:insert>
<!--  /  -->

<!--  HEADER TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>  
</tiles:insert>
<tiles:insert definition=".portlet.error"/>
<!--  /  -->

<!--  UserForm -->
<tiles:insert page="/admin/user/UserForm.jsp"/>
<!--  /UserForm -->

<!--  empty -->
<tiles:insert definition=".toolbar.empty"/>  
<!--  /empty -->

<!--  ok assign -->
<tiles:insert definition=".form.buttons.okAssign">
 <tiles:put name="okAssignOnly" value="true"/>
 <tiles:put name="okAssignButton" value="true"/>
</tiles:insert>

<!--  /ok assign -->

<!--  Page footer -->
<tiles:insert definition=".page.footer">
  <tiles:put name="msgKey" value="admin.user.new.AssigningThisUserEtc"/>
</tiles:insert>
<!--  /Page footer -->

</html:form>
<!-- /  -->
