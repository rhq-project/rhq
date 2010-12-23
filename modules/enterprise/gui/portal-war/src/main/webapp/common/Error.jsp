<%@ page language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="org.rhq.core.clientapi.util.StringUtil" %>
<%@ page import="org.rhq.enterprise.server.auth.SessionNotFoundException"%>
<%@ page import="org.rhq.enterprise.server.auth.SessionTimeoutException"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>

<%
// XXX: move this all into an action
/* get the exception from one of the many places it could be hiding */
if (exception == null)
    exception = (Exception)request.getAttribute("javax.servlet.error.exception");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.EXCEPTION");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.ActionErrors");

request.setAttribute(PageContext.EXCEPTION, exception);

/* guarantee that our exceptions aren't throwables */
Exception root = null;
try {
    if (exception != null) {
        if (exception instanceof ServletException) {
            ServletException se = (ServletException) exception;
            root = (Exception) se.getRootCause();
%>
<c:set var="root">
<%= root %>
</c:set>
<%
        }
    }
}
catch (ClassCastException ce) {
    // give up on having a printable root exception
}

/* if the bizapp session is invalid, so should ours be */
if (root != null &&
    root instanceof SessionNotFoundException ||
    root instanceof SessionTimeoutException) {
    session.invalidate();
    // XXX: include a "session timed out" page
}
%>

<c:set var="exception">
<%= exception %>
</c:set>

<%
int randomNum=(int)(Math.random()*1000);
%>

<c:if test="${not empty param.errorMessage}">
	<div id="errorMessage<%= randomNum %>" style="visibility:hidden"><fmt:message key="${param.errorMessage}"/></div>
</c:if>

<c:catch> 
  <c:if test="${not empty exception}">
      <c:set var="stacktrace"><%=StringUtil.getStackTrace(exception)%></c:set>
      <div id="exception<%= randomNum %>" style="visibility:hidden"><c:out value="${stacktrace}" /></div>
    <c:if test="${not empty root}"> 
      <c:set var="rootStacktrace"><%=StringUtil.getStackTrace(root)%></c:set>
      <div id="root<%= randomNum %>" style="visibility:hidden"><c:out value="${rootStacktrace}" /></div>
    </c:if> 
  </c:if> 
</c:catch>

<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<script type="text/javascript">
/*--- start declaration/initialization ---*/
var exDiv = document.getElementById("exception<%= randomNum %>");
if (exDiv!=null) {
	exDiv.style.display = "none";
	var exText<%= randomNum %> = exDiv.innerHTML;
}
else
	var exText<%= randomNum %> = "";

var rootDiv = document.getElementById("root<%= randomNum %>");
if (rootDiv!=null) {
	rootDiv.style.display = "none";
	var rootText<%= randomNum %> = rootDiv.innerHTML;
}
else
	var rootText<%= randomNum %> = "";

var errorDiv = document.getElementById("errorMessage<%= randomNum %>");
if (errorDiv!=null) {
	errorDiv.style.display = "none";
	var errorText<%= randomNum %> = errorDiv.innerHTML;
}
else
	var errorText<%= randomNum %> = "";
/*--- end declaration/initialization ---*/

document.write(
"<td>\n" + 
"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" + 
"	<tr>\n" + 
"		<td class=\"ErrorBlock\"><img src=\"<html:rewrite page="/images/"/>tt_error.gif\" width=\"10\" height=\"11\" hspace=\"5\" border=\"0\"/></td>\n" + 
"		<td class=\"ErrorBlock\" width=\"100%\"><fmt:message key="errors.jsp.problem"/> <a href=\"javascript:displayStackTrace<%= randomNum %>()\"><fmt:message key="errors.jsp.ClickHere"/></a> <fmt:message key="errors.jsp.ToSee"/></td>\n" + 
"	</tr>\n" + 
"</table>\n" + 
"</td>\n" + 
"<tr>\n"
);
	
function displayStackTrace<%= randomNum %>() {
	errorPopup = open("","errorPopup<%= randomNum %>","width=750,height=600,resizable=yes,scrollbars=yes,left=200,top=10");
	errorPopup.document.open();
	errorPopup.document.write("<html><title><fmt:message key="errors.jsp.problem"/></title>");
	errorPopup.document.write("<body>\n" + 
	"<link rel=stylesheet href=\"<html:rewrite page="/css/win.css"/>\" type=\"text/css\">" +
	"<a name=\"top\"></a>\n" + 
	"<div id=\"PageHeader\" align=\"right\"><a href=\"javascript:window.close()\"><fmt:message key="common.action.window.close"/></a></div>\n" + 
	"<div align='center'>\n" + 
	"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" + 
	"    <tr>\n" + 
	"      <td>\n" + 
	"				<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"				  <tr><td class=\"BlockTitle\" width=\"100%\">Exception:</td></tr>\n" +
	"				</table>\n" +
	"			 </td>\n" + 
	"    </tr>\n" + 
	"    <tr>\n" + 
	"      <td class=\"BlockContent\"><blockquote>\n" + exText<%= randomNum %> + "</blockquote></td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>\n" + 
	"    <tr>\n" + 
<c:if test="${not empty root}">
	"      <td>\n" + 
	"				<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"				  <tr><td class=\"BlockTitle\" width=\"100%\">Root cause:</td></tr>\n" +
	"				</table>\n" +
	"			 </td>\n" + 
	"    </tr>\n" + 
	"    <tr>\n" + 
	"      <td class=\"BlockContent\"><blockquote>\n" + rootText<%= randomNum %> + "</blockquote></td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>" +
</c:if>
    "\n");

	if (errorDiv!=null) {
	errorPopup.document.write(
	"    <tr>\n" + 
	"        <td class=\"BlockContent\">\n" + 
	"            <b>"+ errorText<%= randomNum %> +"</b>\n" + 
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
