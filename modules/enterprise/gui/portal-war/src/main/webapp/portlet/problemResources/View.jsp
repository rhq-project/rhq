 <%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="problemResources"/>
<tiles:importAttribute name="adminUrl" ignore="true"/>

<c:url var="rssUrl" value="/rss/ViewProblemResources.rss">
  <c:param name="user" value="${webUser.username}"/>
</c:url>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.ProblemResources"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="rssUrl" beanName="rssUrl" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>

<c:choose>
  <c:when test="${not monitorEnabled}">   
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="common.marketing.FeatureDisabled"/></td>
      </tr>
    </table>
  </c:when>
  <c:when test="${not empty problemResources}">   
    <display:table cellspacing="0" cellpadding="0" width="100%" action="/Dashboard.do"
                   var="probres" items="${problemResources}" >
      <display:column width="60%" href="/rhq/resource/monitor/graphs.xhtml?id=${probres.resourceId}" property="resourceName" title="dash.home.TableHeader.ResourceName"/>
      <display:column width="10%" property="numAlerts" title="dash.home.TableHeader.Alerts" align="center"/>
      <display:column width="10%" property="availabilityType" title="resource.common.monitor.visibility.CAvailabilityTH" align="center" >
        <display:availabilitydecorator/> 
      </display:column>        
	   <display:footer>
	     <tr class="ListRow">
	       <td class="ListCell" colspan="4">
	         <i><c:out value="${timeRange}"/></i>
	       </td>	
	     </tr>
	   </display:footer>
    </display:table>
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
