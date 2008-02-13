<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="listObjectProperty" ignore="true"/>

    <td width="80%" class="BlockContent">
      <c:choose>
      <c:when test="${listSize == 0}">
        <fmt:message key="alert.current.detail.notifications.none"/>
      </c:when>
      <c:otherwise>
        ${listObjectProperty}
      </c:otherwise>
      </c:choose>
    </td>
