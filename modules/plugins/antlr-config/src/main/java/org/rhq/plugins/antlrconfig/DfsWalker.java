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

package org.rhq.plugins.antlrconfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class implements the depth first search on an arbitrary tree structure.
 * It must be provided with a {@link TreeStructure} implementation that defines the tree structure of the data.
 * <p>
 * This class implements the {@link Iterator} interface, returning a list of NodeType objects representing
 * the current path the walker is currently at.
 * 
 * @author Lukas Krejci
 */
public class DfsWalker<NodeType> implements Iterator<NodeType> {
   
    
    private TreeStructure<NodeType> structure;
    private boolean hasNext;
    private List<NodeType> currentPath;
    private int currentDepth;
    private Deque<Iterator<NodeType>> searchStack;
    private int maxDepth;
    
    public DfsWalker(TreeStructure<NodeType> structure, Collection<NodeType> roots) {
        this(structure, roots, -1);
    }
    
    public DfsWalker(TreeStructure<NodeType> structure, Collection<NodeType> roots, int maxDepth) {
        this.structure = structure;
        hasNext = roots.size() > 0;
        currentPath = new ArrayList<NodeType>();
        searchStack = new ArrayDeque<Iterator<NodeType>>();
        searchStack.push(roots.iterator());
        this.maxDepth = maxDepth;
    }
    
    public DfsWalker(TreeStructure<NodeType> structure, NodeType root) {
        this(structure, Collections.singleton(root));
    }
    
    public DfsWalker(TreeStructure<NodeType> structure, NodeType root, int maxDepth) {
        this(structure, Collections.singleton(root), maxDepth);
    }
    
    public boolean hasNext() {
        return hasNext;
    }
    
    public List<NodeType> nextPath() {
        advance();
        return currentPath;
    }
    
    public NodeType next() {
        advance();
        NodeType last = currentPath.get(currentPath.size() - 1);
        return last;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    public int getCurrentDepth() {
        return currentDepth;
    }
    
    public List<NodeType> getCurrentPath() {
        return currentPath;
    }
    
    private void advance() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        
        Iterator<NodeType> currentLevel = searchStack.peek();
        
        if (currentLevel.hasNext()) {
            NodeType currentElement = currentLevel.next();
            
            if (currentDepth == currentPath.size()) {
                currentPath.add(currentElement);
            } else {
                currentPath.set(currentDepth, currentElement);
            }
            
            if (maxDepth < 0 || currentDepth < maxDepth) {
                currentDepth++;
                Collection<NodeType> children = structure.getChildren(currentElement);
                
                if (children != null && children.size() > 0) {
                    searchStack.push(children.iterator());
                }
            }
        } else {
            searchStack.pop();
            if (currentDepth > 0) {
                //currentPath might not contain an entry for this depth
                //if there were no elements at this level...
                if (currentDepth < currentPath.size()) {
                    currentPath.remove(currentDepth);
                }
            }
            currentDepth--;
        }
        
        hasNext = !searchStack.isEmpty();
    }
}
