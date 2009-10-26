/*
 * @(#)SRDraw.java  1.0  September 18, 2007
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree.sunray;

import ch.randelshofer.tree.DefaultNodeInfo;
import ch.randelshofer.tree.NodeInfo;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * IcerayDraw draws a linear version of a {@link SunrayTree}.
 * <p>
 * Can draw the tree from any node within the tree.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class IcerayDraw {
    /**
     * Center of the sunburst tree.
     */
    private double cx = 100, cy = 100;

    /**
     * Inner and outer radius of the sunburst tree.
     */
    private double width = 96, height = 96;

    /**
     * Root of the sunburst tree.
     */
    private SunrayNode root;

    /**
     * Maximal depth of the sunburst tree.
     */
    private int totalDepth;

    /**
     * Factor for converting the left and right number
     * of a node into an angle.
     */
    private double numberToAngleFactor;

    private NodeInfo info;

    /*
    private int w = 200, h = 200;
    private SunrayTree model;
    private double radius;
    private double selectedAngleFactor;
    private double selectedMaxDepth;
     */

    /** Creates a new instance. */
    public IcerayDraw(SunrayTree model) {
        this(model.getRoot(), model.getInfo());
    }
    public IcerayDraw(SunrayNode root, NodeInfo info) {
        this.root = root;
        totalDepth = getRoot().getMaxRayDepth();
        numberToAngleFactor = Math.PI * 2d / root.getExtent();
        this.info = info;
    }

    public SunrayNode getRoot() {
        return root;
    }

    public NodeInfo getInfo() {
        return info;
    }

    public int getTotalDepth() {
        return totalDepth;
    }

    public double getX() {
        return cx;
    }
    public void setX(double newValue) {
        cx = newValue;
    }

    public double getY() {
        return cy;
    }

    public void setY(double newValue) {
        cy = newValue;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double newValue) {
        width = newValue;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double newValue) {
        height = newValue;
    }

    public SunrayNode getNodeAt(int x, int y) {
        int depth = (int) ((x - cx) / width * getTotalDepth());
        long number = (long) ((y - cy) / height * root.getExtent()) + root.getLeft();

      //  int scatter = (int) ((x - cx) / width * getTotalDepth() * root.getScatter()) % root.getMaxScatter();
        int scatter = (int) (((x - cx) - getRadius(depth)) * root.getMaxScatter() / (getRadius(depth + 1) - getRadius(depth)));
        return root.findNode(depth, number, scatter);
    }

    public String getToolTipText(int x, int y) {
        SunrayNode node = getNodeAt(x, y);
        return (node == null) ? null : info.getTooltip(node.getDataNodePath());
    }


    public void drawNodeBounds(Graphics2D g, SunrayNode node, Color color) {
        double h = height * node.getExtent() / root.getExtent();
        double less;
        if (h > 2 && node.getExtent() < root.getExtent()) {
            less = 0.5;
        } else {
            less = 0;
        }

        Rectangle2D.Double r;
        if (node.isLeaf()) {
            double sw = width / totalDepth / node.getMaxScatter();
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth +
                    node.getScatter() * sw,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    sw - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        } else {
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    width / totalDepth - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        }
        g.setColor(color);
        g.draw(r);
    }
    public void drawSubtreeBounds(Graphics2D g, SunrayNode node, Color color) {
        double h = height * node.getExtent() / root.getExtent();
        double less;
        if (h > 2 && node.getExtent() < root.getExtent()) {
            less = 0.5;
        } else {
            less = 0;
        }

        Rectangle2D.Double r = new Rectangle2D.Double(
                cx + width * node.getDepth() / totalDepth,
                cy + height * node.getLeft() / root.getExtent() + less,
                width - (width * node.getDepth() / totalDepth),
                height * node.getExtent() / root.getExtent() - less * 2
                );
        g.setColor(color);
        g.draw(r);
    }
    public void drawDescendantSubtreeBounds(Graphics2D g, SunrayNode node, Color color) {
        if (node.isLeaf()) {
            drawNodeBounds(g, node, color);
        } else {
            double h = height * node.getExtent() / root.getExtent();
            double less;
            if (h > 2 && node.getExtent() < root.getExtent()) {
                less = 0.5;
            } else {
                less = 0;
            }

            Rectangle2D.Double r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() + 1) / totalDepth,
                    cy + height * node.getLeft() / root.getExtent() + less,
                    width - (width * (node.getDepth() + 1) / totalDepth),
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
            g.setColor(color);
            g.draw(r);
        }
    }

    public static Rectangle2D.Double createArc(double x, double y,
            double startAngle, double arc,
            double outerRadius, double innerRadius) {
        Rectangle2D.Double rec;

        rec = new Rectangle2D.Double(
                x + innerRadius, y + startAngle,
                outerRadius - innerRadius, arc
                );
        return rec;


        /*
        GeneralPath mc = new GeneralPath();

        // if yRadius is undefined, yRadius = radius
        // Init vars
        double segAngle_a, segAngle_b, theta_a, theta_b, angle, angleMid, segs;
        double ax, ay, bx, by, cx, cy, dx, dy;

        // init bx and by, altough they are always initialized
        bx = by = 0;

        // limit sweep to reasonable numbers
        if (Math.abs(arc) > 360) {
            arc = 360;
        }
        // Flash uses 8 segments per circle, to match that, we draw in a maximum
        // of 45 degree segments. First we calculate how many segments are needed
        // for our arc.
        segs = Math.ceil(Math.abs(arc) / 45);
        // Now calculate the sweep of each segment.
        segAngle_a = arc / segs;
        segAngle_b = -arc / segs;
        // The math requires radians rather than degrees. To convert from degrees
        // use the formula (degrees/180)*Math.PI to get radians.
        theta_a = - (segAngle_a / 180) * Math.PI;
        theta_b = - (segAngle_b / 180) * Math.PI;
        // convert angle startAngle to radians
        angle = - (startAngle / 180) * Math.PI;
        // draw the curve in segments no larger than 45 degrees.
        if (segs > 0) {
            // draw a line from the end of the interior curve to the start of the exterior curve
            ax = x + Math.sin( - startAngle / 180 * Math.PI) * outerRadius;
            ay = y + Math.cos( startAngle / 180 * Math.PI) * outerRadius;
            mc.moveTo((float) ax, (float)ay);
            // Loop for drawing exterior  curve segments
            for (int i = 0; i < segs; i ++) {
                angle += theta_a;
                angleMid = angle - (theta_a / 2);
                bx = x + Math.sin(angle) * outerRadius;
                by = y + Math.cos(angle) * outerRadius;
                cx = x + Math.sin(angleMid) * (outerRadius / Math.cos(theta_a / 2));
                cy = y + Math.cos(angleMid) * (outerRadius / Math.cos(theta_a / 2));
                mc.quadTo((float)cx, (float)cy, (float)bx, (float)by);
            }
            // draw a line from the end of the exterior curve to the start of the interior curve

            startAngle += arc;
            angle = - (startAngle / 180) * Math.PI;
            // draw the interior (subtractive) wedge
            // draw a line from the center to the start of the interior curve
            dx = x + Math.sin( - startAngle / 180 * Math.PI) * innerRadius;
            dy = y + Math.cos( startAngle / 180 * Math.PI) * innerRadius;
            if (arc < 360) {
                mc.lineTo((float)dx, (float)dy);
            } else {
                mc.moveTo((float)dx, (float)dy);
            }
            if (innerRadius > 0) {
                // Loop for drawing interior curve segments
                for (int i = 0; i < segs; i ++) {
                    angle += theta_b;
                    angleMid = angle - (theta_b / 2);
                    bx = x + Math.sin(angle) * innerRadius;
                    by = y + Math.cos(angle) * innerRadius;
                    cx = x + Math.sin(angleMid) * (innerRadius / Math.cos(theta_b / 2));
                    cy = y + Math.cos(angleMid) * (innerRadius / Math.cos(theta_b / 2));
                    mc.quadTo((float)cx, (float)cy, (float)bx, (float)by);
                }
            }
            if (arc < 360) {
                mc.lineTo((float)ax, (float)ay);
            }
        }
        return mc;
         */
    }
    public static void addSeg(GeneralPath mc, double x, double y,
            double startAngle, double arc,
            double radius) {

        // Init vars
        double segAngle_a, segAngle_b, theta_a, theta_b, angle, angleMid, segs;
        double ax, ay, bx, by, cx, cy, dx, dy;

        // init bx and by, altough they are always initialized
        bx = by = 0;

        // limit sweep to reasonable numbers
        if (Math.abs(arc) > 360) {
            arc = 360;
        }
        // Flash uses 8 segments per circle, to match that, we draw in a maximum
        // of 45 degree segments. First we calculate how many segments are needed
        // for our arc.
        segs = Math.ceil(Math.abs(arc) / 45);
        // Now calculate the sweep of each segment.
        segAngle_a = arc / segs;
        segAngle_b = -arc / segs;
        // The math requires radians rather than degrees. To convert from degrees
        // use the formula (degrees/180)*Math.PI to get radians.
        theta_a = - (segAngle_a / 180) * Math.PI;
        theta_b = - (segAngle_b / 180) * Math.PI;
        // convert angle startAngle to radians
        angle = - (startAngle / 180) * Math.PI;
        // draw the curve in segments no larger than 45 degrees.
        if (segs > 0) {
            // draw a line from the end of the interior curve to the start of the exterior curve
            ax = x + Math.sin( - startAngle / 180 * Math.PI) * radius;
            ay = y + Math.cos( startAngle / 180 * Math.PI) * radius;
            mc.moveTo((float) ax, (float)ay);
            // Loop for drawing exterior  curve segments
            for (int i = 0; i < segs; i ++) {
                angle += theta_a;
                angleMid = angle - (theta_a / 2);
                bx = x + Math.sin(angle) * radius;
                by = y + Math.cos(angle) * radius;
                cx = x + Math.sin(angleMid) * (radius / Math.cos(theta_a / 2));
                cy = y + Math.cos(angleMid) * (radius / Math.cos(theta_a / 2));
                mc.quadTo((float)cx, (float)cy, (float)bx, (float)by);
            }
        }
    }

    /**
     * Draws the Sunburst tree onto
     * the supplied graphics object.
     */
    public void drawTree(Graphics2D g) {
        drawTree(g, root);
    }

    public void drawTree(Graphics2D g, SunrayNode node) {
        drawNode(g, node);
        drawLabel(g, node);
        for (SunrayNode child : node.children()) {
            drawTree(g, child);
        }
    }

    public void drawContours(Graphics2D g, SunrayNode node, Color color) {
        GeneralPath path = new GeneralPath();
        addArc(path, node);
        g.setColor(color);
        g.draw(path);
    }

    private void addArc(GeneralPath path, SunrayNode node) {
        /*
        if (! node.isLeaf()) {
            double ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double startAngle = (node.getLeft() - root.getLeft()) * numberToAngleFactor;
            double arc = node.getExtent() * numberToAngleFactor;
            addSeg(path, cx, cy,
                    startAngle / Math.PI * 180, arc / Math.PI * 180,
                    ro
                    );
        }
        for (SunrayNode child : node.children()) {
            addArc(path, child);
        }*/
    }

    public void drawDescendants(Graphics2D g, SunrayNode node) {
        for (SunrayNode child : node.children()) {
            drawTree(g, child);
        }
    }
    private double getRadius(int depth) {
        return width * depth / (double) totalDepth;
    }
    /**
     * Converts screen coordinates to polar coordinates in degrees.
     */
    public double getTheta(double x, double y) {
        if (y < cy || y > cy + height) {
            return 0;
        }
        return (y - cy) * 360d / height;
    }


    public void drawLabel(Graphics2D g, SunrayNode node) {
        double h = height * node.getExtent() / root.getExtent();
        double less;
        if (h > 2 && node.getExtent() < root.getExtent()) {
            less = 0.5;
        } else {
            less = 0;
        }
        Rectangle2D.Double r;
        if (node.isLeaf()) {
            double sw = width / totalDepth / node.getMaxScatter();
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth +
                    node.getScatter() * sw,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    sw - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        } else {
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    width / totalDepth - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        }

        FontMetrics fm = g.getFontMetrics();
        int fh = fm.getHeight();
        if (fh < r.height) {
            double space = r.width - 4;

            String name = info.getName(node.getDataNodePath());
            char[] nameC = (name == null) ? new char[0] : name.toCharArray();
            int nameLength = nameC.length;
            int nameWidth = fm.charsWidth(nameC, 0, nameLength);

            while((nameWidth >= space) && (nameLength > 1)) {
                nameLength--;
                nameC[nameLength - 1] = '.';
                nameWidth = fm.charsWidth(nameC, 0, nameLength);
            }

            if (nameLength > 1 || nameLength == nameC.length) {
                g.setColor(Color.BLACK);
                g.drawString(new String(nameC, 0, nameLength),
                        (int) r.x + 4,
                        (int) (r.y + fm.getAscent() + (r.height - fh) / 2));
            }

        }
    }
    public void drawNode(Graphics2D g, SunrayNode node) {
        double h = height * node.getExtent() / root.getExtent();
        double less;
        if (h > 2 && node.getExtent() < root.getExtent()) {
            less = 0.5;
        } else {
            less = 0;
        }
        Rectangle2D.Double r;
        if (node.isLeaf()) {
            double sw = width / totalDepth / node.getMaxScatter();
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth +
                    node.getScatter() * sw,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    sw - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        } else {
            r = new Rectangle2D.Double(
                    cx + width * (node.getDepth() - root.getDepth()) / totalDepth,
                    cy + height * (node.getLeft() - root.getLeft()) / root.getExtent() + less,
                    width / totalDepth - 1,
                    height * node.getExtent() / root.getExtent() - less * 2
                    );
        }
        g.setColor(info.getColor(node.getDataNodePath()));
        if (node.isLeaf()) {
        g.fill(r);
        } else {
//        g.fill(r);
            g.draw(r);
        }
    }
}
