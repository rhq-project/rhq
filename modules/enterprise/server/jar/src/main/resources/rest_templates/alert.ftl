<#ftl >
<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.AlertRest" -->
<table>
    <tr>
        <th>Name</th>
        <th>Value</th>
    </tr>
    <tr>
        <td>Name</td>
        <td>${var.name}</td>
    </tr>
    <tr>
        <td>Id</td>
        <td>${var.id?c}</td>
    </tr>
    <tr>
        <td>Description</td>
        <td>${var.description}</td>
    </tr>
    <tr>
        <td>Acknowledged</td>
        <td>
            <#if (var.ackTime &gt; 0)>
                by ${var.ackBy} on ${var.ackTime?number_to_date}
            <#else>
                No
            </#if>
        </td>
    </tr>
    <tr>
        <td>Definition enabled</td>
        <td>${var.definitionEnabled?string("Yes","No")}</td>
    </tr>
</table>
<a href="/rest/alert/${var.id?c}/definition.html">Alert Definition</a>
<a href="/rest/alert/${var.id?c}/conditions.html">Condition Logs</a>
<a href="/rest/alert/${var.id?c}/notifications.html">Notification Logs</a>
<a href="/rest/resource/${var.resource.getResourceId()?c}.html">Resource</a>