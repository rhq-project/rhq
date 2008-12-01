<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" src="<html:rewrite page="/js/"/>dashboard_SummaryCounts.js" type="text/javascript"></script>
<script type="text/javascript">
  var help = '<hq:help/>';
</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PageTitle"><fmt:message key="dash.home.SummaryCounts.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><%--<html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link>--%><html:img page="/images/spacer.gif" width="20" height="20" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan='2'><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
    <html:form action="/portlet/admin/ModifySummaryCounts.do">

    <!-- Content Block Title: Display Settings -->
    <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
    </tiles:insert>

    <tiles:insert definition=".dashContent.admin.generalSettings">
      <tiles:put name="portletName" beanName="portletName" />
    </tiles:insert>
    <!-- Display Settings Content: the text is static, all boxes should be checked by default -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
      </tr>
       <tr valign="top">
        <td width="20%" class="BlockLabel"><fmt:message key="dash.settings.FormLabel.SummaryCounts"/></td>
        <td width="30%" class="BlockContent">                

          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="platform" styleClass="platformParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.PlatformShowTotal"/></td>
            </tr>
          </table>
                    
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="server" styleClass="serverParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.ServerShowTotal"/></td>
            </tr>
          </table>
                      
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="service" styleClass="serviceParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.ServiceShowTotal"/></td>
            </tr>
          </table>
            
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="groupCompat" styleClass="clusterParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.group.CompatGroupsShowTotal"/></td>
            </tr>              
          </table>            
            
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="groupMixed" styleClass="groupMixedParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.group.mixedGroups"/></td>
            </tr>              
          </table>
            
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="groupDefinition" styleClass="groupDefinitionParent" onclick="checkParent(this)"/><fmt:message key="dash.home.DisplayCategory.group.GroupDefinitionShowTotal"/></td>
            </tr>
          </table>

          </td>
          <td width="50%" class="BlockLabel">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
      </tr>
    </table>
    <tiles:insert definition=".form.buttons"/>
    </html:form>
    </td>
  </tr>
  <tr> 
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
