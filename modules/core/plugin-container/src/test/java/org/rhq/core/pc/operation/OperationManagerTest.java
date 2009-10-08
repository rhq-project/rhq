/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.operation;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ExceptionPackage;

@Test
public class OperationManagerTest {
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
        pcConfig.setOperationInvocationTimeout(5);
        pcConfig.setServerServices(serverServices);

        manager = new MockOperationManager();
        manager.setConfiguration(pcConfig);
        manager.initialize();
    }

    @AfterMethod
    public void afterMethod() {
        opResults = null;
        jobsCompleted = null;
        manager.shutdown();
        pcConfig = null;
    }

    public void testOperationSuccess() throws Exception {
        println("testOperationSuccess - START");
        manager.invokeOperation("success", 0, "opSuccess", null);
        waitForAgentResponse(1000, "success");
        assertSuccessResult("success");
        println("testOperationSuccess - STOP");
    }

    public void testOperationFailure() throws Exception {
        println("testOperationFailure - START");
        manager.invokeOperation("failure", 0, "opFailure", null);
        waitForAgentResponse(1000, "failure");
        assertFailureResult("failure");
        println("testOperationFailure - STOP");
    }

    public void testOperationTimeout() throws Exception {
        println("testOperationTimeout - START");
        manager.invokeOperation("timeout", 0, "opTimeout", null);
        waitForAgentResponse(6000, "timeout");
        assertTimeoutResult("timeout");
        println("testOperationTimeout - STOP");
    }

    public void testOperationTimeoutOverride() throws Exception {
        println("testOperationTimeoutOverride - START");
        Configuration config = new Configuration();
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "1")); // faster than default
        manager.invokeOperation("timeout", 0, "opTimeout", config);
        waitForAgentResponse(10000, "timeout"); // even though the facet will sleep for longer (5s), we force it to die quicker
        assertTimeoutResult("timeout");
        println("testOperationTimeoutOverride - STOP");
    }

    public void testBulkOperationsSuccess() throws Exception {
        println("testBulkOperationsSuccess - START");

        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();
        final Configuration config = new Configuration();

        // spawning alot of threads - might take long time to run each op on a slow laptop
        // so allow it more time to complete
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "60"));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "success" + index;
                        manager.invokeOperation(jobId, index, "opSuccess", config); // resource ID is the unique index
                        waitForAgentResponse(60000, jobId);
                        assertSuccessResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute successful operations: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkOperationsSuccess - STOP");
    }

    public void testBulkSerializedOperationsSuccess() throws Exception {
        println("testBulkSerializedOperationsSuccess - START");

        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();
        final Configuration config = new Configuration();

        // spawning alot of threads - might take long time to run each op on a slow laptop
        // so allow it more time to complete
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "60"));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "success" + index;
                        manager.invokeOperation(jobId, index % 2, "opSuccess", config); // resource ID is either 0 or 1
                        waitForAgentResponse(60000, jobId);
                        assertSuccessResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute successful operations: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkSerializedOperationsSuccess - STOP");
    }

    public void testBulkSerializedOperationsSuccess2() throws Exception {
        // this test actually tests that we do order the the invocations properly (serialized across a resource)
        println("testBulkSerializedOperationsSuccess2 - START");

        final int bulkSize = 200; // must be an even number
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Long> done0 = new Vector<Long>();
        final List<Long> done1 = new Vector<Long>();
        final Configuration config = new Configuration();

        // spawning alot of threads - might take long time to run each op on a slow laptop
        // so allow it more time to complete
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "60"));

        // do not spawn extra threads like our other tests, we want to submit operations so they are invoked in strict order
        for (int i = 0; i < bulkSize; i++) {
            String jobId = String.format("%04d", i); // so jobsCompleted can sort on the jobId properly
            manager.invokeOperation(jobId, i % 2, "opSuccess", config); // resource ID is either 0 or 1
        }

        // give all the threads time to finish
        Thread.sleep(5000);

        assert errors.size() == 0 : "Failed to bulk execute successful operations: " + errors;
        assert jobsCompleted.size() == bulkSize : "Not enough threads finished: " + jobsCompleted.size()
            + " should be " + bulkSize;

        // resource 0 had even job IDs, resource 1 had odd job IDs - each resource should have ordered results
        for (Map.Entry<String, Long> entry : jobsCompleted.entrySet()) {
            String jobIdNumber = entry.getKey();
            Integer i = Integer.parseInt(jobIdNumber);
            if ((i.intValue() % 2) == 0) {
                done0.add(entry.getValue());
            } else {
                done1.add(entry.getValue());
            }
        }

        assert done0.size() == (bulkSize / 2) : "Not enough done0 threads: " + done0.size() + " should be "
            + (bulkSize / 2);
        assert done1.size() == (bulkSize / 2) : "Not enough done1 threads: " + done1.size() + " should be "
            + (bulkSize / 2);

        // our operation gateway will ensure that we never concurrently run the operations;
        // which also means they will always run in the same order that we submitted them
        long previous0 = 0;
        long previous1 = 0;
        for (int i = 0; i < (bulkSize / 2); i++) {
            // the previous job must always have completed in the past, compared to the current one
            // we subtract 100 from the times because System.currentTimeMillis() javadoc says it isn't precise
            assert (previous0 - 100) <= done0.get(i).longValue() : "jobs executed out of order somehow: #" + i
                + "; previous=" + previous0 + "; after=" + done0.get(i) + "; previousTime=" + formatTime(previous0)
                + "; afterTime=" + formatTime(done0.get(i)) + ": " + done0;
            previous0 = done0.get(i);

            assert (previous1 - 100) <= done1.get(i).longValue() : "jobs executed out of order somehow: #" + i
                + "; previous=" + previous1 + "; after=" + done1.get(i) + ": " + "; previousTime="
                + formatTime(previous1) + "; afterTime=" + formatTime(done1.get(i)) + done1;
            previous1 = done1.get(i);
        }

        println("testBulkSerializedOperationsSuccess2 - STOP");
    }

    public void testBulkOperationsFailure() throws Exception {
        println("testBulkOperationsFailure - START");

        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();
        final Configuration config = new Configuration();

        // spawning alot of threads - might take long time to run each op on a slow laptop
        // so allow it more time to complete
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "60"));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "failure" + index;
                        manager.invokeOperation(jobId, index, "opFailure", config);
                        waitForAgentResponse(60000, jobId);
                        assertFailureResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute operations with failures: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkOperationsFailure - STOP");
    }

    public void testBulkSerializedOperationsFailure() throws Exception {
        println("testBulkSerializedOperationsFailure - START");

        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();
        final Configuration config = new Configuration();

        // spawning alot of threads - might take long time to run each op on a slow laptop
        // so allow it more time to complete
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "60"));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "failure" + index;
                        manager.invokeOperation(jobId, index % 2, "opFailure", config); // resource ID is either 0 or 1
                        waitForAgentResponse(60000, jobId);
                        assertFailureResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute operations with failures: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkSerializedOperationsFailure - STOP");
    }

    public void testBulkOperationsTimeout() throws Exception {
        println("testBulkOperationsTimeout - START");
        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "timeout" + index;
                        manager.invokeOperation(jobId, index, "opTimeout", null);
                        waitForAgentResponse(60000, jobId);
                        assertTimeoutResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute operations with timeouts: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkOperationsTimeout - STOP");
    }

    public void testBulkSerializedOperationsTimeout() throws Exception {
        println("testBulkSerializedOperationsTimeout - START");
        final int bulkSize = 200;
        final Thread[] threads = new Thread[bulkSize];
        final List<Throwable> errors = new Vector<Throwable>();
        final List<Integer> done = new Vector<Integer>();

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int index = Integer.parseInt(Thread.currentThread().getName());

                    try {
                        String jobId = "timeout" + index;
                        manager.invokeOperation(jobId, index % 2, "opTimeout", null); // resource ID is either 0 or 1
                        waitForAgentResponse(60000, jobId);
                        assertTimeoutResult(jobId);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.add(index);
                    }
                }
            }, "" + i); // name of thread is the index number
            threads[i].start();
        }

        // wait for the tests to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(60000);
        }

        assert errors.size() == 0 : "Failed to bulk execute operations with timeouts: " + errors;
        assert done.size() == bulkSize : "Not enough threads finished: " + done.size() + " should be " + bulkSize;

        Collections.sort(done);
        for (int i = 0; i < bulkSize; i++) {
            assert done.get(i) == i : done;
        }

        println("testBulkSerializedOperationsTimeout - STOP");
    }

    public void testBulkOperationsCancelOnShutdown() throws Exception {
        println("testBulkOperationsCancelOnShutdown - START");
        final int bulkSize = 200;
        final Configuration config = new Configuration();

        // because op name is opTimeout, our mock will force each op to take 1 second longer than this timeout
        // give it some really high timeout - it doesn't matter, shutdown will kill them all quickly
        config.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, "123456"));

        for (int i = 0; i < bulkSize; i++) {
            String jobId = "timeout" + i;
            manager.invokeOperation(jobId, i, "opTimeout", config); // unique resource IDs
        }

        // hurry up and shutdown - this will cancel those in the threadpool and those queued in the gateway
        manager.shutdown();

        Thread.sleep(5000); // give a bit more time for all operations to cancel themselves

        Set<String> jobIds = opResults.keySet();
        assert jobIds.size() == bulkSize : "Did not cancel all of them: " + jobIds.size() + " should be " + bulkSize;

        for (int i = 0; i < bulkSize; i++) {
            assert jobIds.contains("timeout" + i);
        }

        println("testBulkOperationsCancelOnShutdown - STOP");
    }

    private void assertSuccessResult(String jobId) {
        Object results = opResults.get(jobId);
        assert results != null : "assertSuccessResult1: " + opResults;
        assert results instanceof Configuration : "assertSuccessResult2: " + opResults;
        assert ((Configuration) results).getSimple("successName").getStringValue().equals("successValue") : "assertSuccessResult3: "
            + opResults;
    }

    private void assertFailureResult(String jobId) {
        Object results = opResults.get(jobId);
        assert results != null : "assertFailureResult1: " + opResults;
        assert results instanceof ExceptionPackage : "assertFailureResult2: " + opResults;
        ExceptionPackage error = ((ExceptionPackage) results);
        assert error.getExceptionName().equals(IllegalStateException.class.getName()) : "assertFailureResult3: "
            + opResults;
        assert error.getMessage().equals("Simulates an operation failure") : "assertFailureResult4: " + opResults;
        assert error.getStackTraceString() != null : "assertFailureResult5: " + opResults;
    }

    private void assertTimeoutResult(String jobId) {
        Object results = opResults.get(jobId);
        assert results != null : "assertTimeoutResult1: " + opResults;
        assert results instanceof String : "assertTimeoutResult2: " + opResults;
        assert ((String) results).equals("TIMEOUT") : "assertTimeoutResult3: " + opResults;
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
            if (name.equals("opFailure")) {
                throw new IllegalStateException("Simulates an operation failure");
            } else if (name.equals("opSuccess")) {
                OperationResult config = new OperationResult();
                config.getComplexResults().put(new PropertySimple("successName", "successValue"));
                return config;
            } else if (name.equals("opTimeout")) {
                try {
                    long sleepTime;

                    if ((parameters != null) && (parameters.getSimple(OperationDefinition.TIMEOUT_PARAM_NAME) != null)) {
                        sleepTime = parameters.getSimple(OperationDefinition.TIMEOUT_PARAM_NAME).getLongValue();
                    } else {
                        sleepTime = pcConfig.getOperationInvocationTimeout();
                    }

                    sleepTime = (sleepTime + 1) * 1000L; // convert to millis and go 1 second past the timeout so we get iterrupted/timed out

                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //println( "~~~~~~~~~~~~~~ facet operation invocation was interrupted" );
                    throw e;
                }

                return null;
            } else {
                throw new IllegalStateException("BAD TEST - unknown operation: " + name);
            }
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
    }

    private static void println(String msg) {
        System.out.println(formatTime(new Date().getTime()) + ": " + msg);
    }

    private static String formatTime(long date) {
        return new SimpleDateFormat("HH:mm:ss.S").format(new Date(date));
    }
}