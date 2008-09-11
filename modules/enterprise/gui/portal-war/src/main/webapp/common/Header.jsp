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

    <table width="100%">
        <tr valign="bottom">
            <td align="left">
                <fmt:message var="urlDomain" key="product.url.domain"/>
                <fmt:message var="productName" key="product.fullName"/>
                <html:link page="http://${urlDomain}">
                    <html:img page="/images/logo_header.gif" title="${productName}"/>
                </html:link>
            </td>
            <td align="right">
               <div id="AppMenu">
                    <html:link page="/Start.do"><fmt:message key="header.start.link"/></html:link>
                    |
                    <html:link page="/Dashboard.do"><fmt:message key="dash.home.PageTitle"/></html:link>
                    |
                    <html:link page="/ResourceHub.do"><fmt:message key="resource.hub.ResourceHubPageTitle"/></html:link>
                    |
                    <html:link page="/Admin.do"><fmt:message key="admin.admin.AdministrationTitle"/></html:link>
                    |
                    <html:link href="" onclick="window.open('${helpBaseURL}','help','width=940,height=730,scrollbars=yes,toolbar=yes,left=40,top=40,resizable=yes'); return false;"><fmt:message key="header.help.link"/></html:link>
                    |
                    <html:link page="/Logout.do"><fmt:message key="header.logout.link"/></html:link>
               </div>
            </td>
        </tr>
    </table>

    <hr style="margin-top: 6px; background-color: #090909; color: #090909" size="1" noshade="noshade"/>

        <%--
       <c:if test="${not empty pageHelpURL}">
          <a href=""
             onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><img
                src="/images/title_pagehelp.gif" alt="Page Help" align="right"/></a>
       </c:if>
        --%>

    <div id="Breadcrumb">
        <tiles:insert attribute="breadcrumb">
          <tiles:put name="location" beanName="location"/>
        </tiles:insert>
    </div>

</div>

