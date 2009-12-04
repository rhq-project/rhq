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

package org.rhq.rhqtransform.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
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
import org.rhq.rhqtransform.AugeasToConfiguration;
import org.rhq.rhqtransform.NameMap;
/**
 * 
 * @author Filip Drabek
 * @author Ian Springer
 */
public class AugeasToConfigurationSimple implements AugeasToConfiguration {
    private final Log log = LogFactory.getLog(this.getClass());
    protected AugeasTree tree;
    protected NameMap nameMap;
    
       public AugeasToConfigurationSimple(){
                
       }
       
       public void setTree(AugeasTree tree)
       {
              this.tree = tree;
       }
       
       public void setNameMap(NameMap nameMap){
              this.nameMap = nameMap;
       }
       
         public Configuration loadResourceConfiguration(AugeasNode startNode,ConfigurationDefinition resourceConfigDef) throws Exception {

               Configuration resourceConfig = new Configuration();
              
               Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

               for (PropertyDefinition propDef : propDefs) {
                   resourceConfig.put(loadProperty(propDef,startNode));
               }

               return resourceConfig;
           }

           public Property loadProperty(PropertyDefinition propDef, AugeasNode parentNode) throws Exception{
                     
               Property prop;
               if (propDef instanceof PropertyDefinitionSimple) {
                   prop = createPropertySimple((PropertyDefinitionSimple) propDef, parentNode);
               } else if (propDef instanceof PropertyDefinitionMap) {
                   prop = createPropertyMap((PropertyDefinitionMap) propDef,  parentNode);
               } else if (propDef instanceof PropertyDefinitionList) {
                   prop = createPropertyList((PropertyDefinitionList) propDef,  parentNode);
               } else {
                   throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
               }
              return prop;
           }

           public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode node) throws Exception{
               Object value;
               value = node.getValue();
               return new PropertySimple(propDefSimple.getName(), value);
           }

           public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws Exception {
               PropertyMap propMap = new PropertyMap(propDefMap.getName());
               for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
                   propMap.put(loadProperty(mapEntryPropDef, node));
               }
               return propMap;
           }

           public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws Exception{
                              
                  PropertyList propList = new PropertyList(propDefList.getName());
                   
               List<AugeasNode> nodes = tree.matchRelative(node, propDefList.getName());
               PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();
              
               for (AugeasNode listMemberNode : nodes){
                      propList.add(loadProperty(listMemberPropDef,listMemberNode));
               }

               return propList;
           }
}
