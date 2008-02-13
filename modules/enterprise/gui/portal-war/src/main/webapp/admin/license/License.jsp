<%@ page import="org.rhq.enterprise.server.license.License" %>
<%@ page import="java.util.Date" %>
</html>
<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<tiles:insert definition=".portlet.confirm" flush="true"/>
<tiles:insert definition=".portlet.error" flush="true"/>


<table width="100%" cellpadding="0" cellspacing="0" border="0">

    <tr>
        <td colspan="4" class="BlockHeader">
            <tiles:insert definition=".header.tab">
                <tiles:put name="tabKey" value="admin.license.LicenseInfoTab"/>
            </tiles:insert>
        </td>
    </tr>
    <!--  /  -->

       <c:choose>
              <c:when test="${license != null}">
            <!-- EMAIL CONFIG CONTENTS -->
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.LicenseeUser"/></td>
                <td class="BlockContent"><c:out value="${license.licenseeName}"/></td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.LicenseeEmail"/></td>
                <td class="BlockContent"><c:out value="${license.licenseeEmail}"/></td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.LicenseePhone"/></td>
                <td class="BlockContent"><c:out value="${license.licenseePhone}"/></td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.LicenseExpiration"/></td>
                <td class="BlockContent">
                    <c:choose>
                        <c:when test="${license.isPerpetualLicense}">Never</c:when>
                        <c:otherwise>
                            <% pageContext.setAttribute("expirationDate", new Date(((License)pageContext.findAttribute("license")).getLicenseExpiration()));%>
                            <fmt:formatDate value="${expirationDate}" dateStyle="full"/> 
                        </c:otherwise>
                    </c:choose>
                </td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.PlatformLimit"/></td>
                <td class="BlockContent">
                    <c:choose>
                        <c:when test="${license.platformsUnlimited}">unlimited</c:when>
                        <c:otherwise><c:out value="${license.platformLimit}"/></c:otherwise>
                    </c:choose>
                </td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
            <tr>
                <td class="BlockLabel"><fmt:message key="admin.license.MonitoringEnabled"/></td>
                <td class="BlockContent">
                    <c:choose>
                        <c:when test="${license.supportLevel == 3}">Enabled</c:when>
                        <c:otherwise>Disabled</c:otherwise>
                    </c:choose>
                </td>
                <td class="BlockContent" colspan="2"></td>
            </tr>
        </c:when>
        <c:otherwise>
            <tr>
                <td class="BlockContent" colspan="4" align="center">No license file loaded</td>
            </tr>
        </c:otherwise>
    </c:choose>

        <tr>
            <td class="BlockContent" colspan="4" align="center"><html:link action="/admin/license/LicenseAdmin.do?mode=edit">Update License</html:link>
            </td>
        </tr>

    </table>


