/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.configuration;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * ConfigurationManager Tester.
 *
 * @author  mspritzler
 *
 * @since   1.0
 * @created June 14, 2007
 */
public class ConfigurationManagerTest {
    private ConfigurationManager manager;

    @BeforeMethod
    public void setUp() {
        // create a plugin container config that contains our mock server service so the manager uses it
        ServerServices serverServices = new ServerServices();
        serverServices.setConfigurationServerService(new MockConfigurationServerService());
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
        pcConfig.setServerServices(serverServices);

        // create our mock manager and initialize it
        manager = new MockConfigurationManager();
        manager.setConfiguration(pcConfig);
        manager.initialize();
    }

    @AfterMethod
    public void tearDown() {
        manager.shutdown();
    }

    @Test
    public void testUpdateResourceConfiguration() {
        //TODO: Test goes here...
    }

    @Test
    public void testExecuteUpdateResourceConfigurationImmediatelySuccess() {
        Configuration configuration = new Configuration();
        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, configuration, 1);
        try {
            ConfigurationUpdateResponse response = manager.executeUpdateResourceConfigurationImmediately(request);
            assert response.getStatus().equals(ConfigurationUpdateStatus.SUCCESS);
        } catch (PluginContainerException e) {
            e.printStackTrace();
            assert false : "PluginContainerException has been caught: " + e;
        }
    }

    @Test
    public void testExecuteUpdateResourceConfigurationImmediatelyFailure() {
        Configuration configuration = new Configuration();
        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(2, configuration, 1);
        try {
            ConfigurationUpdateResponse response = manager.executeUpdateResourceConfigurationImmediately(request);
            assert response.getStatus().equals(ConfigurationUpdateStatus.FAILURE);
        } catch (PluginContainerException e) {
            e.printStackTrace();
            assert false : "PluginContainerException has been caught: " + e;
        }
    }

    //@Test
    public void testLoadResourceConfiguration() {
        //TODO: Test goes here...
    }

    private class MockConfigurationManager extends ConfigurationManager {
        @Override
        protected ConfigurationFacet getConfigurationFacet(int resourceId, FacetLockType lockType) {
            return new MockConfigurationFacet();
        }

        @Override
        protected ResourceType getResourceType(int resourceId) {
            return new ResourceType();
        }
    }

    private class MockConfigurationServerService implements ConfigurationServerService {
        public void completedConfigurationUpdate(ConfigurationUpdateResponse response) {
            if (response.getConfigurationUpdateId() == 1) {
                response.setStatus(ConfigurationUpdateStatus.SUCCESS);
            } else {
                response.setStatus(ConfigurationUpdateStatus.FAILURE);
            }
        }
    }

    private class MockConfigurationFacet implements ConfigurationFacet {
        public Configuration loadResourceConfiguration() {
            return null;
        }

        public void updateResourceConfiguration(ConfigurationUpdateReport report) {
            Configuration config = report.getConfiguration();
        }
    }
}