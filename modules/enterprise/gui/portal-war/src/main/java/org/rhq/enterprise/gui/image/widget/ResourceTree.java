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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import org.rhq.enterprise.gui.image.ImageUtil;
import org.rhq.enterprise.gui.image.Line;
import org.rhq.enterprise.gui.image.WebImage;
import org.rhq.enterprise.gui.image.data.IResourceTreeNode;
import org.rhq.enterprise.gui.image.data.ITreeNode;

public class ResourceTree extends WebImage {
    //******** Private Static Variables *********
    private static final int NODE_WHITESPACE = 10;
    private static final int ICON_WHITESPACE = 2;
    private static final int LEVEL_WHITESPACE = 14;
    private static final int LEVEL_DESCENT = (LEVEL_WHITESPACE / 2);
    private static final int LEVEL_INDENT = 0; //(NODE_WHITESPACE / 2);

    private static final int THIN_LINE = 1;
    private static final int THICK_LINE = 2;

    private static int ICON_HEIGHT; // Set in static constructor
    private static int ICON_WIDTH; // Set in static constructor

    private static BufferedImage IMG_RESOURCE;
    private static BufferedImage IMG_AUTO_GROUP;
    private static BufferedImage IMG_CLUSTER;

    //******** Protected Static Variables *******
    protected static final Font NAME_FONT = new Font(DEFAULT_BOLD_TYPEFACE, Font.BOLD, 11);
    protected static final Font DESC_FONT = new Font(DEFAULT_PLAIN_TYPEFACE, Font.PLAIN, 8);
    protected static final Font SELECTED_FONT = new Font(DEFAULT_BOLD_TYPEFACE, Font.BOLD, 11);

    protected static final Color NAME_COLOR = new Color(0x00, 0x31, 0x9C);
    protected static final Color DESC_COLOR = Color.BLACK;
    protected static final Color LINE_COLOR = new Color(0x90, 0x90, 0x90); //Color(0x80, 0x80, 0x80);
    protected static final Color SOFT_LINE_COLOR = new Color(0xC0, 0xC0, 0xC0);
    protected static final Color SELECTED_COLOR = new Color(0xDE, 0x65, 0x2D);
    protected static final Color TRANSPARENT_COLOR = new Color(3, 3, 3);

    protected static final Stroke LINE_STROKE = new BasicStroke(THICK_LINE, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER);

    //******** Private Instance Variables *******
    private FontMetrics m_nameMetrics; // Set in preInit()
    private FontMetrics m_descMetrics; // Set in preInit()

    private Vector m_root = new Vector();
    private Vector m_lines = new Vector();

    private Dimension m_imageSize = new Dimension();
    private int m_yDividerLine;

    //************** Test Variables **************
    // Change the next line assigned from null to the
    // vector to get debug whitespace rectangles.
    private Vector m_testRects = null; //new Vector();

