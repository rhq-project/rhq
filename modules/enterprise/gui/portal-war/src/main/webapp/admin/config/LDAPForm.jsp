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
  <tiles:put name="tabKey" value="admin.settings.LDAPConfigPropTab"/>  
</tiles:insert>
<!--  /  -->

<!--  LDAP CONFIG PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockCheckboxLabel" align="left" colspan="4"><html:checkbox property="ldapEnabled" /><fmt:message key="admin.settings.UseLDAPAuth"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPUrlLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapUrl"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSslLabel"/></td>
    <td width="30%" class="BlockContent"><html:checkbox property="ldapSsl"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPUsernameLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapUsername"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Password"/></td>
    <td width="30%" class="BlockContent"><html:password size="31" property="ldapPassword" redisplay="true"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSearchBaseLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapSearchBase"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSearchFilterLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapSearchFilter"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPLoginPropertyLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapLoginProperty"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>

  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
