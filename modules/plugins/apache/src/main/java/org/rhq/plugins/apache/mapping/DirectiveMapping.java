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
package org.rhq.plugins.apache.mapping;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMapIndex;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectiveToSimpleProperty;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingPositionToConfiguration;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingToAugeasDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingToAugeasDirectivePerMapIndex;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingToAugeasDirectiveToSimple;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingToAugeasParamPerMap;
import org.rhq.rhqtransform.AugeasToConfiguration;
import org.rhq.rhqtransform.ConfigurationToAugeas;

/**
 * This enum represents the list of mapping strategies we use in the resource configuration.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public enum DirectiveMapping {

    DIRECTIVE_PER_MAP {
        public Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectivePerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);

        };
        
        public void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToAugeas mapping = new MappingToAugeasDirectivePerMap();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
        
    },
    
    PARAM_PER_MAP {
        public Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingParamPerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef) {
        	ConfigurationToAugeas mapping = new MappingToAugeasParamPerMap();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
    },
    
    DIRECTIVE_PER_MAP_INDEX {
        public Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectivePerMapIndex();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToAugeas mapping = new MappingToAugeasDirectivePerMapIndex();
        	mapping.setTree(tree);
        	mapping.updateResourceConfiguration(node, configDef, config);
        }
    },
    
    POSITION_PROPERTY {
        public Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingPositionToConfiguration();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef){            
        }
    },
    
    SIMPLE_PROP {
        public Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectiveToSimpleProperty();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
        
        public void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef){
        	ConfigurationToAugeas mapping = new MappingToAugeasDirectiveToSimple();
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
    public abstract Configuration mapToConfiguration(AugeasTree tree, AugeasNode startNode,
        ConfigurationDefinition resourceConfigDef);

    public abstract void mapToAugeas(AugeasTree tree, AugeasNode node, Configuration config, ConfigurationDefinition configDef) ;
}
