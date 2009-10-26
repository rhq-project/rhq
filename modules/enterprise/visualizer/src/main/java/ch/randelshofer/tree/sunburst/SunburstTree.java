/*
 * @(#)SunburstTree.java  1.0  September 18, 2007
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

package ch.randelshofer.tree.sunburst;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;

/**
 * The SunburstTree class implements the model for the SunBurstTree.
 * It's a tree of SunburstNode, each keeping the
 * initial layout of the tree in the SunBurst's Model.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunburstTree {
    private SunburstNode root;
    private NodeInfo info;

    /** Creates a new instance. */
    public SunburstTree(TreeNode root, NodeInfo info) {
        this.info = info;
        if (root.isLeaf()) {
            this.root = new SunburstNode(null, root);
        } else {
            this.root = new SunburstCompositeNode(null, root);
        }
        info.init(root);
        this.root.renumber(info);
        //this.root.dump();
    }

    public NodeInfo getInfo() {
        return info;
    }

    public SunburstNode getRoot() {
        return root;
    }
}
