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
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.ResourceWithType" -->

    <table>
        <tr>
            <th>Name</th><th>Value</th>
        </tr>
        <tr>
            <td>Name</td><td>${var.resourceName}</td>
        </tr>
        <tr>
            <td>Id</td><td><a href="/rest/resource/${var.resourceId?c}.html">${var.resourceId?c}</a></td>
        </tr>
        <tr>
            <td>Type name</td><td>${var.typeName}</td>
        </tr>
        <tr>
            <td>Type id</td><td>${var.typeId?c}</td>
        </tr>
        <tr>
            <td>Plugin</td><td>${var.pluginName}</td>
        </tr>
        <#if var.parentId??>
        <tr>
            <td>Parent Id</td><td><a href="/rest/resource/${var.parentId?c}.html">${var.parentId?c}</a></td>
        </tr>
        </#if>
    </table>
    <a href="/rest/resource/${var.resourceId?c}/children.html">Children</a><br/>
    <a href="/rest/resource/${var.resourceId?c}/schedules.html">Schedules</a><br/>
    <a href="/rest/resource/${var.resourceId?c}/availability.html">Current availability</a><br/>
    <a href="/rest/resource/${var.resourceId?c}/availability/history.html">Availability History</a><br/>
    <a href="/rest/alert.html?resourceId=${var.resourceId?c}">Up to 20 Alerts for this resource</a><br/>
    <a href="/rest/operation/history.html?resourceId=${var.resourceId?c}">Operations history</a>
