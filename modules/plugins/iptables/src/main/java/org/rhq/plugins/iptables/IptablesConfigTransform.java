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
package org.rhq.plugins.iptables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
/**
 * 
 * @author Filip Drabek
 *
 */
public class IptablesConfigTransform {

       private AugeasTree tree;

       
       public IptablesConfigTransform(AugeasTree  tree){
         this.tree = tree;       

       }
       
       public Configuration transform(List<AugeasNode> startNodes,ConfigurationDefinition resourceConfigDef) throws Exception
       {
              Configuration resourceConfig = new Configuration();

                     if (startNodes.isEmpty())
                        { 
                        PropertyList propList = new PropertyList("chains");
                        PropertyMap mp = new PropertyMap("rule");               
                        propList.add(mp);
                        resourceConfig.put(propList);
                        return resourceConfig;
                        }
                           
               PropertyList propList = new PropertyList("chains");
               
                       for (AugeasNode node : startNodes){
                                           //for (AugeasNode nd : node.getChildNodes())
                              PropertyMap mp = new PropertyMap("rule");                                 
                              for (AugeasNode nd : node.getChildByLabel("rule"))
                                 {//rules
                                         
                                  String rule = "";
                                  
                                  for (AugeasNode tempNode : nd.getChildByLabel("parameters"))
                                     {
                                        rule += buildRule(tempNode);
                                     }
                                  
                                  PropertySimple ruleProp = new PropertySimple("param",rule);
                                  mp.put(ruleProp);
                                 }
                              propList.add(mp);                              
               }
                         
               resourceConfig.put(propList);
               return resourceConfig;
       }
       
       private String buildRule(AugeasNode nd) throws Exception{
               String  paramName =  getValue(nd,"/param/paramName");
               String negation = getValue(nd, "/param/negation");
               String value = getValue(nd, "/param/value");
               
               return (((paramName.length()> 1) ?"--" : "-") + paramName + " " + negation + " " +value);
       }
       
       private String getValue(AugeasNode nd,String nm) throws Exception{
         List<AugeasNode> nds = tree.matchRelative(nd, nm);
         if (!nds.isEmpty())
                return nds.get(0).getValue();
         else
                return "";
       }
       
       
       public void updateAugeas(Configuration config,List<AugeasNode> nodes)
       {
              List<String> values = new ArrayList<String>();
              
          Collection<Property> props = config.getAllProperties().values();
          
          for (Property prop : props)
          {
              if (prop.getName().equals("chains")){
                      PropertyList propList = (PropertyList) prop;
                      for (Property property : propList.getList())
                      {
                             if (property.getName().equals("rule"))
                             {
                            PropertyMap propMap = (PropertyMap) property;
                            for (Property propVal : propMap.getMap().values()){
                                  if (propVal.getName().equals("param")){
                                       values.add(((PropertySimple)propVal).getStringValue());
                                  }
                            }
                             }
                      }                      
                }
          }
          String name;
       }
       
       
       public void buildTree(List<AugeasNode> nodes) throws Exception
       {
              List<String> values = new ArrayList<String>();

              for (AugeasNode node : nodes){
                  PropertyMap mp = new PropertyMap("rule");                                 
                  for (AugeasNode nd : node.getChildByLabel("rule"))
                     {       
                      String rule = "";
                      for (AugeasNode tempNode : nd.getChildByLabel("parameters"))
                         {
                            rule += buildRule(tempNode);
                         }
                      
                      values.add(rule);
                     }                  
         }
       }
       
       public void compare(List<String> values,AugeasTree tree,String chainName,String tableName) throws Exception
       {
              String expr = File.separatorChar+tableName+File.separatorChar+chainName;
              
              List<AugeasNode> nodes = tree.matchRelative(tree.getRootNode(), expr);
              
              for (AugeasNode node : nodes)
              {
                     node.remove(false);
              }
              int i =1;
              for (String val : values){
                     i = i + 1;
                     String temp = File.separatorChar+tableName+File.separatorChar+chainName+"["+String.valueOf(i) +"]"+File.separatorChar+"rule";
                     
              }
       }
       
       public void addToAugeas(String parent,String param){
              
       }
       
}
