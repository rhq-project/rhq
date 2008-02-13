<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- CONSTANT DEFINITIONS -->

<hq:pageSize var="pageSize"/>
<c:set var="selfAction"    
        value="/resource/group/Inventory.do?mode=view&groupId=${group.id}&category=${category}" />
<c:set var="widgetInstanceName" value="listGroups"/>

<c:set var="addToListUrl" 
        value="/resource/group/Inventory.do?mode=addResources&groupId=${group.id}&category=${category}"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
  <!-- est="${not empty param.sos}"
    param name="sos" value="${param.sos}"
  :if -->

<c:url var="pageAction" value="${selfAction}">
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
</c:url>

<c:url var="tableAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
</c:url>



<html:form action="/resource/group/inventory/RemoveApp">
<html:hidden property="groupId" value="${group.id}"/>
<html:hidden property="category" value="${category}"/>

<!--  RESOURCES, COMPATIBLE TITLE -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockTitle" width="100%">
    <fmt:message key="resource.group.inventory.ResourcesTab"/>
        <span class="BlockSubtitle">
           <c:choose>
              <c:when test="${category == 'MIXED'}"> 
                <fmt:message key="resource.group.inventory.tab.Mixed"/>
              </c:when>
              <c:when test="${category == 'COMPATIBLE'}"> 
                <fmt:message key="resource.group.inventory.tab.Compatible">
                    <fmt:param value="${group.resourceType.name}"/>
                </fmt:message>
              </c:when>
            </c:choose>              
        </span>    
   </td>
    <td class="BlockTitle" align="right"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<!--  RESOURCES, COMPATIBLE CONTENTS -->
<div id="listDiv">
  <display:table var="resourceItem" cellspacing="0" cellpadding="0" width="100%" action="${tableAction}"
                  items="${groupResources}" >
    <display:column width="1%" property="resource.id"
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
        <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember" suppress="${resourceItem.explicit == false}" />
    </display:column>
    <display:column width="18%" property="resource.name" sortAttr="res.name" title="resource.group.inventory.NameTH"
                    href="/rhq/resource/inventory/view.xhtml?mode=view&id=${resourceItem.resource.id}" />
    <display:column width="18%" property="resource.resourceType.name" title="resource.group.inventory.TypeTH" sortAttr="res.resourceType.name" />
    <display:column width="44%" property="resource.description" title="common.header.Description" sortAttr="res.description" />
    <display:column width="10%" property="availability" title="resource.common.monitor.visibility.AvailabilityTH" 
    				styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle" sortAttr="a.availabilityType">
        <display:availabilitydecorator />
    </display:column>
  </display:table>                
  
</div>
<!--  /  -->

<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="pageAction" beanName="pageAction"/>
  <tiles:put name="pageList" beanName="groupResources"/>
</tiles:insert>

</html:form>
