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
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

/**
 * Component for configuring the &lt;Directory&gt; and underlying directives inside
 * Apache configuration.
 * 
 * @author Lukas Krejci
 */
public class ApacheDirectoryComponent implements ResourceComponent<ApacheVirtualHostServiceComponent>, ConfigurationFacet, DeleteResourceFacet {
	 
	private final Log log = LogFactory.getLog(this.getClass());
    public static final String REGEXP_PROP = "regexp";
    public static final String DIRECTORY_DIRECTIVE = "<Directory";
    
    private ResourceContext<ApacheVirtualHostServiceComponent> resourceContext;
    
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
        ApacheDirectiveTree tree = parentVirtualHost.loadParser();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        
        ApacheDirective virtualHostNode = parentVirtualHost.getNode(tree);
        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(virtualHostNode), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();

        ApacheDirectiveTree tree = null;
        try {
            tree = parentVirtualHost.loadParser();
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            ApacheDirective directoryNode = getNode(tree.getRootNode());
            mapping.updateApache(directoryNode, report.getConfiguration(), resourceConfigDef);
            parentVirtualHost.saveParser(tree);

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            log.info("Apache configuration was updated");
            
            resourceContext.getParentResourceComponent().finishConfigurationUpdate(report);
        } catch (Exception e) {
            if (tree != null)
                log.error("Augeas failed to save configuration ");
            else
                log.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
   }


    public void deleteResource() throws Exception {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();
        ApacheDirectiveTree tree = parentVirtualHost.loadParser();
        ApacheDirective virtualHostNode = parentVirtualHost.getNode(tree);
        
        ApacheDirective myNode = getNode(virtualHostNode);
        
        if (myNode != null) {
            myNode.remove();
            resourceContext.getParentResourceComponent().saveParser(tree);
            
            ApacheVirtualHostServiceComponent parentVhost = resourceContext.getParentResourceComponent();
            
           //TODO do we want to delete empty file?
           // parentVhost.deleteEmptyFile(tree, myNode);
            parentVhost.conditionalRestart();
        } else {
            log.info("Could find the configuration corresponding to the directory " + resourceContext.getResourceKey() + ". Ignoring.");
        }
    }

    /**
     * Gets the node from under given node corresponding to the Directory this
     * component is managing.
     * 
     * @param virtualHost the node of the parent virtualHost (or root node of the augeas tree)
     * @return
     */
    public ApacheDirective getNode(ApacheDirective virtualHost) {
        ApacheDirective directory = AugeasNodeSearch.findNodeById(virtualHost, resourceContext.getResourceKey());        
        return directory;
    }
    
    public ApacheDirective getNode(){
        ApacheDirectiveTree tree = loadParser();
        ApacheDirective virtHostNode = resourceContext.getParentResourceComponent().getNode(tree);
        return getNode(virtHostNode);
    }
    
    
    /**
     * @see ApacheServerComponent#finishConfigurationUpdate(ConfigurationUpdateReport)
     */
    public void finishConfigurationUpdate(ConfigurationUpdateReport report) {
        resourceContext.getParentResourceComponent().finishConfigurationUpdate(report);
    }
    
    public ApacheDirectiveTree loadParser(){
        return resourceContext.getParentResourceComponent().loadParser();
    }
    
    public boolean saveParser(ApacheDirectiveTree tree){
        return resourceContext.getParentResourceComponent().saveParser(tree);
    }
}
