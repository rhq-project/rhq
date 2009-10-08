<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="rangeNow"/>
<tiles:importAttribute name="begin"/>
<tiles:importAttribute name="end"/>
<tiles:importAttribute name="prevProperty" ignore="true"/>
<tiles:importAttribute name="nextProperty" ignore="true"/>

<c:if test="${empty prevProperty}">
<c:set var="prevProperty" value="prevRange"/>
</c:if>
<c:if test="${empty nextProperty}">
<c:set var="nextProperty" value="nextRange"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockBottomLine" colspan="3"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockContent" width="100%" align="right">
      <html:image property="${prevProperty}" page="/images/tbb_pageleft.gif" border="0"/>
    </td>
    <td class="BlockContent" nowrap>
      <hq:dateFormatter value="${begin}"/>
      &nbsp;<fmt:message key="resource.common.monitor.visibility.chart.to"/>&nbsp;
      <hq:dateFormatter value="${end}"/>
    </td>
    <td class="BlockContent">
      <c:choose>
      <c:when test="${rangeNow}">
      <html:img page="/images/tbb_pageright_gray.gif" border="0"/>
      </c:when>
      <c:otherwise>
      <html:image property="${nextProperty}" page="/images/tbb_pageright.gif" border="0"/>
      </c:otherwise>
      </c:choose>
    </td>
  </tr>
</table>
