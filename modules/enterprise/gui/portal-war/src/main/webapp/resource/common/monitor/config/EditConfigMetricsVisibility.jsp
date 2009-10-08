<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="section" ignore="false"/>

<c:set var="isAutoGroup" value="false"/>
<c:url var="selfAction" value="/resource/${section}/monitor/Config.do">
   <c:param name="mode" value="configure"/>
   <c:choose>
      <c:when test="${not empty groupId}">
         <c:param name="groupId" value="${groupId}"/>
         <c:param name="category" value="COMPATIBLE"/>
      </c:when>
      <c:when test="${ResourceType != null && param.parent > 0}">
         <c:param name="parent" value="${param.parent}"/>
         <c:param name="type" value="${param.type}"/>
      </c:when>
      <c:when test="${type!=null}">
         <c:param name="type" value="${type}"/>
      </c:when>
      <c:otherwise>
         <c:param name="id" value="${Resource.id}"/>
      </c:otherwise>
   </c:choose>
   <c:if test="${not empty param.sc}">
      <c:param name="sc" value="${param.sc}"/>
   </c:if>
   <c:if test="${not empty param.so}">
      <c:param name="so" value="${param.so}"/>
   </c:if>
   <c:if test="${not empty param.pn}">
      <c:param name="pn" value="${param.pn}"/>
   </c:if>
   <c:if test="${not empty param.ps}">
      <c:param name="ps" value="${param.ps}"/>
   </c:if>
</c:url>
<c:if test="${ResourceType != null && param.parent > 0}">
   <c:set var="isAutoGroup" value="true"/>
</c:if>

<!-- HEADER TAB -->

<c:choose>
  <%-- Defaults --%>
  <c:when test="${ResourceType != null  && empty param.parent}">
	<tiles:insert definition=".header.tab">
	   <tiles:put name="tabKey" value="resource.common.monitor.visibility.config.default.HeaderTabTitle"/>
	   <tiles:put name="tabName" value="${ResourceType.name}"/>
	</tiles:insert>
	<c:out value="${ResourceType.category}:"/> 
	<b>
	<c:out value="${ResourceType.name}"/>
	</b> 
	<c:if test="${not empty ResourceType.description}">
     <c:out value=" (${ResourceType.description})"/>
   </c:if>
  </c:when>
  <%-- autogroup --%>
    <c:when test="${isAutoGroup == true}">
        <tiles:insert definition=".header.tab">
           <tiles:put name="tabKey"></tiles:put> <%-- needs to be empty --%>
           <tiles:put name="tabName">
            <fmt:message  key="resource.common.monitor.visibility.config.autogroup.HeaderTabTitle">
               <fmt:param>${ResourceType.name}</fmt:param>
               <fmt:param>${parentName}</fmt:param>
            </fmt:message>
           </tiles:put>
           <tiles:put name="subTitle">
           <fmt:message>resource.group.monitor.visibility.config.HeaderTabSubTitle</fmt:message>
           </tiles:put>
        </tiles:insert>
  </c:when>
  <%-- compat group --%>
    <c:when test="${groupId > 0}">
        <tiles:insert definition=".header.tab">
           <tiles:put name="tabKey" value="resource.group.monitor.visibility.config.HeaderTabTitle"/>
           <tiles:put name="tabName" value="${groupName}"/> 
           <tiles:put name="subTitle">
            <fmt:message>resource.group.monitor.visibility.config.HeaderTabSubTitle</fmt:message>
           </tiles:put>       
        </tiles:insert>
  </c:when>
  <c:otherwise>
	<tiles:insert definition=".header.tab">
	   <tiles:put name="tabKey" value="resource.common.monitor.visibility.config.HeaderTabTitle"/>
	</tiles:insert>
 </c:otherwise>
</c:choose>

<c:set var="emptyMsg">
   <fmt:message key="resource.common.monitor.visibility.EmptyMetricsEtc"/>
</c:set>

