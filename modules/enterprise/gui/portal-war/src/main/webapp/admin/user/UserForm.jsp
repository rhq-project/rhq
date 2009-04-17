<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="User" ignore="true"/>
<tiles:importAttribute name="mode" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="${param.mode}"/>
</c:if>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants" symbol="MODE_NEW" var="MODE_NEW"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants" symbol="MODE_REGISTER" var="MODE_REGISTER"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants" symbol="MODE_VIEW" var="MODE_VIEW"/>
<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants" symbol="MODE_EDIT" var="MODE_EDIT"/>

<%
  int textBoxSize;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE"))
    textBoxSize = 12;
  else
    textBoxSize = 14;
%>

<c:set var="textBoxSize">
<%= textBoxSize %>
</c:set>

<tiles:insert definition=".portlet.error"/>
<logic:messagesPresent property="exception.user.alreadyExists">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
    <td class="ErrorBlock" width="100%"><html:errors property="exception.user.alreadyExists"/></td>
    <td class="ErrorBlock" width="100%">
  </tr>
</table>
</logic:messagesPresent>
<!-- CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr class="BlockContent">  
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="common.label.Name"/>
  </td>
  <td width="30%" rowspan="2" class="BlockContent">
   <table cellpadding="0" cellspacing="0" border="0">
    <tr>
     <td>
      <fmt:message key="admin.user.generalProperties.First"/>
     </td>
     <td>
      <html:img page="/images/spacer.gif" width="5" height="1" border="0"/>
     </td>
     <td>
      <fmt:message key="admin.user.generalProperties.Last"/>
     </td>
    </tr>
    <tr>
     <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="5" border="0"/></td>
    </tr>
    <tr>
     <logic:messagesPresent property="firstName">
      <td class="ErrorField">
       <html:text size="${textBoxSize}" maxlength="50" property="firstName" tabindex="1"/><br>
        <span class="ErrorFieldContent">- <html:errors property="firstName"/></span>
      </td>
     </logic:messagesPresent>
     <logic:messagesNotPresent property="firstName">
      <td><html:text size="${textBoxSize}" maxlength="50" property="firstName" tabindex="1"/></td>
      </logic:messagesNotPresent>
      <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
      <logic:messagesPresent property="lastName">
        <td class="ErrorField">
          <html:text size="${textBoxSize}" maxlength="50" property="lastName" tabindex="2"/><br>
          <span class="ErrorFieldContent">- <html:errors property="lastName"/></span>
	</td>
	</logic:messagesPresent>
	<logic:messagesNotPresent property="lastName">
	<td><html:text size="${textBoxSize}" maxlength="50" property="lastName" tabindex="2"/></td>
	</logic:messagesNotPresent>
      </tr>
      </table>
    </td>
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="admin.user.generalProperties.Username"/></td>
   <c:choose>
   <c:when test="${mode eq MODE_EDIT || mode eq MODE_REGISTER}">
    <td width="30%" class="BlockContent"><c:out value="${User.name}"/><br>
     <c:if test="${mode eq MODE_EDIT}">
    <html:hidden property="name"/>
     </c:if> 
    </td>
   </c:when>
   <c:otherwise>   
    <logic:messagesPresent property="name">
     <td width="30%" class="ErrorField">
      <html:text size="31" maxlength="40" property="name" tabindex="8"/><br>
      <span class="ErrorFieldContent">- <html:errors property="name"/></span>
     </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="name">
     <td width="30%" class="BlockContent">
      <html:text size="31" maxlength="40" property="name" tabindex="8"/>
     </td>
    </logic:messagesNotPresent>
   </c:otherwise>
   </c:choose>
    </tr>
    <tr>
     <td width="20%" class="BlockLabel">&nbsp;</td>
     <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Phone"/></td>
     <logic:messagesPresent property="phoneNumber">
      <td width="30%" class="ErrorField"><html:text size="31" maxlength="50" property="phoneNumber" tabindex="9"/><br><span class="ErrorFieldContent">- <html:errors property="phoneNumber"/></span></td>
     </logic:messagesPresent>
     <logic:messagesNotPresent property="phoneNumber">
      <td width="30%" class="BlockContent"><html:text size="31" maxlength="50" property="phoneNumber" tabindex="9"/></td>
     </logic:messagesNotPresent>	    
    </tr>
    <tr>
     <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="admin.user.generalProperties.Email"/></td>
      <logic:messagesPresent property="emailAddress">
        <td width="30%" class="ErrorField">
         <html:text size="31" property="emailAddress" tabindex="3"/><br>
         <span class="ErrorFieldContent">- <html:errors property="emailAddress"/></span>
        </td>
       </logic:messagesPresent>
       <logic:messagesNotPresent property="emailAddress">
        <td width="30%" class="BlockContent"><html:text size="31" property="emailAddress" tabindex="3"/></td>
       </logic:messagesNotPresent>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Department"/></td>
      <td width="30%" class="BlockContent"><html:text size="31" maxlength="50" property="department" tabindex="10"/></td>
    </tr>  
  <c:choose>
   <c:when test="${mode eq MODE_NEW}">
    <tr>
     <td colspan="4">
      <c:set var="tmpu" value="${param.u}" />
      <tiles:insert page="/admin/user/UserPasswordForm.jsp">
       <tiles:put name="userId" beanName="tmpu"/>  
      </tiles:insert>
     </td>
    </tr>
   </c:when>
   <c:when test="${mode eq MODE_EDIT and User.hasPrincipal}">
   <tr>
     <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="common.label.Password"/></td>
      <td width="30%" class="BlockContent"><span class="CaptionText">
	<fmt:message key="admin.user.generalProperties.ReturnTo"/>
	<html:link page="/admin/user/UserAdmin.do?mode=${MODE_VIEW}&u=${param.u}">
	 <fmt:message key="admin.user.generalProperties.ViewUser"/>
	</html:link>
	<fmt:message key="admin.user.generalProperties.ToAccess"/></span>
      </td>
      <td width="20%" class="BlockLabel">&nbsp;</td>
      <td width="30%" class="BlockContent">&nbsp;</td>
	</tr>	 
    </c:when>
  </c:choose>
  <c:if test="${mode ne MODE_REGISTER}">
   <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.EnableLogin"/></td>
    <td width="30%" class="BlockContent">
     <c:choose>
      <c:when test="${empty param.enableLogin}">
	<input type="radio" name="enableLogin" value="yes" checked="checked" tabindex="6"/>
      </c:when>
      <c:otherwise>
       <html:radio property="enableLogin" value="yes" tabindex="6"/>
      </c:otherwise>
     </c:choose>
     <fmt:message key="admin.user.generalProperties.enableLogin.Yes"/><br>
     <html:radio property="enableLogin" value="no" tabindex="7"/>
     <fmt:message key="admin.user.generalProperties.enableLogin.No"/>
    </td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
    <!-- 
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.smsAddress"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" maxlength="50" property="smsAddress" tabindex="10"/></td>
      -->
   </tr>
  </c:if>
   <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<c:if test="${EditUserForm.editingCurrentUser}">         
	<tiles:insert definition=".header.tab">  
	  <tiles:put name="tabKey" value="admin.user.mypreferences"/>  
	</tiles:insert>
	<html:hidden property="editingCurrentUser"/>
	<!-- CONTENTS -->
	<table width="100%" cellpadding="0" cellspacing="0" border="0">
	 
	  
	    <tr>
	       <td width="20%" class="BlockLabel"><fmt:message key="admin.user.mypreferences.pageRefreshPeriod"/></td>
	       <logic:messagesPresent property="pageRefreshPeriod">
	        <td width="10%" class="ErrorField">
	         <html:text size="31" property="pageRefreshPeriod" tabindex="11"/><br>
	         <span class="ErrorFieldContent">- <html:errors property="pageRefreshPeriod"/></span>
	        </td>
	       </logic:messagesPresent>
	       <logic:messagesNotPresent property="pageRefreshPeriod">
	        <td width="10%" class="BlockContent"><html:text size="31" property="pageRefreshPeriod" tabindex="11"/></td>
	       </logic:messagesNotPresent>
	       <td width="60%" class="BlockLeftAlignLabel">
			<fmt:message key="admin.user.mypreferences.pageRefreshPeriod.descr"/>
	       </td>
		   <td width="10%" class="BlockContent">&nbsp;</td>
	    </tr>  
	    <tr>
	    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
	    </tr>
        <tr>
           <td width="20%" class="BlockLabel"><fmt:message key="admin.user.mypreferences.groupConfigurationTimeout"/></td>
           <logic:messagesPresent property="groupConfigurationTimeout">
            <td width="10%" class="ErrorField">
             <html:text size="31" property="groupConfigurationTimeout" tabindex="11"/><br>
             <span class="ErrorFieldContent">- <html:errors property="groupConfigurationTimeout"/></span>
            </td>
           </logic:messagesPresent>
           <logic:messagesNotPresent property="groupConfigurationTimeout">
            <td width="10%" class="BlockContent"><html:text size="31" property="groupConfigurationTimeout" tabindex="11"/></td>
           </logic:messagesNotPresent>
           <td width="60%" class="BlockLeftAlignLabel">
            <fmt:message key="admin.user.mypreferences.groupConfigurationTimeout.descr"/>
           </td>
           <td width="10%" class="BlockContent">&nbsp;</td>
        </tr>  
	
	   <tr>
	    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
	  </tr>
	</table>
</c:if>

<c:if test="${mode eq MODE_EDIT}">         
 <html:hidden name="User" property="id" />
 <html:hidden property="u" value="${param.u}" />
</c:if>
