<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${CAM_SYSLOG_ACTIONS_ENABLED}">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.Syslog.Title"/>
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <c:choose>
  <c:when test="${(! empty syslogActionForm.id) and (syslogActionForm.id > 0)}">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.MetaProject"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.metaProject}"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.Project"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.project}"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.Version"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.version}"/></td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr valign="top">
    <td colspan="2" class="BlockContent"><fmt:message key="alert.config.props.Syslog.NoSyslogAction"/></td>
  </tr>
  </c:otherwise>
  </c:choose>
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:if>
