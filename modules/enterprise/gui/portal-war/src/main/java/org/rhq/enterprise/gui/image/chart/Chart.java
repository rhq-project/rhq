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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.image.WebImage;
import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;
import org.rhq.enterprise.gui.image.data.IHighLowDataPoint;
import org.rhq.enterprise.gui.image.data.IStackedDataPoint;

/**
 * Chart is an abstract base class for the Covalent charting library. Chart uses Java AWT drawing to draw charts
 * dynamically into a Java Image which can be then be used to display the chart in an application. The most common use
 * of the charting library is to draw the chart image and then convert the image to a GIF to send to a Web Browser.
 * Chart is responsible for all of the background drawing of the Chart the Chart frame, text labels, data point crossing
 * lines, etc. Sublcasses of Chart draw on top of the image produce by the Chart class to draw the actual lines,
 * columns, etc. that represent the data points of the chart. To create a chart you instantiate a subclass of Chart
 * (e.g., LineChart, ColumnChart, etc.), call getData() to retrieve a collection, populate the collection and then call
 * Chart.getImage() to get a java.awt.Image object that represents the drawn chart. The chart class is very configurable
 * through get/set methods including configuring things like the text for labels, fonts, line colors, etc. After
 * changing one or more options you call Chart.getImage() again to get a new image based on your options. The chart
 * should have at least three datums in its Datum collection to display without errors.
 *
 * @see java.awt.Image
 */
public abstract class Chart extends WebImage {
    private Log log = LogFactory.getLog(Chart.class.getName());

    ////////////////////////////////////////////////
    // Static Variables

    public static final String EMPTY_STRING = "";

    protected static final String ARG_MUST_BE_ZERO_OR_GREATER = "Argument value must be zero or greater";

    protected static final String AVG = "Average";
    protected static final String BASELINE = "Baseline";
    protected static final String LOW = "Low";
    protected static final String PEAK = "Peak";

    protected static final String DEFAULT_UNIT_LEGEND = "TIME";
    protected static final String DEFAULT_VALUE_LEGEND = "VALUE";
    private static final String NO_DATA = "No Metric Data Available";

    private static final int DEFAULT_LINE_WIDTH = 1;
    private static final int DEFAULT_VALUE_INDENT = 0;
    private static final int DEFAULT_VALUE_LINES = 11;
    private static final int DEFAULT_TEXT_WHITESPACE = 3;
    private static final int DEFAULT_TICK_MARK_HEIGHT = 6;

    protected static final int VARIABLE_HEIGHT = Integer.MIN_VALUE;
    protected static final int VARIABLE_WIDTH = Integer.MIN_VALUE;

    private static final Color DEFAULT_FRAME_COLOR = new Color(0xE6, 0xE5, 0xE5); //new Color(0xCC, 0xCC, 0xCC);
    private static final Color DEFAULT_LEGEND_TEXT_COLOR = Color.BLACK;
    private static final Color DEFAULT_X_LINE_COLOR = new Color(0xE6, 0xE5, 0xE5);
    private static final Color DEFAULT_TEXT_COLOR = new Color(0x80, 0x80, 0x80);

    private static final Color DEFAULT_AVERAGE_COLOR = new Color(0x8C, 0xBF, 0x73);
    private static final Color DEFAULT_BASELINE_COLOR = new Color(0xBF, 0xB2, 0x73);
    private static final Color DEFAULT_LOW_COLOR = new Color(0x73, 0xBF, 0xB3);
    private static final Color DEFAULT_PEAK_COLOR = new Color(0xD9, 0x98, 0x98);

    private static final Color DEFAULT_HIGH_RANGE_COLOR = new Color(0xF7, 0xEB, 0xEB);
    private static final Color DEFAULT_LOW_RANGE_COLOR = new Color(0xEB, 0xF7, 0xF6);

    protected static final Font DEFAULT_LABEL_FONT = new Font("Helvetica", Font.PLAIN, 10);
    protected static final Font DEFAULT_LEGEND_PLAIN = new Font("Helvetica", Font.PLAIN, 11);
    protected static final Font DEFAULT_LEGEND_FONT = new Font("Helvetica", Font.BOLD, 11);

