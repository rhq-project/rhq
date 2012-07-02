<#ftl >
<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
-->
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.OperationHistoryRest" -->
<html>

<table>
    <tr>
        <td>Operation Name</td>
        <td>${var.operationName}</td>
    </tr>
    <tr>
        <td>Resource Name</td>
        <td>${var.resourceName}</td>
    </tr>
    <tr>
        <td>Outcome</td>
        <td>${var.status}</td>
    </tr>
    <#if var.errorMessage?has_content>
        <tr>
            <td>Error</td>
            <td>${var.errorMessage}</td>
        </tr>
    </#if>
    <#if var.result?has_content>
    <tr>
        <td>Result Parameters</td>
        <td>
            <#assign result=var.result/>
            <#list result?keys as p>
                ${p} : ${result[p]}<br/>
            </#list>
        </td>
    </tr>
    </#if>
</table>
</html>