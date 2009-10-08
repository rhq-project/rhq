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
// -*- Mode: Java; indent-tabs-mode: nil; -*-
/*
 * MetricRange.java
 *
 */

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MetricRange {
    public static final long SHIFT_RANGE = 60001;

    private Long begin;
    private Long end;

    public MetricRange() {
    }

    public MetricRange(Long b, Long e) {
        begin = b;
        end = e;
    }

    public Long getBegin() {
        return begin;
    }

    public void setBegin(Long l) {
        begin = l;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long l) {
        end = l;
    }

    /**
     * If the end date is after "now", reset the end to "now" and recalculate the range.
     */
    public void shiftNow() {
        if ((getBegin() == null) || (getEnd() == null)) {
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - getEnd().longValue()) < SHIFT_RANGE) {
            long diff = getEnd().longValue() - getBegin().longValue();
            setEnd(new Long(now));
            setBegin(new Long(now - diff));
        }
    }

    /**
     * Return a date-formatted representation of the range.
     */
    public String getFormattedRange() {
        if ((getBegin() == null) || (getEnd() == null)) {
            return "{}";
        }

        Date bd = new Date(getBegin());
        Date ed = new Date(getEnd());

        SimpleDateFormat df = new SimpleDateFormat();

        return df.format(bd) + " to " + df.format(ed);
    }

    @Override
    public String toString() {
        return getFormattedRange();
    }
}