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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Iterator;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.enterprise.gui.image.data.IDataPoint;
import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;
import org.rhq.enterprise.gui.image.data.IHighLowDataPoint;

public class VerticalChart extends Chart {

    protected static final Color[] DEFAULT_COLORS = { new Color(0x00, 0x00, 0xFF), new Color(0xFF, 0x00, 0x00),
        new Color(0xCC, 0x00, 0x99), new Color(0x9B, 0xBA, 0x70), new Color(0xFF, 0xFF, 0x33),
        new Color(0x00, 0xFF, 0x00), new Color(0x00, 0xFF, 0xFF), new Color(0xA6, 0x78, 0x38),
        new Color(0x99, 0x66, 0x99), new Color(0x74, 0x90, 0xAA), };

    protected static final Color GOOD_COLOR // Green
    = new Color(0x48, 0xB3, 0x68);
    protected static final Color DANGER_COLOR // Red
    = new Color(0xD5, 0x3E, 0x3E);
    protected static final Color UNKNOWN_COLOR // Grey
    = new Color(0x00, 0x00, 0xCC);

    private Rectangle m_rect;
    private long m_timeScale;
    private int m_cumulativeTrend = Trend.TREND_NONE;

    public VerticalChart() {
        super();
        init();
    }

    public int getCumulativeTrend() {
        return m_cumulativeTrend;
    }

    public void setCumulativeTrend(int trend) {
        if ((trend < Trend.TREND_NONE) || (trend > Trend.TREND_UP)) {
            throw new IllegalArgumentException("Argument must be a Cumulative type.");
        }

        m_cumulativeTrend = trend;
    }

    protected VerticalChart(int width, int height) {
        super(width, height);
        init();
    }

    protected VerticalChart(int charts) {
        super(charts);
        init();
    }

    protected VerticalChart(int width, int height, int charts) {
        super(width, height, charts);
        init();
    }

    protected void init() {
        this.showAverage = true;
        this.showValueLines = true;
        this.showLow = true;
        this.showPeak = true;
    }

    @Override
    protected Collection<DataPointCollection> initData(Collection<DataPointCollection> coll) {

        if (this.m_fmtUnits == MeasurementUnits.PERCENTAGE) {
            this.floor = 0;
            this.ceiling = 1;
            for (DataPointCollection dpc : coll) {
                Iterator<IDataPoint> it = dpc.iterator();
                while (it.hasNext()) {
                    IDataPoint point = it.next();
                    double ref;
                    if (point instanceof IHighLowDataPoint) {
                        ref = ((IHighLowDataPoint) point).getHighValue();
                    } else
                        ref = point.getValue();
                    if (ref > this.ceiling)
                        this.ceiling = ref;
                }
            }
        }

        //        if ((this.m_fmtUnits == MeasurementUnits.PERCENTAGE) && ((this.m_dLowValue >= 0) && (this.m_dPeakValue <= 1))) {
        //            this.floor = 0;
        //            this.ceiling = 1;
        //        }

        return coll;
    }

    protected Point adjustBorders(Point pt) {
        if (pt != null) {
            // Adjust to add the left and top margins to put in the interior rectangle
            pt.x += m_rect.x;
            pt.y += m_rect.y;
        }

        return pt;
    }

    @Override
    protected Rectangle adjustRectangle(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int spread = this.getUnitSpread(g, rect);
        rect.width = (spread * (cDataPts - 1)) + (this.valueIndent * 2) + this.lineWidth;

        this.m_rect = rect;
        return rect;
    }

    @Override
    protected Rectangle getInteriorRectangle(ChartGraphics g) {
        return m_rect;
    }

    @Override
    protected String[] getXLabels() {
        DataPointCollection coll = this.getDataPoints();
        int collSize = coll.size();
        String[] result = new String[collSize];

        for (int i = 0; i < collSize; i++) {
            IDisplayDataPoint dp = (IDisplayDataPoint) coll.get(i);
            result[i] = ScaleFormatter.formatTime(dp.getTimestamp(), this.m_timeScale, collSize);
        }

        return result;
    }

