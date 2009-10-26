/*
 * @(#)CirclemapModel.java  1.0  2008-01-16
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

import ch.randelshofer.tree.sunray.*;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;

/**
 * CirclemapModel manages a CirclemapTree and its CirclemapView.
 *
 * @author Werner Randelshofer
 * @version 1.0 2008-01-16 Created.
 */
public class CirclemapModel {
    private CirclemapTree tree;
    private NodeInfo info;

    /** Creates a new instance. */
    public CirclemapModel(TreeNode root, NodeInfo info) {
        tree = new CirclemapTree(root, info);
        this.info = info;
    }

    public CirclemapView getView() {
        return new CirclemapView(tree);
    }

    public NodeInfo getInfo() {
        return info;
    }
}
