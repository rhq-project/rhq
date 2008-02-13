<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="addToList" ignore="true"/>
<tiles:importAttribute name="cancelOnly" ignore="true"/>
<tiles:importAttribute name="noReset" ignore="true"/>
<tiles:importAttribute name="noCancel" ignore="true"/>
<tiles:importAttribute name="done" ignore="true"/>

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
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr align=left valign=bottom>
<c:choose>
  <c:when test="${not empty addToList}">
    <td width="50%">&nbsp;</td>
    <td><html:img page="/images/spacer.gif" width="50" height="1" border="0"/></td>
    <td width="50%">
  </c:when>
  <c:otherwise>
    <td width="20%">&nbsp;</td>
		<td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td width="80%">
  </c:otherwise>
</c:choose>
      <table width="100%" cellpadding="0" cellspacing="7" border="0">
        <tr>
<c:if test="${empty cancelOnly}">
          <td><html:image page="/images/fb_ok.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" onmouseover="imageSwap(this, imagePath + 'fb_ok', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_ok', '');" onmousedown="imageSwap(this, imagePath +  'fb_ok', '_down')" onclick="checkSubmit(); isButtonClicked=true;"/></td>

<c:if test="${empty noReset}">
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
          <td><html:image page="/images/fb_reset.gif" border="0" titleKey="FormButtons.ClickToReset" property="reset" onmouseover="imageSwap(this, imagePath + 'fb_reset', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_reset', '');" onmousedown="imageSwap(this, imagePath + 'fb_reset', '_down')"/></td>
</c:if>          
</c:if>
<c:if test="${empty noCancel}">
          <td><html:image page="/images/fb_cancel.gif" border="0" titleKey="FormButtons.ClickToCancel" property="cancel" onmouseover="imageSwap(this, imagePath + 'fb_cancel', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_cancel', '');" onmousedown="imageSwap(this, imagePath + 'fb_cancel', '_down')"/></td>
</c:if>
<%-- Done is another way to say Ok, I am done and mapps to the ok ImageButton in the form. --%>
<c:if test="${not empty done}">        
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
          <td><html:image page="/images/fb_done.gif" border="0" titleKey="FormButtons.ClickWhenDone" property="ok" onmouseover="imageSwap(this, imagePath + 'fb_done', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_done', '');" onmousedown="imageSwap(this, imagePath + 'fb_done', '_down')"/></td>
</c:if>          
		  <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<!-- /  -->
