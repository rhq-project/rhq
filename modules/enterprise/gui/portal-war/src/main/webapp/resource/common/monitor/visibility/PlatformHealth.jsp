<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<hq:constant symbol="ERR_PLATFORM_HEALTH_ATTR" var="HostErr" />
<c:set var="tabKey" value="resource.server.monitor.visibility.HostPlatformTab"/>
<c:set var="hostResourcesHealthKey" value="resource.common.monitor.visibility.PlatformTH"/>

<c:choose>
<c:when test="${not empty requestScope[HostErr]}">
  <c:set var="errKey" value="${requestScope[HostErr]}" />
  <tiles:insert definition=".resource.common.monitor.visibility.hostResourcesCurrentHealth">
    <tiles:put name="mode" beanName="mode"/>
    <tiles:put name="errKey" beanName="errKey"/>
    <tiles:put name="tabKey" beanName="tabKey" />
    <tiles:put name="hostResourcesHealthKey" beanName="hostResourcesHealthKey" />
    <tiles:put name="checkboxes" beanName="checkboxes" />
  </tiles:insert>
</c:when>
<c:otherwise>
  <tiles:insert definition=".resource.common.monitor.visibility.hostResourcesCurrentHealth">
    <tiles:put name="summaries" beanName="summaries"/>
    <tiles:put name="mode" beanName="mode"/>
    <tiles:put name="tabKey" beanName="tabKey" />
    <tiles:put name="hostResourcesHealthKey" beanName="hostResourcesHealthKey" />
    <tiles:put name="checkboxes" beanName="checkboxes" />
  </tiles:insert>
</c:otherwise>
</c:choose>
