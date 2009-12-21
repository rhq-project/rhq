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

import java.util.List;

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
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;

/**
 * Component for configuring the &lt;Directory&gt; and underlying directives inside
 * Apache configuration.
 * 
 * @author Lukas Krejci
 */
public class ApacheDirectoryComponent implements ResourceComponent<ApacheVirtualHostServiceComponent>, ConfigurationFacet, DeleteResourceFacet {
	 
	private final Log log = LogFactory.getLog(this.getClass());
    public static final String DIRECTIVE_INDEX_PROP = "directiveIndex";

    ResourceContext<ApacheVirtualHostServiceComponent> resourceContext;
    
    public void start(ResourceContext<ApacheVirtualHostServiceComponent> context) throws InvalidPluginConfigurationException, Exception {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();
        AugeasTree tree = parentVirtualHost.getServerConfigurationTree();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        
        AugeasNode virtualHostNode = parentVirtualHost.getNode(tree);
        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(virtualHostNode), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();
        
       AugeasTree tree=null;
         try {
       tree = parentVirtualHost.getServerConfigurationTree();
         ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
       ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
       AugeasNode directoryNode = getNode(tree.getRootNode());
       mapping.updateAugeas(directoryNode,report.getConfiguration(), resourceConfigDef);
       tree.save();
       
       report.setStatus(ConfigurationUpdateStatus.SUCCESS);
       log.info("Apache configuration was updated");
         }catch(Exception e){
              if (tree!=null)
                   log.error("Augeas failed to save configuration "+tree.summarizeAugeasError());
              else
                   log.error("Augeas failed to save configuration",e);
          report.setStatus(ConfigurationUpdateStatus.FAILURE);		
         }
   }


    public void deleteResource() throws Exception {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();
        AugeasTree tree = parentVirtualHost.getServerConfigurationTree();
        AugeasNode virtualHostNode = parentVirtualHost.getNode(tree);
        
        AugeasNode myNode = getNode(virtualHostNode);
        tree.removeNode(myNode, true);
        tree.save();
    }

    /**
     * Gets the node from under given node corresponding to the Directory this
     * component is managing.
     * 
     * @param virtualHost the node of the parent virtualHost (or root node of the augeas tree)
     * @return
     */
    private AugeasNode getNode(AugeasNode virtualHost) {
        List<AugeasNode> directories = virtualHost.getChildByLabel("<Directory");
        int index = resourceContext.getPluginConfiguration().getSimple(DIRECTIVE_INDEX_PROP).getIntegerValue();
        
        for(AugeasNode dir : directories) {
            if (dir.getSeq() == index) {
                return dir;
            }
        }
        throw new IllegalStateException("No Directory directive found in the Apache configuration on the configured index.");
    }
}
