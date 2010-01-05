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

import java.util.ArrayList;
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
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.plugins.apache.mapping.ApacheDirectiveRegExpression;
import org.rhq.plugins.apache.mapping.ConfigurationToAugeasApacheBase;
import org.rhq.rhqtransform.AugeasRhqException;
/**
 * 
 * @author Filip Drabek
 *
 */
public class MappingToAugeasDirectivePerMapIndex extends ConfigurationToAugeasApacheBase{

         public void updateList(PropertyDefinitionList propDef, Property prop,
                           AugeasNode listNode, int seq) throws AugeasRhqException {
         
                  String name = propDef.getName();
                  List<AugeasNode> nodes = tree.matchRelative(listNode, name);
                  PropertyList property = (PropertyList)prop;
                  
                   if (prop==null)
                   {
                     for (AugeasNode node : nodes){
                      node.remove(false);
                      }
                  return;
                   }
                  
                  int nr = property.getList().size();
                  
                  //THERE IS MORE NODES THAN CONFIGURATIONS
                  if (nodes.size()>nr){
                   for (int i=0;i<nodes.size()-nr;i++){
                    nodes.get(nr+i).remove(false);
                    }
                  }
                  //THERE IS LESS NODES THAN CONFIGURATIONS
                  if (nodes.size()<nr){
                      for (int i=0;i<nr-nodes.size();i++){
                        tree.createNode(listNode,name,null,nodes.size()+i+1);
                      }
                   }
                  
                  //update the collection so that we have equal nr. of nodes and properties
                 nodes = tree.matchRelative(listNode, name);
                 PropertyDefinition memberPropDef = ((PropertyDefinitionList) propDef).getMemberDefinition();
                 
                 int i=0;
                 List<PropertyMap> propertyMap = sort(property);
                 
                 for (Property pr : propertyMap){
                         updateProperty(memberPropDef, pr, nodes.get(i), i);
                         i=i+1;
                 }
                
        }

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                        AugeasNode mapNode, int seq) throws AugeasRhqException {
                
                PropertyMap propMap = (PropertyMap) prop;
                String propertyName = prop.getName();
                StringBuffer param= new StringBuffer();
                
                for (PropertyDefinition propVal : propDefMap.getPropertyDefinitions().values()){
                    
                    PropertySimple property = propMap.getSimple(propVal.getName());
                    if (property!=null){
                    if (!property.getName().equals("_index")){
                         String value = property.getStringValue();
                          if (value!=null)
                            param.append(" "+ value);
                        }
                     }
                 }
                
                List<String> params = ApacheDirectiveRegExpression.createParams(param.toString(), propertyName);
                
                List<AugeasNode> nodes = mapNode.getChildByLabel("param");
                
              //THERE IS MORE CONFIGURATIONS THAN NODES, NEW NODES WILL BE CREATED
              if (params.size()>nodes.size()){
                for (int i=0;i<params.size()-nodes.size();i++){
                        tree.createNode(mapNode,"param",null,nodes.size()+i+1);
                    }
                }
        
              //THERE IS LESS CONFIGURATIONS THAN NODES, REDUDANT NODES WILL BE DELETED
              if (params.size() < nodes.size()){
                 for (int i=0;i<nodes.size()-params.size();i++){
                      nodes.get(params.size()+i).remove(false);
                   }
                }
        
              nodes = tree.matchRelative(mapNode, "param");

              int i=0;
              for (String value : params){
                     nodes.get(i).setValue(value);
                     i=i+1;
                }
        }

        public void updateSimple(AugeasNode parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws AugeasRhqException {
                
        }
        
 /*       private void sort(PropertyList list){
            List<Property> propList = list.getList(); 
                
            int min = -1;
            int minIndex = 0;
            int index = 0;
            Integer value = 0;
            
                for (int i=0;i<list.getList().size();i++){
                        index = i;
                    while (index < list.getList().size()){
                  PropertyMap map = (PropertyMap)propList.get(index);
                  PropertySimple simple = (PropertySimple) map.get("_index");
                  value = simple.getIntegerValue();
                  if (value==null){
                	  value = 0;
                  }
                  if (value.intValue() < min){
                          propList.set(index, propList.get(minIndex));
                          propList.set(minIndex, map);
                          
                          min = value;
                          minIndex = index;
                  }                  
                  index++;
                    }
            }
                
        }*/
        private List<PropertyMap> sort(PropertyList list){
            List<PropertyMap> map = new ArrayList<PropertyMap>();
            
            int next = Integer.MAX_VALUE;
            int min=0;
            int count = 0;
               
            while(count<list.getList().size()){
               for (Property prop : list.getList()){
                  PropertyMap propMap = (PropertyMap)prop;
                  PropertySimple propSim = ((PropertySimple)propMap.get("_index"));
                  int value;
                  if (propSim ==null | propSim.getIntegerValue() == null)
                    value = 0;
                          else
                    value = propSim.getIntegerValue().intValue();
                  
                  if (value == min){
                         map.add(propMap);
                     count = count + 1;
                  }
                  if (value > min & value<next){
                         next = value;
                  }
                }
               min = next;
               next = Integer.MAX_VALUE;
            }
            return map;
    }

}
