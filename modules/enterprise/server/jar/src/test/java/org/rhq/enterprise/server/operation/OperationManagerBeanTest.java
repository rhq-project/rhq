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
package org.rhq.enterprise.server.operation;

import java.util.Date;
import java.util.List;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;

import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.operation.CancelResults.InterruptedState;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link OperationManagerBean} SLSB.
 */
@Test(groups = "operation-manager")
public class OperationManagerBeanTest extends AbstractEJB3Test {
    private static final boolean ENABLE_TESTS = true;

    private ConfigurationManagerLocal configurationManager;
    private OperationManagerLocal operationManager;
    private SchedulerLocal schedulerManager;
    private Resource newResource;
    private OperationDefinition newOperation;
    private ResourceGroup newGroup;
    private OperationServerService operationServerService;

    // defines the behavior of the simulated agent
    // Sleep - if more than 0, the amount of milliseconds the agent sleeps
    // Timeout - if true, the operation will fail due to a "timeout"
    // Error - if not null, the operation will fail with this as the error message
    private long simulatedOperation_Sleep;
    private boolean simulatedOperation_Timeout;
    private String simulatedOperation_Error;

    // for those tests that cancel an operation, this will be the results of the simulated cancellation
    private CancelResults simulatedOperation_CancelResults;

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        configurationManager = LookupUtil.getConfigurationManager();
        operationManager = LookupUtil.getOperationManager();
        schedulerManager = LookupUtil.getSchedulerBean();

