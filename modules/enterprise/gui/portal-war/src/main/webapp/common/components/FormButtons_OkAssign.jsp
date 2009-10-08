<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%-- @param okAssignOnly a flag indicating to only display the okAssign, 
            and no ok button --%>

<tiles:importAttribute name="cancelOnly" ignore="true"/>
<tiles:importAttribute name="okAssignBtn" ignore="true"/>
<tiles:importAttribute name="okAssignOnly" ignore="true"/>

<script language="JavaScript" type="text/javascript">
  var isButtonClicked = false;

  function checkSubmit() {
    if (isButtonClicked) {
      alert('<fmt:message key="error.PreviousRequestEtc"/>');
      return false;
    }
  }  
</script>

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
<c:if test="${empty cancelOnly}">
 <c:if test="${empty okAssignOnly}">
          <td><html:image page="/images/fb_ok.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" onmouseover="imageSwap(this, imagePath + 'fb_ok', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_ok', '');" onmousedown="imageSwap(this, imagePath +  'fb_ok', '_down')" onclick="checkSubmit(); isButtonClicked=true;" tabindex="11"/></td>
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
 </c:if>
 <c:choose>
  <c:when test="${not empty okAssignBtn}">
        <td><html:image page="/images/${okAssignBtn}.gif" border="0" titleKey="FormButtons.ClickToAssignToRoles" 
                property="okassign" onmouseover="imageSwap(this, imagePath + '${okAssignBtn}', '_over');" 
                onmouseout="imageSwap(this, imagePath + '${okAssignBtn}', '');" 
                onmousedown="imageSwap(this, imagePath + '${okAssignBtn}', '_down')" onclick="checkSubmit(); isButtonClicked=true;" tabindex="11"/></td>
  </c:when>
  <c:otherwise>
        <td><html:image page="/images/fb_okassign.gif" border="0" titleKey="FormButtons.ClickToAssignToRoles" 
                property="okassign" onmouseover="imageSwap(this, imagePath + 'fb_okassign', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_okassign', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_okassign', '_down')" onclick="checkSubmit(); isButtonClicked=true;" tabindex="11"/></td>
  </c:otherwise>
 </c:choose>
		  <td><html:image page="/images/fb_reset.gif" border="0" titleKey="FormButtons.ClickToReset" property="reset" 
                onmouseover="imageSwap(this, imagePath + 'fb_reset', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_reset', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_reset', '_down')" tabindex="12"/></td>
</c:if>
          <td><html:image page="/images/fb_cancel.gif" border="0" titleKey="FormButtons.ClickToCancel" 
                property="cancel" onmouseover="imageSwap(this, imagePath + 'fb_cancel', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_cancel', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_cancel', '_down')" tabindex="13"/></td>
		  <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<!-- /  -->
