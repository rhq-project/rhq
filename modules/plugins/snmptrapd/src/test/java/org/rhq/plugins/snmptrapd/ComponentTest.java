/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.snmptrapd;

import java.io.File;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.event.EventManager;
import org.rhq.core.pc.inventory.InventoryContextImpl;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.upgrade.plugins.multi.base.NothingDiscoveringDiscoveryComponent;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;

/**
 * Base class for RHQ Component Testing.
 * @author elias
 */
public abstract class ComponentTest {

    protected final Log log = LogFactory.getLog(getClass());
    private static File temp = new File(System.getProperty("java.io.tmpdir"));

    protected ResourceComponent component;

    /**
     * Constructs a new component test.
     */
    protected ComponentTest(ResourceComponent component) {
        this.component = component;
    }

    protected EventManager eventManager;
    protected Configuration configuration;

    @BeforeTest
    protected void before() throws Exception {

        InputStream is = getClass().getResourceAsStream("/META-INF/rhq-plugin.xml");
        PluginDescriptor pd = AgentPluginDescriptorUtil.parsePluginDescriptor(is);
        PluginMetadataManager pmm = new PluginMetadataManager();
        pmm.addTestPlatformType();
        Set<ResourceType> rts = pmm.loadPlugin(pd);
        ResourceType resourceType = rts.iterator().next();
        configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();

        setConfiguration();

        // Speed up propagation of events by adjusting delay/period to 1 second
        PluginContainerConfiguration pcc = new PluginContainerConfiguration();
        pcc.setEventSenderInitialDelay(1);
        pcc.setEventSenderPeriod(1);

        PluginContainer pluginContainer = PluginContainer.getInstance();
        pluginContainer.setConfiguration(pcc);
        pluginContainer.initialize();

        Resource resource = new Resource();
        resource.setResourceType(resourceType);
        resource.setUuid(UUID.randomUUID().toString());

        resource.setPluginConfiguration(configuration);
        ResourceComponent parentResourceComponent = null;
        ResourceContext<?> parentResourceContext = null;
        ResourceDiscoveryComponent resourceDiscoveryComponent = new NothingDiscoveringDiscoveryComponent();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        File temporaryDirectory = temp;
        File dataDirectory = temp;
        String pluginContainerName = "rhq";
        EventContext eventContext = new EventContextImpl(resource, pluginContainer);
        OperationContext operationContext = new OperationContextImpl(0, pluginContainer);
        ContentContext contentContext = new ContentContextImpl(0, pluginContainer);
        PluginContainerDeployment pluginContainerDeployment = null;
        AvailabilityContext availContext = new AvailabilityContextImpl(resource, pluginContainer.getInventoryManager());
        InventoryContext inventoryContext = new InventoryContextImpl(resource, pluginContainer);
        ResourceContext context = new ResourceContext(resource, parentResourceComponent, parentResourceContext,
            resourceDiscoveryComponent, systemInfo, temporaryDirectory, dataDirectory, pluginContainerName,
            eventContext, operationContext, contentContext, availContext, inventoryContext, pluginContainerDeployment);
        Assert.assertNotNull(context.getEventContext());
        component.start(context);
    }

    /**
     * Called before the configuration is processed; override to set specific plugin parameters.
     * @see #configuration
     */
    protected void setConfiguration() {
    }

    @AfterTest
    public void stop() {
        component.stop();
        PluginContainer.getInstance().shutdown();
    }

}
