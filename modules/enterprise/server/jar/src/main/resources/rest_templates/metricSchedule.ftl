<#ftl >
<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.MetricSchedule" -->
<html>
    <table border=1>
        <thead>
            <tr>
                <td>Name</td><td>Value</td>
            </tr>
        </thead>
        <tr>
            <td>Id</td><td>${var.scheduleId}</td>
        </tr>
        <tr>
            <td>Internal Name</td><td>${var.scheduleName}</td>
        </tr>
        <tr>
            <td>Name</td><td>${var.displayName}</td>
        </tr>
        <tr>
            <td>Enabled</td><td>${var.enabled?string("Yes","No")}</td>
        </tr>
        <tr>
            <td>Collection interval (ms)</td><td>${var.collectionInterval}</td>
        </tr>
        <tr>
            <td>Units</td><td>${var.unit}</td>
        </tr>
    </table>
    <a href="/rest/1/metric/data/${var.scheduleId}.html">Metric data</a>
    <br/>

</html>