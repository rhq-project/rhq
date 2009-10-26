/*
 * @(#)FileNameComparator.java  1.0  September 16, 2007
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

package ch.randelshofer.tree.demo;

import ch.randelshofer.quaqua.filechooser.OSXCollator;
import java.io.File;
import java.text.Collator;
import java.util.Comparator;

/**
 * FileNameComparator.
 *
 * @author Werner Randelshofer
 * @version 1.0 September 16, 2007 Created.
 */
public class FileNameComparator implements Comparator {
    private static FileNameComparator instance;

    public static FileNameComparator getInstance() {
        if (instance == null) {
            instance = new FileNameComparator();
        }
        return instance;
    }

    /**
     * Compares two nodes using their collation keys.
     *
     * @param o1 An instance of AliasFileSystemTreeModel.Node.
     * @param o2 An instance of AliasFileSystemTreeModel.Node.
     */
    public int compare(Object o1, Object o2) {
        return OSXCollator.getInstance().compare(((File) o1).getName(), ((File) o2).getName());
    }

}
