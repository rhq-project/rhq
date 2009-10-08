<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<!-- CONFIG TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.MiscConfigTab"/>  
</tiles:insert>
<!--  /  -->

<!-- CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockContent"><html:checkbox property="disableGuide" /></td>
    <td align="left" class="BlockCheckboxLabel"><fmt:message key="admin.settings.DisableGuide"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

