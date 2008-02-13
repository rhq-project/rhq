<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" type="text/javascript">
function checkGroup() {
  if (document.ResourceHubForm.ff.selectedIndex == 4)
    document.ResourceHubForm.g.value = 2;
  else
    document.ResourceHubForm.g.value = 1;
}
</script>

<div class="effectsPortlet">
<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.SearchResources"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>
<!-- fixme: there's no "minimize" functionality on this block, only "close" -->
<html:form action="/resource/hub/ResourceHub" onsubmit="checkGroup()">
<html:hidden property="g" value="1"/>
<!-- Content Block Contents -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockContent" colspan="3"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td class="BlockContent" nowrap valign="center">
      <input type="text" size="12" maxlength="40" value="<fmt:message key="common.header.ResourceName"/>" onfocus="ClearText(this)" name="keywords">      
    </td>
    <td class="BlockContent" nowrap valign="center">
      <html:select property="resourceCategory" styleClass="FilterFormText" size="1" >
        <hq:optionMessageList property="functions" baseKey="resource.hub.filter"/>        
      </html:select>
    </td>
    <td width="100%" class="BlockContent" valign="center"><html:image page="/images/dash-button_go-arrow.gif" border="0" property="ok" title="search" alt="search"/></td>
  </tr>                                                         
  <tr>
    <td class="BlockContent" colspan="3"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</html:form>
</div>
