<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="form" ignore="true"/>

<c:if test="${empty form}">
   <c:set var="form" value="${MetricDisplayRangeForm}"/>
</c:if>

<script type="text/javascript">
   var pageData = new Array();
</script>

<html:form action="/resource/common/monitor/visibility/MetricDisplayRange">

   <tiles:insert definition=".page.title.resource.generic">
      <tiles:put name="titleKey" value="resource.common.monitor.visibility.MetricDisplayRangePageTitle"/>
   </tiles:insert>

   <tiles:insert definition=".resource.common.monitor.visibility.metricDisplayRangeForm"/>

   <tiles:insert definition=".form.buttons"/>
   <tiles:insert definition=".page.footer"/>

   <c:if test="${not empty form.id}">
      <html:hidden property="id"/>
   </c:if>
   <c:if test="${not empty form.ctype}">
      <html:hidden property="ctype"/>
   </c:if>
      <c:if test="${not empty form.parent}">
      <html:hidden property="parent"/>
   </c:if>
   <c:if test="${not empty form.groupId}">
      <html:hidden property="groupId"/>
   </c:if>
</html:form>
