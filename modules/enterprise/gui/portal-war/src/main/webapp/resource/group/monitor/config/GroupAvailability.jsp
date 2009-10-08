<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- this tile is designed to be included from resource/common/monitor/config/ConfigMetrics
  -- if the resource is a compatible group
  --
  -- @author csherr
  --%>

<tiles:importAttribute name="section" ignore="true"/>
<tiles:importAttribute name="Resource"/>

<!--  GROUP AVAILABILITY TITLE -->
<c:set var="tmpTitle"> - <fmt:message key="resource.group.monitor.visibility.config.GroupAvailSubTab"/></c:set>

<tiles:insert definition=".header.tab">
 <tiles:put name="tabKey" value="resource.group.monitor.visibility.config.GroupAvailTab"/>
 <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>
<!--  /  -->

<!--  GROUP AVAILABILITY CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr>
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupAvailLabel"/></td>
  <td width="30%" class="BlockContent"><fmt:message key="resource.group.monitor.visibility.config.When"/> XXX <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupUnavailLabel"/></td>
  <td width="30%" class="BlockContent"><fmt:message key="resource.group.monitor.visibility.config.When"/> XXX <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
 </tr>
</table>

<!-- EDIT TOOLBAR -->
<c:set var="editUrl" value="/resource/group/monitor/Config.do?mode=edit&rid=${Resource.id}&type=${Resource.entityId.type}"/>
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>