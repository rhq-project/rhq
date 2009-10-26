/*
 * @(#)SunrayNode.java  1.0  September 18, 2007
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

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import java.util.*;

/**
 * The SunburstNode encapsulatets a {@link TreeNode} whithin a {@link SunrayTree}.
 * <p>
 * It holds the computed left, right, depth and internalDepth value of a node.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunrayNode {
    private TreePath<TreeNode> dataNodePath;
    /**
     * Nested Sets Tree: left preorder sequence number.
     */
    protected long left;
    /**
     * Nested Sets Tree: right preorder sequence number.
     */
    protected long right;

    /**
     * Internal depth within the depth.
     */
    private int internalDepth;

    protected int maxScatter;
    protected int totalScatter;

    private int maxDepth = -1;
    private int maxRayDepth = -1;

    private SunrayNode parent;

    /** Creates a new instance. */
    public SunrayNode(SunrayNode parent, TreeNode data, int depth) {
        this.parent = parent;
        this.dataNodePath = (parent == null) ? new TreePath<TreeNode>(data) : parent.getDataNodePath().pathByAddingChild(data);
    }

    public TreePath<TreeNode> getDataNodePath() {
        return dataNodePath;
    }

    public SunrayNode getRoot() {
        if (parent == null) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    public int getMaxScatter() {
        return maxScatter;
    }
    /*
    public int getMaxRayDepth() {
        return (getRoot().getMaxDepth() - depth);
    }*/

    public int getMaxDepth() {
        if (maxDepth == -1) {
        maxDepth = getMaxDepth(this, 1);
        }
        return maxDepth;
    }
    private int getMaxDepth(SunrayNode node, int depth) {
        int max = depth;
        for (SunrayNode child : node.children()) {
            max = Math.max(max, getMaxDepth(child, depth + 1));
        }
        return max;
    }
    public int getMaxRayDepth() {
        if (maxRayDepth == -1) {
        maxRayDepth = getMaxRayDepth(this, 1);
        }
        return maxRayDepth;
    }
    private int getMaxRayDepth(SunrayNode node, int depth) {
        int max = depth;
        if (children().size() > 0) {
            /*
         max += (children().size() > totalScatter) ?
             totalScatter / maxScatter + 1:
             children().size() / maxScatter + 2;*/
         max += totalScatter / maxScatter;
        }
        for (SunrayNode child : node.children()) {
            max = Math.max(max, getMaxDepth(child, depth + 1));
        }
        return max;
    }

    public void renumber() {
        renumber(4);
    }
    public void renumber(int maxScatter) {
        renumber(0, 0, 0, maxScatter, getMaxDepth() - 1);
    }

    protected int renumber(int depth, int number, int scatter, int maxScatter, int maxDepth) {
        this.internalDepth = scatter;
        this.maxScatter = maxScatter;
        this.totalScatter = maxDepth * maxScatter;
        if (scatter == totalScatter - 1) {
            left = number++;
            right = number;
        } else {
            left = number;
            right = number + 1;
        }
        return number;
    }

    public List<SunrayNode> children() {
        return Collections.EMPTY_LIST;
    }

    public void dump() {
        System.out.println(getDepth()+","+left+","+right+" "+toString());
        for (SunrayNode child : children()) {
            child.dump();
        }
    }

    public boolean isLeaf() {
        return dataNodePath.getLastPathComponent().isLeaf();
    }

    public long getLeft() {
        return left;
    }
    public long getRight() {
        return right;
    }
    public long getExtent() {
        return right - left;
    }


    public int getDepth() {
        return dataNodePath.getPathCount();
    }
    public int getScatter() {
        return internalDepth;
    }
    public void setScatter(int newValue) {
        internalDepth = newValue;
    }

    public boolean isDescendant(SunrayNode node) {
        return node.getLeft() >= getLeft() &&
                node.getRight() <= getRight() &&
                node.getDepth() >= getDepth();
    }

    public SunrayNode findNode(int depth, long number, int scatter) {
        if (getLeft() <= number && getRight() > number) {
            if (isLeaf()) {
                if (this.internalDepth == scatter + maxScatter * depth) {
                    return this;
                }
            } else {
                if (depth == 0) {
                    return this;
                }
            }
            for (SunrayNode child : children()) {
                SunrayNode found = child.findNode(depth - 1, number, scatter);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
