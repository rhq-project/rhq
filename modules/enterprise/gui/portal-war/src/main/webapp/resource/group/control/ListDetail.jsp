<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%-- Tile for displaying a detailed status list for a group control action,
     whether current or history.
    
    @param returnUrl The url displayed at the bottom and top of the table
    @param returnKey The i18n key for the link text
--%>
<tiles:importAttribute name="returnUrl"/>
<tiles:importAttribute name="returnKey"/>
<tiles:importAttribute name="noButtons" ignore="true"/>

<c:set var="section" value="group"/>

<c:set var="selfAction" value="/resource/${section}/Control.do?mode=${param.mode}&rid=${Resource.id}&type=${Resource.entityId.type}&bid=${param.bid}"/>

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

<c:set var="listSize" value="${request.listSize}"/>
<c:set var="page" value="${hstDetailAttr}"/>
<c:set var="widgetInstanceName" value="controlAction"/>
<c:set var="fullReturnUrl" value="${returnUrl}&rid=${Resource.id}&type=${Resource.entityId.type}"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
  var pageData = new Array();
  initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
  widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="entityId" value="${Resource.entityId}"/>
<tiles:insert definition=".page.title.resource.group">
  <tiles:put name="titleName"><hq:inventoryHierarchy resourceId="666"/></tiles:put>
</tiles:insert>
            
<tiles:insert definition=".page.return">
 <tiles:put name="returnUrl" beanName="fullReturnUrl"/>
 <tiles:put name="returnKey" beanName="returnKey"/>
</tiles:insert>

<!-- HISTORY FORM -->
<tiles:insert definition=".resource.common.control.currentStatus">
 <tiles:put name="tabKey" value="resource.group.ControlDetail.Tab"/>
 <tiles:put name="isDetail" value="true"/>
</tiles:insert>

<html:form action="/resource/group/control/RemoveHistory">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>


<c:forEach items="${hstDetailAttr}" var="tmpDetail" end="1">
 <c:choose>
  <c:when test="${tmpDetail.entityType eq '1'}">
   <c:set var="resourceType" value="service"/>    
  </c:when>
  <c:when test="${tmpDetail.entityType eq '2'}">
   <c:set var="resourceType" value="server"/>       
  </c:when>
  <c:when test="${tmpDetail.entityType eq '5'}">
   <c:set var="resourceType" value="group"/>       
  </c:when>
 </c:choose>
</c:forEach>

<display:table cellspacing="0" cellpadding="0" width="100%" action="${selfActionUrl}"
               items="${hstDetailAttr}" var="hstDetail"> 
  <display:column width="16%" property="entityName" sort="true" sortAttr="9"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Resource" 
                  href="/resource/${resourceType}/Inventory.do?mode=view&type=${hstDetail.entityType}" paramId="rid" paramProperty="entityId" />
  <display:column width="10%" property="status" sort="true" sortAttr="10"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Status" /> 
  <display:column width="20%" property="startTime" sort="true" sortAttr="11"
                  defaultSort="true" title="resource.group.ControlDetail.ListHeader.Started">
   <display:datedecorator/>
  </display:column>
  <display:column width="16%" property="duration" sort="true" sortAttr="12"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Elapsed"> 
   <display:datedecorator isElapsedTime="true"/>
  </display:column> 
  <display:column width="20%" property="dateScheduled" sort="true" sortAttr="13"
                  defaultSort="false" title="resource.group.ControlDetail.ListHeader.Scheduled"> 
   <display:datedecorator/>
  </display:column>
  <display:column width="38%" property="errorStr"
                  defaultSort="false" title="common.header.Description"/>
</display:table>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listItems" beanName="page"/>
  <tiles:put name="noButtons" value="true"/>
  <tiles:put name="listSize" beanName="hstDetailAttr" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction" />
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="11"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
</tiles:insert>
<!-- /  -->

<c:if test="${empty noButtons}">
 <%-- delete the current control action --%>
 <html:hidden property="controlActions" value="${param.bid}"/>
 <tiles:insert definition=".form.buttons.deleteCancel"/>
</c:if>
</html:form>

<tiles:insert definition=".page.return">
 <tiles:put name="returnUrl" beanName="fullReturnUrl"/>
 <tiles:put name="returnKey" beanName="returnKey"/>
</tiles:insert>
            
