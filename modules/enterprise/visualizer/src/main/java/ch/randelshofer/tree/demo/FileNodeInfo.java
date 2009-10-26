/*
 * @(#)FileNodeInfo.java  1.0  September 25, 2007
 *
 * Copyright (colorizer) 2007 Werner Randelshofer
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

import ch.randelshofer.tree.*;
import ch.randelshofer.tree.Colorizer;
import ch.randelshofer.tree.Weighter;
import java.awt.Color;
import java.text.DateFormat;
import java.text.DecimalFormat;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * FileNodeInfo.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 25, 2007 Created.
 */
public class FileNodeInfo extends DefaultNodeInfo {
    private Colorizer colorizer;
    private Weighter weighter;

    /** Creates a new instance. */
    public FileNodeInfo() {
        colorizer = new RGBColorizer();
        weighter = new LastModifiedWeighter();
    }

    public void init(TreeNode root) {
        weighter.init(root);
                colorizer = new RGBColorizer(new float[] {0f,((LastModifiedWeighter)weighter).getMedianWeight(),1f},        new Color[]{
            new Color(0x64c8ff),
            new Color(0xf5f5f5),
            new Color(0xff9946)
        });

                BasicToolTipUI tt;
    }

    public Weighter getWeighter() {
        return weighter;
    }
    public Colorizer getColorizer() {
        return colorizer;
    }


   @Override public long getWeight(TreePath<TreeNode> path) {
        FileNode fn = (FileNode) path.getLastPathComponent();
        return fn.getFileSize();
    }

    @Override public Color getColor(TreePath<TreeNode> path) {
        FileNode fn = (FileNode) path.getLastPathComponent();
        return colorizer.get(weighter.getWeight(path));
    }
    @Override public String getName(TreePath<TreeNode> path) {
        FileNode fn = (FileNode) path.getLastPathComponent();
        return fn.getName();
    }
   @Override public String getTooltip(TreePath<TreeNode> path) {
        StringBuilder buf = new StringBuilder();

        TreePath<TreeNode> parentPath = path;
        do {
            buf.insert(0, "<br>");
            buf.insert(0, getName(parentPath));
            parentPath = parentPath.getParentPath();
        } while (parentPath != null);
        buf.insert(0, "<html>");
        buf.append("<br>");

        FileNode node = (FileNode) path.getLastPathComponent();
        if (! node.isLeaf()) {
        buf.append(DecimalFormat.getIntegerInstance().format(node.children().size()));
        buf.append(" Files");
        buf.append("<br>");
        }

        buf.append(formatSize(getWeight(path)));

        buf.append("<br>Last modified: ");
        buf.append(DateFormat.getDateTimeInstance().format(node.getLastModified()));

        return buf.toString();
    }

    private static String formatSize(long w) {
        double scaledW = w;
        String scaledUnit = "bytes";
        if (scaledW > 1024) {
            scaledW /= 1024;
            scaledUnit = "KB";
            if (scaledW > 1024) {
                scaledW /= 1024;
                scaledUnit = "MB";
            }
            if (scaledW > 1024) {
                scaledW /= 1024;
                scaledUnit = "GB";
            }
        }
        StringBuilder buf = new StringBuilder();
        buf.append(DecimalFormat.getNumberInstance().format(scaledW));
        buf.append(' ');
        buf.append(scaledUnit);
        if (scaledUnit != "bytes") { // string literals get interned
            buf.append(" (");
            buf.append(DecimalFormat.getIntegerInstance().format(w));
            buf.append(" bytes)");
        }
        return buf.toString();
    }


}
