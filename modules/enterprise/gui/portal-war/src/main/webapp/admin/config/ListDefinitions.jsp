<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listAlerts"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  

function setActiveInactive() {
    document.RemoveConfigForm.setActiveInactive.value='y';
    document.RemoveConfigForm.submit();
}
</script>

<hq:pageSize var="pageSize"/>
<c:url var="pnAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
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
<c:url var="psAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="/alerts/Config.do">
  <c:param name="mode" value="list"/>
  <c:param name="aetid" value="${param.aetid}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>
<c:set var="newAction" value="/alerts/Config.do?mode=new&aetid=${param.aetid}"/>

<c:set var="tmpTitle" value=".page.title.resource.${section}"/>
<tiles:insert beanName="tmpTitle">
  <tiles:put name="titleName"><html:link page="/admin/config/EditDefaults.do?mode=monitor"><fmt:message key="admin.resource.templates.PageTitle"/></html:link> >
        <c:out value="${ResourceType.name}"/> <c:out value="${section}"/>s</tiles:put>
</tiles:insert>

<!-- FORM -->
<html:form action="/admin/alerts/RemoveConfig">
<html:hidden property="aetid"/>
<c:if test="${not empty param.so}">
  <html:hidden property="so" value="${param.so}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <html:hidden property="sc" value="${param.sc}"/>
</c:if>

<display:table cellspacing="0" cellpadding="0" width="100%" action="${sortAction}"
               items="${Definitions}" >
  
  <display:column width="1%" property="alertDefId" 
                   title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
                   isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="definitions" onclick="ToggleSelection(this,widgetProperties)" styleClass="listMember"/>
  </display:column>
  <display:column width="20%" property="name" sort="true" sortAttr="1"
                  defaultSort="false" title="alerts.config.DefinitionList.ListHeader.AlertDefinition" href="/alerts/Config.do?mode=viewRoles&aetid=${param.aetid}" paramId="ad" paramProperty="alertDefId"/>
    
  <display:column width="20%" property="description"
                  title="common.header.Description" >
</display:column>

  <display:column width="20%" property="ctime" sort="true" sortAttr="2"
                  defaultSort="true" title="alerts.config.DefinitionList.ListHeader.DateCreated" >
<display:datedecorator/>
</display:column>
                  
  <display:column width="20%" property="enabled"
                  title="alerts.config.DefinitionList.ListHeader.Active">
    <display:booleandecorator flagKey="yesno"/>
</display:column>

</display:table>

  <tiles:insert definition=".toolbar.list">
    <tiles:put name="listNewUrl" beanName="newAction"/> 
    <tiles:put name="listItems" beanName="Definitions"/>
    <tiles:put name="listSize" beanName="listSize"/>
    <tiles:put name="pageNumAction" beanName="pnAction"/>
    <tiles:put name="pageSizeAction" beanName="psAction"/>
    <tiles:put name="defaultSortColumn" value="1"/>
    <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
    <tiles:put name="goButtonLink" value="javascript:setActiveInactive()"/>
  </tiles:insert>

<html:hidden property="setActiveInactive"/>
</html:form>
<!-- /  -->
 <tiles:insert definition=".page.footer"/>

