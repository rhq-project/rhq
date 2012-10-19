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

import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import org.rhq.core.domain.configuration.Property;

/**
 * Enables the {@link ConfigMapper} to add new entries into the configuration file.
 * 
 * The config mapper walks through the configuration properties in a depth first search
 * manner.
 * For each property it first calls the {@link #prepareFor(Property)} method so that
 * the creator can initialize itself for given property.
 * After the whole subtree of that property (if it's a list or a map) has been processed,
 * the {@link #getInstructions(CommonTree, Property)} method is called to actually get the instructions for
 * creating the property.
 * 
 * @author Lukas Krejci
 */
public interface NewEntryCreator {

    enum OpType {
        INSERT_BEFORE,
        INSERT_AFTER,
        REPLACE,
        DELETE
    }
    
    class OpDef {
        public OpType type;
        public int tokenIndex;
        public String text;
    }
    
    /**
     * @param fullTree the full tree representing the whole configuration
     * @param immediateParent the nearest known parent in the tree (might be null)
     * @param property the property that is being added
     * @return the list of instructions to execute on the tree to insert the property into it
     * or null if there is nothing to create at this point.
     */
    List<OpDef> getInstructions(CommonTree fullTree, CommonTree immediateParent, Property property);
}