    //************* Static Constructor ***********
    static {
        try {
            IMG_RESOURCE = ImageUtil.loadImage("images/icon_resource.gif");
            IMG_AUTO_GROUP = ImageUtil.loadImage("images/icon_auto-group.gif");
            IMG_CLUSTER = ImageUtil.loadImage("images/icon_cluster.gif");

            ICON_HEIGHT = IMG_CLUSTER.getHeight();
            ICON_WIDTH = IMG_CLUSTER.getWidth();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //*************** Constructors **************
    public ResourceTree(int width) {
        super(width, 1);
    }

    public ResourceTree(int width, int height) {
        super(width, height);
    }

    //************* Public Methods **************
    public void addLevel(IResourceTreeNode[] resources) {
        m_root.add(resources);
    }

    public Dimension getImageSize() {
        return m_imageSize;
    }

    public IResourceTreeNode[][] getLevels() {
        IResourceTreeNode[][] levels = new IResourceTreeNode[m_root.size()][];
        for (int i = 0; i < levels.length; ++i) {
            levels[i] = (IResourceTreeNode[]) m_root.get(i);
        }

        return levels;
    }

    public void calculateCoordinates() {
        this.calcTree();
    }

    public void reset() {
        this.m_imageSize.setSize(0, 0);

        Iterator iter = m_root.iterator();
        while (iter.hasNext() == true) {
            ITreeNode[] nodes = (ITreeNode[]) iter.next();
            this.resetNodes(nodes);
        }

        this.m_lines.clear();
    }

    private void resetNodes(ITreeNode[] nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("The \'nodes\' argument cannot be null");
        }

        for (int i = 0; i < nodes.length; i++) {
            ITreeNode node = nodes[i];
            node.reset();

            if (node.hasUpChildren() == true) {
                this.resetNodes(node.getUpChildren());
            }

            if (node.hasDownChildren() == true) {
                this.resetNodes(node.getDownChildren());
            }
        }
    }

    public String toString() {
        StringBuffer res = new StringBuffer();

        res.append(this.getClass().getName()).append('[').append("Nodes=").append(m_root).append(',').append("Lines=")
            .append(m_lines).append(',').append("Size=").append(m_imageSize).append(',').append("Divider=").append(
                m_yDividerLine).append(']');

        return res.toString();
    }

    //*********** Protected Methods *************
    protected void addTestRect(Rectangle rect) {
        if (m_testRects != null) {
            m_testRects.add(new Rectangle(rect));
        }
    }

    protected void draw(Graphics2D g) {
        super.draw(g);

        //        g.setPaint(TRANSPARENT_COLOR);
        //        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        this.drawLines(g);
        this.drawNodes(g);
    }

    protected void postInit(Graphics2D g) {
        this.font = NAME_FONT;
        this.textColor = NAME_COLOR;
    }

    protected void preInit() {
        this.frameImage = true;
        this.setBorder(7);

        BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) im.getGraphics();

        m_nameMetrics = g.getFontMetrics(NAME_FONT);
        m_descMetrics = g.getFontMetrics(DESC_FONT);

        g.dispose();
        im.flush();

        Rectangle imageSize = this.calcTree();
        if (imageSize != null) {
            // if the tree is non-existent, let's avoid an IllegalArgumentException
            if (imageSize.width <= 0) {
                imageSize.width = 1;
            }

            if (imageSize.height <= 0) {
                imageSize.height = 1;
            }

            this.width = imageSize.width;
            this.height = imageSize.height;
        }
    }

    //*********** Private Methods ***************
    private Rectangle calcTree() {
        if (m_imageSize.width != 0) {
            return null;
        }

        int xLevelLine = 0;
        int yLevelLine = this.topBorder;
        int yFirstLevel = 0;
        int cyLastLevel = 0;
        int cxBorder = this.leftBorder + this.rightBorder;
        int[] x = null;
        int[] y = null;
        int level;
        CalcResult childResult = null;
        IResourceTreeNode selected = null;

        // Width and height of the entire image
        Rectangle result = new Rectangle(0, 0, cxBorder, this.topBorder + this.bottomBorder);

        m_lines.clear();

        boolean topImage = m_root.size() > 1;

        if ((topImage == false) && (m_root.size() > 0)) {
            ITreeNode[] nodes = (ITreeNode[]) m_root.get(0);

            topImage = ((nodes.length > 1) || ((nodes[0].hasDownChildren() == false) && (nodes[0].hasUpChildren() == false)));
        }

        // Draw top image
        if (topImage == true) {
            Iterator iter = m_root.iterator();
            for (level = 0; iter.hasNext() == true; level++) {
                IResourceTreeNode[] nodes = (IResourceTreeNode[]) iter.next();
                xLevelLine = this.leftBorder + (LEVEL_INDENT * level);
                yLevelLine = yLevelLine + cyLastLevel;

                childResult = this.calcDownChildren(nodes, new Rectangle(xLevelLine, yLevelLine, this.width, 0));
                this.addTestRect(childResult.bounding);

                if (selected == null) {
                    selected = childResult.selected;
                }

                cyLastLevel = childResult.bounding.height + LEVEL_DESCENT;
                ;

                result.height += cyLastLevel;
                result.width = Math.max(result.width, childResult.bounding.width + cxBorder);

                if (level == 0) {
                    yFirstLevel = childResult.yLevelLine;
                }
            }

            if (level > 1) {
                m_lines.add(new Line(this.leftBorder, yFirstLevel - 1, xLevelLine, childResult.yLevelLine + 1));
            }
        } else if (m_root.size() > 0) {
            selected = ((IResourceTreeNode[]) m_root.get(0))[0];
        }

        // Draw bottom image
        if ((selected != null) && ((selected.hasDownChildren() == true) || (selected.hasUpChildren() == true))) {
            Rectangle boundry = new Rectangle();
            boundry.x = this.leftBorder;
            boundry.y = this.topBorder + ((topImage == true) ? (yLevelLine + cyLastLevel) : 0);
            boundry.width = this.width;

            // Add horizontal line
            if (topImage == true) {
                m_yDividerLine = boundry.y - LEVEL_DESCENT;
                result.height += LEVEL_DESCENT;
            }

            childResult = this.calcNodeTree(selected, boundry);
            this.addTestRect(childResult.bounding);

            result.height += childResult.bounding.height;
            result.width = Math.max(result.width, childResult.bounding.width + cxBorder);
        } else {
            // Remove the level that was added for whitespace between the top
            // and bottom image.
            result.height -= LEVEL_DESCENT;
            ;
        }

        // Get rid of the bottom line descent so that the top and bottom borders
        // look the same size.
        result.height -= this.m_descMetrics.getDescent();
        m_imageSize.width = result.width;
        m_imageSize.height = result.height;

        return result;
    }

