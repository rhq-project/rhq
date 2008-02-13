<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="message"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="ControlBlockContainer">
 <tr>
  <td><c:out value="${message}" escapeXml="false" /></td>
 </tr>
</table>