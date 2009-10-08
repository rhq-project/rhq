<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="editUrl"/>
<tiles:importAttribute name="editParamName" ignore="true"/>
<tiles:importAttribute name="editParamValue" ignore="true"/>

<c:choose>
  <c:when test="${not empty editParamName && not empty editParamValue}">
    <c:url var="editUrl" value="${editUrl}">
      <c:param name="${editParamName}" value="${editParamValue}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="editUrl" value="${editUrl}"/>
  </c:otherwise>
</c:choose>

<!-- EDIT TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td><html:link href="${editUrl}"><html:img page="/images/tbb_edit.gif" width="41" height="16" border="0"/></html:link></td>
  </tr>
</table>
<!--  /  -->
