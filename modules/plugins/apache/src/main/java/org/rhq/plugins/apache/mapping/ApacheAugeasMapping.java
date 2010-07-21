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
package org.rhq.plugins.apache.mapping;

import java.util.Collection;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.rhqtransform.RhqAugeasMapping;
/**
 * 
 * @author Filip Drabek
 *
 */
public class ApacheAugeasMapping implements RhqAugeasMapping {

	private AugeasTree tree;
	public ApacheAugeasMapping(AugeasTree tree) {
	    this.tree = tree;
	}
	
	public Configuration updateConfiguration(AugeasNode node,ConfigurationDefinition configDef){

        Configuration resourceConfig = new Configuration();

        Collection<PropertyDefinition> propDefs = configDef.getPropertyDefinitions().values();

        for (PropertyDefinition propDef : propDefs) {
        	DirectiveMapping mapping = ApacheDirectiveRegExpression.getMappingType(propDef.getName());
        	ConfigurationDefinition def = new ConfigurationDefinition("temp","");
        	 def.put(propDef);
        	Configuration conf = mapping.mapToConfiguration(tree, node, def);
        	for (Property prop : conf.getProperties())
        	{
        		resourceConfig.put(prop);
        	}
        }
        return resourceConfig;
	}
	
    public void updateAugeas(AugeasProxy augeasComponent, Configuration config, ConfigurationDefinition configDef) {
     
    }

    public Configuration updateConfiguration(AugeasProxy augeasComponent, ConfigurationDefinition configDef) {
        return null;
    }
    
    public void updateAugeas(AugeasNode node,Configuration config,ConfigurationDefinition configDef){
    	  
          Collection<PropertyDefinition> propDefs = configDef.getPropertyDefinitions().values();

          for (PropertyDefinition propDef : propDefs) {
          	DirectiveMapping mapping = ApacheDirectiveRegExpression.getMappingType(propDef.getName());
          	ConfigurationDefinition def = new ConfigurationDefinition("temp","");
          	  def.put(propDef);
          	mapping.mapToAugeas(tree, node, config, def);
          }
    }

}
