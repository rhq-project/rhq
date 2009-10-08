<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--<html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" border="0" align="right"/></html:link>--%>

<c:choose>
   <c:when test="${not empty Resource}">
      <c:set var="titleName"><hq:inventoryHierarchy resourceId="${Resource.id}"/></c:set>
   </c:when>
   <c:when test="${not empty groupId}">
      <c:set var="titleName"><hq:inventoryHierarchy groupId="${groupId}"/></c:set>
   </c:when>
   <c:when test="${not empty parent}">
      <c:set var="titleName"><hq:inventoryHierarchy parentResourceId="${parent}" resourceTypeId="${type}"/></c:set>
   </c:when>
</c:choose>

<c:choose>
  <c:when test="${not empty titleKey}">
    <fmt:message key="${titleKey}">
      <c:if test="${not empty titleName}">
        <fmt:param value="${titleName}"/>
      </c:if>
      <c:if test="${not empty subTitleName}">
        <fmt:param value="${subTitleName}"/>
      </c:if>
    </fmt:message>
  </c:when>
  <c:otherwise>
    <c:out value="${titleName}" escapeXml="false"/>
      <c:if test="${not empty subTitleName}">
        <c:out value="${subTitleName}"/>
      </c:if>
  </c:otherwise>
</c:choose>

