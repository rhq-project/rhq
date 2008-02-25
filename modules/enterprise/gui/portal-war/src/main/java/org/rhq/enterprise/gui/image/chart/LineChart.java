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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Iterator;

import org.rhq.core.domain.event.Event;

/**
 * LineChart draws a horizontal chart with a line that represents data point along the line. For a description of how to
 * use LineChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class LineChart extends VerticalChart {
    private Color[] m_clrDataLines = VerticalChart.DEFAULT_COLORS;
    private boolean m_showLineEvents;

    /**
     * Specified whether the data to be charted is cumulative data.
     */
    public boolean isCumulative = false;

    public LineChart() {
        super();
    }

    public LineChart(int charts) {
        super(charts);
    }

    public LineChart(int width, int height) {
        super(width, height);
    }

    public LineChart(int width, int height, int charts) {
        super(width, height, charts);
    }

    @Override
    protected Rectangle draw(ChartGraphics g) {
        m_showLineEvents = this.showEvents;
        super.showEvents = false;
        Rectangle result = super.draw(g);
        this.showEvents = m_showLineEvents;

        return result;
    }

    @Override
    protected void paint(ChartGraphics g, Rectangle rect) {
        int yLabelEvtDot = rect.y + rect.height + ChartGraphics.HALF_EVENT_HEIGHT + this.lineWidth;

        // Backup the current stroke and set the line width to 2 pixels
        Stroke origStroke = g.graphics.getStroke();
        BasicStroke stroke = new BasicStroke(2);
        g.graphics.setStroke(stroke);

        // Iterator through each data set
        Iterator iterLines = this.getDataSetIterator();
        for (int line = 0; iterLines.hasNext() == true; line++) {
            // Draw the Line
            DataPointCollection collDataPoints = (DataPointCollection) iterLines.next();

            Point ptData;
            int cActualPts = 0;
            int cDataPts = collDataPoints.size();
            int[] aiX = new int[cDataPts];
            int[] aiY = new int[cDataPts];
            int[] yDataPt = new int[cDataPts];
            long[] timestamp = new long[cDataPts];

            for (int index = 0; index < cDataPts; index++) {
                ptData = this.getDataPoint(rect, index, collDataPoints);

                if (ptData != null) {
                    aiX[cActualPts] = ptData.x;
                    aiY[cActualPts] = ptData.y;
                    yDataPt[index] = ptData.y;
                    cActualPts++;
                } else {
                    yDataPt[index] = yLabelEvtDot;
                }
            }

            g.graphics.setColor(this.m_clrDataLines[line]);
            g.graphics.drawPolyline(aiX, aiY, cActualPts);

            // Draw Events
            if (m_showLineEvents == true) {
                EventPointCollection collEvts = this.getEventPoints(line);
                if (collEvts.size() > 0) {
                    int[] evtDataPts = this.getDataPointEventIndexes(line);
                    int[] x = this.getXPoints(g, rect);

                    g.graphics.setColor(this.m_clrDataLines[line]);

                    for (int i = 0; i < evtDataPts.length; i++) {
                        if (evtDataPts[i] == -1) {
                            continue;
                        }

                        Event evt = collEvts.get(i);
                        int index = evtDataPts[i];

                        g.drawEvent(evt.getId(), x[index], yDataPt[index]);
                    }
                }
            }
        }

        // Reset the stroke as it was when we were called
        g.graphics.setStroke(origStroke);
    }

    public Color getDataLineColor(int index) {
        return this.m_clrDataLines[index];
    }

    public void setDataLineColor(int index, Color color) {
        this.m_clrDataLines[index] = color;
    }
}