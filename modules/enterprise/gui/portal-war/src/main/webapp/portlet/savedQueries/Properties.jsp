<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="savedQueriesList"/>
<c:url var="selfAction" value="/dashboard/Admin.do?mode=savedQueries"/>

<script type="text/javascript">
  var help = "<hq:help/>";
</script>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listRoles"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PageTitle"><fmt:message key="dash.home.SavedQueries.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
      <html:form action="/dashboard/ModifySavedQueries.do" >
      <!-- Content Block Title: Display Settings -->
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>

      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>

      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.SelectedCharts"/>
      </tiles:insert>
           
      <table class="table" width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr class="tableRowHeader">
          <th width="1%" class="ListHeaderCheckbox"><input type="checkbox" onclick="ToggleAll(this, widgetProperties)" name="listToggleAll"></th>
          <th width="50%" class="tableRowSorted"><fmt:message key="dash.settings.ListHeader.ResourceChart"/></th>          
        </tr>
        <c:forEach var="chart" items="${charts}">
        <tr class="tableRowOdd">
          <td  class="ListCellCheckbox" width="1%" align="left" valign="top"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties)" class="listMember" name="charts" value="|<c:out value="${chart.key}"/>,<c:out value="${chart.value}"/>"></td>
          <td  class="tableCell" width="99%" align="left" valign="top"><html:link page="${chart.value}"><c:out value="${chart.key}"/></html:link></td>          
        </tr>
        </c:forEach>
      </table>      
      <tiles:insert definition=".toolbar.list">                
        <tiles:put name="deleteOnly" value="true"/>
        <%--none of this is being used--%>
        <tiles:put name="listItems" value="${chartsize}"/>
        <tiles:put name="listSize" value="${chartsize}"/>
        <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>  
        <tiles:put name="pageSizeAction" beanName="selfAction" />
        <tiles:put name="pageNumAction" beanName="selfAction"/>    
        <tiles:put name="defaultSortColumn" value="1"/>
      </tiles:insert>

      <tiles:insert definition=".form.buttons"/>
      </html:form>
    </td>
  </tr>
  <tr> 
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
