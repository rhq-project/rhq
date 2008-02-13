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
   var widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>'); 
   widgetProperties["buttonType"] = "css";   
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

<hq:constant var="CHART_VIEW"
             classname="org.rhq.enterprise.gui.legacy.action.resource.hub.HubView"
             symbol="CHART"/>
<hq:constant var="LIST_VIEW"
             classname="org.rhq.enterprise.gui.legacy.action.resource.hub.HubView"
             symbol="LIST"/>

<c:choose>
   <c:when test="${ResourceHubForm.resourceCategory == PLATFORM}">
      <fmt:message var="entityTypeTH" key="resource.type.Platform"/>
      <fmt:message var="resourceTypeTH" key="resource.hub.PlatformTypeTH"/>
   </c:when>
   <c:when test="${ResourceHubForm.resourceCategory == SERVER}">
      <fmt:message var="entityTypeTH" key="resource.type.Server"/>
      <fmt:message var="resourceTypeTH" key="resource.hub.ServerTypeTH"/>
   </c:when>
   <c:when test="${ResourceHubForm.resourceCategory == SERVICE}">
      <fmt:message var="entityTypeTH" key="resource.type.Service"/>
      <fmt:message var="resourceTypeTH" key="resource.hub.ServiceTypeTH"/>
   </c:when>
</c:choose>


<c:url var="resourceTypeAction" value="/ResourceHub.do">
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
   <c:if test="${not empty param.resourceType}">
      <c:param name="resourceType" value="${param.resourceType}"/>
   </c:if>
   <c:param name="resourceCategory" value="${ResourceHubForm.resourceCategory}"/>
   <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>



<html:form method="GET" action="/ResourceHub">
 
<tiles:insert definition=".page.title.resource.hub">
   <html:hidden property="resourceCategory" value="${ResourceHubForm.resourceCategory}"/>
   <tiles:put name="titleName">
      <c:out value="${navHierarchy}"/>
   </tiles:put>
   <tiles:put name="showSearch" value="true"/>
</tiles:insert>
</html:form>

<html:form method="GET" action="/resource/hub/RemoveResource.do">


<c:if test="${not empty ResourceSummary}">
   <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
         <td class="ResourceHubBlockTitle" width="100%">
            <c:choose>
               <c:when test="${ResourceHubForm.resourceCategory == PLATFORM}">
                  <fmt:message key="resource.hub.filter.platform"/>
                  ( <c:out value="${ResourceSummary.platformCount}"/> )
               </c:when>
               <c:otherwise>
                  <c:url var="platformUrl" value="/ResourceHub.do">
                     <c:param name="resourceCategory" value="${PLATFORM}"/>
                     <c:param name="view" value="${ResourceHubForm.view}"/>
                  </c:url>
                  <html:link href="${platformUrl}">
                     <fmt:message key="resource.hub.filter.platform"/>
                     ( <c:out value="${ResourceSummary.platformCount}"/> )
                  </html:link>
               </c:otherwise>
            </c:choose>
            |
            <c:choose>
               <c:when test="${ResourceHubForm.resourceCategory == SERVER}">
                  <fmt:message key="resource.hub.filter.server"/>
                  ( <c:out value="${ResourceSummary.serverCount}"/> )
               </c:when>
               <c:otherwise>
                  <c:url var="serverUrl" value="/ResourceHub.do">
                     <c:param name="resourceCategory" value="${SERVER}"/>
                     <c:param name="view" value="${ResourceHubForm.view}"/>
                  </c:url>
                  <html:link href="${serverUrl}">
                     <fmt:message key="resource.hub.filter.server"/>
                     ( <c:out value="${ResourceSummary.serverCount}"/> )
                  </html:link>
               </c:otherwise>
            </c:choose>
            |
            <c:choose>
               <c:when test="${ResourceHubForm.resourceCategory == SERVICE}">
                  <fmt:message key="resource.hub.filter.service"/>
                  ( <c:out value="${ResourceSummary.serviceCount}"/> )
               </c:when>
               <c:otherwise>
                  <c:url var="serviceUrl" value="/ResourceHub.do">
                     <c:param name="resourceCategory" value="${SERVICE}"/>
                     <c:param name="view" value="${ResourceHubForm.view}"/>
                  </c:url>
                  <html:link href="${serviceUrl}">
                     <fmt:message key="resource.hub.filter.service"/>
                     ( <c:out value="${ResourceSummary.serviceCount}"/> )
                  </html:link>
               </c:otherwise>
            </c:choose>
            |

                  <c:url var="groupUrl" value="/GroupHub.do">
                     <c:param name="groupCategory" value="${COMPAT_GROUP}"/>
                     <c:param name="view" value="${ResourceHubForm.view}"/>
                  </c:url>
                  <html:link href="${groupUrl}">
                     <fmt:message key="resource.hub.filter.compatibleGroups"/>
                     ( <c:out value="${ResourceSummary.compatibleGroupCount}"/> )
                  </html:link>

            |

                  <c:url var="groupUrl" value="/GroupHub.do">
                     <c:param name="groupCategory" value="${MIXED_GROUP}"/>
                     <c:param name="view" value="${ResourceHubForm.view}"/>
                  </c:url>
                  <html:link href="${groupUrl}">
                     <fmt:message key="resource.hub.filter.mixedGroups"/>
                     ( <c:out value="${ResourceSummary.mixedGroupCount}"/> )
                  </html:link>
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
<table width="100%" cellpadding="0" cellspacing="0" border="0">
   <tr>
      <td class="FilterLine" colspan="5">
         <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
      </td>
   </tr>
   <tr>
      <td class="FilterLabelText" nowrap align="right">
         <c:choose>
         <c:when test="${ResourceHubForm.types == null}">
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

      <c:url var="viewAction" value="/ResourceHub.do">
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

         <c:param name="resourceCategory" value="${ResourceHubForm.resourceCategory}"/>
         <c:choose>
            <c:when test="${ResourceHubForm.view == LIST_VIEW}">
               <c:param name="view" value="CHART"/>
            </c:when>
            <c:otherwise>
               <c:param name="view" value="LIST"/>
            </c:otherwise>
         </c:choose>
      </c:url>

