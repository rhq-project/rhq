<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<c:set var="type" value="${param.type}"/>
<c:set var="parent" value="${param.parent}"/>

   <c:url var="sAction" value="/resource/common/events/Events.do">
      <c:param name="mode" value="events" />
      <!-- Ensure (form) filter information is supplied via parameters by pagination generated requests -->
      <c:if test="${not empty param.pSeverity}">
        <c:param name="pSeverity" value="${param.pSeverity}"/>
      </c:if>
      <c:if test="${not empty param.pSource}">
        <c:param name="pSource" value="${param.pSource}"/>
      </c:if>                
      <c:if test="${not empty param.pSearch}">
        <c:param name="pSearch" value="${param.pSearch}"/>
      </c:if>                
      <!-- Ensure pagination information is supplied via parameters by pagination generated requests -->      
      <c:if test="${not empty param.ps}">
         <c:param name="ps" value="${param.ps}"/>
      </c:if>
      <c:if test="${not empty param.pn}">
         <c:param name="pn" value="${param.pn}"/>
      </c:if>
      <c:if test="${not empty param.so}">
         <c:param name="so" value="${param.so}"/>
      </c:if>
      <c:if test="${not empty param.sc}">
         <c:param name="sc" value="${param.sc}"/>
      </c:if>
      <!-- Ensure navigation information is supplied for all requests -->      
      <c:if test="${not empty param.id}">
         <c:param name="id" value="${param.id}"/>
      </c:if>
      <c:if test="${not empty param.groupId}">
         <c:param name="groupId" value="${param.groupId}"/>
      </c:if>
      <c:if test="${not empty param.type}">
         <c:param name="type" value="${param.type}"/>
      </c:if>
      <c:if test="${not empty param.parent}">
         <c:param name="parent" value="${param.parent}"/>
      </c:if>
   </c:url>

   <c:url var="timelineAction" value="/resource/common/events/Events.do">
      <c:param name="mode" value="editRange" />
      <!-- Ensure (form) filter information is supplied via parameters by pagination generated requests -->
      <c:if test="${not empty param.pSeverity}">
        <c:param name="pSeverity" value="${param.pSeverity}"/>
      </c:if>
      <c:if test="${not empty param.pSource}">
        <c:param name="pSource" value="${param.pSource}"/>
      </c:if>                
      <c:if test="${not empty param.pSearch}">
        <c:param name="pSearch" value="${param.pSearch}"/>
      </c:if>                
      <!-- Ensure pagination information is supplied via parameters by pagination generated requests -->
      <c:if test="${not empty param.ps}">
         <c:param name="ps" value="${param.ps}"/>
      </c:if>
      <c:if test="${not empty param.pn}">
         <c:param name="pn" value="${param.pn}"/>
      </c:if>
      <c:if test="${not empty param.so}">
         <c:param name="so" value="${param.so}"/>
      </c:if>
      <c:if test="${not empty param.sc}">
         <c:param name="sc" value="${param.sc}"/>
      </c:if>
      <!-- Ensure navigation information is supplied for all requests -->
      <c:if test="${not empty param.id}">
         <c:param name="id" value="${param.id}"/>
      </c:if>
      <c:if test="${not empty param.groupId}">
         <c:param name="groupId" value="${param.groupId}"/>
      </c:if>
      <c:if test="${not empty param.type}">
         <c:param name="type" value="${param.type}"/>
      </c:if>
      <c:if test="${not empty param.parent}">
         <c:param name="parent" value="${param.parent}"/>
      </c:if>
   </c:url>

<script type="text/javascript" language="JavaScript">
<!--
   function loadDetail(eventId) {

       var indicatorDiv = document.getElementById("eventDetailDiv");
       var xmlhttp = getXMLHttpRequest();
       var url = "/resource/common/events/OneEventDetail.do"; 
           url += "?action=getDetail&eventId=" + eventId;
       xmlhttp.open('GET',url,true);
       xmlhttp.onreadystatechange=function()
         {
             if (xmlhttp.readyState==4) {
                 indicatorDiv.innerHTML = xmlhttp.responseText;
             }
         }
       xmlhttp.send(null);
      }
      
   function ackOne(eventId) {

       var indicatorDiv = document.getElementById("ack" + eventId);
       var xmlhttp = getXMLHttpRequest();
       var url = "/resource/common/events/AckEvents.do"; 
           url += "?action=ackEvent&eventId=" + eventId;
       xmlhttp.open('GET',url,true);
       xmlhttp.onreadystatechange=function()
         {
             if (xmlhttp.readyState==4) {
                 indicatorDiv.innerHTML = xmlhttp.responseText;
             }
         }
       xmlhttp.send(null);
      }
//-->
</script>

      </td> <%-- opened in MainLayout.jsp --%>

