<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="availableGroups"/>
<tiles:importAttribute name="availableGroupsCount"/>
<tiles:importAttribute name="pendingGroups"/>
<tiles:importAttribute name="pendingGroupsCount"/>
<tiles:importAttribute name="resourceType" ignore="true"/>

<c:if test="${empty resourceType}">
  <c:set var="resourceType" value="platform"/>
</c:if>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="addGroups"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="selfPnaAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPnpAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPsaAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPspAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPaAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPpAction" value="/resource/${resourceType}/Inventory.do">
  <c:param name="mode" value="addGroups"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <tiles:put name="tabKey" value="resource.common.inventory.groups.GroupsTab"/>
  <tiles:put name="useFromSideBar" value="true"/>
</tiles:insert>

    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.groups.AddToGroupsTab"/>
  <tiles:put name="useToSideBar" value="true"/>
</tiles:insert>

    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${availableGroups}" var="group" action="${selfPaAction}" 
            styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableGroup" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Group" sort="true" sortAttr="5" defaultSort="true" width="50%"/>
          <display:column property="description" title="common.header.Description" width="50%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="useFromSideBar" value="true"/>
  <tiles:put name="listItems" beanName="availableGroups"/>
  <tiles:put name="listSize" beanName="availableGroupsCount"/>
  <tiles:put name="pageSizeParam" value="psa"/>
  <tiles:put name="pageSizeAction" beanName="selfPsaAction"/>
  <tiles:put name="pageNumParam" value="pna"/>
  <tiles:put name="pageNumAction" beanName="selfPnaAction"/>
</tiles:insert>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
      <div id="AddButtonDiv" align="left"><html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/></div>
      <br>&nbsp;<br>
      <div id="RemoveButtonDiv" align="right"><html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/></div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${pendingGroups}" var="group" action="${selfPpAction}" 
            styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingGroup" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Group" sort="true" sortAttr="5" defaultSort="true" width="50%"/>
          <display:column property="description" title="common.header.Description" width="50%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="newButtonKey" value="resource.common.inventory.groups.NewGroupButton"/>
  <tiles:put name="useToSideBar" value="true"/>
  <tiles:put name="listItems" beanName="pendingGroups"/>
  <tiles:put name="listSize" beanName="pendingGroupsCount"/>
  <tiles:put name="pageSizeParam" value="psp"/>
  <tiles:put name="pageSizeAction" beanName="selfPspAction"/>
  <tiles:put name="pageNumParam" value="pnp"/>
  <tiles:put name="pageNumAction" beanName="selfPnpAction"/>
</tiles:insert>

    </td>
    <!-- / ADD COLUMN  -->
	
  </tr>
</table>
<!-- / SELECT & ADD -->
