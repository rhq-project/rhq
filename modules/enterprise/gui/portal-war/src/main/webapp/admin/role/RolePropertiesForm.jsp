<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!-- RolePropertiesForm.jsp -->

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="role" ignore="true"/>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.props.PropertiesAndPermissionsTab"/>
</tiles:insert>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<!--  GENERAL PROPERTIES -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" height="9" width="9" border="0"/><fmt:message key="common.label.Name"/></td>
<c:choose>
  <c:when test="${mode eq 'view'}">
    <td width="30%" class="BlockContent">
      <c:out value="${role.name}"/>
    </td>
  </c:when>
  <c:otherwise>
    <logic:messagesPresent property="name">
    <td width="30%" class="ErrorField">
      <html:text size="30" maxlength="40" property="name"/><br>
      <span class="ErrorFieldContent">- <html:errors property="name"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="name">
    <td width="30%" class="BlockContent">
      <html:text size="30" maxlength="40" property="name"/><br>
    </td>
    </logic:messagesNotPresent>
  </c:otherwise>
</c:choose>
    <td width="20%" class="BlockLabel"></td>
    <td width="30%" class="BlockContent">
    </td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>
<c:choose>
  <c:when test="${mode eq 'view'}">
    <td width="30%" class="BlockContent">
      <c:out value="${role.description}"/>
    </td>
  </c:when>
  <c:otherwise>
    <logic:messagesPresent property="description">
    <td width="30%" class="ErrorField">
      <html:textarea cols="35" rows="3" property="description"/><br>
      <span class="ErrorFieldContent">- <html:errors property="description"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="description">
    <td width="30%" class="BlockContent">
      <html:textarea cols="35" rows="3" property="description"/><br>
      <span class="CaptionText"><fmt:message key="admin.role.props.100"/></span>
    </td>
    </logic:messagesNotPresent>
  </c:otherwise>
</c:choose>
  </tr>
</table>
<!--  /  -->
