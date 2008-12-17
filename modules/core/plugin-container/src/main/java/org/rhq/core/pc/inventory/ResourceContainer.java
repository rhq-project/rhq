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
package org.rhq.core.pc.inventory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.rhq.core.pluginapi.inventory.ResourceContext;

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
    private transient ResourceContext resourceContext;
    private transient ResourceComponentState resourceComponentState = ResourceComponentState.STOPPED;
    private transient ReentrantReadWriteLock facetAccessLock = new ReentrantReadWriteLock();
    private Resource resource;
    private Availability availability;
    private Set<ResourcePackageDetails> installedPackages = new HashSet<ResourcePackageDetails>();
    private Set<MeasurementScheduleRequest> measurementSchedule = new HashSet<MeasurementScheduleRequest>();
    private SynchronizationState synchronizationState = SynchronizationState.NEW;
    private transient Map<Integer, Object> proxyCache = new HashMap<Integer, Object>();

    // thread pools used to invoke methods on container's components
    private static final String DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.daemon";
    private static final String NON_DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.nonDaemon";
    private static ExecutorService DAEMON_THREAD_POOL;
    private static ExecutorService NON_DAEMON_THREAD_POOL;

    /**
     * Initialize the ResourceContainer's internals, such as its thread pools.
     */
    public static void initialize() {
        LoggingThreadFactory daemonFactory = new LoggingThreadFactory(DAEMON_THREAD_POOL_NAME, true);
        LoggingThreadFactory nonDaemonFactory = new LoggingThreadFactory(NON_DAEMON_THREAD_POOL_NAME, false);
        DAEMON_THREAD_POOL = Executors.newCachedThreadPool(daemonFactory);
        NON_DAEMON_THREAD_POOL = Executors.newCachedThreadPool(nonDaemonFactory);
    }

    /**
     * Shuts down ResourceContainer's internals, such as its thread pools.
     */
    public static void shutdown() {
        DAEMON_THREAD_POOL.shutdown();
        NON_DAEMON_THREAD_POOL.shutdown();
    }

    public ResourceContainer(Resource resource) {
        this.resource = resource;
    }

    public Availability updateAvailability(AvailabilityType availabilityType) {
        Date now = new Date();
        this.availability = new Availability(this.resource, now, availabilityType);
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

    public ResourceContext getResourceContext() {
        return resourceContext;
    }

    public void setResourceContext(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;
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
    public boolean updateMeasurementSchedule(Set<MeasurementScheduleRequest> measurementScheduleUpdate) {
        Set<Integer> updateScheduleIds = new HashSet<Integer>();
        for (MeasurementScheduleRequest update : measurementScheduleUpdate) {
            updateScheduleIds.add(update.getScheduleId());
        }

        Set<MeasurementScheduleRequest> toBeRemoved = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest current : this.measurementSchedule) {
            if (updateScheduleIds.contains(current.getScheduleId())) {
                toBeRemoved.add(current);
            }
        }
        // first remove all the old versions of the measurement schedules
        this.measurementSchedule.removeAll(toBeRemoved);

        // then add the new versions
        return measurementSchedule.addAll(measurementScheduleUpdate);
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
     * @param  timeout        if the method invocation thread has not completed after this many milliseconds, interrupt
     *                        it; value must be positive
     * @param  daemonThread   whether or not the thread used for the invocation should be a daemon thread
     * @param  onlyIfStarted  if <code>true</code>, and the component is not started, an exception is thrown
     *
     * @return a proxy that wraps the given component and exposes the given facet interface
     *
     * @throws PluginContainerException if the component does not exist or does not implement the interface
     */
    @SuppressWarnings("unchecked")
    public <T> T createResourceComponentProxy(Class<T> facetInterface, FacetLockType lockType, long timeout,
        boolean daemonThread, boolean onlyIfStarted) throws PluginContainerException {
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

        // Check for a cached proxy
        int key;
        key = facetInterface.hashCode();
        key = 31 * key + lockType.hashCode();
        key = 31 * key + (int) (timeout ^ (timeout >>> 32));
        key = 31 * key + (daemonThread ? 1 : 0);

        if (proxyCache == null)
            proxyCache = new HashMap<Integer, Object>();
        T proxy = (T) proxyCache.get(key);
        if (proxy == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // this is the handler that will actually acquire the lock and invoke the facet method call
            ResourceComponentInvocationHandler handler = new ResourceComponentInvocationHandler(this, lockType,
                timeout, daemonThread, facetInterface);

            // this is the proxy that will look like the facet interface that the caller will use
            proxy = (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { facetInterface }, handler);
            proxyCache.put(key, proxy);
        }
        return proxy;
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

    // Recreate the facet lock on deserialization
    private Object readResolve() throws java.io.ObjectStreamException {
        this.facetAccessLock = new ReentrantReadWriteLock();
        return this;
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

        private final ResourceContainer container;
        private final Lock lock;
        private final long timeout;
        private final boolean daemonThread;
        private final Class facetInterface;
        private static ThreadLocal<Boolean> asynchronous = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return false;
            }
        };

        /**
         *
         * @param container the resource container managing the resource component upon which the method will be invoked;
         *                  caller must ensure the container's component is never null
         * @param lockType the type of lock to use for the invocation
         * @param timeout if the method invocation thread has not completed after this many milliseconds, interrupt it;
        *                value must be positive
         * @param daemonThread whether or not the thread used for the invocation should be a daemon thread
         * @param facetInterface the interface that the component implements that is being exposed by this proxy
         */
        public ResourceComponentInvocationHandler(ResourceContainer container, FacetLockType lockType, long timeout,
            boolean daemonThread, Class facetInterface) {
            this.container = container;
            if (lockType == FacetLockType.WRITE) {
                this.lock = container.getWriteFacetLock();
            } else if (lockType == FacetLockType.READ) {
                this.lock = container.getReadFacetLock();
            } else {
                this.lock = null;
            }
            if (timeout <= 0) {
                throw new IllegalArgumentException("timeout value is not positive.");
            }
            this.timeout = timeout;
            this.daemonThread = daemonThread;
            this.facetInterface = facetInterface;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean callIsAsync = asynchronous.get();
            try {
                // Make sure we don't make a thread call asynchronous when its already been pushed into another
                // thread farther up the stack
                if (method.getDeclaringClass().equals(this.facetInterface) && !callIsAsync) {
                    return invokeInNewThreadWithLock(method, args);
                } else {
                    // toString(), etc.
                    return invokeInCurrentThreadWithoutLock(method, args);
                }
            } finally {
                if (!callIsAsync)
                    asynchronous.set(false);
            }
        }

        private Object invokeInNewThreadWithLock(Method method, Object[] args) throws Throwable {
            ExecutorService threadPool = this.daemonThread ? DAEMON_THREAD_POOL : NON_DAEMON_THREAD_POOL;
            Callable invocationThread = new ComponentInvocationThread(this.container, method, args, this.lock);
            Future<?> future = threadPool.submit(invocationThread);
            String methodArgs = "[" + ((args != null) ? Arrays.asList(args) : "") + "]";
            try {
                return future.get(this.timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("Thread '" + Thread.currentThread().getName() + "' was interrupted.");
                if (this.daemonThread) {
                    future.cancel(true);
                }
                String methodName = this.container.getResourceComponent().getClass().getName() + "." + method.getName()
                    + "()";
                throw new RuntimeException("Call to " + methodName + " with args " + methodArgs
                    + " was rudely interrupted.", e);
            } catch (ExecutionException e) {
                if (LOG.isDebugEnabled()) {
                    String methodName = this.container.getResourceComponent().getClass().getName() + "."
                        + method.getName() + "()";
                    LOG.debug("Call to " + methodName + " with args " + methodArgs + " failed.", e);
                }
                throw e.getCause();
            } catch (java.util.concurrent.TimeoutException e) {
                if (LOG.isDebugEnabled()) {
                    String methodName = this.container.getResourceComponent().getClass().getName() + "."
                        + method.getName() + "()";
                    LOG.debug("Call to " + methodName + " with args " + methodArgs
                        + " timed out. Interrupting the invocation thread...");
                }
                future.cancel(true);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this.container.getFacetLockStatus());
                }
                String methodName = this.container.getResourceComponent().getClass().getName() + "." + method.getName()
                    + "()";
                throw new TimeoutException("Call to " + methodName + " with args " + methodArgs + " timed out.");
            }
        }

        private Object invokeInCurrentThreadWithoutLock(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(this.container.getResourceComponent(), args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            }
        }
    }

    private static class ComponentInvocationThread implements Callable {
        private final ResourceContainer resourceContainer;
        private final Method method;
        private final Object[] args;
        private final Lock lock;

        ComponentInvocationThread(ResourceContainer resourceContainer, Method method, Object[] args, Lock lock) {
            this.resourceContainer = resourceContainer;
            this.method = method;
            this.args = args;
            this.lock = lock;
        }

        public Object call() throws Exception {
            ResourceComponent resourceComponent = this.resourceContainer.getResourceComponent();
            if (this.lock != null) {
                try {
                    this.lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // If we made it here, we have acquired the lock.
            }
            try {
                // This is the actual call into the resource component's facet interface.
                return this.method.invoke(resourceComponent, this.args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (this.lock != null) {
                    this.lock.unlock();
                }
            }
        }
    }
}