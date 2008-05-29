package org.rhq.enterprise.server.core.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LatchedServiceController {

    private final Log log = LogFactory.getLog(LatchedServiceController.class);

    private final Collection<? extends LatchedService> latchedServices;
    private final CountDownLatch serviceStartupLatch;
    private final CountDownLatch serviceCompletionLatch;

    public LatchedServiceController(Collection<? extends LatchedService> services) {
        this.serviceStartupLatch = new CountDownLatch(1);
        this.serviceCompletionLatch = new CountDownLatch(services.size());

        this.latchedServices = services;
        for (LatchedService nextService : latchedServices) {
            nextService.controller = this;
        }
    }

    public void executeServices() throws LatchedServiceCircularityException {
        checkForCircularDependencies();

        // start all latched services, but they'll block
        for (LatchedService service : latchedServices) {
            new Thread(service).start();
        }

        // let them all go at the same time here
        serviceStartupLatch.countDown();

        try {
            // and then wait for all of them to complete
            serviceCompletionLatch.await();
        } catch (InterruptedException ie) {
            log.info("Controller was interrupted; can not be sure if all services have begun");
        }

        log.debug("All services have begun");
    }

    private void checkForCircularDependencies() throws LatchedServiceException {
        Set<LatchedService> visited = new HashSet<LatchedService>();
        List<LatchedService> currentPath = new ArrayList<LatchedService>();
        for (LatchedService nextService : latchedServices) {
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

        private CountDownLatch dependencyLatch;
        private LatchedServiceController controller;
        private final String serviceName;
        private final List<LatchedService> dependencies;
        private final List<LatchedService> dependees;
        private volatile boolean running = false;
        private volatile boolean hasFailed = false;

        public LatchedService(String serviceName) {
            this.serviceName = serviceName;
            this.dependencies = new ArrayList<LatchedService>();
            this.dependees = new ArrayList<LatchedService>();

            /* 
             * so that services with no deps won't throw NPE
             * when awaits on the dependencyLatch in the run method
             */
            this.dependencyLatch = new CountDownLatch(0);
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

        public void notifyComplete(LatchedService service, boolean didFail) {
            if (!dependencies.contains(service)) {
                throw new IllegalArgumentException(service + " is not a dependency of " + this);
            }

            /*
             * if one of my dependencies has failed, I can't possibly succeed starting up; 
             * so set this bit so the run method can use it to fail early
             */
            if (didFail) {
                hasFailed = true;
            }

            dependencyLatch.countDown();
        }

        public void run() {
            running = true;

            try {
                if (controller == null) {
                    throw new IllegalStateException("LatchedServices must be started via some controller");
                }

                dependencyLatch = new CountDownLatch(dependencies.size());

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
                    dependencyLatch.await();
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
                for (LatchedService dependee : dependees) {
                    dependee.notifyComplete(this, hasFailed);
                }

                // and notify the controller as well 
                controller.serviceCompletionLatch.countDown();
            }
        }

        public abstract void executeService() throws LatchedServiceException;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(getServiceName() + ":");
            boolean first = true;
            for (LatchedService dep : dependencies) {
                if (!first) {
                    builder.append(", ");
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
