<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<html:form method="POST" action="/dashboard/resourcehealth/AddResourcesAction">
<html:hidden property="key" value=".dashContent.resourcehealth.resources"/>

<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".page.title">
  <tiles:put name="titleKey" value="dash.settings.resourcehealth.AddResourcesPageTitle"/>
  <tiles:put name="titleBgStyle" value="PageTitle"/>
  <tiles:put name="titleImg" value="spacer.gif"/>  
</tiles:insert>

<tiles:insert page="/portlet/addresources/AddResourcesForm.jsp"/>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

</html:form>
