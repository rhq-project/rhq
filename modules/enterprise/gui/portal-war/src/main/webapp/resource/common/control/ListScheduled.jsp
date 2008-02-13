<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="section" ignore="true"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listServerControl"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<c:set var="tmpTitle"> - <fmt:message key="resource.server.ControlSchedule.SubTitle"/></c:set>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.ControlSchedule.Title"/>
  <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>

<html:form action="/resource/${section}/control/RemoveControlJobs">

<html:hidden property="type" value="${param.type}"/>
<html:hidden property="rid" value="${param.rid}"/>

<c:set var="selfAction" value="/resource/${section}/Control.do?mode=view&rid=${Resource.id}&type=${Resource.entityId.type}"/>

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

<div id="listDiv">
  <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfActionUrl}"
                  items="${ctrlActionsSrvAttr}" var="sched" >
   <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="controlJobs" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
   </display:column>
   <display:column width="20%" property="action" sort="true" sortAttr="9"
                   defaultSort="true" title="resource.server.ControlSchedule.ListHeader.Action"
                   href="/resource/${section}/Control.do?mode=edit&type=${Resource.entityId.type}&rid=${Resource.id}" paramId="bid" paramProperty="id" nowrap="true" />
   <display:column width="16%" property="nextFireTime" title="resource.server.ControlSchedule.ListHeader.NextFire"  nowrap="true" sort="true" sortAttr="15" defaultSort="false">
      <display:datedecorator/>
   </display:column>
   <display:column width="30%" value="${sched.scheduleValue.scheduleString}" title="resource.server.ControlSchedule.ListHeader.Sched"   
                   headerStyleClass="ListHeaderInactive" /> 
   <display:column width="33%" value="${sched.scheduleValue.description}" title="common.header.Description" 
                   headerStyleClass="ListHeaderInactive" />
  </display:table>
</div>


<c:set var="newServerControlUrl" value="/resource/${section}/Control.do?mode=new&rid=${Resource.id}&type=${Resource.entityId.type}"/>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" beanName="newServerControlUrl"/>
  <tiles:put name="listItems" beanName="ctrlActionsSrvAttr"/>
  <tiles:put name="listSize" beanName="ctrlActionsSrvAttr" beanProperty="totalSize" />
  <tiles:put name="pageSizeAction" beanName="psAction" />
  <tiles:put name="pageNumAction" beanName="pnAction"/>    
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="defaultSortColumn" value="9"/>
</tiles:insert>

</html:form>
