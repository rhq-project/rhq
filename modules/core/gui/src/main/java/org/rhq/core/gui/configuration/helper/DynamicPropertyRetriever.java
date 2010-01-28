/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.core.gui.configuration.helper;

import java.util.List;

import org.rhq.core.domain.configuration.PropertyDefinitionDynamic;

/**
 * Plugins to the dynamic property rendering sections of {@link PropertyRenderingUtility} that perform
 * the actual retrieval of the values.
 * <p/>
 * <strong>In order to associate implementations of this interface with the rendering code, calls must be made to
 * {@link PropertyRenderingUtility#putDynamicPropertyRetriever(org.rhq.core.domain.configuration.PropertyDynamicType,
 * DynamicPropertyRetriever)} to map the property type to the instance.</strong> 
 * <p/>
 * This functionality is abstracted out to prevent the core-gui module from having to make non-core
 * dependencies. Modules using core-gui can implement this interface using whatever means available,
 * such as accessing a database or making a network hop, without having to modify the core-gui dependencies.
 *
 * @author Jason Dobies
 */
public interface DynamicPropertyRetriever {

    /**
     * Returns a list of values to display to the user when prompting for the property given.
     *
     * @param propertyDefinition cannot be <code>null</code>
     * @return list of values to use as both the name and value of the UI inputs; empty list if none are found
     */
    List<String> loadValues(PropertyDefinitionDynamic propertyDefinition);
}
