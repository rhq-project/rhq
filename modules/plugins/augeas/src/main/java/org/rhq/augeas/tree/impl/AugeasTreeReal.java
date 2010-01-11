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
package org.rhq.augeas.tree.impl;

import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeReal;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Eager representation of the Augeas tree.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class AugeasTreeReal extends AbstractAugeasTree {

    public AugeasTreeReal(Augeas ag, AugeasModuleConfig moduleConfig) {
        super(ag, moduleConfig);
    }

    public AugeasNode createNode(String fullPath) throws AugeasTreeException {
        AugeasNode node = null;

        int index = fullPath.lastIndexOf(PATH_SEPARATOR);
        if (index != -1) {
            String parentPath = fullPath.substring(0, index);
            AugeasNode parentNode = getNode(parentPath);
            node = new AugeasNodeReal(parentNode, this, fullPath);
        } else
            throw new AugeasTreeException("Node cannot be created. Parent node does not exist.");

        node.setValue(get(fullPath));

        List<AugeasNode> childs = match(fullPath + PATH_SEPARATOR + "*");

        for (AugeasNode chd : childs) {
            node.addChildNode(chd);
        }
        getNodeBuffer().addNode(node);
        return node;

    }

    public void removeNode(AugeasNode node, boolean updateSeq) throws AugeasTreeException {
        int seq = node.getSeq();

        //TODO lkrejci: why are we doing this here?
        List<AugeasNode> nodes = matchRelative(node.getParentNode(), PATH_SEPARATOR + node.getLabel()
            + "[position() > " + String.valueOf(seq) + "]");

        for (AugeasNode nds : nodes) {
            nds.setSeq(nds.getSeq() - 1);
            nds.updateFromParent();
        }

        getAugeas().remove(node.getFullPath());
        getNodeBuffer().removeNode(node, updateSeq, false);
    }
}
