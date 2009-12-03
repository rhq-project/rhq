<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:insert definition=".header.tab">
    <tiles:put name="tabKey" value="admin.settings.ServerPluginsConfigPropTab"/> 
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockCheckboxLabel" align="left" colspan="4">
        <a href="/rhq/admin/plugin/plugin-config.xhtml">Configure Server Plugins</a>
    </td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>