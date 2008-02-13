<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>

<c:set var="entityId" value="${Resource.entityId}"/>

<tiles:importAttribute name="action" />

<c:set var="tmpTitle"> <fmt:message key="resource.group.QuickControl.Caption"/></c:set>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.QuickControl.Tab"/>
  <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>

<!--  GENERAL PROPERTIES CONTENTS -->

<html:form action="${action}">
<html:hidden property="type" value="${entityId.type}"/>
<html:hidden property="rid" value="${entityId.id}"/>
<html:hidden property="resourceType" value="${entityId.type}"/>
<html:hidden property="resourceId" value="${entityId.id}"/>
<html:hidden property="mode" value="${param.mode}"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="3" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
    <tr valign="top">
<c:choose>
 <c:when test="${QuickControlForm.numControlActions == 0}">
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Action"/></td>
  <td width="50%" class="ErrorField"><fmt:message key="resource.service.control.controllist.NoActions"/></td>
  <td width="30%" class="BlockContent">&nbsp;</td>
 </c:when>
 <c:otherwise>
      <td width="20%" class="BlockLabel"><fmt:message key="resource.group.QuickControl.Label.Action"/></td>
      <logic:messagesPresent property="resourceAction">
      <td width="5%" class="ErrorField">
       <html:select property="resourceAction">
        <html:option value="" key="resource.application.applicationProperties.Select"/>
        <html:optionsCollection property="controlActions" />
       </html:select>
       <span class="ErrorFieldContent">- <html:errors property="resourceAction"/></span>
      </td>
      </logic:messagesPresent>
      <logic:messagesNotPresent property="resourceAction">
      <td width="5%" class="BlockContent">
       <html:select property="resourceAction">
        <html:option value="" key="resource.application.applicationProperties.Select"/>
        <html:optionsCollection property="controlActions" />
       </html:select>
      </td>
     </logic:messagesNotPresent>
      <td width="75%" class="BlockContent">
       <html:image property="ok" page="/images/dash-button_go-arrow.gif" border="0"/>
      </td>
 </c:otherwise>
</c:choose>
    </tr>
    <tr valign="top">
      <td width="20%" class="BlockLabel">&nbsp;</td>
      <td width="80%" class="BlockContentSmallText" colspan="2">
       <fmt:message key="resource.group.QuickControl.Content.Caption"/>
      </td>
    </tr>
    <tr>
     <td colspan="3" class="BlockContent">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
     </td>
    </tr>
    <tr>
     <td colspan="3" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
</html:form>
<!--  /  -->
