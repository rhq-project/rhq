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
package org.rhq.enterprise.gui.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

public class WebImage {
    /////////////////////////////////////////////////////
    // Class static variables

    protected static final String ARG_CANNOT_BE_NULL = "Argument cannot be null";

    private static final String IMAGE_JPEG = "jpeg";
    private static final String IMAGE_PNG = "png";

    protected static final int DEFAULT_HEIGHT = 300;
    protected static final int DEFAULT_WIDTH = 755;
    protected static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    protected static final int DEFAULT_BORDER_SIZE = 5;
    protected static final Color DEFAULT_BORDER_COLOR = Color.LIGHT_GRAY;
    protected static final Color DEFAULT_TEXT_COLOR = Color.BLACK;
    protected static final int DEFAULT_SHADOW_WIDTH = 3;
    protected static final String DEFAULT_BOLD_TYPEFACE = "sansserif.bold";
    protected static final String DEFAULT_PLAIN_TYPEFACE = "sansserif.plain";

    protected static final Font DEFAULT_FONT = new Font(DEFAULT_PLAIN_TYPEFACE, Font.PLAIN, 11);
    protected static final FontMetrics DEFAULT_FONT_METRICS;

    public static final Font SMALL_FONT = new Font(DEFAULT_PLAIN_TYPEFACE, Font.PLAIN, 8);

    /////////////////////////////////////////////////////
    // Object variables

    private FontMetrics m_fontMetrics; // Set when font is set
    protected Graphics2D m_graphics;

    /////////////////////////////////////////////////////
    // Public Properties

    /**
     * Text font.
     */
    public Font font = DEFAULT_FONT;

    /**
     * Height of the image.
     */
    public int height = DEFAULT_HEIGHT;

    /**
     * Width of the image.
     */
    public int width = DEFAULT_WIDTH;

    /**
     * Width of the image border on the left side of the image
     */
    public int leftBorder = DEFAULT_BORDER_SIZE;

    /**
     * Height of the image border on the top side of the image
     */
    public int topBorder = DEFAULT_BORDER_SIZE;

    /**
     * Width of the image border on the right side of the image
     */
    public int rightBorder = DEFAULT_BORDER_SIZE;

    /**
     * Height of the image border on the bottom side of the image
     */
    public int bottomBorder = DEFAULT_BORDER_SIZE;

    /**
     * Draws a two pixel light gray frame at the edge of the image
     */
    public boolean frameImage = false;

    /**
     * Background color for the image.
     */
    public Color backgroundColor = DEFAULT_BACKGROUND_COLOR;

    /**
     * Color for text in the image.
     */
    public Color textColor = DEFAULT_TEXT_COLOR;

    /**
     * Width of the shadow around the image.
     */
    public int shadowWidth = DEFAULT_SHADOW_WIDTH;

    /////////////////////////////////////////////////////
    // Protected Properties

    /**
     * Use an IndexColorModel.
     */
    protected boolean antiAliased = true;

    /**
     * Anti-alias shapes and text.
     */
    protected boolean indexColors = false;

    /////////////////////////////////////////////////////
    // Static constructor

    static {
        // Get Font Metrics
        Image img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        java.awt.Graphics g = img.getGraphics();
        DEFAULT_FONT_METRICS = g.getFontMetrics(DEFAULT_FONT);
        g.dispose();
    }

    /////////////////////////////////////////////////////
    // Constructors

    protected WebImage(int width, int height) {
        this.width = width;
        this.height = height;

        m_fontMetrics = DEFAULT_FONT_METRICS;
    }

    /////////////////////////////////////////////////////
    // Protected Methods

    protected void draw(Graphics2D g) {
        g.fillRect(0, 0, this.width + this.shadowWidth, this.height + this.shadowWidth);

        if (this.frameImage == true) {
            // Draw the frame
            Stroke orig = g.getStroke();
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.setColor(DEFAULT_BORDER_COLOR);
            g.drawRect(0, 0, this.width - 1, this.height - 1);

            // Draw the shadow
            if (this.shadowWidth > 0) {
                int[] x = { this.width + 1, this.width + 1, this.shadowWidth };
                int[] y = { this.shadowWidth, this.height + 1, this.height + 1 };
                g.setColor(Color.BLACK);
                g.drawPolyline(x, y, x.length);
            }

            // Put back the color and stroke we started with
            g.setColor(Color.WHITE);
            g.setStroke(orig);
        }
    }

    protected void preInit() {
    }

    protected void postInit(Graphics2D graphics) {
        this.initFontMetrics();
    }

    /////////////////////////////////////////////////////
    // Public Methods

    /**
     * Sets the size of the top, left, right and bottom borders.
     *
     * @param border The size to set the borders to.
     *
     * @see   #LeftBorder
     */
    public void setBorder(int border) {
        this.topBorder = border;
        this.leftBorder = border;
        this.rightBorder = border;
        this.bottomBorder = border;
    }

    /**
     * Retrieves the font metrics.
     *
     * @return A java.awt.Font object that contains the label font.
     *
     * @see    java.awt.Font
     */
    public FontMetrics getFontMetrics() {
        return m_fontMetrics;
    }

