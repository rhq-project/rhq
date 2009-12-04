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
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeReal;
import org.rhq.augeas.node.AugeasRootNode;
import org.rhq.augeas.tree.AugeasNodeBuffer;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * 
 * @author Filip Drabek
 *
 */
public class AugeasTreeReal implements AugeasTree {
    private AugeasModuleConfig moduleConfig;
    private Augeas ag;
    private AugeasNode rootNode;
    private AugeasNode rootConfigNode;
    private AugeasNodeBuffer nodeBuffer;
    private String[] errorNodes = { "pos", "line", "char", "lens", "message" };

    private static String AUGEAS_DATA_PATH = File.separatorChar + "files";

    public AugeasTreeReal(Augeas ag, AugeasModuleConfig moduleConfig) {
        nodeBuffer = new AugeasNodeBuffer();
        this.moduleConfig = moduleConfig;
        this.ag = ag;
    }

    public void update() {

    }

    public void save() {
        ag.save();
    }

    private AugeasNode getLoadedNode(String path) throws AugeasTreeException {
        if (nodeBuffer.isNodeLoaded(path))
            return nodeBuffer.getNode(path);

        throw new AugeasTreeException("Node not found.");
    }

    public AugeasNode getNode(String path) throws AugeasTreeException {
        AugeasNode node;
        try {
            node = getLoadedNode(path);
        } catch (AugeasTreeException e) {
            node = createNode(path);
        }

        return node;
    }

    public List<AugeasNode> match(String expression) throws AugeasTreeException {
        List<String> res = ag.match(expression);

        List<AugeasNode> nodes = new ArrayList<AugeasNode>();

        for (String name : res) {
            nodes.add(getNode(name));
        }

        return nodes;
    }

    public List<AugeasNode> matchRelative(AugeasNode node, String expression) throws AugeasTreeException {
        if (rootNode.getChildNodes().isEmpty())
            throw new AugeasTreeException("Root node has not childs.");

        if (node.equals(rootNode)) {
            List<AugeasNode> nodes = rootNode.getChildNodes();
            List<AugeasNode> returnNodes = new ArrayList<AugeasNode>();
            for (AugeasNode nd : nodes) {
                String tempName = nd.getFullPath() + expression;
                List<AugeasNode> temp = match(tempName);
                returnNodes.addAll(temp);
            }
            return returnNodes;
        }

        return match(node.getFullPath() + File.separatorChar + expression);
    }

    public AugeasNode createNode(String fullPath) throws AugeasTreeException {
        AugeasNode node = null;

        int index = fullPath.lastIndexOf(File.separatorChar);
        if (index != -1) {
            String parentPath = fullPath.substring(0, index);
            AugeasNode parentNode = getNode(parentPath);
            node = new AugeasNodeReal(parentNode, this, fullPath);
        } else
            throw new AugeasTreeException("Node can not be created. Parent node does not exist.");

        node.setValue(get(fullPath));

        List<AugeasNode> childs = match(fullPath + File.separatorChar + "*");

        for (AugeasNode chd : childs) {
            node.addChildNode(chd);
        }
        nodeBuffer.addNode(node);
        return node;

    }

    public AugeasNode createNode(AugeasNode parentNode, String name, String value, int seq) throws AugeasTreeException {
        AugeasNode nd = createNode(parentNode.getFullPath() + File.separatorChar + name + "[" + String.valueOf(seq)
            + "]");
        nd.setValue(value);

        return nd;
    }

    public String get(String expr) {
        return ag.get(expr);
    }

    public AugeasNode getRootNode() {
        return rootNode;
    }

    public void removeNode(AugeasNode node, boolean updateSeq) throws Exception {
        int seq = node.getSeq();

        List<AugeasNode> nodes = matchRelative(node.getParentNode(), File.separatorChar + node.getLabel()
            + "[position() > " + String.valueOf(seq) + "]");

        for (AugeasNode nds : nodes) {
            nds.setSeq(nds.getSeq() - 1);
            nds.updateFromParent();
        }

        int res = ag.remove(node.getFullPath());
        nodeBuffer.removeNode(node, updateSeq, false);
    }

    public void setValue(AugeasNode node, String value) {
        ag.set(node.getFullPath(), value);
    }

    public String summarizeAugeasError() {

        String nodePrefix = "/augeas/files";
        List<String> str = moduleConfig.getIncludedGlobs();
        StringBuilder builder = new StringBuilder();

        for (String path : str) {
            String name = nodePrefix + path + File.separatorChar + "error";
            if (ag.exists(name)) {
                builder.append("Error " + ag.get(name) + '\n');
                for (String errNd : errorNodes) {
                    String pathToMessage = name + File.separatorChar + errNd;
                    if (ag.exists(pathToMessage)) {
                        builder.append(errNd + " " + ag.get(pathToMessage) + '\n');
                    }
                }
            }
        }

        return builder.toString();
    }

    public void setRootNode(AugeasNode node) {
        this.rootNode = node;

    }
}
