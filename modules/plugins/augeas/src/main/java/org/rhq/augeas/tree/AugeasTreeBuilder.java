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

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;

/**
 * Builds up the in-memory tree representation of the data loaded from Augeas.
 * 
 * @author Filip Drabek
 */
public interface AugeasTreeBuilder {

    /**
     * 
     * @param proxy provides direct access to Augeas should the builder need it
     * @param moduleConfig which Augeas module to use to build the tree 
     * @param name the name of the module to load
     * @param lazy should the tree built lazily or eagerly
     * @return fully built representation of the Augeas data
     * @throws AugeasTreeException
     */
    public AugeasTree buildTree(AugeasProxy proxy, AugeasConfiguration moduleConfig, String name, boolean lazy)
        throws AugeasTreeException;
}
