<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="section" ignore="true"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listServerControl"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<hq:pageSize var="pageSize"/>
<c:set var="selfAction" value="/resource/${section}/Control.do?mode=history&rid=${Resource.id}&type=${Resource.entityId.type}"/>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
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

<%-- now add the context path --%>
<c:url var="selfActionUrl" value="${selfAction}"/>

<c:set var="entityId" value="${Resource.entityId}"/>

<c:choose>
 <c:when test="${section eq 'service'}">
  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.service.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
  <!-- CONTROL BAR -->
  <tiles:insert definition=".tabs.resource.service.control.list.history">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  </tiles:insert>
 </c:when>
 <c:when test="${section eq 'group'}">
  <!--  PAGE TITLE -->
  <tiles:insert definition=".page.title.resource.group.full">
   <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
  <!-- CONTROL BAR -->
  <tiles:insert definition=".tabs.resource.group.control.list.history">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <!--  PAGE TITLE -->
   <tiles:insert definition=".page.title.resource.server.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666" /></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
   <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
   <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  </tiles:insert>
  <!-- CONTROL BAR -->
  <tiles:insert definition=".tabs.resource.server.control.list.history">
   <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 </tiles:insert>
 </c:otherwise>
</c:choose>
<br>


<hq:constant symbol="CONTROL_ENABLED_ATTR" var="CONST_ENABLED" />

<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">

<!-- MENU BAR -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.ControlHistory.Title"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<!-- Table Content -->
<html:form action="/resource/${section}/control/RemoveHistory">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>

<c:set var="tmpNoErrors"><fmt:message key="resource.common.control.NoErrors"/></c:set>

<div id="listDiv">
  <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfActionUrl}"
                  items="${hstDetailAttr}" var="hstDetail" >
   <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="controlActions" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
   </display:column>
  <c:choose>
   <c:when test="${section eq 'group'}">
    <display:column width="16%" property="action" sort="true" sortAttr="9"
                    defaultSort="true" title="resource.server.ControlHistory.ListHeader.Action" 
                    href="/resource/${section}/Control.do?mode=hstDetail&type=${Resource.entityId.type}&rid=${Resource.id}" paramId="bid" paramProperty="id" nowrap="true" />
    </c:when>
    <c:otherwise>
     <display:column width="12%" property="action"  
                     title="resource.server.ControlHistory.ListHeader.Action"/> 
    </c:otherwise>
   </c:choose>
   <display:column width="14%" property="status" title="resource.server.ControlHistory.ListHeader.Status" sort="true" sortAttr="10" nowrap="true">
   </display:column> 
   <display:column width="16%" property="dateScheduled" title="resource.server.ControlHistory.ListHeader.Sched"  nowrap="true" sort="true" sortAttr="13" >
      <display:datedecorator/>
   </display:column>
   <display:column width="16%" property="startTime" title="resource.server.ControlHistory.ListHeader.Started" 
                   sort="true" defaultSort="false" sortAttr="11" nowrap="true" >
       <display:datedecorator/>
   </display:column>
   <display:column width="12%" property="duration" title="resource.server.ControlHistory.ListHeader.Elapsed" sort="true" sortAttr="12" >
      <display:datedecorator isElapsedTime="true"/>
   </display:column>
   <display:column width="8%" property="subject" title="resource.server.ControlHistory.ListHeader.Subject">
   </display:column>
   <display:column title="resource.server.ControlHistory.ListHeader.ErrorStatus" headerStyleClass="ListHeaderInactive" property="errorStr">
     <display:alternateDecorator secondChoice="${tmpNoErrors}"/>
   </display:column>
  </display:table>
</div>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" beanName="selfAction"/>
  <tiles:put name="listItems" beanName="hstDetailAttr"/>
  <tiles:put name="listSize" beanName="hstDetailAttr" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction" />
  <tiles:put name="pageNumAction" beanName="pnAction"/>   
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="defaultSortColumn" value="9"/>
  <tiles:put name="deleteOnly" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>
</html:form>

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
   <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
   </c:url>
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/> <fmt:message key="resource.common.control.NotEnabled.ToEnable"/> <html:link href="${enableControlLink}"><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/></html:link> <fmt:message key="resource.common.control.NotEnabled.InInventory"/>
   </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insert definition=".portlet.notenabled">
    <tiles:put name="message" beanName="tmpMessage"/>
   </tiles:insert>

</c:otherwise>
</c:choose>
