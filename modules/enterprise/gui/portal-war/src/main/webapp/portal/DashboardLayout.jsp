<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<td width="100%">
<script src="<html:rewrite page="/js/prototype.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>
<!-- NOTE: rico.js is used for the AJAX stuff below (i.e. the ajaxEngine object). -->
<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>

<script language="JavaScript" type="text/javascript">
  if (top != self)
    top.location.href = self.document.location;

  // Register the removePortlet method
  ajaxEngine.registerRequest( 'removePortlet',
                              '<html:rewrite page="/dashboard/RemovePortlet.do"/>' );

  ajaxEngine.registerRequest( 'movePortletUp',
                              '<html:rewrite page="/dashboard/MovePortletUp.do"/>' );

  ajaxEngine.registerRequest( 'movePortletDown',
                              '<html:rewrite page="/dashboard/MovePortletDown.do"/>' );

  function removePortlet(name, label) {
    ajaxEngine.sendRequest( 'removePortlet', 'portletName=' + name );
    new Effect.BlindUp($(name));

    var wide = isWide(name);
    var portletOptions;
    for (i = 0; i < document.forms.length; i++) {
      if (document.forms[i].wide) {
        if (wide == (document.forms[i].wide.value == 'true')) {
            portletOptions = document.forms[i].portlet.options;
            break;
        }
      }
    }

    if (portletOptions) {
        portletOptions[portletOptions.length] = new Option(label, name);

        // Make sure div is visible
        $('addContentsPortlet' + wide).style.visibility='visible';
    }
  }

  function movePortletUp(id) {
    ajaxEngine.sendRequest( 'movePortletUp', 'portletName=' + id );

    var root;
    if (isWide(id)) {
      root = $('narrowList_false'); 
    }
    else {
      root = $('narrowList_true'); 
    }
    var elem = $(id);
    moveElementUp(elem, root);
  }

  function movePortletDown(id) {
    ajaxEngine.sendRequest( 'movePortletDown', 'portletName=' + id );

    var root;
    if (isWide(id)) {
      root = $('narrowList_false'); 
    }
    else {
      root = $('narrowList_true'); 
    }
    var elem = $(id);
    moveElementDown(elem, root);
  }

    function refreshPortlet(id) {
      var url = '<c:url value="/portal/SingleTile.jsp?portlet="/>';    //.dashContent.softwareSummary
      var div = document.getElementById(id);
      var rs = document.getElementById(id + "RefreshIcon");
      if (rs == null || div == null) {
          return; // This is expected for portlets without tab bar (the add drop-downs)
      }
      rs.innerHTML = "<img src='<c:url value="/images/status_bar.gif"/>'/>";
      var xmlhttp = getXMLHttpRequest();
      url += id;
      xmlhttp.open('GET',url,true);
      xmlhttp.onreadystatechange=function()
          {
             if (xmlhttp.readyState==4 && xmlhttp.status==200) {
                  div.innerHTML = xmlhttp.responseText;
             } else if (xmlhttp.readyState==4) {
                 window.location.reload(true); // refresh the page for re-login
             }
          }
      xmlhttp.send(null);
    }
    var registrationDelay = 0;
    var refreshPeriod = '<c:out value="${refreshPeriod}" />';
    function registerPortletRefresh(id) {
       // This makes it so that all the portlets aren't refreshing simultaneously
       // We time out the interval registration by increasing 2 seconds per registration
       // Such that they all have an interval of the refresh period, but are offset by 2 seconds
       setTimeout('setInterval(\'refreshPortlet("' + id + '")\',1000 * refreshPeriod)', 1000 * (registrationDelay+=2));
    }
</script>

<c:set var="headerColspan" value="${portal.columns + 3}"/>

<div class="effectsContainer ">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
      <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
      <td width="25%"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
      <td width="75%"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
      <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
  </tr>

  <tr>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
<%-- Multi-columns Layout
  This layout render lists of tiles in multi-columns. Each column renders its tiles
  vertically stacked.  
--%>
<c:set var="narrow" value="true" scope="page" />
<c:set var="width"  value=""     scope="page" />
<c:set var="hr"     value=""     scope="page" />
<c:set var="showUpAndDown" value="true" scope="request"/>

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >  
  
  <c:choose>    
    <c:when test="${portal.columns eq 1}">    
      <c:set var="narrow" value="false" />
      <c:set var="hr" value="95%" />
      <c:set var="width" value="width='100%'" />
    </c:when>
  
    <c:when test="${narrow eq 'true'}">      
      <c:set var="hr" value="180" />
      <c:set var="width" value="width='25%'"/>
    </c:when>
    
    <c:otherwise>
      <c:set var="narrow" value="false" />
      <c:set var="hr" value="75%" />
      <c:set var="width" value="width='75%'" />
    </c:otherwise>
  </c:choose>

  <td valign="top" name="specialTd" <c:out value="${width}" escapeXml="false"/>>

<ul id="<c:out value="narrowList_${narrow}"/>" class="boxy">
  <c:forEach var="portlet" items="${columnsList}">
  <c:set var="isFirstPortlet" value="${portlet.isFirst}" scope="request"/>
  <c:set var="isLastPortlet"  value="${portlet.isLast}"  scope="request"/>

      <c:if test="${not empty refreshPeriod}">
          <script type="text/javascript">
              registerPortletRefresh("<c:out value="${portlet.url}"/>");
          </script>
      </c:if>
  <li id="<c:out value="${portlet.url}"/>"><table width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr><td valign="top" class="DashboardPadding">
        <tiles:insert beanProperty="url" beanName="portlet" flush="true"/>
    </td></tr>
  </table></li>
  </c:forEach>
</ul>
      <c:choose >
        <c:when test="${narrow eq 'true'}">              
          <c:set var="narrow" value="false" />
        </c:when>
        <c:otherwise>              
          <c:set var="narrow" value="true" />
        </c:otherwise>
      </c:choose>

  </td>
  
</c:forEach>

  <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
</tr>
</table> 
</div>
<!-- /Content Block --> 
</td>
