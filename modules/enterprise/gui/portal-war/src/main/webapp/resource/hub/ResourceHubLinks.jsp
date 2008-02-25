<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!--  don't show new group icon if privs are lacking -->
<c:if test="${useroperations['MANAGE_INVENTORY']}">
   <table border="0">
      <tr>
         <td class="LinkBox">

            <html:link page="/resource/group/Inventory.do?mode=new">
               <fmt:message key="resource.hub.NewGroupLink"/>
               <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
            </html:link>
            <br>
         </td>
      </tr>
   </table>
</c:if>