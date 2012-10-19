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
import org.rhq.augeas.node.AugeasNodeLazy;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Lazy implementation of the augeas tree.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class AugeasTreeLazy extends AbstractAugeasTree {

    public AugeasTreeLazy(Augeas ag, AugeasModuleConfig moduleConfig) {
        super(ag, moduleConfig);
    }

    protected AugeasNode instantiateNode(String fullPath) {
        return new AugeasNodeLazy(fullPath, this);
    }

    public AugeasNode createNode(String fullPath) throws AugeasTreeException {
        AugeasNode node = getLoadedNode(fullPath);

        if (node != null) {
            return node;
        }

        List<String> list = getAugeas().match(fullPath);
        if (!list.isEmpty()) {
            AugeasNode newNode = instantiateNode(fullPath);
            getNodeBuffer().addNode(newNode);
            return newNode;
        }

        getAugeas().set(fullPath, null);
        node = instantiateNode(fullPath);
        getNodeBuffer().addNode(node);
        return node;
    }

    public void removeNode(AugeasNode node, boolean updateSeq) throws AugeasTreeException {
        getAugeas().remove(node.getFullPath());
        getNodeBuffer().removeNode(node, updateSeq, true);
    }
}
