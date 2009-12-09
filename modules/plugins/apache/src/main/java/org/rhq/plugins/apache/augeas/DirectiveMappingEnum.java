package org.rhq.plugins.apache.augeas;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMapIndex;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectiveToSimpleProperty;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
import org.rhq.rhqtransform.AugeasToConfiguration;

/**
 * This enum represents the list of mapping strategies we use in the resource configuration.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public enum DirectiveMappingEnum {

    DirectivePerMap {
        public Configuration execute(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectivePerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);

        };
    },
    ParamPerMap {
        public Configuration execute(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingParamPerMap();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
    },
    DirectivePerMapIndex {
        public Configuration execute(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectivePerMapIndex();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
    },

    SimpleProp {
        public Configuration execute(AugeasTree tree, AugeasNode startNode, ConfigurationDefinition resourceConfigDef) {
            AugeasToConfiguration config = new MappingDirectiveToSimpleProperty();
            config.setTree(tree);
            return config.loadResourceConfiguration(startNode, resourceConfigDef);
        };
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
    public abstract Configuration execute(AugeasTree tree, AugeasNode startNode,
        ConfigurationDefinition resourceConfigDef);

}
