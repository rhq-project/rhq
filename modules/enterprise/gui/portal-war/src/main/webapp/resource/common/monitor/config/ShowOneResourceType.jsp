<%@ page import="java.util.List" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="org.rhq.core.domain.resource.ResourceType" %>
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
	Map<Integer,SortedSet<ResourceType>> serviceMap = (Map)pageContext.getAttribute("servicesMap");
    if (serviceMap==null)
       return;

	ResourceType ids = (ResourceType)pageContext.getAttribute("resourceType");
	if (ids==null) {
	   return;
	}
	int id = ids.getId();	
	SortedSet<ResourceType> childServices = serviceMap.get(id);
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
         <c:out value="${serviceType.name}" />
      </td>
      <td class="ListCell" align="center" nowrap="nowrap">
         <c:if test="${monitorEnabled}">
         <html:link page="/admin/platform/monitor/Config.do?mode=configure&id=${serviceType.id}&type=${serviceType.id}" styleClass="buttonsmall">
            Edit Metric Template
         </html:link>
         </c:if>
      </td>
      <td class="ListCell" align="center" nowrap="nowrap">
         <c:if test="${monitorEnabled}">
            <html:link page="/rhq/admin/listAlertTemplates.xhtml?type=${serviceType.id}" styleClass="buttonsmall">
                Edit Alert Templates
            </html:link>
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
