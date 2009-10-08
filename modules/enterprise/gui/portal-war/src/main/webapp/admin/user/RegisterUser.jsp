<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants" symbol="MODE_REGISTER" var="MODE_REGISTER"/>

<c:set var="User" value="${sessionScope.webUser.subject}"/>

<html:form action="/admin/user/Register">

<tiles:insert definition=".page.title.admin.user">
  <tiles:put name="titleKey" value="admin.user.RegisterUserPageTitle"/>  
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="monitorBlockContainer">
  <tr>
    <td><fmt:message key="admin.user.generalProperties.WelcomeEtc"/></td>
  </tr>
</table>

&nbsp;<br>

<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>  
</tiles:insert>

<tiles:insert page="/admin/user/UserForm.jsp">
  <tiles:put name="User" beanName="User"/>
  <tiles:put name="mode" beanName="MODE_REGISTER"/>
</tiles:insert>

<tiles:insert definition=".form.buttons.logout"/>

<tiles:insert definition=".page.footer"/>

</html:form>
