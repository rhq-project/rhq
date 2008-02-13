<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${controlEnabled}">
<tiles:insert definition=".events.config.view.controlaction"/>

<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl">
     <c:choose>
        <c:when test="${not empty Resource}">
           <c:out value="/alerts/Config.do?mode=editControlAction&ad=${alertDef.id}&id=${Resource.id}"/>
        </c:when>
        <c:otherwise>
           <c:out value="/alerts/Config.do?mode=editControlAction&ad=${alertDef.id}&type=${ResourceType.id}"/>
        </c:otherwise>
     </c:choose>
  </tiles:put>
</tiles:insert>
<br>
</c:if>
