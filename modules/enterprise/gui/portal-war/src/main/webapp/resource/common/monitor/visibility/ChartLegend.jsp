<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<table width="100%" cellpadding="3" cellspacing="0" border="0">
  <tr>
    <td width="100%">
      <b><fmt:message key="resource.common.monitor.visibility.chart.ResourceAndControlActionKeyLabel"/></b><br>

      <c:forEach var="resource" varStatus="resStatus" items="${checkedResources}">
      <c:url var="resourceUrl" value="/rhq/resource/monitor/graphs.xhtml">
         <c:param name="id" value="${resource.id}"/>
      </c:url>
      <c:url var="parentResourceUrl" value="/rhq/resource/monitor/graphs.xhtml">
         <c:param name="id" value="${resource.parentResource.id}"/>
      </c:url>
      <fmt:formatNumber var="imgidx" pattern="00" value="${resStatus.index + 1}"/>
      <p><b><fmt:message key="resource.common.monitor.visibility.chart.ResourceLabel"/></b>
      <html:img page="/images/icon_resource_${imgidx}.gif" width="11" height="11" border="0"/>
      <html:link href="${parentResourceUrl}">
         <c:out value="${resource.parentResource.name}"/>
      </html:link> 
      <br>
      &nbsp;&nbsp;&nbsp;&nbsp;
      &nbsp;&nbsp;&nbsp;&nbsp;
      &nbsp;&nbsp;&nbsp;&nbsp;
      &nbsp;&nbsp;&nbsp;&nbsp;
      <html:img page="/images/hierarchy.gif" width="16" height="16" alt="" border="0"/>
      <html:link href="${resourceUrl}">
         <c:out value="${resource.name}"/>
      </html:link> 
      <br>
      </c:forEach>
    </td>
  </tr>
</table>
