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