/**
 * @(#)RectmapDraw.java  1.0  Jan 16, 2008
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
package ch.randelshofer.tree.rectmap;

import ch.randelshofer.tree.NodeInfo;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * RectmapDraw draws a {@link RectmapTree}.
 * <p>
 * Can draw a subtree from any node within the tree.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class RectmapDraw {

    private RectmapNode root;
    private RectmapNode drawRoot;
    private NodeInfo info;
    private Insets insets = new Insets(10,10,10,10);

    /**
     * Center of the phyllotactic tree.
     */
    private double cx = 2,  cy = 2;
    /**
     * Radius of the phyllotactic tree.
     */
    private double cwidth = 96;
    private double cheight = 96;
    private double scaleFactorH = 1;
    private double scaleFactorV = 1;

    public RectmapDraw(RectmapTree model) {
        this(model.getRoot(), model.getInfo());
    }

    public RectmapDraw(RectmapNode root, NodeInfo info) {
        this.root = root;
        this.drawRoot = root;
        this.info = info;
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
        return cwidth;
    }

    public void setWidth(double newValue) {
        cwidth = newValue;
    }

    public double getHeight() {
        return cheight;
    }

    public void setHeight(double newValue) {
        cheight = newValue;
    }

    /**
     * Draws the Sunburst tree onto
     * the supplied graphics object.
     */
    public void drawTree(Graphics2D g) {
        scaleFactorH = (cwidth - insets.left - insets.right) / drawRoot.getWidth();
        scaleFactorV = (cheight - insets.top - insets.bottom) / drawRoot.getHeight();
        double sx = 0;
        double sy = 0;
        RectmapNode node = drawRoot;
        while (node != null) {
            sx -= node.getX();
            sy -= node.getY();
            node = node.getParent();
        }
        drawTree0(g, sx + insets.left / scaleFactorH, sy + insets.top / scaleFactorV, scaleFactorH, scaleFactorV, root);
    }

    public void drawTree0(Graphics2D g, double px, double py, double sfh, double sfv, RectmapNode node) {
        drawNode(g, px, py, sfh, sfv, node);
        drawLabel(g, px, py, sfh, sfv, node);

        Rectangle2D.Double rect = new Rectangle2D.Double(
                (px) * sfh + cx, (py) * sfv + cy,
                node.width * sfh, node.height * sfv);
        if (rect.width > 1 && rect.height > 1 && (g.getClipBounds() == null ||
                g.getClipBounds().intersects(rect))) {
            for (RectmapNode child : node.children()) {
                drawTree0(g,
                        px + child.getX(),
                        py + child.getY(),
                        sfh, sfv, child);
            }
            }
    }

    public void drawNode(Graphics2D g, double px, double py, double sfh, double sfv, RectmapNode node) {
        Rectangle2D.Double rect = new Rectangle2D.Double(
                (px) * sfh + cx, (py) * sfv + cy,
                node.width * sfh, node.height * sfv);

        if (rect.width > 4) {
            rect.x += 1;
            rect.width -= 2;
        }
        if (rect.height > 4) {
            rect.y += 1;
            rect.height -= 2;
        }
        /*
        if (node.isLeaf()) {
            Color c = info.getColor(node.getDataNodePath());
        g.setColor(c);
            g.fill(rect);
            g.setColor(c.brighter());
            g.draw(new Line2D.Double(rect.x, rect.y, rect.x+rect.width-1,rect.y));
            g.draw(new Line2D.Double(rect.x, rect.y, rect.x,rect.y+rect.height-1));
            g.setColor(c.darker());
            g.draw(new Line2D.Double(rect.x, rect.y+rect.height-1, rect.x+rect.width-1,rect.y+rect.height-1));
            g.draw(new Line2D.Double(rect.x+rect.width-1, rect.y, rect.x+rect.width-1,rect.y+rect.height-1));

        } else {
            int depth = Math.min(10, node.getDataNodePath().getPathCount());
                int gray = Math.min(255,155 + (100 * depth / 10));
            g.setColor(new Color(gray, gray, gray));
            /*
            rect.width -= 1;
            rect.height -= 1;
            g.draw(rect);* /
            g.fill(rect);
        }*/
            Color c = info.getColor(node.getDataNodePath());
        g.setColor(c);
            g.fill(rect);
            g.setColor(c.brighter());
            g.draw(new Line2D.Double(rect.x, rect.y, rect.x+rect.width-1,rect.y));
            g.draw(new Line2D.Double(rect.x, rect.y, rect.x,rect.y+rect.height-1));
            g.setColor(c.darker());
            g.draw(new Line2D.Double(rect.x, rect.y+rect.height-1, rect.x+rect.width-1,rect.y+rect.height-1));
            g.draw(new Line2D.Double(rect.x+rect.width-1, rect.y, rect.x+rect.width-1,rect.y+rect.height-1));
    }

    public void drawLabel(Graphics2D g, double px, double py, double sfh, double sfv, RectmapNode node) {
        if (node.children().size() == 0) {
        Rectangle2D.Double rect = new Rectangle2D.Double(
                (px) * sfh + cx, (py) * sfv + cy,
                node.width * sfh, node.height * sfv);

            FontMetrics fm = g.getFontMetrics();
            int fh = fm.getHeight();
            if (fh < rect.height) {
                g.setColor(Color.BLACK);

                double space = rect.width - 6;

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
                            (int) (rect.x + (rect.width - nameWidth) / 2),
                            (int) (rect.y + (rect.height + fm.getAscent()) / 2));
                }
            }
        }
    }

    public void drawContours(Graphics2D g, RectmapNode node, Color color) {
    }

    public NodeInfo getInfo() {
        return info;
    }

    public RectmapNode getRoot() {
        return root;
    }

    public RectmapNode getDrawRoot() {
        return drawRoot;
    }

    public void setDrawRoot(RectmapNode newValue) {
        this.drawRoot = newValue;
    }

    public void drawNodeBounds(Graphics2D g, RectmapNode selectedNode, Color color) {
        g.setColor(color);
        double scx = 0;
        double scy = 0;

        RectmapNode node = selectedNode;
        while (node != null) {
            scx += node.getX();
            scy += node.getY();
            node = node.getParent();
        }
        node = drawRoot;
        while (node != null) {
            scx -= node.getX();
            scy -= node.getY();
            node = node.getParent();
        }

        double px = scx * scaleFactorH + cx + insets.left;
        double py = scy * scaleFactorV + cy + insets.top;

        Rectangle.Double rect = new Rectangle2D.Double(px, py,
                selectedNode.width * scaleFactorH - 2,
                selectedNode.height * scaleFactorV - 2);
        g.draw(rect);
    }

    /**
     * Returns the node at the specified view coordinates.
     * @param px
     * @param py
     * @return
     */
    public RectmapNode getNodeAt(int px, int py) {
        return getNodeAt((px - cx - insets.left) / scaleFactorH, (py - cy - insets.top) / scaleFactorV);
    }

    /**
     * Returns the node at the specified draw coordinates.
     */
    public RectmapNode getNodeAt(double px, double py) {
        RectmapNode parent = drawRoot;
        while (parent != null) {
            px += parent.getX();
            py += parent.getY();
            parent = parent.getParent();
        }

        Rectangle2D.Double slimmed = new Rectangle2D.Double();
        if (root.contains(px, py)) {
            RectmapNode found = root;
            parent = found;
            do {
                parent = found;
                px -= parent.x;
                py -= parent.y;
                for (RectmapNode node : parent.children()) {
                    slimmed.setRect(node.x+(1/scaleFactorH), node.y+(1/scaleFactorV),
                            node.width-(2/scaleFactorH), node.height-(2/scaleFactorV));
//                    slimmed.setRect(node.x, node.y, node.width, node.height);
                    if (slimmed.contains(px, py)) {
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
