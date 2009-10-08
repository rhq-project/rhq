<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<table width="100%" cellpadding="3" cellspacing="0" border="0">
   <tr>
      <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
   </tr>
   <tr>
      <td width="20%">&nbsp;</td>
      <td width="80%">
         <table width="100%" cellpadding="3" cellspacing="0" border="0">
            <tr>
               <td width="18%"><html:checkbox property="low" value="true"/> <fmt:message
                     key="resource.common.monitor.visibility.performance.Low"/></td>
               <td width="18%"><html:checkbox property="avg" value="true"/> <fmt:message
                     key="resource.common.monitor.visibility.performance.Average"/></td>
               <td width="18%"><html:checkbox property="peak" value="true"/> <fmt:message
                     key="resource.common.monitor.visibility.performance.Peak"/></td>
               <td colspan="5"><html:image page="/images/fb_redraw.gif" property="redraw" border="0"
                                           onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');"
                                           onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');"
                                           onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')"/></td>
               <td width="41%">&nbsp;</td>
               <html:hidden property="pn"/>
               <html:hidden property="ps"/>
               <c:if test="${not empty param.sc}">
                  <input type="hidden" name="sc" value="${param.sc}"/>
               </c:if>
               <c:if test="${not empty param.so}">
                  <input type="hidden" name="so" value="${param.so}"/>
               </c:if>
            </tr>
            <tr>
               <td colspan="5">
                  <i><fmt:message key="resource.common.monitor.visibility.performance.ControlInstructions"/></i>
               </td>
            </tr>
         </table>
      </td>
   </tr>
   <tr>
      <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
   </tr>
</table>
