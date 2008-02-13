<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<tiles:importAttribute name="favorites" ignore="true"/>
<tiles:importAttribute name="traits" ignore="true"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockContent" width="100%"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/><i>
    <c:choose>
      <c:when test="${not empty favorites && favorites}">
        <fmt:message key="resource.common.monitor.visibility.NoFavoriteMetrics"/>
      </c:when>
      <c:when test="${not empty traits && traits}">
        <fmt:message key="resource.common.monitor.visibility.NoMetrics.Traits"/>
      </c:when>
      <c:otherwise>
        <fmt:message key="resource.common.monitor.visibility.NoMetricsEtc"/>
      </c:otherwise>
    </c:choose>
    </i></td>
  </tr>
  <tr>
    <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
