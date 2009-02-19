<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- FORM BUTTONS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ToolbarLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr align=left valign=bottom>
    <td width="20%">&nbsp;</td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="7" border="0">
        <tr>
          <td><html:image page="/images/fb_ok.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" onmouseover="imageSwap(this, imagePath + 'fb_ok', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_ok', '');" onmousedown="imageSwap(this, imagePath +  'fb_ok', '_down')" onclick="checkSubmit(); isButtonClicked=true;"/></td>
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
          <td><html:image page="/images/fb_reset.gif" border="0" titleKey="FormButtons.ClickToReset" property="reset" onmouseover="imageSwap(this, imagePath + 'fb_reset', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_reset', '');" onmousedown="imageSwap(this, imagePath + 'fb_reset', '_down')"/></td>
          <td nowrap="nowrap"><html:link page="/Logout.do"><fmt:message key="admin.user.generalProperties.Logout"/></html:link> <fmt:message key="admin.user.generalProperties.CompleteLater"/></td>
		  <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<!-- /  -->
