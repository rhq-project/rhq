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
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.Status" -->
<html>
    <table>
        <thead>
        <tr>
            <td>Name</td><td>Value</td>
        </tr>
        </thead>
        <tr>
            <td>Alert definitions</td><td>${var.alertDefinitions}</td>
        </tr>
        <tr>
            <td>Alerts</td><td>${var.alerts}</td>
        </tr>
        <tr>
            <td>Platforms</td><td>${var.platforms}</td>
        </tr>
        <tr>
            <td>Servers</td><td>${var.servers}</td>
        </tr>
        <tr>
            <td>Services</td><td>${var.services}</td>
        </tr>
        <tr>
            <td>Metrics/min</td><td>${var.metricsMin}</td>
        </tr>
    </table>
</html>
