/*
 * @(#)NodeInfo.java  1.0  September 21, 2007
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

package ch.randelshofer.tree;

import java.awt.Color;
import java.awt.Image;
import java.util.List;

/**
 * NodeInfo is used to interpret the data stored in a {@link TreeNode}.
 * <p>
 * All methods of NodeInfo take a path to a node as a parameter. This allows
 * to get an interperation of a node based on more criteria than just on the
 * node alone.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 21, 2007 Created.
 */
public interface NodeInfo {
    /**
     * Initializes the node info.
     * @param root
     */
    public void init(TreeNode root);
    /**
     * Returns the name of the node.
     */
    public String getName(TreePath<TreeNode> path);

    /**
     * Returns the color of the node.
     */
    public Color getColor(TreePath<TreeNode> path);

    /**
     * Returns the weight of a node.
     * @return The weight between 0 and Double.MAX_VALUE.
     */
    public long getWeight(TreePath<TreeNode> path);

    /**
     * Returns the tooltip of the node.
     */
    public String getTooltip(TreePath<TreeNode> path);

    /**
     * Returns the image of the node.
     */
    public Image getImage(TreePath<TreeNode> path);

   public Weighter getWeighter();
    public Colorizer getColorizer();

}
