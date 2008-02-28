<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<c:set var="subTabUrl" value="/resource/common/monitor/Visibility.do?mode=events"/>
<c:set var="selfAction" value="${subTabUrl}&id=${Resource.id}"/>

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

//-->
</script>


   <table width="300" cellpadding="0" cellspacing="0" border="0">
      <tr>
      &nbsp;
      </tr>
   </table>
      </td> <%-- opened in MainLayout.jsp --%>

<c:choose>
  <c:when test="${not empty Resource}">
    <tiles:insert definition=".page.title.resource.common.full">
      <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="${Resource.id}"/></tiles:put>
      <tiles:put name="resource" beanName="Resource"/>
      <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
      <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
    </tiles:insert>

</c:when>
</c:choose>

    <tiles:insert definition=".tabs.resource.common.monitor.events">
      <tiles:put name="id" value="${param.id}"/>
      <tiles:put name="resourceType" value="${Resource.resourceType.id}"/>
    </tiles:insert>
      
<%-- full width from here --%>     
<table width="98%" align="center" cellspacing="0" cellpadding="0" border="0">
<tr>
<td>
   <c:url var="sAction" value="/resource/common/Events.do">
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
      <c:if test="${not empty param.id}">
         <c:param name="id" value="${param.id}"/>
      </c:if>
      <c:if test="${not empty param.groupId}">
         <c:param name="id" value="${param.groupId}"/>
      </c:if>
   </c:url>
   
   <html:form action="/resource/common/events/Events" >
<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0">
<tr colspan="4">
  <td class="FilterLine" colspan="4"><b>
   <fmt:message key="resource.common.monitor.events.FilterBy" />
  </b></td>
</tr>
<tr class="FilterLabelText">
  <td width="5%"/><td>Severity</td><td class="FilterLabelText">Source</td><td class="FilterLabelText">Search String</td></tr>
<tr>
   <td width="5%"/>
   <td>
     <html:select property="sevFilter">
        <html:option value=""/>
        <html:option value="DEBUG"/>
        <html:option value="INFO"/>
        <html:option value="WARN"/>
        <html:option value="ERROR"/>
        <html:option value="FATAL"/>
     </html:select>
   </td>
   <td>
      <html:text property="sourceFilter"/>
   </td>
   <td>
      <html:text property="searchString"/>
   </td>
</tr>
  <tr>
    <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
      <tiles:put name="form" beanName="EventsForm"/>
      <tiles:put name="formName" value="EventsForm"/>
      <tiles:put name="mode" beanName="mode"/>
      <tiles:put name="id" value="${Resource.id}"/>
      <c:if test="${not empty view}">
         <tiles:put name="view" beanName="view"/>
      </c:if>
   </tiles:insert>
 </tr>
</table>
<html:hidden property="id"/>
<html:hidden property="groupId"/>
</html:form>
<p/>
<table width="98%" align="center" cellspacing="0" cellpadding="0" border="0" >
<tr class="FilterLine"><td><b><fmt:message key="resource.common.monitor.events.ListOfEvents"/></b></td></tr>
<tr>
   <td>
   <c:set var="emptyMsg"><fmt:message key="resource.common.monitor.events.EmptyList"/></c:set>
   <display:table items="${EventsForm.events}" var="event" width="100%" 
         emptyMsg="${emptyMsg}" cellspacing="0" cellpadding="0" action="${sAction}">
      <display:column width="4%" property="eventId" title="resource.common.monitor.events.IdTitle"
         sortAttr="eventId" />
      <display:column width="1%" property="severity" title="resource.common.monitor.events.SeverityTitle"
         sortAttr="severity"/>
      <display:column width="20%" property="sourceLocation" title="resource.common.monitor.events.SourceTitle"
         sortAttr="sourceLocation"/>
      <display:column width="54%" property="eventDetail" title="resource.common.monitor.events.DetailTitle"
          onClick="loadDetail(_property:eventId:)" />
      <display:column width="20%" property="timestamp" title="resource.common.monitor.events.TimeTitle"
         sortAttr="timestamp" defaultSort="true"/>
   </display:table>

   <c:url var="pageAction" value="/resource/common/Events.do">
      <c:param name="mode" value="events"/>
      <c:if test="${not empty param.so}">
         <c:param name="so" value="${param.so}"/>
      </c:if>
      <c:if test="${not empty param.sc}">
         <c:param name="sc" value="${param.sc}"/>
      </c:if>
            
      <c:if test="${not empty param.pn}">
         <c:param name="pn" value="${param.pn}"/>
      </c:if>
      <c:if test="${not empty param.ps}">
         <c:param name="ps" value="${param.ps}"/>
      </c:if>
   </c:url>


</td>
</tr>
</table>      
<p/>
   

<table  width="98%" align="center" cellspacing="0" cellpadding="0" border="0" >
<tr class="MonitorToolbar">
   <td>
     <b><fmt:message key="resource.common.monitor.events.DetailsHeader"/></b>
   </td>
</tr>

<tr>
  <td>
<div style="border:1px solid black;height:200px" id="eventDetailDiv" >
<fmt:message key="resource.common.monitor.events.ShowDetail"/>
</div>
  </td>
</tr>
</table> 

