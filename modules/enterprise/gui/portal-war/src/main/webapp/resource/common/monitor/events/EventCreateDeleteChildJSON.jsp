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
    PageList<CreateResourceHistory> creates = resourceFactoryManager.getCreateChildResourceHistory(resourceId, begin, end, new PageControl(0,100));
    PageList<DeleteResourceHistory> deletes = resourceFactoryManager.getDeleteChildResourceHistory(resourceId, begin, end, new PageControl(0,100));
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
                    icon = "/images/icn_create_child_success.png";
                    break;
                case FAILURE:
                    icon = "/images/icn_create_child_failed.png";
                    break;
                case IN_PROGRESS:
                    icon = "/images/icn_create_child.png";
                    break;
                default:
                    icon = "/images/icn_create_child.png"; // TODO
            }

            String link = "/rhq/resource/inventory/view.xhtml?id=" + resourceId ;

            %>

{ "start" : "<%=sdf.format(entry.getCreatedTime())%>",
  "title" : "Child resource created",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=entry.getSubjectName()%><br/><b>Status:</b> <%=entry.getStatus()%>",
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
                    icon = "/images/icn_delete_child_success.png";
                    break;
                case FAILURE:
                    icon = "/images/icn_delete_child_failed.png";
                    break;
                case IN_PROGRESS:
                    icon = "/images/icn_delete_child.png";
                    break;
                default:
                    icon = "/images/icn_delete_child.png"; // TODO
            }

            String link = "/rhq/resource/inventory/view.xhtml?id=" + resourceId ;

    %>
{ "start" : "<%=sdf.format(entry.getCreatedTime())%>",
  "title" : "Child resource deleted",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=entry.getSubjectName()%><br/><b>Status:</b> <%=entry.getStatus()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(entry.getStatus() != DeleteResourceStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}
    <%
        }
    %>
]}