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

import java.util.List;

import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Node is a part of the {@link AugeasTree}.
 *  
 * @author Filip Drabek
 *
 */
public interface AugeasNode {

    /**
     * @return returns the path to the node in the Augeas tree, excluding the node itself.
     */
    public String getPath();

    public void setPath(String path) throws AugeasTreeException;

    /**
     * @return the label of the node
     */
    public String getLabel();

    public void setLabel(String label);

    /**
     * @return the value of the node
     */
    public String getValue();

    public void setValue(String value);

    /**
     * @return the sequence number of the node (i.e. Variable[6] in the augeas notation).
     */
    public int getSeq();

    public void setSeq(int seq);

    /**
     * @return the parent node in the tree
     */
    public AugeasNode getParentNode();

    /** 
     * @return the child nodes in the tree
     */
    public List<AugeasNode> getChildNodes();

    public boolean equals(Object obj);

    /**
     * @return the full path of the node including its label and sequence number
     */
    public String getFullPath();

    public void addChildNode(AugeasNode node);

    public List<AugeasNode> getChildByLabel(String labelName);

    /**
     * Removes the node from its parent children optionally updating
     * the sequence numbers of the sibling nodes with the same label.
     * 
     * @param updateSeq whether to update the siblings' sequence numbers
     * @throws AugeasTreeException
     */
    public void remove(boolean updateSeq) throws AugeasTreeException;

    /**
     * Updates the settings of this node according to the data in the parent.
     * This method should work recursively.
     */
    public void updateFromParent();
}