    /**
     * Retrieves the image as a java.awt.Image object. The image is redrawn with the latest data and properties each
     * time this method is called.
     *
     * @return A java.awt.Image that contains the drawn chart image.
     *
     * @see    java.awt.Image
     */
    public Image getImage() {
        BufferedImage image;
        Graphics2D g;

        this.preInit();

        image = new BufferedImage(this.width + this.shadowWidth, this.height + this.shadowWidth,
            BufferedImage.TYPE_INT_RGB);

        g = m_graphics = (Graphics2D) image.getGraphics();

        if (this.antiAliased == true) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        this.postInit(g);

        g.setColor(this.backgroundColor);
        this.draw(g);
        g.dispose();

        if (this.indexColors == true) {
            image = ImageUtil.convertToIndexColorImage(image);
        }

        return image;
    }

    /**
     * Writes the chart image in a JPEG or PNG format. The chart is redrawn with the latest data and properties each
     * time this method is called.
     *
     * @param     filename The path and filename that specifies where the PNG image should be written.
     * @param     type     The type of image in the format of "image/jpeg" or "image/png".
     *
     * @exception FileNotFoundException    If the filename is not a valid name for a file.
     * @exception IOException              If there is an IO error while writing to the file.
     * @exception IllegalArgumentException If the filename parameter is null.
     */
    private void writeImage(String filename, String type) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(filename);
        this.writeImage(out, type);
        out.close();
    }

    /**
     * Writes the chart image in a JPEG or PNG format. The chart is redrawn with the latest data and properties each
     * time this method is called.
     *
     * @param     stream The java.io.OutputStream to write the PNG image to.
     * @param     type   The type of image in the format of "jpeg" or "png".
     *
     * @exception IOException              If there is an IO error while streaming the PNG image.
     * @exception IllegalArgumentException If the stream parameter is null.
     *
     * @see       java.io.OutputStream
     */
    private void writeImage(OutputStream stream, String type) throws IOException {
        ImageIO.write((BufferedImage) this.getImage(), type, stream);
        stream.flush();
    }

    /**
     * Writes the chart image as a JPEG image. The chart is redrawn with the latest data and properties each time this
     * method is called.
     *
     * @param     filename The path and filename that specifies where the PNG image should be written.
     *
     * @exception FileNotFoundException    If the filename is not a valid name for a file.
     * @exception IOException              If there is an IO error while writing to the file.
     * @exception IllegalArgumentException If the filename parameter is null.
     */
    public void writeJpegImage(String filename) throws FileNotFoundException, IOException {
        if (filename == null) {
            throw new IllegalArgumentException();
        }

        this.writeImage(filename, IMAGE_JPEG);
    }

    /**
     * Writes the chart image as a JPEG image. The chart is redrawn with the latest data and properties each time this
     * method is called.
     *
     * @param     stream The java.io.OutputStream to write the PNG image to.
     *
     * @exception IOException              If there is an IO error while streaming the PNG image.
     * @exception IllegalArgumentException If the stream parameter is null.
     *
     * @see       java.io.OutputStream
     */
    public void writeJpegImage(OutputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException(ARG_CANNOT_BE_NULL);
        }

        this.writeImage(stream, IMAGE_JPEG);
    }

    /**
     * Writes the chart image as a PNG image. The chart is redrawn with the latest data and properties each time this
     * method is called.
     *
     * @param     filename The path and filename that specifies where the PNG image should be written.
     *
     * @exception FileNotFoundException    If the filename is not a valid name for a file.
     * @exception IOException              If there is an IO error while writing to the file.
     * @exception IllegalArgumentException If the filename parameter is null.
     */
    public void writePngImage(String filename) throws FileNotFoundException, IOException {
        if (filename == null) {
            throw new IllegalArgumentException(ARG_CANNOT_BE_NULL);
        }

        this.writeImage(filename, IMAGE_PNG);
    }

    /**
     * Writes the chart image as a PNG image. The chart is redrawn with the latest data and properties each time this
     * method is called.
     *
     * @param     stream The java.io.OutputStream to write the PNG image to.
     *
     * @exception IOException              If there is an IO error while streaming the PNG image.
     * @exception IllegalArgumentException If the stream parameter is null.
     *
     * @see       java.io.OutputStream
     */
    public void writePngImage(OutputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException(ARG_CANNOT_BE_NULL);
        }

        this.writeImage(stream, IMAGE_PNG);
    }

    ////////////////////////////////////////////////////
    // Private Methods

    private void initFontMetrics() {
        if (m_graphics != null) {
            this.m_fontMetrics = m_graphics.getFontMetrics(this.font);
        }
    }

    ////////////////////////////////////////////////////
    // Protected Helper Methods

    protected java.awt.Point getTextCenter(String text) {
        return this.getTextCenter(text, new Rectangle(0, 0, this.width, this.height));
    }

    protected Point getTextCenter(String text, Rectangle rect) {
        return WebImage.getTextCenter(text, rect, m_fontMetrics);
    }

    ////////////////////////////////////////////////////
    // Static Methods

    protected static Point getTextCenter(String text, Rectangle rect, FontMetrics metrics) {
        return new Point((rect.width / 2) - (metrics.stringWidth(text) / 2), (rect.height / 2)
            + (metrics.getAscent() / 2));
    }

    /**
     * Determine if a graphics environment is available. The graphics environment would be X on Unix. GDI on Windows,
     * Mac, etc. This is primarily used as a check for X because it is very difficult to run without a graphics
     * environment on the Windows and Mac operating systems.
     *
     * @return A boolean that is true if a graphics environment is available, false otherwise.
     */
    public static boolean isRunnable() {
        boolean res;

        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
            res = true;
        } catch (InternalError e) {
            res = false;
        } catch (NoClassDefFoundError e) {
            res = false;
        }

        return res;
    }
}