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

package org.rhq.core.pc.operation;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ExceptionPackage;

@Test
public class ParallelOperationsTest {
    private static final String JOB_1_ID = "job1"; // the job ID for the job run on resource #1
    private static final String JOB_2_ID = "job2"; // the job ID for the job run on resource #2
    private static final int RESOURCE_1_ID = 1;
    private static final int RESOURCE_2_ID = 2;
    private static final long OPERATION_TIMEOUT_MILLIS = 5000L;

    // keyed on job Id
    private Map<String, Object> opResults; // key=job ID, value=results object
    private Map<String, Long> jobsCompleted; // key=job ID, value=time it completed
    private OperationManager manager;
    private PluginContainerConfiguration pcConfig;

    @BeforeMethod
    public void beforeMethod() {
        opResults = new Hashtable<String, Object>(); // need synchronized hashtable
        jobsCompleted = Collections.synchronizedMap(new TreeMap<String, Long>());

        ServerServices serverServices = new ServerServices();
        serverServices.setOperationServerService(new MockOperationServerService());

        pcConfig = new PluginContainerConfiguration();
        pcConfig.setOperationInvocationTimeout(OPERATION_TIMEOUT_MILLIS / 1000L);
        pcConfig.setServerServices(serverServices);

        manager = new MockOperationManager(pcConfig);
        println("Starting new test method...");
    }

    @AfterMethod
    public void afterMethod() {
        opResults = null;
        jobsCompleted = null;
        manager.shutdown();
        pcConfig = null;
        println("Test method ended.");
    }

    public void testParallelOperations() throws Exception {
        println("testParallelOperations - START");

        // invoke job 1 - which in turn invokes job 1 (initialOp calls secondOp)
        // but job 1 will invoke on resource 1 and job 2 will invoke on resource 2 - they should be able to run concurrently
        manager.invokeOperation(JOB_1_ID, RESOURCE_1_ID, "initialOp", new Configuration());

        // let's wait for both jobs to finish - they should finish really fast and not timeout
        waitForAgentResponse(OPERATION_TIMEOUT_MILLIS, JOB_1_ID);
        waitForAgentResponse(OPERATION_TIMEOUT_MILLIS, JOB_2_ID);

        Object results = opResults.get(JOB_1_ID);
        assert results != null : "assertJob1Result1: " + opResults;
        assert results instanceof Configuration : "assertJob1Result2: " + opResults;
        assert ((Configuration) results).getSimple("initialOpResults").getStringValue().equals("initialOpSuccess") : "assertJob1Result3: "
            + opResults;

        results = opResults.get(JOB_2_ID);
        assert results != null : "assertJob2Result1: " + opResults;
        assert results instanceof Configuration : "assertJob2Result2: " + opResults;
        assert ((Configuration) results).getSimple("secondOpResults").getStringValue().equals("secondOpSuccess") : "assertJob2Result3: "
            + opResults;

        printJobsCompleted();

        println("testParallelOperations - STOP");
    }

    public void testOperationsOnSameResource() throws Exception {
        println("testOperationsOnSameResource - START");

        // invoke job 1 - which in turn invokes job 1 (initialOpSameResource calls secondOpSameResource)
        // where job 1 and job 2 will invoke on same resource 1. They should not run at the same time.
        // Since job 1 waits for job 2 (and since job 2 can't run until job 1 finished), there is a deadlock
        // condition and job 1 will eventually timeout.
        manager.invokeOperation(JOB_1_ID, RESOURCE_1_ID, "initialOpSameResource", new Configuration());

        // let's wait job 1 and confirm it does timeout (it times out due to being deadlocked)
        waitForAgentResponse(OPERATION_TIMEOUT_MILLIS + 1000L, JOB_1_ID);
        Object results = opResults.get(JOB_1_ID);
        assert results != null : "odd - job 1 should have timed out - it should not still be actively running";
        assert results.toString().equals("TIMEOUT") : "job 1 should have timed out after being deadlocked: " + results;

        printJobsCompleted();

        println("testOperationsOnSameResource - STOP");
    }

    private void printJobsCompleted() {
        if (jobsCompleted == null || jobsCompleted.isEmpty()) {
            println("jobsCompleted is null/empty");
        } else {
            synchronized (jobsCompleted) {
                for (String op : jobsCompleted.keySet()) {
                    println(op + " completed at " + formatTime(jobsCompleted.get(op)));
                }
            }
        }
    }

