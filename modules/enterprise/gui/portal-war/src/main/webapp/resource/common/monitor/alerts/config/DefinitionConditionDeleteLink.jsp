<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute name="formName" ignore="true"/>
<tiles:importAttribute name="i" ignore="true"/>
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">
  
    <html:link href="javascript:document.${formName}.submit()"
               onclick="clickRemove('${formName}', '${i}');"
               titleKey="alert.config.props.CB.Delete">
      <fmt:message key="alert.config.props.CB.Delete"/>
    </html:link>
  </td>
</tr>
