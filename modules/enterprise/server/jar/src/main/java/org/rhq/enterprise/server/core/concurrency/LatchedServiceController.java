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
package org.rhq.enterprise.server.core.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Joseph Marques
 * @author John Mazzitelli
 */
public class LatchedServiceController {

    private final Log log = LogFactory.getLog(LatchedServiceController.class);

    private final Collection<? extends LatchedService> latchedServices;
    private final CountDownLatch serviceStartupLatch;
    private final CountDownLatch serviceCompletionLatch;
    private final Map<String, CountDownLatch> dependencyLatches;

    private int threadPoolSize;

    /**
     * Creates the controller that will be responsible for launching the services in a thread pool.
     * Note that you must ensure the order of the given services is in the order you want them submitted
     * to the thread pool. N services will be executed concurrently (where N is the
     * {@link #setThreadPoolSize(int) size of the thread pool}) but if there are dependencies among
     * services, you must ensure services appear after their dependencies in the list.
     * 
     * @param services list of services that will be invoked
     */
    public LatchedServiceController(Collection<? extends LatchedService> services) {
        this.serviceStartupLatch = new CountDownLatch(1);
        this.serviceCompletionLatch = new CountDownLatch(services.size());
        this.dependencyLatches = new HashMap<String, CountDownLatch>(services.size());

        this.latchedServices = services;
        for (LatchedService nextService : this.latchedServices) {
            this.dependencyLatches.put(nextService.getServiceName(),
                new CountDownLatch(nextService.dependencies.size()));
            nextService.controller = this;
        }

        try {
            this.threadPoolSize = Integer.parseInt(System.getProperty("rhq.server.plugin-deployer-threads", "5"));
        } catch (NumberFormatException e) {
            this.threadPoolSize = 5;
            log.warn("Invalid number of threads specified, defaulting to [" + this.threadPoolSize + "]: " + e);
        }
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void executeServices() throws LatchedServiceCircularityException {
        checkForCircularDependencies();

        ExecutorService threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
        log.debug("Will execute latched services with a concurrency of [" + this.threadPoolSize + "]");

        // submit all latched services, but they'll block either in the thread pool queue or our startup latch
        Map<String, Future<?>> threads = new HashMap<String, Future<?>>();
        for (LatchedService service : this.latchedServices) {
            log.debug("Submitting [" + service.getServiceName() + "] to thread pool");
            Future<?> thread = threadPool.submit(service);
            threads.put(service.getServiceName(), thread);
        }

        // allow them to go
        serviceStartupLatch.countDown();

        try {
            // and then wait for all of them to complete
            int elapsedMinutes = 0;
            final int MINUTES_BETWEEN_UPDATES = 3;
            while (!this.serviceCompletionLatch.await(MINUTES_BETWEEN_UPDATES, TimeUnit.MINUTES)) {
                elapsedMinutes += MINUTES_BETWEEN_UPDATES;
                boolean stillRunning = false;
                for (Map.Entry<String, Future<?>> thread : threads.entrySet()) {
                    if (!thread.getValue().isDone()) {
                        stillRunning = true;
                        log.warn("Still processing [" + thread.getKey() + "] after " + elapsedMinutes
                            + " minutes - is it hung?");
                    }
                }
                if (!stillRunning) {
                    log.error("The controller is waiting for threads that are already dead, breaking deadlock now!");
                    break;
                }
            }
        } catch (InterruptedException ie) {
            log.warn("Controller was interrupted; can not be sure if all services have begun");
        } finally {
            threadPool.shutdownNow();
        }

        log.debug("All services have begun");
    }

    private void checkForCircularDependencies() throws LatchedServiceException {
        Set<LatchedService> visited = new HashSet<LatchedService>();
        List<LatchedService> currentPath = new ArrayList<LatchedService>();
        for (LatchedService nextService : this.latchedServices) {
            if (visited.contains(nextService)) {
                // have already visited this service from a different path
                continue;
            }
            visit(nextService, visited, currentPath);
        }
    }

    private void visit(LatchedService current, Set<LatchedService> visited, List<LatchedService> currentPath)
        throws LatchedServiceException {
        visited.add(current);

        if (currentPath.contains(current)) {
            int firstOccurrence = currentPath.indexOf(current);
            StringBuilder circularMessage = new StringBuilder(current.getServiceName());
            for (int i = firstOccurrence + 1; i < currentPath.size(); i++) {
                circularMessage.append(" -> ");
                circularMessage.append(currentPath.get(i).getServiceName());
            }
            circularMessage.append(" -> ");
            circularMessage.append(current.getServiceName());

            throw new LatchedServiceCircularityException("Circular dependency detected in latched services: "
                + circularMessage + "; " + "will not attempt to start any of them");
        }

        currentPath.add(current);
        for (LatchedService dependency : current.dependencies) {
            visit(dependency, visited, currentPath);
        }
        currentPath.remove(current);
    }

    public static abstract class LatchedService implements Runnable {

        private LatchedServiceController controller;
        private final String serviceName;
        private final Set<LatchedService> dependencies;
        private final Set<LatchedService> dependees;
        private volatile boolean running = false;
        private volatile boolean hasFailed = false;

        public LatchedService(String serviceName) {
            this.serviceName = serviceName;
            this.dependencies = new HashSet<LatchedService>();
            this.dependees = new HashSet<LatchedService>();
        }

        public String getServiceName() {
            return this.serviceName;
        }

        public void addDependency(LatchedService dependency) {
            if (running) {
                throw new IllegalArgumentException(serviceName
                    + " can't accept new dependencies; it is already started");
            }

            // dependencies are needed to correctly construct the dependencyLatch
            this.dependencies.add(dependency);

            // dependees are needed for notification purposes after service start
            dependency.dependees.add(this);
        }

        public void notifyComplete(LatchedService finishedService, boolean didFail) {
            if (!dependencies.contains(finishedService)) {
                controller.log.error(finishedService + " is not a dependency of " + this);
                return;
            }

            /*
             * if one of my dependencies has failed, I can't possibly succeed starting up; 
             * so set this bit so the run method can use it to fail early
             */
            if (didFail) {
                hasFailed = true;
            }

            // one of our dependencies is done, decrement our countdown latch to indicate this
            this.controller.dependencyLatches.get(this.serviceName).countDown();
        }

        public void run() {
            running = true;

            String originalThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName("Latched Service Processor: " + this.serviceName);

            try {
                if (controller.log.isDebugEnabled()) {
                    controller.log.debug("Latched Service Processing [" + this.serviceName + "]; dependencies=["
                        + this.dependencies + "]; dependees=[" + this.dependees + "]...");
                }

                if (controller == null) {
                    throw new IllegalStateException("LatchedServices must be started via some controller");
                }

                try {
                    /* 
                     * wait until all services are ready to begin; this is 
                     * imperative as it will ensure that their dependencyLatches
                     * have been properly constructed
                     */
                    controller.serviceStartupLatch.await();
                } catch (InterruptedException ie) {
                    controller.log.info(serviceName + " will not be started; "
                        + "could not verify all dependent services in ready state");
                    hasFailed = true;
                    return;
                }

                try {
                    /*
                     * do not perform startup actions until all dependencies
                     * have performed their startup actions first
                     */
                    controller.dependencyLatches.get(this.serviceName).await();
                } catch (InterruptedException ie) {
                    controller.log.info(serviceName + " will not be started; "
                        + "did not verify all dependent services successfully started");
                    hasFailed = true;
                    return;
                }

                if (hasFailed) {
                    controller.log.info(serviceName + " will not be started; "
                        + "some upstream dependency has failed to start");
                } else {
                    try {
                        // now perform your startup actions 
                        executeService();
                        controller.log.debug(serviceName + " successfully started!");
                    } catch (LatchedServiceException lsse) {
                        controller.log.error(lsse);
                    }
                }

            } finally {
                // and notify dependees
                try {
                    for (LatchedService dependee : dependees) {
                        dependee.notifyComplete(this, hasFailed);
                    }
                } finally {
                    // and notify the controller as well 
                    controller.serviceCompletionLatch.countDown();

                    controller.log.debug("Processed [" + this.serviceName + "]");
                    Thread.currentThread().setName(originalThreadName);
                }
            }
        }

        public abstract void executeService() throws LatchedServiceException;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(getServiceName() + ":");
            boolean first = true;
            for (LatchedService dep : dependencies) {
                if (!first) {
                    builder.append("|");
                } else {
                    first = false;
                }
                builder.append(dep.getServiceName());
            }
            return builder.toString();
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final LatchedService other = (LatchedService) obj;
            if (serviceName == null) {
                if (other.serviceName != null)
                    return false;
            } else if (!serviceName.equals(other.serviceName))
                return false;
            return true;
        }
    }
}
