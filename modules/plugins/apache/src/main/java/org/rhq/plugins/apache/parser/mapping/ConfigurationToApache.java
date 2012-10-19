package org.rhq.plugins.apache.parser.mapping;

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

/**
 * Mapping from RHQ configuration to Augeas tree.
 * 
 * @author Filip Drabek
 *
 */
public interface ConfigurationToApache {

    /**
     * Sets the tree the mapping should work with.
     * 
     * @param tree
     */
    public void setTree(ApacheDirectiveTree tree);

    /**
     * Updates the augeas tree with the data from the RHQ configuration
     * 
     * @param node the node to start updating the data from
     * @param resourceConfigDef configuration definition of the configuration object
     * @param resourceConfig the configuration object to read the data from
     *  
     * @throws ApacheParserException
     */
    public void updateResourceConfiguration(ApacheDirective node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param propDefMap
     * @param prop
     * @param mapNode
     * @param seq
     * @throws ApacheParserException
     */
    public void updateMap(PropertyDefinitionMap propDefMap, Property prop, ApacheDirective mapNode, int seq)
        throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param propDef
     * @param prop
     * @param listNode
     * @param seq
     * @throws ApacheParserException
     */
    public void updateList(PropertyDefinitionList propDef, Property prop, ApacheDirective listNode, int seq)
        throws ApacheParserException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param parentNode
     * @param propDef
     * @param prop
     * @param seq
     * @throws ApacheParserException
     */
    public void updateSimple(ApacheDirective parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws ApacheParserException;

    /**
     * Performs updates in the tree based on the values in the single property (and its descendants).
     * 
     * @param propDef the property definition of the property we are applying to the tree 
     * @param parentProp the parent property of the property we are dealing with
     * @param parentNode the parent node under which we should apply the property
     * @param seq the sequence number of the property we are applying inside the parentProp
     * 
     * @throws ApacheParserException
     */
    public void updateProperty(PropertyDefinition propDef, Property parentProp, ApacheDirective parentNode, int seq)
        throws ApacheParserException;
}
