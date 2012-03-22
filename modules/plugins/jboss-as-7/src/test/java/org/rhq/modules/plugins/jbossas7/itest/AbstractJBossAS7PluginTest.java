/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.itest;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.plugin.testutil.AbstractAgentPluginTest;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.itest.domain.DomainServerComponentTest;
import org.rhq.modules.plugins.jbossas7.itest.standalone.StandaloneServerComponentTest;
import org.rhq.test.arquillian.AfterDiscovery;

/**
 * The base class for all jboss-as-7 plugin integration tests.
 *
 * @author Ian Springer
 */
public abstract class AbstractJBossAS7PluginTest extends AbstractAgentPluginTest {

    protected static final String PLUGIN_NAME = "jboss-as-7";

    public static final String MANAGEMENT_USERNAME = "test";
    public static final String MANAGEMENT_PASSWORD = "test";

    private static boolean createdManagementUsers;

    /**
     * Make sure a management user is configured on both discovered AS7 test servers.     
     */
    @AfterDiscovery
    public void installManagementUsers() throws PluginContainerException {
        if (!createdManagementUsers) {
            Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
            Resource domainServer = ResourceUtility.getChildResource(platform,
                    DomainServerComponentTest.RESOURCE_TYPE, DomainServerComponentTest.RESOURCE_KEY);
            if (domainServer != null) {
                installManagementUser(domainServer);
            }
            Resource standaloneServer = ResourceUtility.getChildResource(platform,
                    StandaloneServerComponentTest.RESOURCE_TYPE, StandaloneServerComponentTest.RESOURCE_KEY);
            if (standaloneServer != null) {
                installManagementUser(standaloneServer);
            }
            // TODO (ips, 03/16/12): Uncomment this once I fix the issue with Resources that were previously discovered
            //                       and committed getting discovered with a status of NEW.
            //createdManagementUsers = true;
        }
    }

    private void installManagementUser(Resource resource) throws PluginContainerException {
        System.out.println(">>> Installing management user [" + MANAGEMENT_USERNAME + "] for " + resource + "...");

        // Invoke the "installRhqUser" operation on the ResourceComponent - this will update the mgmt-users.properties
        // file in the AS7 server's configuration directory.
        Configuration params = new Configuration();
        params.put(new PropertySimple("user", MANAGEMENT_USERNAME));
        params.put(new PropertySimple("password", MANAGEMENT_PASSWORD));

        String operationName = "installRhqUser";
        OperationResult result = invokeOperation(resource, operationName, params);
        System.out.println("Installed management user [" + MANAGEMENT_USERNAME + "] for " + resource + ".");
        assertOperationSucceeded(operationName, params, result);

        // Update the username and password in the *Server-side* plugin config. This simulates the end user updating the
        // plugin config via the GUI.
        Resource resourceFromServer = getServerInventory().getResourceStore().get(resource.getUuid());
        Configuration pluginConfig = resourceFromServer.getPluginConfiguration();
        pluginConfig.getSimple("user").setStringValue(MANAGEMENT_USERNAME);
        pluginConfig.getSimple("password").setStringValue(MANAGEMENT_PASSWORD);
        
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
