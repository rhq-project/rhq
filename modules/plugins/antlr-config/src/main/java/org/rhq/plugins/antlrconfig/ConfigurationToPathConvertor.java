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

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;

/**
 * Provides the means to convert the configuration property structure to the tree structure of the target
 * configuration file's AST.
 * 
 * @author Lukas Krejci
 */
public interface ConfigurationToPathConvertor {

    String getTreePath(Property property);
    
    /**
     * 
     * @param treePath the individual path elements won't have their type set, 
     * but all other info will be available (i.e. name, text and position). The position
     * is absolute (nth child of the parent, instead of nth child of given type)
     * @return
     */
    PropertyDefinition getPropertyDefinition(List<PathElement> treePath);
}