<c:choose>
   <c:when test="${false == true}"> 

      <!-- METRIC SCHEDULES FOR AN AUTOGROUP -->

      <display:table items="${measurementSchedules}" var="grpavailmetric" action="${selfAction}"
                     styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}"
                     nowrapHeader="true">
         <display:column width="1%" property="id"
                         title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">"
                         isLocalizedTitle="false" styleClass="ListCellCheckbox"
                         headerStyleClass="ListHeaderInactiveSorted">
            <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)"
                                       styleClass="listMember1"/>
         </display:column>
         <display:column value="${grpavailmetric.name}" title="resource.common.monitor.visibility.config.MetricTH"
                         width="70%"
                         headerStyleClass="ListHeaderInactiveSorted"/>
         <display:column property="activeMembers" title="resource.common.monitor.visibility.config.MembersCollectingTH"
                         align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
            <display:groupmetricdecorator active="${grpavailmetric.activeMembers}"
                                          total="${grpavailmetric.totalMembers}"/>
         </display:column>
         <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH"
                         align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
            <display:datedecorator active="${grpavailmetric.activeMembers}" isElapsedTime="true" isGroup="true"/>
         </display:column>
      </display:table>

      <!-- / -->

   </c:when>
   <c:when test="${type != null }">
      <!-- METRIC SCHEDULES FOR A  RESOURCE TYPE -->
      
            <display:table items="${measurementSchedules}" var="schedule" action="${selfAction}"
                     styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}"
                     nowrapHeader="true">
         <display:column value="${schedule.measurementDefinition.id}"
                         title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">"
                         isLocalizedTitle="false" styleClass="ListCellCheckbox"
                         width="1%" headerStyleClass="ListHeaderInactiveSorted">
            <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)"
                                       styleClass="listMember1"/>
         </display:column>
         <display:column value="${schedule.measurementDefinition.displayName}" sortAttr="displayName"
                         title="resource.common.monitor.visibility.config.MetricTH" width="25%"
                         headerStyleClass="ListHeaderInactiveSorted"/>

         <c:choose>
            <c:when test="${schedule.measurementDefinition.description == null}">
               <c:set var="description" value=""/>
            </c:when>
            <c:otherwise>
               <c:set var="description" value="${schedule.measurementDefinition.description}"/>
            </c:otherwise>
         </c:choose>
         <display:column sortAttr="description" value="${description}"
                                  title="Description" isLocalizedTitle="false" width="39%"
                                  headerStyleClass="ListHeaderInactiveSorted">
         </display:column>

         <display:column value="${schedule.measurementDefinition}" sortAttr="category"
                         title="resource.common.monitor.visibility.config.CategoryTH" width="20%"
                         headerStyleClass="ListHeaderInactiveSorted">
               <display:measurementDataTypeDecorator/>
         </display:column>
         <display:column property="collectionEnabled" sortAttr="default_on"
                         title="resource.common.monitor.visibility.config.CollectionEnabledTH"
                         align="center"
                         nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
            <display:booleandecorator flagKey="yesno"/>
         </display:column>
         <display:column property="collectionInterval" sortAttr="default_interval"
                         title="resource.common.monitor.visibility.config.CollectionIntervalTH"
                         align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
            <display:datedecorator isElapsedTime="true" isGroup="${groupId > 0 || isAutoGroup==true}"/>
         </display:column>
      </display:table>
      
   </c:when>
   <c:otherwise>

      <!-- METRIC SCHEDULES FOR A SINGLE RESOURCE OR COMPAT GROUP-->

      <display:table items="${measurementSchedules}" var="schedule" action="${selfAction}"
                     styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}"
                     nowrapHeader="true">
         <display:column value="${schedule.measurementDefinition.id}"
                         title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">"
                         isLocalizedTitle="false" styleClass="ListCellCheckbox"
                         width="1%" headerStyleClass="ListHeaderInactiveSorted">
            <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)"
                                       styleClass="listMember1"/>
         </display:column>
         <display:column value="${schedule.measurementDefinition.displayName}" sortAttr="ms.definition.displayName"
                         title="resource.common.monitor.visibility.config.MetricTH" width="25%"
                         headerStyleClass="ListHeaderInactiveSorted"/>

         <c:choose>
            <c:when test="${schedule.measurementDefinition.description == null}">
               <c:set var="description" value=""/>
            </c:when>
            <c:otherwise>
               <c:set var="description" value="${schedule.measurementDefinition.description}"/>
            </c:otherwise>
         </c:choose>
         <display:column sortAttr="ms.definition.description" value="${description}"
                                  title="Description" isLocalizedTitle="false" width="39%"
                                  headerStyleClass="ListHeaderInactiveSorted">
         </display:column>

         <display:column value="${schedule.measurementDefinition}" sortAttr="ms.definition.category"
                         title="resource.common.monitor.visibility.config.CategoryTH" width="20%"
                         headerStyleClass="ListHeaderInactiveSorted">
               <display:measurementDataTypeDecorator/>
         </display:column>
         <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" /> --%>
         <display:column property="collectionEnabled" sortAttr="ms.enabled"
                         title="resource.common.monitor.visibility.config.CollectionEnabledTH"
                         align="center"
                         nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
            <display:booleandecorator flagKey="yesno"/>
         </display:column>
         <display:column property="collectionInterval" sortAttr="ms.interval"
                         title="resource.common.monitor.visibility.config.CollectionIntervalTH"
                         align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
            <display:datedecorator isElapsedTime="true" isGroup="${groupId > 0 || isAutoGroup==true}"/>
         </display:column>
      </display:table>

      <!-- / -->

   </c:otherwise>
</c:choose>
