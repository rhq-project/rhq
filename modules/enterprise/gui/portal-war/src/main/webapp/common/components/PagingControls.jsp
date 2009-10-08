<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.taglib.Pagination" %>
<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>


<tiles:importAttribute name="pageList"/>
<tiles:importAttribute name="action"/>
<tiles:importAttribute name="postfix" ignore="true"/>

<tiles:importAttribute name="pageSizeMenuDisabled" ignore="true"/>

<c:set var="pageNumber" value="${param[pageNumParam]}"/>

<%
   // GH: Deal with the fact that EL won't let me access properties on an object that implements List
   PageList pageList = (PageList) pageContext.getAttribute("pageList");
   pageContext.setAttribute("totalSize", pageList.getTotalSize());


%>

<td width="100%">
   <table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
      <tr>
         <td width="100%" align="right" nowrap="nowrap"><b>
            <fmt:message key="ListToolbar.Total"/>
            &nbsp;${totalSize}</b>
         </td>
         <td>
            <html:img page="/images/spacer.gif" width="10" height="1" border="0"/>
         </td>
         <td align="right" nowrap="nowrap">
            <b>
               <fmt:message key="ListToolbar.ItemsPerPageLabel"/>
            </b>
         </td>
         <td>
            <html:img page="/images/spacer.gif" width="10" height="1" border="0"/>
         </td>
         <td>
            <html:select property="ps${postfix}" size="1"
                         onchange="goToSelectLocationAndRemove(this, 'ps${postfix}',  '${action}', 'pn${postfix}');">
               <html:option value="15"/>
               <html:option value="30"/>
               <html:option value="45"/>
            </html:select>
         </td>
         <td>
            
               <hq:paginate action="${action}" pageList="${pageList}" postfix="${postfix}"/>
            
         </td>
      </tr>
   </table>
</td>

