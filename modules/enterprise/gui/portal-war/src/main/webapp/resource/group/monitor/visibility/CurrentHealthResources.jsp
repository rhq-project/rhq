<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="showProblems" ignore="true"/>
<tiles:importAttribute name="showOptions" ignore="true"/>

<c:if test="${showProblems}">
  <script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
</c:if>

<tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>

<table width="285" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
      <tiles:insert definition=".resource.group.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" value="false"/>
        <tiles:put name="childResourcesTypeKey" value="resource.common.inventory.security.ResourceTypeLabel"/>
        <tiles:put name="checkboxes" beanName="false"/>
      </tiles:insert>
    </td>
  </tr>
</table>
