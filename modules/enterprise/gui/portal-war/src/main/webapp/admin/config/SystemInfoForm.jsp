<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.TimeZone" %>
<%@ page import="org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility" %>
<%@ page import="org.rhq.enterprise.server.util.SystemInformation" %>

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
   SystemInformation info = SystemInformation.getInstance();
   DateFormat localTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);
%>
<!--  SYSTEM-WIDE CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td nowrap="nowrap" width="45%" class="BlockLabel"><fmt:message key="admin.settings.TimeZone"/></td>
    <td nowrap="nowrap" width="55%" class="BlockContent"><%= TimeZone.getDefault().getDisplayName()%></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.LocalTime"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= localTimeFormatter.format(new Date(System.currentTimeMillis()))%></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseURL"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDatabaseConnectionURL() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductName"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDatabaseProductName() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductVersion"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDatabaseProductVersion() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverName"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDatabaseDriverName() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverVersion"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= info.getDatabaseDriverVersion() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RawTable"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= MeasurementDataManagerUtility.getCurrentRawTable() %></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RotationTime"/></td>
    <td nowrap="nowrap" class="BlockContent"><%= MeasurementDataManagerUtility.getNextRotationTime() %></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
