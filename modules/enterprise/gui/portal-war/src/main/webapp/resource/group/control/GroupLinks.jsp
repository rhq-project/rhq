<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="group"/>

<html:link page="/resource/platform/Inventory.do?mode=edit" paramId="p" paramName="group" paramProperty="id">
	<fmt:message key="common.resource.link.Edit"/>
	<html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
</html:link>

<br>

<html:link page="/resource/server/Inventory.do?mode=new" paramId="p" paramName="group" paramProperty="id">
	<fmt:message key="resource.platform.inventory.NewServerLink"/>
	<html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
</html:link>

<br>

<html:link page="#" paramId="p" paramName="group" paramProperty="id">
	<fmt:message key="resource.platform.inventory.NewDiscoveryLink"/>
	<html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
</html:link>
