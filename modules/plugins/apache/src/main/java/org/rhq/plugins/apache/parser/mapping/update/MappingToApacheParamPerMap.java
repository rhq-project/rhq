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
public class MappingToApacheParamPerMap extends ConfigurationToApacheBase{

        public void updateList(PropertyDefinitionList propDef, Property prop,
                        ApacheDirective listNode, int seq) throws ApacheParserException {
                
              String propertyName = propDef.getName();
              PropertyDefinitionMap memberPropDef = (PropertyDefinitionMap)propDef.getMemberDefinition();
          
              List<ApacheDirective> nodes = tree.search(listNode, propertyName);
              
               //THERE IS NO CONFIGURATION ALL NODES RELATED TO THE CONFIGURATION DEFINITION WILL BE DELETED
              if (prop==null)
              {
                      for (ApacheDirective node : nodes){
                              node.remove();
                      }
                      return;
              }
              
              PropertyList list = (PropertyList) prop;
          List<List<PropertyMap>> map = sort(list);
                               
          nodes = tree.search(listNode, propertyName);
        
      
       //THERE IS MORE CONFIGURATIONS THAN NODES, NEW NODES WILL BE CREATED
        
         if (map.size()>nodes.size())
         {
                 for (int i=0;i<map.size()-nodes.size();i++){
                         tree.createNode(listNode,propertyName);
                 }
         }
         
         //THERE IS LESS CONFIGURATIONS THAN NODES, REDUDANT NODES WILL BE DELETED
         if (map.size()<nodes.size())
         {
                 for (int i=0;i<nodes.size()-map.size();i++){
                       nodes.get(nodes.size()-i-1).remove();
                 }
         }
         
     
        nodes = tree.search(listNode, propertyName);
     
        int i=0;
        StringBuffer param= new StringBuffer();
        for (List<PropertyMap> directive: map){
                for (PropertyMap propMap : directive){
                       for (String propDefMap : memberPropDef.getPropertyDefinitions().keySet())
                       {//for (Property propVal : propMap.getMap().values()){
                          Property propVal = propMap.get(propDefMap);
                          if (propVal!=null)
                             if (!propVal.getName().equals("_index")){
                                  param.append(" "+((PropertySimple) propVal).getStringValue());
                                }
                        }
                            
                }
                List<String> params = ApacheDirectiveRegExpression.createParams(param.toString(), propertyName);
                nodes.get(i).setValues(params);                
                param = new StringBuffer();
                i++;
          }
       }
        

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                        ApacheDirective mapNode, int seq) throws ApacheParserException {        
        }

        public void updateSimple(ApacheDirective parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws ApacheParserException {
                
        }
        
        private List<List<PropertyMap>> sort(PropertyList list){
                List<List<PropertyMap>> map = new ArrayList<List<PropertyMap>>();
                
                int next = Integer.MAX_VALUE;
                int min=0;
                int count = 0;
                
                List<PropertyMap> tempList = new ArrayList<PropertyMap>();
                
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
                             tempList.add(propMap);
                         count = count + 1;
                      }
                      if (value > min & value<next){
                             next = value;
                      }
                    }
                   min = next;
                   next = Integer.MAX_VALUE;
                   if (!tempList.isEmpty())
                   {
                   map.add(tempList);
                   tempList = new ArrayList<PropertyMap>();
                   }
                }
                return map;
        }
        
}
