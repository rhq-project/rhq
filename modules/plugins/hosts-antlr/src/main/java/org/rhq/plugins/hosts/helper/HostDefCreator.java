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

package org.rhq.plugins.hosts.helper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.antlrconfig.AntlrTreeStructure;
import org.rhq.plugins.antlrconfig.DfsWalker;
import org.rhq.plugins.antlrconfig.NewEntryCreator;
import org.rhq.plugins.antlrconfig.TreePath;

/**
 * Creates a new hosts entry in the configuration file AST. 
 * 
 * @author Lukas Krejci
 */
public class HostDefCreator implements NewEntryCreator {

    private String[] treeTypeNames;
    private String hostDefQuery;
    
    private static final String HOST_DEF_NAME = "config://host_def";
    private static final String IP_NAME = "config://$1";
    private static final String CANONICAL_NAME = "config://$2";
    private static final String ALIASES_NAME = "config://$3";
    private static final String ALIAS_NAME = "config://host_name";

    private String ip;
    private String canonicalName;
    private Set<String> aliases;
    
    public HostDefCreator(String hostDefQuery, String[] treeTypeNames) {
        this.aliases = new LinkedHashSet<String>();
        this.treeTypeNames = treeTypeNames;
        this.hostDefQuery = hostDefQuery;
    }
    
    
    public void prepareFor(Property property) {
        if (HOST_DEF_NAME.equals(property.getName())) {
            aliases.clear();
            ip = null;
            canonicalName = null;
        } else if (IP_NAME.equals(property.getName())) {
            this.ip = ((PropertySimple)property).getStringValue();
        } else if (CANONICAL_NAME.equals(property.getName())) {
            this.canonicalName = ((PropertySimple)property).getStringValue();
        } else if (ALIASES_NAME.equals(property.getName())) {
            aliases.clear();
        } else if (ALIAS_NAME.equals(property.getName())) {
            aliases.add(((PropertySimple)property).getStringValue());
        }
    }


    public List<OpDef> getInstructions(Tree fullTree, Property property) {
        if (HOST_DEF_NAME.equals(property.getName())) {
            try {
                OpDef def = new OpDef();
                def.type = OpType.INSERT_AFTER;
                def.tokenIndex = getIndexToInsert(fullTree);
                def.text = "\n" + ip + "\t" + canonicalName + "\t";
                for(String alias : aliases) {
                    def.text += " " + alias;
                }
                def.text += "\n";
                
                return Collections.singletonList(def);
            } catch (RecognitionException e) {
                // TODO logging
                return null;
            }
        } else {
            return null;
        }
    }

    private int getIndexToInsert(Tree fullTree) throws RecognitionException {
        TreePath path = new TreePath(fullTree, hostDefQuery, treeTypeNames);
        List<Tree> host_defs = path.matches();
        if (host_defs.size() > 0) {
            Tree lastHostDef = host_defs.get(host_defs.size() - 1);
            return lastHostDef.getTokenStopIndex();
        } else {
            return fullTree.getTokenStopIndex();
        }
    }    
}
