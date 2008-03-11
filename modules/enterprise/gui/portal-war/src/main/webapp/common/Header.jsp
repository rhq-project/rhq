<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<script language="JavaScript" type="text/javascript">
  var headerPath = "<html:rewrite page="/images/"/>";
  var help = "<hq:help/>";
</script>

<div style="position: absolute; top: 20px; left: 15px;"><html:link page="/Dashboard.do"><html:img page="/images/logo_header.gif" border="0"/></html:link></div>

<div id="TopMenu">
    <table cellpadding="0" cellspacing="0">

    <tr>
      <%--
        <td class="menu_JBnetwork"><a href="http://network.jboss.com"><fmt:message key="menu.top.a"/></a></td>
        <td class="menu_JBjems"><a href="http://www.jboss.com/"><fmt:message key="menu.top.b"/></a></td>
        <td class="menu_JBcom"><a href="http://www.jboss.org/"><fmt:message key="menu.top.c"/></a></td>
        <td class="menu_JBfed"><a href="http://www.redhat.com/"><fmt:message key="menu.top.d"/></a></td>
      --%>
        <td class="menu_JBfed"><a href="http://<fmt:message key="product.url.domain"/>"><fmt:message key="product.url.domain"/></a></td>
    </tr>
    </table>

</div>

<div id="PageHeader">

    <h1 class="appTitle"><fmt:message key="header.app.name"/></h1>
        <div id="AppMenu">
        <html:link page="/Start.do"><fmt:message key="header.start.link"/></html:link>
        |
        <html:link page="/Dashboard.do"><fmt:message key="dash.home.PageTitle"/></html:link>
        |
        <html:link page="/ResourceHub.do"><fmt:message key="resource.hub.ResourceHubPageTitle"/></html:link>
        |           
        <html:link page="/Admin.do"><fmt:message key="admin.admin.AdministrationTitle"/></html:link>
        |
        <html:link href="" onclick="window.open('${helpBaseURL}Users+Guide','help','width=940,height=730,scrollbars=yes,toolbar=yes,left=40,top=40,resizable=yes'); return false;"><fmt:message key="header.help.link"/></html:link>
        |
        <html:link page="/Logout.do"><fmt:message key="header.logout.link"/></html:link>
        </div>
        <hr style="margin-top: 10px;" color="#909090" size="1" noshade="noshade">
        <div id="Breadcrumb">
        <tiles:insert attribute="breadcrumb">
          <tiles:put name="location" beanName="location"/>
        </tiles:insert>
        </div>
    </div>
