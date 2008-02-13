<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%-- 
    Tile that displays the status of a control action.

    @param tabKey resource key for displaying as the table's header
    @param isDetail flag to display "delete current" or "view details" links
    @param section the control that this tile should be displayed as:
           server, service, control

--%>

<tiles:importAttribute name="tabKey" ignore="true"/>
<tiles:importAttribute name="isDetail" ignore="true"/>
<tiles:importAttribute name="section"/>

<c:if test="${empty tabKey}">
 <c:set var="tabKey" value="resource.server.ControlStatus.Title"/>
</c:if>

<c:if test="${controlStatus eq 'In Progress'}">
 <script language="Javascript">
   setTimeout('window.location.reload()', 30000);
 </script>
</c:if>

<!--  CURRENT STATUS TITLE -->
<c:choose>
 <c:when test="${empty isDetail}">
  <tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" beanName="tabKey"/>
  </tiles:insert>
 </c:when>
 <c:otherwise>
  <tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" beanName="tabKey"/>
   <tiles:put name="tabName" beanName="controlCurrentStatus" beanProperty="action"/>
  </tiles:insert>
 </c:otherwise>
</c:choose>
<!--  /  -->

<%-- pick image: completed/inprogress/error --%>
<c:choose>
 <c:when test="${controlStatus eq 'In Progress'}">
  <c:set var="statusImage" value="/images/status_bar.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Progress"/>
  <c:if test="${section eq 'group'}">
   <c:url var="detailsLink" value="/resource/${section}/Control.do">
    <c:param name="mode" value="crntDetail"/>
    <c:param name="rid" value="${param.rid}"/>
    <c:param name="type" value="${param.type}"/>
    <c:param name="bid" value="${requestScope.bid}"/> 
   </c:url>
   <c:set var="statusMsg"><c:out value="${statusMsg}"/>&nbsp;<html:link href="${detailsLink}"><fmt:message key="resource.group.ControlStatus.Link.Details"/></html:link></c:set>
  </c:if>
 </c:when>
 <c:when test="${controlStatus eq 'Failed'}">
  <c:set var="statusImage" value="/images/status_error.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Error"/>
 </c:when>
 <c:when test="${controlStatus eq 'Completed'}">
  <c:set var="statusImage" value="/images/status_complete.gif"/>
  <fmt:message var="statusMsg" key="resource.group.ControlStatus.Content.Completed"/>
 </c:when>
</c:choose>

<!--  CURRENT STATUS CONTENTS -->
<c:choose>
<c:when test="${controlStatus eq 'Failed' || controlStatus eq 'In Progress' || controlStatus eq 'Completed' }">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Action"/></td>
		<td width="30%" class="BlockContent"><c:out value="${controlCurrentStatus.action}"/></td>
		<td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>
		<td width="30%" class="BlockContent"><c:out value="${controlCurrentStatus.description}"/></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Status"/></td>
		<td width="30%" class="BlockContent"><html:img page="${statusImage}" width="50" height="12" border="0"/>&nbsp;<c:out value="${statusMsg}" escapeXml="false"/></td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Started"/></td>
		<td width="30%" class="BlockContent"><hq:dateFormatter value="${controlCurrentStatus.startTime}"/></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.ErrorDescr"/></td>
		<td width="30%" class="BlockContent"><c:out value="${controlCurrentStatus.errorStr}"/></td>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Sched"/></td>
		<td width="30%" class="BlockContent"><hq:dateFormatter value="${controlCurrentStatus.dateScheduled}"/></td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.ControlStatus.Label.Elapsed"/></td>
		<td width="30%" class="BlockContent"><hq:dateFormatter time="true" value="${controlCurrentStatus.duration}"/></td>
		<td width="20%" class="BlockLabel">&nbsp;</td>
		<td width="30%" class="BlockContent">&nbsp;</td>
	</tr>
        <c:if test="${controlStatus eq 'Completed'}">
         <tr valign="top">
          <td width="20%" class="BlockLabel">&nbsp;</td>
           <c:choose>
            <c:when test="${empty isDetail}">
             <html:form action="/resource/${section}/control/RemoveCurrentStatus">
              <td width="80%" class="BlockContent" colspan="3"><html:link href="javascript:document.RemoveControlForm.submit()"><fmt:message key="resource.server.ControlStatus.Link.Clear"/></html:link></td>
              <html:hidden property="rid" value="${Resource.id}"/>
              <html:hidden property="type" value="${Resource.entityId.type}"/>
              <html:hidden property="controlActions" value="${requestScope.bid}" />              
             </html:form>
            </c:when>
            <c:otherwise>
             <td width="80%" class="BlockContent" colspan="3">&nbsp;</td>
            </c:otherwise>
           </c:choose>
         </tr>
        </c:if>
	</tr>
        <tr>
         <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
</table>
</c:when> <%-- end inprogress/error/completed block --%>
<c:otherwise>
 <table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
   <td class="BlockContent" width="100%"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/><i><fmt:message key="resource.server.ControlStatus.Content.None"/></i></td>
  </tr>
  <tr>
   <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:otherwise>
</c:choose>
<!--  /  -->

