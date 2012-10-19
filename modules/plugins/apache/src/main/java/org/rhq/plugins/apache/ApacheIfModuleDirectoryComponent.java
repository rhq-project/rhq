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
package org.rhq.plugins.apache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheIfModuleDirectoryComponent implements ResourceComponent<ApacheDirectoryComponent>, ConfigurationFacet { 

    private ResourceContext<ApacheDirectoryComponent> context;
    private ApacheDirectoryComponent parentComponent; 
    private final Log log = LogFactory.getLog(this.getClass());
    private static final String IFMODULE_DIRECTIVE_NAME="<IfModule"; 
    
    public void start(ResourceContext<ApacheDirectoryComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        
      this.context = context;    
      parentComponent = context.getParentResourceComponent();

    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
       return parentComponent.getAvailability();
    }

    public Configuration loadResourceConfiguration() throws Exception {       
        ConfigurationDefinition resourceConfigDef = context.getResourceType().getResourceConfigurationDefinition();
        
        AugeasNode directoryNode = parentComponent.getNode();
        AugeasTree tree = parentComponent.getServerConfigurationTree();
        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(directoryNode), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        AugeasTree tree = null;
        try {
            tree = parentComponent.getServerConfigurationTree();
            ConfigurationDefinition resourceConfigDef = context.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            AugeasNode directoryNode = getNode(parentComponent.getNode());
            mapping.updateAugeas(directoryNode, report.getConfiguration(), resourceConfigDef);
            tree.save();

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            log.info("Apache configuration was updated");
            
            context.getParentResourceComponent().finishConfigurationUpdate(report);
        } catch (Exception e) {
            if (tree != null)
                log.error("Augeas failed to save configuration " + tree.summarizeAugeasError());
            else
                log.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
   }
    
    private AugeasNode getNode(AugeasNode virtualHost) {
        AugeasNode directory = AugeasNodeSearch.findNodeById(virtualHost, context.getResourceKey());
        return directory;
      }
    
    public boolean isAugeasEnabled(){
        return parentComponent.isAugeasEnabled();          
    }
}

