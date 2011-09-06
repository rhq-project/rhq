<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.resource.ResourceManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.core.domain.resource.Resource" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    Resource resource = resourceManager.getResource(subject, resourceId);
%>

{ "events": [

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);
        String link = "/coregui/CoreGUI.html#Resource/" + resourceId;
        String iconDiscovered = "/images/icons/Inventory_grey_16.png";
        String iconCommitted = "/images/icons/Inventory_16.png";
    %>

{ "start" : "<%=sdf.format(resource.getCtime())%>",
  "title" : "Initial Discovery",
  "link" : "<%=link%>",
  "description" : "Time when the resource was initially discovered by its managing agent.",
  "icon" : "<%=iconDiscovered%>",
  "color" : "#A0A0A0"
},

{ "start" : "<%=sdf.format(resource.getItime())%>",
  "title" : "Committed to Inventory",
  "link" : "<%=link%>",
  "description" : "Time when the resource was committed to inventory.",
  "icon" : "<%=iconCommitted%>",
  "color" : "#3333FF"
}

]}