    protected FontMetrics m_metricsLabel;
    protected FontMetrics m_metricsLegend;

    ////////////////////////////////////////////////
    // Instance Variables

    private String m_strUnitLegend = DEFAULT_UNIT_LEGEND;
    private String m_strValueLegend = DEFAULT_VALUE_LEGEND;

    private boolean m_useAbsTimeLabels = false;
    private SmartLabelMaker m_smartLabelMaker = null;

    private Color m_clrFrame = DEFAULT_FRAME_COLOR;
    private Color m_clrLegendText = DEFAULT_LEGEND_TEXT_COLOR;

    protected MeasurementUnits m_fmtUnits;

    protected double m_dAvgValue = 0;
    protected double m_dLowValue = Double.POSITIVE_INFINITY;
    protected double m_dPeakValue = Double.NEGATIVE_INFINITY;

    protected double[] m_adRangeMarks;
    protected double m_floor;
    protected boolean m_bNoData;

    private String m_strTitle = EMPTY_STRING;

    private ArrayList<DataPointCollection> m_collDataPointColl = new ArrayList<DataPointCollection>(1);
    private ArrayList<EventPointCollection> m_collEvtPointColl = new ArrayList<EventPointCollection>(1);

    private String m_strNoData = NO_DATA;

    public int yTopLegend;
    public int yBottomLegend;
    public int xVertLegend;
    public int yHorzLabels;
    public int x2VertLabels;
    public int xRLabel;
    public int xVertMarks;
    public int x2VertMarks;
    public int xLabelsSkip = 4; // Default to label every 4 ticks

    ////////////////////////////////////////////////
    // Property Variables

    /**
     * Color of the average data point horizontal line.
     */
    public Color averageLineColor = DEFAULT_AVERAGE_COLOR;

    /**
     * Color of the baseline horizontal line.
     */
    public Color baselineColor = DEFAULT_BASELINE_COLOR;

    /**
     * Top number for the left axis of the chart.
     */
    public double ceiling = 0;

    /**
     * Retrieves the chart's interior background color.
     */
    public Color chartColor = Color.WHITE;

    /**
     * Bottom number for the left axis of the chart.
     */
    public double floor = 0;

    /**
     * Baseline in the chart.
     */
    public double baseline = 0;

    /**
     * High range in the chart.
     */
    public double highRange = Double.NaN;

    /**
     * High range color in the chart.
     */
    public Color highRangeColor = DEFAULT_HIGH_RANGE_COLOR;

    /**
     * Font that is used to draw the legend for the chart's X and Y axis.
     */
    public Font legendFont = DEFAULT_LEGEND_FONT;

    /**
     * Color that is used to draw the legend text for the chart's X and Y axis.
     */
    public Color legendTextColor = DEFAULT_LEGEND_TEXT_COLOR;

    /**
     * Width of lines in the chart.
     */
    public int lineWidth = DEFAULT_LINE_WIDTH;

    /**
     * Retrieves the color of the low data point horizontal line.
     */
    public Color lowLineColor = DEFAULT_LOW_COLOR;

    /**
     * Low range in the chart.
     */
    public double lowRange = Double.NaN;

    /**
     * High range color in the chart.
     */
    public Color lowRangeColor = DEFAULT_LOW_RANGE_COLOR;

    /**
     * Color of the peak data point horizontal line.
     */
    public Color peakLineColor = DEFAULT_PEAK_COLOR;

    /**
     * Sets a fixed size for the label width on the right vertical axis.
     */
    public int rightLabelWidth = -1;

    /**
     * Calculates and shows an Average line in the chart.
     */
    public boolean showAverage = false;

    /**
     * Shows a Baseline in the chart.
     */
    public boolean showBaseline = false;

    /**
     * Shows labels on the X axis bottom side.
     */
    public boolean showBottomLabels = true;

    /**
     * Shows a top legend.
     */
    public boolean showBottomLegend = true;

    /**
     * Shows events plotted on a chart.
     */
    public boolean showEvents = true;

    /**
     * Show full labels on the bottom x axis.
     */
    public boolean showFullLabels = false;

