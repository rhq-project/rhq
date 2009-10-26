/*
 * @(#)TreeNode.java  1.0  September 21, 2007
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
 *
 * Original code copyright (c) 2001 www.bouthier.net
 * Roman Kennke [roman@ontographics.com]
 */

package ch.randelshofer.tree;

import java.awt.Color;
import java.awt.Image;
import java.util.List;


/**
 * The TreeNode interface is implemented by objects which encapsulate a tree
 * structure.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 21, 2007 Created.
 */
public interface TreeNode {
    /**
     * Returns the children of this node
     * in a List.
     * If this object does not have children,
     * it returns an empty List.
     *
     * @return    the children of this node
     */
    public List<TreeNode> children();


    /** Returns true, if this node is a leaf.
     * This is used to make a distinction between composite nodes which have
     * no children, and leaf nodes which can have no children.
     */
    public boolean isLeaf();
}