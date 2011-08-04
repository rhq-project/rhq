<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.content.InstalledPackageHistory" %>
<%@ page import="org.rhq.core.domain.content.InstalledPackageHistoryStatus" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.content.ContentUIManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.core.domain.util.PageOrdering" %>
<%@ page import="org.rhq.core.domain.util.OrderingField" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    ContentUIManagerLocal contentManager = LookupUtil.getContentUIManager();

    PageList<InstalledPackageHistory> history = contentManager.getInstalledPackageHistoryForResource(resourceId, new PageControl(0,100, new OrderingField("iph.timestamp",PageOrdering.DESC)));

%>


{ "events": [
    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (InstalledPackageHistory installedPackage : history) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String extra = "";
            switch (installedPackage.getStatus()) {
                case DISCOVERED:
                case INSTALLED:
                    extra = "ok_";
                    break;
                case DELETED:
                    extra = "grey_";
                    break;
                case FAILED:
                    extra = "failed_";
                    break;
                default:
                    extra = "";
            }

            String icon = "/images/icons/Content_" + extra + "16.png";

            String link = "#Resource/" + resourceId + "/Content/Deployed"; 
            // "/rhq/resource/content/installed_package_details.xhtml?id=" + resourceId + "&currentPackageId=" + installedPackage.getId();

            %>
{ "start" : "<%=sdf.format(new Date(installedPackage.getTimestamp()))%>",
  "title" : "<% out.write(
  (installedPackage.getPackageVersion().getDisplayName()==null ? null : installedPackage.getPackageVersion().getDisplayName().replaceAll("[\"']","")) + " " +
  (installedPackage.getPackageVersion().getDisplayVersion() ==null ? null : installedPackage.getPackageVersion().getDisplayVersion().replaceAll("[\"']","")));%>",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <% out.write((installedPackage.getContentServiceRequest() == null ? "-Detected- " : installedPackage.getContentServiceRequest().getSubjectName().replaceAll("[\"']","").trim())+"<br/> <b>Version: "+(installedPackage.getPackageVersion().getDisplayVersion() ==null ? null :installedPackage.getPackageVersion().getDisplayVersion().replaceAll("[\"']","").trim())+" </b> <br/><b>Status:</b> "+installedPackage.getStatus());%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(installedPackage.getStatus() != InstalledPackageHistoryStatus.FAILED ? "#4EB84E" : "#DD5656")%>"
}
    <%
        }
    %>
]}