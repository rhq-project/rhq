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
package org.rhq.core.pc.inventory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * This object holds information relative to the running state of a {@link ResourceComponent} in the Plugin Container.
 * It is serializable for persistence to the Plugin Container's storage mechanisms.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class ResourceContainer implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum SynchronizationState {
        NEW, SYNCHRONIZED, DELETED_ON_AGENT, DELETED_ON_SERVER
    }

    public enum ResourceComponentState {
        STARTED, STOPPED
    }

    private transient ResourceComponent resourceComponent;
    private transient ResourceComponentState resourceComponentState = ResourceComponentState.STOPPED;
    private transient ReentrantReadWriteLock facetAccessLock = new ReentrantReadWriteLock();
    private Resource resource;
    private Availability availability;
    private Set<ResourcePackageDetails> installedPackages = new HashSet<ResourcePackageDetails>();
    private Set<MeasurementScheduleRequest> measurementSchedule;
    private SynchronizationState synchronizationState = SynchronizationState.NEW;

    public ResourceContainer(Resource resource) {
        this.resource = resource;
    }

    public Availability updateAvailability(AvailabilityType availabilityType) {
        Date now = new Date();
        this.availability = new Availability(resource, now, availabilityType);
        return availability;
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Returns the currently known availability of the resource. This will return <code>null</code> if this resource is
     * new and we do not yet know what its availability is.
     *
     * @return resource's availability or <code>null</code> if it is not known
     */
    public Availability getAvailability() {
        return availability;
    }

    /**
     * If a piece of code wants to make a call into a plugin component's facet, and that call doesn't need to write or
     * modify any data within the component or the managed resource itself, that code should obtain the returned read
     * lock.
     *
     * @return lock that provides read-only access into all facets of this container's component.
     */
    public Lock getReadFacetLock() {
        return facetAccessLock.readLock();
    }

    /**
     * If a piece of code wants to make a call into a plugin component's facet, and that call may need to write or
     * modify data within the component or the managed resource itself, that code should obtain the returned write lock.
     *
     * @return lock that provides read-write access into all facets of this container's component.
     */
    public Lock getWriteFacetLock() {
        return facetAccessLock.writeLock();
    }

    public Set<ResourcePackageDetails> getInstalledPackages() {
        return installedPackages;
    }

    public void setInstalledPackages(Set<ResourcePackageDetails> installedPackages) {
        this.installedPackages = installedPackages;
    }

    public ResourceComponent getResourceComponent() {
        return resourceComponent;
    }

    public void setResourceComponent(ResourceComponent resourceComponent) {
        this.resourceComponent = resourceComponent;
    }

    public Set<MeasurementScheduleRequest> getMeasurementSchedule() {
        return measurementSchedule;
    }

    public void setMeasurementSchedule(Set<MeasurementScheduleRequest> measurementSchedule) {
        this.measurementSchedule = measurementSchedule;
    }

    /**
    * Updates the measurementSchedule with the modifications made in the measurementScheduleUpdate.
     *
    * @param measurementScheduleUpdate the updates to the current measurementSchedule
     *
    * @return true if the schedule was updated successfully, false otherwise
    */
    public boolean updateMeasurementSchedule( Set<MeasurementScheduleRequest> measurementScheduleUpdate)
    {
        Set<Integer> updateScheduleIds = new HashSet<Integer>();
        for(MeasurementScheduleRequest update: measurementScheduleUpdate ) {
            updateScheduleIds.add( update.getScheduleId() );
        }

        Set<MeasurementScheduleRequest> toBeRemoved  = new HashSet<MeasurementScheduleRequest>();
        for( MeasurementScheduleRequest current : this.measurementSchedule ) {
            if( updateScheduleIds.contains( current.getScheduleId() ) ) {
                toBeRemoved.add( current );
            }
        }
        // first remove all the old versions of the measurement schedules
        this.measurementSchedule.removeAll(toBeRemoved);

        // then add the new versions
        return  measurementSchedule.addAll(measurementScheduleUpdate);
    }

    public ResourceComponentState getResourceComponentState() {
        return this.resourceComponentState;
    }

    public void setResourceComponentState(ResourceComponentState state) {
        this.resourceComponentState = state;
    }

    public SynchronizationState getSynchronizationState() {
        return synchronizationState;
    }

    public void setSynchronizationState(SynchronizationState synchronizationState) {
        this.synchronizationState = synchronizationState;
    }

    @Override
    public String toString() {
        return "ResourceContainer: resource=[" + this.resource + "]";
    }

    /**
     * Creates a proxy to this container's resource component, essentially returning the component exposed as the given
     * facet interface. This proxy will ensure that calls to the component's interface are synchronized with the given
     * lock type. If <code>lockType</code> is {@link FacetLockType#NONE} and there is no timeout, then the resource's
     * actual component instance is returned as-is (i.e. it will not be wrapped in a proxy - which means this returns
     * the same as {@link #getResourceComponent()}).
     *
     * @param  facetInterface the interface that the component implements and will expose via the proxy
     * @param  lockType       the type of lock to use when synchronizing access
     * @param  timeout        if the method invocation thread has not completed after this many milliseconds, interrupt it;
     *                        a value of <code>0</code> means to wait forever (generally not recommended)
     * @param  daemonThread   whether or not the thread used for the invocation should be a daemon thread
     * @param  onlyIfStarted  if <code>true</code>, and the component is not started, an exception is thrown @return a proxy that wraps the given component and exposes the given facet interface
     *
     * @return a proxy to this container's resource component
     *
     * @throws PluginContainerException if the component does not exist or does not implement the interface
     */
    @SuppressWarnings("unchecked")
    public <T> T createResourceComponentProxy(Class<T> facetInterface, FacetLockType lockType, long timeout, boolean daemonThread, boolean onlyIfStarted)
        throws PluginContainerException {
        if (onlyIfStarted) {
            if (!ResourceComponentState.STARTED.equals(getResourceComponentState())) {
                throw new PluginContainerException("Resource component could not be retrieved for resource ["
                    + getResource() + "] because the component is not started. Its state is ["
                    + getResourceComponentState() + "]");
            }
        }

        ResourceComponent resourceComponent = this.getResourceComponent();

        if (resourceComponent == null) {
            throw new PluginContainerException("Component does not exist for resource: " + getResource());
        }

        if (!(facetInterface.isAssignableFrom(resourceComponent.getClass()))) {
            throw new PluginContainerException("Component does not support the [" + facetInterface.getName()
                + "] interface: " + this);
        }

        // if no locking is required and there is no timeout, there is no need for a proxy - return the actual component
        if (lockType == FacetLockType.NONE && timeout == 0) {
            return (T) resourceComponent;
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // this is the handler that will actually acquire the lock and invoke the facet method call
        ResourceComponentInvocationHandler handler = new ResourceComponentInvocationHandler(this, lockType, timeout, daemonThread);

        // this is the proxy that will look like the facet interface that the caller will use
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { facetInterface }, handler);
    }

    private String getFacetLockStatus() {
        StringBuilder str = new StringBuilder("Facet lock status for [" + getResource());

        str.append("], is-write-locked=[").append(facetAccessLock.isWriteLocked());
        str.append("], is-write-locked-by-current-thread=[").append(facetAccessLock.isWriteLockedByCurrentThread());
        str.append("], read-locks=[").append(facetAccessLock.getReadLockCount());
        str.append("], waiting-for-lock-queue-size=[").append(facetAccessLock.getQueueLength());
        str.append("]");

        return str.toString();
    }

    /**
     * This is a ResourceComponent proxy that invokes component methods in pooled threads. Depending on the parameters
     * passed to its constructor, it may also:
     *
     *   1) obtain a facet lock before passing the invocation call to the actual component, and/or
     *   2) interrupt the invocation thread and throw a {@link TimeoutException} if its execution time exceeds a
     *      specified timeout
     */
    private static class ResourceComponentInvocationHandler implements InvocationHandler {
        private static final Log LOG = LogFactory.getLog(ResourceComponentInvocationHandler.class);

        private static final String DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.daemon";
        private static final String NON_DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.nonDaemon";

        private static final ExecutorService DAEMON_THREAD_POOL = Executors.newCachedThreadPool(new LoggingThreadFactory(
                DAEMON_THREAD_POOL_NAME, true));
        private static final ExecutorService NON_DAEMON_THREAD_POOL = Executors.newCachedThreadPool(new LoggingThreadFactory(
                NON_DAEMON_THREAD_POOL_NAME, false));        

        private final ResourceContainer container;
        private final Lock lock;
        private final long timeout;
        private final boolean daemonThread;

        /**
         *
         * @param container the resource container managing the resource component upon which the method will be invoked;
         *                  caller must ensure the container's component is never null
         * @param lockType the type of lock to use for the invocation
         * @param timeout if the method invocation thread has not completed after this many milliseconds, interrupt it;
         *                a value of <code>0</code> means to wait forever (generally not recommended)
         * @param daemonThread whether or not the thread used for the invocation should be a daemon thread
         */
        public ResourceComponentInvocationHandler(ResourceContainer container, FacetLockType lockType, long timeout, boolean daemonThread) {
            this.container = container;
            if (lockType == FacetLockType.WRITE) {
                this.lock = container.getWriteFacetLock();
            } else if (lockType == FacetLockType.READ) {
                this.lock = container.getReadFacetLock();
            } else {
                this.lock = null;
            }
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout value is negative.");
            }
            this.timeout = timeout;
            this.daemonThread = daemonThread;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ComponentInvocationThread invocationThread = new ComponentInvocationThread(this.container, method, args, this.lock, this.timeout);
            ExecutorService threadPool = this.daemonThread ? DAEMON_THREAD_POOL : NON_DAEMON_THREAD_POOL;
            // This is the actual call into the plugin component's facet interface.
            Future<?> future = threadPool.submit(invocationThread);
            String methodName = this.container.getResourceComponent().getClass().getName() + "." + method.getName() + "()";
            String methodArgs = "[" + ((args != null) ? Arrays.asList(args) : "") + "]";
            try {
                if (this.timeout == 0) {
                    // TODO: Do we really want to wait forever, or should we default to an hour or something?
                    future.get();
                }
                else {
                    future.get(this.timeout, TimeUnit.MILLISECONDS);
                }
            }
            catch (InterruptedException e) {
                LOG.error("Thread '" + Thread.currentThread().getName() + "' was interrupted.");
                if (this.daemonThread) {
                    future.cancel(true);
                }
                throw new RuntimeException("Call to " + methodName + " with args " + methodArgs + " was rudely interrupted.", e);
            }
            catch (ExecutionException e) {
                // This will never happen, since we catch and swallow Throwable in ComponentInvocationThread.run().
                throw e.getCause();
            }
            catch (java.util.concurrent.TimeoutException e) {
                LOG.debug("Call to " + methodName + " with args " + methodArgs + " timed out. Interrupting the invocation thread...");
                future.cancel(true);
                throw new TimeoutException("Call to " + methodName + " with args " + methodArgs + " timed out.");
            }
            // If we got this far, the call completed.
            if (invocationThread.getError() != null) {
                throw invocationThread.getError();
            }
            return invocationThread.getResults();
        }
    }

    // Recreate the facet lock on deserialization
    private Object readResolve() throws java.io.ObjectStreamException {
        this.facetAccessLock = new ReentrantReadWriteLock();
        return this;
    }

    static class ComponentInvocationThread extends Thread {
        private static final int DEFAULT_LOCK_TIMEOUT_IN_SECONDS = 30 * 60; // 1/2 hour

        private final ResourceContainer resourceContainer;
        private final Method method;
        private final Object[] args;
        private final Lock lock;
        private final long lockTimeout;

        private Object results;
        private Throwable error;

        ComponentInvocationThread(ResourceContainer resourceContainer, Method method, Object[] args, Lock lock, long lockTimeout) {
            this.resourceContainer = resourceContainer;
            this.method = method;
            this.args = args;
            this.lock = lock;
            this.lockTimeout = lockTimeout;
        }

        public void run() {
            ResourceComponent resourceComponent = this.resourceContainer.getResourceComponent();
            // We really want to wait indefinitely, but to prevent an infinite deadlock, let's only wait a half hour.
            long lockTimeout = (this.lockTimeout != 0) ? this.lockTimeout : DEFAULT_LOCK_TIMEOUT_IN_SECONDS;
            try {
                if (this.lock != null && !lock.tryLock(lockTimeout, TimeUnit.SECONDS)) {
                    throw new TimeoutException(
                        "Possible deadlock - could not obtain a lock while attempting to invoke method [" + this.method
                            + "] on resource component [" + resourceComponent + "]; facet-lock-status=["
                            + this.resourceContainer.getFacetLockStatus());
                }
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                this.results = this.method.invoke(resourceComponent, this.args);
            } catch (InvocationTargetException ite) {
                this.error = (ite.getCause() != null) ? ite.getCause() : ite;
            } catch (Throwable t) {
                this.error = t;
            } finally {
                if (this.lock != null) {
                    this.lock.unlock();
                }
            }
        }

        public Object getResults() {
            return results;
        }

        public Throwable getError() {
            return error;
        }
    }
}