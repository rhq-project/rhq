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

<c:url var="selfPnaAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPnFilterNameAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
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
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
</c:url>

<c:url var="selfPnFilterAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPnpAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPsaAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPspAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPaAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="/resource/group/Inventory.do">
  <c:param name="mode" value="addResources"/>
  <c:param name="groupId" value="${groupId}"/>
  <c:param name="category" value="${category}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<script language="JavaScript"> <!--
    function applyNameFilter() {
        goToLocationSelfAndElement(
                'nameFilter',
                'nameFilter',
                '<c:out value="${selfPnFilterNameAction}" escapeXml="false"/>');
        return false;
    }
// -->
</script>

<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<!--  SELECT & ADD -->
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
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
        <c:choose>  
            <c:when test="${category == 'MIXED'}">  
              <tr>
                  <td class="FilterLine" width="100%" colspan="4"><html:img page="/images/spacer.gif" width="1" height="1" border="0" /></td>
                  <td><html:img page="/images/spacer.gif" width="1" height="1" border="0" /></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByNameLabel" />
                  </td>
                  <td class="FilterLabelText">
                      <input type="text" name="nameFilter" maxlength="55" size="10" 
                             onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
                             value="<c:out value="${param.nameFilter}"/>" />
                      <html:img page="/images/dash-button_go-arrow.gif" 
                             border="0" onclick="applyNameFilter()" />
                  </td>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel" />
                  </td>
                  <td class="FilterLabelText">
                      <html:select property="filterBy" styleClass="FilterFormText" 
                            onchange="goToSelectLocation(this, 'filterBy',  '${selfPnFilterAction}');">
                          <html:optionsCollection property="availResourceTypes" />
                      </html:select>
                  </td>
                  <td><html:img page="/images/spacer.gif" width="5" height="1" border="0" /></td>
              </tr>
            </c:when>
            <c:when test="${category == 'COMPATIBLE'}"> 
              <tr>
                  <td class="FilterLine" width="100%" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0" /></td>
                  <td><html:img page="/images/spacer.gif" width="1" height="1" border="0" /></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText" colspan=2>
                      <fmt:message key="resource.group.inventory.Edit.FilterByNameLabel" />&nbsp;
                      <input type="text" name="nameFilter" maxlength="55" size="10"
                             onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
                             value="<c:out value="${param.nameFilter}"/>" />&nbsp;
                      <html:img page="/images/dash-button_go-arrow.gif"
                             border="0" onclick="applyNameFilter()" />
                  </td>
                  <td><html:img page="/images/spacer.gif" width="5" height="1" border="0" /></td>
              </tr>
            </c:when>
        </c:choose>
			</table>

		</td>
		<td>&nbsp;</td>
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td rowspan="2"><html:img page="/images/spacer.gif" width="5" height="1" border="0" /></td>
					<td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0" /></td>
				</tr>
				<tr>
           <%-- 
             -- Support three different spacer patterns. The first is for adhoc
             -- grp of grp where you have a single selector. The second supports
             -- a height of 32 pixels to accomodate the slector and name filter.
             -- third is a normal spacer pattern.  --%>
          <c:choose>
              <c:when test="${category == 'COMPATIBLE' || category == 'MIXED'}">
                <td width="100%" class="FilterLabelText" height="32">&nbsp;</td>
              </c:when>
              <c:otherwise>
                <td width="100%" class="FilterLabelText" height="10">&nbsp;</td>
              </c:otherwise>
            </c:choose>              
					
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
	               var="resource"
	               padRows="true" 
	               rightSidebar="true"  
	               styleId="fromTable"
                   postfix="a" >
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableResource" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember" />
          </display:column>
          <display:column property="name" title="resource.group.inventory.NameTH" 
                          width="30%" 
                          sortAttr="res.name" />
          <display:column property="parentResource.name" title="resource.group.inventory.ParentTH" 
                          width="30%" 
                          sortAttr="parent.name" />
          <display:column property="resourceType.name" title="resource.group.inventory.TypeTH" 
                          width="39%" 
                          sortAttr="res.resourceType.name" >
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
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table cellpadding="0" cellspacing="0" border="0" width="100%"
                       action="${selfPpAction}"
                       items="${PendingResources}"
                       var="resource"
                       padRows="true" 
                       leftSidebar="true"
                       styleId="toTable"    
                       postfix="p" >
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingResource" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember" />
          </display:column>
          <display:column property="name" title="resource.group.inventory.NameTH" 
                          width="30%" 
                          sortAttr="res.name" />
          <display:column property="parentResource.name" title="resource.group.inventory.ParentTH" 
                          width="30%" 
                          sortAttr="parent.name" />
          <display:column property="resourceType.name" title="resource.group.inventory.TypeTH" 
                          width="39%" 
                          sortAttr="res.resourceType.name" >
             <%-- TODO GH: I don't see this doing anything at all? <display:resourcedecorator resource="${resource}" type="true" />--%>
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

