<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<script type="text/javascript">
	var noDelete = true;
</script>
<script src="<html:rewrite page="/js/"/>pageLayout.js" type="text/javascript"></script>
<script type="text/javascript">
	var imagePath = "<html:rewrite page="/images/"/>";
</script>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.Control.Behavior.Tab"/>
</tiles:insert>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr valign="top">
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.Control.Behavior.Label.Occur"/></td>
  <td width="80%" class="BlockContent"><html:radio property="inParallel" value="true"/><fmt:message key="resource.group.Control.Behavior.Content.Parallel"/></td>
 </tr>
 <tr valign="top">
  <td width="20%" class="BlockLabel">&nbsp;</td>
  <td width="80%" class="BlockContent"><html:radio property="inParallel" value="false"/><fmt:message key="resource.group.Control.Behavior.Content.Order"/></td>
 </tr>
 <tr valign="top">
  <td width="20%" class="BlockLabel">&nbsp;</td>
  <td width="80%" class="BlockContent">
   <table width="100%" border="0" cellspacing="0" cellpadding="2">
    <tr valign="top"> 
     <td rowspan="2"><html:img page="/images/spacer.gif" width="20" height="20" border="0"/></td>
     <td><html:img page="/images/schedule_return.gif" width="17" height="21" border="0"/></td>
     <td>
      <html:select property="resourceOrdering" styleId="leftSel" multiple="true" style="WIDTH: 200px;" size="10" onchange="replaceButtons(this, 'left')" onclick="replaceButtons(this, 'left')">
       <html:optionsCollection property="resourceOrderingOptions"/>
      </html:select>
     </td>
     <td>&nbsp;</td>
     <td width="100%" id="leftNav">
      <div id="leftUp"><html:img page="/images/dash_movecontent_up-off.gif" width="20" height="20" alt="Click to Save Changes" border="0"/></div>
			<html:img page="/images/spacer.gif" width="1" height="10" border="0"/>
			<div id="leftDown"><html:img page="/images/dash_movecontent_dn-off.gif" width="20" height="20" alt="Click to Save Changes" border="0"/></div>
     </td>
    </tr>
   </table>
  </td>
 </tr>
 <tr>
  <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
 </tr>
</table>
<!--  /  -->
