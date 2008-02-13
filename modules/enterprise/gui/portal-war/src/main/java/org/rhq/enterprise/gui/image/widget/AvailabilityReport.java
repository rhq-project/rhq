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
package org.rhq.enterprise.gui.image.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import org.rhq.enterprise.gui.image.ImageUtil;
import org.rhq.enterprise.gui.image.WebImage;

public class AvailabilityReport extends WebImage {
    private static int CIRCLE_SIZE = 11;

    private static BufferedImage GOOD_CIRCLE;
    private static BufferedImage DANGER_CIRCLE;
    private static BufferedImage UNKNOWN_CIRCLE;

    private static final Font FONT = new Font("sansserif.plain", Font.PLAIN, 10);
    private static final Color COLOR_TRANSPARENT = new Color(0x3, 0x3, 0x3);

    private static final String LARGEST_NUMBER = "999";

    private static final int TEXT_BUFFER = 2;
    private static final int SET_BUFFER = 5;

    private static final int CIRCLE_WITH_BUFFER_WIDTH;
    private static final int STANDARD_SET_WIDTH;
    private static final int IMAGE_WIDTH;

    /////////////////////////////////////////////
    // Public Properties

    /**
     * Number of Available Resources
     */
    public int Available;

    /**
     * Number of Unavailable Resources
     */
    public int Unavailable;

    /**
     * Number of Unknown Resources
     */
    public int Unknown;

    /////////////////////////////////////////////
    // Static constructors

    static {
        // Load Images
        InputStream i;

        try {
            GOOD_CIRCLE = ImageUtil.loadImage("images/icon_available_green.gif");
            DANGER_CIRCLE = ImageUtil.loadImage("images/icon_available_red.gif");
            UNKNOWN_CIRCLE = ImageUtil.loadImage("images/icon_available_error.gif");
        } catch (IOException e) {
            System.out.println(e);
        }

        CIRCLE_WITH_BUFFER_WIDTH = GOOD_CIRCLE.getWidth() + TEXT_BUFFER;
        STANDARD_SET_WIDTH = CIRCLE_WITH_BUFFER_WIDTH + DEFAULT_FONT_METRICS.stringWidth(LARGEST_NUMBER) + SET_BUFFER;
        IMAGE_WIDTH = (STANDARD_SET_WIDTH * 3) - SET_BUFFER;
    }

    //////////////////////////////////////////////
    // Object Constructors

    public AvailabilityReport() {
        super(IMAGE_WIDTH, GOOD_CIRCLE.getHeight());
        this.antiAliased = false;
        this.indexColors = true;
    }

    //////////////////////////////////////////////
    // Methods

    protected void draw(Graphics2D g) {
        int x = 0;
        int yCircle = 0;
        int y2Circle = CIRCLE_SIZE - 1;
        String text;

        g.setPaint(COLOR_TRANSPARENT);
        g.fillRect(0, 0, this.width, this.height);

        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(0);

        g.setColor(this.textColor);
        g.setFont(FONT);

        if (this.Available > 0) {
            text = fmt.format(this.Available);
            g.drawImage(GOOD_CIRCLE, 0, yCircle, COLOR_TRANSPARENT, null);
            g.drawString(text, CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
        }

        if (this.Unavailable > 0) {
            text = fmt.format(this.Unavailable);
            g.drawImage(DANGER_CIRCLE, STANDARD_SET_WIDTH, yCircle, COLOR_TRANSPARENT, null);
            g.drawString(text, STANDARD_SET_WIDTH + CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
        }

        if (this.Unknown > 0) {
            text = fmt.format(this.Unknown);
            g.drawImage(UNKNOWN_CIRCLE, STANDARD_SET_WIDTH * 2, yCircle, COLOR_TRANSPARENT, null);
            g.drawString(text, (STANDARD_SET_WIDTH * 2) + CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
        }
    }

    //    protected IndexColorModel getIndexColorModel() {
    //        IndexColorModel cm = super.getIndexColorModel();
    //        ImageUtil.getColors(DANGER_CIRCLE);
    //        return ImageUtil.getTransparentColorModel(cm, COLOR_TRANSPARENT);
    //    }
}