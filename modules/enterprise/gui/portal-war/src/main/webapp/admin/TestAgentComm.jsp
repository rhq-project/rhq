<%@ page
    language="java"
    contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil"%>
<%@page import="org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean"%>
<%@page import="org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration"%>
<%@page import="org.rhq.enterprise.communications.command.client.ClientCommandSender"%>
<%@page import="org.rhq.enterprise.server.util.LookupUtil"%>
<%@page import="org.rhq.core.domain.resource.Agent"%>
<%@page import="org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory"%>
<%@page import="org.rhq.enterprise.gui.legacy.util.SessionUtils"%>
<%@page import="org.rhq.enterprise.communications.Ping"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Test Agent Communications</title>
</head>
<body>

<h1>Testing Agent Communications</h1>

<p>
This page allows you to test whether or not this RHQ Server can send messages to any given RHQ Agent.
</p><p>
In order to send a message, the RHQ Agent must already be registered with the RHQ Server.
You can specify either the agent name or the agent's host/port (if you specify a name, the host/port
fields will be ignored).
</p><p>
You can specify the number of messages you want to send via the messageCount field.
</p><p>
The async field determines if you want to send the message(s) asynchronously.
The guaranteed field determines if you want to guarantee the delivery of the message(s).
The sendThrottled field determines if you want to throttle the messages that are to be sent.
</p>

<%
   if (SessionUtils.getWebUser(session).getSubject().getId() > 2) // no one but rhqadmin can view this page
   {
      throw new IllegalAccessException("You do not have admin permissions");
   }

   String  agentName       = request.getParameter("agentName");
   String  agentHost       = request.getParameter("agentHost");
   String  agentPort       = request.getParameter("agentPort");
   String  timeoutStr      = request.getParameter("timeout");
   boolean async           = Boolean.parseBoolean( request.getParameter("async") );
   boolean guaranteed      = Boolean.parseBoolean( request.getParameter("guaranteed") );
   boolean sendThrottled   = Boolean.parseBoolean( request.getParameter("sendThrottled") );
   String  messageCountStr = request.getParameter("messageCount");
   
   if (timeoutStr == null) timeoutStr = "";
   Long    timeout         = (timeoutStr.trim().length() > 0) ? Long.valueOf(messageCountStr) : null;
   int     messageCount    = (messageCountStr != null ) ? Integer.parseInt(messageCountStr) : 1;

   boolean skip  = false;
   Agent   agent = null;
   
   if ( agentName != null && agentName.length() > 0 )
   {
      agent = LookupUtil.getAgentManager().getAgentByName( agentName );
      agentHost = agentPort = "";
   }
   else if ( agentHost != null && agentHost.length() > 0 )
   {
      agent = LookupUtil.getAgentManager().getAgentByAddressAndPort( agentHost, Integer.parseInt( agentPort ) );
      agentName = "";
   }
   else
   {
      skip = true;
      agentName = agentHost = agentPort = "";
   }
%>

<form action="TestAgentComm.jsp" method="get">
   <table border="1">
   <tr><td>Agent Name: </td><td><input name="agentName" type="text" size="40" value="<%= agentName %>" /></td></tr>
   <tr><td>Agent Host: </td><td><input name="agentHost" type="text" size="40" value="<%= agentHost %>" /></td></tr>
   <tr><td>Agent Port: </td><td><input name="agentPort" type="text" size="10" value="<%= agentPort %>" /></td></tr>
   <tr><td>Timeout: </td><td><input name="timeout" type="text" size="10" value="<%= timeoutStr %>" /></td></tr>
   <tr><td>Async: </td><td><input name="async" type="checkbox" value="true" <%= async ? "checked" : "" %> /></td></tr>
   <tr><td>Guaranteed: </td><td><input name="guaranteed" type="checkbox" value="true" <%= guaranteed ? "checked" : "" %> /></td></tr>
   <tr><td>SendThrottled: </td><td><input name="sendThrottled" type="checkbox" value="true" <%= sendThrottled ? "checked" : "" %> /></td></tr>
   <tr><td>Message Count: </td><td><input name="messageCount" type="text" size="40" value="<%= messageCount %>" /></td></tr>
   <tr><td></td><td><input name="submit" type="submit" value="Send Messages"/></td></tr>
   </table>
</form>

<%
   if (!skip)
   {
      if (agent == null)
         throw new IllegalArgumentException("There is no agent associated with name ["
                                            + agentName
                                            + "] or host:port of ["
                                            + agentHost + ":" + agentPort + "]");
      
      ServerCommunicationsServiceMBean comm = ServerCommunicationsServiceUtil.getService();
      ClientCommandSenderConfiguration senderConfig = comm.getConfiguration().getClientCommandSenderConfiguration();
      ClientCommandSender sender = comm.getServiceContainer().createClientCommandSender( agent.getRemoteEndpoint(), senderConfig );
      sender.startSending();
      
      try
      {
         ClientRemotePojoFactory pojoFactory = sender.getClientRemotePojoFactory();
         pojoFactory.setTimeout( timeout );
         pojoFactory.setAsynch( async, null );
         pojoFactory.setDeliveryGuaranteed( guaranteed );
         pojoFactory.setSendThrottled( sendThrottled );
         Ping pojo = (Ping) pojoFactory.getRemotePojo( Ping.class );
      
         out.write("<h2>Sending " + messageCount + " Messages To Agent</h2>\n");
         out.write("<h3>" + agent + "</h3>\n<hr/>\n");

         for (int i = 1; i <= messageCount; i++)
         {
            String results = pojo.ping("" + i, "ACK #");
            if (!async)
            {
               out.write(new java.util.Date() + ": " + results + "<br/>\n");
               out.flush();
            }
         }
      
         out.write("<hr/><b>Sent " + messageCount + " Messages.</b>\n<hr/>\n");
      }
      finally
      {
         sender.stopSending( false );
      }
   }
%>

</body>
</html>