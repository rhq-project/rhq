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
package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Tests the configuration manager.
 */
@Test
public class ConfigurationManagerBeanTest extends AbstractEJB3Test {
    private static final boolean ENABLE_TESTS = true;

    private ConfigurationManagerLocal configurationManager;
    private ResourceManagerLocal resourceManager;
    private Resource newResource1;
    private Resource newResource2;
    private ResourceGroup compatibleGroup;
    private PageControl pageControl;
    private Agent agent;
    private Subject overlord;

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        // make sure page control sorts so the latest config is last
        pageControl = PageControl.getUnlimitedInstance();
        pageControl.addDefaultOrderingField("cu.id", PageOrdering.ASC);

        configurationManager = LookupUtil.getConfigurationManager();
        resourceManager = LookupUtil.getResourceManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        TestServices testServices = new TestServices();
        agentServiceContainer.configurationService = testServices;
        agentServiceContainer.discoveryService = testServices;
    }

    @AfterClass
    public void afterClass() throws Exception {
        unprepareForTestAgents();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        prepareScheduler();

        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            compatibleGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, null, // no role necessary here
                "compat");

            newResource1 = SessionTestHelper.createNewResourceForGroup(em, compatibleGroup, "res"
                + System.currentTimeMillis());
            newResource2 = SessionTestHelper.createNewResourceForGroup(em, compatibleGroup, "res"
                + System.currentTimeMillis());

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
        } finally {
            em.close();
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        try {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();

            try {
                resourceManager.deleteSingleResourceInNewTransaction(overlord, newResource1);
                resourceManager.deleteSingleResourceInNewTransaction(overlord, newResource2);
            } catch (Exception e) {
                System.out.println(e);
                throw e;
            }

            try {
                ResourceGroup group = em.find(ResourceGroup.class, compatibleGroup.getId());
                Agent a = em.find(Agent.class, agent.getId());

                em.remove(group);
                em.remove(a);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            } finally {
                em.close();
            }

            getTransactionManager().begin();
            em = getEntityManager();
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
            } finally {
                em.close();
            }

        } finally {
            unprepareScheduler();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testLastestConfiguration() throws Exception {
        int resourceId = newResource1.getId();

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a config, the another that spends some time in INPROGRESS before succeeding
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
        assert myprop.getStringValue().equals("true");

        // now update to that second config - the "agent" will sleep for a bit before it completes
        // so we will have an INPROGRESS configuration for a few seconds before it goes to SUCCESS
        configurationManager.updateResourceConfiguration(overlord, resourceId, configuration2);

        // still INPROGRESS - this should the first config still
        // we sleep for a tiny bit to allow the live config to be different from the last SUCCESS config
        Thread.sleep(3000);
        ResourceConfigurationUpdate history2 = configurationManager.getLatestResourceConfigurationUpdate(overlord,
            resourceId);
        assert history2 != null;
        assert history2.getId() == history1.getId();
        myprop = history2.getConfiguration().getSimple("myboolean");
        assert myprop != null;
        assert myprop.getStringValue().equals("true");
        myprop = history2.getConfiguration().getSimple("mysleep"); // this wasn't in the first config
        assert myprop == null;

        // wait for the test agent to complete the request that is in INPROGRESS
        Thread.sleep(6000);
        history2 = configurationManager.getLatestResourceConfigurationUpdate(overlord, resourceId);
        assert history2 != null;
        assert history2.getId() != history1.getId();
        myprop = history2.getConfiguration().getSimple("myboolean");
        assert myprop != null;
        assert myprop.getStringValue().equals("false");
        myprop = history2.getConfiguration().getSimple("mysleep");
        assert myprop.getLongValue() == 7000L;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeleteType() throws Exception {
        // the purpose of this little test is to test an error condition I'm getting when attempting to delete
        // a resource type - just forces a run with before/afterMethod
        return;
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

        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no plugin config settings yet

        configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration1);
        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("foo").getIntegerValue() == 1 : current;

        configurationManager.updatePluginConfiguration(overlord, resource.getId(), configuration2);
        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 1 : current;
        assert current.getSimple("bar").getIntegerValue() == 2 : current;
        assert current.getSimple("foo") == null : current; // this is gone now, we overrode it

        assert current.remove("foo") == null : current; // secondary test - just exercise the remove method
        assert current.remove("bar") != null : current; // again just test the remove method
        current.put(new PropertySimple("hello", "3"));
        configurationManager.updatePluginConfiguration(overlord, resource.getId(), current);
        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
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

        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() == 0 : current; // there is no plugin config settings yet

        PluginConfigurationUpdate update = configurationManager.updatePluginConfiguration(overlord, resource.getId(),
            configuration1);
        assert update.getErrorMessage() != null : "We should have simulated a failure, "
            + "but instead received no error.";

        // Even if a plugin container exception occurs, the plugin configuration should still be updated.
        current = configurationManager.getCurrentPluginConfiguration(overlord, resource.getId());
        assert current != null;
        assert current.getProperties().size() != 0 : current.toString();
    }

    @Test(enabled = false)
    public void testAggregatePluginConfigurationUpdateWorkflowSuccess() throws Exception {
        // TODO (joseph): Fix and then re-enable this test.
        //testAggregatePluginConfigurationUpdateWorkflowHelper(false);
    }

    @Test(enabled = false)
    public void testAggregatePluginConfigurationUpdateWorkflowFailure() throws Exception {
        // TODO (joseph): Fix and then re-enable this test.
        //testAggregatePluginConfigurationUpdateWorkflowHelper(true);
    }

    @Test(enabled = false)
    public void testAggregatePluginConfigurationUpdateMergeAlgorithmPerformance() throws Exception {
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
         * that means it took 0.000165ms per merge into the aggregate;
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
         * getAggregateConfiguration is the most expensive part of the algorithm that helps to render a ResourceGroup's
         * inventory tab
         *
         * so let's run timings on uber groups to make sure it's speedy
         */
        long before = System.currentTimeMillis();
        //AggregatePluginConfigurationUpdate.getAggregateConfiguration(timingSet);
        long after = System.currentTimeMillis();

        long millisTaken = after - before;

        if (millisTaken < EXPECTED) {
            if ((millisTaken + 5000) < EXPECTED) {
                assert false : "Merging AggregatePluginConfigurationUpdates is too slow: " + "It took " + millisTaken
                    + " millis for " + TEST_SIZE + " resources; " + "We wanted less than " + EXPECTED + "ms";
            } else {
                System.out.println("Merging AggregatePluginConfigurationUpdates is a bit slow: " + "It took "
                    + millisTaken + " millis for " + TEST_SIZE + " resources; " + "We wanted less than " + EXPECTED
                    + "ms");
            }
        }
    }

    private void testAggregatePluginConfigurationUpdateWorkflowHelper(boolean failOnChildUpdates) throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

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
            PluginConfigurationUpdate update1 = configurationManager.updatePluginConfiguration(overlord, resource1
                .getId(), configuration1);
            assert update1.getErrorMessage() == null : "We weren't expecting a failure here";

            PluginConfigurationUpdate update2 = configurationManager.updatePluginConfiguration(overlord, resource2
                .getId(), configuration2);
            assert update2.getErrorMessage() == null : "We weren't expecting a failure here";

            Configuration updatedConfiguration1 = configurationManager.getCurrentPluginConfiguration(overlord,
                resource1.getId());
            Configuration updatedConfiguration2 = configurationManager.getCurrentPluginConfiguration(overlord,
                resource2.getId());

            assert updatedConfiguration1.equals(configuration1) : "configuration1 was: " + updatedConfiguration1 + ", "
                + "expected was: " + configuration1;

            assert updatedConfiguration2.equals(configuration2) : "configuration2 was: " + updatedConfiguration2 + ", "
                + "expected was: " + configuration2;
            /*  end simple checks */

            /* begin aggregate configuration creation checks */
            Configuration expectedAggregate = new Configuration();
            expectedAggregate.put(new PropertySimple("foo", AggregatePluginConfigurationUpdate.MIXED_VALUES_MARKER));
            expectedAggregate.put(new PropertySimple("fail", "false"));

            List<Configuration> updatedConfigurations = Arrays.asList(new Configuration[] { updatedConfiguration1,
                updatedConfiguration2 });

            Configuration aggregateConfiguration = null;
            /*
            Configuration aggregateConfiguration = AggregatePluginConfigurationUpdate
                .getAggregateConfiguration(updatedConfigurations);
             */

            assert aggregateConfiguration.equals(expectedAggregate) : "aggregate was: " + aggregateConfiguration + ", "
                + "expected was: " + expectedAggregate;
            /*  end aggregate configuration creation checks */

            /* begin aggregate modification */
            Configuration overrideAggregate = new Configuration();
            PropertySimple propertySimple1 = new PropertySimple("foo", "3");
            PropertySimple propertySimple2 = new PropertySimple("fail", "true");
            overrideAggregate.put(propertySimple1);
            overrideAggregate.put(propertySimple2);

            // regardless of failures, semantics dictate that the new configuration should be persisted to the resource
            Configuration expectedAggregateResults = new Configuration();
            expectedAggregateResults.put(new PropertySimple("foo", "3")); // from overrideAggregate
            propertySimple1.setOverride(Boolean.TRUE);

            if (failOnChildUpdates) {
                expectedAggregateResults.put(new PropertySimple("fail", "true")); // from overrideAggregate
                propertySimple2.setOverride(Boolean.TRUE); // will make TestServices fail
            } else {
                expectedAggregateResults.put(new PropertySimple("fail", "false")); // from both resource's current configuration
                propertySimple2.setOverride(Boolean.FALSE); // false is default, but setting explicitly for test clarity
            }

            int aggregateUpdateId = configurationManager.scheduleAggregatePluginConfigurationUpdate(overlord,
                compatibleGroup.getId(), overrideAggregate);

            // instead of sleeping, let's directly execute what would normally be scheduled
            //configurationManager.completeAggregatePluginConfigurationUpdate(aggregateUpdateId);

            AggregatePluginConfigurationUpdate update = configurationManager
                .getAggregatePluginConfigurationById(aggregateUpdateId);

            assert update != null : "Aggregate plugin configuration update should not have been null";

            int i = 0;
            for (PluginConfigurationUpdate childUpdate : update.getConfigurationUpdates()) {
                Configuration childUpdateConfiguration = childUpdate.getConfiguration();
                assert childUpdateConfiguration.equals(expectedAggregateResults) : "new updateChildConfig[" + i
                    + "] was: " + childUpdateConfiguration + ", " + "expected was: " + expectedAggregateResults;
                i++;
            }

            Configuration configurationAfterAggregate1 = configurationManager.getCurrentPluginConfiguration(overlord,
                resource1.getId());
            Configuration configurationAfterAggregate2 = configurationManager.getCurrentPluginConfiguration(overlord,
                resource2.getId());

            ConfigurationUpdateStatus expectedResultStatus = null;

            if (failOnChildUpdates) {
                expectedResultStatus = ConfigurationUpdateStatus.FAILURE;
            } else {
                expectedResultStatus = ConfigurationUpdateStatus.SUCCESS;
            }

            assert configurationAfterAggregate1.equals(expectedAggregateResults) : "new config1 was: "
                + configurationAfterAggregate1 + ", " + "expected was: " + expectedAggregateResults;

            assert configurationAfterAggregate2.equals(expectedAggregateResults) : "new config2 was: "
                + configurationAfterAggregate2 + ", " + "expected was: " + expectedAggregateResults;

            assert update.getStatus() == expectedResultStatus : "Aggregate plugin configuration update "
                + "should have been marked as " + expectedResultStatus + ": " + update;

            /*  end aggregate modification */
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Failed test with this exception: " + e;
        } finally {
            getTransactionManager().rollback();
            try {
                em.close();
            } catch (Exception e) {
            }
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

            Thread.sleep(4000); // wait for the test agent to complete the request

            // our test service pretends the agent got an error - it will set some errors and the call to
            // completedConfigurationUpdate is made inline (in the real code, this would be asynchronous)
            // at this point in time, the round trip messaging is done and we have the agent response
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
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
                try {
                    em.close();
                } catch (Exception e) {
                }
            }

            assert configurationManager.getLatestResourceConfigurationUpdate(overlord, resource.getId()) != null : "Resource wasn't configured yet - but we should have populated it with live values";

            // purging a non-existing request is a no-op
            configurationManager.purgeResourceConfigurationUpdate(overlord, Integer.MIN_VALUE, false);

            // delete the request now
            configurationManager.purgeResourceConfigurationUpdate(overlord, history.getId(), false);

            getTransactionManager().begin();
            em = getEntityManager();
            try {
                resource = em.find(Resource.class, resource.getId());
                assert resource.getResourceConfigurationUpdates().size() == 1; // the initial built for us for free with the live config
            } finally {
                getTransactionManager().rollback();
                try {
                    em.close();
                } catch (Exception e) {
                }
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

        Thread.sleep(4000); // wait for the test agent to complete the request

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

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
        assert current.getCreatedTime().getTime() == request.getCreatedTime().getTime();
        assert current.getModifiedTime().getTime() == request.getModifiedTime().getTime();
        assert current.getConfiguration().getId() == request.getConfiguration().getId();

        Configuration live = configurationManager.getLiveResourceConfiguration(overlord, resource.getId());
        assert live != null;
        mybool = live.getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("true");
        assert mybool.getErrorMessage() == null;

        // purging a non-existing request is a no-op
        configurationManager.purgeResourceConfigurationUpdate(overlord, Integer.MIN_VALUE, false);

        // delete the request now
        System.out.println("REQUEST WAS: " + request.toString());
        configurationManager.purgeResourceConfigurationUpdate(overlord, request.getId(), false);

        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

        assert requests.size() == 1; // it will create one for us from the "live" configuration
    }

    @Test(enabled = ENABLE_TESTS)
    public void testConfigurationRollback() throws Exception {
        Resource resource = newResource1;

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // create a couple configs in history
        Configuration configuration1 = new Configuration();
        configuration1.put(new PropertySimple("myboolean", "true"));

        Configuration configuration2 = new Configuration();
        configuration2.put(new PropertySimple("myboolean", "false"));

        Configuration configuration3 = new Configuration();
        configuration3.put(new PropertySimple("myboolean", "TRUE"));

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(2000); // wait for the test agent to complete the request
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(2000); // wait for the test agent to complete the request
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration3);
        Thread.sleep(2000); // wait for the test agent to complete the request

        List<ResourceConfigurationUpdate> history;
        history = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

        assert history != null;
        assert history.size() == 3;
        Configuration currentConfiguration = history.get(2).getConfiguration(); // the last is the current one
        PropertySimple mybool = currentConfiguration.getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("TRUE");

        // now grab one of the earlier configurations and rollback to it
        Configuration rollbackToHere = history.get(1).getConfiguration(); // the "false" one
        mybool = rollbackToHere.getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("false");

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), rollbackToHere);
        Thread.sleep(2000); // wait for the test agent to complete the request

        history = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);
        assert history != null;
        assert history.size() == 4;
        ResourceConfigurationUpdate newConfigUpdate = history.get(3);
        Configuration newConfiguration = newConfigUpdate.getConfiguration(); // the last is the new one
        assert newConfiguration.getId() != rollbackToHere.getId();
        mybool = newConfiguration.getSimple("myboolean");
        assert mybool != null;
        assert mybool.getStringValue().equals("false");
        assert mybool.getErrorMessage() == null;

        ResourceConfigurationUpdate current = configurationManager.getLatestResourceConfigurationUpdate(overlord,
            resource.getId());
        assert current != null;
        assert current.getId() == newConfigUpdate.getId();
        assert current.getResource().equals(resource);
        assert current.getStatus().equals(newConfigUpdate.getStatus());
        assert current.getSubjectName().equals(newConfigUpdate.getSubjectName());
        assert current.getCreatedTime().getTime() == newConfigUpdate.getCreatedTime().getTime();
        assert current.getModifiedTime().getTime() == newConfigUpdate.getModifiedTime().getTime();
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
        Thread.sleep(2000); // wait for the test agent to complete the request
        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(2000); // wait for the test agent to complete the request

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

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
        assert configuration1.equals(configurationManager.getActiveResourceConfiguration(resource.getId()));
        up = configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(2000); // wait for the test agent to complete the request
        assert configuration2.equals(configurationManager.getActiveResourceConfiguration(resource.getId()));

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

        assert requests != null;
        assert requests.size() == 2 : "Got " + requests.size() + " config update requests - expected 2.";

        // delete the first one
        ResourceConfigurationUpdate doomedRequest = requests.get(0);
        ResourceConfigurationUpdate savedRequest = requests.get(1);

        configurationManager.purgeResourceConfigurationUpdates(overlord, new int[] { doomedRequest.getId() }, false);

        // now get the current configs/requests and
        // make sure we deleted just the one configuration, leaving one left
        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

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

        Configuration activeConfigurationBefore = configurationManager.getActiveResourceConfiguration(resource.getId());

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration1);
        Thread.sleep(4000); // wait for the test agent to complete the request

        Configuration activeConfigurationAfter = configurationManager.getActiveResourceConfiguration(resource.getId());
        assert activeConfigurationBefore.equals(activeConfigurationAfter) : "ActiveResourceConfiguration was not supposed to change for a failed update -- old was: "
            + activeConfigurationBefore + ", new was: " + activeConfigurationAfter;

        configurationManager.updateResourceConfiguration(overlord, resource.getId(), configuration2);
        Thread.sleep(4000); // wait for the test agent to complete the request

        Configuration activeConfiguration = configurationManager.getActiveResourceConfiguration(resource.getId());
        assert activeConfiguration != null : "ActiveResourceConfiguration was not updated with configuration2";
        Map<String, PropertySimple> activeProperties = activeConfiguration.getSimpleProperties();
        assert activeProperties.size() == 1;
        assert activeProperties.containsKey("myboolean");
        PropertySimple activeProperty = activeProperties.get("myboolean");
        assert activeProperty.getName().equals("myboolean");
        assert activeProperty.getStringValue().equals("true");

        // at this point in time, the round trip messaging is done and we have the agent response
        List<ResourceConfigurationUpdate> requests;

        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

        assert requests != null;
        assert requests.size() == 2; // one succeeded and one failed

        assert requests.get(0).getStatus() == ConfigurationUpdateStatus.FAILURE;
        assert requests.get(1).getStatus() == ConfigurationUpdateStatus.SUCCESS;

        ResourceConfigurationUpdate savedRequest = requests.get(0); // this is the one that failed
        ResourceConfigurationUpdate doomedRequest = requests.get(1); // this is the one that succeeded

        configurationManager.purgeResourceConfigurationUpdate(overlord, doomedRequest.getId(), false);

        // now get the current configs/requests and
        // make sure we deleted the only one configuration that succeeded, leaving one update record
        requests = configurationManager.getResourceConfigurationUpdates(overlord, resource.getId(), pageControl);

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
            createSession(noPermSubject);

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
                new Integer[] { noPermSubject.getId() });
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
                            mybool.setErrorMessageFromThrowable(new IllegalArgumentException("Not a valid boolean"));
                            response = new ConfigurationUpdateResponse(request.getConfigurationUpdateId(), request
                                .getConfiguration(), new NullPointerException("This simulates a failed update"));
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

        public void disableServiceScans(int serverResourceId) {
        }

        public void enableServiceScans(int serverResourceId, Configuration config) {
        }

        public InventoryReport executeServerScanImmediately() throws PluginContainerException {
            return null;
        }

        public InventoryReport executeServiceScanImmediately() throws PluginContainerException {
            return null;
        }

        public void executeServiceScanDeferred() {
            return;
        }

        public Availability getAvailability(Resource resource) {
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

        public void removeResource(int resourceId) {
        }

        @Asynchronous(guaranteedDelivery = true)
        public void synchronizeInventory(ResourceSyncInfo syncInfo) {
        }
    }
}