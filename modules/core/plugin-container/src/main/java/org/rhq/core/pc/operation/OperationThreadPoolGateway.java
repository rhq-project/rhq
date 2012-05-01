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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.operation.OperationInvocation.Status;

/**
 * This provides a queue-like structure that will submit operations to a thread pool for execution, or will store the
 * operations until a time when they are allowed to be invoked. This class is used to enforce the rule that no two
 * operations that are to be invoked on the same resource can run concurrently. This gateway will pass through all
 * operations to its internal thread pool, unless an operation is already queued or running in the thread pool that is
 * executing on a resource that the newly submitted operation needs to execute on. In this case, the second operation
 * will be queued in this gateway until such time when the first operation is finished with the resource.
 *
 * <p>An analogy of this is shopping in a department store. Assume for this example a store has two departments - a
 * jewelry department and a shoe department. There is one sales person per department. Two shoppers can concurrently buy
 * different types of products - one shopper can be buying from the sales person in the jewelry department while, at the
 * exact same time, the other shopper can be buying from the sales person in the shoe department. However, if both
 * shoppers want to buy something from the shoe department, the second one has to wait until the first is finished with
 * the shoe department's sales person. They cannot both buy something in the shoe department at the same time. In this
 * analogy, a resource is a sales person, and a shopper's purchasing interaction is an operation invocation.</p>
 *
 * @author John Mazzitelli
 */
public class OperationThreadPoolGateway {
    private final Log log = LogFactory.getLog(OperationThreadPoolGateway.class);

    /**
     * Keyed on resource IDs, this contains the list of all queued up operation invocations. If an operation is
     * submitted to the gateway object, but that operation needs to be invoked on a resource that already has an
     * operation submitted to the thread pool, that second operation will be temporarily queued in this map. Once that
     * first operation completes, the second operation queued here will be moved from this queue into the thread pool's
     * queue.
     *
     * <p>This object is also used for its monitor lock to perform things atomically.</p>
     */
    private final Map<Integer, LinkedList<OperationInvocation>> resourceQueues;

    /**
     * Contains all operation invocations currently queued or running, keyed on their job ID.
     */
    private final Map<String, OperationInvocation> allOperations;

    /**
     * This is where the operation invocations are submitted when they are allowed to be invoked. This thread pool will
     * be allowed to concurrently execute any operation submitted to it. The gateway object will ensure no two
     * operations that are to be invoked on the same resource will be submitted to this thread pool.
     */
    private final ThreadPoolExecutor threadPool;

    /**
     * When <code>true</code>, this gateway has been {@link #shutdown()} and will no longer accept operation
     * submissions. Once a gateway is stopped, it is useless and must be discarded. Must synchronize on <code>
     * resourceQueues</code> when you want to read or write to this boolean.
     */
    private boolean stopped;

    /**
     * Constructor for {@link OperationThreadPoolGateway}. When an {@link OperationInvocation} passes through this
     * gateway, it will be submitted for execution in the given thread pool.
     *
     * @param threadPool
     */
    public OperationThreadPoolGateway(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
        this.resourceQueues = new HashMap<Integer, LinkedList<OperationInvocation>>();
        this.allOperations = new HashMap<String, OperationInvocation>();
        this.stopped = false;
    }

    /**
     * Follows the same semantics as {@link ExecutorService#shutdownNow()}. All operations previously submitted to the
     * thread pool (that is, those operations that passed the gateway into the thread pool) will be canceled (or, at
     * least, a best attempt will be made to cancel them). No additional operations will be allowed to be submitted to
     * the gateway or the internal thread pool (including those that are queued in this gateway but not yet submitted to
     * the thread pool). Any queued operations sitting in this gateway will be immediately canceled.
     */
    @SuppressWarnings("unchecked")
    public void shutdown() {
        List<OperationInvocation> doomedOperations;

        synchronized (resourceQueues) {
            if (stopped) {
                return;
            }

            stopped = true;

            // drain the gateway queue
            doomedOperations = drainQueue(resourceQueues);

            // drain the thread pool queue and shutdown the thread pool
            Collection<? super Runnable> threadPoolQueueDrain = new ArrayList<Runnable>();
            threadPool.getQueue().drainTo(threadPoolQueueDrain);
            for (Object runnable : threadPoolQueueDrain) {
                doomedOperations.add((OperationInvocation) runnable);
            }

            threadPoolQueueDrain = null; // help GC

            log.debug("Shutting down operation invocation thread pool...");
            PluginContainer pluginContainer = PluginContainer.getInstance();
            pluginContainer.shutdownExecutorService(threadPool, true);
        }

        for (OperationInvocation operationToCancel : doomedOperations) {
            operationToCancel.markAsCanceled();
            operationToCancel.run();
        }

        // Let's be kind and at least give some amount of time for all threads to cancel.
        // In most cases, this will return almost immediately.
        try {
            threadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }

        // Under rare conditions, it is possible for the thread pool to have popped
        // an operation off the queue but did not have time to hand it off to a worker thread.
        // When we shutdown the thread pool and an operation is in limbo like this,
        // that operation fails to be invoked and stays in the QUEUED status.  Let's
        // look at all our operations - if any are still in the QUEUED state, these are the
        // ones in limbo - we need to forcibly cancel them.

        doomedOperations.clear();

        synchronized (resourceQueues) {
            for (OperationInvocation operationInvocation : allOperations.values()) {
                if (operationInvocation.getStatus().contains(Status.QUEUED)) {
                    doomedOperations.add(operationInvocation);
                }
            }

            allOperations.clear();
        }

        for (OperationInvocation operationToCancel : doomedOperations) {
            log.info("Operation is in limbo after shutdown - forcibly canceling: " + operationToCancel);
            operationToCancel.markAsCanceled();
            operationToCancel.run();
        }

        doomedOperations = null; // help GC

        return;
    }

