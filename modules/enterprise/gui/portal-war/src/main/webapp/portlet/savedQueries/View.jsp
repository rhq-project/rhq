<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="effectsPortlet">
<tiles:importAttribute name="charts"/>
<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.SavedQueries"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>

<!-- Content Block Contents -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
    <c:choose>    
      <c:when test="${empty charts}">
        <tr class="ListRow">
          <td class="ListCell"><fmt:message key="dash.home.no.charts.to.display"/></td>
        </tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="chartTuple" items="${charts}">
          <tr class="ListRow">
            <td class="ListCell"><html:link page="${chartTuple.righty}"><c:out value="${chartTuple.lefty}"/></html:link></td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
</table>