    /**
     * Shows a shaded high range in the chart.
     */
    public boolean showHighRange = false;

    /**
     * Shows labels on the Y axis left side.
     */
    public boolean showLeftLabels = true;

    /**
     * Shows a Left legend.
     */
    public boolean showLeftLegend = true;

    /**
     * Calculates and shows a Low line in the chart.
     */
    public boolean showLow = false;

    /**
     * Shows a shaded low range in the chart.
     */
    public boolean showLowRange = false;

    /**
     * Calculates and shows a peak line in the chart.
     */
    public boolean showPeak = false;

    /**
     * Shows labels on the Y axis right side.
     */
    public boolean showRightLabels = true;

    /**
     * Shows a Right legend.
     */
    public boolean showRightLegend = false;

    /**
     * Shows labels on the X axis top side.
     */
    public boolean showTopLabels = false;

    /**
     * Shows a top legend.
     */
    public boolean showTopLegend = false;

    /**
     * Shows vertical crossing lines at each data point on the y axis.
     */
    public boolean showUnitLines = false;

    /**
     * Shows horizontal crossing lines at each data point on the x axis.
     */
    public boolean showValueLines = false;

    /**
     * Shows values.
     */
    public boolean showValues = true;

    /**
     * The width of the white space between tick marks and the label text.
     */
    public int textWhitespace = DEFAULT_TEXT_WHITESPACE;

    /**
     * The height, in pixels, of the tick mark lines that extend outside the chart's border. The tick marks for data
     * points that don't have a label are drawn at half this height.
     */
    public int tickMarkHeight = DEFAULT_TICK_MARK_HEIGHT;

    /**
     * Number of pixels to indent the value axis.
     */
    public int valueIndent = DEFAULT_VALUE_INDENT;

    /**
     * Number of value lines that the chart draws at data points along the chart's value axis. For vertical chart's this
     * is the Y axis. For horizontal charts this is the chart's X axis. This number includes the chart's top and bottom
     * border.
     */
    public int valueLines = DEFAULT_VALUE_LINES;

    /**
     * Color of the border lines, horizontal lines and tick marks
     */
    public Color xLineColor = DEFAULT_X_LINE_COLOR;

    /**
     * Amount to offset the chart on the X axis in pixels.
     */
    public int xOffset = 0;

    /**
     * Amount to offset the chart on the Y axis in pixels.
     */
    public int yOffset = 0;

    ////////////////////////////////////////////////
    // Constructors

    /**
     * Constructs a Chart class with a default width of 755 pixels and a default height of 300 pixels.
     */
    protected Chart() {
        this(1);
    }

    /**
     * Constructs a Chart class with a default width of 755 pixels and a default height of 300 pixels.
     *
     * @param charts The number of charts to display.
     */
    protected Chart(int charts) {
        this(WebImage.DEFAULT_WIDTH, WebImage.DEFAULT_HEIGHT, charts);
    }

    /**
     * Constructs a Chart class with a specified width and height.
     *
     * @param width  The width of the chart in pixels.
     * @param height The height of the chart in pixels.
     */
    protected Chart(int width, int height) {
        this(width, height, 1);
    }

    /**
     * Constructs a Chart class with a specified width and height.
     *
     * @param width  The width of the chart in pixels.
     * @param height The height of the chart in pixels.
     * @param charts The number of charts to display.
     */
    protected Chart(int width, int height, int charts) {
        super(width, height);
        this.shadowWidth = 0;
        this.textColor = DEFAULT_TEXT_COLOR;

        if (charts <= 0) {
            charts = 1;
        }

        this.setNumberDataSets(charts);

        this.initFonts();
    }

    ////////////////////////////////////////////////
    // Methods

    @Override
    protected void draw(Graphics2D g) {
        super.draw(g);
        this.draw(new ChartGraphics(this, g));
    }

