<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="section" ignore="false"/>

<c:set var="widgetInstanceName" value="configMetricsList"/>
<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
   var pageData = new Array();
   initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
   widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
<c:choose>
   <c:when test="${not empty groupId}">
      <c:url var="selfAction" value="/resource/group/monitor/Config.do">
         <c:param name="mode" value="configure"/>
         <c:param name="groupId" value="${groupId}"/>
         <c:param name="category" value="COMPATIBLE"/>
      </c:url>
   </c:when>
   <c:otherwise>
      <c:url var="selfAction" value="/resource/common/monitor/Config.do">
         <c:param name="mode" value="configure"/>
         <c:param name="id" value="${Resource.id}"/>
      </c:url>
   </c:otherwise>
</c:choose>

<html:form action="/resource/${section}/monitor/config/ConfigMetrics">
<c:choose>
   <c:when test="${not empty ResourceType && empty param.parent}">
      <!-- resource type wasn't empty so it is a default-->
      <html:hidden property="type" value="${ResourceType.id}"/>
   </c:when>
   <c:when test="${not empty groupId}">
      <html:hidden property="groupId" value="${groupId}"/>
   </c:when>
   <c:when test="${not empty param.type && param.parent >0 }">
      <!-- autogroup-->
      <html:hidden property="type" value="${param.type}"/>
      <html:hidden property="parent" value="${param.parent}"/>
      <html:hidden property="parentName" value="${parentName}"/>
   </c:when>
   <c:otherwise>
      <html:hidden property="id" value="${Resource.id}"/>
   </c:otherwise>
</c:choose>
<!-- PAGE TITLE -->
<c:set var="tmpTitle" value=".page.title.resource.${section}.full"/>
<tiles:insert beanName="tmpTitle">
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
   <c:choose>
      <c:when test="${not empty param.type}">
         <!--  this is a resource type -->
         <tiles:put name="titleName">
            <html:link page="/admin/config/EditDefaults.do?mode=monitor">
               <fmt:message key="admin.home.ResourceTemplates"/>
            </html:link>
            >
            <bean:write name="ResourceType" property="name"/>  
            <c:out value="${section}"/>
            s
         </tiles:put>
      </c:when>
      <c:when test="${not empty groupId }">
         <%-- TODO enhance --%>
         <tiles:put name="titleName">
            Group with id <c:out value="${groupId}"/>
         </tiles:put>
      </c:when>
      <c:otherwise>
         <!--  this is a resource -->
         <tiles:put name="titleName">
            <hq:inventoryHierarchy resourceId="${Resource.id}"/> <!--  print the "foo > bar > baz"  at top of page -->
         </tiles:put>
      </c:otherwise>
   </c:choose>
   <c:choose>
      <c:when test="${not empty ChildResourceType}">
         <tiles:put name="subTitleName" beanName="ChildResourceType" beanProperty="name"/>
      </c:when>
      <c:otherwise>
         <tiles:put name="titleKey" beanName="resource.common.monitor.visibility.config.ConfigureVisibility.PageTitle"/>
      </c:otherwise>
   </c:choose>
</tiles:insert>

<!-- default a null button -->
<html:image page="/images/spacer.gif" border="0" property="nullBtn"/>

<!-- CONTROL BAR -->
<c:if test="${ section eq 'service' || section eq 'group' || section eq 'server'}">
   <c:if test="${ !canControl }">
      <c:set var="nocontrol" value=".nocontrol"/>
   </c:if>
</c:if>

<c:if test="${ResourceType != null}">
   <c:set var="nocontrol" value=".defaults"/>
</c:if>


<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<hq:constant symbol="MONITOR_ENABLED_ATTR" var="CONST_ENABLED"/>

<c:choose>
   <c:when test="${requestScope[CONST_ENABLED]}">
      <br/>
      
      <tiles:insert definition=".resource.common.monitor.config.editConfigMetricsVisibility">
         <tiles:put name="section" value="${section}"/>
      </tiles:insert>

      <hq:authorization permission="MANAGE_MEASUREMENTS">
      <c:set var="tmpMetrics" value="${requestScope.measurementSchedules}"/>
      <tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
         <tiles:put name="showAddToListBtn" value="false"/>
         <tiles:put name="useDisableBtn" value="true"/>
         <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
         <tiles:put name="pageList" beanName="tmpMetrics"/>
         <%--
         When derived metrics are exposed through this UI, then the list can
         grow long and the pagination will be necessary (and will need to be
         fixed, since it wasn't working anyway).  For now, we'll suppress the
         pagination controls per PR 7821
         --%>
         <tiles:put name="showPagingControls" value="false"/>
         <tiles:put name="pageAction" beanName="selfAction"/>
      </tiles:insert>
      </hq:authorization>

   </c:when>
   <c:when test="${not empty ResourceType}">
      <br/>
      <%--
    <c:if test="${section eq 'group'}">

     <tiles:insert definition=".resource.group.monitor.config.Availability">
      <tiles:put name="Resource" beanName="Resource"/>
     </tiles:insert>
     <br/>
    </c:if>
      --%>
      <tiles:insert definition=".resource.common.monitor.config.editConfigMetricsVisibility"/>

      <hq:authorization permission="MANAGE_SETTINGS">
      <c:set var="tmpMetrics" value="${requestScope.measurementSchedules}"/>
      <tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
         <tiles:put name="showAddToListBtn" value="false"/>
         <tiles:put name="useDisableBtn" value="true"/>
         <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
         <tiles:put name="addToListParamName" value="id"/>
         <tiles:put name="addToListParamValue" beanName="Resource" beanProperty="id"/>
         <tiles:put name="pageList" beanName="tmpMetrics"/>
         <tiles:put name="showChangeSchedules" value="true"/>
         <%--
         When derived metrics are exposed through this UI, then the list can
         grow long and the pagination will be necessary (and will need to be
         fixed, since it wasn't working anyway).  For now, we'll suppress the
         pagination controls per PR 7821
         --%>
         <tiles:put name="showPagingControls" value="false"/>
         <tiles:put name="pageSizeParam" value="ps"/>
         <tiles:put name="pageAction" beanName="selfAction"/>
      </tiles:insert>
      </hq:authorization>

   </c:when>
   <c:otherwise>
      <c:choose>
         <c:when test="${section eq 'group'}">
            <c:set var="tmpMessage">
               <fmt:message key="resource.group.monitor.visibility.NotEnabled"/>
            </c:set>
         </c:when>
         <c:otherwise>
            <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
               <c:param name="mode" value="editConfig"/>
               <c:param name="id" value="${Resource.id}"/>
            </c:url>
            <c:set var="tmpMessage">
               <fmt:message key="resource.common.monitor.NotEnabled.MonitoringNotEnabled"/>
               <fmt:message key="resource.common.monitor.NotEnabled.ToEnable"/>
               <html:link href="${enableControlLink}">
                  <fmt:message key="resource.common.monitor.NotEnabled.ConfPropLink"/>
               </html:link>
               <fmt:message key="resource.common.monitor.NotEnabled.InInventory"/>
            </c:set>
         </c:otherwise>
      </c:choose>
      <tiles:insert definition=".portlet.notenabled">
         <tiles:put name="message" beanName="tmpMessage"/>
      </tiles:insert>

   </c:otherwise>
</c:choose>

<tiles:insert definition=".page.footer"/>
</html:form>
