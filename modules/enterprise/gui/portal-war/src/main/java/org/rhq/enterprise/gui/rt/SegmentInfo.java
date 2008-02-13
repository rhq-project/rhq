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
import java.util.Arrays;
import java.util.List;
import org.rhq.enterprise.gui.image.data.IStackedDataPoint;

/**
 * Provides segmented and total response time measurement for an individual request (or average for a group of
 * requests).
 */
public class SegmentInfo implements Serializable, IStackedDataPoint {
    private Segment[] segments;
    private double total;
    private String chartLabel = null;
    private long timestamp;

    public SegmentInfo(String label) {
        segments = new Segment[3];
        chartLabel = label;
    }

    public void addSegment(int index, Segment s) {
        if ((segments[index] != null) && !Double.isNaN(segments[index].getValue())) {
            total -= segments[index].getValue();
        }

        segments[index] = s;
        if (!Double.isNaN(segments[index].getValue())) {
            total += segments[index].getValue();
        }
    }

    public Segment getSegment(int index) {
        return segments[index];
    }

    public List getSegments() {
        return Arrays.asList(segments);
    }

    public boolean removeSegment(Segment s) {
        for (int i = 0; i < 3; i++) {
            if (segments[i].equals(s)) {
                segments[i] = null;
                return true;
            }
        }

        return false;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double d) {
        total = d;
    }

    public double getValue() {
        for (int i = 0; i < 3; i++) {
            if ((segments[i] != null) && !Double.isNaN(segments[i].getValue())) {
                return segments[i].getValue();
            }
        }

        return Double.NaN;
    }

    public double[] getValues() {
        double[] values = new double[3];
        for (int i = 0; i < 3; i++) {
            if (segments[i] != null) {
                values[i] = segments[i].getValue();
            }
        }

        return values;
    }

    public void setLabel(String label) {
        chartLabel = label;
    }

    public String getLabel() {
        return chartLabel;
    }

    public void setTimestamp(long time) {
        timestamp = time;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("{");
        s.append("total=").append(total);
        s.append(" segments=[").append(segments[0]);
        s.append(",").append(segments[1]);
        s.append(",").append(segments[2]).append("]");
        return s.append("}").toString();
    }
}