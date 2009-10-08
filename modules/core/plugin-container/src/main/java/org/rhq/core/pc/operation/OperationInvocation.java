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

import java.util.EnumSet;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;

/**
 * The runnable that is actually responsible for invoking an operation on a plugin's {@link OperationFacet}.
 *
 * @author John Mazzitelli
 */
public class OperationInvocation implements Runnable {
    private final Log log = LogFactory.getLog(OperationInvocation.class);

    /**
     * Indicates the current status of this invocation (QUEUED, RUNNING or FINISHED). A invocation may have additional
     * indicators: CANCELED (if it was told to stop) and TIMED_OUT (if it was told to stop due to a time-out).
     */
    public enum Status {
        QUEUED, RUNNING, FINISHED, CANCELED, TIMED_OUT
    }

    private final int resourceId;
    private final long invocationTime;
    private final TimerTask timerTask;
    private final Configuration parameterConfig;
    private final String jobId;
    private final String operationName;
    private final OperationFacet operationComponent;
    private final OperationServerService operationServerService;
    private final OperationThreadPoolGateway operationThreadPoolGateway;
    private final OperationDefinition operationDefinition;

    private final EnumSet<Status> status;
    private Thread operationThread;

    public OperationInvocation(int resourceId, long invocationTime, TimerTask timerTask, Configuration parameterConfig,
        String jobId, String operationName, OperationFacet operationComponent,
        OperationServerService operationServerService, OperationThreadPoolGateway operationThreadPoolGateway,
        OperationDefinition operationDefinition) {
        this.resourceId = resourceId;
        this.invocationTime = invocationTime;
        this.timerTask = timerTask;
        this.parameterConfig = parameterConfig;
        this.jobId = jobId;
        this.operationName = operationName;
        this.operationComponent = operationComponent;
        this.operationServerService = operationServerService;
        this.operationThreadPoolGateway = operationThreadPoolGateway;
        this.operationDefinition = operationDefinition;

        this.status = EnumSet.of(Status.QUEUED);
        this.operationThread = null; // will be non-null when in running state
    }

    /**
     * Identifies the resource that this invocation will operate on.
     *
     * @return the resource's ID
     */
    public int getResourceId() {
        return resourceId;
    }

    /**
     * Returns the job ID that identifies this specific operation invocation.
     *
     * @return unique job identification string
     */
    public String getJobId() {
        return jobId;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("OperationInvocation: ");

        str.append("resource-id=[" + resourceId);
        str.append("], job-id=[" + jobId);
        str.append("], op-name=[" + operationName);
        str.append("], status=[" + getStatus());
        str.append("]");

        return str.toString();
    }

    public EnumSet<Status> getStatus() {
        synchronized (status) {
            return EnumSet.copyOf(status);
        }
    }

    /**
     * Flags this operation as being canceled. This will also interrupt the thread running the operation, if it is in
     * the running state. Note that if the operation has already completed, this method does nothing since it is too
     * late to cancel the operation.
     *
     * @return the current state of the operation when it was canceled
     */
    public EnumSet<Status> markAsCanceled() {
        synchronized (status) {
            EnumSet<Status> interruptedStatus = EnumSet.copyOf(status);

            if (!status.contains(Status.FINISHED)) {
                status.add(Status.CANCELED);

                if (operationThread != null) {
                    operationThread.interrupt();
                }
            }

            return interruptedStatus;
        }
    }

    /**
     * Flags this operation as being canceled due to a time out. This will also interrupt the thread running the
     * operation, if it is in the running state. Note that if the operation has already completed, this method does
     * nothing since it is too late to time out the operation.
     */
    public void markAsTimedOut() {
        synchronized (status) {
            if (!status.contains(Status.FINISHED)) {
                status.add(Status.TIMED_OUT);
                markAsCanceled();
            }
        }
    }

    /**
     * Flags this operation as running within the given thread. If this operation was prematurely timed out or canceled,
     * this will return <code>false</code> indicating to the caller that the operation should abort and not run. <code>
     * true</code> is returned if the operation can move forward and begin to execute.
     *
     * @param  thread the thread in which this operation is running in
     *
     * @return <code>false</code> if the operation was prematurely canceled/timed out; <code>true</code> if the
     *         operation can continue to execute
     */
    private boolean markAsRunning(Thread thread) {
        synchronized (status) {
            operationThread = thread;
            status.remove(Status.QUEUED);
            status.add(Status.RUNNING);

            return !status.contains(Status.CANCELED);
        }
    }

    /**
     * Flags this operation as being finished (it either completed normally or it was canceled or it timed out).
     */
    private void markAsFinished() {
        synchronized (status) {
            operationThread = null;
            status.remove(Status.QUEUED);
            status.remove(Status.RUNNING);
            status.add(Status.FINISHED);
        }
    }

