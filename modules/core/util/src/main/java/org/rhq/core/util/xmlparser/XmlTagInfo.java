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
 * This class is used to describe which subtags a given tag supports. The object has a 'tag', which will be traversed
 * to, and a validity requirement, indicating if the subtag must exist in the tag, is optional, is valid one or more
 * times, or is valid zero or more times.
 */

public class XmlTagInfo {
    public static final int REQUIRED = 0;
    public static final int OPTIONAL = 1;
    public static final int ONE_OR_MORE = 2;
    public static final int ZERO_OR_MORE = 3;

    private XmlTagHandler tag;
    private int type;

    /**
     * Create a new tag info object with the specified tag and type.
     *
     * @param tag  The sub tag which will be traversed to
     * @param type One of REQUIRED, OPTIONAL, ONE_OR_MORE, ZERO_OR_MORE
     */
    public XmlTagInfo(XmlTagHandler tag, int type) {
        this.tag = tag;
        this.type = type;

        if ((type < REQUIRED) || (type > ZERO_OR_MORE)) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    XmlTagHandler getTag() {
        return this.tag;
    }

    int getType() {
        return this.type;
    }
}