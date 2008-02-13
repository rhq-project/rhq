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
import java.awt.Rectangle;
import java.util.StringTokenizer;

public class ChartGraphics {
    private static final int DRAW_NONE = 0;
    private static final int DRAW_CENTERED = 1;

    protected static final int EVENT_HEIGHT = 11;
    protected static final int EVENT_WIDTH = 11;
    protected static final int HALF_EVENT_HEIGHT = EVENT_HEIGHT / 2;
    protected static final int HALF_EVENT_WIDTH = EVENT_HEIGHT / 2;

    private static final Font EVENT_FONT = new Font("Helvetica", Font.BOLD, 9);
    private static FontMetrics m_metricsEvent;

    private Chart m_chart;
    public Graphics2D graphics;

    public ChartGraphics(Chart chart, Graphics2D graph) {
        this.m_chart = chart;
        this.graphics = graph;

        m_metricsEvent = graph.getFontMetrics();
    }

    public void dispose() {
        this.graphics.dispose();
    }

    public void drawEvent(int eventId, int x, int y) {
        String evt = Integer.toString(eventId);
        this.graphics.fillOval(x - HALF_EVENT_WIDTH, y - HALF_EVENT_HEIGHT, EVENT_WIDTH, EVENT_HEIGHT);

        // Backup drawing objects
        Font fontOrig = this.graphics.getFont();
        Color clrOrig = this.graphics.getColor();

        this.graphics.setColor(Color.WHITE);
        this.graphics.setFont(EVENT_FONT);
        this.graphics.drawString(evt, x - (m_chart.m_metricsLabel.stringWidth(evt) / 2) + 1, y
            + (m_chart.m_metricsLabel.getAscent() / 2) - 1);

        // Restore drawing objects
        this.graphics.setColor(clrOrig);
        this.graphics.setFont(fontOrig);
    }

    public void drawXLegendString(String text) {
        this.graphics.setColor(m_chart.legendTextColor);

        Rectangle rect = m_chart.getInteriorRectangle(this);
        int yHorzLegend = rect.height - m_chart.bottomBorder;

        // Split out anything in parens (e.g., TIME (2:00pm to 3:00pm)
        String text1;
        String text2;
        int textWidth;
        int text1Width = 0;

        int paren = text.indexOf('(');
        if (paren != -1) {
            text1 = text.substring(0, paren);
            text2 = text.substring(paren);
            text1Width = m_chart.m_metricsLegend.stringWidth(text1);
            textWidth = text1Width + m_chart.m_metricsLabel.stringWidth(text2);
        } else {
            text1 = text;
            text2 = null;
            textWidth = m_chart.m_metricsLegend.stringWidth(text1);
        }

        int x = (m_chart.width / 2) - (textWidth / 2);
        int x2 = x + text1Width;

        if (m_chart.showTopLegend == true) {
            this.graphics.setFont(m_chart.legendFont);
            this.graphics.drawString(text1, x, m_chart.yTopLegend);

            if (text2 != null) {
                this.graphics.setFont(m_chart.font);
                this.graphics.drawString(text2, x2, m_chart.yTopLegend);
            }
        }

        if (m_chart.showBottomLegend == true) {
            this.graphics.setFont(m_chart.legendFont);
            this.graphics.drawString(text1, x, m_chart.yBottomLegend);

            if (text2 != null) {
                this.graphics.setFont(m_chart.font);
                this.graphics.drawString(text2, x2, m_chart.yBottomLegend);
            }
        }
    }

    public void drawYLegendString(String text) {
        char[] achVal = text.toCharArray();

        graphics.setColor(m_chart.legendTextColor);
        graphics.setFont(m_chart.legendFont);

        for (int i = 0, y = (m_chart.height / 2) - (m_chart.m_metricsLegend.getAscent() * text.length() / 2); i < achVal.length; i++, y += m_chart.m_metricsLegend
            .getAscent()) {
            if (m_chart.showLeftLegend) {
                this.graphics.drawChars(achVal, i, 1, m_chart.xVertLegend, y);
            }
        }
    }