    protected Rectangle draw(ChartGraphics g) {
        /////////////////////////////////////////////////////////////
        // Draw the chart outline and fills the interior

        // Fill chart background
        Rectangle rect = this.getInteriorRectangle(g);

        if (this.m_bNoData == false) {
            // Fill chart interior
            g.graphics.setColor(this.m_clrFrame);
            g.graphics.drawRect(rect.x, rect.y, rect.width, rect.height);

            g.graphics.setColor(this.chartColor);
            g.graphics.fillRect(rect.x + this.lineWidth, rect.y + this.lineWidth, rect.width - (this.lineWidth),
                rect.height - (this.lineWidth));
        } else {
            FontMetrics metrics = g.graphics.getFontMetrics(this.legendFont);

            g.graphics.setColor(this.m_clrLegendText);
            g.graphics.setFont(DEFAULT_LEGEND_PLAIN);
            g.graphics.drawString(this.m_strNoData, (this.width / 2) - (metrics.stringWidth(this.m_strNoData) / 2),
                this.yOffset + (this.height / 2) + (metrics.getAscent() / 2));
        }

        return rect;
    }

    protected void calc(Graphics2D g) {
        //FormattedNumber[] fmtValueLabels = UnitsFormat.formatSame(m_adRangeMarks, m_fmtType, m_fmtScale);

        Rectangle rect = new Rectangle(this.xOffset, this.yOffset, this.width, this.height);

        // Calculate the X axis
        rect.x += this.leftBorder;

        if ((this.showHighRange == true) || (this.showLowRange == true)) {
            rect.x += this.lineWidth;
        }

        int xVertLabels = (this.showLeftLegend == true) ? (rect.x + m_metricsLegend.charWidth('V') + (this.textWhitespace * 2))
            : 0;
        int cxLabels = (this.rightLabelWidth < 0) ? this.getYLabelWidth(g) : this.rightLabelWidth;

        if (this.showLeftLabels == true) {
            x2VertLabels = xVertLabels + cxLabels;
            xVertMarks = x2VertLabels + this.textWhitespace;
            rect.x = xVertMarks + this.tickMarkHeight;
        }

        rect.width = rect.width - rect.x - this.rightBorder;

        if (this.showRightLabels == true) {
            rect.width -= this.tickMarkHeight;
            rect.width = rect.width - cxLabels;
        } else {
            // We need to bring the right frame in a little if there's no right
            // label, but there are top or bottom labels.
            String[] labels = this.getXLabels();
            if ((labels != null) && (labels.length > 1)) {
                rect.width -= (this.m_metricsLabel.stringWidth(labels[labels.length - 1]) / 2);
            }
        }

        // Adjust the interior of the rectangle
        Rectangle rectAdj = this.adjustRectangle(g, rect);

        x2VertMarks = rect.x + rect.width + (this.lineWidth * 2);
        if (this.showRightLabels == true) {
            x2VertMarks += this.tickMarkHeight;
        }

        xRLabel = x2VertMarks + this.textWhitespace;

        int yVertLegend = rect.y + (this.height / 2)
            - (m_metricsLegend.getAscent() * this.m_strValueLegend.length() / 2);

        // Calculate the Y axis
        rect.y = rect.y + this.topBorder;

        if (this.showTopLegend == true) {
            rect.y += m_metricsLegend.getAscent();
            yTopLegend = rect.y;
            rect.y += this.textWhitespace;
        }

        if (this.showTopLabels == true) {
            rect.y += (this.getXLabelHeight() + this.tickMarkHeight);
        }

        if ((this.showTopLegend == false) && (this.showTopLabels == false)) {
            rect.y += (m_metricsLabel.getAscent() / 2);
        }

        yBottomLegend = this.yOffset + (rect.height - this.bottomBorder);

        int yHorzMarks;
        if ((this.showBottomLegend == false) && (this.showBottomLabels == false)) {
            yHorzLabels = yBottomLegend - (m_metricsLabel.getAscent() / 2);
            yHorzMarks = yHorzLabels;
        } else {
            yHorzLabels = yBottomLegend - ((this.showBottomLegend == true) ? this.getXLabelHeight() : 0);
            yHorzMarks = yHorzLabels
                - ((this.showBottomLabels == true) ? (this.m_metricsLabel.getAscent() + this.tickMarkHeight) : 0);
        }

        int y2Rect = yHorzMarks - this.lineWidth;
        rect.height = y2Rect - rect.y;

        int xHorzLegend = (this.width / 2) - (m_metricsLegend.stringWidth(this.getUnitLegend()) / 2);
        int xHorzMarks = rect.x + this.valueIndent;
    }

