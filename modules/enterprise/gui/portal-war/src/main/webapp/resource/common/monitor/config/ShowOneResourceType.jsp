<%@ page import="java.util.SortedSet" %>
<%@ page import="org.rhq.core.domain.resource.ResourceType" %>
<%@ page import="org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite" %>
<%@ page import="java.util.Map" %>
<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="servicesMap"  />
<tiles:importAttribute name="resourceType" />
<tiles:importAttribute name="indent" ignore="true"/>

<%
    // find the right List within the map and put that in page context
	Map<Integer,SortedSet<ResourceTypeTemplateCountComposite>> serviceMap = (Map)pageContext.getAttribute("servicesMap");
    if (serviceMap==null)
       return;

    ResourceTypeTemplateCountComposite ids = (ResourceTypeTemplateCountComposite)pageContext.getAttribute("resourceType");
	if (ids==null) {
	   return;
	}
	int id = ids.getType().getId();	
	SortedSet<ResourceTypeTemplateCountComposite> childServices = serviceMap.get(id);
	if (childServices==null)
	   return;
	pageContext.setAttribute("servicesList", childServices);
%>


<c:forEach var="serviceType" items="${servicesList}">
   <tr class="ListRow">
      <td class="ListCellPrimary"><%-- iterate over indent variable and put spacer imgs at the front --%>
         <c:choose>
            <c:when test="${not empty indent}">
               <c:forEach begin="1" end="${indent}">
                  <html:img page="/images/spacer.gif" width="16" height="1" border="0" />
               </c:forEach>
            </c:when>
            <c:otherwise>
               <c:set var="indent" value="0" />
            </c:otherwise>
         </c:choose> 
         <html:img page="/images/icon_indent_arrow.gif" width="16" height="16" border="0" /> 
         <c:out value="${serviceType.type.name}" />
      </td>
      <td class="ListCell" align="left" nowrap="nowrap">
         <c:if test="${monitorEnabled}">
            <c:if test="${not empty param.nomenu}">
               <html:link page="/admin/platform/monitor/Config.do?nomenu=true&mode=configure&id=${serviceType.type.id}&type=${serviceType.type.id}" styleClass="buttonsmall">
                  Edit Metric Template
               </html:link>
            </c:if>
            <c:if test="${empty param.nomenu}">
               <html:link page="/admin/platform/monitor/Config.do?mode=configure&id=${serviceType.type.id}&type=${serviceType.type.id}" styleClass="buttonsmall">
                  Edit Metric Template
               </html:link>
            </c:if>
         <c:if test="${(serviceType.enabledMetricCount + serviceType.disabledMetricCount) > 0}">
            <span title="(enabled | disabled)">  
               (<c:out value="${serviceType.enabledMetricCount}" /> | <c:out value="${serviceType.disabledMetricCount}" />)
            </span>
         </c:if>
         </c:if>
      </td>
      <td class="ListCell" align="left" nowrap="nowrap">
         <c:if test="${monitorEnabled}">
            <html:link page="/rhq/admin/listAlertTemplates.xhtml?type=${serviceType.type.id}" styleClass="buttonsmall">
               Edit Alert Templates
            </html:link>
            <c:if test="${(serviceType.enabledAlertCount + serviceType.disabledAlertCount) > 0}">
               <span title="(enabled | disabled)">  
                  (<c:out value="${serviceType.enabledAlertCount}" /> | <c:out value="${serviceType.disabledAlertCount}" />)
               </span>
            </c:if>
         </c:if>
      </td>
   </tr>

    <%-- Now call ourslves with a larger indent to see if this entry has children  --%>
    <tiles:insert
       definition=".resource.common.monitor.config.ShowOneResourceType">
       <tiles:put name="resourceType" beanName="serviceType" />
       <tiles:put name="servicesMap" beanName="servicesMap" />
       <tiles:put name="indent" value="${indent + 1}" />
    </tiles:insert>
    
</c:forEach>
