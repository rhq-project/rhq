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
package org.rhq.core.util.xmlparser;

/**
 * A class which defines an attribute and the conditions on which it is valid.
 */
public class XmlAttr {
    public static final int REQUIRED = 0;
    public static final int OPTIONAL = 1;

    private String name;
    private int type;

    /**
     * Create a new XmlAttr attribute with the given name, and type.
     *
     * @param name The name of the XML tag attribute
     * @param type One of REQUIRED or OPTIONAL, indicating that the attribute is required for the tag to be valid, or
     *             optional, meaning it is allowed but not required.
     */
    public XmlAttr(String name, int type) {
        this.name = name;
        this.type = type;

        if ((type != REQUIRED) && (type != OPTIONAL)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    public String getName() {
        return this.name;
    }

    public int getType() {
        return this.type;
    }
}