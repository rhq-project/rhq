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
package org.rhq.plugins.apache.parser.mapping;

import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheParserException;

public class MappingPositionToConfiguration extends ApacheToConfigurationSimple {
    public static final String LIST_PROPERTY_NAME = "IfModules";
    public static final String MAP_PROPERTY_NAME="IfModule";
    public static final String SIMPLE_PROPERTY_NAME="condition";
    
    public Configuration loadResourceConfiguration(ApacheDirective startNode, ConfigurationDefinition resourceConfigDef)
    throws ApacheParserException {
    if (startNode == null)
        return null;
    
    Configuration resourceConfig = new Configuration();
    String nodeName = startNode.getName();
    ApacheDirective parentNode = getParentName(startNode);
    //List<String> params = ApacheDirectiveSearch.getParams(startNode, parentNode);
    List<String> params = startNode.getValues();
    if (nodeName.equals("<Directory"))
      if (params.size()>0)
        params.remove(0);
    
    PropertyList list = new PropertyList(LIST_PROPERTY_NAME);
    for (String parameter : params){
       PropertyMap map = new PropertyMap(MAP_PROPERTY_NAME);
       PropertySimple condition = new PropertySimple(SIMPLE_PROPERTY_NAME,parameter);
       map.put(condition);
       list.add(map);       
    }
    
    resourceConfig.put(list);
    return resourceConfig;
}
    
 private ApacheDirective getParentName(ApacheDirective node){
      
      ApacheDirective tempNode = node.getParentNode();
      
      while (!tempNode.equals(tree.getRootNode())){
         if (tempNode.getName().equals("<Directory"))
          return tempNode;
         
         if (tempNode.getName().equals("<VirtualHost"))
          return tempNode;
         
         tempNode = tempNode.getParentNode();
      }
      
        return tree.getRootNode();
  }
  
}