    private CalcResult calcNodeTree(IResourceTreeNode node, Rectangle boundry) {
        CalcResult result = null;
        Rectangle nodeRect;
        int x;
        int y;

        int type = node.getType();

        if ((type == IResourceTreeNode.AUTO_GROUP) || (type == IResourceTreeNode.CLUSTER)) {
            Rectangle parentRect = null;

            // Draw the parent node for the group if we have it
            if (node.hasUpChildren() == true) {
                parentRect = this.calcNode((IResourceTreeNode) node.getUpChildren()[0], boundry.x, boundry.y);

                boundry.y += (parentRect.height );
                boundry.grow(0, -LEVEL_DESCENT);

                //                result.width  += nodeRect.width;
            }

            // Draw the selected node
            nodeRect = this.calcNode(node, boundry.x, boundry.y);
            this.addTestRect(nodeRect);

            x = boundry.x + (ICON_WIDTH / 2);
            y = boundry.y + (ICON_HEIGHT / 2);

            // Add the line from the parent node to this node if there is a
            // parent node
            if (parentRect != null) {
                m_lines.add(new Line(parentRect.x + (ICON_WIDTH / 2), parentRect.y + (ICON_HEIGHT / 2), x, y));
            }

            // Add the lines for this node
            m_lines.add(new Line(x, y, x, boundry.y + (LEVEL_WHITESPACE * 2) + 12));

            y = nodeRect.y + nodeRect.height + 3;

            m_lines.add(new Line(x, y, x + (NODE_WHITESPACE * 2), y));

            boundry.grow(-NODE_WHITESPACE, 0);
            boundry.y = nodeRect.y + nodeRect.height + LEVEL_DESCENT;

            // Draw the down children
            result = this.calcChildren((IResourceTreeNode[]) node.getDownChildren(), boundry, true, false, true);
            this.addTestRect(result.bounding);

            result.bounding.width = Math.max(result.bounding.width + ICON_WIDTH, nodeRect.width);
            result.bounding.height += nodeRect.height + LEVEL_DESCENT;

            if (parentRect != null) {
                result.bounding.height += (parentRect.height + LEVEL_DESCENT);
            }
        } else {
            IResourceTreeNode[] children;

            // Calculate Up Children
            children = (IResourceTreeNode[]) node.getUpChildren();

            if ((children != null) && (children.length > 0)) {
                boundry.grow(-NODE_WHITESPACE, 0);
                result = this.calcUpChildren(children, boundry);
                this.addTestRect(result.bounding);

                result.bounding.height += LEVEL_DESCENT;

                boundry.x -= NODE_WHITESPACE;
                boundry.y += result.bounding.height;
                result.bounding.x -= NODE_WHITESPACE;
                result.bounding.width += NODE_WHITESPACE;
            }

            // Calc Node
            nodeRect = this.calcNode(node, boundry.x, boundry.y);
            this.addTestRect(nodeRect);

            if (result != null) {
                if (nodeRect.width > result.bounding.width) {
                    result.bounding.width += (nodeRect.width - result.bounding.width);
                }

                m_lines.add(new Line(nodeRect.x + (ICON_WIDTH / 2) + 1, nodeRect.y + (ICON_HEIGHT / 2), nodeRect.x
                    + NODE_WHITESPACE + 1, result.yLevelLine));
            } else {
                result = new CalcResult();
                result.bounding = new Rectangle();
                result.bounding.width += nodeRect.width;
                result.selected = node;
            }

            // Calculate Down Children
            children = (IResourceTreeNode[]) node.getDownChildren();

            if ((children != null) && (children.length > 0)) {
                boundry.grow(-NODE_WHITESPACE, 0);
                boundry.y += nodeRect.height + LEVEL_DESCENT;

                CalcResult downResult = this.calcDownChildren(children, boundry);
                this.addTestRect(downResult.bounding);

                m_lines.add(new Line(nodeRect.x + (ICON_WIDTH / 2) + 1, nodeRect.y + (ICON_HEIGHT / 2), nodeRect.x
                    + NODE_WHITESPACE + 1, downResult.yLevelLine));

                if (result != null) {
                    result.bounding.height += downResult.bounding.height;
                    if (downResult.bounding.width > result.bounding.width) {
                        result.bounding.width = downResult.bounding.width + downResult.bounding.x - nodeRect.x;
                    }

                    // Look for children attached to children if there is only
                    // one child at the initial level.
                    if (children.length == 1) {
                        IResourceTreeNode[] down = (IResourceTreeNode[]) children[0].getDownChildren();

                        if ((down != null) && (down.length > 0)) {
                            boundry.grow(-(NODE_WHITESPACE * 2), -(downResult.bounding.height + LEVEL_DESCENT));

                            downResult = this.calcDownChildren((IResourceTreeNode[]) down, boundry);
                            this.addTestRect(downResult.bounding);

                            Rectangle[] rects = children[0].getRectangles();
                            nodeRect = rects[rects.length - 1];

                            m_lines.add(new Line(nodeRect.x + (ICON_WIDTH / 2) + 1, nodeRect.y + (ICON_HEIGHT / 2) + 1,
                                nodeRect.x + NODE_WHITESPACE + 1, downResult.yLevelLine + 1));

                            result.bounding.height += downResult.bounding.height + LEVEL_DESCENT;
                            if (downResult.bounding.width > result.bounding.width) {
                                result.bounding.width = downResult.bounding.width + (NODE_WHITESPACE * 3);
                            }
                        }
                    }
                } else {
                    result = downResult;
                }

                result.bounding.height += LEVEL_DESCENT;
            }

            // Add height of selected node to the result height
            result.bounding.height += nodeRect.height;
        }

        return result;
    }

