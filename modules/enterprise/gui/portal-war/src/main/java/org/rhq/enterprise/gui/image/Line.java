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

public class Line {
    public int x1;
    public int y1;
    public int x2;
    public int y2;

    private int LineWidth;

    public Line() {
    }

    public Line(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Line(int x1, int y1, int x2, int y2, int width) {
        this(x1, y1, x2, y2);
        this.LineWidth = width;
    }

    public String toString() {
        StringBuffer res = new StringBuffer();

        res.append(this.getClass().getName()).append('[').append("x1=").append(x1).append(',').append("y1=").append(y1)
            .append(',').append("x2=").append(x2).append(',').append("y2=").append(y2).append(',').append("lineWidth=")
            .append(this.LineWidth).append(']');

        return res.toString();
    }
}