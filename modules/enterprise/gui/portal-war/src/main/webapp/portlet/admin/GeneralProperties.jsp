<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="portletName"/>

<html:hidden property="portletName" value="${portletName}"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="dash.settings.FormLabel.Display"/></td>
    <td width="30%" class="BlockContent">
      <input type="radio" name="displayOnDash" value="on" checked><fmt:message key="dash.settings.FormContent.Yes"/><br>
      <input type="radio" name="displayOnDash" value="off"><fmt:message key="dash.settings.FormContent.No"/>
    </td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
</table>
