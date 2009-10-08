<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

    <tr>
      <td class="BlockLabel">
        <fmt:message key="alert.config.props.CB.Expression"/>
      </td>
      <td class="BlockContent">
        <fmt:message key="alert.config.props.CB.ExpressionDetails.1"/>
        <html:select property="conditionExpression">
          <html:optionsCollection property="conditionExpressionNames" label="key" value="value"/>
        </html:select>
        <fmt:message key="alert.config.props.CB.ExpressionDetails.2"/>
      </td>
    </tr>
    


