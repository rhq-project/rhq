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
package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.util.Glob;
import org.rhq.rhqtransform.AugeasRhqException;

public class AugeasTreeBuilderApache implements AugeasTreeBuilder {

    private Map<String, List<File>> includes;
    private Map<AugeasNode, List<String>> incl;
    private Augeas ag;

    public AugeasTreeBuilderApache() {
        includes = new HashMap<String, List<File>>();
        incl = new HashMap<AugeasNode, List<String>>();
    }

    public AugeasTree buildTree(AugeasProxy component, AugeasConfiguration config, String name, boolean lazy)
        throws AugeasRhqException {

        this.ag = component.getAugeas();

        AugeasConfigurationApache apacheConfig = (AugeasConfigurationApache) config;
        AugeasModuleConfig module = config.getModuleByName(name);

        ApacheAugeasTree tree = new ApacheAugeasTree(apacheConfig.getServerRootPath(), component.getAugeas(), module);

        List<String> incld = module.getConfigFiles();

        if (incld.isEmpty())
            throw new AugeasRhqException("No configuration provided.");

        String rootPath = incld.get(0);

        AugeasNode rootNode = new ApacheAugeasNode(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath, tree);
        tree.setRootNode(rootNode);
        // we need to know which files are related to each glob

        for (String inclName : module.getIncludedGlobs()) {

            List<File> files = new ArrayList<File>();

            File check = new File(inclName);
            File root = new File(check.isAbsolute() ? Glob.rootPortion(inclName) : apacheConfig.getServerRootPath());
            files.addAll(Glob.match(root, inclName, Glob.ALPHABETICAL_COMPARATOR));

            if (module.getExcludedGlobs() != null)
                Glob.excludeAll(files, module.getExcludedGlobs());

            if (!includes.containsKey(inclName))
                includes.put(inclName, files);
        }

        updateIncludes((ApacheAugeasNode) rootNode, tree, rootPath, null);

        //List<String> rootconf = new ArrayList<String>();
        // rootconf.add(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath);
        //  this.incl.put(rootNode, rootconf);

        tree.setIncludes(this.incl);
        return tree;
    }

    public void updateIncludes(ApacheAugeasNode parentNode, AugeasTree tree, String fileName, AugeasNode includeNode)
        throws AugeasRhqException {

        List<String> nestedNodes = ag.match(ApacheAugeasTree.AUGEAS_DATA_PATH + fileName + File.separator + "*");

        List<AugeasNode> createdNodes = new ArrayList<AugeasNode>();

        for (String nodeName : nestedNodes) {
            ApacheAugeasNode newNode = (ApacheAugeasNode) tree.createNode(nodeName);
            newNode.setParentNode(parentNode);
            //ApacheAugeasNode newNode = new ApacheAugeasNode(parentNode,tree,nodeName);
            createdNodes.add(newNode);
        }

        if (includeNode != null)
            parentNode.addIncludeNodes(includeNode, createdNodes);

        for (AugeasNode node : createdNodes) {
            String label = node.getLabel();
            if (canContainNestedNodes(label)) {
                String labelName =
                    label + ((node.getSeq() != 0) ? "[" + String.valueOf(node.getSeq()) + "]" : "");
                updateIncludes((ApacheAugeasNode) node, tree, fileName + File.separator + labelName, null);
            }
            if (label.equals("Include") || label.equals("IncludeOptional")) {
                String val = ag.get(node.getFullPath() + File.separator + "param");
                if (includes.containsKey(val)) {
                    //include directive contains globNames
                    List<File> files = includes.get(val);
                    List<String> names = new ArrayList<String>();
                    for (File file : files) {
                        names.add(ApacheAugeasTree.AUGEAS_DATA_PATH + file.getAbsolutePath());
                        updateIncludes((ApacheAugeasNode) node.getParentNode(), tree, file.getAbsolutePath(), node);
                    }
                    if (incl.containsKey(node.getParentNode())) {
                        List<String> list = incl.get(node.getParentNode());
                        list.addAll(names);
                    } else
                        incl.put(node.getParentNode(), names);
                }
            }
        }
    }

    private boolean canContainNestedNodes(String name) {
        return name.startsWith("<");
    }
}
