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
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import org.rhq.plugins.antlrconfig.tokens.Skip;
import org.rhq.plugins.antlrconfig.tokens.Transparent;

/**
 * A tree structure provider for the generic Antlr {@link CommonTree} objects. 
 * 
 * The tree structure can be configured to honor the behaviour intended by the {@link Skip} 
 * and {@link Transparent} token types (which is the default).
 * 
 * @author Lukas Krejci
 */
public class AntlrTreeStructure implements TreeStructure<CommonTree> {
    
    private boolean honorOverrides;
    
    public AntlrTreeStructure() {
        honorOverrides = true;
    }
    
    public AntlrTreeStructure(boolean honorOverrides) {
        this.honorOverrides = honorOverrides;
    }
    
    public List<CommonTree> getChildren(CommonTree parent) {
        List<CommonTree> children = new ArrayList<CommonTree>();
        
        for(int i = 0; i < parent.getChildCount(); ++i) {
            CommonTree child = (CommonTree) parent.getChild(i);
            
            if (honorOverrides && child.getToken() instanceof Skip) {
                children.addAll(getChildren(child));
            } else {
                if (!honorOverrides || !(child.getToken() instanceof Transparent)) {
                    children.add(child);
                }
            }
        }
        return children;
    }
    
    public CommonTree getParent(CommonTree child) {
        CommonTree parent = (CommonTree) child.getParent();
        while (parent != null && honorOverrides && parent.getToken() instanceof Skip) {
            parent = (CommonTree) parent.getParent();
        }
        return parent;
    }
}