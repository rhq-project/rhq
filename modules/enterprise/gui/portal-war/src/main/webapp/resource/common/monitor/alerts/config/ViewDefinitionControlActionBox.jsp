<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:if test="${controlEnabled}">
<tiles:insert definition=".events.config.view.controlaction"/>

<c:if test="${not empty Resource}">
  <hq:authorization permission="MANAGE_ALERTS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/alerts/Config.do?mode=editControlAction&ad=${alertDef.id}&id=${Resource.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
</c:if>

<c:if test="${not empty ResourceType}">
  <hq:authorization permission="MANAGE_SETTINGS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/alerts/Config.do?mode=editControlAction&ad=${alertDef.id}&type=${ResourceType.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
</c:if>
<br>
</c:if>
