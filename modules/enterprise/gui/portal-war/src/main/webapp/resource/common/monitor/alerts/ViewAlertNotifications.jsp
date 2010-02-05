<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<!-- Content Block Title: Notification -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.current.detail.notify.Tab"/>
</tiles:insert>

<!-- Notification Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
    Details of fired alerts go here
</table>
