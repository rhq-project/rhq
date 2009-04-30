<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:form action="/alerts/EditSyslogAction">

<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>
<html:hidden property="ad"/>
<html:hidden property="id"/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.props.Syslog.AddActionTitle"/>
</tiles:insert>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.Syslog.Title"/>
</tiles:insert>

<script language="JavaScript" src="<html:rewrite page='/js/alertConfigFunctions.js'/>" type="text/javascript"></script>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.MetaProject"/>:
    </td>
    <logic:messagesPresent property="metaProject">
    <td width="80%" class="ErrorField">
      <html:text size="30" property="metaProject"/>
      <span class="ErrorFieldContent">- <html:errors property="metaProject"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="metaProject">
    <td width="80%" class="BlockContent">
      <html:text size="30" property="metaProject"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.Project"/>:
    </td>
    <logic:messagesPresent property="project">
    <td width="80%" class="ErrorField">
      <html:text size="30" property="project"/>
      <span class="ErrorFieldContent">- <html:errors property="project"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="project">
    <td width="80%" class="BlockContent">
      <html:text size="30" property="project"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.Version"/>:
    </td>
    <logic:messagesPresent property="version">
    <td width="80%" class="ErrorField">
      <html:text size="30" property="version"/>
      <span class="ErrorFieldContent">- <html:errors property="version"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="version">
    <td width="80%" class="BlockContent">
      <html:text size="30" property="version"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <c:if test="${(! empty EditAlertDefinitionSyslogActionForm.id) and (EditAlertDefinitionSyslogActionForm.id > 0)}">
  <tr valign="top">
    <td colspan="2" width="100%" class="BlockContent">
      <html:checkbox property="shouldBeRemoved" onclick="syslogFormEnabledToggle(this.form);"/>
      <fmt:message key="alert.config.props.Syslog.Dissociate"/>
    </td>
  </tr>
  </c:if>
  <tr>
    <td colspan="2" class="BlockBottomLine">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
    </td>
  </tr>
</table>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

</html:form>
