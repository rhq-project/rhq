<%@ page language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="nameSort" ignore="false" scope="page"/>
<tiles:importAttribute name="typeSort" ignore="false"/>

<script language="JavaScript" src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listResources"/>
<script type="text/javascript">
   var pageData = new Array();
   var FOO = "chart";
   var LIST = "list";
   var imagePath = "<html:rewrite page="/images/"/>";

   initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
   widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<hq:constant var="PLATFORM"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="PLATFORM"/>
<hq:constant var="SERVER"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVER"/>
<hq:constant var="SERVICE"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVICE"/>

<hq:constant var="COMPAT_GROUP"
             classname="org.rhq.core.domain.resource.group.GroupCategory"
             symbol="COMPATIBLE"/>
<hq:constant var="MIXED_GROUP"
             classname="org.rhq.core.domain.resource.group.GroupCategory"
             symbol="MIXED"/>

<c:choose>
   <c:when test="${GroupHubForm.groupCategory == COMPAT_GROUP}">
      <fmt:message var="resourceTypeTH" key="resource.hub.GroupTypeTH"/>
   </c:when>
   <%--
       Since mixed groups don't have a resourceType, do NOT set the
       resourceTypeTH, which will suppress the display of that column
       for Mixed groups.
    --%>
</c:choose>


<html:form method="GET" action="/GroupHub">
   <html:hidden property="groupCategory" value="${GroupHubForm.groupCategory}"/>
   <tiles:insert definition=".page.title.resource.hub">
      <tiles:put name="titleName">
         <c:out value="${navHierarchy}"/>
      </tiles:put>
      <tiles:put name="showSearch" value="true"/>
   </tiles:insert>
</html:form>


