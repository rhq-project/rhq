<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:insert definition=".form.buttons"/>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="21%">&nbsp;</td>
    <td width="79%" class="ButtonCaptionText">
      <i><fmt:message key="alert.config.props.CB.Content.TinyType"/></i>
    </td>
  </tr>
</table>
