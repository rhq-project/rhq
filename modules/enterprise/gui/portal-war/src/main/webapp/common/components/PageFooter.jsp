<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="msgKey" ignore="true"/>

<c:if test="${not empty msgKey}">
      <table width="100%" cellpadding="0" cellspacing="7" border="0">
        <tr align="left" valign="bottom">
          <td width="20%">&nbsp;</td>
          <td width="80%"><i><fmt:message key="${msgKey}"/></i></td>
        </tr>
      </table>
</c:if>
  </tr>
</table>
