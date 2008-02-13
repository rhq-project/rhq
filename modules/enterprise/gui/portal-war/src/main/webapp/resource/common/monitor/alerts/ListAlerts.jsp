<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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
<script language="JavaScript" src="<html:rewrite page="/js/schedule.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listAlerts"/>
<script language="JavaScript" type="text/javascript">
  var jsPath = "<html:rewrite page="/js/"/>";
  var cssPath = "<html:rewrite page="/css/"/>";
  var isMonitorSchedule = true;
  var pageData = new Array();
  initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
  widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="hyphenStr" value="--"/>
<c:url var="pnAction" value="/alerts/Alerts.do">
  <c:param name="mode" value="list"/>
  <c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.year}">
    <c:param name="year" value="${param.year}"/>
  </c:if>
  <c:if test="${not empty param.month}">
    <c:param name="month" value="${param.month}"/>
  </c:if>
  <c:if test="${not empty param.day}">
    <c:param name="day" value="${param.day}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="/alerts/Alerts.do">
  <c:param name="mode" value="list"/>
  <c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.year}">
    <c:param name="year" value="${param.year}"/>
  </c:if>
  <c:if test="${not empty param.month}">
    <c:param name="month" value="${param.month}"/>
  </c:if>
  <c:if test="${not empty param.day}">
    <c:param name="day" value="${param.day}"/>
  </c:if>
</c:url>
<c:url var="calAction" value="/alerts/Alerts.do">
  <c:param name="mode" value="list"/>
  <c:param name="eid" value="${entityId.type}:${Resource.id}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<!-- FORM -->
<html:form method="POST" action="/alerts/RemoveAlerts.do">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>

<c:if test="${ CONST_PLATFORM == entityId.type}">
<c:set var="entityId" value="${Resource.entityId}"/>


<tiles:insert  definition=".page.title.events.list.platform">
    <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<tiles:insert definition =".tabs.resource.platform.alert.alerts">
    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>
</c:if>
<c:if test="${ CONST_SERVER == entityId.type}">
<tiles:insert  definition=".page.title.events.list.server">
    <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
    <c:choose>
        <c:when test="${ canControl }">
            <tiles:insert definition =".tabs.resource.server.alert.alerts">
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:when>
        <c:otherwise>
            <tiles:insert definition =".tabs.resource.server.alert.alerts.nocontrol">
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${ CONST_SERVICE == entityId.type}">
<tiles:insert  definition=".page.title.events.list.service">
    <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
    <c:choose>
        <c:when test="${ canControl }">
            <tiles:insert definition =".tabs.resource.service.alert.alerts">
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:when>
        <c:otherwise>
            <tiles:insert definition =".tabs.resource.service.alert.alerts.nocontrol">
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
<tiles:insert definition =".tabs.resource.application.monitor.alerts">
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
            <tiles:insert definition =".tabs.resource.group.monitor.alerts">
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:when>
        <c:otherwise>
            <tiles:insert definition =".tabs.resource.group.monitor.alerts.nocontrol">
                    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
                    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
            </tiles:insert>
        </c:otherwise>
    </c:choose>
</c:if>

<script>
  function nextDay() {
    var tomorrow = new Date(<c:out value="${date}"/> + 86400000);
    var url = '<c:out value="${calAction}" escapeXml="false"/>' +
              '&year=' + tomorrow.getFullYear() +
              '&month=' + tomorrow.getMonth() +
              '&day=' + tomorrow.getDate();
    document.location = url;
  }

  function previousDay() {
    var yesterday = new Date(<c:out value="${date}"/> - 86400000);
    var url = '<c:out value="${calAction}" escapeXml="false"/>' +
              '&year=' + yesterday.getFullYear() +
              '&month=' + yesterday.getMonth() +
              '&day=' + yesterday.getDate();
    document.location = url;
  }

  function popupCal() {
    var today = new Date(<c:out value="${date}"/>);
    writeCal(today.getMonth(), today.getFullYear(),
             '<c:out value="${calAction}" escapeXml="false"/>');
  }
</script>

<table width="100%"><tr>
<td width="100%">&nbsp;</td>
<td><a href="javascript:previousDay()"><html:img page="/images/schedule_left.gif" border="0"/></a></td>
<td nowrap="true" class="BoldText"><hq:dateFormatter value="${date}" showTime="false"/></td>
<td><a href="javascript:nextDay()"><html:img page="/images/schedule_right.gif" border="0"/></a></td>
<td><html:link href="javascript:popupCal()"><html:img page="/images/schedule_iconCal.gif" width="19" height="17" alt="" border="0"/></html:link></td>
</tr></table>

<display:table cellspacing="0" cellpadding="0" width="100%"
action="${sortAction}" items="${Alerts}" var="Alert">

<display:column width="1%" property="id" title="<input
type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\"
name=\"listToggleAll\">" isLocalizedTitle="false"
styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
<display:checkboxdecorator name="alerts"
onclick="ToggleSelection(this,widgetProperties)"
styleClass="listMember"/> </display:column>

<display:column width="20%" property="priority"
title="alerts.alert.AlertList.ListHeader.Priority">
<display:prioritydecorator
flagKey="alerts.alert.alertlist.listheader.priority"/>
</display:column>


<display:column width="20%" property="ctime" sort="true" sortAttr="2"
defaultSort="true" title="alerts.alert.AlertList.ListHeader.AlertDate"
href="/alerts/Alerts.do?mode=viewAlert&eid=${Resource.entityId.appdefKey}"
paramId="a" paramProperty="id" ><display:datedecorator/>
</display:column>

<display:column width="20%" property="name" sort="true"
sortAttr="1" defaultSort="false"
title="alerts.alert.AlertList.ListHeader.AlertDefinition"
href="/alerts/Config.do?mode=viewRoles&eid=${Resource.entityId.appdefKey}"
paramId="ad" paramProperty="alertDefId"/>

<display:column width="20%" property="conditionFmt"
title="alerts.alert.AlertList.ListHeader.AlertCondition"/>

<display:column width="20%" property="value"
title="alerts.alert.AlertList.ListHeader.ActualValue" />

</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listItems" beanName="Alerts"/>
  <tiles:put name="deleteOnly" value="true"/>
  <tiles:put name="listSize" beanName="listSize"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="pageSizeAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="2"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
</tiles:insert>
<tiles:insert definition=".page.footer">
</tiles:insert>
</html:form>
<!-- /  -->
