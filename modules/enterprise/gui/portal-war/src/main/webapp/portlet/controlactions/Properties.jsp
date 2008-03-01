<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script type="text/javascript">
  var help = "<hq:help/>";
</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PageTitle"><fmt:message key="dash.home.ControlActions.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><%--<html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link>--%><html:img page="/images/spacer.gif" width="20" height="20" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan="2">
      <html:form action="/dashboard/ModifyControlActions" >

      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>

      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>

      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr valign="top">
          <td width="20%" class="BlockLabel" rowspan="3"><fmt:message key="dash.settings.FormLabel.ControlRange"/></td>
          <td width="5%" class="BlockContent" nowrap>
             <table><tr><td valign="middle"><html:checkbox property="useLastCompleted"/></td>
                    <td valign="middle"><fmt:message key="dash.settings.controlActions.last"/></td></tr></table>
          </td>
          <td width="75%" class="BlockContent">
            <table><tr><td valign="middle">
               <html:select property="lastCompleted">
                 <html:option value="1">1</html:option>
                 <html:option value="5">5</html:option>
                 <html:option value="10">10</html:option>   
                 <html:option value="15">15</html:option>
               </html:select></td>
               <td valign="middle"><fmt:message key="dash.settings.controlActions.completed"/><br></td></tr></table>
          </td>
        </tr>
        <tr>
          <td width="5%" class="BlockContent" nowrap>
             <table><tr><td valign="middle"><html:checkbox property="useNextScheduled"/></td>
                    <td valign="middle"><fmt:message key="dash.settings.controlActions.next"/></td></tr></table>
          <td width="75%" class="BlockContent">
             <table><tr><td valign="middle">
                <html:select property="nextScheduled"  >
                   <html:option value="1">1</html:option>
                   <html:option value="5">5</html:option>
                   <html:option value="10">10</html:option>   
                   <html:option value="15">15</html:option>
                </html:select></td>
                <td valign="middle"><fmt:message key="dash.settings.controlActions.scheduled"/><br></td></tr></table>
          </td>
        </tr>
        <tr>
          <td colspan="3" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
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
