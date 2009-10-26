/*
 * @(#)LastModifiedWeighter.java  2.0  2008-01-27
 *
 * Copyright (c) 2007-2008 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree.demo;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import ch.randelshofer.tree.Weighter;
import java.awt.*;
import java.awt.geom.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * LastModifiedWeighter.
 *
 * @author Werner Randelshofer
 * @version 2.0 2008-01-27 Added computation and drawing of histogram.
 * <br>1.0 September 26, 2007 Created.
 */
public class LastModifiedWeighter implements Weighter {
    private long min = new Date(2003-1900,0,1).getTime();
    private long max = new Date().getTime();
    private long median = (max - min) / 2;
    private int[] histogram;

    /** Creates a new instance. */
    public LastModifiedWeighter() {
    }

    public float getWeight(TreePath path) {
        TreeNode node = (TreeNode) path.getLastPathComponent();
        FileNode fn = (FileNode) node;
        long lastModified = fn.getLastModified();

        return (float) ((lastModified - min) /
                (float) (max - min));
    }

    public float getMedianWeight() {
        return (float) ((median - min) /
                (float) (max - min));
    }

    public void init(TreeNode root) {
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
            ArrayList<Long> dates = new ArrayList<Long>();
            collectDatesRecursive(root, dates);
            Collections.sort(dates);
            if (dates.size() > 0) {
                min = dates.get(0);
                max = dates.get(dates.size() - 1);
                median = dates.get(dates.size() / 2);
            }
//        medianRecursive(root, valueList);
        if (dates.size() > 0) {
        histogram = new int[256];
        histogramRecursive(root); // XXX - Could be done without recursion, because we got all values now
        } else {
        histogram = new int[1];
        histogram[0] = 1;
        }
    }
    private void collectDatesRecursive(TreeNode node, java.util.List<Long> dates) {
        FileNode fn = (FileNode) node;
        long lastModified = fn.getLastModified();
        dates.add(lastModified);
        for (TreeNode child : node.children()) {
            collectDatesRecursive((TreeNode) child, dates);
        }
    }
    private void histogramRecursive(TreeNode root) {
        FileNode fn = (FileNode) root;
        long lastModified = fn.getLastModified();

            int index = Math.min(histogram.length - 1, Math.max(0, (int) ((lastModified - min) * (histogram.length - 1) / (double) (max - min))));
            histogram[index]++;

        for (TreeNode child : root.children()) {
            histogramRecursive(child);
        }
    }

    public int[] getHistogram() {
        return histogram;
    }

    public String getMinimumWeightLabel() {
        return DateFormat.getDateTimeInstance().format(new Date(min));
    }

    public String getMaximumWeightLabel() {
        return DateFormat.getDateTimeInstance().format(new Date(max));
    }
}
