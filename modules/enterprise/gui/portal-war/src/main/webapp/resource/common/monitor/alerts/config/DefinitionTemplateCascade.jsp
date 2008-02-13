<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

  <c:if test="${not empty ResourceType}">
    <tr>
      <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>    
  
    <tr>
      <td class="BlockLabel">
        &nbsp;
      </td>
      <td class="BlockContent">
        <html:checkbox property="cascade" />
        <fmt:message key="alert.config.props.CB.Template.Cascade" />
      </td>
    </tr>
  </c:if>
