<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="java.util.Arrays"%>
<%@page import="java.util.Map"%>
<%@page import="org.rhq.enterprise.gui.legacy.util.SessionUtils"%>
<%@page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@page import="org.rhq.enterprise.server.core.EmailManagerLocal"%>

<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
      <title>Testing RHQ Email configuration</title>
   </head>
   <body>

      <jsp:include page="/admin/include/adminTestLinks.html" flush="true" />

      <h1>Testing RHQ Email Configuration</h1>

      <p>
      The page allows you to confirm that the RHQ Email configuration
      is correct and can send emails successfully.
      </p>

      <%
         String  toAddresses      = request.getParameter("to");
         String  messageSubject   = request.getParameter("subject");
         String  messageBody      = request.getParameter("body");
         boolean useAlertTemplate = Boolean.parseBoolean(request.getParameter("alert"));

         boolean skip  = false;

         if ( toAddresses == null || toAddresses.trim().length() == 0)
         {
            skip = true;
            toAddresses = "";
         }

         if (messageSubject == null) {
             messageSubject = "test subject";
         }
         if (messageBody    == null) {
             messageBody = "test body";
         }

         String error = "(Press Submit To Test)";

         if ( !skip )
         {
            try {
               EmailManagerLocal email = LookupUtil.getEmailManagerBean();

               if (useAlertTemplate)
               {
                  Map<String, String> alertMessage =
                     email.getAlertEmailMessage("Test Resource Hierarchy", "Test Resource Name", "Test Alert Name", "!!! - High", "Jan 1, 1970", "Test Conditions", "http://localhost:7080",
                             null); // last param null -> default template
                  messageSubject=alertMessage.keySet().iterator().next();
                  messageBody=alertMessage.values().iterator().next();
               }

               email.sendEmail(Arrays.asList(toAddresses.split(",")), messageSubject, messageBody);
               error = "None";
            }
            catch (Exception e) {
               error = e.toString();
            }
         }
      %>

      <form action="email.jsp" method="get">
         <table border="1">
            <tr><td>Recipient Addresses: </td><td><input name="to" type="text" size="100" value="<%= toAddresses %>" /></td></tr>
            <tr><td>Message Subject: </td><td><input name="subject" type="text" size="100" value="<%= messageSubject %>" /></td></tr>
            <tr><td>Message Body: </td><td><textarea name="body" cols=100 rows=6><%= messageBody %></textarea></td></tr>
            <tr><td>Use Alert Template: </td><td><input name="alert" type="checkbox" value="true" <%= useAlertTemplate ? "checked" : "" %> /></td></tr>
            <tr><td></td><td><input name="submit" type="submit" value="SendEmail"/></td></tr>
         </table>
      </form>

      <h3>Error: <%= error %></h3>

   </body>
</html>
