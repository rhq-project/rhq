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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.enterprise.gui.image.data.IDataPoint;
import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;

public class HorizontalChart extends Chart {
    private Rectangle m_rect;

    protected HorizontalChart() {
        this.init();
    }

    protected HorizontalChart(int width, int height) {
        super(width, height);
        this.init();
    }

    protected HorizontalChart(int width, int height, int charts) {
        super(width, height, charts);
        init();
    }

    protected void init() {
        this.showUnitLines = true;
    }

    @Override
    protected Rectangle adjustRectangle(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int iSpread = this.getUnitSpread(g, rect);
        rect.height = (iSpread * (cDataPts - 1)) + (this.valueIndent * 2) + this.lineWidth;

        this.m_rect = rect;
        return rect;
    }

    @Override
    protected Rectangle getInteriorRectangle(ChartGraphics g) {
        return m_rect;
    }

    protected String[] getUnitLabels() {
        DataPointCollection coll = this.getDataPoints();
        Iterator<IDataPoint> iter = coll.iterator();
        String[] result = new String[coll.size()];

        for (int i = 0; iter.hasNext() == true; i++) {
            result[i] = ((IDisplayDataPoint) iter.next()).getLabel();
        }

        return result;
    }

    @Override
    protected String[] getXLabels() {
        if (this.m_adRangeMarks == null) {
            return null;
        }

        String[] result = MeasurementConverter.formatToSignificantPrecision(m_adRangeMarks, m_fmtUnits, true);

        return result;
    }

    private int getUnitSpread(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int iSpread = rect.height - (this.valueIndent * 2);

        return (cDataPts > 1) ? (iSpread / (cDataPts - 1)) : iSpread;
    }

    @Override
    protected int getYLabelWidth(Graphics2D g) {
        int maxWidth = 0;

        String[] labels = this.getUnitLabels();

        for (int i = 0; i < labels.length; i++) {
            int labelWidth = this.m_metricsLabel.stringWidth(labels[i]);

            if (labelWidth > maxWidth) {
                maxWidth = labelWidth;
            }
        }

        return maxWidth;
    }

    @Override
    protected Rectangle draw(ChartGraphics g) {
        ///////////////////////////////
        // Paint the chart background

        Rectangle rect = super.draw(g);

        if (this.hasData() == false) {
            return rect;
        }

        ///////////////////////////////////////
        // Paint the chart exterior and lines

        // Calculate points
        double dScale = this.scale(rect.width);

        //////////////////////////////////////////////////////////
        // Draw the Value (Y) Legend

        if (this.showLeftLegend == true) {
            g.drawYLegendString(this.getUnitLegend());
        }

        //////////////////////////////////////////////////////////
        // Draw the unit (Y) axis cross lines and labels

        DataPointCollection coll = this.getDataPoints();

        int[] lines = new int[coll.size()];
        String[] labels = this.getUnitLabels();

        int spread = this.getUnitSpread(g.graphics, rect);

        for (int i = 0, y = rect.y + rect.height - this.valueIndent; i < coll.size(); i++, y -= spread) {
            lines[i] = y;
        }

        g.drawXLines(lines, labels, false);

        //////////////////////////////////////////////////////////
        // Draw the unit (X) axis tick marks and labels

        labels = this.getXLabels();
        lines = new int[this.m_adRangeMarks.length];

        for (int i = 0; i < this.m_adRangeMarks.length; i++) {
            lines[i] = rect.x + (int) Math.round((this.m_adRangeMarks[i] - this.m_floor) * dScale);
        }

        g.drawYLines(lines, labels, true, xLabelsSkip);

        ////////////////////////////////////////////////////////////
        // Draw the Top & Bottom Legend

        g.drawXLegendString(this.getValueLegend());

        ///////////////////////////////
        // Paint the chart interior

        if (this.showValues == true) {
            paint(g, rect);
        }

        return rect;
    }

    protected void paint(ChartGraphics g, Rectangle rect) {
    }

    protected Point getDataPoint(Rectangle rect, int datapoint) {
        return this.getDataPoint(rect, datapoint, this.getDataPoints());
    }

    protected Point getDataPoint(Rectangle rect, int datapoint, DataPointCollection coll) {
        Point ptResult = super.getDataPoint(rect.width, rect.height, datapoint, coll);

        // Add & Flip the units
        if (ptResult != null) {
            ptResult = new Point(rect.x + (rect.width - ptResult.y), rect.y + ptResult.x);
        }

        return ptResult;
    }
}