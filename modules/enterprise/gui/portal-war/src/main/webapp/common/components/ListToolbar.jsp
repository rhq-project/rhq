<%@ page language="java" %>

<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="listNewUrl" ignore="true"/>
<tiles:importAttribute name="listNewParamName" ignore="true"/>
<tiles:importAttribute name="listNewParamValue" ignore="true"/>
<tiles:importAttribute name="widgetInstanceName" ignore="true"/>
<tiles:importAttribute name="noButtons" ignore="true"/>
<tiles:importAttribute name="newOnly" ignore="true"/>
<tiles:importAttribute name="deleteOnly" ignore="true"/>
<tiles:importAttribute name="pageList" ignore="true"/>
<tiles:importAttribute name="postfix" ignore="true"/>
<tiles:importAttribute name="pageAction" ignore="true"/>
<tiles:importAttribute name="goButtonLink" ignore="true"/>
<tiles:importAttribute name="useCssButtons" ignore="true"/>

<script type="text/javascript">
  var goButtonLink;
</script>

<c:choose>
  <c:when test="${not empty listNewParamName && not empty listNewParamValue}">
    <c:url var="listNewUrl" value="${listNewUrl}">
      <c:param name="${listNewParamName}" value="${listNewParamValue}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="listNewUrl" value="${listNewUrl}"/>
  </c:otherwise>
</c:choose>

<!-- LIST TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>  
  <c:if test="${empty noButtons}">
    <c:if test="${!deleteOnly}">
    <%-- this is for formatting nazis (you know who you are): there is a good reason for "bad" formatting of the next line --%>
    <%-- for example, it fixes https://intranet.covalent.net/bugz/show_bug.cgi?id=6780.  so, suffer in silence.  vitaliy.--%>
    <td width="40"><html:link href="${listNewUrl}"><html:img page="/images/tbb_new.gif" width="42" height="16" border="0"/></html:link></td>
    </c:if>
    <c:if test="${!newOnly}">
    <%-- this is for formatting nazis (you know who you are): there is a good reason for "bad" formatting of the next line --%>
    <td width="40" align="left" id="<c:out value="${widgetInstanceName}"/>DeleteButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DeleteButtonDiv">
       <c:choose>
          <c:when test="${useCssButtons}">
             <html:img page="/images/tbb_uninventory_gray.gif" border="0" />
          </c:when>
          <c:otherwise>
             <html:img page="/images/tbb_delete_gray.gif" border="0" />
          </c:otherwise>
       </c:choose>
    </div></td>
    <%----%>
    </c:if>
  </c:if>
<c:if test="${not empty goButtonLink}">
<td width="50%">
  <div id="goButtonDiv">
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td class="BoldText" nowrap><fmt:message key="alert.config.list.SetActiveLabel"/></td>
        <td><html:img page="/images/spacer.gif" width="10" height="1" alt="" border="0"/></td>
        <td>
          <select name="active" id="active" size="1">
          	<option></option>
              <option value="1"><fmt:message key="alert.config.props.PB.ActiveYes"/></option>
              <option value="2"><fmt:message key="alert.config.props.PB.ActiveNo"/></option>
          </select>
        </td>
        <td><html:img page="/images/spacer.gif" width="10" height="1" alt="" border="0"/></td>
        <td width="100%"><html:link href="#" styleId="goButtonLink"><html:img page="/images/dash-button_go-arrow_gray.gif" width="23" height="17" alt="" border="0" styleId="goButtonImg"/></html:link></td>
        <script type="text/javascript">
          goButtonLink = "<c:out value="${goButtonLink}"/>";
          
          hideDiv("goButtonDiv");
  
          var checkboxesArr = document.getElementsByName("definitions");
          var numCheckboxes = checkboxesArr.length;
          
          if (numCheckboxes > 0) {
            showDiv("goButtonDiv");
          }
        </script>
      </tr>
    </table>
  </div>
</td>
</c:if>
<c:choose>
  <c:when test="${not empty pageAction}">
    <tiles:insert definition=".controls.paging" ignore="false">
       <tiles:put name="pageList" beanName="pageList"/>
       <tiles:put name="postfix" value="${postfix}" />
       <tiles:put name="action" beanName="pageAction"/>
    </tiles:insert>
  </c:when>
  <c:otherwise>
    <td>&nbsp;</td>
  </c:otherwise>
</c:choose>
</tr>
</table>
