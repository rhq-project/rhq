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
package org.rhq.plugins.apache.parser.mapping.update;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheParserException;
import org.rhq.plugins.apache.parser.mapping.ApacheDirectiveRegExpression;
/**
 * 
 * @author Filip Drabek
 *
 */
public class MappingToApacheDirectivePerMapIndex extends ConfigurationToApacheBase{

         public void updateList(PropertyDefinitionList propDef, Property prop,
                           ApacheDirective listNode, int seq) throws ApacheParserException {
         
                  String name = propDef.getName();
                  List<ApacheDirective> nodes = tree.search(listNode, name);
                  PropertyList property = (PropertyList)prop;
                  
                   if (prop==null)
                   {
                     for (ApacheDirective node : nodes){
                      node.remove();
                      }
                  return;
                   }
                  
                  int nr = property.getList().size();
                  
                  //THERE IS MORE NODES THAN CONFIGURATIONS
                  if (nodes.size()>nr){
                   for (int i=0;i<nodes.size()-nr;i++){
                    nodes.get(nr+i).remove();
                    }
                  }
                  //THERE IS LESS NODES THAN CONFIGURATIONS
                  if (nodes.size()<nr){
                      for (int i=0;i<nr-nodes.size();i++){
                        tree.createNode(listNode,name);
                      }
                   }
                  
                  //update the collection so that we have equal nr. of nodes and properties
                 nodes = tree.search(listNode, name);
                 PropertyDefinition memberPropDef = ((PropertyDefinitionList) propDef).getMemberDefinition();
                 
                 int i=0;
                 List<PropertyMap> propertyMap = sort(property);
                 
                 for (Property pr : propertyMap){
                         updateProperty(memberPropDef, pr, nodes.get(i), i);
                         i=i+1;
                 }
                
        }

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                        ApacheDirective mapNode, int seq) throws ApacheParserException {
                
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
                mapNode.setValues(params);
        }

        public void updateSimple(ApacheDirective parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws ApacheParserException {
                
        }

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
