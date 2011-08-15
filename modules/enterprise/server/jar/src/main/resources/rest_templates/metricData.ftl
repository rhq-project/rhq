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
    <script type="text/javascript" src="/rest/js/whisker.js"></script>
<table border=1>
    <thead>
    <tr>
        <td>Name</td><td>Value</td>
    </tr>
    </thead>
    <tr>
        <#assign sched=var.scheduleId/>
        <td>ScheduleId</td><td><a href="/rest/1/metric/schedule/${sched?c}.html">${sched?c}</a></td>    </tr>
    <tr>
        <td>Min</td><td>${var.min}</td>
    </tr>
    <tr>
        <td>Avg</td><td>${var.avg}</td>
    </tr>
    <tr>
        <td>Max</td><td>${var.max}</td>
    </tr>
    <tr>
        <td><a align="top" href="javascript:whisker(${sched?c},'one')">DataPoints</a></td><td>
        <table>
            <thead>
            <tr>
                <td>Time</td><td>Min</td><td>Value</td><td>Max</td>
            </tr>
            </thead>
            <#list var.dataPoints as item>
            <tr>

                <td>${item.timeStamp?number_to_time}</td><td>${item.low}</td><td>${item.value}</td><td>${item.high}</td>
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
