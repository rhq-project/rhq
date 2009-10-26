/**
 * @(#)CirclemapCompositeNode.java  1.0  Jan 16, 2008
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
import java.util.*;

/**
 * The CirclemapNode class encapsulates a composite {@link TreeNode} whithin a
 * {@link CirclemapTree}.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 16, 2008 Created.
 */
public class CirclemapCompositeNode extends CirclemapNode {

    private ArrayList<CirclemapNode> children;

    /** Creates a new instance. */
    public CirclemapCompositeNode(CirclemapNode parent, TreeNode node) {
        super(parent, node);

        children = new ArrayList<CirclemapNode>();
        for (TreeNode c : node.children()) {
            if (c.isLeaf()) {
                children.add(new CirclemapNode(this, c));
            } else {
                children.add(new CirclemapCompositeNode(this, c));
            }
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public List<CirclemapNode> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void layout(NodeInfo info) {
        for (CirclemapNode child : children) {
            child.layout(info);
        }
        if (children.size() == 0) {
            radius = 10;
            return;
        } else if (children.size() == 1) {
            radius = children.get(0).radius + 1;
            return;
        }


        ArrayList<Circle> circles = new ArrayList<Circle>();
        circles.addAll(children);

        Circles.pairPack(circles);
       // Circles.phyllotacticPack(circles);

        Circle cbounds = Circles.boundingCircle(circles);
        radius = cbounds.radius;

        for (CirclemapNode child : children) {
            child.cx -= cbounds.cx;
            child.cy -= cbounds.cy;
        }

    }

}

