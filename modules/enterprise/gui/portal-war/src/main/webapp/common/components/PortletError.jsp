<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<logic:messagesPresent property="org.apache.struts.action.GLOBAL_MESSAGE">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
    <td class="ErrorBlock" width="100%"><html:errors property="org.apache.struts.action.GLOBAL_MESSAGE"/></td>
  </tr>
</table>
</logic:messagesPresent>
