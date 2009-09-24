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
<#-- @ftlvariable name="props" type="org.rhq.helpers.pluginGen.Props" -->
<#--
    This file contains the body of the descriptor for a single
    platform/server/service. It is called by descriptor.ftl

-->
            name="${props.name}" <#-- TODO separate out plugin name and service name -->
            discovery="${props.discoveryClass}"
            class="${props.componentClass}"
            <#if props.singleton>
            singleton="true"
            </#if>
            <#if props.manualAddOfResourceType>
            supportsManualAdd="true"
            </#if>
            <#if props.createChildren && props.deleteChildren>
                createDeletePolicy="both"
            <#elseif props.createChildren && !props.deleteChildren>
                createDeletePolicy="create-only"
            <#elseif !props.createChildren && props.deleteChildren>
                createDeletePolicy="delete-only"
            <#else > <#-- Dont mention it, as 'neither' is default -->
            </#if>
        >

            <plugin-configuration>
                <!-- TODO add your own here -->
                <c:simple-property name="dummy"/>
            </plugin-configuration>

        <!-- TODO process scans -->

        <#if props.operations>
            <operation name="dummyOperation">
                <!-- TODO supply parameters and return values -->
            </operation>

        </#if>
        <#if props.monitoring>
            <metric property="dummyMetric" displayName="Dummy display name"/>

        </#if>
        <#if props.events>
            <event name="${props.name}DummyEvent"/>

        </#if>
        <#if props.resourceConfiguration>
            <resource-configuration>
                <!-- TODO supply your configuration parameters -->
                <c:simple-property name="dummy"/>
            </resource-configuration>

        </#if>
