<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- NOT SURE
<tiles:importAttribute name="resource" ignore="true"/>
-->

<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />
<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />
<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_SERVICE" var="CONST_SERVICE" />
<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_APPLICATION" var="CONST_APPLICATION" />
<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP" />

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listAlertDefinitions"/>
<script language="JavaScript" type="text/javascript">
function setActiveInactive() {
    document.RemoveConfigForm.setActiveInactive.value='y';
    document.RemoveConfigForm.submit();
}

var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="entityId" value="${Resource.id}"/>
<c:url var="pnAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="id" value="${Resource.id}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="psAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="id" value="${Resource.id}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="id" value="${Resource.id}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<c:set var="newAction" value="/alerts/Config.do?mode=new&id=${entityId}"/>

<!-- FORM -->
<html:form action="/alerts/RemoveConfig">
<html:hidden property="rid" value="${Resource.id}"/>


<%--
<tiles:insert  definition=".page.title.events.list.platform">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="${Resource.id}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<tiles:insert definition =".tabs.resource.platform.alert.configAlerts">
    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
    <tiles:put name="resourceType" beanName="Resource" beanProperty="resourceType"/>
</tiles:insert>
--%>

  <tiles:insert definition=".tabs.resource.platform.alert.configAlerts">
    <tiles:put name="id" value="${Resource.id}"/>
    <tiles:put name="resourceType" value="${Resource.resourceType.id}"/>
  </tiles:insert>
<%--

<c:if test="${ CONST_PLATFORM == entityId.type}">
<tiles:insert  definition=".page.title.events.list.platform">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="${entityId.appdefKey}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<tiles:insert definition =".tabs.resource.platform.alert.configAlerts">
    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:if>
<c:if test="${ CONST_SERVER == entityId.type}">
<tiles:insert  definition=".page.title.events.list.server">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="${entityId.appdefKey}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<c:choose>
 <c:when test="${ canControl }">
  <tiles:insert definition=".tabs.resource.server.alert.configAlerts">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".tabs.resource.server.alert.configAlerts.nocontrol">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>
</c:if>
<c:if test="${ CONST_SERVICE == entityId.type}">
<tiles:insert  definition=".page.title.events.list.service">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="${entityId.appdefKey}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<c:choose>
 <c:when test="${ canControl }">
  <tiles:insert definition=".tabs.resource.service.alert.configAlerts">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".tabs.resource.service.alert.configAlerts.nocontrol">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
   <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>

</c:if>
<c:if test="${ CONST_APPLICATION == entityId.type}">
<tiles:insert  definition=".page.title.events.list.application">
    <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<tiles:insert definition =".tabs.resource.application.monitor.configAlerts">
    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:if>
<c:if test="${ CONST_GROUP == entityId.type}">
    <tiles:insert  definition=".page.title.events.list.group">
        <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
        <tiles:put name="resource" beanName="Resource"/>
        <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
        <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
    </tiles:insert>
    <c:choose>
        <c:when test="${ canControl }">
            <tiles:insert definition =".tabs.resource.group.monitor.configAlerts">
                <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:when>
        <c:otherwise>
            <tiles:insert definition =".tabs.resource.group.monitor.configAlerts.nocontrol">
                <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:otherwise>
    </c:choose>
</c:if>

--%>

<display:table cellspacing="0" cellpadding="0" width="100%"
               action="${sortAction}" items="${Definitions}" >
  <display:column width="1%" property="alertDefId" 
                  title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
                   isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
  <display:checkboxdecorator name="definitions" onclick="ToggleSelection(this,widgetProperties)" styleClass="listMember"/>
  </display:column>
  <display:column width="1%" property="parentId"
                  title="nbsp" styleClass="redTableCell">
    <display:equalsdecorator flagKey="alerts.config.service.DefinitionList.isResourceAlert" value="null"/>
  </display:column>

  <display:column width="20%" property="name" sortAttr="1"
                  title="alerts.config.DefinitionList.ListHeader.AlertDefinition" href="/alerts/Config.do?mode=viewRoles&id=${Resource.id}" paramId="ad" paramProperty="alertDefId"/>
    
  <display:column width="20%" property="description"
                  title="common.header.Description" />

  <display:column width="20%" property="ctime" sortAttr="2"
                 title="alerts.config.DefinitionList.ListHeader.DateCreated" >
    <display:datedecorator/>
  </display:column>
                  
  <display:column width="20%" property="enabled"
                  title="alerts.config.DefinitionList.ListHeader.Active">
    <display:booleandecorator flagKey="yesno"/>
  </display:column>

</display:table>

<tiles:insert definition=".toolbar.list">
<!-- only show new alert def link if user can see it -->

   
<hq:userResourcePermissions debug="false" resource="${Resource.id}"/>
<c:choose>
        <c:when test="${true}" >
            <tiles:put name="listNewUrl" beanName="newAction"/> 
        </c:when>
        <c:otherwise>
            <tiles:put name="deleteOnly" value="true"/>
        </c:otherwise>
</c:choose>
  <tiles:put name="listItems" beanName="Definitions"/>
  <tiles:put name="listSize" beanName="listSize"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="defaultSortColumn" value="1"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="goButtonLink" value="javascript:setActiveInactive()"/>
</tiles:insert>

<br>
<span class="red"><fmt:message key="alerts.config.service.DefinitionList.isResourceAlert.false"/></span> <fmt:message key="admin.home.TypeAlerts"/>

<tiles:insert definition=".page.footer"/>
<html:hidden property="setActiveInactive"/>
</html:form>

<!-- /  -->
