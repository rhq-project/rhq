/*
 * @(#)SunrayDraw.java  1.0  September 18, 2007
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
 *SunrayDraw draws a {@link CirclemapTree}.
 * <p>
 * Can draw the tree from any node within the tree.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunrayDraw {
    /**
     * Center of the sunburst tree.
     */
    private double cx = 100, cy = 100;

    /**
     * Inner and outer radius of the sunburst tree.
     */
    private double innerRadius = 0, outerRadius = 96;

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

    /**
     * The rotation in radians of the tree.
     */
    private double rotation;


    private NodeInfo info;

    private int maxScatter = 4;

    /*
    private int w = 200, h = 200;
    private SBModel model;
    private double radius;
    private double selectedAngleFactor;
    private double selectedMaxDepth;
     */

    /** Creates a new instance. */
    public SunrayDraw(SunrayTree model) {
        this(model.getRoot(), model.getInfo());
    }
    public SunrayDraw(SunrayNode root, NodeInfo info) {
        this.root = root;
        this.info = info;
        totalDepth = root.getMaxRayDepth();
        numberToAngleFactor = Math.PI * 2d / root.getExtent();
        rotation = 0;
    }

    public NodeInfo getInfo() {
        return info;
    }
    public SunrayNode getRoot() {
        return root;
    }

    public double getRotation() {
        return rotation;
    }
    public void setRotation(double newValue) {
        this.rotation = (Math.PI * 2 + newValue) % (Math.PI * 2);
    }

    public int getTotalDepth() {
        return totalDepth;
    }

    public double getCX() {
        return cx;
    }
    public void setCX(double newValue) {
        cx = newValue;
    }

    public double getCY() {
        return cy;
    }

    public void setCY(double newValue) {
        cy = newValue;
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(double newValue) {
        innerRadius = newValue;
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(double newValue) {
        outerRadius = newValue;
    }

    private double getRadius(int depth) {
        return innerRadius +
                (outerRadius - innerRadius) * depth / (double) totalDepth;
    }

    private int getMaxScatter() {
        return maxScatter;
    }

    /**
     * Converts screen coordinates to polar coordinates in degrees.
     */
    public double getTheta(double x, double y) {
        return ((
                (Math.atan2(cx - x, y - cy) + rotation) / Math.PI * 180 + 360)
                % 360) / 180 * Math.PI;
    }

    public SunrayNode getNodeAt(int x, int y) {
        double r = Math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y));
        if (r < innerRadius || r > outerRadius) {
            return null;
        }

        double theta = getTheta(x, y);
        int depth = (int) ((r - innerRadius) / (outerRadius - innerRadius) * totalDepth);
        int scatter = (int) ((r - getRadius(depth)) * maxScatter / (getRadius(depth + 1) - getRadius(depth)));

        long number = (long) (theta / numberToAngleFactor) + root.getLeft();
        return root.findNode(depth, number, scatter);
    }

    public String getToolTipText(int x, int y) {
        SunrayNode node = getNodeAt(x, y);
            return (node == null) ? null : info.getTooltip(node.getDataNodePath());
    }


    public void drawNodeBounds(Graphics2D g, SunrayNode node, Color color) {
        double ri;
        double ro;
        if (node.isLeaf()) {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double shift = (ro - ri) / node.getMaxScatter();
            ri += shift * node.getScatter();
            ro = ri + shift;
            ro--;
        } else {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1) - 1;
        }
        double startAngle = (node.getLeft() - root.getLeft()) * numberToAngleFactor - getRotation();
        double arc = (node.getRight() - node.getLeft()) * numberToAngleFactor;
        GeneralPath path = createArc(getCX(), getCY(), startAngle / Math.PI * 180, arc / Math.PI * 180, ro, ri);
        g.setColor(color);
        g.draw(path);
    }
    public void drawSubtreeBounds(Graphics2D g, SunrayNode node, Color color) {
        double ri;
        double ro;
        if (node.isLeaf()) {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double shift = (ro - ri) / node.getMaxScatter();
            ri += shift * node.getScatter();
            ro = ri + shift;
        } else {
            ro = getRadius(totalDepth);
            ri = getRadius(node.getDepth() - root.getDepth());
        }
        double startAngle = (node.getLeft() - root.getLeft()) * numberToAngleFactor - getRotation();
        double arc = (node.getRight() - node.getLeft()) * numberToAngleFactor;
        GeneralPath path = createArc(getCX(), getCY(), startAngle / Math.PI * 180, arc / Math.PI * 180, ro, ri);
        g.setColor(color);
        g.draw(path);
    }
    public void drawDescendantSubtreeBounds(Graphics2D g, SunrayNode selectedNode, Color color) {
        double ro = getRadius(totalDepth);
        double ri = getRadius(selectedNode.getDepth() - root.getDepth() + 1);
        double startAngle = (selectedNode.getLeft()) * numberToAngleFactor - getRotation();
        double arc = (selectedNode.getRight() - selectedNode.getLeft()) * numberToAngleFactor;
        GeneralPath path = createArc(getCX(), getCY(), startAngle / Math.PI * 180, arc / Math.PI * 180, ro, ri);
        g.setColor(color);
        g.draw(path);
    }

    public static GeneralPath createArc(double x, double y,
            double startAngle, double arc,
            double outerRadius, double innerRadius) {
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
            if (innerRadius > 0) {
                if (arc < 360) {
                    mc.lineTo((float)dx, (float)dy);
                } else {
                    mc.moveTo((float)dx, (float)dy);
                }
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
        if (! node.isLeaf()) {
            double ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double startAngle = (node.getLeft() - root.getLeft()) * numberToAngleFactor - rotation;
            double arc = node.getExtent() * numberToAngleFactor;
            addSeg(path, cx, cy,
                    startAngle / Math.PI * 180, arc / Math.PI * 180,
                    ro
                    );
        }
        for (SunrayNode child : node.children()) {
            addArc(path, child);
        }
    }

    public void drawDescendants(Graphics2D g, SunrayNode node) {
        for (SunrayNode child : node.children()) {
            drawTree(g, child);
        }
    }

    public void drawLabel(Graphics2D g, SunrayNode node) {
        double ri;
        double ro;
        if (node.isLeaf()) {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double shift = (ro - ri) / node.getMaxScatter();
            ri += shift * node.getScatter();
            ro = ri + shift;
        } else {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1);
        }

        double startAngle = (node.getLeft() - root.getLeft()) *
                numberToAngleFactor - rotation;
        double endAngle = (node.getRight() - root.getLeft()) *
                numberToAngleFactor - rotation;
        double arc = node.getExtent() * numberToAngleFactor;

        double sx = cx + Math.cos(startAngle) *ri;
        double sy = cy + Math.sin( - startAngle) * ri;
        double ex = cx + Math.cos(endAngle) * ri;
        double ey = cy + Math.sin( - endAngle) * ri;

        double seg = Math.sqrt((sx - ex) * (sx - ex) + (sy - ey) * (sy - ey));
        g.setColor(Color.BLACK);

        FontMetrics fm = g.getFontMetrics();
        int fh = fm.getHeight();
        if (fh < seg || arc > Math.PI) {
            double space = ro - ri - 8;

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
                AffineTransform t = new AffineTransform();
                t.translate(cx, cy);
                t.rotate((startAngle + endAngle + Math.PI) / 2d);
                AffineTransform oldT = (AffineTransform) g.getTransform().clone();
                g.setTransform(t);
                g.drawString(new String(nameC, 0, nameLength), (int) ri + 4, fm.getAscent() - fh / 2);
                g.setTransform(oldT);
            }

        }
        //g.draw(new Line2D.Double(sx, sy, ex, ey));
    }
    public void drawNode(Graphics2D g, SunrayNode node) {
        double ri;
        double ro;
        double less;
        if (node.isLeaf()) {
            ri = getRadius(node.getDepth() - root.getDepth());
            ro = getRadius(node.getDepth() - root.getDepth() + 1);
            double shift = (ro - ri) / node.getMaxScatter();
            ri += shift * node.getScatter();
            ro = ri + shift;
            less = (node.getExtent() == root.getExtent()) ? 0d : Math.PI * 0.1 / ri;
        } else {
            ri = getRadius(node.getDepth() - root.getDepth());
            if (ri > 0) ri++;
            ro = getRadius(node.getDepth() - root.getDepth() + 1)- 1;
            less = (node.getExtent() == root.getExtent()) ? 0d : Math.PI * 0.3 / ri;
        }
        double startAngle = (node.getLeft() - root.getLeft()) * numberToAngleFactor - rotation;
        double arc = node.getExtent() * numberToAngleFactor;
        if (arc < Math.PI * 2 - less && arc > less * 4) {
            startAngle += less;
            arc -= less * 2;
        }

        GeneralPath path = createArc(
                cx, cy,
                startAngle / Math.PI * 180, arc / Math.PI * 180,
                ro - 1, ri
                );
        g.setColor(info.getColor(node.getDataNodePath()));
        if (node.isLeaf()) {
            g.fill(path);
        } else {
//            g.fill(path);
            g.draw(path);
        }
    }
}
