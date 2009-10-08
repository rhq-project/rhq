<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="childServers"/>
<tiles:importAttribute name="childServices"/>

<tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" value=""/>
   <tiles:put name="tabName" value="Child Resources"/>
</tiles:insert>

<display:table items="${childServers}" cellspacing="0" cellpadding="0" width="100%" var="server">
   <display:column width="55%" property="name" sortAttr="res.name"
                   title="resource.platform.inventory.servers.ServerTH"
                   href="/resource/Inventory.do?mode=view&id=${server.id}"/>

   <display:column width="30%" property="description" title="common.header.Description"/>
   <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="15%"
                   styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
      <%--<display:availabilitydecorator resource="${server}"/>--%>

   </display:column>
</display:table>

<display:table items="${childServices}" cellspacing="0" cellpadding="0" width="100%" var="service">
   <display:column width="55%" property="name" sortAttr="res.name"
                   title="resource.server.inventory.services.ServiceTH"
                   href="/resource/Inventory.do?mode=view&id=${service.id}"/>

   <display:column width="30%" property="description" title="common.header.Description"/>
   <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="15%"
                   styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">

      <%--<display:availabilitydecorator resource="${service}"/>--%>

   </display:column>
</display:table>