    private CalcResult calcChildren(IResourceTreeNode[] nodes, Rectangle boundry, boolean wrap, boolean calcLines,
        boolean down) {
        int yHorzLine;
        int xNode = 0;
        int yNode = 0;
        int cxLastNode = 0;
        int cyTallestNode = 0;
        int xLevelLine = boundry.x;
        int yLevelLine = boundry.y;
        int wrapLevel;
        int xFirstNode = 0;
        int yFirstNode = 0;
        int xLastNode = 0;
        int yLastNode = 0;
        int nodeIndex = 0;
        int lastWrapIndex = 0;

        Rectangle nodeRect = null;
        IResourceTreeNode node = null;

        CalcResult result = new CalcResult();
        result.bounding = new Rectangle(boundry.x, boundry.y, 0, 0);

        FontMetrics nameMetrics = this.getFontMetrics();
        Rectangle[] rects = new Rectangle[nodes.length];
        Vector levelLines = new Vector();

        for (wrapLevel = ((wrap == true) ? 1 : 0); nodeIndex < nodes.length; wrapLevel++) {
            if (down == true) {
                yHorzLine = (wrapLevel > 1) ? (yLastNode + cyTallestNode + LEVEL_DESCENT) : yLevelLine;
            } else {
                yHorzLine = (wrapLevel > 1) ? (yLastNode - cyTallestNode - LEVEL_WHITESPACE - LEVEL_DESCENT)
                    : yLevelLine;
            }

            xNode = xLevelLine;
            if (calcLines == true) {
                xNode += NODE_WHITESPACE;
            }

            // Calc Level and Node Position
            if (nodes.length == 1) {
                yNode = yHorzLine;
                if (down == false) {
                    yNode += LEVEL_DESCENT;
                }

                yHorzLine += ICON_HEIGHT / 2;

                if (wrapLevel == 0) {
                    xNode -= ICON_WHITESPACE;
                    yLevelLine = yHorzLine;
                }
            } else {
                yNode = yHorzLine + ((calcLines == true) ? LEVEL_DESCENT : 0);
            }

            // Reset the last node height for the level to zero
            cyTallestNode = 0;

            int levelStart;
            for (levelStart = nodeIndex; nodeIndex < nodes.length; nodeIndex++) {
                node = nodes[nodeIndex];

                // Set the selected parameter
                if (node.isSelected() == true) {
                    if (result.selected == null) {
                        result.selected = node;
                    }
                }

                nodeRect = this.calcNode(node, xNode, yNode, false);
                //this.addTestRect(nodeRect);

                // Add the rect to an array to set after the loop. This allows
                // us to set the node rects transactionally.
                rects[nodeIndex] = nodeRect;

                // Determine if the node can fit in the image width. If not,
                // we should wrap the node to the next line, unless it also
                // won't fit on the next line. If it can't fit on this line
                // or the next line then the right side is cutoff.
                if (((nodeRect.x + nodeRect.width) > (boundry.x + boundry.width)) && (nodeIndex != lastWrapIndex)) {
                    // If we didn't already know we need to wrap, then we must
                    // start all over again because the wrapping has a differnt
                    // layout.
                    if (wrap == false) {
                        boundry.grow(-NODE_WHITESPACE, 0);
                        result = this.calcChildren(nodes, boundry, true, true, down);
                        boundry.grow(NODE_WHITESPACE, 0);
                        return result;
                    }

                    // If we knew then we calculate this node again on a new
                    // line. The break will automatically start a new line.
                    lastWrapIndex = nodeIndex;
                    break;
                }

                // Keep track of the height of the talest node
                cxLastNode = nodeRect.width;
                cyTallestNode = Math.max(cyTallestNode, nodeRect.height);

                // Setup for the next node
                if (nodeIndex == 0) {
                    xFirstNode = xNode;
                    yFirstNode = yNode;
                }

                xLastNode = xNode;
                yLastNode = yNode;

                xNode += (nodeRect.width + NODE_WHITESPACE);
            }

            // Swap the nodes left to right
            if (down == false) {
                this.swapNodes(rects, levelStart, nodeIndex - 1);
            }

            // Add Horizontal Level Line
            if (calcLines == true) {
                Line line = new Line();
                line.x1 = xLevelLine - ((wrapLevel == 1) ? NODE_WHITESPACE : 0);
                line.y1 = yHorzLine;

                if (nodes.length > 1) {
                    line.x2 = ((down == true) ? xLastNode : rects[levelStart].x) - (NODE_WHITESPACE / 2)
                        + (ICON_WIDTH / 2) + 1;
                } else {
                    line.x2 = xLastNode + (ICON_WIDTH / 2);
                }

                levelLines.add(line);
            }

            // Update the tree width
            result.bounding.width = Math.max(result.bounding.width, xLastNode - xLevelLine + cxLastNode
                + ((wrapLevel == 1) ? NODE_WHITESPACE : 0));
            result.bounding.height += ((nodeIndex <= 1) && (wrap == false)) ? nodeRect.height
                : (nodeRect.height + ((calcLines == true) ? LEVEL_DESCENT : 0));
        }

        if (--wrapLevel == 0) {
            wrapLevel = 1;
        }

        // Set all the node rectangles, transactionally
        for (int i = 0; i < nodes.length; i++) {
            Rectangle rect = rects[i];

            if (down == false) {
                if (wrap == true) {
                    rect.y += ((cyTallestNode + LEVEL_WHITESPACE) * (wrapLevel - 1)) - LEVEL_DESCENT;
                } else {
                    rect.y -= LEVEL_DESCENT;
                }
            }

            nodes[i].addRectangle(rect.x, rect.y, rect.width, rect.height);
            this.addTestRect(rect);
        }

        // Set all the level lines
        Line line = null;
        Iterator iter = levelLines.iterator();
        for (int i = 0; iter.hasNext() == true; i++) {
            line = (Line) iter.next();

            if ((down == false) && (nodes.length > 1)) {
                line.y1 += (((cyTallestNode + LEVEL_WHITESPACE) * wrapLevel) - LEVEL_DESCENT);
            }

            line.y2 = line.y1;
            m_lines.add(line);

            if (i == 0) {
                result.yLevelLine = line.y1;
            }
        }

        // Add wrap line if necessary
        if ((calcLines == true) && (wrapLevel >= 1)) {
            m_lines.add(new Line(xLevelLine + 1, result.yLevelLine, xLevelLine + 1, line.y1));
        }

        // Add Node Lines
        if ((calcLines == true) && (nodes.length > 1)) {
            for (int i = 0; i < rects.length; i++) {
                Rectangle rect = rects[i];

                line = new Line();
                line.x1 = rect.x;
                line.y2 = rect.y + 2;

                if (down == true) {
                    line.y1 = rect.y - LEVEL_DESCENT;
                    line.x2 = rect.x + (ICON_WIDTH / 2) - 1;
                } else {
                    line.y1 = rect.y + LEVEL_DESCENT + rect.height;
                    line.x2 = rect.x + (ICON_WIDTH / 2) + 1;
                }

                m_lines.add(line);
            }
        }

        // Update the tree width and height
        if (calcLines == false) {
            result.bounding.width -= NODE_WHITESPACE;
        }

        if (wrapLevel > 0) {
            result.bounding.height += (wrapLevel - 1) * LEVEL_DESCENT;
        }

        return result;
    }

