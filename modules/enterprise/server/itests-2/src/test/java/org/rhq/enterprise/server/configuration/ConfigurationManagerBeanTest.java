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
package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.PlatformSyncInfo;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.BadArgumentException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Tests the configuration manager.
 */
@Test
@SuppressWarnings("all")
public class ConfigurationManagerBeanTest extends AbstractEJB3Test {
    private static final boolean ENABLE_TESTS = true;

    private ConfigurationManagerLocal configurationManager;
    private ResourceManagerLocal resourceManager;
    private Resource newResource1;
    private Resource newResource2;
    private ResourceGroup compatibleGroup;
    private PageControl configUpdatesPageControl;
    private Agent agent;
    private Subject overlord;

    @Override
    protected void beforeMethod() throws Exception {
        // Make sure page control sorts so the latest config update is last (the default is for the latest to be first).
        configUpdatesPageControl = PageControl.getUnlimitedInstance();
        // (ips, 04/01/10): Use createdTime, rather than id, to order by, since the id's are not guaranteed to be
        //                  ordered sequentially (this is because, dbsetup configures our sequences to pre-create
        //                  and cache sequence values 10 at a time.
        configUpdatesPageControl.addDefaultOrderingField("cu.createdTime", PageOrdering.ASC);

        configurationManager = LookupUtil.getConfigurationManager();
        resourceManager = LookupUtil.getResourceManager();

        prepareScheduler();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        TestServices testServices = new TestServices();
        agentServiceContainer.configurationService = testServices;
        agentServiceContainer.discoveryService = testServices;

        overlord = LookupUtil.getSubjectManager().getOverlord();

        getTransactionManager().begin();

        try {
            compatibleGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, null, // no role necessary here
                "compat");

            newResource1 = SessionTestHelper.createNewResourceForGroup(em, compatibleGroup,
                "res" + System.currentTimeMillis());
            newResource2 = SessionTestHelper.createNewResourceForGroup(em, compatibleGroup,
                "res" + System.currentTimeMillis());

            // set one resource as the child of another, so that they don't both look like platforms under the agent
            newResource1.addChildResource(newResource2);

            agent = SessionTestHelper.createNewAgent(em, "agent-" + getClass().getSimpleName());
            newResource1.setAgent(agent);
            newResource2.setAgent(agent);

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println(e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }

            throw e;
        }
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            // perform in-band and out-of-band work in quick succession
            // only need to delete newResource1, which will delete his child newResource2 as well as the agent
            List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, newResource1.getId());
            for (Integer deletedResourceId : deletedIds) {
                resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
            }
        } catch (Exception e) {
            System.out.println(e);
            throw e;
        }

        try {
            getTransactionManager().begin();

            try {
                ResourceGroup group = em.find(ResourceGroup.class, compatibleGroup.getId());
                em.remove(group);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            }

            getTransactionManager().begin();

            try {
                ResourceType type = em.find(ResourceType.class, newResource1.getResourceType().getId());

                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            }

        } finally {
            unprepareScheduler();
            unprepareForTestAgents();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testLatestConfiguration() throws Exception {
        int resourceId = newResource1.getId();

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a config, then another that spends some time in INPROGRESS before succeeding
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "false"));
        configuration2.put(new PropertySimple("mysleep", "7000"));

        configurationManager.updateResourceConfiguration(overlord, resourceId, configuration1);
        Thread.sleep(2000); // wait for the test agent to complete the request

        ResourceConfigurationUpdate history1;
        history1 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
        assert history1 != null;
        PropertySimple myprop = history1.getConfiguration().getSimple("myboolean");
        assert myprop != null;
        assert "true".equals(myprop.getStringValue());

        // now update to that second config - the "agent" will sleep for a bit before it completes
        // so we will have an INPROGRESS configuration for a few seconds before it goes to SUCCESS
        ResourceConfigurationUpdate history2 = null;
        boolean inProgress = false;
        boolean inProgressTested = false;

        configurationManager.updateResourceConfiguration(overlord, resourceId, configuration2);

        do {
            history2 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
            inProgress = configurationManager.isResourceConfigurationUpdateInProgress(overlord, resourceId);

            if (inProgress) {
                // history2 should be history1 since the update is not complete
                assert history2 != null;
                assert history2.getId() == history1.getId();
                myprop = history2.getConfiguration().getSimple("myboolean");
                assert myprop != null;
                assert "true".equals(myprop.getStringValue());
                myprop = history2.getConfiguration().getSimple("mysleep"); // this wasn't in the first config
                assert myprop == null;
                // record that this test case ran, we expect it will if the agent delay is there
                inProgressTested = true;
            } else {
                // update is complete, history 2 should be different
                history2 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
                assert history2 != null;
                assert history2.getId() != history1.getId();
                myprop = history2.getConfiguration().getSimple("myboolean");
                assert myprop != null;
                assert "false".equals(myprop.getStringValue());
                myprop = history2.getConfiguration().getSimple("mysleep");
                assert myprop.getLongValue() != null;
                assert myprop.getLongValue().longValue() == 7000L;
            }
        } while (inProgress);

        assertTrue(inProgressTested);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInProgressConfiguration() throws Exception {
        int resourceId = newResource1.getId();

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create 3 configs: config1 will be stored as current. config2 will be in progress, config3 should get
        // blocked from updating by the inprogress update
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "false"));
        configuration2.put(new PropertySimple("mysleep", "7000"));

        Configuration configuration3 = new Configuration();
        configuration3.put(new PropertySimple("mysleep", "10000"));

        // make config1 the current
        configurationManager.updateResourceConfiguration(overlord, resourceId, configuration1);
        Thread.sleep(2000); // wait for the test agent to complete the request

        ResourceConfigurationUpdate history1;
        history1 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
        assert history1 != null;
        PropertySimple myprop = history1.getConfiguration().getSimple("myboolean");
        assert myprop != null;
        assert "true".equals(myprop.getStringValue());

        // now update to config2 - the "agent" will sleep for a bit before it completes
        // so we will have an INPROGRESS configuration for a few seconds before it goes to SUCCESS
        configurationManager.updateResourceConfiguration(overlord, resourceId, configuration2);

        // now update to config3 - this should fail as you can't update while there is one in progress
        try {
            configurationManager.updateResourceConfiguration(overlord, resourceId, configuration3);
            assert false : "Should have thrown an in progress exception";

        } catch (ConfigurationUpdateStillInProgressException e) {
            System.out.println("======> " + e);

            // make sure everything works as expected (like the above test)

            boolean inProgress = false;
            boolean inProgressTested = false;

            do {
                ResourceConfigurationUpdate history2 = configurationManager.getLatestResourceConfigurationUpdate(
                    overlord, resourceId);
                inProgress = configurationManager.isResourceConfigurationUpdateInProgress(overlord, resourceId);

                if (inProgress) {
                    // history2 should be history1 since the update is not complete
                    assert history2 != null;
                    assert history2.getId() == history1.getId();
                    myprop = history2.getConfiguration().getSimple("myboolean");
                    assert myprop != null;
                    assert "true".equals(myprop.getStringValue());
                    myprop = history2.getConfiguration().getSimple("mysleep"); // this wasn't in the first config
                    assert myprop == null;
                    // record that this test case ran, we expect it will if the agent delay is there
                    inProgressTested = true;
                } else {
                    // update is complete, history 2 should be different
                    history2 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
                    assert history2 != null;
                    assert history2.getId() != history1.getId();
                    myprop = history2.getConfiguration().getSimple("myboolean");
                    assert myprop != null;
                    assert "false".equals(myprop.getStringValue());
                    myprop = history2.getConfiguration().getSimple("mysleep");
                    assert myprop.getLongValue() != null;
                    assert myprop.getLongValue().longValue() == 7000L;
                }
            } while (inProgress);

        } catch (Throwable t) {
            assert false : "Should have thrown an in progress exception, not: " + t;
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeleteType() throws Exception {
        // the purpose of this little test is to test an error condition I'm getting when attempting to delete
        // a resource type - just forces a run with before/afterMethod
        return;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInvalidPluginConfigurationUpdate() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("fakeReadOnly", "1"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("fakeReadOnly", "2"));

        Configuration current;

        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no plugin config settings yet

        configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration1);
        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("fakeReadOnly").getIntegerValue() == 1 : current;

        try {
            configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration2);
            fail("Should have thrown BadArgumentException when trying to change readOnly property.");
        } catch (BadArgumentException e) {
            // expected
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInvalidResourceConfigurationUpdate() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("fakeReadOnly", "1"));
        configuration1.put(new PropertySimple("myboolean", "false"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("fakeReadOnly", "2"));
        configuration2.put(new PropertySimple("myboolean", "false"));

        Configuration current = configurationManager.getResourceConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no res config settings yet

        ResourceConfigurationUpdate rcu = configurationManager.updateResourceConfiguration(overlord, resource.getId(),
            configuration1);
        for (int i = 0; configurationManager.isResourceConfigurationUpdateInProgress(overlord, newResource1.getId())
            && i < 5; ++i) {
            Thread.sleep(1000);
        }

        current = configurationManager.getResourceConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 2 : current;
        assert current.getSimple("fakeReadOnly").getIntegerValue() == 1 : current;

        try {
            configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
            fail("Should have thrown BadArgumentException when trying to change readOnly property.");
        } catch (BadArgumentException e) {
            // expected
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testPluginConfigurationUpdateCallbackSuccess() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("foo", "1"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("bar", "2"));

        Configuration current;

        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no plugin config settings yet

        configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration1);
        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("foo").getIntegerValue() == 1 : current;

        configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration2);
        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("bar").getIntegerValue() == 2 : current;
        assert current.getSimple("foo") == null : current; // this is gone now, we overrode it

        assert current.remove("foo") == null : current; // secondary test - just exercise the remove method
        assert current.remove("bar") != null : current; // again just test the remove method
        current.put(new PropertySimple("hello", "3"));
        configurationManager.updatePluginConfiguration(overlord, resource.getId(), current);
        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("hello").getIntegerValue() == 3 : current;
        assert current.getSimple("foo") == null : current; // this is gone now
        assert current.getSimple("bar") == null : current; // this is gone now
    }

    @Test(enabled = ENABLE_TESTS)
    public void testPluginConfigurationUpdateCallbackFailure() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("foo", "1"));
        configuration1.put(new PropertySimple("fail", "true"));

        Configuration current;

        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no plugin config settings yet

        PluginConfigurationUpdate update = configurationManager.updatePluginConfiguration(overlord, resource.getId(),
            configuration1);
        assert update.getErrorMessage() != null : "We should have simulated a failure, "
            + "but instead received no error.";

        // Even if a plugin container exception occurs, the plugin configuration should still be updated.
        current = configurationManager.getPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() != 0 : current.toString();
    }

    @Test(enabled = false)
    public void testGroupPluginConfigurationUpdateWorkflowSuccess() throws Exception {
        // TODO (joseph): Fix and then re-enable this test.
        //testGroupPluginConfigurationUpdateWorkflowHelper(false);
    }

    @Test(enabled = false)
    public void testGroupPluginConfigurationUpdateWorkflowFailure() throws Exception {
        // TODO (joseph): Fix and then re-enable this test.
        //testGroupPluginConfigurationUpdateWorkflowHelper(true);
    }

    @Test(enabled = false)
    public void testGroupPluginConfigurationUpdateMergeAlgorithmPerformance() throws Exception {
        Configuration configuration = new Configuration();
        configuration.put(new PropertySimple("foo1", "1"));
        configuration.put(new PropertySimple("foo2", "2"));
        configuration.put(new PropertySimple("foo3", "3"));
        configuration.put(new PropertySimple("foo4", "4"));
        configuration.put(new PropertySimple("foo5", "5"));

        /*
         * for a configuration object that has 5 simple properties, baseline for groupSize of 100K was 165ms (Dec 6,
         * 2007);
         *
         * that means it took 0.000165ms per merge into the group configuration;
         */
        final int TEST_SIZE = 100000;
        final double BASELINE = 0.00165;
        final double TOLERANCE = 3.0; // a number, representing percentage
        final long EXPECTED = (long) (BASELINE * TEST_SIZE * (1 + TOLERANCE));

        List<Configuration> timingSet = new ArrayList<Configuration>();
        for (int i = 0; i < TEST_SIZE; i++) {
            timingSet.add(configuration);
        }

        /*
         * getGroupConfiguration is the most expensive part of the algorithm that helps to render a ResourceGroup's
         * inventory tab
         *
         * so let's run timings on uber groups to make sure it's speedy
         */
        long before = System.currentTimeMillis();
        //GroupPluginConfigurationUpdate.getGroupConfiguration(timingSet);
        long after = System.currentTimeMillis();

        long millisTaken = after - before;

        if (millisTaken < EXPECTED) {
            if ((millisTaken + 5000) < EXPECTED) {
                assert false : "Merging GroupPluginConfigurationUpdates is too slow: " + "It took " + millisTaken
                    + " millis for " + TEST_SIZE + " resources; " + "We wanted less than " + EXPECTED + "ms";
            } else {
                System.out.println("Merging GroupPluginConfigurationUpdates is a bit slow: " + "It took " + millisTaken
                    + " millis for " + TEST_SIZE + " resources; " + "We wanted less than " + EXPECTED + "ms");
            }
        }
    }

    private void testGroupPluginConfigurationUpdateWorkflowHelper(boolean failOnChildUpdates) throws Exception {
        getTransactionManager().begin();

        Resource resource1 = newResource1;
        Resource resource2 = newResource2;

        try {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();

            Configuration configuration1 = new Configuration();
            configuration1.put(new PropertySimple("foo", "1"));
            configuration1.put(new PropertySimple("fail", "false"));

            Configuration configuration2 = new Configuration();
            configuration2.put(new PropertySimple("foo", "2"));
            configuration2.put(new PropertySimple("fail", "false"));

            /* begin simple checks */
            PluginConfigurationUpdate update1 = configurationManager.updatePluginConfiguration(overlord,
                resource1.getId(), configuration1);
            assert update1.getErrorMessage() == null : "We weren't expecting a failure here";

            PluginConfigurationUpdate update2 = configurationManager.updatePluginConfiguration(overlord,
                resource2.getId(), configuration2);
            assert update2.getErrorMessage() == null : "We weren't expecting a failure here";

            Configuration updatedConfiguration1 = configurationManager.getPluginConfiguration(overlord,
                resource1.getId());
            Configuration updatedConfiguration2 = configurationManager.getPluginConfiguration(overlord,
                resource2.getId());

            assert updatedConfiguration1.equals(configuration1) : "configuration1 was: " + updatedConfiguration1 + ", "
                + "expected was: " + configuration1;

            assert updatedConfiguration2.equals(configuration2) : "configuration2 was: " + updatedConfiguration2 + ", "
                + "expected was: " + configuration2;
            /*  end simple checks */

            /* begin group configuration creation checks */
            Configuration expectedGroupConfiguration = new Configuration();
            expectedGroupConfiguration
                .put(new PropertySimple("foo", GroupPluginConfigurationUpdate.MIXED_VALUES_MARKER));
            expectedGroupConfiguration.put(new PropertySimple("fail", "false"));

            List<Configuration> updatedConfigurations = Arrays.asList(new Configuration[] { updatedConfiguration1,
                updatedConfiguration2 });

            Configuration groupConfiguration = null;
            /*
            Configuration groupConfiguration = GroupPluginConfigurationUpdate
                .getGroupConfiguration(updatedConfigurations);
             */

            assert groupConfiguration.equals(expectedGroupConfiguration) : "group configuration was: "
                + groupConfiguration + ", " + "expected was: " + expectedGroupConfiguration;
            /*  end group configuration creation checks */

            /* begin group modification */
            Configuration groupConfigurationOverride = new Configuration();
            PropertySimple propertySimple1 = new PropertySimple("foo", "3");
            PropertySimple propertySimple2 = new PropertySimple("fail", "true");
            groupConfigurationOverride.put(propertySimple1);
            groupConfigurationOverride.put(propertySimple2);

            // regardless of failures, semantics dictate that the new configuration should be persisted to the resource
            Configuration expectedGroupConfigurationResults = new Configuration();
            expectedGroupConfigurationResults.put(new PropertySimple("foo", "3")); // from groupConfigurationOverride
            propertySimple1.setOverride(Boolean.TRUE);

            if (failOnChildUpdates) {
                expectedGroupConfigurationResults.put(new PropertySimple("fail", "true")); // from groupConfigurationOverride
                propertySimple2.setOverride(Boolean.TRUE); // will make TestServices fail
            } else {
                expectedGroupConfigurationResults.put(new PropertySimple("fail", "false")); // from both resource's current configuration
                propertySimple2.setOverride(Boolean.FALSE); // false is default, but setting explicitly for test clarity
            }

            Map<Integer, Configuration> memberConfigs = new HashMap<Integer, Configuration>();
            memberConfigs.put(resource1.getId(), configuration1);
            memberConfigs.put(resource2.getId(), configuration2);
            int groupUpdateId = configurationManager.scheduleGroupPluginConfigurationUpdate(overlord,
                compatibleGroup.getId(), memberConfigs);

            // instead of sleeping, let's directly execute what would normally be scheduled
            //configurationManager.completeGroupPluginConfigurationUpdate(groupUpdateId);

            GroupPluginConfigurationUpdate update = configurationManager.getGroupPluginConfigurationById(groupUpdateId);

            assert update != null : "Group plugin configuration update should not have been null";

            int i = 0;
            for (PluginConfigurationUpdate childUpdate : update.getConfigurationUpdates()) {
                Configuration childUpdateConfiguration = childUpdate.getConfiguration();
                assert childUpdateConfiguration.equals(expectedGroupConfigurationResults) : "new updateChildConfig["
                    + i + "] was: " + childUpdateConfiguration + ", " + "expected was: "
                    + expectedGroupConfigurationResults;
                i++;
            }

            Configuration configurationAfterGroupUpdate1 = configurationManager.getPluginConfiguration(overlord,
                resource1.getId());
            Configuration configurationAfterGroupUpdate2 = configurationManager.getPluginConfiguration(overlord,
                resource2.getId());

            ConfigurationUpdateStatus expectedResultStatus = null;

            if (failOnChildUpdates) {
                expectedResultStatus = ConfigurationUpdateStatus.FAILURE;
            } else {
                expectedResultStatus = ConfigurationUpdateStatus.SUCCESS;
            }

            assert configurationAfterGroupUpdate1.equals(expectedGroupConfigurationResults) : "new config1 was: "
                + configurationAfterGroupUpdate1 + ", " + "expected was: " + expectedGroupConfigurationResults;

            assert configurationAfterGroupUpdate2.equals(expectedGroupConfigurationResults) : "new config2 was: "
                + configurationAfterGroupUpdate2 + ", " + "expected was: " + expectedGroupConfigurationResults;

            assert update.getStatus() == expectedResultStatus : "Group plugin configuration update "
                + "should have been marked as " + expectedResultStatus + ": " + update;

            /*  end group modification */
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Failed test with this exception: " + e;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testResourceConfigurationUpdateCallbackFailure() throws Exception {
        Resource resource = newResource1;

        try {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();

            // this is simulating what the UI would be doing, build the config and call the server-side API
            // we'll pretend the user is the overlord - another test will check a real user to see permission errors
            Configuration configuration = new Configuration();
            configuration.put(new PropertySimple("myboolean", "invalid-bool"));

            ResourceConfigurationUpdate history = new ResourceConfigurationUpdate(resource, configuration, "dummyuser");

            assert history.getStatus().equals(ConfigurationUpdateStatus.INPROGRESS);

            configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration);

            Thread.sleep(2000); // wait for the test agent to complete the request

            // our test service pretends the agent got an error - it will set some errors and the call to
            // completedConfigurationUpdate is made inline (in the real code, this would be asynchronous)
            // at this point in time, the round trip messaging is done and we have the agent response
            getTransactionManager().begin();

            try {
                resource = em.find(Resource.class, resource.getId());

                history = resource.getResourceConfigurationUpdates().get(0);

                assert history.getStatus().equals(ConfigurationUpdateStatus.FAILURE) : "Status was "
                    + history.getStatus();
                assert history.getErrorMessage() != null;
                assert history.getErrorMessage().indexOf("This simulates a failed update") > 0;
                assert history.getConfiguration() != null;
                PropertySimple mybool = history.getConfiguration().getSimple("myboolean");
                assert mybool != null;
                assert mybool.getStringValue().equals("invalid-bool");
                assert mybool.getErrorMessage().indexOf("Not a valid boolean") > 0;
            } finally {
                getTransactionManager().rollback();
            }

            assert configurationManager.getLatestResourceConfigurationUpdate(overlord, resource.getId()) != null : "Resource wasn't configured yet - but we should have populated it with live values";

            // purging a non-existing request is a no-op
            configurationManager.purgeResourceConfigurationUpdate(overlord, Integer.MIN_VALUE, false);

            // delete the request now
            configurationManager.purgeResourceConfigurationUpdate(overlord, history.getId(), false);

            getTransactionManager().begin();

            try {
                resource = em.find(Resource.class, resource.getId());
                assert resource.getResourceConfigurationUpdates().size() == 1; // the initial built for us for free with the live config
            } finally {
                getTransactionManager().rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Failed test with this exception: " + e;
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testResourceConfigurationUpdateCallbackSuccess() throws Exception {
        Resource resource = newResource1;
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // this is simulating what the UI would be doing, build the config and call the server-side API
        // we'll pretend the user is the overlord - another test will check a real user to see permission errors
        Configuration configuration = new Configuration();
        configuration.put(new PropertySimple("myboolean", "true"));

        ResourceConfigurationUpdate request = new ResourceConfigurationUpdate(resource, configuration, "dummyuser");

        assert request.getStatus().equals(ConfigurationUpdateStatus.INPROGRESS);

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration);

        Thread.sleep(2000); // wait for the test agent to complete the request

        // at this point in time, the round trip messaging is done and we have the agent response
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resource.getId());
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(PageOrdering.ASC);

        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.findResourceConfigurationUpdatesByCriteria(overlord, criteria);

        assert requests.size() == 1;
        assert requests.get(0) != null;

        request = requests.get(0);

        assert request.getStatus().equals(ConfigurationUpdateStatus.SUCCESS);
        assert request.getErrorMessage() == null;
        assert request.getConfiguration() != null;
        PropertySimple mybool = request.getConfiguration().getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("true");
        assert mybool.getErrorMessage() == null;

        ResourceConfigurationUpdate current;
        current = configurationManager.getLatestResourceConfigurationUpdate(overlord, resource.getId());

        assert current != null;
        assert current.getId() == request.getId();
        assert current.getResource().equals(resource);
        assert current.getStatus().equals(request.getStatus());
        assert current.getSubjectName().equals(request.getSubjectName());
        assert current.getCreatedTime() == request.getCreatedTime();
        assert current.getModifiedTime() == request.getModifiedTime();
        assert current.getConfiguration().getId() == request.getConfiguration().getId();

        Configuration live = configurationManager.getLiveResourceConfiguration(overlord, resource.getId(), false);
        assert live != null;
        mybool = live.getSimple("myboolean");
        assert mybool != null;
        assert "true".equals(mybool.getStringValue());
        assert mybool.getErrorMessage() == null;

        // purging a non-existing request is a no-op
        configurationManager.purgeResourceConfigurationUpdate(overlord, Integer.MIN_VALUE, false);

        // delete the request now
        System.out.println("REQUEST WAS: " + request.toString());
        configurationManager.purgeResourceConfigurationUpdate(overlord, request.getId(), false);

        requests = configurationManager.findResourceConfigurationUpdates(overlord, resource.getId(), null, null, false,
            configUpdatesPageControl);

        assert requests.size() == 1; // it will create one for us from the "live" configuration
    }

    /** Exercise the ConfigurationManagerBean getOptionsForConfigurationDefinition.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testResourceConfigurationDefinitionsOptions() throws Exception {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        getTransactionManager().begin();
        Resource resource = em.find(Resource.class, newResource1.getId());
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        try {
            // this is simulating what the UI would be doing, build the config and call the server-side API
            // we'll pretend the user is the overlord - another test will check a real user to see permission errors
            ResourceType newResource1Type = resource.getResourceType();
            ConfigurationDefinition initialDefinition = newResource1Type.getResourceConfigurationDefinition();
            int loadCount = 300;
            HashSet<String> parsedNames = new HashSet<String>();
            for (int i = 0; i < loadCount; i++) {
                String name = "fakeProperty_" + i;
                initialDefinition
                    .put(new PropertyDefinitionSimple(name, "fake_" + i, false, PropertySimpleType.BOOLEAN));
                parsedNames.add(name);
            }
            newResource1Type.setResourceConfigurationDefinition(initialDefinition);
            newResource1Type = em.merge(newResource1Type);
            em.flush(); // so that slsb calls below will see the change reflected in the db

            ConfigurationDefinition configurationDefinition = newResource1Type.getResourceConfigurationDefinition();
            assert configurationDefinition != null : "Configuration Definition could not be located.";
            //retrieve the options for ConfigurationDefinition
            ConfigurationDefinition options = configurationManager.getOptionsForConfigurationDefinition(overlord,
                newResource1.getId(), -1, configurationDefinition);
            assert options != null : "Unable able to retrieve options for resource with id [" + newResource1.getId()
                + "].";
            assert !options.getPropertyDefinitions().entrySet().isEmpty() : "No PropertyDefinitionSimple instances found.";

            PropertyDefinitionSimple locatedPropertyDefSimple = null;
            int locatedCount = 0;
            for (Map.Entry<String, PropertyDefinition> entry : options.getPropertyDefinitions().entrySet()) {
                PropertyDefinition pd = entry.getValue();
                if (pd instanceof PropertyDefinitionSimple) {
                    locatedPropertyDefSimple = (PropertyDefinitionSimple) pd;
                    locatedCount++;
                    parsedNames.remove(locatedPropertyDefSimple.getName());
                }
            }
            assert locatedPropertyDefSimple != null : "PropertyDefinitionSimple was not located!";
            assert locatedCount != loadCount : "All expected properties were not loaded. Found '" + locatedCount + "'.";
            assert parsedNames.isEmpty() : "Not all loaded options were parsed.";

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testConfigurationRollback() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a few configs in history
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(3000); // wait for the test agent to complete the request

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "false"));
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(3000); // wait for the test agent to complete the request

        Configuration configuration3 = new Configuration();
        configuration3.put(new PropertySimple("myboolean", "TRUE"));
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration3);
        Thread.sleep(3000); // wait for the test agent to complete the request

        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resource.getId());
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(PageOrdering.ASC);
        List<ResourceConfigurationUpdate> history = configurationManager.findResourceConfigurationUpdatesByCriteria(
            overlord, criteria);
        assert history != null;
        assert history.size() == 3;

        Configuration currentConfiguration = history.get(2).getConfiguration();
        PropertySimple mybool = currentConfiguration.getSimple("myboolean");
        assert mybool != null;
        assertEquals("TRUE", mybool.getStringValue());

        // now grab one of the earlier configurations and rollback to it
        Configuration rollbackToHere = history.get(1).getConfiguration(); // the "false" one
        mybool = rollbackToHere.getSimple("myboolean");
        assert mybool != null;
        assertEquals("false", mybool.getStringValue());

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), rollbackToHere);
        Thread.sleep(3000); // wait for the test agent to complete the request

        criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resource.getId());
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(PageOrdering.ASC);

        history = configurationManager.findResourceConfigurationUpdatesByCriteria(overlord, criteria);
        assert history != null;
        assert history.size() == 4;
        ResourceConfigurationUpdate newConfigUpdate = history.get(3); // the last one is the new one
        Configuration newConfiguration = newConfigUpdate.getConfiguration();
        assert newConfiguration.getId() != rollbackToHere.getId();
        mybool = newConfiguration.getSimple("myboolean");
        assert mybool != null;
        assertEquals("false", mybool.getStringValue());
        assert mybool.getErrorMessage() == null;

        ResourceConfigurationUpdate current = configurationManager.getLatestResourceConfigurationUpdate(overlord,
            resource.getId());
        assert current != null;
        assert current.getId() == newConfigUpdate.getId();
        assert current.getResource().equals(resource);
        assert current.getStatus().equals(newConfigUpdate.getStatus());
        assert current.getSubjectName().equals(newConfigUpdate.getSubjectName());
        assert current.getCreatedTime() == newConfigUpdate.getCreatedTime();
        assert current.getModifiedTime() == newConfigUpdate.getModifiedTime();
        assert current.getConfiguration().getId() == newConfigUpdate.getConfiguration().getId();
    }

    @Test(enabled = ENABLE_TESTS)
    public void testPurgeConfigurationHistorySame() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a couple configs in history
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "true"));

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(6000); // wait for the test agent to complete the request
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(6000); // wait for the test agent to complete the request

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.findResourceConfigurationUpdates(overlord, resource.getId(), null, null, false,
            configUpdatesPageControl);

        assert requests != null;
        assert requests.size() == 1 : "Got " + requests.size() + " config update requests - expected 1.";
    }

    @Test(enabled = true)
    public void testPurgeConfigurationHistoryDifferent() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a couple configs in history
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "false"));

        ResourceConfigurationUpdate up;
        up = configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(2000); // wait for the test agent to complete the request
        assert configuration1.equals(configurationManager.getResourceConfiguration(resource.getId()));
        up = configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(2000); // wait for the test agent to complete the request
        assert configuration2.equals(configurationManager.getResourceConfiguration(resource.getId()));

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.findResourceConfigurationUpdates(overlord, resource.getId(), null, null, false,
            configUpdatesPageControl);

        assert requests != null;
        assert requests.size() == 2 : "Got " + requests.size() + " config update requests - expected 2.";

        // delete the first one
        ResourceConfigurationUpdate doomedRequest = requests.get(0);
        ResourceConfigurationUpdate savedRequest = requests.get(1);

        configurationManager.purgeResourceConfigurationUpdates(overlord, new int[] { doomedRequest.getId() }, false);

        // now get the current configs/requests and
        // make sure we deleted just the one configuration, leaving one left
        requests = configurationManager.findResourceConfigurationUpdates(overlord, resource.getId(), null, null, false,
            configUpdatesPageControl);

        assert requests.size() == 1;
        assert requests.get(0).getId() == savedRequest.getId();
    }

    @Test(enabled = ENABLE_TESTS)
    public void testPurgeConfigurationHistoryWithFailedUpdateRequest() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a couple update/requests in history - one request will fail, so only a single config in history will be there
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "invalid-boolean"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "true"));

        Configuration activeConfigurationBefore = configurationManager.getResourceConfiguration(resource.getId());

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(2000); // wait for the test agent to complete the request

        Configuration activeConfigurationAfter = configurationManager.getResourceConfiguration(resource.getId());
        assert activeConfigurationBefore.equals(activeConfigurationAfter) : "ActiveResourceConfiguration was not supposed to change for a failed update -- old was: "
            + activeConfigurationBefore + ", new was: " + activeConfigurationAfter;

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(2000); // wait for the test agent to complete the request

        Configuration activeConfiguration = configurationManager.getResourceConfiguration(resource.getId());
        assert activeConfiguration != null : "ActiveResourceConfiguration was not updated with configuration2";
        Map<String, PropertySimple> activeProperties = activeConfiguration.getSimpleProperties();
        assert activeProperties.size() == 1;
        assert activeProperties.containsKey("myboolean");
        PropertySimple activeProperty = activeProperties.get("myboolean");
        assert activeProperty.getName().equals("myboolean");
        assert "true".equals(activeProperty.getStringValue());

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.findResourceConfigurationUpdates(overlord, resource.getId(), null, null, false,
            configUpdatesPageControl);

        assert requests != null;
        assert requests.size() == 2; // one succeeded and one failed

        assert requests.get(0).getStatus() == ConfigurationUpdateStatus.FAILURE : "actual: "
            + requests.get(0).getStatus();
        assert requests.get(1).getStatus() == ConfigurationUpdateStatus.SUCCESS : "actual: "
            + requests.get(1).getStatus();

        ResourceConfigurationUpdate savedRequest = requests.get(0); // this is the one that failed
        ResourceConfigurationUpdate doomedRequest = requests.get(1); // this is the one that succeeded

        configurationManager.purgeResourceConfigurationUpdate(overlord, doomedRequest.getId(), false);

        // now get the current configs/requests and
        // make sure we deleted the only one configuration that succeeded, leaving one update record
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resource.getId());
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(PageOrdering.ASC);

        requests = configurationManager.findResourceConfigurationUpdatesByCriteria(overlord, criteria);

        assert requests.size() == 1;

        ResourceConfigurationUpdate request = requests.get(0);
        assert request.getId() == savedRequest.getId();
        assert request.getStatus().equals(ConfigurationUpdateStatus.FAILURE);
        assert request.getErrorMessage() != null;
        assert request.getErrorMessage().indexOf("This simulates a failed update") > 0;
        assert request.getConfiguration() != null;
        PropertySimple mybool = request.getConfiguration().getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("invalid-boolean");
        assert mybool.getErrorMessage().indexOf("Not a valid boolean") > 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testNoPermissionCallback() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Subject noPermSubject = new Subject("userWithNoPermissions", true, false);
        Resource resource = newResource1;

        try {
            noPermSubject = LookupUtil.getSubjectManager().createSubject(superuser, noPermSubject);
            noPermSubject = createSession(noPermSubject);

            try {
                configurationManager.updateResourceConfiguration(noPermSubject, resource.getId(), new Configuration());
                assert false : "Should not have been updated - user didn't have permissions";
            } catch (PermissionException expected) {
                System.out.println("This was expected and OK:\n" + expected);
            }

            try {
                configurationManager.updatePluginConfiguration(noPermSubject, resource.getId(), new Configuration());
                assert false : "Should not have been updated - user didn't have permissions";
            } catch (PermissionException expected) {
                System.out.println("This was expected and OK:\n" + expected);
                expected.printStackTrace();
            }
        } finally {
            LookupUtil.getSubjectManager().deleteUsers(LookupUtil.getSubjectManager().getOverlord(),
                new int[] { noPermSubject.getId() });
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInvalidUpdateCallback() throws Exception {
        ConfigurationUpdateResponse response = new ConfigurationUpdateResponse(Integer.MAX_VALUE, new Configuration(),
            ConfigurationUpdateStatus.SUCCESS, null);

        try {
            configurationManager.completeResourceConfigurationUpdate(response);
            assert false : "Should not have been able to process an unknown request - we didn't persist it yet";
        } catch (Exception expected) {
            System.out.println("This was expected and OK:\n" + expected);
        }
    }

    void persist(Object entity) throws Exception {
        getEntityManager().persist(entity);
        em.flush();
        em.clear();
    }

    void delete(Configuration configuration) {
        Configuration managedConfig = em.find(Configuration.class, configuration.getId());
        em.remove(managedConfig);
    }

    private class TestServices implements ConfigurationAgentService, DiscoveryAgentService {
        private Configuration savedConfiguration = null;

        public ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(
            ConfigurationUpdateRequest request) throws PluginContainerException {
            return null;
        }

        public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
            return null;
        }

        public void updateResourceConfiguration(final ConfigurationUpdateRequest request) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        PropertySimple mysleep = request.getConfiguration().getSimple("mysleep");
                        PropertySimple mybool = request.getConfiguration().getSimple("myboolean");
                        assert mybool != null;

                        ConfigurationUpdateResponse response;

                        if (mybool.getStringValue().equalsIgnoreCase("true")) {
                            savedConfiguration = request.getConfiguration();

                            // tests need us to sleep AFTER we save the config
                            if (mysleep != null) {
                                Thread.sleep(mysleep.getLongValue());
                            }

                            response = new ConfigurationUpdateResponse(request.getConfigurationUpdateId(), null,
                                ConfigurationUpdateStatus.SUCCESS, null);
                        } else if (mybool.getStringValue().equalsIgnoreCase("false")) {
                            savedConfiguration = request.getConfiguration();

                            // tests need us to sleep AFTER we save the config
                            if (mysleep != null) {
                                Thread.sleep(mysleep.getLongValue());
                            }

                            response = new ConfigurationUpdateResponse(request.getConfigurationUpdateId(), null,
                                ConfigurationUpdateStatus.SUCCESS, null);
                        } else {
                            mybool.setErrorMessage(ThrowableUtil.getStackAsString(new IllegalArgumentException(
                                "Not a valid boolean")));
                            response = new ConfigurationUpdateResponse(request.getConfigurationUpdateId(),
                                request.getConfiguration(), new NullPointerException("This simulates a failed update"));
                        }

                        LookupUtil.getConfigurationManager().completeResourceConfigurationUpdate(response);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException(t);
                    }
                }
            };

            thread.start();
        }

        public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
            throws InvalidPluginConfigurationClientException, PluginContainerException {
            PropertySimple mybool = newPluginConfiguration.getSimple("fail");
            if ((mybool != null) && mybool.getBooleanValue()) {
                throw new PluginContainerException("Simulates a failure to update plugin config");
            }

            return;
        }

        public ResourceConfigurationUpdate createResource(Resource parent, ResourceType resourceType,
            ResourceConfigurationUpdate configuration) throws PluginContainerException {
            return null;
        }

        public void deleteResource(Resource resource) throws PluginContainerException {
        }

        public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
            return savedConfiguration;
        }

        public Configuration loadResourceConfiguration(int resourceId, boolean fromStructured)
            throws PluginContainerException {
            return savedConfiguration;
        }

        public Configuration merge(Configuration configuration, int resourceId, boolean fromStructured)
            throws PluginContainerException {
            return null;
        }

        public void disableServiceScans(int serverResourceId) {
        }

        public void enableServiceScans(int serverResourceId, Configuration config) {
        }

        @NotNull
        public InventoryReport executeServerScanImmediately() throws PluginContainerException {
            return null;
        }

        @NotNull
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

        public AvailabilityReport getCurrentAvailability(Resource resource, boolean changesOnly) {
            return null;
        }

        public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
            Configuration pluginConfiguration, int ownerSubjectId) throws InvalidPluginConfigurationClientException,
            PluginContainerException {
            return null;
        }

        public Resource getPlatform() {
            return null;
        }

        public InventoryReport getPlatformScanResults(long jobId) {
            return null;
        }

        public long startPlatformScan(Configuration config) {
            return 0;
        }

        public boolean stopPlatformScan(long jobId) {
            return false;
        }

        public void uninventoryResource(int resourceId) {
        }

        public Configuration validate(Configuration configuration, int resourceId, boolean isStructured)
            throws PluginContainerException {
            return null;
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
