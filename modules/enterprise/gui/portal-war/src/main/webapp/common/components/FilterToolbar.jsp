<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<tiles:importAttribute name="defaultKey"/>
<tiles:importAttribute name="optionsProperty"/>
<tiles:importAttribute name="labelProperty" ignore="true"/>
<tiles:importAttribute name="valueProperty" ignore="true"/>
<tiles:importAttribute name="filterParam" ignore="true"/>
<tiles:importAttribute name="filterAction"/>

<c:if test="${empty labelProperty}">
  <c:set var="labelProperty" value="label"/>
</c:if>
<c:if test="${empty valueProperty}">
  <c:set var="valueProperty" value="value"/>
</c:if>
<c:if test="${empty filterParam}">
  <c:set var="filterParam" value="f"/>
</c:if>

<!--  FILTER TOOLBAR  -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
  	<td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>		
    <td class="FilterLabelText" nowrap align="right"><fmt:message key="Filter.ViewLabel"/></td>
    <td class="FilterLabelText" width="100%">
      <html:select property="f" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, '${filterParam}',  '${filterAction}');">
        <html:option value="-1" key="${defaultKey}"/>
        <html:optionsCollection property="${optionsProperty}" value="${valueProperty}" label="${labelProperty}"/>
      </html:select>
    </td>
  </tr>
</table>
<!--  /  -->