<c:choose>
   <c:when test="${groupId>0}">
      <%-- comp group --%>
      <tiles:insert definition=".page.title.resource.group.full">
         <tiles:put name="resource" beanName="Resource" />
         <tiles:put name="resourceOwner" beanName="ResourceOwner" />
         <tiles:put name="resourceModifier" beanName="ResourceModifier" />
      </tiles:insert>

      <tiles:insert definition=".tabs.resource.group.monitor">
         <tiles:put name="id" value="${param.id}" />
         <tiles:put name="resourceType"
            value="${Resource.resourceType.id}" />
      </tiles:insert>
   </c:when>
   <c:when test="${parent > 0 && type > 0 }">
      <%-- autogroup --%>
      <tiles:insert definition=".page.title.resource.autogroup.full">
         <tiles:put name="autogroupResourceId" value="${parent}" />
         <tiles:put name="autogroupResourceType" value="${type}" />
      </tiles:insert>

      <tiles:insert definition=".tabs.resource.autogroup.monitor.events">
         <tiles:put name="autogroupResourceId" value="${parent}" />
         <tiles:put name="autogroupResourceType" value="${type}" />
      </tiles:insert>
   </c:when>
   <c:otherwise>
      <%-- single resource --%>
      <tiles:insert definition=".page.title.resource.common.full">
         <tiles:put name="titleName">
            <hq:inventoryHierarchy resourceId="${Resource.id}" />
         </tiles:put>
         <tiles:put name="resource" beanName="Resource" />
         <tiles:put name="resourceOwner" beanName="ResourceOwner" />
         <tiles:put name="resourceModifier" beanName="ResourceModifier" />
      </tiles:insert>

      <tiles:insert definition=".tabs.resource.common.monitor.events">
         <tiles:put name="id" value="${param.id}" />
         <tiles:put name="resourceType"
            value="${Resource.resourceType.id}" />
      </tiles:insert>
   </c:otherwise>
</c:choose>

<%-- full width from here --%>
<html:form action="${sAction}" >     

<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0">
	<tr>
		<td class="FilterLine" colspan="4"><b><fmt:message key="resource.common.monitor.events.FilterBy" /></b></td>
	</tr>
	<tr>
		<td width="5%"/>
		<td>
            Severity:
            <html:select property="sevFilter" onchange="this.form.submit(); return true;">
     			<html:option value="ALL"/>
    			<html:option value="DEBUG"/>
    			<html:option value="INFO"/>
    			<html:option value="WARN"/>
    			<html:option value="ERROR"/>
    			<html:option value="FATAL"/>
  			</html:select>
		</td>
		<td>Source: <html:text property="sourceFilter"/></td>
		<td>Search: <html:text property="searchString"/></td>
	</tr>
</table>

<p/>

<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0" >
	<tr class="FilterLine">
		<td><b><fmt:message key="resource.common.monitor.events.ListOfEvents"/></b></td>
	</tr>
	<tr>
		<td>
			<c:set var="emptyMsg">
				<fmt:message key="resource.common.monitor.events.EmptyList"/>
			</c:set>
			<display:table items="${EventsForm.events}" var="event" width="100%" 
   			               emptyMsg="${emptyMsg}" cellspacing="0" cellpadding="0" action="${sAction}">
				<display:column width="1%" property="severity" title="resource.common.monitor.events.SeverityTitle" sortAttr="ev.severity"/>
				<display:column width="15%" property="sourceLocation" title="resource.common.monitor.events.SourceTitle" sortAttr="evs.location"/>
				<display:column width="50%" property="eventDetail" title="resource.common.monitor.events.DetailTitle" onClick="loadDetail(_property:eventId:)" maxLength="160"/>
				<display:column width="20%" property="timestamp" title="resource.common.monitor.events.TimeTitle" sortAttr="ev.timestamp" defaultSort="true" maxLength="60"/>
				<display:column width="14%" property="ackTimeUser" title="resource.common.monitor.events.Acknowledged">
		  			<display:userackdecorator onclick="ackOne(_property:eventId:)" id="ack_property:eventId:"/>
      			</display:column>
   			</display:table>
		</td>
	</tr>
	<tr>
		<%-- paging controls here they define their own <td>, so it is not needed here --%>
		<c:set var="pageList" value="${EventsForm.events}" />
		<c:set var="myAction" value="javascript:document.EventsForm.submit()" />
		<tiles:insert definition=".controls.paging">
			<tiles:put name="pageList" beanName="pageList"/>
			<tiles:put name="postfix" value=""/>
			<tiles:put name="action" beanName="sAction"/>
		</tiles:insert>
	</tr>
</table>

<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0" >
	<tr>
		<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
			<tiles:put name="form" beanName="EventsForm"/>
			<tiles:put name="formName" value="EventsForm"/>
			<tiles:put name="mode" beanName="mode" />
			<c:if test="${not empty Resource}">
				<tiles:put name="id" value="${Resource.id}"/>
			</c:if>
			<c:if test="${groupId > 0}">
				<tiles:put name="groupId" beanName="groupId"/>
			</c:if>
			<c:if test="${parent > 0 && type>0 }">
				<tiles:put name="type" value="${type}"/>
				<tiles:put name="parent" value="${parent}"/>
			</c:if>
			<c:if test="${not empty view}">
				<tiles:put name="view" beanName="view"/>
			</c:if>
		</tiles:insert>
	</tr>
</table>
</html:form>      

<p/>

<table width="98%" align="center" cellspacing="0" cellpadding="0" border="0" >
	<tr class="MonitorToolbar">
		<td>
			<b><fmt:message key="resource.common.monitor.events.DetailsHeader"/></b>
		</td>
	</tr>
	<tr>
		<td>
			<div style="border:1px solid black;height:200px;overflow:auto;" id="eventDetailDiv" >
				<fmt:message key="resource.common.monitor.events.ShowDetail"/>
			</div>
  		</td>
	</tr>
</table> 

