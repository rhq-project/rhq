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

import java.io.File;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasRootNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * Default implementation of the tree builder.
 * This just loads the data from Augeas and represents the returned data as the
 * node tree without any modifications.
 * 
 * @author Filip Drabek
 */
public class DefaultAugeasTreeBuilder implements AugeasTreeBuilder {
    private static String AUGEAS_DATA_PATH = File.separatorChar + "files";

    public DefaultAugeasTreeBuilder() {
    }

    public AugeasTree buildTree(AugeasProxy component, AugeasConfiguration moduleConfig, String name, boolean lazy)
        throws AugeasTreeException {

        AugeasTree tree;
        AugeasModuleConfig module = moduleConfig.getModuleByName(name);
        if (lazy = true)
            tree = new AugeasTreeLazy(component.getAugeas(), module);
        else
            tree = new AugeasTreeReal(component.getAugeas(), module);

        AugeasNode rootNode = new AugeasRootNode();

        for (String fileName : module.getConfigFiles()) {
            rootNode.addChildNode(tree.createNode(AUGEAS_DATA_PATH + File.separatorChar + fileName));
        }

        tree.setRootNode(rootNode);

        return tree;
    }

}