    private void waitForAgentResponse(long sleepMillis, String jobId) throws InterruptedException {
        // keep checking to see if it finished
        long now = System.currentTimeMillis();
        long timeout = now + sleepMillis;

        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(1000);
            if (opResults.containsKey(jobId)) {
                break;
            }
        }
    }

    private class MockOperationManager extends OperationManager {

        public MockOperationManager(PluginContainerConfiguration configuration) {
            super(configuration, null);
        }

        @Override
        protected OperationFacet getOperationFacet(int resourceId, long facetMethodTimeout)
            throws PluginContainerException {
            return new MockOperationFacet();
        }

        @Override
        protected ResourceType getResourceType(int resourceId) {
            return new ResourceType("testType", "testPlugin", ResourceCategory.PLATFORM, null);
        }
    }

    private class MockOperationFacet implements OperationFacet {
        public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
            OperationResult opConfig = new OperationResult();
            if (name.equals("initialOp")) {
                manager.invokeOperation(JOB_2_ID, RESOURCE_2_ID, "secondOp", new Configuration()); // call back into OperationManager on different resource
                // The above call to secondOp should be asynchronous and be able to run concurrently with us (because
                // we are invoking it on resource 2, and we are running on resource 1).
                // If it cannot complete within a few seconds, we have deadlocked.
                // That would mean resource 1 is delaying a resource 2 op - which would be a bug.
                // Different resources should not delay operations on other resources.
                waitForAgentResponse(OPERATION_TIMEOUT_MILLIS, JOB_2_ID);
                Object results = opResults.get(JOB_2_ID);
                assert results != null : "our called job 2 did not finish";
                assert results instanceof Configuration : "our called job 2 did not finish as expected: " + opResults;
                assert ((Configuration) results).getSimple("secondOpResults").getStringValue()
                    .equals("secondOpSuccess") : "our called job 2 did not finish as expected: " + opResults;

                opConfig.getComplexResults().put(new PropertySimple("initialOpResults", "initialOpSuccess"));
            } else if (name.equals("secondOp")) {
                opConfig.getComplexResults().put(new PropertySimple("secondOpResults", "secondOpSuccess"));
            } else if (name.equals("initialOpSameResource")) {
                manager.invokeOperation(JOB_2_ID, RESOURCE_1_ID, "secondOpSameResource", new Configuration()); // call back into OperationManager on same resource #1
                // The above call to secondOp should not be able to run concurrently with us (because
                // we are invoking it on resource 1, which is the same resource we are running on).
                // This should deadlock - job 2 should never finish because it should never start (at least
                // until we finish first).
                // Therefore, we are going to wait for an agent response to job 2 that will never come.
                // This will in turn cause our job to timeout.
                waitForAgentResponse(OPERATION_TIMEOUT_MILLIS + 1000L, JOB_2_ID); // wait longer than the operation timeout
            } else if (name.equals("secondOpSameResource")) {
                opConfig.getComplexResults().put(
                    new PropertySimple("secondOpSameResourceResults", "secondOpSameResourceSuccess"));
            } else {
                throw new IllegalStateException("BAD TEST - unknown operation: " + name);
            }

            return opConfig;
        }
    }

    private class MockOperationServerService implements OperationServerService {
        public void operationFailed(String jobId, Configuration result, ExceptionPackage error, long invocationTime,
            long completionTime) {
            //println( "~~~~~~~~~~~~~~ server service - failed: " + jobId + " : " + formatTime( completionTime ) );
            jobsCompleted.put(jobId, completionTime);
            opResults.put(jobId, error);
        }

        public void operationSucceeded(String jobId, Configuration result, long invocationTime, long completionTime) {
            //println( "~~~~~~~~~~~~~~ server service - success: " + jobId + " : " + formatTime( completionTime ) );
            jobsCompleted.put(jobId, completionTime);
            opResults.put(jobId, result);
        }

        public void operationTimedOut(String jobId, long invocationTime, long timeoutTime) {
            //println( "~~~~~~~~~~~~~~ server service - timed out: " + jobId + " : " + formatTime( timeoutTime ) );
            jobsCompleted.put(jobId, timeoutTime);
            opResults.put(jobId, "TIMEOUT");
        }

        public void operationCanceled(String jobId, Configuration result, ExceptionPackage error, long invocationTime,
            long canceledTime) {
            //println( "~~~~~~~~~~~~~~ server service - timed out: " + jobId + " : " + formatTime( timeoutTime ) );
            jobsCompleted.put(jobId, canceledTime);
            opResults.put(jobId, "CANCELED");
        }
    }

    private static void println(String msg) {
        System.out.println(formatTime(new Date().getTime()) + ": " + msg);
    }

    private static String formatTime(long date) {
        return new SimpleDateFormat("HH:mm:ss.S").format(new Date(date));
    }
}
