<%@ page
    language="java"
    contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="org.rhq.enterprise.gui.legacy.util.SessionUtils"%>
<%@page import="org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Test Data Purge Job</title>
</head>
<body>

<h1>Testing Data Purge Job</h1>

<%
   if (SessionUtils.getWebUser(session).getSubject().getId() > 2) // no one but rhqadmin can view this page
   {
      throw new IllegalAccessException("You do not have admin permissions");
   }

   boolean purge = Boolean.parseBoolean( request.getParameter("purge") );
%>

<form action="TestDataPurgeJob.jsp" method="get">
   <table border="1">
   <tr><td>Run Purge: </td><td><input name="purge" type="checkbox" value="true" <%= purge ? "checked" : "" %> /></td></tr>
   <tr><td></td><td><input name="submit" type="submit" value="Do It"/></td></tr>
   </table>
</form>

<%
   if (purge)
   {
      DataPurgeJob.purgeNow();
   }
%>

</body>
</html>