    /**
     * Give the child class an opportunity to change the size of the interior rectangle. This is done to make the tick
     * marks fit symetrically in the chart rectangle.
     */
    protected Rectangle adjustRectangle(Graphics2D g, Rectangle rect) {
        return rect;
    }

    /**
     * Calculates the high, low and average values of the chart data set.
     */
    protected void calcRanges() {
        int cActualVals = 0;
        int index;
        double unit;
        double topRange;

        // Calculate Top and Bottom Range
        double dVal;

        // ///////////////////////////////////////////////////////////////
        // Iterator through the DataSets to calculate the avg, low & peak
        Iterator<DataPointCollection> iterDataSet = this.m_collDataPointColl.iterator();
        while (iterDataSet.hasNext() == true) {
            // Each DataSet has a collection of data points.
            Iterator iterDataPt = (iterDataSet.next()).iterator();

            while (iterDataPt.hasNext() == true) {
                IDisplayDataPoint datapt = (IDisplayDataPoint) iterDataPt.next();

                // Skip NaN
                if (Double.isNaN(datapt.getValue()) == true) {
                    continue;
                }

                if (checkHighLow()) {
                    if (datapt instanceof IHighLowDataPoint) {
                        IHighLowDataPoint hlPt = (IHighLowDataPoint) datapt;
                        if (!Double.isNaN(hlPt.getLowValue())) {
                            this.m_dLowValue = Math.min(this.m_dLowValue, hlPt.getLowValue());
                        }

                        if (!Double.isNaN(hlPt.getHighValue())) {
                            this.m_dPeakValue = Math.max(this.m_dPeakValue, hlPt.getHighValue());
                        }
                    }
                }

                double[] vals = (datapt instanceof IStackedDataPoint) ? ((IStackedDataPoint) datapt).getValues()
                    : new double[] { datapt.getValue() };

                for (int i = 0; i < vals.length; i++) {
                    dVal = vals[i];

                    // Accumulate a total and number of points that make up the
                    // total
                    this.m_dAvgValue += dVal;
                    cActualVals++;

                    // Set the low
                    this.m_dLowValue = Math.min(this.m_dLowValue, dVal);

                    // Set the high
                    this.m_dPeakValue = Math.max(this.m_dPeakValue, dVal);
                }
            }
        }

        // Set the NoData flag and exit if we don't have any
        this.m_bNoData = (cActualVals == 0);
        if (this.m_bNoData == true) {
            this.m_adRangeMarks = new double[0];
            return;
        }

        // Caclulate the average
        this.m_dAvgValue /= cActualVals;

        ///////////////////////////////////////////////////////////////////
        // Calculate the value axis units (X for horiz, Y for vertical)

        if (this.ceiling == this.floor) {
            double range = (this.m_dPeakValue - this.m_dLowValue);

            if (range != 0) {
                // Buffer the top and bottom by 10%
                double buffer = range * .1;
                double topbuf = buffer / 2;
                double botbuf = ((this.m_dLowValue - topbuf) < 0) ? m_dLowValue : topbuf;

                range += (topbuf + botbuf);
                unit = range / (this.valueLines - 1);

                double tmp = range / unit;

                this.m_floor = this.m_dLowValue - botbuf;
                topRange = this.m_dPeakValue + topbuf;
            } else {
                // If the peak value and low value are the same create a range
                // that puts the charted value 1/2 up the chart
                this.m_floor = 0;

                if (this.m_dPeakValue == 0) {
                    topRange = (this.valueLines - 1);
                    if (this.m_fmtUnits.getFamily() == MeasurementUnits.Family.DURATION) {
                        topRange *= 1000;
                    }
                } else {
                    topRange = this.m_dPeakValue * 2;
                }

                unit = (topRange - this.m_floor) / (this.valueLines - 1);
            }
        } else {
            // We just accept the floor and ceiling if they are preset by
            // whoever instantiated the chart
            this.m_floor = this.floor;
            unit = (this.ceiling - this.floor) / (this.valueLines - 1);
        }

        ////////////////////////////////////////////////////////////////////
        // Calculate Cross Line Values

        this.m_adRangeMarks = new double[this.valueLines];

        for (int i = 0; i < this.valueLines; i++) {
            this.m_adRangeMarks[i] = this.m_floor + (i * unit);
        }
    }

