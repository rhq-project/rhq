
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
                <tiles:put name="tabKey" value="admin.license.UpdateLicenseTab"/>
            </tiles:insert>
        </td>
    </tr>


    <html:form action="/admin/license/UpdateLicense" enctype="multipart/form-data" method="POST">
        <tr>
            <td colspan="4">An up to date license file can be downloaded from the
                <a href="https://network.jboss.com/jbossnetwork/restricted/listSoftware.html">
                    JBoss Network Customer Service Portal</a>.</td>
        </tr>
        <tr>
            <td class="BlockLabel"><fmt:message key="admin.license.LicenseFile"/></td>
            <td><html:file property="licenseFile"/></td>
            <td class="BlockContent" colspan="2"></td>
        </tr>


    </table>

        <tiles:insert definition=".form.buttons"/>

    </html:form>

