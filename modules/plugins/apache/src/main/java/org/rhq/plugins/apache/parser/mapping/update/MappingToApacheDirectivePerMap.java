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
public class MappingToApacheDirectivePerMap extends ConfigurationToApacheBase{

        public void updateList(PropertyDefinitionList propDef, Property prop,
                        ApacheDirective listNode, int seq) throws ApacheParserException {
                
       String propertyName = propDef.getName();
       PropertyDefinition memberPropDef = propDef.getMemberDefinition();
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
       //THERE IS MORE CONFIGURATIONS THAN NODES, NEW NODES WILL BE CREATED
       if (list.getList().size()>nodes.size())
       {
               for (int i=0;i<list.getList().size()-nodes.size();i++){
                       tree.createNode(listNode,propertyName);
               }
       }
       
       //THERE IS LESS CONFIGURATIONS THAN NODES, REDUDANT NODES WILL BE DELETED
       if (list.getList().size()<nodes.size())
       {
               for (int i=0;i<nodes.size()-list.getList().size();i++){
                       nodes.get(nodes.size()-1-i).remove();
               }
       }
       
       nodes = tree.search(listNode, propertyName);
       int i=0;
       
       for (Property property : list.getList()){
               updateProperty(memberPropDef, property, nodes.get(i), i);
               i=i+1;
          }       
       }
       

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                ApacheDirective mapNode, int seq) throws ApacheParserException {
                
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
             mapNode.setValues(params);
                
        }

        public void updateSimple(ApacheDirective parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws ApacheParserException {
                
        }

}
