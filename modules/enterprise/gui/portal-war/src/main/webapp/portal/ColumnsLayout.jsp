<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >
  
  <td valign="top" width='100%' >      
    <c:forEach var="portlet" items="${columnsList}" >        
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr>      
        <td valign="top">           
          <tiles:insert  beanProperty="url" beanName="portlet" flush="true"/>
        </td>        
      </tr>
    </table>             
    </c:forEach>    
  </td> 

</c:forEach>
<!-- /Content Block -->
