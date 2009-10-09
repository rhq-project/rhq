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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import org.rhq.core.antlr.TreePathLexer;
import org.rhq.core.antlr.TreePathParser;

/**
 * A query for a token in the Antlr AST.
 * 
 * @author Lukas Krejci
 */
public class TreePath {

    private Tree tree;
    private String[] typeNames;
    private List<PathElement> path;
    
    public TreePath(Tree tree, String path, String[] typeNames) throws RecognitionException {
        this(tree, parsePath(path), typeNames);
    }

    public TreePath(Tree tree, List<PathElement> path, String[] typeNames) {
        this.tree = tree;
        this.typeNames = new String[typeNames.length];
        System.arraycopy(typeNames, 0, this.typeNames, 0, typeNames.length);
        
        for(int i = 0; i < this.typeNames.length; i++) {
            this.typeNames[i] = this.typeNames[i].toLowerCase();
        }
        
        this.path = path;        
    }
    
    public static List<PathElement> parsePath(String path) throws RecognitionException {
        TreePathParser parser = new TreePathParser(new CommonTokenStream(new TreePathLexer(new ANTLRStringStream(path))));
        TreePathParser.path_return result = parser.path();
        return result.elements;
    }
    
    public static List<PathElement> getPath(Tree tree, String[] typeNames) {
        List<PathElement> elements = new ArrayList<PathElement>();
        while(tree != null) {
            PathElement el = new PathElement();
            
            int type = tree.getType();
            el.setTokenName(typeNames[type]);

            el.setTokenPosition(tree.getChildIndex() + 1);
            
            el.setTokenText(tree.getText());
            
            elements.add(el);

            tree = tree.getParent();
        }
        
        Collections.reverse(elements);
        return elements;
    }
    
    public List<PathElement> getPath() {
        return path;
    }
    
    public Tree match() {
        List<Tree> all = matches();
        if (all.size() == 0) {
            return null;
        } else {
            return all.get(0);
        }
    }
    
    public List<Tree> matches() {
        List<Tree> currentParents = new ArrayList<Tree>();
        CommonTree virtualParent = new CommonTree();
        virtualParent.addChild(tree);
        currentParents.add(virtualParent);
        
        for(PathElement pathElement : path) {
            List<Tree> matchingChildren = new ArrayList<Tree>();

            for(Tree parent : currentParents) {
                matchingChildren.addAll(matchingChildren(parent, pathElement));
            }
            
            currentParents = matchingChildren;
        }
        
        return currentParents;
    }
    
    private int getTokenType(String typeName) {
        for(int i = 0; i < typeNames.length; i++) {
            if (typeNames[i].equals(typeName)) {
                return i;
            }
        }
        
        return -1;
    }
    
    private List<Tree> matchingChildren(Tree parent, PathElement spec) {
        List<Tree> children = new ArrayList<Tree>();
        
        int tokenType = getTokenType(spec.getTokenName());

        switch (spec.getType()) {
        case NAME_REFERENCE:
            for(int i = 0; i < parent.getChildCount(); ++i) {
                Tree child = parent.getChild(i);
                if (child.getType() == tokenType) {
                    children.add(child);
                }
            }
            break;
        case INDEX_REFERENCE:
            if (spec.getTokenPosition() > 0 && parent.getChildCount() >= spec.getTokenPosition()) {
                children.add(parent.getChild(spec.getTokenPosition() - 1));
            }
            break;
        case POSITION_REFERENCE:
            int position = 0;
            for(int i = 0; i < parent.getChildCount(); ++i) {
                Tree child = parent.getChild(i);
                if (child.getType() == tokenType) {
                    position++; //we're 1 based, so increase before checking...
                    if (position == spec.getTokenPosition()) {
                        children.add(child);
                        break;
                    }
                }
            }
            break;
        case VALUE_REFERENCE:
            for(int i = 0; i < parent.getChildCount(); ++i) {
                Tree child = parent.getChild(i);
                if (child.getType() == tokenType && spec.getTokenText().equals(child.getText())) {
                    children.add(child);
                }
            }
        }
        
        return children;
    }
    
    public String toString() {
        StringBuilder bld = new StringBuilder();
        
        for(PathElement el : path) {
            bld.append("/").append(el);
        }
        
        return bld.toString();
    }    
}
