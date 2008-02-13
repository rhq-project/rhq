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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import javax.imageio.ImageIO;
import org.rhq.enterprise.gui.image.data.IDataPoint;

public class AvailabilityReportChart extends Chart {
    private static int CIRCLE_SIZE = 11;

    private static BufferedImage GOOD_CIRCLE;
    private static BufferedImage DANGER_CIRCLE;
    private static BufferedImage UNKNOWN_CIRCLE;

    private static final int TEXT_HEIGHT = 11;
    private static final Font TEXT_FONT = new Font("Helvetica", Font.PLAIN, TEXT_HEIGHT);
    private static final FontMetrics TEXT_METRICS;

    private static final Color COLOR_TRANSPARENT = new Color(0, 0, 255); //,0);

    private static final String LARGEST_NUMBER = "999";

    private static final int TEXT_BUFFER = 2;
    private static final int SET_BUFFER = 5;

    private static final int CIRCLE_WITH_BUFFER_WIDTH;
    private static final int STANDARD_SET_WIDTH;
    private static final int IMAGE_WIDTH;

    static {
        // Get Font Metrics
        Image img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        java.awt.Graphics g = img.getGraphics();
        TEXT_METRICS = g.getFontMetrics(TEXT_FONT);
        g.dispose();

        // Load Images
        InputStream i;

        try {
            ClassLoader ldr = AvailabilityReportChart.class.getClassLoader();

            i = ldr.getResourceAsStream("images/icon_available_green.gif");
            GOOD_CIRCLE = ImageIO.read(i);
            i.close();

            i = ldr.getResourceAsStream("images/icon_available_red.gif");
            DANGER_CIRCLE = ImageIO.read(i);
            i.close();

            i = ldr.getResourceAsStream("images/icon_available_error.gif");
            UNKNOWN_CIRCLE = ImageIO.read(i);
            i.close();
        } catch (IOException e) {
            System.out.println(e);
        }

        CIRCLE_WITH_BUFFER_WIDTH = GOOD_CIRCLE.getWidth() + TEXT_BUFFER;
        STANDARD_SET_WIDTH = CIRCLE_WITH_BUFFER_WIDTH + TEXT_METRICS.stringWidth(LARGEST_NUMBER) + SET_BUFFER;
        IMAGE_WIDTH = (STANDARD_SET_WIDTH * 3) - SET_BUFFER;
    }

    public AvailabilityReportChart() {
        super(IMAGE_WIDTH, GOOD_CIRCLE.getHeight());

        this.setBorder(0);

        //this.useIndexColors = true;
        this.indexColors = true;
    }

    protected void init() {
        this.showLeftLabels = false;
        this.showBottomLabels = false;
        this.showLeftLegend = false;
        this.showTopLegend = false;
    }

    protected Rectangle draw(ChartGraphics g) {
        g.graphics.setFont(TEXT_FONT);

        int x = 0;
        int yCircle = 0;
        int y2Circle = CIRCLE_SIZE - 1;

        g.graphics.setPaint(COLOR_TRANSPARENT);
        g.graphics.fillRect(0, 0, this.width, this.height);

        DataPointCollection datapts = this.getDataPoints();
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(0);

        g.graphics.setColor(this.textColor);

        double val;
        String text;

        if (datapts.size() >= 1) {
            val = ((IDataPoint) datapts.get(0)).getValue();

            if (val > 0) {
                text = fmt.format(val);
                g.graphics.drawImage(GOOD_CIRCLE, 0, yCircle, COLOR_TRANSPARENT, null);
                g.graphics.drawString(text, CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
            }
        }

        if (datapts.size() >= 2) {
            val = ((IDataPoint) datapts.get(1)).getValue();

            if (val > 0) {
                text = fmt.format(val);
                g.graphics.drawImage(DANGER_CIRCLE, STANDARD_SET_WIDTH, yCircle, COLOR_TRANSPARENT, null);
                g.graphics.drawString(text, STANDARD_SET_WIDTH + CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
            }
        }

        if (datapts.size() >= 3) {
            val = ((IDataPoint) datapts.get(2)).getValue();

            if (val > 0) {
                text = fmt.format(val);
                g.graphics.drawImage(UNKNOWN_CIRCLE, STANDARD_SET_WIDTH * 2, yCircle, COLOR_TRANSPARENT, null);
                g.graphics.drawString(text, (STANDARD_SET_WIDTH * 2) + CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
            }
        }

        return new Rectangle(0, 0, this.height, this.width);
    }

    //    protected IndexColorModel getIndexColorModel() {
    //        IndexColorModel cm = super.getIndexColorModel();
    //
    //        int size = cm.getMapSize();
    //        byte r[] = new byte[size];
    //        byte g[] = new byte[size];
    //        byte b[] = new byte[size];
    //
    //        cm.getReds(r);
    //        cm.getGreens(g);
    //        cm.getBlues(b);
    //
    //        // Make room by moving the first color to the end of the list
    //        r[size-1] = r[0];
    //        g[size-1] = g[0];
    //        b[size-1] = b[0];
    //
    //        // Set our transparent color as the first in the index
    //        r[0] = (byte)COLOR_TRANSPARENT.getRed();
    //        g[0] = (byte)COLOR_TRANSPARENT.getGreen();
    //        b[0] = (byte)COLOR_TRANSPARENT.getBlue();
    //
    //        return new IndexColorModel(8, size, r, g, b, 0);
    //    }

    protected int getYLabelWidth(Graphics2D g) {
        return 0;
    }

    protected Rectangle getInteriorRectangle(ChartGraphics g) {
        return new Rectangle(0, 0, this.width, this.height);
    }

    protected String[] getXLabels() {
        return null;
    }
}