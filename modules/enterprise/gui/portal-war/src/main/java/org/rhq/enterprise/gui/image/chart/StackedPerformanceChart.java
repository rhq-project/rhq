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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.Iterator;
import org.rhq.core.domain.measurement.MeasurementUnits;

public class StackedPerformanceChart extends HorizontalChart {
    private static String CALLS = "Calls: ";
    private static String SEMICOLON = ": ";
    private static String DEFAULT_DESTINATION_TYPE = "Destination";

    //private static final int DEFAULT_HEIGHT = 52;
    private static final int CHART_INDENT = 25;

    private static final Font DEFAULT_TITLE_FONT = new Font("Helvetica", Font.BOLD, 11);
    private static final Font DEFAULT_TITLE_TEXT_FONT = new Font("Helvetica", Font.PLAIN, 11);

    private PerformanceChart m_perf;
    private int m_interiorHeight;
    private String m_destinationType;

    public StackedPerformanceChart() {
        this(Chart.DEFAULT_WIDTH, VARIABLE_HEIGHT);
    }

    public StackedPerformanceChart(int width, int charts) {
        this(width, VARIABLE_HEIGHT, charts);
    }

    public StackedPerformanceChart(int width, int charts, String destinationType) {
        this(width, VARIABLE_HEIGHT, charts, destinationType);
    }

    public StackedPerformanceChart(int width, int height, int charts) {
        this(width, height, charts, DEFAULT_DESTINATION_TYPE);
    }

    public StackedPerformanceChart(int width, int height, int charts, String destinationType) {
        super(width, Chart.VARIABLE_HEIGHT, charts);

        this.m_interiorHeight = height;
        m_perf = new PerformanceChart(width, 1);
        m_perf.valueIndent = 8;
        m_perf.valueLines = 10;
        m_perf.setValueLegend("Call Time");
        m_destinationType = (destinationType != null) ? destinationType : DEFAULT_DESTINATION_TYPE;
    }

    protected void init() {
        super.init();

        this.showBottomLegend = false;
        this.showLeftLegend = false;
        this.topBorder = 0;
        this.bottomBorder = 0;
    }

    protected int calcVariableHeight() {
        int height = 0;

        if (this.m_interiorHeight == Chart.VARIABLE_HEIGHT) {
            this.m_interiorHeight = ((PerformanceChart.DEFAULT_BAR_HEIGHT * 2) * this.getDataPoints().size())
                + (this.lineWidth * 2);
        }

        // Iterator through each data set
        Iterator iterBars = this.getDataSetIterator();
        for (int line = 0; iterBars.hasNext(); line++) {
            // Calculate the height
            PerfDataPointCollection coll = (PerfDataPointCollection) iterBars.next();
            if (coll.size() == 0) {
                continue;
            }

            this.setChartProperties(m_perf, coll, line, this.getDataSetCount());
            if (line < (this.getDataSetCount() - 1)) {
                height++;
            }

            height += (this.m_interiorHeight + m_perf.getExteriorHeight());
        }

        if (height == 0) {
            height = this.m_metricsLegend.getHeight();
            this.m_bNoData = true;
        }

        return height;
    }

    protected Class getDataCollectionClass() {
        return PerfDataPointCollection.class;
    }

    private int getTitleHeight(PerfDataPointCollection coll) {
        int cyText = m_metricsLegend.getHeight();
        int result = cyText;

        if (coll.getURL() != null) {
            result += cyText;
        }

        if (coll.getTypeString().length() > 0) {
            result += cyText;
        }

        return result;
    }

