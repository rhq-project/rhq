/**
 * @(#)CirclemapDraw.java  1.0  Jan 16, 2008
 *
 * Copyright (c) 2008 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.tree.circlemap;

import ch.randelshofer.tree.NodeInfo;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * CirclemapDraw draws a {@link CirclemapTree}.
 * <p>
 * Can draw a subtree from any node within the tree.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class CirclemapDraw {

    private CirclemapNode root;
    private CirclemapNode drawRoot;
    private NodeInfo info;
    /**
     * Center of the phyllotactic tree.
     */
    private double cx = 100,  cy = 100;
    /**
     * Radius of the phyllotactic tree.
     */
    private double radius = 96;
    private double scaleFactor = 1;

    public CirclemapDraw(CirclemapTree model) {
        this(model.getRoot(), model.getInfo());
    }

    public CirclemapDraw(CirclemapNode root, NodeInfo info) {
        this.root = root;
        this.drawRoot = root;
        this.info = info;
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

    public double getRadius() {
        return radius;
    }

    public void setRadius(double newValue) {
        radius = newValue;
    }

    /**
     * Draws the Sunburst tree onto
     * the supplied graphics object.
     */
    public void drawTree(Graphics2D g) {
        scaleFactor = radius / drawRoot.getRadius();
        double sx = 0;
        double sy = 0;
        CirclemapNode node = drawRoot;
        while (node != null) {
            sx -= node.getCX();
            sy -= node.getCY();
            node = node.getParent();
        }

       Rectangle clipBounds = g.getClipBounds();
       if (clipBounds == null) {
           clipBounds = new Rectangle(0, 0, Integer.MAX_VALUE,Integer.MAX_VALUE);
       }

        drawTree0(g, sx, sy, scaleFactor, root, clipBounds);
    }

    public void drawTree0(Graphics2D g, double px, double py, double sf, CirclemapNode node, Rectangle clipBounds) {
        if (node.radius * sf > 1 && node.children().size() > 0) {
            double r = node.getRadius() * sf;
            Rectangle2D.Double bounds = new Rectangle2D.Double(
                    cx + sf * px - r, cy + sf * py - r, r * 2, r * 2);
            if (clipBounds.intersects(bounds)) {
                for (CirclemapNode child : node.children()) {
                    drawTree0(g,
                            px + child.getCX(),
                            py + child.getCY(),
                            sf, child, clipBounds);
                }
            }
        }

        drawNode(g, px, py, sf, node);
        drawLabel(g, px, py, sf, node);
    }

    public void drawNode(Graphics2D g, double px, double py, double sf, CirclemapNode node) {
        double r = node.getRadius() * sf;
        Ellipse2D.Double ellipse = new Ellipse2D.Double(
                cx + sf * px - r, cy + sf * py - r, r * 2, r * 2);

        if (node.isLeaf()) {
            g.setColor(info.getColor(node.getDataNodePath()));
            g.fill(ellipse);
            g.setColor(info.getColor(node.getDataNodePath()).darker());
            g.draw(ellipse);
        } else {
            g.setColor(info.getColor(node.getDataNodePath()).darker());
            g.draw(ellipse);
        }
    }

    public void drawLabel(Graphics2D g, double px, double py, double sf, CirclemapNode node) {
        if (node.children().size() == 0) {
            double r = node.getRadius() * sf;
            Ellipse2D.Double ellipse = new Ellipse2D.Double(
                    cx + sf * px - r, cy + sf * py - r, r * 2, r * 2);

            FontMetrics fm = g.getFontMetrics();
            int fh = fm.getHeight();
            if (fh < ellipse.height) {
                g.setColor(Color.BLACK);

                double space = ellipse.width - 6;

                String name = info.getName(node.getDataNodePath());
                char[] nameC = name.toCharArray();
                int nameLength = nameC.length;
                int nameWidth = fm.charsWidth(nameC, 0, nameLength);

                while ((nameWidth >= space) && (nameLength > 1)) {
                    nameLength--;
                    nameC[nameLength - 1] = '.';
                    nameWidth = fm.charsWidth(nameC, 0, nameLength);
                }

                if (nameLength > 1 || nameLength == nameC.length) {
                    g.drawString(new String(nameC, 0, nameLength),
                            (int) (ellipse.x + (ellipse.width - nameWidth) / 2),
                            (int) (ellipse.y + (ellipse.height + fm.getAscent()) / 2));
                }
            }
        }
    }

    public void drawContours(Graphics2D g, CirclemapNode node, Color color) {
    }

    public NodeInfo getInfo() {
        return info;
    }

    public CirclemapNode getRoot() {
        return root;
    }

    public CirclemapNode getDrawRoot() {
        return drawRoot;
    }

    public void setDrawRoot(CirclemapNode newValue) {
        this.drawRoot = newValue;
    }

    public void drawNodeBounds(Graphics2D g, CirclemapNode selectedNode, Color color) {
        g.setColor(color);
        double r = selectedNode.getRadius() * scaleFactor;
        double scx = 0;
        double scy = 0;

        CirclemapNode node = selectedNode;
        while (node != null) {
            scx += node.getCX();
            scy += node.getCY();
            node = node.getParent();
        }
        node = drawRoot;
        while (node != null) {
            scx -= node.getCX();
            scy -= node.getCY();
            node = node.getParent();
        }

        double px = scx * scaleFactor + cx;
        double py = scy * scaleFactor + cy;

        Ellipse2D.Double ellipse = new Ellipse2D.Double(px - r, py - r, r * 2, r * 2);
        g.draw(ellipse);
    }

    public void drawSubtreeBounds(Graphics2D g, CirclemapNode selectedNode, Color color) {
    }

    public void drawDescendantSubtreeBounds(Graphics2D g, CirclemapNode parent, Color color) {
    }

    /**
     * Returns the node at the specified view coordinates.
     */
    public CirclemapNode getNodeAt(int px, int py) {
        return getNodeAt((px - cx) / scaleFactor, (py - cy) / scaleFactor);
    }

    /**
     * Returns the node at the specified draw coordinates.
     */
    public CirclemapNode getNodeAt(double px, double py) {
        CirclemapNode parent = drawRoot;
        while (parent != null) {
            px += parent.getCX();
            py += parent.getCY();
            parent = parent.getParent();
        }

        if (root.contains(px, py)) {
            CirclemapNode found = root;
            parent = found;
            do {
                parent = found;
                px -= parent.cx;
                py -= parent.cy;
                for (CirclemapNode node : parent.children()) {
                    if (node.contains(px, py)) {
                        found = node;
                        break;
                    }
                }
            } while (found != parent);
            return found;
        } else {
            return null;
        }
    }
}
