/**
 * @(#)RectmapNode.java  1.0  Jan 16, 2008
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
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

/**
 * The RectmapNode encapsulatets a {@link TreeNode} whithin a {@link RectmapTree}.
 * <p>
 * It holds the width and height of the node as an absolute value.
 * The location is held relative to the location of the parent node.
 * <p>
 * This node can layout its subtree in a space-filling rectangular
 * treemap.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class RectmapNode extends Rectangle2D.Double {

    protected RectmapNode parent;
    protected TreePath<TreeNode> dataNodePath;
    protected double cumulatedWeight = -1;

    public RectmapNode(RectmapNode parent, TreeNode data) {
        this.parent = parent;
        this.dataNodePath = (parent == null) ? new TreePath<TreeNode>(data) : parent.getDataNodePath().pathByAddingChild(data);
    }

    public List<RectmapNode> children() {
        return Collections.EMPTY_LIST;
    }

    public boolean isLeaf() {
        return true;
    }
    public TreePath<TreeNode> getDataNodePath() {
        return dataNodePath;
    }


    /**
     * Lays out the subtree starting at this node in a space-filling
     * rectangular treemap.
     * <p>
     * Note: You must call updateCumulatedWeight before you can layout a
     * node.
     *
     */
    public void layout() {
        if (parent == null) {
            width = height = Math.max(1, Math.sqrt(getCumulatedWeight()));
            x = y = 0;
        }
    }

    public RectmapNode getParent() {
        return parent;
    }

    public void updateCumulatedWeight(NodeInfo info) {
       cumulatedWeight = Math.max(1, info.getWeight(dataNodePath));
    }
    public double getCumulatedWeight() {
        return cumulatedWeight;
    }
    public String toString() {
        return dataNodePath.getLastPathComponent()+" [x:"+x+",y:"+y+",w:"+width+",h:"+height+"]";
    }
}
