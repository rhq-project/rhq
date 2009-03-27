<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<tiles:insert definition=".page.title">
  <tiles:put name="titleBgStyle" value="PageTitle"/>
  <tiles:put name="titleImg" value="spacer.gif"/>
  <tiles:put name="titleKey" value="admin.settings.EditServerConfig.PageTitle"/>
</tiles:insert>

<hq:authorization permission="MANAGE_SETTINGS">

<html:form action="/admin/config/EditConfig">

<c:if test="${not empty param.debug}">
   <input type="hidden" name="debug" value="${param.debug}" />
</c:if>

<tiles:insert page="/admin/config/SystemInfoForm.jsp"/>
<br>
<br>
<tiles:insert page="/admin/config/GeneralPropertiesConfigForm.jsp"/>
<br>
<br>
<tiles:insert page="/admin/config/DataManagerConfigForm.jsp"/>
<br>
<br>
<tiles:insert page="/admin/config/BaselineConfigForm.jsp"/>
<br>
<br>
<tiles:insert page="/admin/config/LDAPForm.jsp"/>
<br>
<br>
<tiles:insert page="/admin/config/SNMPForm.jsp"/>

<%--<tiles:insert page="/admin/config/MiscForm.jsp"/>--%>

<!-- FORM BUTTONS -->
<tiles:insert definition=".form.buttons"/>

</html:form>

</hq:authorization>

<!-- FOOTER -->
<tiles:insert definition=".page.footer"/>
