/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest;

import static org.testng.Assert.assertNotNull;

import java.util.Set;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceFilter;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.domain.util.TypeAndKeyResourceFilter;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.plugin.testutil.AbstractAgentPluginTest;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.itest.domain.DomainServerComponentTest;
import org.rhq.modules.plugins.jbossas7.itest.standalone.StandaloneServerComponentTest;
import org.rhq.test.arquillian.AfterDiscovery;

/**
 * The base class for all as7 plugin integration tests.
 *
 * @author Ian Springer
 */
public abstract class AbstractJBossAS7PluginTest extends AbstractAgentPluginTest {

    protected static final String PLUGIN_NAME = "JBossAS7";

    public static final String MANAGEMENT_USERNAME = "admin";
    public static final String MANAGEMENT_PASSWORD = "admin";

    private static final int TYPE_HIERARCHY_DEPTH = 6;

    private static int OVERRIDE_TYPE_HIERARCHY_DEPTH = -1;

    public static int getMaxDiscoveryDepthOverride() {
        return OVERRIDE_TYPE_HIERARCHY_DEPTH;
    }

    public static void setMaxDiscoveryDepthOverride(int overrideDepth) {
        OVERRIDE_TYPE_HIERARCHY_DEPTH = overrideDepth;
    }

    private static boolean createdManagementUsers;

    /**
     * Make sure a management user is configured on both discovered AS7 test servers.     
     */
    @AfterDiscovery
    public void installManagementUsers() throws PluginContainerException {
        System.out.println("\n=== Discovery scan completed.");
        if (!createdManagementUsers) {
            System.out.println("====== Installing management users...");

            Resource platform = this.pluginContainer.getInventoryManager().getPlatform();

            Resource domainServer = getResourceByTypeAndKey(platform, DomainServerComponentTest.RESOURCE_TYPE,
                    DomainServerComponentTest.RESOURCE_KEY);
            installManagementUser(domainServer);

            Resource standaloneServer = getResourceByTypeAndKey(platform, StandaloneServerComponentTest.RESOURCE_TYPE,
                    StandaloneServerComponentTest.RESOURCE_KEY);
            installManagementUser(standaloneServer);

            // TODO (ips, 03/16/12): Uncomment this once I fix the issue with Resources that were previously discovered
            //                       and committed getting discovered with a status of NEW.
            //createdManagementUsers = true;
        }
    }

    private void installManagementUser(Resource resource) throws PluginContainerException {
        System.out.println("========= Installing management user [" + MANAGEMENT_USERNAME + "] for " + resource + "...");

        // Invoke the "installRhqUser" operation on the ResourceComponent - this will update the mgmt-users.properties
        // file in the AS7 server's configuration directory.
        Configuration params = new Configuration();
        params.setSimpleValue("user", MANAGEMENT_USERNAME);
        params.setSimpleValue("password", MANAGEMENT_PASSWORD);

        String operationName = "installRhqUser";
        OperationResult result = invokeOperation(resource, operationName, params);
        System.out.println("Installed management user [" + MANAGEMENT_USERNAME + "] for " + resource + ".");
        assertOperationSucceeded(operationName, params, result);

        // Update the username and password in the *Server-side* plugin config. This simulates the end user updating the
        // plugin config via the GUI.
        Resource resourceFromServer = getServerInventory().getResourceStore().get(resource.getUuid());
        Configuration pluginConfig = resourceFromServer.getPluginConfiguration();
        pluginConfig.setSimpleValue("user", MANAGEMENT_USERNAME);
        pluginConfig.setSimpleValue("password", MANAGEMENT_PASSWORD);
        
        // Restart the ResourceComponent, so it will start using the new plugin config.
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        try {
            inventoryManager.updatePluginConfiguration(resource.getId(), pluginConfig);
        } catch (InvalidPluginConfigurationClientException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected int getTypeHierarchyDepth() {
        if (OVERRIDE_TYPE_HIERARCHY_DEPTH != -1) {
            return OVERRIDE_TYPE_HIERARCHY_DEPTH;
        }
        return TYPE_HIERARCHY_DEPTH;
    }

    protected static Resource getResourceByTypeAndKey(Resource parent, final ResourceType type, String key) {
        Set<Resource> childResources = ResourceUtility.getChildResources(parent,
                new TypeAndKeyResourceFilter(type, key));
        if (childResources.size() > 1) {
            throw new IllegalStateException(parent + " has more than one child Resource with same type ("
                    + type + ") and key (" + key + ").");
        }
        Resource serverResource = (childResources.isEmpty()) ? null : childResources.iterator().next();

        assertNotNull(serverResource,
                type.getName() + " Resource with key [" + key + "] not found in inventory - child "
                        + type.getCategory().getDisplayName() + "s that were discovered: "
                + ResourceUtility.getChildResources(parent,
                new ResourceFilter() {
                    public boolean accept(Resource resource) {
                        return (resource.getResourceType().getCategory() == type.getCategory());
                    }
                }));
        return serverResource;
    }

    private void restartResourceComponent(Resource resource) throws PluginContainerException {
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.deactivateResource(resource);
        ResourceContainer serverContainer = inventoryManager.getResourceContainer(resource);
        inventoryManager.activateResource(resource, serverContainer, true);
    }

}