<html:form method="GET" action="/resource/hub/RemoveGroup.do">

   <c:if test="${not empty ResourceSummary}">
   <table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
   <td class="ResourceHubBlockTitle" width="100%">

   <c:url var="platformUrl" value="/ResourceHub.do">
      <c:param name="resourceCategory" value="${PLATFORM}"/>
   </c:url>
   <html:link href="${platformUrl}">
      <fmt:message key="resource.hub.filter.platform"/>
      ( <c:out value="${ResourceSummary.platformCount}"/> )
   </html:link>
   |

   <c:url var="serverUrl" value="/ResourceHub.do">
      <c:param name="resourceCategory" value="${SERVER}"/>
   </c:url>
   <html:link href="${serverUrl}">
      <fmt:message key="resource.hub.filter.server"/>
      ( <c:out value="${ResourceSummary.serverCount}"/> )
   </html:link>
   |

   <c:url var="serviceUrl" value="/ResourceHub.do">
      <c:param name="resourceCategory" value="${SERVICE}"/>
   </c:url>
   <html:link href="${serviceUrl}">
      <fmt:message key="resource.hub.filter.service"/>
      ( <c:out value="${ResourceSummary.serviceCount}"/> )
   </html:link>
   |

   <c:choose>
   <c:when test="${GroupHubForm.groupCategory == COMPAT_GROUP}">
      <fmt:message key="resource.hub.filter.compatibleGroups"/>
      ( <c:out value="${ResourceSummary.compatibleGroupCount}"/> )
   </c:when>
   <c:otherwise>
      <c:url var="groupUrl" value="/GroupHub.do">
         <c:param name="groupCategory" value="${COMPAT_GROUP}"/>
      </c:url>
      <html:link href="${groupUrl}">
         <fmt:message key="resource.hub.filter.compatibleGroups"/>
         ( <c:out value="${ResourceSummary.compatibleGroupCount}"/> )
      </html:link>
   </c:otherwise>
   </c:choose>
   |
   <c:choose>
      <c:when test="${GroupHubForm.groupCategory == MIXED_GROUP}">
         <fmt:message key="resource.hub.filter.mixedGroups"/>
         ( <c:out value="${ResourceSummary.mixedGroupCount}"/> )
      </c:when>
      <c:otherwise>
         <c:url var="groupUrl" value="/GroupHub.do">
            <c:param name="groupCategory" value="${MIXED_GROUP}"/>
         </c:url>
         <html:link href="${groupUrl}">
            <fmt:message key="resource.hub.filter.mixedGroups"/>
            ( <c:out value="${ResourceSummary.mixedGroupCount}"/> )
         </html:link>
      </c:otherwise>
   </c:choose>
   |
   <html:link href="/rhq/definition/group/list.xhtml">
      <fmt:message key="resource.hub.filter.groupDefinitions"/>
      ( <c:out value="${ResourceSummary.groupDefinitionCount}"/> )
   </html:link>
   </td>
   </tr>
   </table>
   </c:if>

   <tiles:insert definition=".portlet.confirm"/>
   <tiles:insert definition=".portlet.error"/>

   <!-- FILTER TOOLBAR CONTENTS -->

   <c:url var="resourceTypeAction" value="/GroupHub.do">
      <c:if test="${not empty param.keywords}">
         <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
      <c:if test="${not empty param.ps}">
         <c:param name="ps" value="${param.ps}"/>
      </c:if>
      <c:if test="${not empty param.so}">
         <c:param name="so" value="${param.so}"/>
      </c:if>
      <c:if test="${not empty param.sc}">
         <c:param name="sc" value="${param.sc}"/>
      </c:if>
      <c:param name="groupCategory" value="${GroupHubForm.groupCategory}"/>
   </c:url>

   <table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
   <td class="FilterLine" colspan="2">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
   </td>
   </tr>
   <tr>
   <td class="FilterLabelText" nowrap align="right">
      <c:choose>
      <c:when test="${GroupHubForm.types == null}">
         <html:hidden property="resourceType" value=""/>
         &nbsp;
      </c:when>
      <c:otherwise>
      <fmt:message key="Filter.ViewLabel"/>
   </td>
   <td class="FilterLabelText" width="100%">
      <html:select property="resourceType" styleClass="FilterFormText" size="1"
                   onchange="goToSelectLocation(this, 'resourceType', '${resourceTypeAction}');">
         <html:optionsCollection property="types"/>
      </html:select>
      </c:otherwise>
      </c:choose>
   </td>

   <c:url var="viewAction" value="/GroupHub.do">
      <c:if test="${not empty param.keywords}">
         <c:param name="keywords" value="${param.keywords}"/>
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
      <c:if test="${not empty param.resourceType}">
         <c:param name="resourceType" value="${param.resourceType}"/>
      </c:if>

      <c:param name="groupCategory" value="${GroupHubForm.groupCategory}"/>
   </c:url>

   </tr>
   </table>
   <!-- / -->

   <!-- GROUP HUB CONTENTS -->
   <c:url var="sAction" value="/GroupHub.do">
      <c:if test="${not empty param.keywords}">
         <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
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
      <c:if test="${not empty param.resourceType}">
         <c:param name="resourceType" value="${param.resourceType}"/>
      </c:if>
      <c:param name="groupCategory" value="${GroupHubForm.groupCategory}"/>
   </c:url>


      <display:table items="${AllResources}" var="groupComposite" action="${sAction}"
                     width="100%" cellspacing="0" cellpadding="0" styleId="resourceHub">

         <display:column width="1%" property="resourceGroup.id"
                         title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"
                         isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
         </display:column>

         <display:column width="5%" value="${groupComposite}" title="nbsp" nowrap="true">
            <display:group-quicknav-decorator/>
         </display:column>

         <c:choose>
            <c:when test="${GroupHubForm.groupCategory == 'COMPATIBLE'}">
               <display:column width="25%" property="resourceGroup.name" title="common.header.Group"
                               isLocalizedTitle="true" sortAttr="rg.name"
                               href="/rhq/group/monitor/graphs.xhtml?category=${GroupHubForm.groupCategory}&groupId=${groupComposite.resourceGroup.id}"/>
            </c:when>
            <c:otherwise>
               <display:column width="25%" property="resourceGroup.name" title="common.header.Group"
                               isLocalizedTitle="true" sortAttr="rg.name"
                               href="/rhq/group/inventory/view.xhtml?category=${GroupHubForm.groupCategory}&groupId=${groupComposite.resourceGroup.id}"/>
            </c:otherwise>
         </c:choose>
         <c:if test="${not empty resourceTypeTH}">
            <display:column width="25%" property="resourceGroup.resourceType.name" title="${resourceTypeTH}"
                            isLocalizedTitle="false" sortAttr="resType.name">
            </display:column>
         </c:if>

         <display:column width="30%" property="resourceGroup.description"
                         title="common.header.Description" sortAttr="rg.description"/>

         <display:column width="10%" property="explicitFormatted" title="resource.common.monitor.visibility.ChildAvailabilityTH"
                         headerStyleClass="ListHeaderCheckbox" valign="middle"
                         sortAttr="explicitAvail"/>

         <display:column width="10%" property="implicitFormatted" title="resource.common.monitor.visibility.DescendentAvailabilityTH"
                         headerStyleClass="ListHeaderCheckbox" valign="middle"
                         sortAttr="implicitAvail"/>

      </display:table>
   <!-- / -->

   <c:url var="pageAction" value="/GroupHub.do">
      <c:if test="${not empty param.keywords}">
         <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
      <c:if test="${not empty param.resourceType}">
         <c:param name="resourceType" value="${param.resourceType}"/>
      </c:if>

      <c:param name="groupCategory" value="${GroupHubForm.groupCategory}"/>
      
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

   <%-- I am a bit weary of this as the listNewUrl won't work in RHQ. --%>
   <tiles:insert definition=".toolbar.list">
      <tiles:put name="listNewUrl" value="/resource/platform/Inventory.do?mode=new"/>
      <tiles:put name="deleteOnly" value="true"/>
      <tiles:put name="pageList" beanName="AllResources"/>
      <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
      <tiles:put name="pageAction" beanName="pageAction"/>
   </tiles:insert>

   <html:hidden property="view"/>
   <html:hidden property="groupCategory"/>

   </html:form>
   <tiles:insert definition=".page.footer"/>

   <script type="text/javascript">
   clearIfAnyChecked();
   </script>
