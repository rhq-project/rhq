<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${CAM_SYSLOG_ACTIONS_ENABLED}">
<tiles:insert definition=".events.config.view.syslogaction"/>

<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl"><c:out value="/alerts/Config.do?mode=editSyslogAction&type=${Resource.entityId.type}&rid=${Resource.id}&ad=${alertDef.id}"/></tiles:put>
</tiles:insert>
<br>
</c:if>
