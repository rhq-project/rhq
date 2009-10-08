<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="resource"/>

<font class="PageTitleSmallText">
<c:choose>
  <c:when test="${isFavorite}">
    <html:link page="/resource/common/QuickFavorites.do?resourceId=${resource.id}&mode=remove">
    	<fmt:message key="resource.common.quickFavorites.remove"/>
    	<html:img width="11" height="9" border="0" page="/images/title_arrow.gif"/>
    </html:link>
  </c:when> 
  <c:otherwise> 
    <html:link page="/resource/common/QuickFavorites.do?resourceId=${resource.id}&mode=add">
    	<fmt:message key="resource.common.quickFavorites.add"/>
    	<html:img width="11" height="9" border="0" page="/images/title_arrow.gif"/>
    </html:link>
  </c:otherwise> 
</c:choose>
</font>
