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
package org.rhq.enterprise.gui.util;

/**
 * Describe the operation mode for metrics view we are in.
 *
 * @author Heiko W. Rupp
 */
public enum MetricsDisplayMode {
    /** A schedules for a single resource */
    RESOURCE,
    /** Definitions for a resource type */
    RESOURCE_DEFAULT,
    /** Schedules for all resources of a compatible group */
    COMPGROUP,
    /** Schedules for all resources of an autogroup */
    AUTOGROUP,
    /** Unset - instead of returning null */
    UNSET
}