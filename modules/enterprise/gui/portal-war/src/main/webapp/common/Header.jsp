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

<div id="PageHeader">

    <table width="100%" border="0" cellpadding="0" cellspacing="0" height="70px">
        <tr valign="bottom">
            <td align="left" rowspan="2">
                <fmt:message var="urlDomain" key="product.url.domain"/>
                <fmt:message var="productName" key="product.fullName"/>
                <html:link page="http://${urlDomain}">
                    <html:img page="/images/logo_header.gif" title="${productName}"/>
                </html:link>
            </td>
            <td valign="top" align="right">
               <map name="redhat-jboss-logo-map">
                  <area href="http://www.redhat.com/" alt="Red Hat Homepage" title="Red Hat" shape="rect" coords="0,0,100,42" />
                  <area href="http://www.jboss.org/" alt="JBoss Homepage" title="JBoss" shape="rect" coords="100,0,200,42" />
               </map>
               <div>
                  <img src="/images/redhat-jboss-logo.gif" usemap="#redhat-jboss-logo-map" />
               </div>
            </td>
        </tr>
        <tr>
            <td align="right">
               <script type="text/javascript" src="/js/popup.js"></script>
               <div id="resMenu" class="headerresourcemenu">
                  <html:link page="/ResourceHub.do?resourceCategory=PLATFORM"><fmt:message key="resource.hub.filter.platform"/></html:link><br/>
                  <html:link page="/ResourceHub.do?resourceCategory=SERVER"><fmt:message key="resource.hub.filter.server"/></html:link><br/>
                  <html:link page="/ResourceHub.do?resourceCategory=SERVICE"><fmt:message key="resource.hub.filter.service"/></html:link><br/>
                  <html:link page="/GroupHub.do?groupCategory=COMPATIBLE"><fmt:message key="resource.hub.filter.compatibleGroups"/></html:link><br/>
                  <html:link page="/GroupHub.do?groupCategory=MIXED"><fmt:message key="resource.hub.filter.mixedGroups"/></html:link><br/>
                  <html:link page="/rhq/definition/group/list.xhtml"><fmt:message key="resource.hub.filter.groupDefinitions"/></html:link><br/>
               </div>
               <div id="AppMenu">
                    <html:link page="/Start.do"><fmt:message key="header.start.link"/></html:link>
                    |
                    <html:link page="/Dashboard.do"><fmt:message key="dash.home.PageTitle"/></html:link>
                    |
                    <html:link page="/ResourceHub.do" onmouseover="menuLayers.show('resMenu',event)" onmouseout="menuLayers.hide()"><fmt:message key="resource.hub.ResourceHubPageTitle"/></html:link>
                    |
                    <html:link page="/Admin.do"><fmt:message key="admin.admin.AdministrationTitle"/></html:link>
                    |
                    <html:link href="" onclick="window.open('${helpBaseURL}','help','width=940,height=730,scrollbars=yes,toolbar=yes,left=40,top=40,resizable=yes'); return false;"><fmt:message key="header.help.link"/></html:link>
                    |
                    <script language="JavaScript" type="text/javascript">var aboutWindowTitle = '<fmt:message key="about.Title"/>';</script>
                    <a href="#" onclick="openAbout(aboutWindowTitle)"><fmt:message key="header.about.link"/></a>
                    |
                    <html:link page="/Logout.do"><fmt:message key="header.logout.link"/></html:link>
               </div>
            </td>
        </tr>
    </table>

    <hr style="margin-top: 6px; background-color: #090909; color: #090909" size="1" noshade="noshade"/>

</div>

<div id="about" class="dialog" style="display: none; position: absolute; top: 0; left: 0">
   <div class="DisplayContent" style="margin:0">
      <div style="margin:5px">
         <p class="compact">
            <span class="DisplaySubhead"><fmt:message key="footer.aboutLink"/></span><br/>
            <span class="DisplayLabel"><fmt:message key="footer.version"/>: <fmt:message key="product.version"/></span><br/>
            <span class="DisplayLabel"><fmt:message key="footer.buildNumber"/>: <fmt:message key="product.buildNumber"/></span>            
         </p>
         <p class="compact">
            <fmt:message key="footer.shortCopyright"/> <fmt:message key="about.Copyright.Content"/>
         </p>
         <p class="compact">
            <fmt:message key="about.MoreInfo.Label"/><br/>
            <a href="mailto:<fmt:message key="about.MoreInfo.LinkSales"/>"><fmt:message key="about.MoreInfo.LinkSales"/></a><br/>
            <a href="mailto:<fmt:message key="about.MoreInfo.LinkSupport"/>"><fmt:message key="about.MoreInfo.LinkSupport"/></a>
         </p>
      </div>
   </div>
</div>
