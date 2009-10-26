/**
 * @(#)RectmapTree.java  1.0  Jan 16, 2008
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

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;

/**
 * RectmapTree lays out a tree structure in a space-filling rectangular treemap.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class RectmapTree {

    private RectmapNode root;
    private NodeInfo info;

    /** Creates a new instance. */
    public RectmapTree(TreeNode root, NodeInfo info) {
        this.info = info;
        if (root.isLeaf()) {
        this.root = new RectmapNode(null, root);
        } else {
        this.root = new RectmapCompositeNode(null, root);
        }
        info.init(root);
        this.root.updateCumulatedWeight(info);
        this.root.layout();
    }

    public NodeInfo getInfo() {
        return info;
    }

    public RectmapNode getRoot() {
        return root;
    }
}
