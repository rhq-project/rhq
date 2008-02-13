<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

  <tr valign="top">
    <td width="20%" class="BlockLabel" valign="top"><fmt:message key="alert.current.detail.notify.OR"/></td>
    <tiles:insert definition=".events.alert.view.notifications.list">
      <tiles:put name="listObjectProperty" value="${alert.alertNotificationLog.emails}"/>
    </tiles:insert>
  </tr>
