<%@ page language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.rhq.core.clientapi.util.StringUtil" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUserPreferences" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<%
// XXX: move this all into an action
/* get the exception from one of the many places it could be hiding */
if (exception == null)
    exception = (Exception)request.getAttribute("javax.servlet.error.exception");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.EXCEPTION");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.ActionErrors");
%>

<% 
if (exception instanceof javax.faces.application.ViewExpiredException) {
   WebUser webUser = SessionUtils.getWebUser(session);
   if (webUser != null) {
      WebUserPreferences prefs = webUser.getWebPreferences();
      // get the url BEFORE the one that threw the ViewExpiredException
      String lastURL = prefs.getLastVisitedURL(2);
      response.sendRedirect(lastURL);
   }
}
%>


<html>
<head>
<title><fmt:message key="error.Error.Title"/></title>
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<script language="JavaScript" src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
  var path = "<html:rewrite page="/images/"/>";
</script>
</head>
<body>

<% 
request.setAttribute(PageContext.EXCEPTION, exception);

if (exception != null) {
%>
<c:set var="exception"><%= exception %></c:set>
<% } %>




<div align="center" style="margin-top: 100px;">
<table width="400" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockTitle" width="100%"><fmt:message key="error.Error.Tab"/></td>
          <td class="BlockTitle" align="right"><html:link href="" onclick="window.open('${helpBaseURL}','help','width=800,height=650,scrollbars=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/tt_help.gif" width="16" height="16" border="0"/></html:link></td>
        </tr>
      </table>	
    </td>
  </tr>
  <tr>
    <td class="BlockContent" colspan="2">
      <p>
          <c:out value="${exception}"/>
      <fmt:message key="error.Error.ThePageRequestedEtc"/>  <br/>

      <c:if test="${exception != null}">
          <fmt:message key="error.Error.YouCan"/>
          <html:link href="javascript:displayStackTrace()">
            <fmt:message key="error.Error.StackTraceHereLink"/>
          </html:link>
       </c:if>
      <fmt:message key="error.Error.ReturnTo"/>
      <html:link href="javascript:history.back(1)"><fmt:message key="error.Error.PreviousPageLink"/></html:link> 
      <html:link page="/Dashboard.do"><fmt:message key="error.Error.DashboardLink"/></html:link> 
      <html:link page="/ResourceHub.do"><fmt:message key="error.Error.ResourceHubLink"/></html:link> 
      </p>


    </td>
  </tr>


    <% if (request.getAttribute("javax.servlet.error.message") != null) { %>
    <tr>
        <td class="ErrorBlock" colspan="2"><%=request.getAttribute("javax.servlet.error.message")%></td>
    </tr>
    <% } %>



  <tr>
    <td class="ErrorBlock" colspan="2"><b><html:errors/></b></td>
  </tr>
  <tr>
    <td class="BlockBottomLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
</table>
</div>
</body>
</html>


<c:if test="${param.errorMessage}">
	<div id="errorMessage" style="visibility:hidden"><fmt:message key="${param.errorMessage}"/></div>
</c:if>

<%
    int i = 0;
    while (exception != null) {

%>
        <div id="exceptionMessage<%=i%>" style="visibility:hidden"><%=exception.getLocalizedMessage()%></div>
        <div id="exception<%=i++%>" style="visibility:hidden"><%=StringUtil.getFirstStackTrace(exception)%></div>
<%
        exception = exception.getCause();
    }
%>

<script type="text/javascript">

    var exceptions = new Array();
    var exceptionMessages = new Array();
    var i = 0;

    var exDiv = document.getElementById("exception"+i);
    var exDivMsg = document.getElementById("exceptionMessage"+i);
    while (exDiv != null) {
        exDiv.style.display = "none";
        exDivMsg.style.display = "none";
        exceptions[i] = exDiv.innerHTML;
        exceptionMessages[i] = exDivMsg.innerHTML;
        i++;
        exDiv = document.getElementById("exception" + i);
        exDivMsg = document.getElementById("exceptionMessage" + i);
    }


var errorDiv = document.getElementById("errorMessage");
if (errorDiv!=null) {
    errorDiv.style.display = "none";
    var errorText = errorDiv.innerHTML;
}
else
    var errorText= "";
/*--- end declaration/initialization ---*/

function getRedirectURL(offset) {
    var url = history[offset];
    return url;
}

function displayStackTrace() {
	errorPopup = open("","errorPopup","width=750,height=600,resizable=yes,scrollbars=yes,left=200,top=10");
	errorPopup.document.open();
	errorPopup.document.write("<html><title><fmt:message key="errors.jsp.problem"/></title>");
	errorPopup.document.write("<body>\n" +
	"<link rel=stylesheet href=\"<html:rewrite page="/css/win.css"/>\" type=\"text/css\">" +
	"<a name=\"top\"></a>\n" + 
	"<div id=\"PageHeader\" align=\"right\"><a href=\"javascript:window.close()\"><fmt:message key="common.action.window.close"/></a></div>\n" + 
	"<div align='center'>\n" + 
	"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n");


    for (i = 0; i < exceptions.length; i++) {
        errorPopup.document.write(
    "    <tr> " +
    "        <td>\n" +
	"				<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"				  <tr><td class=\"BlockTitle\" width=\"100%\">" + exceptionMessages[i] + "</td></tr>\n" +
	"				</table>\n" +
	"			 </td>\n" + 
	"    </tr>\n" + 
	"    <tr>\n" + 
	"      <td class=\"BlockContent\"><blockquote>\n" + exceptions[i] + "</blockquote></td>\n" +
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>"
                );
    }


	if (errorDiv!=null) {
	errorPopup.document.write(
	"    <tr>\n" + 
	"        <td class=\"BlockContent\">\n" + 
	"            <b>"+ errorText +"</b>\n" + 
	"        </td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>\n"
	);
	}

	errorPopup.document.write(
	"</table>\n" + 
	"</div>\n" +
	"<br><br><br><a href=\"javascript:window.close()\"><fmt:message key="common.action.window.close"/></a>\n" + 
	"</body>\n</html>"
	);
	
	errorPopup.document.close(); 
}
</script>
