<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<hq:constant classname="org.rhq.enterprise.gui.legacy.Constants"
                symbol="CONTROL_ACTION_NONE"
                   var="CONTROL_ACTION_NONE"/>

<html:form action="/alerts/EditControlAction">

<c:choose>
  <c:when test="${not empty Resource}">
    <html:hidden property="id" value="${Resource.id}"/>
  </c:when>
  <c:otherwise>
    <html:hidden property="type" value="${ResourceType.id}"/>
  </c:otherwise>
</c:choose>
<html:hidden property="ad"/>
<html:hidden property="id"/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.props.Control.NewActionTitle"/>
</tiles:insert>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.Control"/>
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.ControlType"/>
    </td>
    <logic:messagesPresent property="controlAction">
    <td width="80%" class="ErrorField">
      <html:select property="controlAction">
        <html:option value="" key="alerts.dropdown.SelectOption"/>
        <html:optionsCollection property="controlActions"/>
      </html:select>
      <span class="ErrorFieldContent">- <html:errors property="controlAction"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="controlAction">
    <td width="80%" class="BlockContent">
      <html:select property="controlAction">
        <html:option value="${CONTROL_ACTION_NONE}" key="alert.config.props.ControlType.none"/>
        <html:optionsCollection property="controlActions"/>
      </html:select>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
    </td>
  </tr>  
  <tiles:insert definition=".events.config.template.cascade"/>
</table>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

</html:form>
