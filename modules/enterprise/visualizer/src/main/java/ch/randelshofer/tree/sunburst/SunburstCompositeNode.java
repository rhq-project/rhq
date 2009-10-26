/*
 * @(#)SunburstCompositeNode.java  1.0  September 18, 2007
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
import java.util.*;
/**
 * SunburstCompositeNode.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunburstCompositeNode extends SunburstNode {
    private ArrayList<SunburstNode> children;

    /** Creates a new instance. */
    public SunburstCompositeNode(SunburstNode parent, TreeNode node) {
        super(parent, node);

        children = new ArrayList<SunburstNode>();
        for (TreeNode c : node.children()) {
            if (c.isLeaf()) {
                children.add(new SunburstNode(this, c));
            } else {
                children.add(new SunburstCompositeNode(this, c));
            }
        }
    }
    public List<SunburstNode> children() {
        return Collections.unmodifiableList(children);
    }
}
