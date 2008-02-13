<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <logic:messagesPresent>
  <tr>
    <td colspan="6" align="left" class="ErrorField"><html:errors/></td>
  </tr>
  </logic:messagesPresent>
  <tr>
    <td width="10%" class="BlockLabel" nowrap="true"><fmt:message key="alert.config.edit.snmp.host"/>:</td>
    <td width="30%" class="BlockContent"><html:text size="40" maxlength="100" property="host"/></td>
    <td width="10%" class="BlockLabel"><fmt:message key="alert.config.edit.snmp.port"/>:</td>
    <td width="10%" class="BlockContent"><html:text size="30" maxlength="5" property="port"/></td>
    <td width="10%" class="BlockLabel"><fmt:message key="alert.config.edit.snmp.oid"/>:</td>
    <td width="30%" class="BlockContent"><html:text size="30" maxlength="250" property="oid"/></td>
  </tr>
  <tr>
    <td colspan="6" class="BlockBottomLine"><div style="width: 1px; height: 1px;"/></td>
  </tr>
</table>

<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td><html:image page="/images/tbb_set.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok"/></td>
    <td><html:image page="/images/tbb_remove.gif" border="0" titleKey="FormButtons.ClickToDelete" property="delete"/></td>
    <td colspan="4" width="100%">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="6" class="BlockBottomLine"><div style="width: 1px; height: 1px;"/></td>
  </tr>
</table>
