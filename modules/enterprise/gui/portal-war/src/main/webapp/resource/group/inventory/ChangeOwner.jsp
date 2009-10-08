<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:set var="selfUrl" value="/resource/group/Inventory.do?mode=changeOwner&rid=${Resource.id}&type=${Resource.entityId.type}"/>

<html:form action="/resource/group/inventory/ChangeOwner">

<tiles:insert definition=".page.title.resource.group">
  <tiles:put name="titleKey" value="common.title.Edit"/>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".resource.common.inventory.changeResourceOwner">
  <tiles:put name="users" beanName="AllUsers"/>
  <tiles:put name="userCount" beanName="NumUsers"/>
  <tiles:put name="formName" value="ChangeResourceOwnerForm"/>
  <tiles:put name="selfUrl" beanName="selfUrl"/>
</tiles:insert>
      &nbsp;<br>

<tiles:insert definition=".form.buttons">
  <tiles:put name="cancelOnly" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="rid"/>
<html:hidden property="type"/>
</html:form>
