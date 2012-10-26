/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.test.LargeGroupTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class LargeGroupResourceConfigurationTest extends LargeGroupTestBase {

    private LargeGroupEnvironment env;
    private CountDownLatch latch;

    @Override
    protected void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer) {
        TestServices testServices = new TestServices();
        agentServiceContainer.configurationService = testServices;
    }

    /**
     * Create a large group of 1000+ resources.
     */
    @BeforeMethod
    public void beforeMethod() throws Exception {
        env = createLargeGroupWithNormalUserRoleAccess(1010, Permission.CONFIGURE_READ, Permission.CONFIGURE_WRITE);
        SessionTestHelper.simulateLogin(env.normalSubject);
    }

    /**
     * Remove the group and all its members.
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        tearDownLargeGroupWithNormalUserRoleAccess(env);
        SessionTestHelper.simulateLogout(env.normalSubject);
    }

    public void testGroupResourceConfigurationUpdate() throws Exception {
        // all of our platform's children are members of the group we are testing with
        int memberCount = env.platformResource.getChildResources().size();

        // we don't have any resource config
        // let's see that we can obtain the current live configs for all resources in the group
        // our mock agent will return the live configs
        Map<Integer, Configuration> existingMap;
        System.out.println("Getting resource config for group [#members=" + memberCount + "]");
        long start = System.currentTimeMillis();
        existingMap = configurationManager.getResourceConfigurationsForCompatibleGroup(env.normalSubject,
            env.compatibleGroup.getId());
        long duration = System.currentTimeMillis() - start;
        System.out.println("Took [" + duration + "]ms to get resource config for group [#members=" + memberCount + "]");

        assert existingMap.size() == memberCount : "did not get back the amount of configs [" + existingMap.size()
            + "] we expected [" + memberCount + "]";

        for (Map.Entry<Integer, Configuration> entry : existingMap.entrySet()) {
            Integer resourceId = entry.getKey();
            Configuration config = entry.getValue();
            assert RC_PROP1_VALUE.equals(config.getSimpleValue(RC_PROP1_NAME, null)) : "bad resource config for "
                + resourceId + "=" + config.toString(true);
            assert resourceId.toString().equals(config.getSimpleValue(RC_PROP2_NAME, null)) : "bad resource config for "
                + resourceId + "=" + config.toString(true);

            // create new resource configuration settings for this resource, we will update the group members to these new configs
            config.getSimple(RC_PROP2_NAME).setStringValue("UPDATE" + resourceId);
        }

        // update our group with the new resource configs
        System.out.println("Scheduling a group resource config update.");
        latch = new CountDownLatch(memberCount); // our TestService will count this down
        int groupUpdateId = configurationManager.scheduleGroupResourceConfigurationUpdate(env.normalSubject,
            env.compatibleGroup.getId(), existingMap);

        // group resource configuration update has been kicked off, wait for the mock agents to each complete their update 
        System.out.print("Waiting for mock agents");
        assert latch.await(5, TimeUnit.MINUTES) : "agents should not have taken this long";
        System.out.println(" Mock agents are done.");

        // wait for group update to get out of the INPROGRESS state (it will eventually get to SUCCESS), but don't wait indefinitely
        // this shouldn't take too long - just have to wait for the all the DB commits to complete
        System.out.print("Waiting for group resource config update to be finished...");
        ConfigurationUpdateStatus status = getGroupResourceConfigurationStatus(env.compatibleGroup.getId());
        boolean inprogress = (status == ConfigurationUpdateStatus.INPROGRESS);
        for (int i = 0; i < 20 && inprogress; i++) {
            Thread.sleep(3000);
            status = getGroupResourceConfigurationStatus(env.compatibleGroup.getId());
            inprogress = (status == ConfigurationUpdateStatus.INPROGRESS);
        }
        assert !inprogress : "group resource configuration update is still inprogress - this is taking too long";
        System.out.println(" Done. Status=" + ((status != null) ? status.name() : "????null????"));
        assert status == ConfigurationUpdateStatus.SUCCESS : "should have finished in the success status";

        // now see if our group resource configuration update affected all of our resources by getting the group plugin config
        System.out.println("Getting updated resource config for group [#members=" + memberCount + "]");
        start = System.currentTimeMillis();
        existingMap = configurationManager.getResourceConfigurationsForCompatibleGroup(env.normalSubject,
            env.compatibleGroup.getId());
        duration = System.currentTimeMillis() - start;
        System.out.println("Took [" + duration + "]ms to get updated resource config for group [#members="
            + memberCount + "]");

        assert existingMap.size() == memberCount : "did not get back the amount of configs [" + existingMap.size()
            + "] we expected [" + memberCount + "]";

        for (Map.Entry<Integer, Configuration> entry : existingMap.entrySet()) {
            Integer resourceId = entry.getKey();
            Configuration config = entry.getValue();
            assert RC_PROP1_VALUE.equals(config.getSimpleValue(RC_PROP1_NAME, null)) : "bad resource config for "
                + resourceId + "=" + config.toString(true);
            assert ("UPDATE" + resourceId.toString()).equals(config.getSimpleValue(RC_PROP2_NAME, null)) : "bad resource config for "
                + resourceId + "=" + config.toString(true);
        }

        // now test our authz - unauthorized user should bomb out
        System.out.println("Attempting unauthz access");
        SessionTestHelper.simulateLogin(env.unauthzSubject);
        start = System.currentTimeMillis();
        try {
            configurationManager.getResourceConfigurationsForCompatibleGroup(env.unauthzSubject,
                env.compatibleGroup.getId());
            assert false : "should have failed - unauthz user not allowed to get resource configuration";
        } catch (PermissionException expected) {
        } finally {
            duration = System.currentTimeMillis() - start;
            SessionTestHelper.simulateLogout(env.unauthzSubject);
        }
        System.out.println("Took [" + duration + "]ms to attempt unauthz access");
        return;
    }

    /**
     * This class mocks out the remote agent. It will receive calls to, for example,
     * update configurations.
     */
    private class TestServices implements ConfigurationAgentService {

        private HashMap<Integer, Configuration> configs = new HashMap<Integer, Configuration>();

        @Override
        public void updateResourceConfiguration(ConfigurationUpdateRequest request) {
            try {
                synchronized (configs) {
                    configs.put(request.getResourceId(), request.getConfiguration());
                }
                ConfigurationUpdateResponse response = new ConfigurationUpdateResponse(
                    request.getConfigurationUpdateId(), request.getConfiguration(), ConfigurationUpdateStatus.SUCCESS,
                    null);
                // simulate agent calling into the server to complete the update
                ConfigurationServerService server = new ConfigurationServerServiceImpl();
                server.completeConfigurationUpdate(response);
            } finally {
                latch.countDown();
            }
            return;
        }

        @Override
        public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
            synchronized (configs) {
                Integer id = Integer.valueOf(resourceId);
                if (configs.containsKey(id)) {
                    return configs.get(id);
                } else {
                    Configuration config = new Configuration();
                    config.put(new PropertySimple(RC_PROP1_NAME, RC_PROP1_VALUE));
                    config.put(new PropertySimple(RC_PROP2_NAME, resourceId));
                    configs.put(id, config);
                    return config;
                }
            }
        }

        @Override
        public ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(
            ConfigurationUpdateRequest request) throws PluginContainerException {
            return null;
        }

        @Override
        public Configuration merge(Configuration configuration, int resourceId, boolean fromStructured)
            throws PluginContainerException {
            return null;
        }

        @Override
        public Configuration validate(Configuration configuration, int resourceId, boolean isStructured)
            throws PluginContainerException {
            return null;
        }
    }
}
