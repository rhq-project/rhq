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