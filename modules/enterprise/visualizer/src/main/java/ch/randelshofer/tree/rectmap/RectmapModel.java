/*
 * @(#)RectmapModel.java  1.0  2008-01-16
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

import ch.randelshofer.tree.sunray.*;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;

/**
 * RectmapModel
 *
 *
 * @author Werner Randelshofer
 * @version 1.0 2008-01-16 Created.
 */
public class RectmapModel {
    private RectmapTree tree;
    private NodeInfo info;

    /** Creates a new instance. */
    public RectmapModel(TreeNode root, NodeInfo info) {
        tree = new RectmapTree(root, info);
        this.info = info;
    }

    public RectmapView getView() {
        return new RectmapView(tree);
    }
    public NodeInfo getInfo() {
        return info;
    }
}