    protected int calcVariableHeight() {
        return this.height;
    }

    protected int calcVariableWidth() {
        return this.width;
    }

    /**
     * Calculates the label width of the horizontal axis of the chart.
     *
     * @param  graph The java.awt.Graphics context to draw into.
     *
     * @return The width of the widest label on the X (horizontal) axis.
     */
    protected int getXLabelWidth() {
        int iWidth;
        int iMaxWidth = 0;

        Iterator iter = this.getDataPoints().iterator();

        while (iter.hasNext()) {
            iWidth = m_metricsLabel.stringWidth(((IDisplayDataPoint) iter.next()).getLabel());

            if (iWidth > iMaxWidth) {
                iMaxWidth = iWidth;
            }
        }

        return iMaxWidth;
    }

    protected int getXLegendHeight() {
        int height = m_metricsLegend.getAscent() + (this.textWhitespace * 2);
        int result = 0;

        if (this.showTopLegend == true) {
            result += height;
        }

        if (this.showBottomLegend == true) {
            result += height;
        }

        return result;
    }

    /**
     * Calculates the label width of the vertical axis of the chart.
     *
     * @param  graph The ChartGraphics context to draw into.
     *
     * @return The width of the widest label on the Y (vertical) axis.
     */
    protected abstract int getYLabelWidth(Graphics2D g);

    protected abstract String[] getXLabels();

    protected int getXLabelHeight() {
        int result = 0;

        if ((this.showTopLabels == true) || (this.showBottomLabels == true)) {
            String[] labels = this.getXLabels();
            int labelHeight = 0;

            if ((labels != null) && (labels.length > 0)) {
                labelHeight = this.tickMarkHeight
                    + ((labels != null) ? ChartGraphics.getStringHeight(labels[0], this.m_metricsLabel)
                        : this.m_metricsLabel.getAscent());
            } else {
                labelHeight = this.tickMarkHeight + m_metricsLabel.getAscent();
            }

            if (this.showTopLabels == true) {
                result += labelHeight;
            }

            if (this.showBottomLabels == true) {
                result += labelHeight;
            }
        }

        return result;
    }

    protected abstract Rectangle getInteriorRectangle(ChartGraphics g);

    protected int getExteriorHeight() {
        int cyLabel = this.getXLabelHeight();
        int cyLegend = this.getXLegendHeight();

        int cyBuf = (m_metricsLabel.getAscent() / 2);

        // Provide a little extra space if there is no bottom label or legend
        if ((this.showBottomLabels == false) && (this.showBottomLegend == false)) {
            cyBuf += (m_metricsLabel.getAscent() / 2);
        }

        return (this.topBorder + cyLegend + cyLabel + cyBuf + this.bottomBorder);
    }

    protected Collection initData(Collection coll) {
        return coll;
    }

    protected void initFonts() {
        // Initialize FontMetrics
        Image img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = (Graphics2D) img.getGraphics();
        m_metricsLabel = g.getFontMetrics(this.font);
        m_metricsLegend = g.getFontMetrics(this.legendFont);
        g.dispose();
    }

    @Override
    protected void preInit() {
        // Create the ChartGraphics
        Collection<DataPointCollection> coll = this.initData(this.m_collDataPointColl);
        if (coll instanceof ArrayList) {
            this.m_collDataPointColl = (ArrayList<DataPointCollection>) coll;
        } else {
            throw new ClassCastException("initData() must return a collection of type ArrayList.");
        }

        // Set the variable width and height
        if (this.height == Chart.VARIABLE_HEIGHT) {
            this.height = this.calcVariableHeight();
        }

        if (this.width == Chart.VARIABLE_WIDTH) {
            this.width = this.calcVariableWidth();
        }
    }

    @Override
    protected void postInit(Graphics2D g) {
        // Calculate the base part of the image
        this.calcRanges();
        this.calc(g);
    }

