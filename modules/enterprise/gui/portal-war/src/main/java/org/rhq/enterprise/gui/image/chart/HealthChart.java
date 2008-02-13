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
package org.rhq.enterprise.gui.image.chart;

import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;

public class HealthChart extends ColumnChart {
    protected HealthChart(int width, int height) {
        super(width, height);
    }

    protected void init() {
        super.init();
        this.columnWidth = 11;
    }

    protected String[] getXLabels() {
        return HealthChart.getUnitStrings(this.getDataPoints(), true);
    }

    protected static String[] getUnitStrings(DataPointCollection datapts, boolean showHealthLabels) {
        String[] result = new String[datapts.size()];

        // Assume 8 hours
        double interval = 8.0 / result.length;
        for (int i = 0; i < result.length; i++) {
            if (showHealthLabels == true) {
                int remainder = result.length - i;

                if (remainder > 1) {
                    double time = remainder * interval;
                    String label = ((time % 1) == 0) ? String.valueOf((int) time) : String.valueOf(time);

                    result[i] = "-" + label + "hr";
                } else {
                    result[i] = "Now";
                }
            } else {
                result[i] = ((IDisplayDataPoint) datapts.get(i)).getLabel();
            }
        }

        return result;
    }
}