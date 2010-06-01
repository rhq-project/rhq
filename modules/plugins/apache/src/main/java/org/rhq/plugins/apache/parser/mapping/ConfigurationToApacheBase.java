package org.rhq.plugins.apache.parser.mapping;

import java.util.Collection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParserException;

public abstract class ConfigurationToApacheBase implements ConfigurationToApache {

    protected ApacheDirectiveTree tree;

    public void setTree(ApacheDirectiveTree tree) {
        this.tree = tree;
    }

    public void updateResourceConfiguration(ApacheDirective node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws ApacheParserException {

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
 
        for (PropertyDefinition propDef : propDefs) {
            Property prop = resourceConfig.get(propDef.getName());
            updateProperty(propDef, prop, node, 0);
        }
    }

    public abstract void updateMap(PropertyDefinitionMap propDefMap, Property prop, ApacheDirective mapNode, int seq)
        throws ApacheParserException ;
    
    public abstract void updateList(PropertyDefinitionList propDef, Property prop, ApacheDirective listNode, int seq)
        throws ApacheParserException; 

    public abstract void updateSimple(ApacheDirective parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws ApacheParserException;

    public void updateProperty(PropertyDefinition propDef, Property parentProp, ApacheDirective parentNode, int seq)
        throws ApacheParserException {

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

