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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceFilter;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.domain.util.TypeAndKeyResourceFilter;
import org.rhq.core.pc.inventory.InventoryManager;
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

    private static final Log log = LogFactory.getLog(AbstractJBossAS7PluginTest.class);

    protected static final String PLUGIN_NAME = "JBossAS7";

    public static final String MANAGEMENT_USERNAME = "admin";
    public static final String MANAGEMENT_PASSWORD = "admin";

    private static final int TYPE_HIERARCHY_DEPTH = 6;

    private static int OVERRIDE_TYPE_HIERARCHY_DEPTH = -1;

    protected static int getMaxDiscoveryDepthOverride() {
        return OVERRIDE_TYPE_HIERARCHY_DEPTH;
    }

    protected static void setMaxDiscoveryDepthOverride(int overrideDepth) {
        OVERRIDE_TYPE_HIERARCHY_DEPTH = overrideDepth;
    }

    private static boolean createdManagementUsers;

    /**
     * Make sure a management user is configured on both discovered AS7 test servers.
     */
    @AfterDiscovery
    protected void installManagementUsers() throws PluginContainerException, Exception {
        waitForAsyncDiscoveries();

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
        System.out
            .println("========= Installing management user [" + MANAGEMENT_USERNAME + "] for " + resource + "...");

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

    /**
     * Convenience, same as calling {{waitForResourceByTypeAndKey(platform, parent, type, key, 600)}}.
     * @see #waitForResourceByTypeAndKey(Resource, Resource, ResourceType, String, int)
     */
    protected static Resource waitForResourceByTypeAndKey(Resource platform, Resource parent, final ResourceType type,
        String key) {
        return waitForResourceByTypeAndKey(platform, parent, type, key, 600);
    }

    /**
     * A method that looks for a child resource, given the parent, and will wait for the resource to
     * show up (and be committed) under the parent if it isn't there already - on the assumption that
     * discovery is still taking place.  If the resource still does not show up after the timeout period, an assertion
     * failure is generated.
     *
     * @param parent
     * @param type
     * @param key
     * @param timeoutInSeconds minimum timeout is 10s.
     * @return
     */
    protected static Resource waitForResourceByTypeAndKey(Resource platform, Resource parent, final ResourceType type,
        String key, int timeoutInSeconds) {
        int maxTries = (timeoutInSeconds / 10) + 1;
        Resource result = getResourceByTypeAndKey(parent, type, key);

        // Discovery is not enough, it needs to be imported for test success (and started??)
        for (int numTries = 1; ((null == result || 0 == result.getId()) && numTries <= maxTries); ++numTries) {
            try {
                log.info("Waiting 10s for resource [" + key + "] to be discovered. Current Tree Size ["
                    + getResCount(platform) + "] Number of tries remaining [" + (maxTries - numTries) + "]");
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                // just keep trying
            }
            result = getResourceByTypeAndKey(parent, type, key);
        }

        assertNotNull(
            result,
            type.getName() + " Resource with key [" + key + "] not found in inventory - child "
                + type.getCategory().getDisplayName() + "s that were discovered: "
                + ResourceUtility.getChildResources(parent, new ResourceFilter() {
                    public boolean accept(Resource resource) {
                        return (resource.getResourceType().getCategory() == type.getCategory());
                    }
                }));

        return result;
    }

    private static Resource getResourceByTypeAndKey(Resource parent, final ResourceType type, String key) {
        Set<Resource> childResources = ResourceUtility.getChildResources(parent,
            new TypeAndKeyResourceFilter(type, key));
        if (childResources.size() > 1) {
            throw new IllegalStateException(parent + " has more than one child Resource with same type (" + type
                + ") and key (" + key + ").");
        }
        Resource serverResource = (childResources.isEmpty()) ? null : childResources.iterator().next();

        return serverResource;
    }

    // Not currently used.
    // TODO: If needed they may need to be modified to recursively start the ancestors first, because you can't
    //       start a resource whose parent is not started.
    //
    //    protected void restartResourceComponent(Resource resource) throws PluginContainerException {
    //        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
    //        inventoryManager.deactivateResource(resource);
    //        ResourceContainer serverContainer = inventoryManager.getResourceContainer(resource);
    //        inventoryManager.activateResource(resource, serverContainer, true);
    //    }
    //
    //    /**
    //     * Use to ensure a resourceComponent is started. After discovery it may take unacceptably long for
    //     * the resource to activate. If already active this call is a no-op.
    //     *
    //     * @param resource
    //     * @throws PluginContainerException
    //     */
    //    protected void startResourceComponent(Resource resource) throws PluginContainerException {
    //        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
    //        ResourceContainer serverContainer = inventoryManager.getResourceContainer(resource);
    //        inventoryManager.activateResource(resource, serverContainer, true);
    //    }

}
