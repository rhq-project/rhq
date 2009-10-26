package ch.randelshofer.tree.sunray;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;
/*
 * @(#)SunBurstTree.java  1.0  September 18, 2007
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

/**
 * IcerayModel manages a SunrayTree and its IcerayView.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class IcerayModel {
    private SunrayTree tree;
    private NodeInfo info;

    /** Creates a new instance. */
    public IcerayModel(TreeNode root, NodeInfo info) {
        this.info = info;
        tree = new SunrayTree(root, info, 8);
    }

    public IcerayView getView() {
        return new IcerayView(tree);
    }
    public NodeInfo getInfo() {
        return info;
    }
}