    protected Rectangle draw(ChartGraphics g) {
        Rectangle rect = null;

        if (!this.hasData()) {
            return super.draw(g);
        }

        // Iterator through each data set
        Iterator iterLines = this.getDataSetIterator();
        for (int line = 0; iterLines.hasNext(); line++) {
            // Draw the chart
            PerfDataPointCollection src = (PerfDataPointCollection) iterLines.next();

            if (src.size() == 0) {
                continue;
            }

            DataPointCollection dest = m_perf.getDataPoints();
            dest.clear();
            dest.addAll(src);

            this.setChartProperties(m_perf, src, line, this.getDataSetCount());
            m_perf.height = m_interiorHeight + m_perf.getExteriorHeight();

            ChartGraphics g2 = new ChartGraphics(m_perf, g.graphics);
            m_perf.floor = this.m_adRangeMarks[0];
            m_perf.ceiling = this.m_adRangeMarks[this.m_adRangeMarks.length - 1];
            m_perf.calcRanges();
            m_perf.calc(g2.graphics);
            m_perf.draw(g2);

            rect = m_perf.getExteriorRectangle();
            m_perf.yOffset += m_perf.height;

            // Draw titles
            this.drawTitles(g, src, rect);

            g.graphics.setColor(this.xLineColor);
            g.graphics.drawLine(rect.x, m_perf.yOffset, rect.x + rect.width, m_perf.yOffset);

            m_perf.yOffset += this.lineWidth;
        }

        return rect;
    }

    private void drawTitles(ChartGraphics g, PerfDataPointCollection coll, Rectangle rect) {
        g.graphics.setColor(this.legendTextColor);

        FontMetrics metrics = g.graphics.getFontMetrics(DEFAULT_TITLE_FONT);
        int x = DEFAULT_BORDER_SIZE;
        int cyTitle = this.getTitleHeight(coll);
        int yTitle = rect.y + metrics.getAscent();

        String text = coll.getURL();
        if (text != null) {
            String label = m_destinationType + SEMICOLON;
            g.graphics.setFont(DEFAULT_TITLE_FONT);
            g.graphics.drawString(label, x, yTitle);
            g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
            g.graphics.drawString(coll.getURL(), x + metrics.stringWidth(label), yTitle);

            yTitle += this.m_metricsLabel.getHeight();
        }

        String title = coll.getTypeString();
        if (title.length() > 0) {
            title += SEMICOLON;
            g.graphics.setFont(DEFAULT_TITLE_FONT);
            g.graphics.drawString(title, x, yTitle);

            text = coll.getTypeName();
            if (text != null) {
                g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
                g.graphics.drawString(coll.getTypeName(), x + metrics.stringWidth(title), yTitle);
            }

            yTitle += this.m_metricsLabel.getHeight();
        }

        g.graphics.setFont(DEFAULT_TITLE_FONT);
        g.graphics.drawString(CALLS, x, yTitle);
        g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
        g.graphics.drawString(Integer.toString(coll.getRequest()), x + metrics.stringWidth(CALLS), yTitle);
    }

    private void setChartProperties(Chart chart, PerfDataPointCollection coll, int chartnum, int total) {
        if (chartnum == 0) // First Chart
        {
            m_perf.showTopLabels = true;
            m_perf.showTopLegend = true;
            m_perf.showBottomLabels = false;
            m_perf.showBottomLegend = false;
        } else if (chartnum < (this.getDataSetCount() - 1)) // Middle Chart
        {
            m_perf.showTopLabels = false;
            m_perf.showTopLegend = false;
            m_perf.showBottomLabels = false;
            m_perf.showBottomLegend = false;
        } else { // Last Chart
            m_perf.showTopLabels = false;
            m_perf.showTopLegend = false;
            m_perf.showBottomLabels = true;
            m_perf.showBottomLegend = true;
        }

        m_perf.topBorder = this.getTitleHeight(coll);
        m_perf.leftBorder = CHART_INDENT;
        m_perf.setHealthChart(false);
        m_perf.showMinDigits = false;
        m_perf.showStacked = true;
    }

    public void setFormat(MeasurementUnits units) {
        super.setFormat(units);
        m_perf.setValueLegend(m_perf.getValueLegend() + " (" + m_fmtUnits + ")");
    }
}