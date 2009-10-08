<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.Control.Properties.Title"/>
</tiles:insert>
<!--  /  -->
<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>
<!-- CONSTANT DEFINITIONS -->

<c:set var="formBean" value="${requestScope[\"org.apache.struts.taglib.html.BEAN\"]}"/>
<c:set var="instance" value="${requestScope[\"org.apache.struts.action.mapping.instance\"]}"/>
<c:set var="formName" value="${instance.name}"/>

<script src="<html:rewrite page="/js/"/>control_ControlActionProperties.js" type="text/javascript"></script>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="resource.server.Control.Properties.Label.ControlAction"/>
 </td>
<c:choose>
 <c:when test="${formBean.numControlActions == 0}">
 <td width="30%" class="BlockContent">
  <fmt:message key="resource.service.control.controllist.NoActions"/>
 </td>
 </c:when>
 <c:otherwise>
  <logic:messagesPresent property="controlAction">
  <td width="30%" class="ErrorField">
   <html:select property="controlAction">
    <html:option value="" key="resource.application.applicationProperties.Select"/>
    <html:optionsCollection property="controlActions" />
   </html:select>
     <span class="ErrorFieldContent">- <html:errors property="controlAction"/></span>
  </td>
  </logic:messagesPresent>
  <logic:messagesNotPresent property="controlAction">
  <td width="30%" class="BlockContent">
   <html:select property="controlAction">
    <html:option value="" key="resource.application.applicationProperties.Select"/>
    <html:optionsCollection property="controlActions" />
   </html:select>
  </td>
  </logic:messagesNotPresent>
  </c:otherwise>
 </c:choose>
  <td width="20%" class="BlockLabel">
   <fmt:message key="common.label.Description"/>
  </td>
  <logic:messagesPresent property="description">
    <td width="30%" class="ErrorField">
     <html:textarea cols="35" rows="3" property="description" />
      <span class="ErrorFieldContent">- <html:errors property="description"/></span>
    </td>
  </logic:messagesPresent>
  <logic:messagesNotPresent property="description">
    <td width="30%" class="BlockContent">
     <html:textarea cols="35" rows="3" property="description" /> 
    </td>
  </logic:messagesNotPresent>
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
 </tr>
</table>
