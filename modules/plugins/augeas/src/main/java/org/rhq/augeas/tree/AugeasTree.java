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

package org.rhq.augeas.tree;

import java.io.File;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.node.AugeasNode;


/**
 * Represents the augeas tree.
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasTree {
    
    /**
     * All the paths to nodes representing configuration data in Augeas are
     * prefixed by this.
     */
    public static final String AUGEAS_DATA_PATH = File.separatorChar + "files";
    
    /**
     * Persists the tree using Augeas to the individual files.
     */
    public void save();

    /**
     * Checks whether a node exists on given path. If it exists,
     * the node is returned otherwise a new one is created and returned.
     * 
     * @param path the path to look for
     * @return the node on given path
     * @throws AugeasTreeException
     */
    public AugeasNode getNode(String path) throws AugeasTreeException;

    /**
     * Performs a search using Augeas expression on the tree.
     * 
     * @param expression
     * @return list of nodes matching the expression
     * @throws AugeasTreeException
     */
    public List<AugeasNode> match(String expression) throws AugeasTreeException;

    /**
     * Performs a search using Augeas expression on the tree starting at given node.
     * 
     * @param node the node to start searching from
     * @param expression the expression
     * @return list of child nodes matching the expressions
     * @throws AugeasTreeException
     */
    public List<AugeasNode> matchRelative(AugeasNode node, String expression) throws AugeasTreeException;

    /**
     * Creates a new node on given path. If there already exists a node on given path,
     * it is returned instead.
     * 
     * @param fullPath
     * @return
     * @throws AugeasTreeException
     */
    public AugeasNode createNode(String fullPath) throws AugeasTreeException;

    /**
     * Creates a new child node under given parent.
     * 
     * @param parentNode the parent node
     * @param name name of the child node
     * @param value value of the child node
     * @param seq the sequence number of the child node
     * @return the child nodes
     * @throws AugeasTreeException
     */
    public AugeasNode createNode(AugeasNode parentNode, String name, String value, int seq) throws AugeasTreeException;

    /**
     * Returns a value of the first node matching the expression.
     * Akin to {@link Augeas#get(String)}.
     * 
     * @param expr the Augeas expression.
     * @return the value 
     */
    public String get(String expr);

    /**
     * @return returns the root node of the tree.
     */
    public AugeasNode getRootNode();

    /**
     * Removes the node from the tree optionally updating the sequence numbers
     * of its siblings.
     * 
     * @param node the node to remove
     * @param updateSeq whether to update sequence numbers or not.
     * @throws AugeasTreeException
     */
    public void removeNode(AugeasNode node, boolean updateSeq) throws AugeasTreeException;

    /**
     * Sets the value of the node directly in Augeas, bypassing the in-memory tree representation.
     *  
     * @param node
     * @param value
     */
    public void setValue(AugeasNode node, String value);

    /**
     * @return a summary of an update error obtained from the Augeas library.
     */
    public String summarizeAugeasError();

    /**
     * Sets the root node.
     * 
     * @param node
     */
    public void setRootNode(AugeasNode node);
}
