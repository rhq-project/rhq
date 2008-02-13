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
package org.rhq.core.clientapi.util.units;

import java.io.Serializable;

public class FormattedNumber implements Serializable {
    public static final String DEFAULT_SEPARATOR = " ";

    private String value;
    private String tag;
    private String separator;

    public FormattedNumber(String value, String tag) {
        this(value, tag, DEFAULT_SEPARATOR);
    }

    public FormattedNumber(String value, String tag, String separator) {
        this.value = value;
        this.tag = tag;
        this.separator = separator;
    }

    public String getValue() {
        return this.value;
    }

    public String getTag() {
        return this.tag;
    }

    @Override
    public String toString() {
        String res;

        res = this.value + this.separator + this.tag;
        return res.trim();
    }
}