<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="userId" ignore="true"/>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="common.label.Password"/>
   </td>
   <td width="30%" class="BlockContent">
    <c:if test="${not empty userId}">  
      <tiles:importAttribute name="administrator"/>
      <html:hidden property="id" value="${param.u}"/>
      <html:hidden property="u" value="${param.u}"/>         
      <c:choose>
        <c:when test="${administrator eq true}">        
          <html:hidden property="currentPassword" value="${param.u}"/>
        </c:when>      
        <c:when test="${administrator eq false}">  
          <fmt:message key="admin.user.changePassword.EnterCurrent"/><br>
          <input type="password" size="31" maxlength="40" name="currentPassword" tabindex="3"><br>
        </c:when>
      </c:choose>
    </c:if>
       
    <fmt:message key="admin.user.changePassword.EnterNew"/><br>
    <input type="password" size="31" maxlength="40" name="newPassword" tabindex="4"><br>
    <span class="CaptionText">
     <fmt:message key="admin.user.changePassword.NoSpaces"/><br>&nbsp;<br>
    </span>
    <fmt:message key="admin.user.changePassword.ConfirmNew"/><br>
    <input type="password" size="31" maxlength="40" name="confirmPassword" tabindex="5">
   </td>

   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="30%" class="BlockLabel">&nbsp;</td>    
  </tr>

  <%-- we need to display the yellow box below if there are password
       messages --%>
  <c:set var="passwordMessagesPresent" value="false"/>
  <logic:messagesPresent property="currentPassword">
  <c:set var="passwordMessagesPresent" value="true"/>
  </logic:messagesPresent>
  <logic:messagesPresent property="newPassword">
  <c:set var="passwordMessagesPresent" value="true"/>
  </logic:messagesPresent>
  <logic:messagesPresent property="confirmPassword">
  <c:set var="passwordMessagesPresent" value="true"/>
  </logic:messagesPresent>

  <c:if test="${passwordMessagesPresent}">
   <tr valign="top"> 
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="ErrorField">
     <span class="ErrorFieldContent">
      <logic:messagesPresent property="currentPassword">
       -<html:errors property="currentPassword"/><br>
      </logic:messagesPresent>
      <logic:messagesPresent property="newPassword">
       -<html:errors property="newPassword"/><br>
      </logic:messagesPresent>
      <logic:messagesPresent property="confirmPassword">
       -<html:errors property="confirmPassword"/><br>
      </logic:messagesPresent>
     </span>
    </td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockLabel">&nbsp;</td>    
   </tr> 
  </c:if>

</table>
<!--  /  -->
