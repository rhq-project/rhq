<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>


<!-- Content Block Title: Senders -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.current.detail.notify.Tab"/>
</tiles:insert>

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><span style="height: 1px;"></span></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.current.detail.notify.Label"/></td>
    <td width="80%" class="BlockContent"><c:out value="${alertNotifCount}"/> <fmt:message key="alert.current.detail.notify.Message"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockContent"><span style="height: 1px;"></span></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>

<c:if test="${not empty Resource}">
 <c:if test="${!alertDef.deleted}">
  <hq:authorization permission="MANAGE_ALERTS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/rhq/common/alert/notification/details.xhtml?context=resource&contextId=${alertDef.id}&contextSubId=${Resource.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
  </c:if>
</c:if>

<c:if test="${not empty ResourceGroup}">
 <c:if test="${!alertDef.deleted}">
  <hq:authorization permission="MANAGE_ALERTS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/rhq/common/alert/notification/details.xhtml?context=group&contextId=${alertDef.id}&contextSubId=${ResourceGroup.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
  </c:if>
</c:if>

<c:if test="${not empty ResourceType}">
 <c:if test="${!alertDef.deleted}">
  <hq:authorization permission="MANAGE_SETTINGS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/rhq/common/alert/notification/details.xhtml?context=type&contextId=${alertDef.id}&contextSubId=${ResourceType.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
    </c:if>
</c:if>

