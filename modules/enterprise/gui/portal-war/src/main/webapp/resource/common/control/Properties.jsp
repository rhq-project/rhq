<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<script src="<html:rewrite page="/js/"/>control_ControlActionProperties.js" type="text/javascript"></script>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.Control.Properties.Tab"/>
</tiles:insert>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel">
   <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
   <fmt:message key="resource.group.Control.Properties.Label.ControlAction"/>
  </td>
  <td width="30%" class="BlockContent">
   <html:select name="ControlAction" property="controlAction" onchange="toggleConfigFileDiv();">
    <html:option value="" key="resource.props.SelectOption"/>
    <html:optionsCollection property="controlActions" />
   </html:select>
  </td>
  <td width="20%" class="BlockLabel">
   <fmt:message key="common.label.Description"/>
  </td>
  <logic:messagesPresent property="description">
   <td width="30%" class="ErrorField">
    <html:textarea cols="35" rows="3" property="description" />
    <span class="ErrorFieldContent">- <html:errors property="description"/></span>
   </td>
  </logic:messagesPresent>
  <logic:messagesNotPresent property="description">
   <td width="30%" class="BlockContent">
    <html:textarea cols="35" rows="3" property="description" /> 
   </td>
  </logic:messagesNotPresent>
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine">
   <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
  </td>
 </tr>
</table>
<!--  /  -->

<!--  CONFIGURATION FILE CONTENTS -->
<div id="configFile">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr valign="top">
		<td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.group.Control.Properties.Label.ConfigurationFile"/></td>
		<td width="80%" class="BlockContent"><fmt:message key="resource.group.Control.Properties.SourceFile"/></td>
	</tr>
  <tr valign="top">
		<td class="BlockLabel" rowspan="5">&nbsp;</td>
		<td class="BlockContent"><input type="text" size="60" name="sourceFile"></td>
	</tr>
  <tr valign="top">
		<td class="BlockContent"><fmt:message key="resource.group.Control.Properties.Destination"/></td>
	</tr>
  <tr valign="top">
		<td class="BlockContent"><input type="text" size="60" name="destinationFile"></td>
	</tr>
  <tr valign="top">
		<td class="BlockContent"><input type="checkbox"> <fmt:message key="resource.group.Control.Properties.PerformVariableSubstitution"/></td>
	</tr>
  <tr valign="top">
		<td class="BlockContent"><html:link href="#"><fmt:message key="resource.group.Control.Properties.AddAnotherConfigurationFile"/></html:link></td>
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</div>
<!--  /  -->
<script language="JavaScript" type="text/javascript">
  document.getElementById("configFile").style.display = "none";
</script>