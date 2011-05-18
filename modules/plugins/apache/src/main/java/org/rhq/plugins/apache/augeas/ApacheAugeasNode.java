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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeLazy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * A specialization of an AugeasNode for purposes of Apache mapping.
 * We need to specialize, because the Augeas tree we are building needs 
 * transparently places nodes from different files in appropriate
 * places in the tree where those files are included using the Apache
 * &lt;Include&gt; directive.
 * 
 * This node holds a list of all nodes that have been included to it
 * and modifies the get* methods to handle these as well.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class ApacheAugeasNode extends AugeasNodeLazy implements AugeasNode {

    /**
     * List of included nodes.
     */
    private Map<Integer, List<AugeasNode>> includedNodes;

    public ApacheAugeasNode(String fullPath, AugeasTree tree) {
        super(fullPath, tree);
    }

    public ApacheAugeasNode(AugeasNode parentNode, AugeasTree tree, String fullPath) {
        super(fullPath, tree);
        this.parentNode = parentNode;
    }

    /**
     * Returns the list of child nodes.
     * A child node is either a direct child node or a child of
     * an direct child include node.
     * I.e. we are transparently leaving out the Include directives
     * and replacing them with their children.
     * 
     * @return the child nodes
     * @see AugeasNodeLazy#getChildNodes()
     */
    public List<AugeasNode> getChildNodes() {
        List<AugeasNode> nodes = null;
        nodes = ag.match(getFullPath() + File.separatorChar + "*");
        
        if (includedNodes != null) {
            //to avoid having to recompute indexes to insert the included nodes into the
            //list of nodes as seen by augeas, let's include them from the biggest index
            //to the lowest.
            List<Integer> includeNodeIndexes = new ArrayList<Integer>(includedNodes.keySet());
            Collections.sort(includeNodeIndexes, Collections.reverseOrder());
            
            for(Integer idx : includeNodeIndexes) {
                //remove the include node itself
                nodes.remove(idx);
                
                //add the included nodes instead of it
                nodes.addAll(idx, includedNodes.get(idx));
            }
        }

        return nodes;
    }

    /**
     * Adds the provided nodes to the list of the included child nodes.
     * 
     * @param nodes
     */
    public void addIncludeNodes(AugeasNode includeNode, List<AugeasNode> nodes) {
        if (nodes.isEmpty())
            return;

        if (includedNodes == null)
            includedNodes = new HashMap<Integer, List<AugeasNode>>();

        List<AugeasNode> childNodes = super.getChildNodes();
        int idx = 0;
        boolean found = false;
        
        for(AugeasNode child : childNodes) {
            if (child.getLabel().equals(includeNode.getLabel()) && child.getSeq() == includeNode.getSeq()) {
                found = true;
                break;
            }
            
            ++idx;
        }
        
        if (found) {
            List<AugeasNode> alreadyIncluded = includedNodes.get(idx);
            if (alreadyIncluded == null) {
                //copy the nodes over to a new list so that we can modify it later without modifying the original collection
                //which might be unexpected on the caller site.
                includedNodes.put(idx, new ArrayList<AugeasNode>(nodes));
            } else {
                alreadyIncluded.addAll(nodes);
            }
        }
    }

    public AugeasNode getParentNode() {
        //apache nodes get their parent nodes set by the tree builder
        //(AugeasTreeBuilderApache)
        //in case they are included from another files.
        //so bail out immediately if we have our parent set.
        if (parentNode != null)
            return parentNode;

        return super.getParentNode();
    }

    public void addChildNode(AugeasNode node) {
        //TODO check if the node belongs here
        childNodes.add(node);
    }

    public void setPath(String path) throws AugeasTreeException {
        this.path = path;

    }

    public void updateFromParent() {
        //don't update from parent
        //tree builder can set the parent node to something
        //else if this node is included from another file
        //and we would destroy that association here.
    }
    
    public void setParentNode(AugeasNode node){
    	this.parentNode = node;
    }    
}
