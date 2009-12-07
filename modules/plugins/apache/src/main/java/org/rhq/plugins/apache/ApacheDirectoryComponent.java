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

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.augeas.AugeasToApacheConfiguration;
import org.rhq.rhqtransform.AugeasRhqException;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class ApacheDirectoryComponent implements ResourceComponent<ApacheVirtualHostServiceComponent>, ConfigurationFacet, DeleteResourceFacet {

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

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() throws Exception {
        ApacheVirtualHostServiceComponent parentVirtualHost = resourceContext.getParentResourceComponent();
        AugeasTree tree = parentVirtualHost.getServerConfigurationTree();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        
        AugeasToApacheConfiguration config = new AugeasToApacheConfiguration() {

            @Override
            public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode node)
                throws AugeasRhqException {
                if ("regexp".equals(propDefSimple.getName())) {
                    List<AugeasNode> regexp = node.getChildByLabel("regexp");
                    return new PropertySimple("regexp", !regexp.isEmpty());
                } else {
                    return super.createPropertySimple(propDefSimple, node);
                }
            }
            
        };
        
        config.setTree(tree);

        AugeasNode virtualHostNode = parentVirtualHost.getNode(tree);
        
        return config.loadResourceConfiguration(getNode(virtualHostNode), resourceConfigDef);
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#updateResourceConfiguration(org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.DeleteResourceFacet#deleteResource()
     */
    public void deleteResource() throws Exception {
        // TODO Auto-generated method stub
        
    }

    private AugeasNode getNode(AugeasNode virtualHost) {
        List<AugeasNode> directories = virtualHost.getChildByLabel("Directory");
        int index = resourceContext.getPluginConfiguration().getSimple(DIRECTIVE_INDEX_PROP).getIntegerValue();
        
        for(AugeasNode dir : directories) {
            if (dir.getSeq() == index) {
                return dir;
            }
        }
        throw new IllegalStateException("No Directory directive found in the Apache configuration on the configured index.");
    }
}
