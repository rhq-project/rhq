<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<%-- end vit: delete this block --%>

<!-- Content Block Title: Properties -->

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.NotifyOR.Label.Email"/></td>
    <logic:messagesPresent property="emailAddresses">
    <td width="80%" class="ErrorField">
      <html:textarea cols="80" rows="3" property="emailAddresses"/><br>
      <span class="ErrorFieldContent"><html:errors property="emailAddresses"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="emailAddresses">
    <td width="80%" class="BlockContent">
      <html:textarea cols="80" rows="3" property="emailAddresses"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="80%" class="BlockContentSmallText"><fmt:message key="alert.config.NotifyOR.TinyText"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<tiles:insert definition=".form.buttons"/>
<html:hidden property="ad"/>
<html:hidden property="id"/>
<html:hidden property="type"/>
