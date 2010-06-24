package org.rhq.plugins.apache.parser.mapping;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParserException;

/**
 * Implementations of this interface provide mapping of Augeas data
 * to the RHQ configuration.
 * 
 * @author Filip Drabek
 *
 */
public interface ApacheToConfiguration {

    /**
     * Sets the Augeas tree that should be worked with.
     * 
     * @param tree
     */
    public void setTree(ApacheDirectiveTree tree);


    /**
     * Loads the RHQ configuration instance from the Augeas tree.
     *  
     * @param startNode the node to start the mapping from
     * @param resourceConfigDef the configuration definition to use
     * @return the RHQ configuration with the values loaded from the Augeas tree 
     * @throws ApacheParserException
     */
    public Configuration loadResourceConfiguration(ApacheDirective startNode, ConfigurationDefinition resourceConfigDef)
        throws ApacheParserException;

    /**
     * Loads a single property from given node.
     * 
     * @param propDef the definition of the property
     * @param parentNode the parent node from which the property should be loaded.
     * @return
     * @throws ApacheParserException
     */
    public Property loadProperty(PropertyDefinition propDef, ApacheDirective parentNode) throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a simple property from the property definition.
     * 
     * @param propDefSimple the definition of the property
     * @param parentNode the parent node where the property should be looked for
     * @return
     * @throws ApacheParserException
     */
    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, ApacheDirective parentNode) throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a map property from the data in the parent node.
     * 
     * @param propDefMap the definition of the property map
     * @param parentNode the parent node
     * @return
     * @throws ApacheParserException
     */
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, ApacheDirective parentNode) throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a property list from the data in the parent node.
     * 
     * @param propDefList
     * @param parentNode
     * @return
     * @throws ApacheParserException
     */
    public Property createPropertyList(PropertyDefinitionList propDefList, ApacheDirective parentNode) throws ApacheParserException;
}

