<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<logic:messagesPresent message="true">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="confirm">
  <tr>
    <td class="ConfirmationBlock"><html:img page="/images/tt_check.gif" width="9" height="9" alt="" border="0"/></td>
    <td class="ConfirmationBlock" width="100%" id="message">
<html:messages message="true" id="msg">
      <c:out value="${msg}"/><br>
</html:messages>
    </td>
  </tr>
</table>
</logic:messagesPresent>
