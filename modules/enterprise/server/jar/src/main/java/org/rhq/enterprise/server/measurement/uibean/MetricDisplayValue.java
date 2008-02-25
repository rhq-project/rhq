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
package org.rhq.enterprise.server.measurement.uibean;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetricDisplayValue implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MetricDisplayValue.class);
    private String valueFmt;
    private Double value;

    /**
     * flags whether or not the value meets the criteria for comparison against a user specified threshold
     */
    private Boolean highlighted;

    public MetricDisplayValue(double aValue) {
        this.value = aValue;
    }

    public MetricDisplayValue(Double aValue) {
        this.value = aValue;
    }

    public Double getValue() {
        return value;
    }

    public String getValueFmt() {
        return valueFmt;
    }

    public void setValue(Double aValue) {
        this.value = aValue;
    }

    public void setValueFmt(String aValueFmt) {
        valueFmt = aValueFmt;
    }

    public Boolean getHighlighted() {
        return highlighted;
    }

    public void setHighlighted(Boolean flag) {
        highlighted = flag;
    }

    @Override
    public String toString() {
        if (valueFmt != null) {
            return valueFmt;
        }

        log.trace("toString() returning unformatted value");
        return value.toString();
    }
}