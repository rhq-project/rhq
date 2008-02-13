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
package org.rhq.core.domain.configuration.definition;

import org.rhq.core.domain.configuration.PropertySimple;

/**
 * These represent the supported data types for {@link PropertySimple} values.
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public enum PropertySimpleType {
    /**
     * Single-line strings
     */
    STRING,

    /**
     * Multi-line strings
     */
    LONG_STRING,

    /**
     * Strings where the value is hidden at entry and not redisplayed
     */
    PASSWORD,

    /**
     * A boolean value - "true" or "false"
     */
    BOOLEAN,

    INTEGER, LONG, FLOAT, DOUBLE,

    /**
     * The absolute path to a file on the target platform
     */
    FILE,

    /**
     * The absolute path to a directory on the target platform
     */
    DIRECTORY
}