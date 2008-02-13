<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<html>
<head>
<title><fmt:message key="securityAlert.SecurityAlert.Title"/></title>
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<script language="JavaScript" src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
</head>

<body>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<div align="center">
<table width="400" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockTitle" width="100%"><fmt:message key="securityAlert.SecurityAlert.Tab"/></td>
          <td class="BlockTitle" align="right"><html:link href="" onclick="window.open('${helpBaseURL}Users+Guide','help','width=940,height=730,scrollbars=yes,toolbar=yes,left=40,top=40,resizable=yes'); return false;"><html:img page="/images/tt_help.gif" width="16" height="16" border="0"/></html:link></td>
        </tr>
      </table>	
    </td>
  </tr>
    <tr>
      <td class="BlockContent" colspan="2">
        <p>
        <fmt:message key="securityAlert.CannotBeDisplayed"/> 
        <fmt:message key="securityAlert.PleaseContact"/> 
        <fmt:message key="securityAlert.RHQAdministrator"/>
        <fmt:message key="securityAlert.toAdd"/>
        </p>
        
        <p>
        <fmt:message key="securityAlert.ReturnTo"/>
        <html:link href="javascript:history.back(1)"><fmt:message key="securityAlert.previousPage"/></html:link>
        <html:link page="/ResourceHub.do"><fmt:message key="error.Error.ResourceHubLink"/></html:link>
        </p>
      </td>
    </tr>
      <tr>
        <td class="BlockContent" colspan="2"><html:img page="/images/spacer.gif" width="1" height="5" alt="" border="0"/></td>
      </tr>
      <tr>
        <td class="BlockBottomLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
      </tr>
    </table>
    </td>
   </tr>
  </table>
</div>
</body>
</html>