    /**
     * Retrieves the value and unit coordinate for a data point in the chart.
     *
     * @param  valuePixels An int that specifies the size of the value axis in pixels.
     * @param  unitPixels  An int that specifies the size of the unit axis in pixels.
     * @param  datapoint   An int that specifies the zero-based index of the datum to calculate coordinates for.
     * @param  collection  The DataPointCollection the datapoint is located in.
     *
     * @return A java.awt.Point object that contains the X and Y coordinate.
     *
     * @see    java.awt.Rectangle
     */
    protected Point getDataPoint(int valuePixels, int unitPixels, int datapoint, DataPointCollection coll) {
        int cDataPts = coll.size();
        double dVal = ((IDisplayDataPoint) coll.get(datapoint)).getValue();
        return this.getDisplayPoint(valuePixels, unitPixels, cDataPts, dVal, datapoint);
    }

    /**
     * Retrieves the value and unit coordinate for a value on the x and y axis in the chart.
     *
     * @param  valuePixels An int that specifies the size of the value axis in pixels.
     * @param  unitPixels  An int that specifies the size of the unit axis in pixels.
     * @param  unitPoints  Number of ticks on the unit axis.
     * @param  value       A double that specifies the value to get coordinates for.
     * @param  unitIndex   Ticks on the unit axis to get coordinates for.
     *
     * @return A java.awt.Point object that contains the X and Y coordinate.
     */
    protected Point getDisplayPoint(int valuePixels, int unitPixels, int unitPoints, double value, int unitIndex) {
        int iSpread = unitPixels - (this.valueIndent * 2);
        iSpread = (unitPoints > 1) ? (iSpread / (unitPoints - 1)) : iSpread;

        if (Double.isNaN(value) == true) {
            return null;
        }

        if (this.ceiling != this.floor) {
            if (value < this.floor) {
                // This is a problem, set value to the floor
                log.error("Data point value (" + value + ") lower than floor (" + this.floor + ")");
                value = this.floor;
            } else if (value > this.ceiling) {
                // This is a problem, set value to the ceiling
                log.error("Data point value (" + value + ") higher than ceiling (" + this.ceiling + ")");
                value = this.ceiling;
            }
        }

        // X = unitPixels, Y = valuePixels
        int x = this.valueIndent + (iSpread * unitIndex);
        int y = valuePixels - (int) Math.round((value - this.m_floor) * this.scale(valuePixels));

        if (x == 0) {
            x++;
        } else if (x == (unitPixels - this.lineWidth)) {
            x--;
        }

        if (y == 0) {
            y++;
        } else if (y == valuePixels) {
            y--;
        }

        return new Point(x, y);
    }

    protected String getUnitLabel(IDisplayDataPoint data) {
        return data.getLabel();
    }

    protected boolean hasData() {
        return (this.m_bNoData == false);
    }

    /**
     * Calculates the scale of the graph data points. This is the number of pixels per data point.
     *
     * @param  height The height of the rectangle to calculate the vertical scale for.
     *
     * @return A floating point value that specifies the scale multiplier of the vertical axis.
     */
    protected double scale(int height) {
        double result = (height / (this.m_adRangeMarks[this.m_adRangeMarks.length - 1] - this.m_adRangeMarks[0]));
        return result;
    }

    protected Class<DataPointCollection> getDataCollectionClass() {
        return DataPointCollection.class;
    }

    protected boolean checkHighLow() {
        return false;
    }

    //    protected IndexColorModel getIndexColorModel() {
    //        return (IndexColorModel)(new BufferedImage(
    //                   1, 1, BufferedImage.TYPE_BYTE_INDEXED)).getColorModel();
    //    }

    ////////////////////////////////////////////////
    // Properties

    /**
     * Retrieves the average value of the chart's data set.
     *
     * @return A floating point value that is the average value of the chart's data set.
     */
    public double getAverageValue() {
        return this.m_dAvgValue;
    }

    public String getNoDataString() {
        return this.m_strNoData;
    }

    public void setNoDataString(String s) {
        this.m_strNoData = (s == null) ? EMPTY_STRING : s;
    }

