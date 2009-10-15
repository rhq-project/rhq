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
package org.rhq.plugins.antlrconfig.test;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * Tests with the Simple.g grammar
 * 
 * @author Lukas Krejci
 */
@Test
public class Simple {

    private static final String FILE = "config:///file";
    private static final String ASSIGNMENT = "config://assignment";
    private static final String NAME = "config://$1";
    private static final String VALUE = "config://$2";
    private static final String EXPORT = "config://$3";
    
    private static ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("", null);
        
        PropertyDefinitionList file = new PropertyDefinitionList(FILE, null, true, null);
        def.put(file);
        
        PropertyDefinitionMap assignment = new PropertyDefinitionMap(ASSIGNMENT, null, false, (PropertyDefinition)null);
        file.setMemberDefinition(assignment);
        
        assignment.put(new PropertyDefinitionSimple(NAME, null, true, PropertySimpleType.STRING));
        assignment.put(new PropertyDefinitionSimple(VALUE, null, true, PropertySimpleType.STRING));
        assignment.put(new PropertyDefinitionSimple(EXPORT, null, true, PropertySimpleType.BOOLEAN));
        
        return def;
    }
}
