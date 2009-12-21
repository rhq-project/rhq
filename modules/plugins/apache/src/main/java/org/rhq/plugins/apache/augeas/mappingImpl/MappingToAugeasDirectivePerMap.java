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
package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.mapping.ApacheDirectiveRegExpression;
import org.rhq.plugins.apache.mapping.ConfigurationToAugeasApacheBase;
import org.rhq.rhqtransform.AugeasRhqException;
/**
 * 
 * @author Filip Drabek
 *
 */
public class MappingToAugeasDirectivePerMap extends ConfigurationToAugeasApacheBase{

        public void updateList(PropertyDefinitionList propDef, Property prop,
                        AugeasNode listNode, int seq) throws AugeasRhqException {
                
       String propertyName = propDef.getName();
       PropertyDefinition memberPropDef = propDef.getMemberDefinition();
       List<AugeasNode> nodes = tree.matchRelative(listNode, propertyName);
      
       //THERE IS NO CONFIGURATION ALL NODES RELATED TO THE CONFIGURATION DEFINITION WILL BE DELETED
      if (prop==null)
      {
              for (AugeasNode node : nodes){
                      node.remove(false);
              }
              return;
      }
      
       PropertyList list = (PropertyList) prop;
       //THERE IS MORE CONFIGURATIONS THAN NODES, NEW NODES WILL BE CREATED
       if (list.getList().size()>nodes.size())
       {
               for (int i=0;i<list.getList().size()-nodes.size();i++){
                       tree.createNode(listNode,propertyName,null,nodes.size()+i+1);
               }
       }
       
       //THERE IS LESS CONFIGURATIONS THAN NODES, REDUDANT NODES WILL BE DELETED
       if (list.getList().size()<nodes.size())
       {
               for (int i=0;i<nodes.size()-list.getList().size();i++){
                       nodes.get(nodes.size()-i).remove(false);
               }
       }
       
       nodes = tree.matchRelative(listNode, propertyName);
       int i=0;
       
       for (Property property : list.getList()){
               updateProperty(memberPropDef, property, nodes.get(i), i);
               i=i+1;
          }       
       }
       

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                        AugeasNode mapNode, int seq) throws AugeasRhqException {
                
                PropertyMap propMap = (PropertyMap) prop;
                String propertyName = propDefMap.getName();
                
                 
                StringBuffer param= new StringBuffer();
               
               for (PropertyDefinition propVal : propDefMap.getPropertyDefinitions().values()){
                       
                       PropertySimple property = propMap.getSimple(propVal.getName());
                       if (property!=null){
                       
                       String value = property.getStringValue();
                          if (value!=null)
                          param.append(" "+ value);
                        }
               }
                        
             List<String> params = ApacheDirectiveRegExpression.createParams(param.toString(), propertyName);
         
                        List<AugeasNode> nodes = mapNode.getChildByLabel("param");
                        
                      //THERE IS MORE CONFIGURATIONS THAN NODES, NEW NODES WILL BE CREATED
                if (params.size()>nodes.size())
                {
                        for (int i=0;i<params.size()-nodes.size();i++){
                                tree.createNode(mapNode,"param",null,nodes.size()+i+1);
                        }
                }
                
                //THERE IS LESS CONFIGURATIONS THAN NODES, REDUDANT NODES WILL BE DELETED
                if (params.size() < nodes.size())
                {
                        for (int i=0;i<nodes.size()-params.size();i++){
                              nodes.get(params.size()+i).remove(false);
                        }
                }
                
                nodes = tree.matchRelative(mapNode, "param");
                
                int i =0;
                for (AugeasNode tempNode : nodes){
                    tempNode.setValue(params.get(i));
                        i++;
                }
                
        }

        public void updateSimple(AugeasNode parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws AugeasRhqException {
                
        }

}
