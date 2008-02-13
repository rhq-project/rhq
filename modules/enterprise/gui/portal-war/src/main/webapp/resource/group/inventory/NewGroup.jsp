<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<html:form action="/resource/group/inventory/NewGroup.do">

<tiles:insert definition=".page.title.resource.group.new"/>

<tiles:insert definition=".resource.group.inventory.generalProperties"/>
      &nbsp;<br>
      
<tiles:insert page="/resource/group/inventory/GroupTypeForm.jsp"/>
      &nbsp;<br>

<!--  ok assign -->
<tiles:insert definition=".form.buttons"/>
<!--  /ok assign -->

<!--  Page footer -->
<tiles:insert definition=".page.footer">
  <tiles:put name="msgKey" value="resource.group.inventory.New.AddResourcesEtc"/>
</tiles:insert>
<!--  /Page footer -->

</html:form>

