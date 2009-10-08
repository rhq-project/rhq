<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.measurement.MeasurementDataTrait" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageOrdering" %>
<%@ page import="org.rhq.core.domain.util.OrderingField" %>
<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>


<c:choose>
   <c:when test="${not empty traits}">

<%
    List s = (List)pageContext.findAttribute("traits");
    PageList l = new PageList(s, new PageControl(0,-1,new OrderingField("timestamp", PageOrdering.ASC)));
    pageContext.setAttribute("TraitList",l);
%>
    

	<c:out value="Historic data for trait:"/>
	<b>
	<c:out value="${traitName}"/>
	</b>
	<table width="95%">
	  <tr>
	    <td width="80%">&nbsp;</td>
	    <td class="LinkBox" width="20%">
	       <a href="/resource/common/monitor/Visibility.do?mode=resourceMetrics&id=${rid}">Back to Resource
	          <img src="/images/title_arrow.gif" height="9" width="11" border="0" alt="">
	       </a>
	    </td> 
	  </tr>
	</table>
   <display:table width="95%" items="${TraitList}" var="trait">
        <display:column title="Value"  isLocalizedTitle="false" width="70%"
                property="value"/>
        <display:column title="Change date" isLocalizedTitle="false" width="30%"
                property="timestamp">
                <display:datedecorator format="dd MMM yy, HH:mm:ss Z"/>
        </display:column>
   </display:table>
   </c:when>
   <c:otherwise>
     <tiles:insert definition=".resource.common.monitor.visibility.noMetrics">
       <c:if test="${not empty favorites}">
         <tiles:put name="favorites" beanName="favorites"/>
       </c:if>
     </tiles:insert>
   </c:otherwise>
</c:choose>
      