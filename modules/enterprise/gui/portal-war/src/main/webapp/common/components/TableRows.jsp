<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<tiles:importAttribute name="principalBean" ignore="true" />
<tiles:importAttribute name="rowBean" ignore="true" />
<c:choose>
<%-- header --%>
<c:when test="${not empty principalBean}">
    <th>ID Header (.table must be overridden with a tableComp defined)</th>
    <th>Name Header (.table must be overridden with a tableComp defined)</th>
</c:when>
<%-- data --%>
<c:when test="${not empty rowBean}">
    <td><c:out value="${rowBean.id}" /></td><td><c:out value="${rowBean.name}" /></td>
</c:when>
<c:otherwise>
    <td>ID empty data (.table must be overridden with a tableComp defined)</td>
    <td>Name empty data (.table must be overridden with a tableComp defined)</td>
</c:otherwise>
</c:choose>
