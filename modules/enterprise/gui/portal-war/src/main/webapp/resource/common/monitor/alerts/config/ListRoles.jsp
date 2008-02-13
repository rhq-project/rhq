<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<tiles:importAttribute name="selfUrl"/>

<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
<c:choose>
<c:when test="${null == notifyList || empty listSize}">
<!-- error occured -->
</c:when>
<c:otherwise>

<hq:pageSize var="pageSize"/>
<display:table cellspacing="0" cellpadding="0" width="100%" action="${selfUrl}" items="${notifyList}">
  <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
    <display:checkboxdecorator name="roles" onclick="ToggleSelection(this,widgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column width="20%" property="role.name" title="alert.config.props.NB.Name" href="/admin/role/RoleAdmin.do?mode=view" paramId="r" paramProperty="role.id"/>
  <display:column width="79%" property="role.description" title="common.header.Description"/>
</display:table>

</c:otherwise>
</c:choose>