    protected int[] getXPoints(ChartGraphics g, Rectangle rect) {
        DataPointCollection coll = this.getDataPoints();
        int collSize = coll.size();
        int[] res = new int[collSize];
        int spread = this.getUnitSpread(g.graphics, this.getInteriorRectangle(g));
        int xHorzMarks = rect.x + this.valueIndent;

        for (int i = 0, x = xHorzMarks; i < collSize; i++, x += spread) {
            res[i] = x;
        }

        return res;
    }

    private int getUnitSpread(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int iSpread = rect.width - (this.valueIndent * 2);

        return (cDataPts > 1) ? (iSpread / (cDataPts - 1)) : iSpread;
    }

    @Override
    protected int getYLabelWidth(Graphics2D g) {
        int width;
        int maxWidth = 0;

        String[] formatted = MeasurementConverter.formatToSignifantPrecision(m_adRangeMarks, m_fmtUnits, true);

        for (int i = 0; i < formatted.length; i++) {
            width = this.m_metricsLabel.stringWidth(formatted[i]);

            if (width > maxWidth) {
                maxWidth = width;
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

        Graphics2D graph = g.graphics;

        // Calculate points
        double dScale = this.scale(rect.height);

        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;

        int yAvgLine = y2 - (int) Math.round((this.getAverageValue() - this.m_floor) * dScale);
        int yLowLine = y2 - (int) Math.round((this.getLowValue() - this.m_floor) * dScale);
        int yBaseLine = y2 - (int) Math.round((this.baseline - this.m_floor) * dScale);
        int yPeakLine = y2 - (int) Math.round((this.getPeakValue() - this.m_floor) * dScale);

        int yHighBottom = 0;
        if (Double.isNaN(this.highRange) == false) {
            yHighBottom = Math.min(y2 - lineWidth, y2 - (int) Math.round((this.highRange - this.m_floor) * dScale));
        }

        yHighBottom = Math.max(yHighBottom, rect.y);

        int yLowTop = 0;
        if (Double.isNaN(this.lowRange) == false) {
            yLowTop = Math.max(rect.y + lineWidth, y2 - (int) Math.round((this.lowRange - this.m_floor) * dScale));
        }

        yLowTop = Math.min(yLowTop, y2);

        int xAvgLabel = x2 - m_metricsLabel.stringWidth(Chart.AVG) - 3;
        int yAvgLabel = yAvgLine - 3;
        Rectangle avgLabel = new Rectangle();
        if (this.showAverage == true) {
            avgLabel.setRect(xAvgLabel, yAvgLabel, m_metricsLabel.stringWidth(Chart.AVG), m_metricsLabel.getHeight());
        }

        int xBaselineLabel = x2 - m_metricsLabel.stringWidth(Chart.BASELINE) - 4;
        int yBaselineLabel = yBaseLine - 3;
        Rectangle baselineLabel = new Rectangle();
        if (this.showBaseline == true) {
            baselineLabel.setRect(xBaselineLabel, yBaselineLabel, m_metricsLabel.stringWidth(Chart.BASELINE),
                m_metricsLabel.getHeight());
        }

        int xLowLabel = x2 - m_metricsLabel.stringWidth(Chart.LOW) - 4;
        int yLowLabel = yLowLine - 3;
        Rectangle lowLabel = new Rectangle();
        if (this.showLow == true) {
            lowLabel.setRect(xLowLabel, yLowLabel, m_metricsLabel.stringWidth(Chart.LOW), m_metricsLabel.getHeight());
        }

        int xPeakLabel = x2 - m_metricsLabel.stringWidth(Chart.PEAK) - 4;
        int yPeakLabel = yPeakLine - 3;
        Rectangle peakLabel = new Rectangle();
        if (this.showPeak == true) {
            peakLabel.setRect(xPeakLabel, yPeakLabel, m_metricsLabel.stringWidth(Chart.PEAK), m_metricsLabel
                .getHeight());
        }

        //////////////////////////////////////////////////////////
        // Draw the Value (Y) Legend

        if (this.showTopLegend == true) {
            g.drawYLegendString(this.getValueLegend());
        }

        //////////////////////////////////////////////////////////
        // Draw the value (Y) axis cross lines and labels

        String[] labels = MeasurementConverter.formatToSignifantPrecision(m_adRangeMarks, m_fmtUnits, true);
        int[] lines = new int[this.m_adRangeMarks.length];

        for (int i = 0; i < lines.length; i++) {
            lines[i] = rect.y + (int) Math.round((this.m_adRangeMarks[i] - this.m_floor) * dScale);
        }

        g.drawXLines(lines, labels, true);

        //////////////////////////////////////////////////////////
        // Draw the high range and low range

        boolean bHighLow = false;
        int cxGuide = lineWidth * 3;
        int xGuide = rect.x - cxGuide;

        if ((this.showHighRange == true) && (Double.isNaN(this.highRange) == false)
            && (yHighBottom > (rect.y + lineWidth))) {
            graph.setColor(this.highRangeColor);
            graph.fillRect(rect.x + lineWidth, rect.y + lineWidth, rect.width - lineWidth, yHighBottom - rect.y);

            graph.setColor(DANGER_COLOR);
            graph.fillRect(xGuide, rect.y + lineWidth, cxGuide, yHighBottom - rect.y);

            bHighLow = true;
        }

        if ((this.showLowRange == true) && (Double.isNaN(this.lowRange) == false) && (yLowTop < (y2 - lineWidth))) {
            graph.setColor(this.lowRangeColor);
            graph.fillRect(rect.x + lineWidth, yLowTop, rect.width - lineWidth, y2 - yLowTop);

            graph.setColor(DANGER_COLOR);
            graph.fillRect(xGuide, yLowTop, cxGuide, y2 - yLowTop);

            bHighLow = true;
        }

        if (bHighLow == true) {
            if (this.showHighRange == false) {
                yHighBottom = rect.y + lineWidth;
            } else {
                yHighBottom++;
                if (this.showLowRange == false) {
                    yLowTop = y2;
                }
            }

            graph.setColor(GOOD_COLOR);
            graph.fillRect(xGuide, yHighBottom, cxGuide, yLowTop - yHighBottom);
        }

        //////////////////////////////////////////////////////////
        // Draw the unit (X) axis tick marks and labels

        lines = this.getXPoints(g, rect);
        g.drawYLines(lines, this.getXLabels(), false, xLabelsSkip);

        //////////////////////////////////////////////////////////
        // Draw Events

        if (this.showEvents) {
            if (this.getDataSetCount() == 1) {
                EventPointCollection collEvts = this.getEventPoints();
                if (collEvts.size() > 0) {
                    int[] evtDataPts = getDataPointEventIndexes(0);

                    g.graphics.setColor(DEFAULT_COLORS[0]);

                    for (int i = 0; i < evtDataPts.length; i++) {
                        if (evtDataPts[i] == -1) {
                            continue;
                        }

                        Event evt = collEvts.get(i);
                        g.drawEvent(evt.getId(), lines[evtDataPts[i]], y2 + ChartGraphics.HALF_EVENT_HEIGHT
                            + this.lineWidth);
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////
        // Draw the Bottom Legend

        if (this.showBottomLegend == true) {
            g.drawXLegendString(this.getUnitLegend());
        }

        //////////////////////////////////////////////////////////
        // Draw the Peak, Avg and Low Lines

        graph.setFont(this.font);

        int xLast = 0;

        if (this.showLow == true) {
            graph.setColor(this.lowLineColor);
            graph.drawLine(this.xVertMarks, yLowLine, this.x2VertMarks, yLowLine);
            graph.drawString(Chart.LOW, xLowLabel, yLowLabel);

            xLast = xLowLabel;
        }

        if (this.showAverage == true) {
            if (avgLabel.intersects(lowLabel) == true) {
                xAvgLabel = xLast - this.m_metricsLabel.stringWidth(Chart.AVG) - this.m_metricsLabel.charWidth('W');
            }

            graph.setColor(this.averageLineColor);
            graph.drawLine(xVertMarks, yAvgLine, x2VertMarks, yAvgLine);
            graph.drawString(Chart.AVG, xAvgLabel, yAvgLabel);

            xLast = Math.min(xLast, xAvgLabel);
        }

        if (this.showPeak == true) {
            if ((peakLabel.intersects(lowLabel) == true) || (peakLabel.intersects(avgLabel) == true)) {
                xPeakLabel = xLast - this.m_metricsLabel.stringWidth(Chart.PEAK) - this.m_metricsLabel.charWidth('W');
            }

            graph.setColor(this.peakLineColor);
            graph.drawLine(xVertMarks, yPeakLine, x2VertMarks, yPeakLine);
            graph.drawString(Chart.PEAK, xPeakLabel, yPeakLabel);

            xLast = Math.min(xLast, xPeakLabel);
        }

        if ((this.showBaseline == true) && (yBaseLine > rect.y) && (yBaseLine < y2)) {
            if ((baselineLabel.intersects(lowLabel) == true) || (baselineLabel.intersects(avgLabel) == true)
                || (baselineLabel.intersects(peakLabel) == true)) {
                xBaselineLabel = xLast - this.m_metricsLabel.stringWidth(Chart.BASELINE)
                    - this.m_metricsLabel.charWidth('W');
            }

            graph.setColor(this.baselineColor);
            graph.drawLine(xVertMarks, yBaseLine, x2VertMarks, yBaseLine);
            graph.drawString(Chart.BASELINE, xBaselineLabel, yBaselineLabel);
        }

        ///////////////////////////////////////////////////////////
        // Paint the chart interior

        if (this.showValues == true) {
            this.paint(g, rect);
        }

        return rect;
    }

    protected void paint(ChartGraphics graph, Rectangle rect) {
        // Subclasses will take care of the painting
    }

    protected int[] getDataPointEventIndexes(int dataSetNumber) {
        DataPointCollection datapts = this.getDataPoints(dataSetNumber);
        EventPointCollection collEvts = this.getEventPoints(dataSetNumber);
        Iterator<Event> iterEvts = collEvts.iterator();
        int[] tmp = new int[collEvts.size()];
        int cActual = 0;

        for (int i = 0; iterEvts.hasNext() == true; i++) {
            Event evt = iterEvts.next();

            int index = this.findDataPointIndex(evt.getTimestamp().getTime(), datapts);

            //            if(index == -1)
            //                continue;

            tmp[i] = index;
            cActual++;
        }

        /*
         * if(cActual == 0) return null;
         */

        // Compact the array and return it
        int[] res = new int[cActual];
        for (int i = 0; i < res.length; i++) {
            res[i] = tmp[i];
        }

        return res;
    }

    protected Point getDataPoint(Rectangle rect, int datapoint) {
        return this.getDataPoint(rect, datapoint, this.getDataPoints());
    }

    protected Point getDataPoint(Rectangle rect, int datapoint, DataPointCollection coll) {
        Point ptResult = super.getDataPoint(rect.height, rect.width, datapoint, coll);

        if (ptResult != null) {
            this.adjustBorders(ptResult);
        }

        return ptResult;
    }

    protected void setTimeScale(long scale) {
        this.m_timeScale = scale;
    }

    protected int findDataPointIndex(long timestamp, DataPointCollection coll) {
        int collSize = coll.size();

        if (collSize == 0) {
            return -1;
        }

        long first = ((IDisplayDataPoint) coll.get(0)).getTimestamp();

        if (collSize == 1) {
            return ((first == timestamp) ? 0 : -1);
        }

        long second = ((IDisplayDataPoint) coll.get(1)).getTimestamp();
        long interval = second - first;
        long prev = first - interval;
        int index;

        for (index = 0; index < collSize; index++) {
            IDisplayDataPoint datapt = (IDisplayDataPoint) coll.get(index);

            // Break if we find what we're looking for
            if ((timestamp > prev) && (timestamp <= datapt.getTimestamp())) {
                break;
            }

            // Prepare for the next loop
            prev = datapt.getTimestamp();
        }

        // Return a index for an actual point
        return ((index == collSize) ? -1 : index);
    }
}