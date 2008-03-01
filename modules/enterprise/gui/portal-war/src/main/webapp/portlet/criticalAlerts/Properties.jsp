<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:set var="widgetInstanceName" value="criticalAlertsList"/>
<c:url var="selfAction"         value="/dashboard/Admin.do?mode=criticalAlerts"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
var help = '<hq:help/>';
</script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="34%" class="PageTitle"><fmt:message key="dash.home.RecentAlerts.Title"/></td>
    <td width="33%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><%--<html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link>--%><html:img page="/images/spacer.gif" width="20" height="20" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td ><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='3'>
      <html:form action="/dashboard/ModifyCriticalAlerts" >

      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>

      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
         <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.AlertRange"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
            <fmt:message key="dash.settings.criticalAlerts.last"/>&nbsp;
            <html:select property="numberOfAlerts">
              <html:option value="5" >5</html:option>
              <html:option value="10">10</html:option> 
            </html:select> 
            <html:select property="priority">
              <html:option value="3">!!! - High</html:option>
              <html:option value="2">!! - Medium</html:option>
              <html:option value="1">! - Low</html:option>
              <html:option value="0">ALL</html:option>
            </html:select >
            &nbsp;<fmt:message key="dash.settings.criticalAlerts.withinThePast"/>

            <html:select property="past">     
              <html:option value="1800000"   >30 minutes</html:option>
              <html:option value="3600000"   >hour</html:option>
              <html:option value="43200000"  >12 hours</html:option>
              <html:option value="86400000"  >day</html:option>
              <html:option value="604800000" >week</html:option>
              <html:option value="2419200000">month</html:option>
            </html:select > 

            &nbsp;<fmt:message key="dash.settings.criticalAlerts.for"/>&nbsp;      
            <html:select property="selectedOrAll">
              <html:option value="selected">selected resources</html:option>
              <html:option value="all"     >all resources</html:option>
            </html:select >
            &nbsp;<fmt:message key="dash.settings.criticalAlerts.period"/>
          </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
      &nbsp;<br>
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.SelectedResources"/>
      </tiles:insert>

      <display:table cellspacing="0" cellpadding="0" width="100%" 
                     action="${selfAction}"
                     items="${criticalAlertsList}" 
                     var="resource">

        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">          
          <display:checkboxdecorator name="ids" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>
      
        <display:column width="50%" property="name"        sortAttr="res.name"        title="dash.settings.ListHeader.Resource" /> 
        <display:column width="50%" property="description" sortAttr="res.description" title="common.header.Description" /> 

      </display:table>

      <tiles:insert definition=".toolbar.addToList">
        <tiles:put name="addToListUrl" value="/dashboard/Admin.do?mode=crtAlertsAddResources&key=.dashContent.criticalalerts.resources"/>  
        <tiles:put name="pageList" beanName="criticalAlertsList"/>
        <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
        <tiles:put name="pageAction" beanName="selfAction"/>
      </tiles:insert>

      <tiles:insert definition=".form.buttons"/>
      </html:form>
      
    </td>
  </tr>
  <tr> 
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>

