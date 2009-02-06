<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.operation.OperationRequestStatus" %>
<%@ page import="org.rhq.core.domain.operation.ResourceOperationHistory" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.operation.OperationManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    PageList<ResourceOperationHistory> operations = operationManager.getCompletedResourceOperationHistories(
            subject, resourceId, new PageControl(0,100));
%>

{ "events": [


    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (ResourceOperationHistory operation : operations) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (operation.getStatus()) {
                case SUCCESS:
                    icon = "/images/icn_operation_success.png";
                    break;
                case FAILURE:
                    icon = "/images/icn_operation_failed.png";
                    break;
                case INPROGRESS:
                    icon = "/images/icn_operation_inprogress.png";
                    break;
                case CANCELED:
                    icon = "/images/icn_operation_canceled.png";
            }


            String link = "/rhq/resource/operation/resourceOperationHistoryDetails.xhtml?id=" + resourceId + "&opId=" + operation.getId();

//            out.write(
//                    "    <event start=\"" + sdf.format(new Date(operation.getCreatedTime())) + "\" " +
//                    "title=\"Operation: " + operation.getOperationDefinition().getDisplayName() + "\" \n" +
//                    "link=\"/rhq/resource/operation/resourceOperationHistoryDetails.xhtml?id=" + resourceId + "&amp;opId=" + operation.getId() + "\" " +
//                    "icon=\"" + icon + "\" >\n" +
//                            "&lt;b&gt;User:&lt;/b&gt; " + operation.getSubjectName() + "&lt;br/&gt;" +
//                            "&lt;b&gt;Status:&lt;/b&gt; " + operation.getStatus() +
//                    "    </event>\n\n");
%>

{ "start" : "<%=sdf.format(new Date(operation.getCreatedTime()))%>",
  "title" : "Operation: <%=operation.getOperationDefinition().getName()%>",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=operation.getSubjectName()%><br/><b>Status:</b> <%=operation.getStatus()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(operation.getStatus() == OperationRequestStatus.SUCCESS ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>
]
}