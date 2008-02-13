<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<script language="JavaScript" type="text/javascript">
  var headerPath = "<html:rewrite page="/images/"/>";
</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td width="22%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td><html:img page="/images/logo_small.gif" width="119" height="22" alt="" border="0"/></td>
          <td width="100%" background="<html:rewrite page="/images/logo_Gradient_large.gif"/>"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td><html:img page="/images/logo_Image_small.gif" width="135" height="22" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
    <td width="78%" class="MastheadBgBottom">
      <table width="250" border="0" cellspacing="4" cellpadding="0">
        <tr> 
          <td rowspan="99"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td width="100%"><html:link href="" onclick="window.open('${helpBaseURL}Users+Guide','help','width=940,height=730,scrollbars=yes,toolbar=yes,left=40,top=40,resizable=yes'); return false;"><html:img page="/images/toolbox_Help.gif" onmouseover="imageSwap(this, headerPath + 'toolbox_Help', '_on');" onmouseout="imageSwap(this, headerPath + 'toolbox_Help', '')" width="28" height="14" alt="" border="0"/></html:link></td>
        </tr>
      </table>
    </td>
  </tr>
  <tr> 
    <td colspan="3" class="MastheadBottomLine"><html:img page="/images/spacer.gif" width="1" height="2" alt="" border="0"/></td>
  </tr>
</table>
