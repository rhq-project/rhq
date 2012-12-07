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
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.MetricAggregate" -->
<html>
    <script type="text/javascript" src="/rest/js/d3.js"></script>
    <script type="text/javascript" src="/rest/js/d3.layout.js"></script>
    <script type="text/javascript" src="/rest/js/d3.time.js"></script>
    <script type="text/javascript" src="/rest/js/rhq.js"></script>
<table border=1>
    <thead>
    <tr>
        <td>Name</td><td>Value</td>
    </tr>
    </thead>
    <tr>
        <#assign sched=var.scheduleId/>
        <#if var.isGroup()>
            <td>DefinitionId</td><td>${sched?c}</a></td>
        <#else>
            <td>ScheduleId</td><td><a href="/rest/metric/schedule/${sched?c}.html">${sched?c}</a></td>
        </#if>
    </tr>

    <tr>
        <td>Min</td><td>
        <#if var.min?has_content>
            ${var.min}
        <#else>
        NaN
        </#if>
    </td>
    </tr>
    <tr>
        <td>Avg</td>
        <td>
            <#if var.avg?has_content>
            ${var.avg}
            <#else>
            NaN
            </#if>
        </td>
    </tr>
    <tr>
        <td>Max</td>
        <td>
            <#if var.max?has_content>
            ${var.max}
            <#else>
            NaN
            </#if>
        </td>
    </tr>
    <tr>
        <#if !var.isGroup()>
            <td><a align="top" href="javascript:rhq.whisker(${sched?c},'one',400,200)">DataPoints</a></td><td>
        </#if>
        <table>
            <thead>
            <tr>
                <td>Time</td><td>Min</td><td>Value</td><td>Max</td>
            </tr>
            </thead>
            <#list var.dataPoints as item>
            <tr>

                <td>${item.timeStamp?number_to_time}</td>
                <td>
                    <#if item.low?has_content>
                    ${item.low}
                    <#else>
                    NaN
                    </#if>
                </td>
                <td>
                    <#if item.value?has_content>
                        ${item.value}
                    <#else>
                        NaN
                    </#if>
                </td>
                <td>
                    <#if item.high?has_content>
                        ${item.high}
                    <#else>
                        NaN
                    </#if>
                </td>
            </tr>
            </#list>
        </table>
        </td>
        <td>
            <div id="one"></div>
        </td>
    </tr>
</table>
</html>
