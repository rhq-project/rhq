package org.rhq.plugins.apache.mapping;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMapIndex;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectiveToSimpleProperty;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
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
public enum DirectiveMappingEnum {

    DirectivePerMap {
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
    
    ParamPerMap {
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
    
    DirectivePerMapIndex {
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

    SimpleProp {
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
