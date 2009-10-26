package ch.randelshofer.tree.sunburst;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;
/*
 * @(#)SunburstModel.java  1.0  September 18, 2007
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
 * SunburstModel manages a SunburstTree and its SunburstView.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunburstModel {
    private SunburstTree tree;
    private NodeInfo info;

    /** Creates a new instance. */
    public SunburstModel(TreeNode root, NodeInfo info) {
        tree = new SunburstTree(root, info);
        this.info = info;
    }

    public SunburstView getView() {
        return new SunburstView(tree);
    }
    public NodeInfo getInfo() {
        return info;
    }
}
