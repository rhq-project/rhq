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
package org.rhq.core.domain.resource;

/**
 * Provides the types of errors that can be associated with a {@link Resource}.
 *
 * @author John Mazzitelli
 * @see    ResourceError
 */
public enum ResourceErrorType {
    /**
     * The Resource's plugin configuration was somehow invalid, causing the connection to the Resource to fail.
     */
    INVALID_PLUGIN_CONFIGURATION,
    /**
     * An exception was thrown by the Resource component's getAvailablity() method the last time it was called.
     */
    AVAILABILITY_CHECK,

    /**
     * There was an attempt to upgrade the resource on the agent but it failed.
     */
    UPGRADE,

    /**
     * During resource sync the server reported a resource for which the agent does not recognize the type. This
     * happens when the agent disables a plugin for which there is already server-side inventory.
     */
    DISABLED_TYPE
}
