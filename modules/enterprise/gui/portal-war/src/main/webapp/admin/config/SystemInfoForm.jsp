<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.TimeZone" %>
<%@ page import="org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.core.domain.common.ServerDetails" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!--  SYSTEM-WIDE TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.SystemTab"/>
</tiles:insert>
<!--  /  -->

<%
   Subject subject = SessionUtils.getWebUser(session).getSubject();
   ServerDetails info = LookupUtil.getSystemManager().getServerDetails(subject);
%>
<!--  SYSTEM-WIDE CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td nowrap="nowrap" width="45%" class="BlockLabel"><fmt:message key="admin.settings.TimeZone"/></td>
    <td nowrap="nowrap" width="55%" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.SERVER_TIMEZONE) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.LocalTime"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.SERVER_LOCAL_TIME) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseURL"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.DATABASE_CONNECTION_URL) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductName"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.DATABASE_PRODUCT_NAME) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductVersion"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.DATABASE_PRODUCT_VERSION) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverName"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.DATABASE_DRIVER_NAME) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverVersion"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.DATABASE_DRIVER_VERSION) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RawTable"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.CURRENT_MEASUREMENT_TABLE) %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RotationTime"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDetails().get(ServerDetails.Detail.NEXT_MEASUREMENT_TABLE_ROTATION) %></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
