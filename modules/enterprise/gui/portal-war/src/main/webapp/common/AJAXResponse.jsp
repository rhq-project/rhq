<?xml version="1.0" encoding="ISO-8859-1"?>
<%@ page language="java" contentType="text/xml" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% response.setHeader("Pragma","no-cache");%>
<% response.setHeader("Cache-Control","no-store");%>
<% response.setDateHeader("Expires",-1);%>
<ajax-response>
  <c:if test="${not empty ajaxType}">
  <response type="<c:out value="${ajaxType}"/>" id="<c:out value="${ajaxId}"/>"><c:if test="${ajaxType eq 'element'}"><c:out value="${ajaxHTML}" escapeXml="false"/></c:if></response>
  </c:if>
</ajax-response>
