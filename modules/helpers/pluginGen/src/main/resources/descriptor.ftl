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
<?xml version="1.0"?>
<plugin name="${props.name}"
        displayName="${props.name}Plugin"
        package="${props.packagePrefix}"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">

        <${props.category.lowerName}
            name="${props.name}" <!-- TODO separate out plugin name and service name -->
            discovery="${props.discoveryClass}"
            class="${props.componentClass}"
            version="1.0"
            <#if props.singleton>
            singleton="true"
            </#if>

        >
            <plugin-configuration>
                <!-- TODO add your own here -->
            </plugin-configuration>

        <#if props.monitoring>
            <metric name="dummyMetric"/>
        </#if>

    <!-- TODO process scans -->

        <#if props.operations>
            <operation name="dummyOperation">
                <!-- TODO supply parameters and return values -->
            </operation>
        </#if>

        <#if props.events>
            <event name="dummyEvent"/>
        </#if>

        <#if props.resourceConfiguration>
            <resource-configuration>
                <!-- TODO supply your configuration parameters -->
            </resource-configuration>
        </#if>

        </${props.category.lowerName}>

</plugin>