    public void drawXLines(int[] lines, String[] labels, boolean fullLines) {
        graphics.setFont(m_chart.font);
        FontMetrics metrics = this.graphics.getFontMetrics(m_chart.font);

        Rectangle rect = m_chart.getInteriorRectangle(this);
        int x2 = rect.x + ((fullLines == true) ? rect.width : 0);
        int y2 = rect.y + rect.height;
        int xBeginLine = rect.x;

        if (m_chart.showLeftLabels == true) {
            xBeginLine -= (m_chart.lineWidth + m_chart.tickMarkHeight);
        }

        for (int i = 0; i < lines.length; i++) {
            int y = rect.y + y2 - lines[i];

            int xEndLine = x2;
            if (m_chart.showRightLabels == true) {
                xEndLine += (m_chart.lineWidth + m_chart.tickMarkHeight);
            }

            this.graphics.setColor(m_chart.xLineColor);
            this.graphics.drawLine(xBeginLine, y, xEndLine, y);

            this.graphics.setColor(m_chart.textColor);

            if (m_chart.showLeftLabels == true) {
                this.graphics.drawString(labels[i], m_chart.x2VertLabels - metrics.stringWidth(labels[i]), y
                    + (metrics.getAscent() / 2) - 1);
            }

            if (m_chart.showRightLabels == true) {
                this.graphics.drawString(labels[i], m_chart.xRLabel, y + (metrics.getAscent() / 2) - 1);
            }
        }
    }

    public void drawYLines(int[] lines, String[] labels, boolean fullLines, int skip) {
        if ((m_chart.showBottomLabels == false) && (m_chart.showTopLabels == false)) {
            return;
        }

        graphics.setFont(m_chart.font);

        Rectangle rect = m_chart.getInteriorRectangle(this);
        int yBegin = rect.y + m_chart.lineWidth;
        int yEnd = yBegin + rect.height;
        int y = (fullLines == true) ? yBegin : yEnd;
        int y1;
        int y2;

        for (int i = 0; i < lines.length; i++) {
            if (m_chart.showTopLabels == false) {
                y1 = yBegin;
            } else if ((m_chart.showFullLabels == false) && ((i % skip) != 0)) {
                y1 = yBegin - (m_chart.lineWidth * 2) - (m_chart.tickMarkHeight / 2);
            } else {
                y1 = yBegin - (m_chart.lineWidth * 2) - m_chart.tickMarkHeight;
            }

            if (m_chart.showBottomLabels == false) {
                y2 = yEnd;
            } else if ((m_chart.showFullLabels == false) && ((i % skip) != 0)) {
                y2 = yEnd + (m_chart.tickMarkHeight / 2);
            } else {
                y2 = yEnd + m_chart.tickMarkHeight;
            }

            graphics.setColor(m_chart.xLineColor);
            if (m_chart.showTopLabels == true) {
                graphics.drawLine(lines[i], y1, lines[i], yEnd);
            } else if (m_chart.showBottomLabels == true) {
                graphics.drawLine(lines[i], y, lines[i], y2);
            } else {
                graphics.drawLine(lines[i], y1, lines[i], y2 - this.m_chart.lineWidth);
            }

            // Draw the text label
            if ((m_chart.showFullLabels == true) || ((i % skip) == 0) || ((i + 1) == labels.length)) {
                graphics.setColor(m_chart.textColor);

                if ((m_chart.showTopLabels == true) && (labels[i] != null)) {
                    this.drawString(labels[i], lines[i], y1 - m_chart.textWhitespace, DRAW_CENTERED);
                }

                if ((m_chart.showBottomLabels == true) && (labels[i] != null)) {
                    this.drawString(labels[i], lines[i], m_chart.yHorzLabels, DRAW_CENTERED);
                }
            }
        }
    }

    /**
     * Draws multiline strings
     *
     * @param s String to draw.
     * @param x x coordinate.
     * @param y y coordinate.
     */
    public void drawString(String s, int x, int y, int effect) {
        FontMetrics metrics = this.graphics.getFontMetrics();
        StringTokenizer tok = new StringTokenizer(s);

        while (tok.hasMoreTokens() == true) {
            String text = tok.nextToken();
            int xText = (effect == DRAW_CENTERED) ? (x - (metrics.stringWidth(text) / 2)) : x;

            this.graphics.drawString(text, xText, y);

            y += metrics.getAscent();
        }
    }

    public static int getStringHeight(String s, FontMetrics metrics) {
        int result = 0;

        int cTokens = (new StringTokenizer(s)).countTokens();
        for (int i = 0; i < cTokens; i++) {
            result += metrics.getAscent();
        }

        return result;
    }
}