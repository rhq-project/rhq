<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%-- the context-relative url for the "add to list button", and the
  -- (optional) name and value for a request parameter to be attached
  -- to the url.
  --%>
<tiles:importAttribute name="addToListParamName" ignore="true"/>
<tiles:importAttribute name="addToListParamValue" ignore="true"/>
<tiles:importAttribute name="useDisableBtn" ignore="true"/>
<tiles:importAttribute name="showChangeSchedules" ignore="true"/>

<c:choose>
   <c:when test="${not empty useDisableBtn && useDisableBtn}">
      <c:set var="removeImg" value="/images/tbb_disablecollection_gray.gif"/>
   </c:when>
   <c:otherwise>
      <c:set var="removeImg" value="/images/tbb_removefromlist_gray.gif"/>
   </c:otherwise>
</c:choose>

<%--
  -- shows flag indicating whether to show the addToList button or not
  -- set this value to false to not show the addToList button.
  --%>
<tiles:importAttribute name="showAddToListBtn" ignore="true"/>

<c:if test="${empty showAddToListBtn}">
   <c:set var="showAddToListBtn" value="true"/>
   <tiles:importAttribute name="addToListUrl"/>
</c:if>

<%-- the unique name of the list widget to which this toolbar is
  -- attached; used to enable/disable the "remove from list" button
  --%>
<tiles:importAttribute name="widgetInstanceName" ignore="true"/>

<%-- the collection of items displayed in the attached list widget
  --%>
<tiles:importAttribute name="pageList"/>

<%-- the root-relative url that the paging widgets use to refresh the
  -- web page
  --%>
<tiles:importAttribute name="pageAction"/>


<%-- whether or not to show the paging controls
  --%>
<tiles:importAttribute name="showPagingControls" ignore="true"/>

<%-- whether or not to show interval form controls. See 2.1.5 mockup for an example.
  --%>
<tiles:importAttribute name="showIntervalControls" ignore="true"/>

<%-- a postfix used to differentiated between two lists on the same page
  --%>
<tiles:importAttribute name="postfix" ignore="true"/>

<script type="text/javascript">
   var goButtonLink;
</script>

<c:choose>
   <c:when test="${not empty addToListParamName && not empty addToListParamValue}">
      <c:url var="addToListUrl" value="${addToListUrl}">
         <c:param name="${addToListParamName}" value="${addToListParamValue}"/>
      </c:url>
   </c:when>
   <c:otherwise>
      <c:url var="addToListUrl" value="${addToListUrl}"/>
   </c:otherwise>
</c:choose>

<!-- ADD TO LIST TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <c:if test="${not empty showChangeSchedules && showChangeSchedules}">
   <tr>
	 <td colspan="4" align="left"><html:checkbox property="schedulesShouldChange"/>
        <fmt:message key="resource.common.monitor.visibility.config.UpdateExisting"/>
     </td>
   </tr>
  </c:if>
   <tr>
      <c:if test="${showAddToListBtn == true}">
         <td width="40">
            <html:link href="${addToListUrl}">
               <html:img page="/images/tbb_addtolist.gif" width="85" height="16" border="0"/>
            </html:link>
         </td>
      </c:if>

      <td width="40" id="<c:out value="${widgetInstanceName}"/>DeleteButtonTd">
         <div id="<c:out value="${widgetInstanceName}"/>DeleteButtonDiv">
            <html:image page="${removeImg}" border="0" property="remove"/>
         </div>
      </td>
      
      <c:if test="${not empty showIntervalControls and showIntervalControls}">
         <td class="BoldText" nowrap="nowrap">
            <fmt:message key="resource.common.monitor.visibility.config.CollectionIntervalForSelectedLabel"/>
         </td>
         <td>
            <html:text size="4" maxlength="4" property="collectionInterval"/>
         </td>
         <td>
            <html:select styleClass="FilterFormText" property="collectionUnit">
               <html:option value="1000">
                  <fmt:message key="resource.common.monitor.visibility.config.Seconds"/>
               </html:option>
               <html:option value="60000">
                  <fmt:message key="resource.common.monitor.visibility.config.Minutes"/>
               </html:option>
               <html:option value="3600000">
                  <fmt:message key="resource.common.monitor.visibility.config.Hours"/>
               </html:option>
            </html:select>
         </td>
         <td width="100%" id="<c:out value="${widgetInstanceName}"/>GoButtonTd">
            <div id="<c:out value="${widgetInstanceName}"/>GoButtonDiv">
               <html:img page="/images/tbb_go_gray.gif" border="0"/>
            </div>
         </td>
      </c:if>

      <c:choose>
         <c:when test="${empty showPagingControls or showPagingControls}">
            <tiles:insert definition=".controls.paging">
               <tiles:put name="pageList" beanName="pageList"/>
               <tiles:put name="postfix" value="${postfix}" />
               <tiles:put name="action" beanName="pageAction"/>
            </tiles:insert>
         </c:when>
         <c:otherwise>
            <td width="100%">&nbsp;</td>
         </c:otherwise>
      </c:choose>
   </tr>

   <%-- need another row to display collectionInterval error message --%>
   <c:if test="${not empty showIntervalControls and showIntervalControls}">
      <logic:messagesPresent property="collectionInterval">
         <tr>
            <td width="40">&nbsp;</td>
            <td width="40">&nbsp;</td>
            <td class="ErrorField" nowrap="nowrap" colspan="3"><span class="ErrorFieldContent">- <html:errors
                  property="collectionInterval"/></span></td>
            <td width="100%">&nbsp;</td>
         </tr>
      </logic:messagesPresent>
   </c:if>

</table>
<!-- / -->
