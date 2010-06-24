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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheParserException;
/**
 * 
 * @author Filip Drabek
 *
 */
public class MappingToApacheDirectiveToSimple extends ConfigurationToApacheBase{

        
        public void updateList(PropertyDefinitionList propDef, Property prop,
                        ApacheDirective listNode, int seq) throws ApacheParserException {
                throw new ApacheParserException("Error in configuration. There can not be any ListProperty in simple directive.");
                
        }

        public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
                        ApacheDirective mapNode, int seq) throws ApacheParserException {
                throw new ApacheParserException("Error in configuration.  There can not be any MapProperty in simple directive.");
        }

        public void updateSimple(ApacheDirective parentNode,
                        PropertyDefinitionSimple propDef, Property prop, int seq)
                        throws ApacheParserException {
                
                String propName = propDef.getName();
                String propertyValue = ((PropertySimple) prop).getStringValue();
                
                List<ApacheDirective> nodes = tree.search(parentNode, propName);
                try {
                //NODE WAS DELETED IN CONFIGURATION
            if (propertyValue == null || propertyValue.equals(""))
               {
                    for (ApacheDirective nod : nodes)
                            nod.remove();
                return;
               }
            
            //NODE IS NOT PRESENT IN AUGEAS
            if (nodes.isEmpty())
                    {
                    ApacheDirective dir = tree.createNode(parentNode, propName);
                    dir.addValue(propertyValue);
                    return;
                    }
            
            //NODE IS PRESENT IN AUGEAS AND IN CONFIGURATION AND WILL BE UPDATED
            if (nodes.size()>1)
                    throw new ApacheParserException("Error in configuration. Directive"+propName+" is declared multiple times.");
          
            List<String> list= new ArrayList<String>();
            list.add(propertyValue);
            nodes.get(0).setValues(list);
                        
                }catch(Exception e){
                        throw new ApacheParserException("Mapping configuration to Augeas failed. "+e.getMessage());
                }
        }

}
