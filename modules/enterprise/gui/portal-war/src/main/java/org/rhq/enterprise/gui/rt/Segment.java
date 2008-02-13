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
package org.rhq.enterprise.gui.rt;

import java.io.Serializable;

/**
 * Defines a specific segment of a response time measurement. Each segment represents the response time of one of the
 * service tiers that handles some portion of a request.
 */
public class Segment implements Serializable {
    /**
     * One of a set of constants defined by the rt subsystem that identifies which service tier generated this
     * measurement value
     */
    private Integer id;

    /**
     * The measurement value itself
     */
    private double value;

    public Segment() {
    }

    public Segment(int type, double value) {
        this.id = type;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer i) {
        id = i;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double d) {
        value = d;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("{");
        s.append("id=").append(id);
        s.append(" value=").append(value);
        return s.append("}").toString();
    }
}