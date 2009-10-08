<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ page import="net.hyperic.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<p>
<b>Page Attributes</b><br>
<c:forEach var="entry" items="${pageScope}">
<i><c:out value="${entry.key}"/>: </i><c:out value="${entry.value}"/><br>
</c:forEach>
</p>

<p>
<b>Request Attributes</b><br>
<c:forEach var="entry" items="${requestScope}">
<i><c:out value="${entry.key}"/>: </i><c:out value="${entry.value}"/><br>
</c:forEach>
</p>

<p>
<b>Session Attributes</b><br>
<c:forEach var="entry" items="${sessionScope}">
<i><c:out value="${entry.key}"/>: </i><c:out value="${entry.value}"/><br>
</c:forEach>
</p>

<p>
<b>Application Attributes</b><br>
<c:forEach var="entry" items="${applicationScope}">
<i><c:out value="${entry.key}"/>: </i><c:out value="${entry.value}"/><br>
</c:forEach>
</p>

<p>
<b>System Properties</b><br>
<%=System.getProperties().toString()%>
</p>
