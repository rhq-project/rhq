<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
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
    <display:checkboxdecorator name="emails" onclick="ToggleSelection(this,widgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column width="99%" property="emailAddress" title="alert.config.props.NB.Email"/>
</display:table>

</c:otherwise>
</c:choose>
