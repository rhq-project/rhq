/*
 * @(#)XMLNode.java  1.0  23. Juni 2008
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.tree.demo;

import ch.randelshofer.tree.NodeInfo;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMLNode.
 *
 * @author  Werner Randelshofer
 * @version 1.0 23. Juni 2008 Created.
 */
public class XMLNode implements TreeNode {

    private ArrayList<XMLNode> children;
    private String name;
    private HashMap<String, String> attributes;
    private long cumulatedWeight;

    /** Creates a new instance. */
    public XMLNode() {
        attributes = new HashMap<String, String>();
    }

    public List<TreeNode> children() {
        return (children == null) ? Collections.EMPTY_LIST : children;
    }

    public void addChild(TreeNode child) {
        if (children == null) {
            children = new ArrayList<XMLNode>();
        }
        children.add((XMLNode) child);
    }

    public boolean isLeaf() {
        return children == null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void putAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public long getCumulatedWeight() {
        return cumulatedWeight;
    }

    public void setCumulatedWeight(long newValue) {
        cumulatedWeight = newValue;
    }

    public void accumulateWeights(NodeInfo info, TreePath path) {
        TreePath myPath;
        if (path == null) {
            myPath = new TreePath(this);
        } else {
            myPath = path.pathByAddingChild(this);
        }
        cumulatedWeight = info.getWeight(myPath);
        if (children != null) {
            for (XMLNode child : children) {
                child.accumulateWeights(info, myPath);
                cumulatedWeight += child.getCumulatedWeight();
            }
        }
    }
}
