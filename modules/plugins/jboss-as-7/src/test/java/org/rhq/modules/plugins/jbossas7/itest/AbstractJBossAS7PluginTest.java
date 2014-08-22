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

package org.rhq.modules.plugins.jbossas7.itest;

import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_TYPE;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.MANAGEMENT_PASSWORD;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.MANAGEMENT_USERNAME;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.plugin.testutil.AbstractAgentPluginTest;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.test.arquillian.AfterDiscovery;

/**
 * The base class for all as7 plugin integration tests.
 *
 * @author Ian Springer
 */
public abstract class AbstractJBossAS7PluginTest extends AbstractAgentPluginTest {

    /**
     * Every test sub-class requires a management user, create it up front
     */
    @AfterDiscovery
    public void installManagementUsersTest() throws Exception {
        System.out.println("== Installing management users...");

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();

        Resource domainServer = getResourceByTypeAndKey(platform, DOMAIN_RESOURCE_TYPE, DOMAIN_RESOURCE_KEY, true);
        installManagementUser(domainServer);

        Resource standaloneServer = getResourceByTypeAndKey(platform, STANDALONE_RESOURCE_TYPE,
            STANDALONE_RESOURCE_KEY, true);
        installManagementUser(standaloneServer);
    }

    protected Resource validatePlatform() throws Exception {

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        int waits = 0;
        while (null == platform && waits++ < 15) {
            try {
                System.out.println("\n== Waiting for Platform to be discovered...");
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                //
            }
            platform = this.pluginContainer.getInventoryManager().getPlatform();
        }
        if (null == platform) {
            throw new RuntimeException("Platform not discovered in 15 seconds. Something seems wrong.");
        }

        return platform;
    }

    protected void validateDiscovery() throws Exception {
        validateDiscovery(false);
    }

    protected void validateDiscovery(boolean serverOnly) throws Exception {

        System.out.println("\n== Waiting for Server discovery to stabilize (using 35 second quiet time)...");

        Resource platform = validatePlatform();

        if (serverOnly) {
            waitForResourceByTypeAndKey(platform, platform, STANDALONE_RESOURCE_TYPE, STANDALONE_RESOURCE_KEY);
            waitForResourceByTypeAndKey(platform, platform, DOMAIN_RESOURCE_TYPE, DOMAIN_RESOURCE_KEY);
        } else {
            waitForAsyncDiscoveryToStabilize(platform);
        }
    }

    private void installManagementUser(Resource resource) throws PluginContainerException {
        System.out.println("==== Installing management user [" + MANAGEMENT_USERNAME + "] for " + resource + "...");

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
}
