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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.PlatformSyncInfo;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.test.LargeGroupTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class LargeGroupPluginConfigurationTest extends LargeGroupTestBase {

    private LargeGroupEnvironment env;
    private CountDownLatch latch;

    @Override
    protected void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer) {
        TestServices testServices = new TestServices();
        agentServiceContainer.discoveryService = testServices;
    }

    /**
     * Create a large group of 1000+ resources.
     */
    @Override
    protected void beforeMethod() throws Exception {
        super.beforeMethod();

        env = createLargeGroupWithNormalUserRoleAccess(1010, Permission.MODIFY_RESOURCE);
        SessionTestHelper.simulateLogin(env.normalSubject);
    }

    /**
     * Remove the group and all its members.
     */
    @Override
    protected void afterMethod() throws Exception {
        tearDownLargeGroupWithNormalUserRoleAccess(env);
        SessionTestHelper.simulateLogout(env.normalSubject);

        super.afterMethod();
    }

    public void testGroupPluginConfigurationUpdate() throws Exception {
        // all of our platform's children are members of the group we are testing with
        int memberCount = env.platformResource.getChildResources().size();

        // we don't have any updates yet, but when we created the resources, they had a default plugin config already set.
        // let's see that we can obtain those plugin configs for all resources in the group
        Map<Integer, Configuration> existingMap;
        System.out.println("Getting plugin config for group [#members=" + memberCount + "]");
        long start = System.currentTimeMillis();
        existingMap = configurationManager.getPluginConfigurationsForCompatibleGroup(env.normalSubject,
            env.compatibleGroup.getId());
        long duration = System.currentTimeMillis() - start;
        System.out.println("Took [" + duration + "]ms to get plugin config for group [#members=" + memberCount + "]");

        assert existingMap.size() == memberCount : "did not get back the amount of configs [" + existingMap.size()
            + "] we expected [" + memberCount + "]";

        for (Map.Entry<Integer, Configuration> entry : existingMap.entrySet()) {
            Integer resourceId = entry.getKey();
            Configuration config = entry.getValue();
            assert PC_PROP1_VALUE.equals(config.getSimpleValue(PC_PROP1_NAME, null)) : "bad plugin config for "
                + resourceId + "=" + config.toString(true);
            assert resourceId.toString().equals(config.getSimpleValue(PC_PROP2_NAME, null)) : "bad plugin config for "
                + resourceId + "=" + config.toString(true);

            // create new plugin configuration settings for this resource, we will update the group members to these new configs
            config.getSimple(PC_PROP2_NAME).setStringValue("UPDATE" + resourceId);
        }

        // update our group with the new plugin configs
        System.out.println("Scheduling a group plugin update.");
        latch = new CountDownLatch(memberCount); // our TestService will count this down
        int groupUpdateId = configurationManager.scheduleGroupPluginConfigurationUpdate(env.normalSubject,
            env.compatibleGroup.getId(), existingMap);

        // group plugin configuration update has been kicked off, wait for the mock agents to each complete their update
        System.out.print("Waiting for mock agents");
        assert latch.await(5, TimeUnit.MINUTES) : "agents should not have taken this long";
        System.out.println(" Mock agents are done.");

        // wait for group update to get out of the INPROGRESS state (it will eventually get to SUCCESS), but don't wait indefinitely
        // this shouldn't take too long - just have to wait for the all the DB commits to complete
        System.out.print("Waiting for group plugin config update to be finished...");
        ConfigurationUpdateStatus status = getGroupPluginConfigurationStatus(env.compatibleGroup.getId());
        boolean inprogress = (status == ConfigurationUpdateStatus.INPROGRESS);
        for (int i = 0; i < 20 && inprogress; i++) {
            Thread.sleep(3000);
            status = getGroupPluginConfigurationStatus(env.compatibleGroup.getId());
            inprogress = (status == ConfigurationUpdateStatus.INPROGRESS);
        }
        assert !inprogress : "group plugin configuration update is still inprogress - this is taking too long";
        System.out.println(" Done. Status=" + ((status != null) ? status.name() : "????null????"));
        assert status == ConfigurationUpdateStatus.SUCCESS : "should have finished in the success status";

        // now see if our group plugin configuration update affected all of our resources by getting the group plugin config
        System.out.println("Getting updated plugin config for group [#members=" + memberCount + "]");
        start = System.currentTimeMillis();
        existingMap = configurationManager.getPluginConfigurationsForCompatibleGroup(env.normalSubject,
            env.compatibleGroup.getId());
        duration = System.currentTimeMillis() - start;
        System.out.println("Took [" + duration + "]ms to get updated plugin config for group [#members=" + memberCount
            + "]");

        assert existingMap.size() == memberCount : "did not get back the amount of configs [" + existingMap.size()
            + "] we expected [" + memberCount + "]";

        for (Map.Entry<Integer, Configuration> entry : existingMap.entrySet()) {
            Integer resourceId = entry.getKey();
            Configuration config = entry.getValue();
            assert PC_PROP1_VALUE.equals(config.getSimpleValue(PC_PROP1_NAME, null)) : "bad plugin config for "
                + resourceId + "=" + config.toString(true);
            assert ("UPDATE" + resourceId.toString()).equals(config.getSimpleValue(PC_PROP2_NAME, null)) : "bad plugin config for "
                + resourceId + "=" + config.toString(true);
        }

        // now test our authz - unauthorized user should bomb out
        System.out.println("Attempting unauthz access");
        SessionTestHelper.simulateLogin(env.unauthzSubject);
        start = System.currentTimeMillis();
        try {
            configurationManager.getPluginConfigurationsForCompatibleGroup(env.unauthzSubject,
                env.compatibleGroup.getId());
            assert false : "should have failed - unauthz user not allowed to get plugin configuration";
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
     * update plugin configurations.
     */
    private class TestServices implements DiscoveryAgentService {

        @Override
        public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
            throws InvalidPluginConfigurationClientException, PluginContainerException {
            latch.countDown();
            long latchCount = latch.getCount();
            System.out.print(((latchCount % 100) != 0) ? "." : String.valueOf(latchCount));
        }

        @Override
        public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
            return null;
        }

        @Override
        public AvailabilityReport getCurrentAvailability(Resource resource, boolean changesOnly) {
            return null;
        }

        @NotNull
        @Override
        public InventoryReport executeServerScanImmediately() throws PluginContainerException {
            return null;
        }

        @NotNull
        @Override
        public InventoryReport executeServiceScanImmediately() throws PluginContainerException {
            return null;
        }

        @Override
        public boolean executeServiceScanDeferred() {
            return true;
        }

        @Override
        public void executeServiceScanDeferred(int resourceId) {
        }

        @Override
        public Resource getPlatform() {
            return null;
        }

        @Override
        public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
            Configuration pluginConfiguration, int creatorSubjectId) throws InvalidPluginConfigurationClientException,
            PluginContainerException {
            return null;
        }

        @Override
        public void uninventoryResource(int resourceId) {
        }

        @Override
        public void disableServiceScans(int serverResourceId) {
        }

        @Override
        public void enableServiceScans(int serverResourceId, Configuration config) {
        }

        @Override
        public void requestFullAvailabilityReport() {
            return;
        }

        @Override
        public void synchronizePlatform(PlatformSyncInfo syncInfo) {
        }

        @Override
        public void synchronizeServer(int resourceId, Collection<ResourceSyncInfo> toplevelServerSyncInfo) {
        }

    }
}
