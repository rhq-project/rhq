package org.rhq.plugins.apache.parser.mapping.load;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParserException;
import org.rhq.plugins.apache.parser.mapping.ApacheToConfiguration;


/**
 * Basic implementation of Augeas mapping to RHQ configuration based on property names.
 * 
 * @author Filip Drabek
 * @author Ian Springer
 */
public class ApacheToConfigurationBase implements ApacheToConfiguration {
    private final Log log = LogFactory.getLog(this.getClass());
    protected ApacheDirectiveTree tree;

    public ApacheToConfigurationBase() {

    }

    public void setTree(ApacheDirectiveTree tree) {
        this.tree = tree;
    }

    public Configuration loadResourceConfiguration(ApacheDirective startNode, ConfigurationDefinition resourceConfigDef)
        throws ApacheParserException {

        Configuration resourceConfig = new Configuration();

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

        for (PropertyDefinition propDef : propDefs) {
            resourceConfig.put(loadProperty(propDef, startNode));
        }

        return resourceConfig;
    }

    public Property loadProperty(PropertyDefinition propDef, ApacheDirective parentNode) throws ApacheParserException {

        Property prop;
        if (propDef instanceof PropertyDefinitionSimple) {
            prop = createPropertySimple((PropertyDefinitionSimple) propDef, parentNode);
        } else if (propDef instanceof PropertyDefinitionMap) {
            prop = createPropertyMap((PropertyDefinitionMap) propDef, parentNode);
        } else if (propDef instanceof PropertyDefinitionList) {
            prop = createPropertyList((PropertyDefinitionList) propDef, parentNode);
        } else {
            throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
        }
        return prop;
    }

    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, ApacheDirective node) throws ApacheParserException {
        Object value;
        value = node.getText();
        return new PropertySimple(propDefSimple.getName(), value);
    }

    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, ApacheDirective node) throws ApacheParserException {
        PropertyMap propMap = new PropertyMap(propDefMap.getName());
        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
            propMap.put(loadProperty(mapEntryPropDef, node));
        }
        return propMap;
    }

    public Property createPropertyList(PropertyDefinitionList propDefList, ApacheDirective node) throws ApacheParserException {

        PropertyList propList = new PropertyList(propDefList.getName());

        List<ApacheDirective> nodes = tree.search(node, propDefList.getName());
        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

        for (ApacheDirective listMemberNode : nodes) {
            propList.add(loadProperty(listMemberPropDef, listMemberNode));
        }

        return propList;
    }
}
