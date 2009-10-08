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
    <display:checkboxdecorator name="users" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
  </display:column>
  <display:column width="15%" property="subject.firstName" title="alert.config.props.NB.FirstName"/>
  <display:column width="15%" property="subject.lastName" title="alert.config.props.NB.LastName"/>
  <display:column width="20%" property="subject.name" title="alert.config.props.NB.Username" href="/admin/user/UserAdmin.do?mode=view" paramId="u" paramProperty="subject.id"/>
  <display:column width="29%" property="subject.emailAddress" title="alert.config.props.NB.Email" autolink="true"/>
  <display:column width="20%" property="subject.department" title="alert.config.props.NB.Dept"/>
</display:table>

</c:otherwise>
</c:choose>
