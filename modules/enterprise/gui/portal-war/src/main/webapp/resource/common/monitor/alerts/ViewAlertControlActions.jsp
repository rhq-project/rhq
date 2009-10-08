<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- Content Block Title: Control -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.current.detail.control.Tab"/>
</tiles:insert>

<!-- Control Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <c:forEach var="action" items="${alertControlActions}">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.ControlType"/></td>
    <td width="80%" class="BlockContent"><i>FIXME:&nbsp;</i><c:out value="${action}"/></td>
  </tr>
  </c:forEach>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
