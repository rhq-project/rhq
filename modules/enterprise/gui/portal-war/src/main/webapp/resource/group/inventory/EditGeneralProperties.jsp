<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<html:form action="/resource/group/inventory/EditGeneralProperties">
<html:hidden property="groupId"/>
<html:hidden property="category"/>

<tiles:insert definition=".page.title.resource.group">
  <tiles:put name="titleKey" value="common.title.Edit"/>
  <tiles:put name="titleName" beanName="group" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".resource.group.inventory.generalProperties">
  <tiles:put name="group" beanName="group" />
  <tiles:put name="category" beanName="category" />
</tiles:insert>

&nbsp;<br>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

</html:form>