        operationServerService = new OperationServerServiceImpl();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.operationService = new TestConfigService();
    }

    @AfterClass
    public void afterClass() {
        operationServerService = null;
        unprepareForTestAgents();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        prepareScheduler();

        simulatedOperation_Sleep = 500L;
        simulatedOperation_Timeout = false;
        simulatedOperation_Error = null;

        newResource = createNewResource();
        newOperation = newResource.getResourceType().getOperationDefinitions().iterator().next();
        newGroup = newResource.getExplicitGroups().iterator().next();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        try {
            deleteNewResource(newResource);
        } finally {
            unprepareScheduler();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSchedulerCustomProperties() throws Exception {
        // our test scheduler configuration defines a custom timeout for all operations
        assert schedulerManager.getDefaultOperationTimeout() != null;
        assert schedulerManager.getDefaultOperationTimeout() == 5;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testTrueTimeout() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 20000L; // the operation timeout is defined at 10 seconds, we'll block it for 20s

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;

        Thread.sleep(12000L); // wait for it to timeout

        // this will change all INPROGRESS histories that have timed out to FAILURE
        operationManager.checkForTimedOutOperations(superuser);

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        ResourceOperationHistory history = results.get(0);
        System.out.println("~~~~~~~~~~~~~~~~~" + history);
        assert history.getErrorMessage() != null : history;
        assert history.getErrorMessage().indexOf("Timed out") > -1 : history;
        assert history.getStatus() == OperationRequestStatus.FAILURE : history;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);

        // make sure it was purged
        results = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;

        // nothing to unschedule really

        // but lets prove to ourselves that it isn't scheduled anymore (because it executed)
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testUnscheduledGroupOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        // let the trigger not fire until several seconds from now so we can query the schedule itself
        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 10000L));
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(), null,
            true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        List<GroupOperationSchedule> schedules;
        schedules = operationManager.getScheduledGroupOperations(superuser, newGroup.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        GroupOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getGroup().getId() == newGroup.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        // let's immediately unschedule it before it triggers
        operationManager.unscheduleGroupOperation(superuser, returnedSchedule.getJobId().toString(), returnedSchedule
            .getGroup().getId());

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 0;

        // should be no resource histories that belong to it
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testUnscheduledGroupOperationWithParameters() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Configuration params = new Configuration();
        params.put(new PropertySimple("param1", "group-test"));
        params.put(new PropertySimple("param2", "blah"));

        // let the trigger not fire until several seconds from now so we can query the schedule itself
        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 3600000L));
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(), null,
            true, "testOp", params, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() != null;
        assert schedule.getGroup().getId() == newGroup.getId();

        int configId = params.getId();
        Configuration returnedConfiguration = configurationManager.getConfigurationById(configId);
        assert returnedConfiguration.getProperties().size() == 2;
        assert returnedConfiguration.getSimple("param1").getStringValue().equals("group-test");
        assert returnedConfiguration.getSimple("param2").getStringValue().equals("blah");

        List<GroupOperationSchedule> schedules;
        schedules = operationManager.getScheduledGroupOperations(superuser, newGroup.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        GroupOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getGroup().getId() == newGroup.getId();
        assert returnedSchedule.getParameters() != null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        PageList<GroupOperationScheduleComposite> list;
        list = operationManager.getCurrentlyScheduledGroupOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 1;
        assert list.get(0).getGroupId() == newGroup.getId();
        assert list.get(0).getGroupName().equals(newGroup.getName());
        assert list.get(0).getOperationName().equals("Test Operation");

        // let's immediately unschedule it before it triggers
        operationManager.unscheduleGroupOperation(superuser, returnedSchedule.getJobId().toString(), returnedSchedule
            .getGroup().getId());

        list = operationManager.getCurrentlyScheduledGroupOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 0;

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 0;

        // should be no resource histories that belong to it
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;

        // should be no dangling configuration entities representing group operation parameters
        Configuration returnedConfiguration2 = configurationManager.getConfigurationById(configId);
        assert returnedConfiguration2 == null;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetScheduledGroupOperations() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        // let the trigger not fire until several seconds from now so we can query the schedule itself
        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 5000L));
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(), null,
            true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        List<GroupOperationSchedule> schedules;
        schedules = operationManager.getScheduledGroupOperations(superuser, newGroup.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        GroupOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getGroup().getId() == newGroup.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        Thread.sleep(9000L); // wait for it to be triggered and finish

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 1 : "Expected 1 result, but got " + results.size();

        operationManager.deleteOperationHistory(superuser, results.get(0).getId(), false);

        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 0;

        // purging group history purges all resource histories that belong to it
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;

        // see that it isn't scheduled anymore
        schedules = operationManager.getScheduledGroupOperations(superuser, newGroup.getId());
        assert schedules != null;
        assert schedules.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testCancelGroupOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 30000L;
        simulatedOperation_CancelResults = new CancelResults(InterruptedState.RUNNING);

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        operationManager.scheduleGroupOperation(superuser, newGroup.getId(), new int[] { newGroup.getId() }, true,
            "testOp", null, trigger, "desc");

        PageList<GroupOperationHistory> results = null;

        // wait for it to be triggered so we get a history item
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            results = operationManager.getPendingGroupOperationHistories(superuser, newGroup.getId(), PageControl
                .getUnlimitedInstance());
            if ((results != null) && (results.size() > 0)) {
                break; // operation was triggered - got the history item
            }
        }

        assert results != null;
        assert results.size() == 1 : "-->" + results;
        GroupOperationHistory history = results.get(0);
        assert history.getStatus() == OperationRequestStatus.INPROGRESS : history;

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2 = null;

        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            results2 = operationManager.getPendingResourceOperationHistories(superuser, newResource.getId(),
                PageControl.getUnlimitedInstance());
            if ((results2 != null) && (results2.size() > 0)) {
                break; // operation was triggered - got the history item
            }
        }

        assert results2.size() == 1 : "Should have had 1 resource history result: " + results2;

        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getStatus() == OperationRequestStatus.INPROGRESS : rHistory;

        // cancel the group history - which cancels all the resource histories
        operationManager.cancelOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        assert results.get(0).getStatus() == OperationRequestStatus.CANCELED : results.get(0);
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2.size() == 1 : "Should have had 1 resource history result: " + results2;
        assert results2.get(0).getStatus() == OperationRequestStatus.CANCELED : results2.get(0);

        // try to cancel it again, just to make sure it blows up appropriately
        try {
            operationManager.cancelOperationHistory(superuser, history.getId(), false);
            assert false : "Should not have been able to cancel an operation that is not INPROGRESS";
        } catch (EJBException expected) {
            assert expected.getCause() instanceof IllegalStateException;
        }

        // purge the group history
        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testCancelUncancelableGroupOperation() throws Exception {
        // this test will attempt to cancel an operation that has already finished, in effect
        // trying to cancel an uncancelable operation.  This simulates the situation when
        // an agent has finished running an operation but it has not yet sent the "success"
        // or "failed" message to the server.  So on the agent side it is finished, but the
        // server side still thinks its INPROGRESS.

        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 30000L;
        simulatedOperation_CancelResults = new CancelResults(InterruptedState.FINISHED);

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        operationManager.scheduleGroupOperation(superuser, newGroup.getId(), new int[] { newGroup.getId() }, true,
            "testOp", null, trigger, "desc");

        PageList<GroupOperationHistory> results = null;

        // wait for it to be triggered so we get a history item
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            results = operationManager.getPendingGroupOperationHistories(superuser, newGroup.getId(), PageControl
                .getUnlimitedInstance());
            if ((results != null) && (results.size() > 0)) {
                break; // operation was triggered - got the history item
            }
        }

        assert results != null;
        assert results.size() == 1;
        GroupOperationHistory history = results.get(0);
        assert history.getStatus() == OperationRequestStatus.INPROGRESS : history;

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getPendingResourceOperationHistories(superuser, newResource.getId(), PageControl
            .getUnlimitedInstance());
        assert results2.size() == 1 : "Should have had 1 resource history result: " + results2;

        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getStatus() == OperationRequestStatus.INPROGRESS : rHistory;

        // cancel the group history - but we'll see that even though the group history will say canceled,
        // this doesn't actually cancel the FINISHED resource operation.  This simulates the fact that
        // the agent couldn't cancel the resource op since it already finished.
        operationManager.cancelOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 1;
        assert results.get(0).getStatus() == OperationRequestStatus.CANCELED : results.get(0);

        results = operationManager.getPendingGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results.size() == 0;

        // still pending - our operation wasn't really canceled - waiting for the agent to tell us its finished
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2.size() == 0;
        results2 = operationManager.getPendingResourceOperationHistories(superuser, newResource.getId(), PageControl
            .getUnlimitedInstance());
        assert results2.size() == 1;
        assert results2.get(0).getStatus() == OperationRequestStatus.INPROGRESS : results2.get(0);

        // purge the group history (note we tell it to even purge those in progress)
        operationManager.deleteOperationHistory(superuser, history.getId(), true);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleGroupOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        // make it a success
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(),
            new int[] { newGroup.getId() }, true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        Thread.sleep(4000L); // wait for it to finish, should be fast

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        GroupOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() == null : history;
        assert history.getStatus() == OperationRequestStatus.SUCCESS : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;

        PageList<GroupOperationLastCompletedComposite> list;
        list = operationManager.getRecentlyCompletedGroupOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 1;
        assert list.get(0).getOperationHistoryId() == history.getId();
        assert list.get(0).getGroupId() == newGroup.getId();
        assert list.get(0).getGroupName().equals(newGroup.getName());
        assert list.get(0).getOperationName().equals("Test Operation");

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2.size() == 1 : "Should have had 1 result: " + results2;

        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getId() > 0 : rHistory;
        assert rHistory.getJobId() != null : rHistory;
        assert rHistory.getJobName() != null : rHistory;
        assert rHistory.getJobGroup() != null : rHistory;
        assert rHistory.getErrorMessage() == null : rHistory;
        assert rHistory.getStatus() == OperationRequestStatus.SUCCESS : rHistory;
        assert rHistory.getSubjectName().equals(superuser.getName()) : rHistory;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;

        list = operationManager.getRecentlyCompletedGroupOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleGroupOperationRecurring() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        // make it a success
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", 1, 750);
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(),
            new int[] { newGroup.getId() }, true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        Thread.sleep(4000L); // wait for it to finish, should be fast

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());

        // the group job executed twice
        assert results != null;
        assert results.size() == 2 : results;
        GroupOperationHistory history0 = results.get(0);
        GroupOperationHistory history1 = results.get(1);
        assert history0.getId() > 0 : history0;
        assert history0.getJobId() != null : history0;
        assert history0.getJobName() != null : history0;
        assert history0.getJobGroup() != null : history0;
        assert history0.getErrorMessage() == null : history0;
        assert history0.getStatus() == OperationRequestStatus.SUCCESS : history0;
        assert history0.getSubjectName().equals(superuser.getName()) : history0;
        assert history1.getId() > 0 : history1;
        assert history1.getId() != history0.getId() : history1;
        assert history1.getJobId() != null : history1;
        assert !history1.getJobId().equals(history0.getJobId()) : history1;
        assert history1.getJobName() != null : history1;
        assert history1.getJobGroup() != null : history1;
        assert history1.getErrorMessage() == null : history1;
        assert history1.getStatus() == OperationRequestStatus.SUCCESS : history1;
        assert history1.getSubjectName().equals(superuser.getName()) : history1;

        // get the one resource's two history items from the group (resource executed once per group trigger)
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2.size() == 2 : "Should have had 2 results since it was triggered twice: " + results2;

        ResourceOperationHistory rHistory0 = results2.get(0);
        assert rHistory0.getId() > 0 : rHistory0;
        assert rHistory0.getJobId() != null : rHistory0;
        assert rHistory0.getJobName() != null : rHistory0;
        assert rHistory0.getJobGroup() != null : rHistory0;
        assert rHistory0.getErrorMessage() == null : rHistory0;
        assert rHistory0.getStatus() == OperationRequestStatus.SUCCESS : rHistory0;
        assert rHistory0.getSubjectName().equals(superuser.getName()) : rHistory0;

        ResourceOperationHistory rHistory1 = results2.get(1);
        assert rHistory1.getId() > 0 : rHistory1;
        assert rHistory1.getId() != rHistory0.getId() : rHistory1;
        assert rHistory1.getJobId() != null : rHistory1;
        assert !rHistory1.getJobId().equals(rHistory0.getJobId()) : rHistory1;
        assert rHistory1.getJobId().getJobGroup().equals(rHistory1.getJobGroup()) : rHistory1;
        assert rHistory1.getJobId().getJobName().equals(rHistory1.getJobName()) : rHistory1;
        assert rHistory1.getJobName() != null : rHistory1;
        assert rHistory1.getJobGroup() != null : rHistory1;
        assert rHistory1.getJobGroup().equals(rHistory0.getJobGroup()) : rHistory1;
        assert rHistory1.getErrorMessage() == null : rHistory1;
        assert rHistory1.getStatus() == OperationRequestStatus.SUCCESS : rHistory1;
        assert rHistory1.getSubjectName().equals(superuser.getName()) : rHistory1;

        operationManager.deleteOperationHistory(superuser, history0.getId(), false);
        operationManager.deleteOperationHistory(superuser, history1.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0 : results; // none left, we purged the two group histories

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleGroupOperationWithParameters() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        // make it a success
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        Configuration params = new Configuration();
        params.put(new PropertySimple("param1", "group-test"));
        params.put(new PropertySimple("param2", "blah"));

        // the manager will ignore duplicates in the list - we put dups in here to
        // test the comma-separator parser in the manager
        int[] order = new int[] { newGroup.getId(), newGroup.getId() };

        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(), order,
            true, "testOp", params, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() != null;
        assert schedule.getParameters().getId() > 0;
        assert schedule.getParameters().getNames().size() == 2;
        assert schedule.getParameters().getNames().contains("param1");
        assert schedule.getParameters().getNames().contains("param2");
        assert schedule.getParameters().getSimple("param1").getStringValue().equals("group-test");
        assert schedule.getParameters().getSimple("param2").getStringValue().equals("blah");
        assert schedule.getGroup().getId() == newGroup.getId();

        int scheduleParamId = schedule.getParameters().getId();

        Thread.sleep(4000L); // wait for it to finish, should be fast

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        GroupOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() == null : history;
        assert history.getStatus() == OperationRequestStatus.SUCCESS : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;
        assert history.getGroup().getId() == newGroup.getId();

        // parameters and results are lazily loaded in the paginated queries, but are eagerly individually
        history = (GroupOperationHistory) operationManager.getOperationHistoryByHistoryId(superuser, history.getId());
        assert history.getParameters().getId() != scheduleParamId : "params should be copies - not shared";

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getId() > 0 : rHistory;
        assert rHistory.getJobId() != null : rHistory;
        assert rHistory.getJobName() != null : rHistory;
        assert rHistory.getJobGroup() != null : rHistory;
        assert rHistory.getErrorMessage() == null : rHistory;
        assert rHistory.getStatus() == OperationRequestStatus.SUCCESS : rHistory;
        assert rHistory.getSubjectName().equals(superuser.getName()) : rHistory;

        // parameters and results are lazily loaded in the paginated queries, but are eagerly individually
        rHistory = (ResourceOperationHistory) operationManager.getOperationHistoryByHistoryId(superuser, rHistory
            .getId());
        assert rHistory.getResults() != null;
        assert rHistory.getResults().getSimple("param1echo") != null;
        assert rHistory.getResults().getSimple("param1echo").getStringValue().equals("group-test");
        assert rHistory.getParameters().getId() != scheduleParamId : "params should be copies - not shared";
        assert rHistory.getParameters().getId() != history.getParameters().getId() : "params should be copies - not shared";

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleGroupOperationError() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = "an error!";
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(),
            new int[] { newGroup.getId() }, true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        Thread.sleep(4000L); // wait for it to finish, should be fast

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1 : "Did not get 1 result back, but " + results.size();
        GroupOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() != null : history;
        assert history.getErrorMessage().indexOf(newResource.getName()) > -1 : history; // the name will be in the group error message
        assert history.getStatus() == OperationRequestStatus.FAILURE : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getId() > 0 : rHistory;
        assert rHistory.getJobId() != null : rHistory;
        assert rHistory.getJobName() != null : rHistory;
        assert rHistory.getJobGroup() != null : rHistory;
        assert rHistory.getErrorMessage() != null : rHistory;
        assert rHistory.getErrorMessage().indexOf("an error!") > -1 : rHistory;
        assert rHistory.getStatus() == OperationRequestStatus.FAILURE : rHistory;
        assert rHistory.getSubjectName().equals(superuser.getName()) : rHistory;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleGroupOperationTimeout() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = true;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        GroupOperationSchedule schedule = operationManager.scheduleGroupOperation(superuser, newGroup.getId(),
            new int[] { newGroup.getId() }, true, "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getGroup().getId() == newGroup.getId();

        Thread.sleep(4000L); // wait for it to finish, should be fast

        PageList<GroupOperationHistory> results;
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        GroupOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() != null : history;
        assert history.getErrorMessage().indexOf(newResource.getName()) > -1 : history; // the name will be in the group error message
        assert history.getStatus() == OperationRequestStatus.FAILURE : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;

        // get the one resource history from the group
        PageList<ResourceOperationHistory> results2;
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        ResourceOperationHistory rHistory = results2.get(0);
        assert rHistory.getId() > 0 : rHistory;
        assert rHistory.getJobId() != null : rHistory;
        assert rHistory.getJobName() != null : rHistory;
        assert rHistory.getJobGroup() != null : rHistory;
        assert rHistory.getErrorMessage() != null : rHistory;
        assert rHistory.getErrorMessage().indexOf("Timed out") > -1 : rHistory;
        assert rHistory.getStatus() == OperationRequestStatus.FAILURE : rHistory;
        assert rHistory.getSubjectName().equals(superuser.getName()) : rHistory;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedGroupOperationHistories(superuser, newGroup.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0; // none left, we purged the only group history there was

        // purging group history purges all resource histories that belong to it
        results2 = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results2 != null;
        assert results2.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testUnscheduledResourceOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 10000L));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        ResourceOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getResource().getId() == resource.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        PageList<ResourceOperationScheduleComposite> list;
        list = operationManager.getCurrentlyScheduledResourceOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 1;
        assert list.get(0).getResourceId() == resource.getId();
        assert list.get(0).getResourceName().equals(resource.getName());
        assert list.get(0).getOperationName().equals("Test Operation");

        // let's immediately unschedule it before it triggers
        operationManager.unscheduleResourceOperation(superuser, returnedSchedule.getJobId().toString(),
            returnedSchedule.getResource().getId());

        list = operationManager.getCurrentlyScheduledResourceOperations(superuser, PageControl.getUnlimitedInstance());
        assert list.size() == 0;

        // history should never have existed - we unscheduled faster than its trigger
        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testUnscheduledResourceOperationWithParameters() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Configuration params = new Configuration();
        params.put(new PropertySimple("param1", "group-test"));
        params.put(new PropertySimple("param2", "blah"));

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 10000L));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", params, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() != null;
        assert schedule.getResource().getId() == newResource.getId();

        int configId = params.getId();
        Configuration returnedConfiguration = configurationManager.getConfigurationById(configId);
        assert returnedConfiguration.getProperties().size() == 2;
        assert returnedConfiguration.getSimple("param1").getStringValue().equals("group-test");
        assert returnedConfiguration.getSimple("param2").getStringValue().equals("blah");

        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        ResourceOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getResource().getId() == resource.getId();
        assert returnedSchedule.getParameters() != null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        // let's immediately unschedule it before it triggers
        operationManager.unscheduleResourceOperation(superuser, returnedSchedule.getJobId().toString(),
            returnedSchedule.getResource().getId());

        // history should never have existed - we unscheduled faster than its trigger
        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;

        // should be no dangling configuration entities representing group operation parameters
        Configuration returnedConfiguration2 = configurationManager.getConfigurationById(configId);
        assert returnedConfiguration2 == null;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetScheduledResourceOperations() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 5000L));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        ResourceOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getResource().getId() == resource.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        Thread.sleep(9000L); // wait for it to be triggered and complete

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());

        assert results != null : "Results were unexpectedly empty";
        if (results.isEmpty()) {
            System.out.println("We did not yet get a result -- waiting some more");
            Thread.sleep(5000L);
            results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
                PageControl.getUnlimitedInstance());
        }

        assert results != null : "Results were unexpectedly empty";
        assert !results.isEmpty() : "We did not get results back";

        operationManager.deleteOperationHistory(superuser, results.get(0).getId(), false);

        // make sure it was purged
        results = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;

        // see that it isn't scheduled anymore
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetScheduledResourceOperationsError() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = "some error";
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 5000L));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        ResourceOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getResource().getId() == resource.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        Thread.sleep(9000L); // wait for it to be triggered and complete

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());

        assert results != null;
        if (results.isEmpty()) {
            System.out.println("We did not yet get a result -- waiting some more");
            Thread.sleep(5000L);
            results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
                PageControl.getUnlimitedInstance());
        }
        assert results.size() == 1 : "Did not get 1 result, but " + results.size();

        ResourceOperationHistory history = results.get(0);
        assert history.getErrorMessage() != null : history;
        assert history.getErrorMessage().indexOf("some error") > -1 : history;
        assert history.getStatus() == OperationRequestStatus.FAILURE : history;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);

        // make sure it was purged
        results = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetScheduledResourceOperationsTimeout() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = true;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis() + 5000L));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        List<ResourceOperationSchedule> schedules;
        schedules = operationManager.getScheduledResourceOperations(superuser, resource.getId());
        assert schedules != null;
        assert schedules.size() == 1;
        ResourceOperationSchedule returnedSchedule = schedules.get(0);
        assert returnedSchedule.getSubject().equals(superuser);
        assert returnedSchedule.getResource().getId() == resource.getId();
        assert returnedSchedule.getParameters() == null;
        assert returnedSchedule.getOperationName().equals("testOp");
        assert returnedSchedule.getDescription().equals("desc");

        Thread.sleep(9000L); // wait for it to be triggered and complete

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        if (results.isEmpty()) {
            System.out.println("We did not yet get a result -- waiting some more");
            Thread.sleep(5000L);
            results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
                PageControl.getUnlimitedInstance());
        }
        assert results.size() == 1 : "Did not get 1 result but " + results.size();

        ResourceOperationHistory history = results.get(0);
        assert history.getErrorMessage() != null : history;
        assert history.getErrorMessage().indexOf("Timed out") > -1 : history;
        assert history.getStatus() == OperationRequestStatus.FAILURE : history;

        operationManager.deleteOperationHistory(superuser, history.getId(), false);

        // make sure it was purged
        results = operationManager.getCompletedResourceOperationHistories(superuser, newResource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0 : "Did not get 0 result but " + results.size();
    }

    @Test(enabled = ENABLE_TESTS)
    public void testCancelResourceOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 30000L; // long enough so it doesn't finish before we cancel
        simulatedOperation_CancelResults = new CancelResults(InterruptedState.RUNNING);

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        operationManager.scheduleResourceOperation(superuser, resource.getId(), "testOp", null, trigger, "desc");

        PageList<ResourceOperationHistory> results = null;

        // wait for it to be triggered so we get a history item
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            results = operationManager.getPendingResourceOperationHistories(superuser, resource.getId(), PageControl
                .getUnlimitedInstance());
            if ((results != null) && (results.size() > 0)) {
                break; // operation was triggered - got the history item
            }
        }

        assert results != null;
        assert results.size() == 1;
        ResourceOperationHistory history = results.get(0);
        assert history.getStatus() == OperationRequestStatus.INPROGRESS : history;

        operationManager.cancelOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        history = results.get(0);
        assert history.getStatus() == OperationRequestStatus.CANCELED : history;
        System.out.println("test: Canceled resource history: " + history);

        // try to cancel it again, just to make sure it blows up appropriately
        try {
            operationManager.cancelOperationHistory(superuser, history.getId(), false);
            assert false : "Should not have been able to cancel an operation that is not INPROGRESS";
        } catch (EJBException expected) {
            assert expected.getCause() instanceof IllegalStateException;
        }

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testCancelUncancelableResourceOperation() throws Exception {
        // this test will attempt to cancel an operation that has already finished, in effect
        // trying to cancel an uncancelable operation.  This simulates the situation when
        // an agent has finished running an operation but it has not yet sent the "success"
        // or "failed" message to the server.  So on the agent side it is finished, but the
        // server side still thinks its INPROGRESS.

        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 30000L; // long enough so it doesn't notify that it finished before we cancel
        simulatedOperation_CancelResults = new CancelResults(InterruptedState.FINISHED); // agent says its finished

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        operationManager.scheduleResourceOperation(superuser, resource.getId(), "testOp", null, trigger, "desc");

        PageList<ResourceOperationHistory> results = null;

        // wait for it to be triggered so we get a history item
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            results = operationManager.getPendingResourceOperationHistories(superuser, resource.getId(), PageControl
                .getUnlimitedInstance());
            if ((results != null) && (results.size() > 0)) {
                break; // operation was triggered - got the history item
            }
        }

        assert results != null;
        assert results.size() == 1;
        ResourceOperationHistory history = results.get(0);
        assert history.getStatus() == OperationRequestStatus.INPROGRESS : history;

        operationManager.cancelOperationHistory(superuser, history.getId(), false);

        // show that there are still no completed operations yet
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;

        // still pending - our operation wasn't really canceled - waiting for the agent to tell us its finished
        results = operationManager.getPendingResourceOperationHistories(superuser, resource.getId(), PageControl
            .getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        history = results.get(0);

        // we should have been told it finished on the agent - so we didn't cancel it
        // server-side still will say inprogress - we are waiting for the agent to tell us
        // the results of the finished operation, which should be imminent
        assert history.getStatus() == OperationRequestStatus.INPROGRESS : history;
        System.out.println("test: Uncancelable resource history: " + history);

        operationManager.deleteOperationHistory(superuser, history.getId(), true);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleResourceOperation() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        // make it a success after 500ms
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date(System.currentTimeMillis()));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        Thread.sleep(4000L); // wait for it to finish, should be very quick

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1;
        ResourceOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() == null : history;
        assert history.getStatus() == OperationRequestStatus.SUCCESS : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;

        PageList<ResourceOperationLastCompletedComposite> list;
        list = operationManager.getRecentlyCompletedResourceOperations(superuser, null, PageControl
            .getUnlimitedInstance());
        assert list.size() == 1;
        assert list.get(0).getOperationHistoryId() == history.getId();
        assert list.get(0).getResourceId() == resource.getId();
        assert list.get(0).getResourceName().equals(resource.getName());
        assert list.get(0).getOperationName().equals("Test Operation");

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;

        list = operationManager.getRecentlyCompletedResourceOperations(superuser, null, PageControl
            .getUnlimitedInstance());
        assert list.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleResourceOperationRecurring() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        // make it a success
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", 1, 750);
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", null, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() == null;
        assert schedule.getResource().getId() == newResource.getId();

        Thread.sleep(4000L); // wait for it to finish, should be very quick

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 2 : "Should have had multiple results: " + results;
        ResourceOperationHistory history0 = results.get(0);
        assert history0.getId() > 0 : history0;
        assert history0.getJobId() != null : history0;
        assert history0.getJobName() != null : history0;
        assert history0.getJobGroup() != null : history0;
        assert history0.getErrorMessage() == null : history0;
        assert history0.getStatus() == OperationRequestStatus.SUCCESS : history0;
        assert history0.getSubjectName().equals(superuser.getName()) : history0;

        ResourceOperationHistory history1 = results.get(1);
        assert history1.getId() > 0 : history1;
        assert history1.getId() != history0.getId() : history1;
        assert history1.getJobId() != null : history1;
        assert !history1.getJobId().equals(history0.getJobId()) : history1;
        assert history1.getJobName() != null : history1;
        assert history1.getJobName().equals(history1.getJobId().getJobName()) : history1;
        assert history1.getJobId().getJobName().equals(history0.getJobId().getJobName()) : history1;
        assert history1.getJobGroup() != null : history1;
        assert history1.getJobGroup().equals(history1.getJobId().getJobGroup()) : history1;
        assert history1.getJobGroup().equals(history0.getJobGroup()) : history1;
        assert history1.getJobId().getJobGroup().equals(history0.getJobId().getJobGroup()) : history1;
        assert history1.getErrorMessage() == null : history1;
        assert history1.getStatus() == OperationRequestStatus.SUCCESS : history1;
        assert history1.getSubjectName().equals(superuser.getName()) : history1;

        operationManager.deleteOperationHistory(superuser, history0.getId(), false);
        operationManager.deleteOperationHistory(superuser, history1.getId(), false);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testScheduleResourceOperationWithParameters() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        // make it a success after 500ms
        simulatedOperation_Error = null;
        simulatedOperation_Timeout = false;
        simulatedOperation_Sleep = 0L;

        Trigger trigger = new SimpleTrigger("tgrname", "tgrgroup", new Date());
        Configuration params = new Configuration();
        params.put(new PropertySimple("param1", "test-value!"));
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(superuser, resource.getId(),
            "testOp", params, trigger, "desc");
        assert schedule != null;
        assert schedule.getDescription().equals("desc");
        assert schedule.getOperationName().equals("testOp");
        assert schedule.getParameters() != null;
        assert schedule.getParameters().getId() > 0;
        assert schedule.getParameters().getSimple("param1") != null;
        assert schedule.getResource().getId() == newResource.getId();

        int scheduleParamId = schedule.getParameters().getId();

        Thread.sleep(4000L); // wait for it to finish, should be very quick

        PageList<ResourceOperationHistory> results;
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 1 : "size was " + results.size();
        ResourceOperationHistory history = results.get(0);
        assert history.getId() > 0 : history;
        assert history.getJobId() != null : history;
        assert history.getJobName() != null : history;
        assert history.getJobGroup() != null : history;
        assert history.getErrorMessage() == null : history;
        assert history.getStatus() == OperationRequestStatus.SUCCESS : history;
        assert history.getSubjectName().equals(superuser.getName()) : history;

        // parameters and results are lazily loaded in the paginated queries, but are eagerly individually
        history = (ResourceOperationHistory) operationManager
            .getOperationHistoryByHistoryId(superuser, history.getId());
        assert history.getResults() != null;
        assert history.getResults().getSimple("param1echo").getStringValue().equals("test-value!");
        assert history.getParameters().getId() != scheduleParamId : "params should be copies - not shared";

        operationManager.deleteOperationHistory(superuser, history.getId(), false);
        results = operationManager.getCompletedResourceOperationHistories(superuser, resource.getId(), null, null,
            PageControl.getUnlimitedInstance());
        assert results != null;
        assert results.size() == 0;
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetSupportedOperations() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Resource resource = newResource;

        assert operationManager.isResourceOperationSupported(superuser, resource.getId());
        assert operationManager.isGroupOperationSupported(superuser, newGroup.getId());

        OperationDefinition op;
        List<OperationDefinition> ops;

        // need to eager load the definition because .equals compares the resource type objects
        op = operationManager.getSupportedGroupOperation(superuser, newGroup.getId(), "testOp", true);
        assert op != null;
        assert op.getId() > 0;
        assert op.getName().equals("testOp");
        assert op.equals(newOperation);

        // need to eager load the definition because .equals compares the resource type objects
        ops = operationManager.getSupportedGroupOperations(superuser, newGroup.getId(), true);
        assert ops != null;
        assert ops.size() == 1;
        op = ops.iterator().next();
        assert op != null;
        assert op.getId() > 0;
        assert op.getName().equals("testOp");
        assert op.equals(newOperation);

        // need to eager load the definition because .equals compares the resource type objects
        op = operationManager.getSupportedResourceOperation(superuser, newResource.getId(), "testOp", true);
        assert op != null;
        assert op.getId() > 0;
        assert op.getName().equals("testOp");
        assert op.equals(newOperation);

        // need to eager load the definition because .equals compares the resource type objects
        ops = operationManager.getSupportedResourceOperations(superuser, newResource.getId(), true);
        assert ops != null;
        assert ops.size() == 1;
        op = ops.iterator().next();
        assert op != null;
        assert op.getId() > 0;
        assert op.getName().equals("testOp");
        assert op.equals(newOperation);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testNoPermissions() throws Exception {
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        Subject noPermSubject = new Subject("userWithNoPermissions", true, false);
        Resource resource = newResource;

        try {
            noPermSubject = LookupUtil.getSubjectManager().createSubject(superuser, noPermSubject);
            createSession(noPermSubject);

            assert !operationManager.isResourceOperationSupported(noPermSubject, resource.getId()) : "Should not have permission to get control info";
            assert !operationManager.isGroupOperationSupported(noPermSubject, newGroup.getId()) : "Should not have permission to get control info";

            try {
                Trigger trigger = new SimpleTrigger();
                operationManager
                    .scheduleResourceOperation(noPermSubject, resource.getId(), "testOp", null, trigger, "");
                assert false : "Should not have permission to schedule a new op";
            } catch (PermissionException expected) {
            }

            try {
                operationManager.getScheduledGroupOperations(noPermSubject, newGroup.getId());
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }

            try {
                operationManager.getScheduledResourceOperations(noPermSubject, newResource.getId());
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }

            try {
                // do not need to eager load just to test authorization
                operationManager.getSupportedGroupOperation(noPermSubject, newGroup.getId(), "testOp", false);
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }

            try {
                // do not need to eager load just to test authorization
                operationManager.getSupportedGroupOperations(noPermSubject, newGroup.getId(), false);
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }

            try {
                // do not need to eager load just to test authorization
                operationManager.getSupportedResourceOperation(noPermSubject, newResource.getId(), "testOp", false);
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }

            try {
                // do not need to eager load just to test authorization
                operationManager.getSupportedResourceOperations(noPermSubject, newResource.getId(), false);
                assert false : "Should not have permission to do this";
            } catch (PermissionException expected) {
            }
        } finally {
            LookupUtil.getSubjectManager().deleteUsers(LookupUtil.getSubjectManager().getOverlord(),
                new int[] { noPermSubject.getId() });
        }
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                OperationDefinition def = new OperationDefinition(resourceType, "testOp");
                def.setTimeout(10);
                def.setDisplayName("Test Operation");
                resourceType.addOperationDefinition(def);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setAgent(agent);
                em.persist(resource);

                ResourceGroup group = new ResourceGroup("testgroup" + System.currentTimeMillis(), resourceType);
                em.persist(group);
                group.addExplicitResource(resource);
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (null != resource) {
            EntityManager em = null;

            try {
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
                ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
                Subject overlord = LookupUtil.getSubjectManager().getOverlord();

                // first, get entity for group removal
                getTransactionManager().begin();
                em = getEntityManager();

                Resource res = em.find(Resource.class, resource.getId());
                ResourceGroup group = res.getExplicitGroups().iterator().next();

                getTransactionManager().commit();
                em.close();
                em = null;

                // then invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
                // perform in-band and out-of-band work in quick succession
                List<Integer> deletedIds = resourceManager.deleteResource(overlord, resource.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.deleteSingleResourceInNewTransaction(overlord, deletedResourceId);
                }

                // then kill the group via the RG manager, it also handles cleanup of non hibernate model info
                resourceGroupManager.deleteResourceGroup(overlord, group.getId());

                // now dispose of other hibernate entities
                getTransactionManager().begin();
                em = getEntityManager();

                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Agent agent = em.find(Agent.class, resource.getAgent().getId());
                em.remove(agent);
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                if (null != em) {
                    em.close();
                }
            }
        }
    }

    private class TestConfigService implements OperationAgentService {
        public void invokeOperation(final String jobId, final int resourceId, final String operationName,
            final Configuration paramConfig) throws PluginContainerException {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("~~~~~OPERATION TRIGGERED! op=" + operationName + ", jobId=" + jobId
                            + ", time=" + new Date());

                        long start = System.currentTimeMillis();

                        // this method simulates the agent actually invoking the operation
                        if (simulatedOperation_Sleep > 0L) {
                            Thread.sleep(simulatedOperation_Sleep);
                        }

                        long end = System.currentTimeMillis();

                        if (simulatedOperation_Error != null) {
                            ExceptionPackage error = new ExceptionPackage(new Exception(simulatedOperation_Error));
                            if (operationServerService != null) {
                                operationServerService.operationFailed(jobId, null, error, start, end);
                            }
                        } else if (simulatedOperation_Timeout) {
                            if (operationServerService != null) {
                                operationServerService.operationTimedOut(jobId, start, end);
                            }
                        } else {
                            Configuration results = null;
                            if ((paramConfig != null) && (paramConfig.getSimple("param1") != null)) {
                                results = new Configuration();
                                results.put(new PropertySimple("param1echo", paramConfig.getSimple("param1")
                                    .getStringValue()));
                            }

                            if (operationServerService != null) {
                                operationServerService.operationSucceeded(jobId, results, start, end);
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException(t);
                    }
                }
            };

            thread.start();
        }

        public CancelResults cancelOperation(String jobId) {
            return simulatedOperation_CancelResults;
        }
    }
}