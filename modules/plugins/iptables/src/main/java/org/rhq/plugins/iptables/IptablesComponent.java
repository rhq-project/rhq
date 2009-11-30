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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.rhqtransform.RhqConfig;
/**
 * 
 * @author Filip Drabek
 *
 */
public class IptablesComponent implements AugeasRHQComponent, ConfigurationFacet{

       private ResourceContext context;
       private final Log log = LogFactory.getLog(this.getClass());
       private AugeasTree augeasTree;
       private AugeasComponent augeasComponent;
       
       public void start(ResourceContext context)
                     throws InvalidPluginConfigurationException, Exception {
              this.context = context;
       }

       public void stop() {
              
       }

       public AvailabilityType getAvailability() {
              return AvailabilityType.UP;
              }

       public Configuration loadResourceConfiguration() throws Exception {
              Configuration pluginConfiguration = new Configuration();
              
              AugeasTree tree=null;
              AugeasComponent augeas =null;
              try {       
                      RhqConfig config = new RhqConfig(context.getPluginConfiguration());
                      augeas = new AugeasComponent(config);
                      augeas.load();
                      tree = augeas.getAugeasTree("Iptables", true);
                      tree.load();
                      
              }catch(Exception e)
              {
                     log.error(e.getMessage());
                     
              }
       return pluginConfiguration;
       }
       

       public void updateResourceConfiguration(ConfigurationUpdateReport report) {
              Configuration pluginConfiguration = new Configuration();
              AugeasTree tree=null;
              AugeasComponent augeas =null;
              try {       
                      RhqConfig config = new RhqConfig(context.getPluginConfiguration());
                      augeas = new AugeasComponent(config);
                      
                      tree = augeas.getAugeasTree("Iptables", false);
                      
                      
              }catch(Exception e)
              {
                     log.error(e);
              }
              
       }

       public void loadAugeas() throws Exception{              
              RhqConfig config = new RhqConfig(context.getPluginConfiguration());
              augeasComponent = new AugeasComponent(config);
              augeasComponent.load();
              augeasTree = augeasComponent.getAugeasTree("Iptables", true);
              augeasTree.load();
                      
       }
       public AugeasComponent getAugeasComponent() throws Exception{
       if (augeasComponent == null)
              loadAugeas();
       
       return augeasComponent;
       }

       public AugeasTree getAugeasTree() throws Exception {
               if (augeasTree == null)
                     loadAugeas();
              
              return augeasTree;
       }

}
