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
import java.util.Iterator;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import org.rhq.plugins.antlrconfig.util.TreePathLexer;
import org.rhq.plugins.antlrconfig.util.TreePathParser;

/**
 * A query for a token in the Antlr AST.
 * 
 * @author Lukas Krejci
 */
public class TreePath {

    private CommonTree tree;
    private String[] typeNames;
    private List<PathElement> path;
    
    public TreePath(CommonTree tree, String path, String[] typeNames) throws RecognitionException {
        this(tree, parsePath(path), typeNames);
    }

    public TreePath(CommonTree tree, List<PathElement> path, String[] typeNames) {
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
    
    public static List<PathElement> getPath(CommonTree tree, CommonTree root, String[] typeNames) {
        List<PathElement> elements = new ArrayList<PathElement>();
        while(tree != null && (root == null || !root.equals(tree))) {
            PathElement el = new PathElement();
            
            int type = tree.getType();
            el.setTokenTypeName(typeNames[type].toLowerCase());

            el.setAbsoluteTokenPosition(tree.getChildIndex() + 1);

            //get type relative index
            CommonTree parent = (CommonTree) tree.getParent();
            if (parent != null) {
                int idx = 1;
                for (int i = tree.getChildIndex() - 1; i > 0; --i) {
                    if (parent.getChild(i).getType() == type) {
                        idx++;
                    }
                }
                el.setTypeRelativeTokenPosition(idx);
            }
            
            el.setTokenText(tree.getText());
            
            elements.add(el);

            tree = parent;
        }
        
        Collections.reverse(elements);
        return elements;
    }
    
    public static List<PathElement> getPath(CommonTree tree, String[] typeNames) {
        return getPath(tree, null, typeNames);
    }
    
    public List<PathElement> getPath() {
        return path;
    }
    
    public CommonTree match() {
        List<CommonTree> all = matches();
        if (all.size() == 0) {
            return null;
        } else {
            return all.get(0);
        }
    }
    
    public List<CommonTree> matches() {
        List<CommonTree> currentParents = new ArrayList<CommonTree>();

        if (path.size() > 0 && rootMatches(tree, path.get(0))) {
            currentParents.add(tree);
            
            Iterator<PathElement> it = path.iterator();
            
            it.next();//skip the first element, we've checked the root already
            
            while(it.hasNext()) {
                PathElement pathElement = it.next();
                List<CommonTree> matchingChildren = new ArrayList<CommonTree>();
    
                for(CommonTree parent : currentParents) {
                    matchingChildren.addAll(matchingChildren(parent, pathElement));
                }
                
                currentParents = matchingChildren;
            }
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
    
    private List<CommonTree> matchingChildren(CommonTree parent, PathElement spec) {
        List<CommonTree> children = new ArrayList<CommonTree>();
        
        int tokenType = getTokenType(spec.getTokenTypeName());

        switch (spec.getType()) {
        case NAME_REFERENCE:
            for(int i = 0; i < parent.getChildCount(); ++i) {
                CommonTree child = (CommonTree) parent.getChild(i);
                if (child.getType() == tokenType) {
                    children.add(child);
                }
            }
            break;
        case INDEX_REFERENCE:
            if (spec.getAbsoluteTokenPosition() > 0 && parent.getChildCount() >= spec.getAbsoluteTokenPosition()) {
                children.add((CommonTree)parent.getChild(spec.getAbsoluteTokenPosition() - 1));
            }
            break;
        case POSITION_REFERENCE:
            int position = 0;
            for(int i = 0; i < parent.getChildCount(); ++i) {
                CommonTree child = (CommonTree) parent.getChild(i);
                if (child.getType() == tokenType) {
                    position++; //we're 1 based, so increase before checking...
                    if (position == spec.getTypeRelativeTokenPosition()) {
                        children.add(child);
                        break;
                    }
                }
            }
            break;
        case VALUE_REFERENCE:
            for(int i = 0; i < parent.getChildCount(); ++i) {
                CommonTree child = (CommonTree) parent.getChild(i);
                if (child.getType() == tokenType && spec.getTokenText().equals(child.getText())) {
                    children.add(child);
                }
            }
        }
        
        return children;
    }
    
    private boolean rootMatches(CommonTree root, PathElement spec) {
        int tokenType = getTokenType(spec.getTokenTypeName());

        switch (spec.getType()) {
        case NAME_REFERENCE:
            return root.getType() == tokenType;
        case INDEX_REFERENCE:
            return root.getChildIndex() == spec.getAbsoluteTokenPosition() - 1;
        case POSITION_REFERENCE:
            int position = 0;
            CommonTree parent = (CommonTree) root.getParent();
            if (parent != null) {
                for(int i = 0; i < parent.getChildCount(); ++i) {
                    CommonTree child = (CommonTree) parent.getChild(i);
                    if (child.getType() == tokenType) {
                        position++; //we're 1 based, so increase before checking...
                        if (position == spec.getTypeRelativeTokenPosition() && child == root) {
                            return true;
                        }
                    }
                }
            }
            break;
        case VALUE_REFERENCE:
            return root.getType() == tokenType && spec.getTokenText().equals(root.getText());
        }
        
        return false;
    }
    
    public String toString() {
        StringBuilder bld = new StringBuilder();
        
        for(PathElement el : path) {
            bld.append("/").append(el);
        }
        
        return bld.toString();
    }    
}
