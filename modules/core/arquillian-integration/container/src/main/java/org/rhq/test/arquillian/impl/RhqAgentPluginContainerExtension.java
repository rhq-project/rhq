/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.test.arquillian.impl;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import org.rhq.test.arquillian.impl.util.EnrichmentHook;
import org.rhq.test.arquillian.impl.util.PluginContainerClassEnhancer;
import org.rhq.test.arquillian.spi.PluginContainerOperation;
import org.rhq.test.arquillian.spi.PluginContainerOperationRemedy;
import org.rhq.test.arquillian.spi.PluginContainerPreparator;
import org.rhq.test.arquillian.spi.PostPrepareEnricher;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainerExtension implements LoadableExtension {

    //do this as the first thing... we need to enhance the PluginContainer
    //class before it gets referenced by any other classes in this extension
    static {
        PluginContainerClassEnhancer.init();
    }

    @Override
    public void register(ExtensionBuilder builder) {       
        builder.observer(EnrichmentHook.class)
        .observer(PluginContainerPreparatorExecutor.class)
        .observer(PluginContainerOperationExecutor.class)
        .observer(PluginContainerRemedyExecutor.class)
        .observer(PostPrepareEnricherExecutor.class)
        .observer(DataCleanupExecutor.class);
        
        builder.service(DeployableContainer.class, RhqAgentPluginContainer.class)
        .service(ResourceProvider.class, PluginContainerProvider.class)
        .service(ResourceProvider.class, PluginContainerConfigurationProvider.class)
        .service(ResourceProvider.class, ServerServicesProvider.class)
        .service(PostPrepareEnricher.class, DiscoveryResultsTestEnricher.class)
        .service(PluginContainerOperation.class, RunDiscoveryExecutor.class)
        .service(PluginContainerPreparator.class, BeforeDiscoveryPreparator.class)
        .service(PluginContainerPreparator.class, ServerServicesPreparator.class)
        .service(PluginContainerOperationRemedy.class, AfterDiscoveryRemedy.class);
    }
}
