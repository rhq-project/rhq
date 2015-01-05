<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.resource.CreateResourceHistory" %>
<%@ page import="org.rhq.core.domain.resource.CreateResourceStatus" %>
<%@ page import="org.rhq.core.domain.resource.DeleteResourceHistory" %>
<%@ page import="org.rhq.core.domain.resource.DeleteResourceStatus" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
    PageList<CreateResourceHistory> creates;
    PageList<DeleteResourceHistory> deletes;
    try {
        creates = resourceFactoryManager.findCreateChildResourceHistory(subject, resourceId, begin, end, new PageControl(0, 100));
        deletes = resourceFactoryManager.findDeleteChildResourceHistory(subject, resourceId, begin, end, new PageControl(0, 100));
    } catch (Exception e) {
        creates = new PageList<CreateResourceHistory>(0, PageControl.getUnlimitedInstance());
        deletes = new PageList<DeleteResourceHistory>(0, PageControl.getUnlimitedInstance());
    }
%>


{ "events": [

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (CreateResourceHistory entry : creates) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (entry.getStatus()) {
                case SUCCESS:
                    icon = "/portal/images/icn_create_child_success.png";
                    break;
                case FAILURE:
                    icon = "/portal/images/icn_create_child_failed.png";
                    break;
                case IN_PROGRESS:
                    icon = "/portal/images/icn_create_child.png";
                    break;
                default:
                    icon = "/portal/images/icn_create_child.png"; // TODO
            }

            String link = "/coregui/CoreGUI.html#Resource/" + resourceId  + "/Inventory/ChildHistory";
            String username = entry.getSubjectName();
            String resourceName = entry.getCreatedResourceName();
            %>

{ "start" : "<%=sdf.format(entry.getCreatedTime())%>",
  "title" : "Child Resource Created",
  "link" : "<%=link%>",
  "description" : "<b>Resource Name:</b> <%=(""+resourceName).replaceAll("[\"']","").trim()%><br/><b>User:</b> <%=(""+username).replaceAll("[\"']","").trim()%><br/><b>Status:</b> <%=(""+entry.getStatus())%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(entry.getStatus() != CreateResourceStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }

        for (DeleteResourceHistory entry : deletes) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (entry.getStatus()) {
                case SUCCESS:
                    icon = "/portal/images/icn_delete_child_success.png";
                    break;
                case FAILURE:
                    icon = "/portal/images/icn_delete_child_failed.png";
                    break;
                case IN_PROGRESS:
                    icon = "/portal/images/icn_delete_child.png";
                    break;
                default:
                    icon = "/portal/images/icn_delete_child.png"; // TODO
            }

            String link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Inventory/ChildHistory";
            String username = entry.getSubjectName();
            String description = "<b>User:</b> "+ username.replaceAll("[\"']","").trim() + "<br/><b>Status:</b> " + entry.getStatus();
            if (entry.getResourceName() != null) {
               description = "<b>Resource Name:</b> " +  entry.getResourceName().replaceAll("[\"']","").trim() + "<br/>" + description;
            }
    %>
{ "start" : "<%=sdf.format(entry.getCreatedTime())%>",
  "title" : "Child Resource Deleted",
  "link" : "<%=link%>",
  "description" : "<%= description %>",
  "icon" : "<%=icon%>",
  "color" : "<%=(entry.getStatus() != DeleteResourceStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}
    <%
        }
    %>
]}