    /**
     * This actually invokes the plugin's operation facet and executes the operation. If it does not finish before the
     * timeout expires, the timer task will {@link #markAsTimedOut()} interrupt this thread}. This thread will also be
     * interrupted if the operation was {@link #markAsCanceled() canceled}.
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        Configuration result = null;
        String errorMessage = null;
        Throwable failure = null;
        long finishedTime;

        try {
            boolean canContinue = markAsRunning(Thread.currentThread());

            // We are at the point of no return now. If someone tries to cancel us, its up to
            // the plugin writer of the operation facet to handle the interrupt exception, if applicable.

            if (canContinue) {
                // call the plugin component's operation facet to actually execute the operation
                Configuration parameters = (parameterConfig != null) ? parameterConfig : new Configuration();
                OperationResult opResult = operationComponent.invokeOperation(operationName, parameters);

                // allow a plugin to return a null (aka void) result set
                result = (opResult != null) ? opResult.getComplexResults() : null;
                if (result != null) {
                    if (this.operationDefinition != null) {
                        if (this.operationDefinition.getResultsConfigurationDefinition() != null) {
                            // Normalize the result Configuration.
                            ConfigurationUtility.normalizeConfiguration(result, operationDefinition
                                .getResultsConfigurationDefinition());
                            // TODO: Validate the result Configuration?
                        } else if (!result.getProperties().isEmpty()) {
                            log.error("Plugin error: Operation [" + this.operationDefinition.getName()
                                + "] is defined as returning no results, but it returned non-null results: "
                                + result.toString(true));
                            result = null; // Don't return results that the GUI won't be able to display anyway.
                        }
                    }
                    errorMessage = opResult.getErrorMessage();
                }
            } else {
                failure = new InterruptedException("Operation was aborted before it started.");
            }
        } catch (Throwable t) {
            failure = t;
        } finally {
            // IT IS IMPORTANT THAT THIS FINALLY CLAUSE NOT THROW EXCEPTIONS.  EVERYTHING IN HERE
            // MUST EXECUTE PROPERLY OR ELSE THE OPERATION MANAGER WILL NOT EXECUTE OPERATIONS PROPERLY

            // Note: if the timer triggers in the nanoseconds between the above and our mark here
            // then the timer task will erroneously cause us to send out a timeout message to the server. I know of no
            // way to prevent this right now.  This will be a rare occurrence, but not an impossibility.
            markAsFinished();
            timerTask.cancel();

            // remember the time we finished - to ensure our times are in order,
            // be sure we mark this time before we allow the gateway to submit the next operation to the thread pool
            finishedTime = System.currentTimeMillis();

            // before taking the time to send the request up to the server, let's allow the next
            // operation in the queue to get executed right now.  Make sure this always is called,
            // otherwise, no other operations on the resource will be allowed
            operationThreadPoolGateway.operationCompleted(this);
        }

        // if we have a server that we need to tell, notify it of the results/failure/cancellation/timeout
        if (operationServerService != null) {
            if (failure == null) {
                // Note that even if the operation was canceled and/or timed out, we may still get here.
                // This happens if either the plugin quickly finished before we received the order to cancel
                // or the plugin ignored the order to cancel (i.e. the thread interrupt) and finished doing
                // what it was doing anyway.  In either case, the operation really did succeed (it was not
                // canceled) so we need to indicate this via calling operationSucceeded
                if (errorMessage == null) {
                    try {
                        operationServerService.operationSucceeded(jobId, result, invocationTime, finishedTime);
                    } catch (Throwable t) {
                        log.error("Failed to send operation succeeded message to server. resource=[" + resourceId
                            + "], operation=[" + operationName + "], jobId=[" + jobId + "]", t);
                    }
                } else {
                    ExceptionPackage errorResults = new ExceptionPackage(Severity.Severe, new Exception(errorMessage));
                    try {
                        operationServerService.operationFailed(jobId, result, errorResults, invocationTime,
                            finishedTime);
                    } catch (Throwable t) {
                        log.error("Failed to send operation failed message to server. resource=[" + resourceId
                            + "], operation=[" + operationName + "], jobId=[" + jobId + "]", t);
                    }
                }
            } else {
                if (status.contains(Status.TIMED_OUT)) {
                    try {
                        operationServerService.operationTimedOut(jobId, invocationTime, finishedTime);
                    } catch (Throwable t) {
                        log.error("Failed to send operation timed out message to server. resource=[" + resourceId
                            + "], operation=[" + operationName + "], jobId=[" + jobId + "]", t);
                    }
                } else {
                    ExceptionPackage errorResults;

                    if (status.contains(Status.CANCELED)) {
                        errorResults = new ExceptionPackage(Severity.Info, new Exception("Canceled", failure));
                    } else {
                        errorResults = new ExceptionPackage(Severity.Severe, failure);
                    }

                    try {
                        operationServerService.operationFailed(jobId, null, errorResults, invocationTime, finishedTime);
                    } catch (Throwable t) {
                        log.error("Failed to send operation failed message to server. resource=[" + resourceId
                            + "], operation=[" + operationName + "], jobId=[" + jobId + "], operation-error=["
                            + errorResults.toString() + "]", t);
                    }
                }
            }
        }

        return;
    }
}