    /**
     * Returns the operation invocation that is responsible for executing the operation identified with the given job
     * ID.
     *
     * @param  jobId identifies the specific operation invocation to return
     *
     * @return the operation invocation associated with the given job ID or <code>null</code> if not found
     */
    public OperationInvocation getOperationInvocation(String jobId) {
        synchronized (resourceQueues) {
            return allOperations.get(jobId);
        }
    }

    /**
     * Submits the given operation to the gateway for execution within the thread pool. If the operation's resource
     * already has an operation submitted to the thread pool, this gateway will hold onto the given operation until that
     * previous operation has finished. This ensures that no two operations can be invoked on the same resource
     * concurrently.
     *
     * @param  operation
     *
     * @throws IllegalStateException if the gateway has been shutdown
     */
    public void submit(OperationInvocation operation) {
        Integer operationResourceId = Integer.valueOf(operation.getResourceId());
        boolean failedToExecute = false;

        synchronized (resourceQueues) {
            if (stopped) {
                throw new IllegalStateException("Operations thread pool is shutdown - not accepting new submissions");
            }

            allOperations.put(operation.getJobId(), operation);

            LinkedList<OperationInvocation> queuedOps = resourceQueues.get(operationResourceId);

            // IF there are no operations queued or running on the resource already
            //    Submit it to the thread pool and create an empty list to indicate the resource will be busy
            // ELSE the resource is already busy (or will be busy) with a previous operation
            //    Add the new operation to the linked list so it can be next in line for the resource
            // END IF
            if (queuedOps == null) {
                resourceQueues.put(operationResourceId, new LinkedList<OperationInvocation>());

                try {
                    threadPool.execute(operation);
                } catch (Exception e) {
                    failedToExecute = true;
                    log.error("Failed to submit operation: " + operation);
                }
            } else {
                log.debug("Resource is busy executing a prior operation - queuing up operation: " + operation);
                queuedOps.add(operation);
            }
        }

        // If we failed to submit to the thread pool, this is a bad error and probably means our
        // thread pool queue is full.  If this ever happens, something reallly bad is happening since
        // our thread pool queue should be large enough to handle everything - blowing out this
        // thread pool queue is an indication something is awry. If this happens, cancel the
        // operation and make sure it cleans up and notifies the server as appropriate.
        // Note that we do this outside of the synchronized block
        if (failedToExecute) {
            operation.markAsCanceled();
            operation.run();
        }

        return;
    }

    /**
     * This is called by the {@link OperationInvocation} when it finished to notify this gateway that if there are any
     * other pending operations for the resource, that the next one is allowed to be executed.
     *
     * @param operation the operation that has just completed
     */
    public void operationCompleted(OperationInvocation operation) {
        Integer operationResourceId = Integer.valueOf(operation.getResourceId());

        synchronized (resourceQueues) {
            if (stopped) {
                return;
            }

            allOperations.remove(operation.getJobId());

            LinkedList<OperationInvocation> queuedOps = resourceQueues.get(operationResourceId);
            if (queuedOps != null) {
                // if there are no more operations waiting to be invoked on the resource, clean up the linked list;
                // otherwise, pop the next operation from the list and submit it to the thread pool for execution
                if (queuedOps.isEmpty()) {
                    resourceQueues.remove(operationResourceId);
                } else {
                    OperationInvocation nextOperation = queuedOps.remove();

                    try {
                        log.debug("Resource is no longer busy - the next operation in line will be invoked: "
                            + nextOperation);
                        threadPool.execute(nextOperation);
                    } catch (Exception e) {
                        log.error("Failed to submit next operation: " + nextOperation);
                    }
                }
            }
        }

        return;
    }

    /**
     * Will drain the given queue and return its contents. No synchronization is performed on the queue object. After
     * this returns, the given queue will be empty.
     *
     * @param  queue the queue to drain of all contents (will never be <code>null</code> but may be empty)
     *
     * @return the invocations that were drained from the queue
     */
    private List<OperationInvocation> drainQueue(Map<Integer, LinkedList<OperationInvocation>> queue) {
        List<OperationInvocation> contents = new ArrayList<OperationInvocation>();

        for (LinkedList<OperationInvocation> resourceOperations : queue.values()) {
            contents.addAll(resourceOperations);
            resourceOperations.clear();
        }

        queue.clear();

        return contents;
    }
}