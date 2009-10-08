<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
   var pageData = new Array();
</script>

<c:set var="selfAction" value="/resource/Inventory.do?mode=view&id=${Resource.id}"/>

<tiles:insert definition=".page.title.resource.full">
   <tiles:put name="titleName"><%-- TODO <hq:inventoryHierarchy resourceId="${entityId.appdefKey}" /> --%></tiles:put>
   <tiles:put name="resource" beanName="Resource"/>
</tiles:insert>

<!-- TODO (ips): Insert different tile depending on resource category. -->
<tiles:insert definition=".tabs.resource.inventory.current">
   <tiles:put name="id" beanName="Resource" beanProperty="id"/>
   <tiles:put name="resourceType" beanName="Resource" beanProperty="resourceType.category"/>
</tiles:insert>
&nbsp;<br>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".resource.common.inventory.generalProperties.view">
   <tiles:put name="resource" beanName="Resource"/>
</tiles:insert>
&nbsp;<br>

<%-- TODO: List config props. --%>

<tiles:insert definition=".resource.inventory.childResources">
   <tiles:put name="childServers" beanName="ChildServers"/>
   <tiles:put name="childServices" beanName="ChildServices"/>
</tiles:insert>
&nbsp;<br>

<tiles:insert definition=".page.footer"/>

<script type="text/javascript">
   clearIfAnyChecked();
</script>