    /**
     * Retrieves the data set of the chart.
     *
     * @return A net.hyperic.chart.DataPointCollection object that contains the current data set points.
     *
     * @see    net.hyperic.chart.DataPointCollection
     */
    public DataPointCollection getDataPoints() {
        return this.getDataPoints(0);
    }

    public EventPointCollection getEventPoints() {
        return this.getEventPoints(0);
    }

    public DataPointCollection getDataPoints(int index) {
        return this.m_collDataPointColl.get(index);
    }

    public EventPointCollection getEventPoints(int index) {
        return this.m_collEvtPointColl.get(index);
    }

    public int getDataSetCount() {
        return this.m_collDataPointColl.size();
    }

    public Iterator<DataPointCollection> getDataSetIterator() {
        return this.m_collDataPointColl.iterator();
    }

    public Iterator<EventPointCollection> getEventSetIterator() {
        return this.m_collEvtPointColl.iterator();
    }

    public void setNumberDataSets(int number) {
        int delta = number - m_collDataPointColl.size();

        if (delta > 0) {
            try {
                for (int i = 0; i < delta; i++) {
                    m_collDataPointColl.add(this.getDataCollectionClass().newInstance());
                    m_collEvtPointColl.add(new EventPointCollection());
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        } else if (delta < 0) {
            for (int i = delta; i < 0; i++) {
                m_collDataPointColl.remove(m_collDataPointColl.size() - 1);
                m_collEvtPointColl.remove(m_collEvtPointColl.size() - 1);
            }
        }

        // Make sure we allways have at least one empty collection
        if (this.getDataSetCount() == 0) {
            try {
                m_collDataPointColl.add(getDataCollectionClass().newInstance());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public void setFormat(MeasurementUnits units) {
        this.m_fmtUnits = units;
    }

    public Rectangle getExteriorRectangle() {
        return new Rectangle(this.xOffset, this.yOffset, this.width, this.height);
    }

    /**
     * Retrieves the low value of the chart's data set.
     *
     * @return A floating point value that is the low value of the chart's data set.
     */
    public double getLowValue() {
        return this.m_dLowValue;
    }

    /**
     * Retrieves the peak value of the chart's data set.
     *
     * @return A floating point value that is the peak value of the chart's data set.
     */
    public double getPeakValue() {
        return this.m_dPeakValue;
    }

    /**
     * Get the chart's title.
     *
     * @return A string containing the chart's title.
     */
    public String getTitle() {
        return this.m_strTitle;
    }

    /**
     * Set the chart's title.
     *
     * @param title A string containing the chart's title.
     */
    public void setTitle(String title) {
        this.m_strTitle = ((title == null) ? Chart.EMPTY_STRING : title);
    }

    /**
     * Retrieves the legend for the chart's Unit axis. The default legend is "TIME".
     *
     * @return A java.lang.String object that contains the chart's Unit axis lagend.
     */
    public String getUnitLegend() {
        return this.m_strUnitLegend;
    }

    /**
     * Sets the legend for the chart's Unit axis.
     *
     * @param     label A java.lang.String object that contains the chart's Unit axis legend.
     *
     * @exception IllegalArgumentException If the legend parameter is null.
     */
    public void setUnitLegend(String legend) {
        this.m_strUnitLegend = ((legend == null) ? Chart.EMPTY_STRING : legend);
    }

    /**
     * Retrieves the legend for the chart's Value axis. The default legend is "VALUE".
     *
     * @return A java.lang.String object that contains the chart's Value axis legend.
     */
    public String getValueLegend() {
        return this.m_strValueLegend;
    }

    /**
     * Sets the legend for the chart's Value axis.
     *
     * @param     label A java.lang.String object that contains the chart's Value axis legend.
     *
     * @exception IllegalArgumentException If the legend parameter is null.
     */
    public void setValueLegend(String legend) {
        this.m_strValueLegend = ((legend == null) ? Chart.EMPTY_STRING : legend);
    }

    public void setAbsTimeLabels(boolean useAbsTimeLabels, long interval) {
        m_useAbsTimeLabels = useAbsTimeLabels;
        m_smartLabelMaker = new SmartLabelMaker(interval);
    }
}