    private CalcResult calcDownChildren(IResourceTreeNode[] nodes, Rectangle boundry) {
        return this.calcChildren(nodes, boundry, false, true, true);
    }

    private CalcResult calcUpChildren(IResourceTreeNode[] nodes, Rectangle boundry) {
        return this.calcChildren(nodes, boundry, false, true, false);
    }

    private Rectangle calcNode(IResourceTreeNode node, int x, int y) {
        return this.calcNode(node, x, y, true);
    }

    private Rectangle calcNode(IResourceTreeNode node, int x, int y, boolean set) {
        int cxImg;
        int cyImg;
        int cxNode;
        int cyNode;

        String name = node.getName();
        String desc = node.getDescription();
        BufferedImage img = getIcon(node.getType());

        cxImg = (img != null) ? ICON_WIDTH : 0;
        cyImg = (img != null) ? ICON_HEIGHT : 0;

        // Calc Node Width
        int cxName = (name != null) ? m_nameMetrics.stringWidth(name) : 0;
        int cxDesc = (desc != null) ? m_descMetrics.stringWidth(desc) : 0;
        cxNode = Math.max(cxName, cxDesc);
        cxNode += (cxImg + ICON_WHITESPACE);

        // Calc Node Height
        cyNode = (name != null) ? m_nameMetrics.getAscent() : 0;
        cyNode += (desc != null) ? m_descMetrics.getAscent() : 0;

        // Save the Node Rectangle for anyone interested in creating
        // an image map on top of this image
        if (set == true) {
            node.addRectangle(x, y, cxNode, cyNode);
        }

        return new Rectangle(x, y, cxNode, cyNode);
    }

