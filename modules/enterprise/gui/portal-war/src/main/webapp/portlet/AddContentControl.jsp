<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="availablePortlets"/>
<tiles:importAttribute name="wide"/>
<tiles:importAttribute name="portlets"/>

<c:if test="${wide}">
<script>
  function isWide(portlet) {
    <c:forEach var="portlet" items="${portlets}">
      if (portlet == '<c:out value="${portlet}"/>')
        return true;
    </c:forEach>
    return false;
  }
</script>
</c:if>

<c:choose>
<c:when test="${not empty availablePortlets }">
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet">
</c:when>
<c:otherwise>
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet" style="visibility: hidden">
</c:otherwise>
</c:choose>
<html:form action="/dashboard/AddPortlet">
<html:hidden property="wide" value="${wide}"/>
  <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tr valign="top">
      <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr valign="top">
      <td colspan="3"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr>
      <td colspan="3" class="FormLabel"><fmt:message key="dash.home.FormLabel.AddContent"/></td>
    </tr>
    <tr>
      <td valign="center">
        <select name="portlet">
          <option value="bad" SELECTED><fmt:message key="dash.home.AddContent.select"/></option>
          <c:forEach var="portlet" items="${availablePortlets}" >                                                
             <option value="<c:out value="${portlet}"/>"><fmt:message key="${portlet}" /></option>
          </c:forEach>           
        </select>
      </td>
      <td>&nbsp;</td>
      <td width="100%"><html:image page="/images/dash_movecontent_add.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" onmouseover="imageSwap(this, imagePath + 'dash_movecontent_add-on', '');" onmouseout="imageSwap(this, imagePath +  'dash_movecontent_add', '');" onmousedown="imageSwap(this, imagePath +  'dash_movecontent_add-off', '')" /></td>
    </tr>
  </table>
</html:form>
</div>
