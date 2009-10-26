/*
 * @(#)Weighter.java  1.0  September 26, 2007
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

/**
 * Weighter.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 26, 2007 Created.
 */
public interface Weighter {
    public void init(TreeNode root);

    public float getWeight(TreePath path);

    public int[] getHistogram();

    public String getMinimumWeightLabel();
    public String getMaximumWeightLabel();
}
