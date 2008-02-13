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
import java.awt.Rectangle;

/**
 * ColumnLineChart draws a chart with vertical bars that represent the value of each data point with a connecting data
 * points on top of the bars. For a description of how to use ColumnChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class ColumnLineChart extends ColumnChart {
    private final LineChart m_lineChart = new LineChart();

    /**
     * Constructs a ColumnLineChart class with a default width, height and properties.
     */
    public ColumnLineChart() {
    }

    /**
     * Constructs a ColumnLineChart class with a specified width and height.
     *
     * @param width  The width of the chart in pixels.
     * @param height The height of the chart in pixels.
     */
    public ColumnLineChart(int width, int height) {
        super(width, height);
    }

    protected Rectangle draw(ChartGraphics g) {
        Rectangle rect = super.draw(g);

        this.m_lineChart.width = this.width;
        this.m_lineChart.height = this.height;

        this.m_lineChart.getDataPoints().addAll(this.getDataPoints());
        this.m_lineChart.calcRanges();

        this.m_lineChart.paint(g, rect);

        return rect;
    }

    /**
     * Retrieves the color of the chart's datum line. This is the line that represents the chart's data points.
     *
     * @return A java.awt.Color object that contains the datum line color.
     *
     * @see    java.awt.Color
     */
    public Color getDataLineColor() {
        return this.m_lineChart.getDataLineColor(0);
    }

    /**
     * Sets the color of the chart's datum line. This is the line that represents the chart's data points.
     *
     * @param     value A java.awt.Color object that contains the datum line color.
     *
     * @exception IllegalArgumentException If the value parameter is null.
     *
     * @see       java.awt.Color
     */
    public Color setDataLineColor(Color value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        this.m_lineChart.setDataLineColor(0, value);
        return value;
    }
}