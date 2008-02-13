<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<html:form method="POST" action="/alerts/config/EditMetricBaseline">

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="common.title.Edit"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/resource/common/monitor/alerts/config/DefinitionEditMetricBaseline.jsp"/>

<tiles:insert definition=".page.footer"/>

</html:form>