    private void drawLines(Graphics2D g) {
        Stroke strokeOrig = g.getStroke();
        g.setStroke(LINE_STROKE);
        g.setColor(LINE_COLOR);

        Iterator iter = m_lines.iterator();

        while (iter.hasNext() == true) {
            Line line = (Line) iter.next();
            g.drawLine(line.x1, line.y1, line.x2, line.y2);
        }

        g.setStroke(strokeOrig);
    }

    private void drawNodes(Graphics2D g) {
        Iterator iter = m_root.iterator();
        while (iter.hasNext() == true) {
            IResourceTreeNode[] nodes = (IResourceTreeNode[]) iter.next();
            this.drawNodes(g, nodes, NAME_FONT);
        }
    }

    private void drawNodes(Graphics2D g, IResourceTreeNode[] nodes, Font nameFont) {
        int cyAscent = g.getFontMetrics(nameFont).getAscent();

        for (int i = 0; i < nodes.length; i++) {
            IResourceTreeNode node = nodes[i];

            String name = node.getName();
            String desc = node.getDescription();
            BufferedImage img = getIcon(node.getType());
            Rectangle[] rects = node.getRectangles();

            if (rects == null) {
                continue;
            }

            for (int r = 0; r < rects.length; r++) {
                Rectangle rect = rects[r];

                int xText = rect.x + ICON_WHITESPACE;
                int yText = rect.y + cyAscent - 3;

                if (img != null) {
                    g.drawImage(img, rect.x, rect.y, null);
                    xText += img.getWidth();
                }

                if (name != null) {
                    g.setColor((node.isSelected() == true) ? SELECTED_COLOR : this.textColor);
                    g.setFont((node.isSelected() == true) ? SELECTED_FONT : nameFont);
                    g.drawString(name, xText, yText);
                }

                if (desc != null) {
                    g.setColor(DESC_COLOR);
                    g.setFont(DESC_FONT);
                    g.drawString(desc, xText, yText + m_descMetrics.getAscent());
                }
            }

            if (m_yDividerLine > 0) {
                g.setColor(DEFAULT_BORDER_COLOR);
                g.drawLine(0, m_yDividerLine, this.width - 1, m_yDividerLine);
            }

            if (node.hasUpChildren() == true) {
                this.drawNodes(g, (IResourceTreeNode[]) node.getUpChildren(), NAME_FONT);
            }

            if (node.hasDownChildren() == true) {
                this.drawNodes(g, (IResourceTreeNode[]) node.getDownChildren(), NAME_FONT);
            }

            // Draw test rectangles if they're turned on
            if (m_testRects != null) {
                g.setColor(DEFAULT_BORDER_COLOR);

                Iterator iter = m_testRects.iterator();
                while (iter.hasNext() == true) {
                    Rectangle rect = (Rectangle) iter.next();
                    g.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
            }
        }
    }

    private IResourceTreeNode getNodeWithUpChildren(IResourceTreeNode[] nodes) {
        IResourceTreeNode result = null;

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].hasUpChildren() == true) {
                result = nodes[i];
            }
        }

        return result;
    }

    private void swapNodes(Rectangle[] rects, int start, int end) {
        if (((end - start) < 1) || (rects.length <= 1)) {
            return;
        }

        int x = rects[start].x;
        int space = rects[start + 1].x - (x + rects[start].width);

        for (int i = end; i >= start; i--) {
            rects[i].x = x;
            x += (rects[i].width + space);
        }
    }

    //*********** Static Methods ****************
    public static BufferedImage getIcon(int type) {
        BufferedImage result;

        switch (type) {
        case IResourceTreeNode.RESOURCE: {
            result = IMG_RESOURCE;
            break;
        }

        case IResourceTreeNode.AUTO_GROUP: {
            result = IMG_AUTO_GROUP;
            break;
        }

        case IResourceTreeNode.CLUSTER: {
            result = IMG_CLUSTER;
            break;
        }

        default: {
            result = null;
        }
        }

        return result;
    }

    //*********** Inner Classes *****************
    private class CalcResult {
        private Rectangle bounding;
        private IResourceTreeNode selected;
        private int yLevelLine;
    }
}