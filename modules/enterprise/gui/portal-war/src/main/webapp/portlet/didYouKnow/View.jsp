 <%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.DidYouKnow"/>
  <tiles:put name="portletName" beanName="portletName" />
</tiles:insert>

<tiles:importAttribute name="tip"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr class="ListRow">
    <!-- Would be nice if this didn't escape characters -->
    <td class="ListCell"><c:out value="${tip}"/></td>
  </tr>
</table>

