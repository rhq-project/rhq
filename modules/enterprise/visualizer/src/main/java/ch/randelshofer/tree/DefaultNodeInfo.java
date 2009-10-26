/**
 * @(#)DefaultNodeInfo.java  1.0  Jan 26, 2008
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
package ch.randelshofer.tree;

import java.awt.Color;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * DefaultNodeInfo.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 26, 2008 Created.
 */
public class DefaultNodeInfo implements NodeInfo {

    public String getName(TreePath<TreeNode> path) {
        return path.getLastPathComponent().toString();
    }

    public Color getColor(TreePath<TreeNode> path) {
        return Color.black;
    }

    public long getWeight(TreePath<TreeNode> path) {
        return 1;
    }

    public String getTooltip(TreePath<TreeNode> path) {
        StringBuilder buf = new StringBuilder();

        TreePath<TreeNode> parentPath = path;
        do {
            buf.insert(0, "<br>");
            buf.insert(0, getName(parentPath));
            parentPath = parentPath.getParentPath();
        } while (parentPath != null);
        buf.insert(0, "<html>");
        buf.append("<br>");

        TreeNode node = path.getLastPathComponent();
        if (!node.isLeaf()) {
            buf.append(DecimalFormat.getIntegerInstance().format(node.children().size()));
            buf.append(" Files");
            buf.append("<br>");
        }

        long w = getWeight(path);
        buf.append(DecimalFormat.getIntegerInstance().format(w));
        return buf.toString();
    }

    public Image getImage(TreePath<TreeNode> path) {
        return null;
    }

    public void init(TreeNode root) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Weighter getWeighter() {
        return null;
    }

    public Colorizer getColorizer() {
        return null;
    }
}
