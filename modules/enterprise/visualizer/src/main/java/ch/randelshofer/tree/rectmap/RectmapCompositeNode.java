/**
 * @(#)RectmapCompositeNode.java  1.1  2008-06-24
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

import ch.randelshofer.tree.*;
import java.awt.geom.*;
import java.util.*;

/**
 * RectmapCompositeNode.
 *
 * @author Werner Randelshofer
 * @version 1.1 2008-06-24 Replaced recursion in squarify algorithm by iteration.
 * <br>1.0 Jan 16, 2008 Created.
 */
public class RectmapCompositeNode extends RectmapNode {

    private ArrayList<RectmapNode> children;

    /** Creates a new instance. */
    public RectmapCompositeNode(RectmapNode parent, TreeNode data) {
        super(parent, data);

        children = new ArrayList<RectmapNode>();
        for (TreeNode c : data.children()) {
            RectmapNode n;
            if (c.isLeaf()) {
                children.add(n = new RectmapNode(this, c));
            } else {
                children.add(n = new RectmapCompositeNode(this, c));
            }
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public List<RectmapNode> children() {
        return Collections.unmodifiableList(children);
    }

    public void updateCumulatedWeight(NodeInfo info) {
        cumulatedWeight = 0;
        for (RectmapNode node : children) {
            node.updateCumulatedWeight(info);
            cumulatedWeight += node.getCumulatedWeight();
        }
        cumulatedWeight = Math.max(1, cumulatedWeight);
    }

    /**
     * Lays out the children of this composite node using the <a
     * href="http://www.win.tue.nl/~vanwijk/stm.pdf">squarify algorithm</a>
     * by Bruls, Huizing, and van Wijk, 2000.
     *
     * @param info
     */
    public void layout() {
        super.layout();
        if (children.size() > 0) {
            ArrayList<RectmapNode> remaining = new ArrayList<RectmapNode>(children);
            Collections.sort(remaining, RectmapCumulatedSizeComparator.getDescendingInstance());

            Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
                    this.width,
                    this.height);
            squarify(remaining, 0, rect);

            for (RectmapNode child : children) {
                child.layout();
            }
        }
    }

    /**
     * Lays out nodes in the supplied rectangle using the squarify algorithm.
     * The area of the rectangle must be equal or greater than the weight of
     * the nodes.
     *
     * @param info
     * @param nodes
     * @param rect Is changed by this method.
     */
    private void squarify(ArrayList<RectmapNode> nodes, int start, Rectangle2D.Double rect) {
        while (start < nodes.size()) {
            if (rect.width >= rect.height) {
                start = layoutColumn(nodes, start, rect);
            } else {
                start = layoutRow(nodes, start, rect);
            }
        }
    }

    /**
     * Lays out a single column of nodes in the supplied rectangle using the
     * squarify algorithm. The nodes are placed on the left side of the rectangle.
     * If not all nodes fit into the column, squarify is called with the remaining
     * nodes and the remaining rectangle.
     *
     * @param info
     * @param nodes
     * @param rect is changed by this method
     * @return Returns the index of last node + 1 which fitted into the column.
     */
    private int layoutColumn(ArrayList<RectmapNode> nodes, int start, Rectangle2D.Double rect) {
        double ratio = worstAspectRatio(nodes, start, start, rect.height);
        double totalWeight = nodes.get(start).getCumulatedWeight();
        int to;
        for (to = start + 1; to <
                nodes.size(); to++) {
            double newRatio = worstAspectRatio(nodes, start, to, rect.height);
            if (newRatio > ratio) {
                break;
            }

            ratio = newRatio;
            totalWeight +=
                    nodes.get(to).getCumulatedWeight();
        }

        double ny = rect.y;
        double nw = totalWeight / rect.height;
        for (int i = start; i <
                to; i++) {
            RectmapNode node = nodes.get(i);
            node.width = nw;
            node.height = node.getCumulatedWeight() / nw;
            node.x = rect.x;
            node.y = ny;
            ny +=
                    node.height;
        }
        /*
        if (to < nodes.size()) {
            squarify(nodes, to, new Rectangle2D.Double(rect.x + nw, rect.y, rect.width - nw, rect.height));
        }
        */
        rect.x += nw;
        rect.width -= nw;
        return to;
    }

    /**
     * Lays out a single row of nodes in the supplied rectangle using the
     * squarify algorithm. The nodes are placed on the bottom side of the rectangle.
     * If not all nodes fit into the row, squarify is called with the remaining
     * nodes and the remaining rectangle.
     *
     * @param info
     * @param nodes
     * @param rect is changed by this method
     * @return Returns the index of last node + 1 which fitted into the column.
     */
    private int layoutRow(ArrayList<RectmapNode> nodes, int start, Rectangle2D.Double rect) {
        double ratio = worstAspectRatio(nodes, start, start, rect.width);
        double totalWeight = nodes.get(start).getCumulatedWeight();
        int to;
        for (to = start + 1; to <
                nodes.size(); to++) {
            double newRatio = worstAspectRatio(nodes, start, to, rect.width);
            if (newRatio > ratio) {
                break;
            }

            ratio = newRatio;
            totalWeight +=
                    nodes.get(to).getCumulatedWeight();
        }

        double nx = rect.x;
        double nh = totalWeight / rect.width;
        for (int i = start; i <
                to; i++) {
            RectmapNode node = nodes.get(i);
            node.height = nh;
            node.width = node.getCumulatedWeight() / nh;
            node.x = nx;
            node.y = rect.y + rect.height - nh;
            nx +=
                    node.width;
        }
        /*
        if (to < nodes.size()) {
        squarify(nodes, to, new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height - nh));
        }*/
        rect.height -= nh;
        return to;
    }

    /**
     * Returns the aspect ratio of the node, which will have the worst
     * ratio, if the supplied nodes are laid out on a line with the specified nw.
     *
     * @param nodes A collection of nodes
     * @param from Index of the first node to consider
     * @param to Index of the last node to consider
     * @param nw length of the line.
     * @return Worst aspect ratio.
     */
    private double worstAspectRatio(ArrayList<RectmapNode> nodes, int from, int to, double w) {
        double s = 0;
        double rmax = java.lang.Double.MIN_VALUE;
        double rmin = java.lang.Double.MAX_VALUE;
        for (int i = from; i <=
                to; i++) {
            double nodeWeight = nodes.get(i).getCumulatedWeight();
            s +=
                    nodeWeight;
            rmax =
                    Math.max(rmax, nodeWeight);
            rmin =
                    Math.min(rmin, nodeWeight);
        }

        return Math.max((w * w * rmax) / (s * s), (s * s) / (w * w * rmin));
    }
}

