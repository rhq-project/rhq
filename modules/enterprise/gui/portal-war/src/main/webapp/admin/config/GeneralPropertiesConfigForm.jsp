<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="DAYS_LABEL" var="CONST_DAYS" />

<logic:messagesPresent>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorField"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td align="left" class="ErrorField"><html:errors/></td>
  </tr>
</table>
</logic:messagesPresent>

<!--  BASE SERVER CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaseConfigTab"/>
</tiles:insert>
<!--  /  -->

<!--  BASE SERVER CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="45%" class="BlockLabel"><fmt:message key="admin.settings.BaseURL"/></td>
    <td width="55%" class="BlockContent"><html:text size="31" property="baseUrl" /></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AgentMaxQuietTimeAllowed"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="agentMaxQuietTimeAllowedVal">
          <td class="ErrorField">
            <html:text size="2" property="agentMaxQuietTimeAllowedVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="agentMaxQuietTimeAllowed">
              <html:option value="${CONST_MINUTES}"><fmt:message key="admin.settings.Minutes"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="agentMaxQuietTimeAllowedVal">
          <td class="BlockContent">
            <html:text size="2" property="agentMaxQuietTimeAllowedVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="agentMaxQuietTimeAllowed">
              <html:option value="${CONST_MINUTES}"><fmt:message key="admin.settings.Minutes"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="agentMaxQuietTimeAllowedVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="agentMaxQuietTimeAllowedVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="agentMaxQuietTimeAllowedVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.EnableAgentAutoUpdate"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td align="left"><html:radio property="enableAgentAutoUpdate" value="true"/><fmt:message key="yesno.true"/></td>
          <td align="left"><html:radio property="enableAgentAutoUpdate" value="false"/><fmt:message key="yesno.false"/></td>
        </tr>
      </table>
    </td>
  </tr>

</table>
