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
package org.rhq.enterprise.server.plugin.pc;

/**
 * Defines the types of server-side plugins that are currently supported.
 * 
 * @author John Mazzitelli
 */
public enum ServerPluginType {
    /**
     * Generic plugins need only be started and stopped by the plugin container, but otherwise has no
     * dependencies or needs that are provided to it by the plugin container.
     */
    GENERIC,

    /**
     * Content plugins obtain repos and packages from remote locations.
     */
    CONTENT,

    /**
     * Perspective plugins allow customization of the management interface.
     */
    PERSPECTIVE,

    /**
     * Alert plugins allow alerts to be sent to custom endpoints.
     */
    ALERT
}
