<%@ page import="java.net.URL" %>
<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<% response.setStatus(401);%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>

<%-- An example of forcing secure login only
<%

    if (!request.isSecure())
    {
        String reqUrl = HttpUtils.getRequestURL(request).toString();
        URL url = new URL(reqUrl);
        URL redirectUrl = new URL("https", url.getHost(), 7443, url.getPath());
        response.sendRedirect(redirectUrl.toExternalForm());
    }

%>
--%>

<html>
<head>
<title><fmt:message key="login.title"/></title>
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<script language="JavaScript" src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script language="JavaScript" src="<html:rewrite page="/js/rhq.js"/>" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
  if (top != self)
    top.location.href = self.document.location;

  var path = "<html:rewrite page="/images/"/>";
  
  // takes care of jsession garbage, because sometimes jsession gets tacked on, like this:
  // var path = "/images/;jsessionid=2FA16379595FE7804C33FEDB13FAB8D0";
  var semiIndex = path.indexOf(";");
  if (semiIndex!= -1)
    path = path.substring(0, semiIndex);
</script>
	<style>
		body {font: 12px verdana, arial, helvetica, "sans serif"; margin: 0; /* to avoid margins */
		     text-align: center; /* to correct the centering IE bug*/ }
		h1 {font: bold 14px verdana, arial, helvetica, "sans serif";}
		input {font: 12px verdana, arial, helvetica, "sans serif";}
		input.button_submit {padding-left: 10px; padding-right:10px; }
		.box {padding: 10px; border: solid 1px silver; background-color: #f6f6f6;}
      #LoginSupport { margin-left: auto; margin-right: auto;
    		width: 35em; text-align: left; /* to realign your text */
     	}
   </style>
<script language="JavaScript" type="text/javascript">var aboutWindowTitle = '<fmt:message key="about.Title"/>';</script>
</head>

<body>

<div id="PageHeader">

    <table width="100%" border="0" cellpadding="0" cellspacing="0" height="70px">
        <tr valign="bottom">
            <td align="left" rowspan="2">
                <fmt:message var="urlDomain" key="product.url.domain"/>
                <fmt:message var="productName" key="product.fullName"/>

                <a href="#" onclick="openAbout(aboutWindowTitle)">
                    <html:img page="/images/logo_header.png" title="${productName}"/>
                </a>
            </td>
            <td valign="top" align="right">
               <map name="redhat-jboss-logo-map">
                  <area href="http://www.redhat.com/" alt="Red Hat Homepage" title="Red Hat" shape="rect" coords="0,0,100,42" />
                  <area href="http://www.jboss.org/" alt="JBoss Homepage" title="JBoss" shape="rect" coords="100,0,200,42" />
               </map>
               <div>
                  <img src="/images/redhat-jboss-logo.gif" usemap="#redhat-jboss-logo-map" border="0" />
               </div>
            </td>
        </tr>
        <tr>
            <td align="right">&nbsp;</td>
        </tr>
    </table>

</div>

<div id="LoginSupport">

	<h1><fmt:message key="login.message"/></h1>
	
	<html:form action="/j_security_check">
	
	<p><fmt:message key="login.login"/></p>
	
	
	<div style="margin-bottom: 5px">
		<div style="width: 100px; float: left; text-align:right; margin-right: 10px;"><fmt:message key="login.username"/></div>
		<div><html:text property="j_username" size="25" tabindex="1" /><html:submit value="Login" styleClass="button_submit" tabindex="3"/></div>
	</div>
	
	
	<div style="margin-bottom: 5px">
		<div style="width: 100px; float: left; text-align:right; margin-right: 10px;"><fmt:message key="common.label.Password"/></div>
		<div><input type="Password" name="j_password" size="25" value="" tabindex="2"></div>
	</div>
		
	<c:if test='${loginStatus ne null}'>
	<div style="margin-top: 25px; width: 300px; color: maroon" class="box">
		<div class="smalltext" style="line-height: 1.5">
			<html:img page="/images/important.gif" align="middle" border="0" hspace="10"/>
			<fmt:message key="${loginStatus}"/>
		</div>
	</div>
	</c:if>

	<logic:messagesPresent>
	<div style="margin-top: 25px; width: 300px; color: maroon" class="box">
		<div class="smalltext" style="line-height: 1.5">
			<html:img page="/images/important.gif" align="middle" border="0" hspace="10"/>
			<html:errors/>
		</div>
	</div>
	</logic:messagesPresent>
	
	</html:form>

</div>

<script language="JavaScript" type="text/javascript">
  <!--
    document.forms["LoginForm"].elements["j_username"].focus();
  // -->
</script>
</body>
</html>
