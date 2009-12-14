package org.rhq.plugins.apache.mapping;

import java.util.Collection;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.ConfigurationToAugeas;

public abstract class ConfigurationToAugeasApacheBase implements ConfigurationToAugeas {

    protected AugeasTree tree;

    public void setTree(AugeasTree tree) {
        this.tree = tree;
    }

    public void updateResourceConfiguration(AugeasNode node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws AugeasRhqException {

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
 
        for (PropertyDefinition propDef : propDefs) {
        	Property prop = resourceConfig.get(propDef.getName());
            updateProperty(propDef, prop, node, 0);
        }
    }

    public abstract void updateMap(PropertyDefinitionMap propDefMap, Property prop, AugeasNode mapNode, int seq)
        throws AugeasRhqException ;
    
    public abstract void updateList(PropertyDefinitionList propDef, Property prop, AugeasNode listNode, int seq)
        throws AugeasRhqException; 

    public abstract void updateSimple(AugeasNode parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws AugeasRhqException;

    public void updateProperty(PropertyDefinition propDef, Property parentProp, AugeasNode parentNode, int seq)
        throws AugeasRhqException {

        if (propDef instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple propDefSimple = (PropertyDefinitionSimple) propDef;
            updateSimple(parentNode, propDefSimple, parentProp, seq);
        } else if (propDef instanceof PropertyDefinitionMap) {
            PropertyDefinitionMap propDefMap = (PropertyDefinitionMap) propDef;
            updateMap(propDefMap, parentProp, parentNode, seq);
        } else if (propDef instanceof PropertyDefinitionList) {
            PropertyDefinitionList propDefList = (PropertyDefinitionList) propDef;
            updateList(propDefList, parentProp, parentNode, seq);
        } else {
            throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
        }

    }
}
