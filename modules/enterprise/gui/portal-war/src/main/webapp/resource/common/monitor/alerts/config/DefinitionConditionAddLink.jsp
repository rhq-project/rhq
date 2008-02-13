<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="formName"/>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">
    <html:hidden property="addingCondition" value="false"/>
    <html:link href="javascript:document.${formName}.submit()"
               onclick="clickAdd('${formName}');"
               titleKey="alert.config.props.CB.Another">
      <fmt:message key="alert.config.props.CB.Another"/>
    </html:link>
  </td>
</tr>
