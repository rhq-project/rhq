<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- ViewUser.jsp -->

<hq:pageSize var="pageSize"/>

<!--  PAGE TITLE -->
<c:set var="pagetmpname" value="${User.firstName} ${User.lastName}" />
<tiles:insert definition=".page.title.admin.user.view">
 <tiles:put name="titleName"  beanName="pagetmpname" />
</tiles:insert>
<!--  /  -->

<!-- USER PROPERTIES -->

<tiles:insert definition=".portlet.confirm" flush="true"/>
<tiles:insert definition=".portlet.error" flush="true"/>
<tiles:insert definition=".admin.user.ViewProperties"/>

<!-- USER ROLES -->
<tiles:insert definition=".admin.user.ViewRoles"/>

<tiles:insert definition=".page.return">
  <tiles:put name="returnUrl" value="/admin/user/UserAdmin.do?mode=list"/>
  <tiles:put name="returnKey" value="admin.user.ReturnToUsers"/>
</tiles:insert>

<!--  Page footer -->
<tiles:insert definition=".page.footer"/>
<!--  /Page footer -->
