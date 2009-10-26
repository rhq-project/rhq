/**
 * @(#)CirclemapNode.java  1.0  Jan 16, 2008
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

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;
import ch.randelshofer.tree.TreePath;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The CirclemapNode class encapsulates a {@link TreeNode} whithin a
 * {@link CirclemapTree}.
 * <p>
 * It holds the radius of the data as an absolute value.
 * The location is held relative to the center of the parent data.
 * <p>
 * This data can layout its subtree in a space-filling circular treemap.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class CirclemapNode extends Circle {

    private CirclemapNode parent;
    private TreePath<TreeNode> dataNodePath;

    public CirclemapNode(CirclemapNode parent, TreeNode data) {
        this.parent = parent;
        this.dataNodePath = (parent == null) ? new TreePath<TreeNode>(data) : parent.getDataNodePath().pathByAddingChild(data);
    }

    public List<CirclemapNode> children() {
        return Collections.EMPTY_LIST;
    }

    public boolean isLeaf() {
        return true;
    }

    public TreePath<TreeNode> getDataNodePath() {
        return dataNodePath;
    }

    /**
     * Lays out the subtree starting at this data in a space-filling
     * circular treemap.
     */
    public void layout(NodeInfo info) {
        radius = Math.max(1, Math.sqrt(info.getWeight(dataNodePath) / Math.PI));
    //radius = 1;
    }

    public CirclemapNode getParent() {
        return parent;
    }

    public TreeNode getDataNode() {
        return dataNodePath.getLastPathComponent();
    }

    public String toString() {
        return dataNodePath.getLastPathComponent().toString();
    }
}
