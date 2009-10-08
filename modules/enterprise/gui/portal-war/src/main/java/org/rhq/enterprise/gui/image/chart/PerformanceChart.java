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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.enterprise.gui.image.data.IDataPoint;
import org.rhq.enterprise.gui.image.data.IStackedDataPoint;

public class PerformanceChart extends HorizontalChart {
    private static final Color[] DEFAULT_BAR_COLORS = { new Color(0x9B, 0xBA, 0x70), new Color(0x12, 0xB3, 0xB3),
        new Color(0xE7, 0x5A, 0x00), };

    protected static final int DEFAULT_BAR_HEIGHT = 7;
    private int m_cyBar;
    private Color[] m_clrBars = DEFAULT_BAR_COLORS;
    private boolean m_bHealthChart = true;

    public boolean showMinDigits = true;
    public boolean showStacked = false;

    public PerformanceChart() {
        super();
    }

    public PerformanceChart(int width, int height) {
        super(width, height);
    }

    @Override
    protected void init() {
        this.m_cyBar = DEFAULT_BAR_HEIGHT;

        this.showFullLabels = true;
        this.showRightLabels = false;
        this.showLeftLegend = false;
        this.showBottomLegend = false;
        this.valueIndent = (this.m_cyBar / 2) + this.lineWidth;
        this.valueLines = 5;

        this.setFormat(MeasurementUnits.EPOCH_MILLISECONDS);
    }

    @Override
    protected String[] getUnitLabels() {
        return HealthChart.getUnitStrings(this.getDataPoints(), this.m_bHealthChart);
    }

    @Override
    protected String[] getXLabels() {
        if (this.m_adRangeMarks == null) {
            return null;
        }

        if (this.showMinDigits == true) {
            return super.getXLabels();
        }

        String[] result = new String[m_adRangeMarks.length];

        for (int i = 0; i < m_adRangeMarks.length; i++) {
            result[i] = new MeasurementNumericValueAndUnits(m_adRangeMarks[i], m_fmtUnits).toString();
        }

        return result;
    }

    @Override
    protected void paint(ChartGraphics g, Rectangle rect) {
        super.paint(g, rect);

        /////////////////////////////////////////////////////////
        // Draw the Column Bars

        // Calculate Bar Width
        Rectangle rectBar = new Rectangle(rect.x + this.lineWidth, rect.y + this.lineWidth, 0, this.m_cyBar);

        int cDataPoints = this.getDataPoints().size();

        if (cDataPoints == 0) {
            return;
        }

        int overhang = this.m_cyBar / 2;

        Iterator iter = this.getDataPoints().iterator();
        for (int i = 0; iter.hasNext() == true; i++) {
            IDataPoint datapt = (IDataPoint) iter.next();

            if (Double.isNaN(datapt.getValue())) {
                continue;
            }

            Point ptData = this.getDataPoint(rect, i);
            if (ptData == null) {
                continue;
            }

            // Draw the max bar
            int cx = ((ptData.x == rectBar.x) ? 1 : (ptData.x - rectBar.x + this.lineWidth));
            g.graphics.setColor(this.m_clrBars[2]);
            g.graphics.fillRect(rectBar.x, ptData.y - overhang, cx, this.m_cyBar);

            if ((this.showStacked == true) && (datapt instanceof IStackedDataPoint)
                && (((IStackedDataPoint) datapt).getValues().length > 1)) {
                IStackedDataPoint sdp = (IStackedDataPoint) datapt;
                double scale = this.scale(rect.width);

                double[] vals = sdp.getValues();

                if (sdp.getValues().length >= 2) {
                    double tmp = (scale * (vals[1] - this.m_floor));
                    cx = (int) Math.round(tmp) + xOffset;
                    if (cx == 0) {
                        cx++;
                    }

                    g.graphics.setColor(this.m_clrBars[1]);
                    g.graphics.fillRect(rectBar.x, ptData.y - overhang + 2, cx, this.m_cyBar - 2);
                }

                cx = (int) (scale * (vals[2] - this.m_floor));
                if (cx == 0) {
                    cx++;
                }

                g.graphics.setColor(this.m_clrBars[0]);
                g.graphics.fillRect(rectBar.x, ptData.y - overhang + 2, cx, this.m_cyBar - 2);
            }
        }
    }

    protected void setHealthChart(boolean health) {
        this.m_bHealthChart = health;
    }
}