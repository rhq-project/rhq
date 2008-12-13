<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant classname="org.rhq.enterprise.server.RHQConstants" symbol="JDBCJAASProvider" var="camProvider"/>
<hq:constant classname="org.rhq.enterprise.server.RHQConstants" symbol="LDAPJAASProvider" var="ldapProvider"/>

<hq:config var="ldapAuth" prop="CAM_JAAS_PROVIDER" value="${ldapProvider}"/>
<hq:config var="camAuth" prop="CAM_JAAS_PROVIDER" value="${camProvider}"/>

<!--  LDAP CONFIG PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.SNMPConfigPropTab"/>  
</tiles:insert>
<!--  /  -->

<script>
  function initSNMPForm() {
	var form = document.forms['SystemConfigForm'];
    snmpVersionChange(form.snmpVersion);
  }

  onloads.push( initSNMPForm );

  function snmpVersionChange(e) {
    showSnmpDiv(e.value);
  }

  function showSnmpDiv(v) {
	var allDiv = document.getElementById('snmpopts');
	var v1Div  = document.getElementById('snmpv1opts');
	var v2Div  = document.getElementById('snmpv1v2opts');
	var v3Div  = document.getElementById('snmpv3opts');

    if (v == '') {
        v1Div.style.display  = 'none';
        v2Div.style.display  = 'none';
        v3Div.style.display  = 'none';
        allDiv.style.display = 'none';
        return;
    }

    allDiv.style.display = '';

    if (v == '3') {
        v2Div.style.display = 'none';
        v3Div.style.display = '';
    }
    else {
        v2Div.style.display = '';
        v3Div.style.display = 'none';
    }

    if (v == '1') {
        v1Div.style.display = '';
    }
    else {
        v1Div.style.display = 'none';
    }
  }

</script>

<!--  LDAP CONFIG PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockCheckboxLabel" align="left" colspan="4"><fmt:message key="admin.settings.SNMPVersion"/>
    <html:select property="snmpVersion" onchange="snmpVersionChange(this)">
      <html:option key="admin.settings.SNMPNone" value=""/>
      <html:option value="3"/>
      <html:option value="2c"/>
      <html:option value="1"/>
    </html:select>
    </td>
  </tr>
</table>

<div id="snmpopts" style="position: relative;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="4" class="BlockBottomLine"><div style="width: 1px; height: 1px;"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPTrapOID"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpTrapOID"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
</table>
</div>

<div id="snmpv3opts" style="position: relative;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPAuthProtocol"/></td>
    <td width="30%" class="BlockContent">
      <html:select property="snmpAuthProtocol">
        <html:option value="" key="common.label.None"/>
        <html:option value="MD5"/>
        <html:option value="SHA"/>
      </html:select>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPAuthPassphrase"/></td>
    <td width="30%" class="BlockContent"><html:password size="31" property="snmpAuthPassphrase" redisplay="true"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPPrivProtocol"/></td>
    <td width="30%" class="BlockContent">
      <html:select property="snmpPrivacyProtocol">
        <html:option value="" key="common.label.None"/>
        <html:option value="DES"/>
        <html:option value="AES"/>
        <html:option value="AES192"/>
        <html:option value="AES256"/>
      </html:select>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPPrivPassphrase"/></td>
    <td width="30%" class="BlockContent"><html:password size="31" property="snmpPrivacyPassphrase" redisplay="true"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPContextName"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpContextName"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPSecurityName"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpSecurityName"/></td>
  </tr>
</table>
</div>

<div id="snmpv1v2opts" style="position: relative;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPCommunity"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpCommunity"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
</table>
</div>

<div id="snmpv1opts" style="position: relative;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPEngineID"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpEngineID"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>

  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPGenericID"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpGenericID"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPSpecificID"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpSpecificID"/></td>
  </tr>

  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPEnterpriseOID"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpEnterpriseOID"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.SNMPAgentAddress"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="snmpAgentAddress"/></td>
  </tr>
</table>
</div>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockBottomLine"><div style="width: 1px; height: 1px;"/></td>
  </tr>
</table>
<!--  /  -->
