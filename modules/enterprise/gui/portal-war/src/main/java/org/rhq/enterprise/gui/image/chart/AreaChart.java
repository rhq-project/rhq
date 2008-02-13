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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import org.rhq.enterprise.gui.image.data.IDataPoint;

/**
 * HighLowChart draws a horizontal chart with shaded areas to display the data point values. For a description of how to
 * use AreaChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class AreaChart extends ColumnChart {
    public AreaChart() {
        super();
    }

    public AreaChart(int width, int height) {
        super(width, height);
    }

    protected void init() {
        super.init();
        this.valueIndent = 0;
    }

    protected void paint(ChartGraphics g, Rectangle rect) {
        g.graphics.setColor(this.columnColor);

        DataPointCollection coll = this.getDataPoints();
        Iterator iter = coll.iterator();

        Rectangle rectBar = new Rectangle();
        rectBar.width = rect.width / coll.size();

        for (int index = 0; iter.hasNext() == true; index++) {
            IDataPoint datapt = (IDataPoint) iter.next();

            if (Double.isNaN(datapt.getValue()) == true) {
                continue;
            }

            Point ptData = this.getDataPoint(rect, index, coll);
            if (ptData == null) {
                continue;
            }

            rectBar.x = ptData.x;
            rectBar.y = ptData.y;
            rectBar.height = (rect.y + rect.height) - rectBar.y;

            // Draw Bar
            g.graphics.fillRect(rectBar.x, rectBar.y, rectBar.width, rectBar.height);
        }
    }
}