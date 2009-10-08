<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<c:set var="subTabUrl" value="/resource/common/monitor/Visibility.do?mode=performance"/>
<c:set var="selfAction" value="${subTabUrl}&id=${Resource.id}"/>

<html:form action="/resource/common/monitor/visibility/Performance">

   <table width="300" cellpadding="0" cellspacing="0" border="0">
      <tr>
         <td colspan="3">
            <tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>
         </td>
      </tr>

      <tr>
         <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
         <td>
            <%-- left-hand panel --%>
            <tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.byUrl">
               <tiles:put name="summaries" beanName="PerfSummaries"/>
               <tiles:put name="resource" beanName="Resource"/>
               <tiles:put name="selfAction" beanName="selfAction"/>
            </tiles:insert>
         </td>
         <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
      </tr>

   </table>
   </td>

   <td valign="top">
   <table width="100%" cellpadding="0" cellspacing="0" border="0">

      <tr>
         <td colspan="3">
            <tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
               <tiles:put name="selectedIndex" value="2"/>
               <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
               <tiles:put name="tabListName" value="perf"/>
            </tiles:insert>
         </td>
      </tr>

      <tr>
         <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
         <td>
            <%-- right-hand panel --%>
            <tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.table">
               <tiles:put name="perfSummaries" beanName="PerfSummaries"/>
               <tiles:put name="resource" beanName="Resource"/>
               <tiles:put name="selfAction" beanName="selfAction"/>
            </tiles:insert>
         </td>
         <td><html:img page="/images/spacer.gif" width="2" height="1" alt="" border="0"/></td>
      </tr>
   </table>
   <html:hidden property="id"/>   
   <html:hidden property="ctype"/>
   <html:hidden property="rb"/>
   <html:hidden property="re"/>
</html:form>
