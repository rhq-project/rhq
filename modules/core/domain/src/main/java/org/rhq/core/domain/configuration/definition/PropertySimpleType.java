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
     * Single-line strings (maximum length is 2000)
     */
    STRING("string"),

    /**
     * Multi-line strings (maximum length is 2000)
     */
    LONG_STRING("longString"),

    /**
     * Strings where the value is hidden at entry and not redisplayed (maximum length is 2000)
     */
    PASSWORD("password"),

    /**
     * A boolean value - "true" or "false"
     */
    BOOLEAN("boolean"),

    INTEGER("integer"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),

    /**
     * The absolute path to a file on the target platform (maximum length is 2000)
     */
    FILE("file"),

    /**
     * The absolute path to a directory on the target platform (maximum length is 2000)
     */
    DIRECTORY("directory");

    private String xmlName;

    PropertySimpleType(String xmlName) {
        this.xmlName = xmlName;
    }

    public String xmlName() {
        return this.xmlName;
    }

    public static PropertySimpleType fromXmlName(String xmlName) {
        for (PropertySimpleType type: PropertySimpleType.values()) {
            if (type.xmlName.equals(xmlName)) {
                return type;
            }
        }
        throw new IllegalArgumentException(xmlName);
    }
}