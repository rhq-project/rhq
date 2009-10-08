<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="groups"/>
<tiles:importAttribute name="selfAction"/>

<bean:define id="groupCount" name="groups" property="totalSize"/>
<c:set var="widgetInstanceName" value="listServerGroups"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
groupsWidgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="addToListUrl" value="/resource/${resource.entityId.typeName}/Inventory.do?mode=addGroups&rid=${resource.id}&type=${resource.entityId.type}"/>

<c:url var="psgAction" value="${selfAction}">
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<c:url var="pngAction" value="${selfAction}">
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<c:url var="sgAction" value="${selfAction}">
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
  <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
</c:url>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.groups.GroupsTab"/>
</tiles:insert>

<!--  GROUPS CONTENTS -->
<div id="<c:out value="${widgetInstanceName}"/>Div">
<display:table items="${groups}" var="group" action="${sgAction}" width="100%" cellspacing="0" cellpadding="0">
  <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, groupsWidgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
    <display:checkboxdecorator name="g" onclick="ToggleSelection(this, groupsWidgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column property="name" title="common.header.Group" href="/resource/group/Inventory.do?mode=view&type=5" paramId="rid" paramProperty="id" sort="true" sortAttr="5" defaultSort="true" width="25%"/>
  <display:column property="description" title="common.header.Description" width="75%"/>
</display:table>
</div>
<!--  /  -->

<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="listItems" beanName="groups"/>
  <tiles:put name="listSize" beanName="groupCount"/>
  <tiles:put name="pageSizeParam" value="psg"/>
  <tiles:put name="pageSizeAction" beanName="psgAction"/>
  <tiles:put name="pageNumParam" value="png"/>
  <tiles:put name="pageNumAction" beanName="pngAction"/>  
</tiles:insert>
