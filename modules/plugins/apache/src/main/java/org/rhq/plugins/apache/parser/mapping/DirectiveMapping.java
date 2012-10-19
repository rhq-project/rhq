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
package org.rhq.plugins.apache.parser.mapping;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.mapping.load.MappingDirectivePerMap;
import org.rhq.plugins.apache.parser.mapping.load.MappingDirectivePerMapIndex;
import org.rhq.plugins.apache.parser.mapping.load.MappingDirectiveToSimpleProperty;
import org.rhq.plugins.apache.parser.mapping.load.MappingParamPerMap;
import org.rhq.plugins.apache.parser.mapping.load.MappingPositionToConfiguration;
import org.rhq.plugins.apache.parser.mapping.update.MappingToApacheDirectivePerMap;
import org.rhq.plugins.apache.parser.mapping.update.MappingToApacheDirectivePerMapIndex;
import org.rhq.plugins.apache.parser.mapping.update.MappingToApacheDirectiveToSimple;
import org.rhq.plugins.apache.parser.mapping.update.MappingToApacheParamPerMap;

/**
 * This enum represents the list of mapping strategies we use in the resource configuration.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public enum DirectiveMapping {

    DIRECTIVE_PER_MAP {
        public Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode, ConfigurationDefinition resourceConfigDef) {
            ApacheToConfiguration config = new MappingDirectivePerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);

        };
        
        public void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToApache mapping = new MappingToApacheDirectivePerMap();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
        
    },
    
    PARAM_PER_MAP {
        public Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode, ConfigurationDefinition resourceConfigDef) {
            ApacheToConfiguration config = new MappingParamPerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef) {
        	ConfigurationToApache mapping = new MappingToApacheParamPerMap();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
    },
    
    DIRECTIVE_PER_MAP_INDEX {
        public Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode, ConfigurationDefinition resourceConfigDef) {
            ApacheToConfiguration config = new MappingDirectivePerMapIndex();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToApache mapping = new MappingToApacheDirectivePerMapIndex();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
    },
    
    POSITION_PROPERTY {
        public Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode, ConfigurationDefinition resourceConfigDef) {
            ApacheToConfiguration config = new MappingPositionToConfiguration();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef){            
        }
    },
    
    SIMPLE_PROP {
        public Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode, ConfigurationDefinition resourceConfigDef) {
            ApacheToConfiguration config = new MappingDirectiveToSimpleProperty();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToApache mapping = new MappingToApacheDirectiveToSimple();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
    };

    /**
     * Creates the configuration based on the supplied configuration definition,
     * parsing the augeas tree starting at the start node.
     * 
     * @param tree the augeas tree
     * @param startNode the starting node
     * @param resourceConfigDef the config definition
     * @return the parsed configuration
     */
    public abstract Configuration mapToConfiguration(ApacheDirectiveTree tree, ApacheDirective startNode,
        ConfigurationDefinition resourceConfigDef);

    public abstract void mapToAugeas(ApacheDirectiveTree tree, ApacheDirective node, Configuration config, ConfigurationDefinition configDef) ;
}
