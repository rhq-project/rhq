<%@ page language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!-- This page is not used anymore and will be DELETED eventually - use the menu bar instead -->
<!-- I'm afraid to delete this jsp because I don't know where else its accessed -->
<!-- but only let the users with MANAGE_SECURITY see it. More fine-grained -->
<!-- auth controls are placed over the menu bar. -->
<hq:authorization permission="MANAGE_SECURITY">

<!--  PAGE TITLE -->
<tiles:insert definition=".page.title.admin.admin">
  <tiles:put name="titleKey" value="admin.admin.AdministrationTitle"/>
  <tiles:put name="titleName" beanName="fullName"/>
</tiles:insert>
<!--  /  -->

<tiles:insert definition=".portlet.confirm"/>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.AuthAuthZTab"/>
</tiles:insert>
<!-- AUTH -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Users"/></td>
    <td width="30%" class="BlockContent"><html:link page="/admin/user/UserAdmin.do?mode=list"><fmt:message key="admin.home.ListUsers"/></html:link></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Roles"/></td>
    <td width="30%" class="BlockContent"><html:link page="/admin/role/RoleAdmin.do?mode=list"><fmt:message key="admin.home.ListRoles"/></html:link></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent"><html:link page="/admin/user/UserAdmin.do?mode=new"><fmt:message key="admin.home.NewUser"/></html:link></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent"><html:link page="/admin/role/RoleAdmin.do?mode=new"><fmt:message key="admin.home.NewRole"/></html:link></td>
  </tr>
</table>
<!--  /  -->

<!--  some empty space -->
<br>
<br>
<!--  /  -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.CompanyWideSettingsTab"/>
</tiles:insert>

<!--  SERVER SETTINGS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Settings"/></td>
      <td width="30%" class="BlockContent"><html:link page="/admin/config/Config.do?mode=edit"><fmt:message key="admin.home.ServerConfig"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.home.MonitoringDefaults"/></td>
      <td width="30%" class="BlockContent">
         <html:link page="/admin/config/EditDefaults.do?mode=monitor&viewMode=existing"><fmt:message key="admin.home.MonitoringDefaults.existing"/></html:link>
          &nbsp;|&nbsp;
         <html:link page="/admin/config/EditDefaults.do?mode=monitor&viewMode=all"><fmt:message key="admin.home.MonitoringDefaults.all"/></html:link>
      </td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.home.License"/></td>
      <td width="30%" class="BlockContent"><html:link page="/admin/license/LicenseAdmin.do?mode=view"><fmt:message key="admin.home.LicenseManager"/></html:link></td>
   </tr>

   <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Plugins"/></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/admin/plugin/plugin-list.xhtml"><fmt:message key="admin.home.PluginsLink"/></html:link></td>
   </tr>

<%--   Disabled for Beta1, See JBNADM-2351
   <tr>
      <td width="20%" class="BlockLabel"><fmt:message key="admin.home.LargeEnvironment"/></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/admin/largeenv.xhtml"><fmt:message key="admin.home.LargeEnvironmentLink"/></html:link></td>
   </tr>
--%>   
</table>
<!--  /  -->


<!--  some empty space -->
<br>
<br>
<!--  /  -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.Content"/>
</tiles:insert>

<!--  CONTENT SOURCES AND CHANNELS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/content/listContentSources.xhtml"><fmt:message key="admin.home.Content.ListContentSources"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/content/listChannels.xhtml"><fmt:message key="admin.home.Content.ListChannels"/></html:link></td>
   </tr>
</table>
<!--  /  -->

<!--  some empty space -->
<br>
<br>
<!--  /  -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.Ha"/>
</tiles:insert>

<!--  HIGH AVAILABILITY -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/ha/listServers.xhtml"><fmt:message key="admin.home.Ha.ListServers"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/ha/listAgents.xhtml"><fmt:message key="admin.home.Ha.ListAgents"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/ha/listAffinityGroups.xhtml"><fmt:message key="admin.home.Ha.ListAffinityGroups"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/rhq/ha/listPartitionEvents.xhtml"><fmt:message key="admin.home.Ha.ListPartitionEvents"/></html:link></td>
   </tr>
</table>
<!--  /  -->

<!--  some empty space -->
<br>
<br>
<!--  /  -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.AgentUpdateBinary"/>
</tiles:insert>

<!--  AGENT -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/agentupdate/version"><fmt:message key="admin.home.AgentUpdateBinary.version"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/agentupdate/download"><fmt:message key="admin.home.AgentUpdateBinary.download"/></html:link></td>
   </tr>
   <tr>
      <td width="20%" class="BlockLabel"></td>
      <td width="30%" class="BlockContent"><html:link page="/downloads"><fmt:message key="admin.home.AgentUpdateBinary.connector-downloads"/></html:link></td>
   </tr>
</table>
<!--  /  -->

</hq:authorization>

<tiles:insert definition=".page.footer"/>
