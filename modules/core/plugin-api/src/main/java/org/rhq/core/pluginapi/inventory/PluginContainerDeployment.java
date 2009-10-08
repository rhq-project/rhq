/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pluginapi.inventory;

/**
 * This enum indicates where the plugin container (and therefore the plugins) are deployed.
 * 
 * @author John Mazzitelli
 */
public enum PluginContainerDeployment {
    /**
     * Indicates the plugin container is deployed inside an external agent. All
     * managed resources are going to be remote from where the plugins reside.
     */
    AGENT,

    /**
     * Indicates the plugin container is embedded directly inside some managed resource.
     * In other words, the plugin container is running inside a resource that is itself being
     * managed by one or more plugins.
     */
    EMBEDDED
}
