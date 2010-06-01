package org.rhq.plugins.apache.parser.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    /**
     * Basic implementation of updating the Augeas with data from RHQ configuration.
     * The mapping is based on the RHQ property names.
     * 
     * @author Filip Drabek
     *
     */
    public class ConfigurationToApacheSimple implements ConfigurationToApache {

        protected ApacheDirectiveTree tree;

        public ConfigurationToApacheSimple() {

        }

        public void setTree(ApacheDirectiveTree tree) {
            this.tree = tree;
        }

        public void updateResourceConfiguration(ApacheDirective node, ConfigurationDefinition resourceConfigDef,
            Configuration resourceConfig) throws ApacheParserException {

            Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
            PropertyMap startProp = new PropertyMap();

            for (Property property : resourceConfig.getProperties())
                startProp.put(property);

            for (PropertyDefinition propDef : propDefs) {
                updateProperty(propDef, startProp, node, 0);
            }
        }

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop, ApacheDirective mapNode, int seq)
            throws ApacheParserException {

            PropertyMap map = null;

            if (prop instanceof PropertyList) {
                PropertyList lst = (PropertyList) prop;
                List<Property> props = lst.getList();
                map = (PropertyMap) props.get(seq - 1);
            }

            if (prop instanceof PropertyMap) {
                PropertyMap mp = (PropertyMap) prop;
                map = (PropertyMap) mp.get(propDefMap.getName());
            }

            List<ApacheDirective> nodes = tree.search(mapNode, propDefMap.getName());

            ApacheDirective node;
            int i = 0;
            if (nodes.isEmpty() | nodes.size() < seq) {
                node = tree.createNode(mapNode, propDefMap.getName());
                nodes.add(node);
                i = ((seq == 0) ? 0 : seq - 1);
            } else if (seq == 0) {
                node = nodes.get(0);
                i = 0;
            } else {
                node = nodes.get(seq - 1);
                i = seq - 1;
            }

            for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
                updateProperty(mapEntryPropDef, map, nodes.get(i), 0);
            }

        }

        public void updateList(PropertyDefinitionList propDef, Property prop, ApacheDirective listNode, int seq)
            throws ApacheParserException {

            PropertyList listProperty = null;
            PropertyDefinition childDefinition = propDef.getMemberDefinition();

            if (prop instanceof PropertyList) {
                PropertyList lst = (PropertyList) prop;
                listProperty = (PropertyList) lst.getList().get(seq - 1);
            }

            if (prop instanceof PropertyMap) {
                PropertyMap map = (PropertyMap) prop;
                listProperty = (PropertyList) map.get(propDef.getName());
            }

            List<ApacheDirective> nodes = tree.search(listNode, propDef.getName());
            ApacheDirective node = null;

            if (nodes.isEmpty() | nodes.size() < seq) {
                node = tree.createNode(listNode, propDef.getName());
                nodes.add(node);
            } else
                node = nodes.get(seq);

            int i = 1;
            for (Property prp : listProperty.getList()) {
                updateProperty(childDefinition, listProperty, node, i);
                i = i + 1;
            }
        }

        public void updateSimple(ApacheDirective parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
            throws ApacheParserException {

            PropertySimple simpleProp = null;
            ApacheDirective node = null;
            if (prop instanceof PropertyList) {
                PropertyList lst = (PropertyList) prop;
                List<Property> props = lst.getList();
                simpleProp = (PropertySimple) props.get(seq - 1);
            }

            if (prop instanceof PropertyMap) {
                PropertyMap map = (PropertyMap) prop;
                simpleProp = (PropertySimple) map.get(propDef.getName());
            }

            List<ApacheDirective> nodes = tree.search(parentNode, propDef.getName());

            if (nodes.isEmpty()) {
                node = tree.createNode(parentNode, propDef.getName());
            } else
                node = nodes.get(0);

            List<String> val = new ArrayList<String>();
            val.add(simpleProp.getStringValue());
            
            node.setValues(val);            
        }

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
