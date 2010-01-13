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

import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Represents the root node of the augeas tree.
 * @author Filip Drabek
 *
 */
public class AugeasRootNode extends AugeasNodeBase implements AugeasNode {

    public AugeasRootNode(List<AugeasNode> nodes) {
        super();
        childNodes = nodes;
    }

    public AugeasRootNode() {
        super();
    }

    public void addChildNode(AugeasNode node) {
        childNodes.add(node);
    }

    public List<AugeasNode> getChildNodes() {
        return childNodes;
    }

    public String getFullPath() {
        return "" + File.pathSeparatorChar;
    }

    public String getLabel() {
        return null;
    }

    public AugeasNode getParentNode() {
        return null;
    }

    public String getPath() {
        return File.separator;
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
        throw new AugeasTreeException(
            "Root node is virtual and can not be removed. If you want to remove data remove all child nodes.");
    }

    public void setPath(String path) throws AugeasTreeException {
    }

    public void updateFromParent() {

    }
}
