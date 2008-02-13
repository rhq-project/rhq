<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="first" value="true" />

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >  
  
  <td valign="top" width='100%' >      
    <c:forEach var="portlet" items="${columnsList}" >        
    <table width="100%" border="0" cellspacing="0" cellpadding="0">          
      <c:choose>
        <c:when test="${first eq true}">
        <tr> 
          <td colspan="4">
            <tiles:insert  beanProperty="url" beanName="portlet" flush="true"/>
          </td>
          <c:set var="first" value="false" />
        </tr>
        </c:when >            
        <c:otherwise>
        <tr>  
          <td><html:img page="/images/spacer.gif" width="75" height="1" alt="" border="0"/></td>
          <td valign="top" width="100%">           
            <tiles:insert  beanProperty="url" beanName="portlet" flush="true"/>
            <c:if test="${not portal.dialog}">
            &nbsp;<br>
            </c:if>
          </td>      
        </tr>  
        </c:otherwise>
        
      </c:choose>
      
    </table>             
    </c:forEach>    
    <small><br></small><html:img page="/images/spacer.gif" width="95%" height="1" border="0"/>
  </td> 

</c:forEach>
<!-- /Content Block -->