<%-- Commented out for now -- See JBNADM-1925 
      <c:choose>
         <c:when test="${ResourceHubForm.view == LIST_VIEW}">
--%>         
            <td class="FilterImage" width="104">
               <html:link href="${viewAction}">
                  <html:img page="/images/SubHub_ChartView_off.gif" alt="Chart View" width="104" height="15" border="0"
                            onmouseover="imageSwap (this, imagePath + 'SubHub_ChartView', '_over')"
                            onmouseout="imageSwap (this, imagePath + 'SubHub_ChartView', '_off')"/>
               </html:link>
            </td>
            <td class="FilterImage" width="104">
               <html:img page="/images/SubHub_ListView_on.gif" alt="List View" width="104" height="15" border="0"/>
            </td>
<%-- Commented out for now -- See JBNADM-1925 
         </c:when>
         <c:otherwise> <!-- CHART view -->
            <td class="FilterImage" width="104">
               <html:img page="/images/SubHub_ChartView_on.gif" alt="Chart View" width="104" height="15" border="0"/>
            </td>
            <td class="FilterImage" width="104">
               <html:link href="${viewAction}">
                  <html:img page="/images/SubHub_ListView_off.gif" alt="List View" width="104" height="15" border="0"
                            onmouseover="imageSwap (this, imagePath + 'SubHub_ListView', '_over')"
                            onmouseout="imageSwap (this, imagePath + 'SubHub_ListView', '_off')"/>
               </html:link>
            </td>
         </c:otherwise>
      </c:choose>
--%>         

      <td class="FilterImage" width="5">
         <html:img page="/images/spacer.gif" width="5" height="1" border="0"/>
      </td>
   </tr>
</table>
<!-- / -->

<!-- RESOURCE HUB CONTENTS -->
<c:url var="sAction" value="/ResourceHub.do">
   <c:if test="${not empty param.keywords}">
      <c:param name="keywords" value="${param.keywords}"/>
   </c:if>
   <c:if test="${not empty param.resourceType}">
      <c:param name="resourceType" value="${param.resourceType}"/>
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
   <c:param name="resourceCategory" value="${ResourceHubForm.resourceCategory}"/>
   <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>


<%-- Commented out for now -- See JBNADM-1925 
<c:choose>
   <c:when test="${ResourceHubForm.view == LIST_VIEW}">
--%>   
      <display:table items="${AllResources}" var="resourceComposite" action="${sAction}"
                     width="100%" cellspacing="0" cellpadding="0" styleId="resourceHub">

         <display:column width="1%" property="resource.id"
                         title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"
                         isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
         </display:column>
         
         <display:column width="5%" title="nbsp" nowrap="true">
            <display:resource-quicknav-decorator/>
         </display:column>

         <display:column width="25%" property="resource.name" title="${entityTypeTH}"
                         isLocalizedTitle="false" sortAttr="res.name"
                         href="/resource/common/monitor/Visibility.do?id=${resourceComposite.resource.id}&mode=currentHealth"/>

         <c:if test="${not empty resourceTypeTH}">
            <display:column width="25%" property="resource.resourceType.name"
                            title="${resourceTypeTH}" isLocalizedTitle="false" sortAttr="res.resourceType.name"/>
         </c:if>

         <display:column width="30%" property="resource.description" title="common.header.Description" sortAttr="res.description"/>

         <display:column width="15%" property="availability" title="resource.common.monitor.visibility.AvailabilityTH"
                         styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle"
                         sortAttr="a.availabilityType" href="/admin/browser.jsp?entityClass=org.rhq.core.domain.resource.Resource&key=${resourceComposite.resource.id}">
            <display:availabilitydecorator/>
         </display:column>
         
      </display:table>
<%-- Commented out for now -- See JBNADM-1925 
   </c:when>
   <c:otherwise> <!-- CHART view -->
      <tiles:insert definition=".resource.common.monitor.visibility.minicharts">
         <tiles:put name="Resources" beanName="AllResources"/>
      </tiles:insert>
   </c:otherwise>
</c:choose>
--%>
<!-- / -->


<c:url var="pageAction" value="/ResourceHub.do">
   <c:if test="${not empty param.keywords}">
      <c:param name="keywords" value="${param.keywords}"/>
   </c:if>  
   <c:if test="${not empty param.resourceType}">
      <c:param name="resourceType" value="${param.resourceType}"/>
   </c:if>
   <c:param name="resourceCategory" value="${param.resourceCategory}"/>
   <c:param name="view" value="${ResourceHubForm.view}"/>

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

<tiles:insert definition=".toolbar.list">
   <tiles:put name="listNewUrl" value="/ResourceHub.do"/>
   <tiles:put name="deleteOnly" value="true"/>
   <tiles:put name="pageList" beanName="AllResources"/>
   <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
   <tiles:put name="pageAction" value="${pageAction}"/>
   <tiles:put name="useCssButtons" value="true"/>
</tiles:insert>

<html:hidden property="view"/>
<html:hidden property="resourceCategory"/>
<html:hidden property="resourceType"/>
<html:hidden property="keywords"/>

</html:form>
<tiles:insert definition=".page.footer"/>

<script type="text/javascript">
   clearIfAnyChecked();
</script>