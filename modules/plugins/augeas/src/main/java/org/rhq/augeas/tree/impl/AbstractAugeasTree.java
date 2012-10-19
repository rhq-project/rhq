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
import org.rhq.augeas.tree.AugeasNodeBuffer;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.util.Glob;

/**
 * Abstract base class implementing the basic set of methods of the tree. 
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public abstract class AbstractAugeasTree implements AugeasTree {

    private static final String[] ERROR_NODES = { "pos", "line", "char", "lens", "message" };

    private Augeas augeas;
    private AugeasModuleConfig moduleConfig;
    private AugeasNode rootNode;
    private AugeasNodeBuffer nodeBuffer;

    protected AbstractAugeasTree(Augeas ag, AugeasModuleConfig moduleConfig) {
        this.augeas = ag;
        this.moduleConfig = moduleConfig;
        this.nodeBuffer = new AugeasNodeBuffer();
    }

    protected Augeas getAugeas() {
        return augeas;
    }

    protected AugeasModuleConfig getModuleConfig() {
        return moduleConfig;
    }

    protected AugeasNodeBuffer getNodeBuffer() {
        return nodeBuffer;
    }

    public AugeasNode createNode(AugeasNode parentNode, String name, String value, int seq) throws AugeasTreeException {
        AugeasNode nd = createNode(parentNode.getFullPath() + File.separatorChar + name + "[" + String.valueOf(seq)
            + "]");
        nd.setValue(value);

        return nd;
    }

    public String get(String expr) {
        return augeas.get(expr);
    }

    public File getFile(AugeasNode node) {
        //leave out the initial "/files"
        String path = node.getPath().substring(AUGEAS_DATA_PATH.length());

        String[] pathSegments = path.split("\\" + PATH_SEPARATOR);

        //TODO will this work under windows?
        File root = File.listRoots()[0];

        for (String glob : getIncludeGlobs()) {
            int sepCnt = getSeparatorCount(glob);
            if (sepCnt < pathSegments.length) {
                StringBuilder bld = new StringBuilder();
                for (int i = 0; i < sepCnt + 1; ++i) {
                    bld.append(pathSegments[i]);
                    if (i < sepCnt) {
                        bld.append(PATH_SEPARATOR);
                    }
                }

                File f = new File(bld.toString());

                if (Glob.matches(root, glob, f)) {
                    return f;
                }
            }
        }

        return null;
    }

    public AugeasNode getNode(String path) throws AugeasTreeException {
        AugeasNode node = getLoadedNode(path);
        return node == null ? createNode(path) : node;
    }

    public AugeasNode getRootNode() {
        return rootNode;
    }

    public List<AugeasNode> match(String expression) throws AugeasTreeException {
        List<String> res = augeas.match(expression);

        List<AugeasNode> nodes = new ArrayList<AugeasNode>();

        for (String name : res) {
            nodes.add(getNode(name));
        }

        return nodes;
    }

    public List<AugeasNode> matchRelative(AugeasNode node, String expression) throws AugeasTreeException {
        if (rootNode.getChildNodes().isEmpty())
            throw new AugeasTreeException("Root node has no children.");

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

        return match(node.getFullPath() + PATH_SEPARATOR + expression);
    }

    public void save() {
        augeas.save();
    }

    public void setRootNode(AugeasNode node) {
        this.rootNode = node;
    }

    public void setValue(AugeasNode node, String value) {
        augeas.set(node.getFullPath(), value);
    }

    public String summarizeAugeasError() {
        String nodePrefix = "/augeas/files";
        List<String> str = moduleConfig.getConfigFiles();
        StringBuilder builder = new StringBuilder();

        for (String path : str) {
            String name = nodePrefix + path + File.separatorChar + "error";
            if (augeas.exists(name)) {
                builder.append("Error " + augeas.get(name) + '\n');
                for (String errNd : ERROR_NODES) {
                    String pathToMessage = name + File.separatorChar + errNd;
                    if (augeas.exists(pathToMessage)) {
                        builder.append(errNd).append(" ").append(augeas.get(pathToMessage)).append('\n');
                    }
                }
            }
        }

        return builder.toString();
    }

    /**
     * Returns the node on given path or null if it wasn't loaded yet.
     * 
     * @param path
     * @return
     */
    protected AugeasNode getLoadedNode(String path) {
        return nodeBuffer.getNode(path);
    }

    protected List<String> getIncludeGlobs() {
        return moduleConfig.getIncludedGlobs();
    }
    
    private int getSeparatorCount(String path) {
        int cnt = 0;
        for (int i = 0; i < path.length(); ++i) {
            if (path.charAt(i) == PATH_SEPARATOR) {
                cnt++;
            }
        }

        return cnt;
    }
}
