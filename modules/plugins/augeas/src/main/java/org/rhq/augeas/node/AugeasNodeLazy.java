/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.augeas.node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * 
 * Implementation of the augeas node that loads the children lazily.
 * 
 * @author Filip Drabek
 *
 */
public class AugeasNodeLazy extends AugeasNodeBase implements AugeasNode {

    public AugeasNodeLazy(String fullPath, AugeasTree ag) {
        super();

        this.ag = ag;
        this.path = fullPath.substring(0, fullPath.lastIndexOf(File.separatorChar) + 1);
        String val = fullPath.substring(fullPath.lastIndexOf(File.separatorChar) + 1, fullPath.length());

        int firstB = val.indexOf("[");
        if (firstB != -1) {
            seq = Integer.valueOf(val.substring(firstB + 1, val.indexOf(']')));
            label = val.substring(0, firstB);
        } else {
            seq = 0;
            label = fullPath.substring(fullPath.lastIndexOf(File.separatorChar) + 1);
        }
    }

    public List<AugeasNode> getChildNodes() {
        List<AugeasNode> nodes = null;
        nodes = ag.match(getFullPath() + File.separatorChar + "*");
        return nodes;
    }

    public AugeasNode getParentNode() {
        String parentNodePath = path.substring(0, path.length() - 1);
        if (parentNodePath.equals(ag.getRootNode().getFullPath()))
            return ag.getRootNode();
        try {
            parentNode = ag.getNode(parentNodePath);
        } catch (AugeasTreeException e) {
            return null;
        }

        return parentNode;
    }

    public String getValue() {
        if (value == null)
            value = ag.get(getFullPath());

        return value;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AugeasNode that = (AugeasNode) obj;

        if (!this.getFullPath().equals(that.getFullPath()))
            return false;

        return true;
    }

    public void setValue(String value) {
        ag.setValue(this, value);
    }

    public String getFullPath() {
        return path + label + (seq != 0 ? "[" + String.valueOf(seq) + "]" : "");
    }

    public void addChildNode(AugeasNode node) {
        //TODO check if the node belongs here
        childNodes.add(node);
    }

    public List<AugeasNode> getChildByLabel(String labelName) {
        List<AugeasNode> nodes = getChildNodes();
        List<AugeasNode> tempNode = new ArrayList<AugeasNode>();

        for (AugeasNode node : nodes) {
            if (node.getLabel().equals(labelName))
                tempNode.add(node);
        }
        return tempNode;
    }

    public void remove(boolean updateSeq) throws AugeasTreeException {
        ag.removeNode(this, updateSeq);
    }

    public void setPath(String path) throws AugeasTreeException {
        this.path = path;

    }

    public void updateFromParent() {
        AugeasNode node = this.getParentNode();
        if (!this.path.equals(node.getFullPath())) {
            this.path = node.getFullPath();
        }
    }
}
