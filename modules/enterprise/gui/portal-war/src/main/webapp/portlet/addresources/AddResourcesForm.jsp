<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript">
</script>

<c:set var="widgetInstanceName" value="addResources"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<!-- 
   ff -> category
   ft -> type
 -->

<c:url var="selfPnaAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/> 
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPnFilterAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>  
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="categoryAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>    
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="typeAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPnpAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/> 
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>  
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPsaAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPspAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPaAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="/dashboard/Admin.do">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.category}">
    <c:param name="category" value="${param.category}"/>
  </c:if>  
  <c:if test="${not empty param.type}">
    <c:param name="type" value="${param.type}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
</c:url>


<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="50%" valign="top">
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="resource.group.inventory.Edit.ResourcesTab"/>
      <tiles:put name="useFromSideBar" value="true"/>
     </tiles:insert>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="resource.group.inventory.Edit.AddResourcesTab"/>
      <tiles:put name="useToSideBar" value="true"/>
     </tiles:insert>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <!--  FILTER TOOLBAR CONTENTS -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
          <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
        </tr>
        <tr>
          <td class="FilterLabelText" nowrap="nowrap" align="right">View:</td>
          <td class="FilterLabelText" width="100%">      
            <html:select property="category" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, 'category', '${categoryAction}');">
              <hq:optionMessageList property="functions" baseKey="resource.hub.filter"/>
            </html:select>
            
            <html:select property="type" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, 'type', '${typeAction}');">
              <html:option value="-1" key="resource.hub.filter.AllResourceTypes"/>
              <html:optionsCollection property="types"/>
            </html:select>
            
          </td>
        </tr>
      </table>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
          <td class="FilterLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="30" border="0"/></td>
          <td width="100%" class="FilterLabelText">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">
	<display:table cellpadding="0" cellspacing="0" border="0" width="100%"
	               action="${selfPaAction}"
	               items="${AvailableResources}" 
	               var="item"
	               padRows="true" 
                   rightSidebar="true"
                   styleId="fromTable" 
                   postfix="a" >
          <display:column width="1%" property="original.id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableResources" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column title="common.header.ResourceName" 
                          width="49%" 
                          sortAttr="res.name">
                          
            <display:disambiguatedResourceNameDecorator resourceName="${item.original.name}" disambiguationReport="${item}" nameAsLink="false"/>
          </display:column>
          <display:column title="resource.group.inventory.ParentTH" 
                          width="50%">
            <display:disambiguatedResourceLineageDecorator parents="${item.parents}" renderLinks="false" />
          </display:column>                

        </display:table>
      </div>
      <!--  /  -->
      <!-- LIST ITEMS -->
      <tiles:insert definition=".toolbar.new">
        <tiles:put name="useFromSideBar" value="true"/>
        <tiles:put name="pageList" beanName="AvailableResources"/>
        <tiles:put name="pageAction" beanName="selfPsaAction"/>
        <tiles:put name="postfix" value="a"/>
      </tiles:insert>
    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
     <div id="AddButtonDiv" align="left">
      <html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/>
     </div>
      <br>&nbsp;<br>
     <div id="RemoveButtonDiv" align="right">
      <html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/>
     </div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table cellpadding="0" cellspacing="0" border="0" width="100%"
                       action="${selfPpAction}"
                       items="${PendingResources}" 
                       var="item"
                       padRows="true" 
                       leftSidebar="true"   
                       styleId="toTable" 
                       postfix="p" >
          <display:column width="1%" property="original.id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingResources" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column title="common.header.ResourceName" 
                          width="49%" 
                          sortAttr="res.name">
            <display:disambiguatedResourceNameDecorator resourceName="${item.original.name}" disambiguationReport="${item}" nameAsLink="false"/>
          </display:column>                
          <display:column title="resource.group.inventory.ParentTH" 
                          width="50%">
            <display:disambiguatedResourceLineageDecorator parents="${item.parents}" renderLinks="false" />
          </display:column>                
        </display:table>
      </div>
      <!--  /  -->
      <tiles:insert definition=".toolbar.new">
        <tiles:put name="useToSideBar" value="true"/>
        <tiles:put name="pageList" beanName="PendingResources"/>
        <tiles:put name="pageAction" beanName="selfPspAction"/>
        <tiles:put name="postfix" value="p"/>
      </tiles:insert>

    </td>
    <!-- / ADD COLUMN  -->	
  </tr>
</table>
<!-- / SELECT & ADD -->

<%--tiles:insert definition=".diagnostics"/--%>