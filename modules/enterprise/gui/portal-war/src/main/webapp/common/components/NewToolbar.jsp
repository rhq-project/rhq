<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%-- removed for later consideration 
<tiles:importAttribute name="newButtonKey" ignore="true"/>
--%>
<tiles:importAttribute name="useFromSideBar" ignore="true"/>
<tiles:importAttribute name="useToSideBar" ignore="true"/>
<tiles:importAttribute name="postfix" ignore="true"/>
<tiles:importAttribute name="pageList"/>
<tiles:importAttribute name="pageAction"/>

      <!--  NEW TOOLBAR -->
      <table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
        <tr>

<c:if test="${not empty useToSideBar}">
          <td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
</c:if>

<c:choose>
  <c:when test="${not empty newButtonKey}">
          <td><html:image page="/images/tbb_new.gif" border="0" property="create" titleKey="${newButtonKey}"/></td>
  </c:when>
  <c:otherwise>
          <td>&nbsp;</td>
  </c:otherwise>
</c:choose>

<tiles:insert definition=".controls.paging">
   <tiles:put name="pageList" beanName="pageList"/>
   <tiles:put name="postfix" value="${postfix}" />
   <tiles:put name="action" beanName="pageAction"/>
</tiles:insert>

<c:if test="${not empty useFromSideBar}">
          <td class="ListCellLineEmpty"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
</c:if>
        </tr>
      </table>
      <!--  /  -->
