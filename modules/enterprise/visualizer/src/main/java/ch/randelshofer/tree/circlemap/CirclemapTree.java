/**
 * @(#)CirclemapTree.java  1.0  Jan 16, 2008
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

/**
 * CirclemapTree lays out a tree structure in a space-filling circular treemap.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class CirclemapTree {

    private CirclemapNode root;
    private NodeInfo info;

    /** Creates a new instance. */
    public CirclemapTree(TreeNode root, NodeInfo info) {
        this.info = info;
        if (root.isLeaf()) {
        this.root = new CirclemapNode(null, root);
        } else {
        this.root = new CirclemapCompositeNode(null, root);
        }
        info.init(root);
        long start = System.currentTimeMillis();
        this.root.layout(info);
        long end = System.currentTimeMillis();
        System.out.println("CirclemapTree layout elapsed "+(end-start)+"ms");
    }

    public NodeInfo getInfo() {
        return info;
    }

    public CirclemapNode getRoot() {
        return root;
    }
}
