/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.core.pc.inventory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
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
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.component.ComponentInvocationContextImpl;
import org.rhq.core.pc.component.ComponentInvocationContextImpl.LocalContext;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * This object holds information relative to the running state of a {@link ResourceComponent} in the Plugin Container.
 * It is serializable for persistence to the Plugin Container's storage mechanisms.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResourceContainer implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum SynchronizationState {
        NEW, SYNCHRONIZED, DELETED_ON_AGENT, DELETED_ON_SERVER
    }

    public enum ResourceComponentState {
        STARTED, STOPPED, STARTING
    }

    // thread pools used to invoke methods on container's components
    private static final String DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.daemon";
    private static final String NON_DAEMON_THREAD_POOL_NAME = "ResourceContainer.invoker.nonDaemon";
    private static final String AVAIL_CHECK_THREAD_POOL_NAME = "ResourceContainer.invoker.availCheck.daemon";
    private static ExecutorService DAEMON_THREAD_POOL;
    private static ExecutorService NON_DAEMON_THREAD_POOL;

    /**
     * This thread pool protects us from generating a potentially huge number of threads on slow running
     * agents where avail checks are taking longer that 1s (given a default setting).  Each avail check
     * requests a thread on the assumption that most if not all checks will be sub-second.  But if that
     * is not the case we could, if using an CachedThreadPool, end up with N concurrent avail check threads,
     * where N is the number of resources managed by the agent (because that type of pool can grow unbounded).
     * Instead, limit the max # of threads and fall back to synchronous checking when overloaded.
     */
    private static ExecutorService AVAIL_CHECK_THREAD_POOL;

    // non-transient fields
    private final Resource resource;
    private SynchronizationState synchronizationState = SynchronizationState.NEW;
    private Set<MeasurementScheduleRequest> measurementSchedule = new HashSet<MeasurementScheduleRequest>();
    private Set<ResourcePackageDetails> installedPackages = new HashSet<ResourcePackageDetails>();
    private final Map<String, DriftDefinition> driftDefinitions = new HashMap<String, DriftDefinition>();
    private MeasurementScheduleRequest availabilitySchedule = null;

    // transient fields
    private transient ResourceComponent resourceComponent;
    private transient ResourceContext resourceContext;
    private transient ResourceComponentState resourceComponentState = ResourceComponentState.STOPPED;
    private transient ReentrantReadWriteLock facetAccessLock = new ReentrantReadWriteLock();
    private transient Map<Integer, Object> proxyCache = new HashMap<Integer, Object>();
    private transient ClassLoader resourceClassLoader;
    // the currently known availability
    private transient Availability availability;
    // the time at which this resource is up for an avail check. null indicates unscheduled.
    private transient Long availabilityScheduleTime;
    private transient AvailabilityProxy availabilityProxy;

    /**
     * Initialize the ResourceContainer's internals, such as its thread pools.
     *
     * @param configuration the plugin container's configuration
     */
    public static void initialize(PluginContainerConfiguration pcConfig) {
        LoggingThreadFactory daemonFactory = new LoggingThreadFactory(DAEMON_THREAD_POOL_NAME, true);
        LoggingThreadFactory nonDaemonFactory = new LoggingThreadFactory(NON_DAEMON_THREAD_POOL_NAME, false);
        LoggingThreadFactory availCheckFactory = new LoggingThreadFactory(AVAIL_CHECK_THREAD_POOL_NAME, true);
        DAEMON_THREAD_POOL = Executors.newCachedThreadPool(daemonFactory);
        NON_DAEMON_THREAD_POOL = Executors.newCachedThreadPool(nonDaemonFactory);
        AVAIL_CHECK_THREAD_POOL = Executors.newFixedThreadPool(pcConfig.getAvailabilityScanThreadPoolSize(),
            availCheckFactory);
    }

    /**
     * Shuts down ResourceContainer's internals, such as its thread pools.
     */
    public static void shutdown() {
        // TODO (ips, 04/30/12): Should we funnel these through PluginContainer.shutdownExecutorService()?
        DAEMON_THREAD_POOL.shutdown();
        NON_DAEMON_THREAD_POOL.shutdown();
        AVAIL_CHECK_THREAD_POOL.shutdown();
    }

    public ResourceContainer(Resource resource, ClassLoader resourceClassLoader) {
        this.resource = resource;
        this.resourceClassLoader = resourceClassLoader;
    }

    public Availability updateAvailability(AvailabilityType availabilityType) {
        synchronized (this) {
            this.availability = new Availability(this.resource, availabilityType);
            return this.availability;
        }
    }

    public Resource getResource() {
        return this.resource;
    }

    /**
     * Returns the currently known availability of the resource. This will return <code>null</code> if this resource is
     * new and we do not yet know what its availability is.
     *
     * @return resource's availability or <code>null</code> if it is not known
     */
    @Nullable
    public Availability getAvailability() {
        synchronized (this) {
            return this.availability;
        }
    }

    /**
     * If a piece of code wants to make a call into a plugin component's facet, and that call doesn't need to write or
     * modify any data within the component or the managed resource itself, that code should obtain the returned read
     * lock.
     *
     * @return lock that provides read-only access into all facets of this container's component.
     */
    public Lock getReadFacetLock() {
        return this.facetAccessLock.readLock();
    }

    /**
     * If a piece of code wants to make a call into a plugin component's facet, and that call may need to write or
     * modify data within the component or the managed resource itself, that code should obtain the returned write lock.
     *
     * @return lock that provides read-write access into all facets of this container's component.
     */
    public Lock getWriteFacetLock() {
        return this.facetAccessLock.writeLock();
    }

    public Set<ResourcePackageDetails> getInstalledPackages() {
        synchronized (this) {
            return this.installedPackages;
        }
    }

    public void setInstalledPackages(Set<ResourcePackageDetails> installedPackages) {
        synchronized (this) {
            this.installedPackages = installedPackages;
        }
    }

    public ResourceComponent getResourceComponent() {
        synchronized (this) {
            return this.resourceComponent;
        }
    }

    public void setResourceComponent(ResourceComponent resourceComponent) {
        synchronized (this) {
            this.resourceComponent = resourceComponent;
            this.availabilityProxy = new AvailabilityProxy(resourceComponent, AVAIL_CHECK_THREAD_POOL,
                resourceClassLoader);
        }
    }

    public ResourceContext getResourceContext() {
        synchronized (this) {
            return this.resourceContext;
        }
    }

    public void setResourceContext(ResourceContext resourceContext) {
        synchronized (this) {
            this.resourceContext = resourceContext;
        }
    }

    public Set<MeasurementScheduleRequest> getMeasurementSchedule() {
        synchronized (this) {
            return this.measurementSchedule;
        }
    }

    public void setMeasurementSchedule(Set<MeasurementScheduleRequest> measurementSchedule) {
        synchronized (this) {
            this.measurementSchedule = measurementSchedule;

            // this should not happen but if it does, protect against it because it will sink the agent
            if (null != this.measurementSchedule) {
                for (MeasurementScheduleRequest sched : this.measurementSchedule) {
                    if (sched.getInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                        String smallStack = ThrowableUtil.getFilteredStackAsString(new Throwable());
                        String msg = "Invalid collection interval ["
                            + sched
                            + "] for Resource ["
                            + resource
                            + "]. Setting it to 20 minutes until the situation is corrected. Please report to Development: "
                            + smallStack;
                        LogFactory.getLog(ResourceContainer.class).error(msg);
                        sched.setInterval(20L * 60L * 1000L);
                    }
                }
            }
        }
    }

    public MeasurementScheduleRequest getAvailabilitySchedule() {
        // platforms don't have a schedule but other types should. If one has not yet been set (this can
        // happen in various upgrade scenarios) set one, using a default interval.
        synchronized (this) {
            if (null == availabilitySchedule) {
                switch (this.resource.getResourceType().getCategory()) {
                case PLATFORM:
                    break;
                case SERVER:
                    availabilitySchedule = new MeasurementScheduleRequest(-1, MeasurementDefinition.AVAILABILITY_NAME,
                        MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVER, true, DataType.AVAILABILITY);
                    break;
                case SERVICE:
                    availabilitySchedule = new MeasurementScheduleRequest(-1, MeasurementDefinition.AVAILABILITY_NAME,
                        MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVICE, true, DataType.AVAILABILITY);
                    break;
                }
            }
        }

        return availabilitySchedule;
    }

    public void setAvailabilitySchedule(MeasurementScheduleRequest availabilitySchedule) {
        synchronized (this) {
            this.availabilitySchedule = availabilitySchedule;
            // when the schedule is (re)set just null out the schedule time and it will get rescheduled on the
            // next avail execution.
            this.availabilityScheduleTime = null;
        }
    }

    public Long getAvailabilityScheduleTime() {
        return availabilityScheduleTime;
    }

    // TODO: Is there a reason for this to be synchronized like the other setters? I don't see why it would need to be.
    public void setAvailabilityScheduleTime(Long availabilityScheduleTime) {
        this.availabilityScheduleTime = availabilityScheduleTime;
    }

    /**
    * Updates the measurementSchedule with the modifications made in the measurementScheduleUpdate.
     *
    * @param measurementScheduleUpdate the updates to the current measurementSchedule
     *
    * @return true if the schedule was updated successfully, false otherwise or if measurementScheduleUpdate is null
    */
    public boolean updateMeasurementSchedule(Set<MeasurementScheduleRequest> measurementScheduleUpdate) {
        if (null == measurementScheduleUpdate) {
            return false;
        }

        // this should not happen but if it does, protect against it because it will sink the agent
        for (MeasurementScheduleRequest sched : measurementScheduleUpdate) {
            if (sched.getInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                String smallStack = ThrowableUtil.getFilteredStackAsString(new Throwable());
                String msg = "Invalid collection interval [" + sched + "] for Resource [" + resource
                    + "]. Setting it to 20 minutes until the situation is corrected. Please report to Development: "
                    + smallStack;
                LogFactory.getLog(ResourceContainer.class).error(msg);
                sched.setInterval(20L * 60L * 1000L);
            }
        }

        Set<Integer> updateScheduleIds = new HashSet<Integer>();
        for (MeasurementScheduleRequest update : measurementScheduleUpdate) {
            updateScheduleIds.add(update.getScheduleId());
        }

        synchronized (this) {
            Set<MeasurementScheduleRequest> toBeRemoved = new HashSet<MeasurementScheduleRequest>();
            for (MeasurementScheduleRequest current : this.measurementSchedule) {
                if (updateScheduleIds.contains(current.getScheduleId())) {
                    toBeRemoved.add(current);
                }
            }
            // first remove all the old versions of the measurement schedules
            this.measurementSchedule.removeAll(toBeRemoved);

            // then add the new versions
            return this.measurementSchedule.addAll(measurementScheduleUpdate);
        }
    }

    public Collection<DriftDefinition> getDriftDefinitions() {
        synchronized (this) {
            return driftDefinitions.values();
        }
    }

    public boolean containsDriftDefinition(DriftDefinition d) {
        synchronized (this) {
            return driftDefinitions.containsKey(d.getName());
        }
    }

    public void addDriftDefinition(DriftDefinition d) {
        synchronized (this) {
            driftDefinitions.put(d.getName(), d);
        }
    }

    public void removeDriftDefinition(DriftDefinition d) {
        synchronized (this) {
            driftDefinitions.remove(d.getName());
        }
    }

    public ResourceComponentState getResourceComponentState() {
        synchronized (this) {
            return this.resourceComponentState;
        }
    }

    public void setResourceComponentState(ResourceComponentState state) {
        synchronized (this) {
            this.resourceComponentState = state;
        }
    }

    public SynchronizationState getSynchronizationState() {
        synchronized (this) {
            return this.synchronizationState;
        }
    }

    public void setSynchronizationState(SynchronizationState synchronizationState) {
        synchronized (this) {
            this.synchronizationState = synchronizationState;
        }
    }

    public ClassLoader getResourceClassLoader() {
        return this.resourceClassLoader;
    }

    /**
     * Sets the classloader that should be used by the resource when its component interfaces are being invoked.
     * In most (but not all) cases, this is the plugin classloader of the plugin that defined the resource.
     *
     * @param resourceClassLoader the resource's context classloader
     */
    public void setResourceClassLoader(ClassLoader resourceClassLoader) {
        this.resourceClassLoader = resourceClassLoader;
    }

    @Override
    public String toString() {
        AvailabilityType avail = (this.availability != null) ? this.availability.getAvailabilityType() : null;
        return this.getClass().getSimpleName() + "[resource=" + this.resource + ", syncState="
            + this.synchronizationState + ", componentState=" + this.resourceComponentState + ", avail=" + avail + "]";
    }

    /**
     * Creates a proxy to this container's resource component, essentially returning the component exposed as the given
     * facet interface. This proxy will ensure that calls to the component's interface are synchronized with the given
     * lock type. If <code>lockType</code> is {@link FacetLockType#NONE} and there is no timeout, then the resource's
     * actual component instance is returned as-is (i.e. it will not be wrapped in a proxy - which means this returns
     * the same as {@link #getResourceComponent()}).
     *
     * @param  facetInterface the interface that the component implements and will expose via the proxy
     * @param  lockType       the type of lock to use when synchronizing access; must not be null
     * @param  timeout        if the method invocation thread has not completed after this many milliseconds, interrupt
     *                        it; value must be positive
     * @param  daemonThread   whether or not the thread used for the invocation should be a daemon thread
     * @param  onlyIfStarted  if <code>true</code>, and the component is not started, an exception is thrown
     * @param transferInterrupt whether or not interruption of the calling thread should be transfered to the executor
     *                          thread
     *
     * @return a proxy that wraps the given component and exposes the given facet interface; will never be null
     *
     * @throws PluginContainerException if the component does not exist or does not implement the interface
     */
    public <T> T createResourceComponentProxy(Class<T> facetInterface, FacetLockType lockType, long timeout,
        boolean daemonThread, boolean onlyIfStarted, boolean transferInterrupt) throws PluginContainerException {
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

        // If no locking is required and there is no timeout, there is no need for a proxy - return the actual component.
        if (lockType == FacetLockType.NONE && timeout == 0) {
            return (T) resourceComponent;
        }

        // Check for a cached proxy.
        int key;
        key = facetInterface.hashCode();
        key = 31 * key + lockType.hashCode();
        key = 31 * key + (int) (timeout ^ (timeout >>> 32));
        key = 31 * key + (daemonThread ? 1 : 0);
        key = 31 * key + (transferInterrupt ? 1 : 0);

        synchronized (this) {
            if (this.proxyCache == null) {
                this.proxyCache = new HashMap<Integer, Object>();
            }

            T proxy = (T) this.proxyCache.get(key);
            if (proxy == null) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

                // this is the handler that will actually acquire the lock and invoke the facet method call
                ResourceComponentInvocationHandler handler = new ResourceComponentInvocationHandler(this, lockType,
                    timeout, daemonThread, facetInterface, transferInterrupt);

                // this is the proxy that will look like the facet interface that the caller will use
                proxy = (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { facetInterface }, handler);
                this.proxyCache.put(key, proxy);
            }

            return proxy;
        }
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

    // Recreate the facet lock on deserialization.
    private Object readResolve() throws java.io.ObjectStreamException {
        this.facetAccessLock = new ReentrantReadWriteLock();
        return this;
    }

    /**
     * Return a proxy for a call to check resource availability, using the daemon thread pool.
     *
     * @see AvailabilityProxy for details
     */
    public AvailabilityFacet getAvailabilityProxy() {
        return this.availabilityProxy;
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
        private final boolean transferInterrupt;

        /**
         *
         * @param container the resource container managing the resource component upon which the method will be invoked;
         *                  caller must ensure the container's component is never null
         * @param lockType the type of facet lock to acquire for the invocation; must not be null
         * @param timeout if the method invocation thread has not completed after this many milliseconds, interrupt it;
         *                value must be positive
         * @param daemonThread whether or not the thread used for the invocation should be a daemon thread
         * @param facetInterface the interface that the component implements that is being exposed by this proxy
         * @param transferInterrupt whether or not interruption of the calling thread should be transfered to the
         *                          executor thread
         */
        public ResourceComponentInvocationHandler(ResourceContainer container, FacetLockType lockType, long timeout,
            boolean daemonThread, Class facetInterface, boolean transferInterrupt) {
            this.container = container;
            switch (lockType) {
            case WRITE: {
                this.lock = container.getWriteFacetLock();
                break;
            }
            case READ: {
                this.lock = container.getReadFacetLock();
                break;
            }
            default: {
                this.lock = null;
                break;
            }
            }
            if (timeout <= 0) {
                throw new IllegalArgumentException("timeout value is not positive.");
            }
            this.timeout = timeout;
            this.daemonThread = daemonThread;
            this.facetInterface = facetInterface;
            this.transferInterrupt = transferInterrupt;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(this.facetInterface)) {
                return invokeInNewThreadWithLock(method, args);
            } else {
                // toString(), etc.
                return invokeInCurrentThreadWithoutLock(method, args);
            }
        }

        private Object invokeInNewThreadWithLock(Method method, Object[] args) throws Throwable {
            ExecutorService threadPool = this.daemonThread ? DAEMON_THREAD_POOL : NON_DAEMON_THREAD_POOL;
            ComponentInvocation componentInvocation = new ComponentInvocation(this.container, method, args, this.lock);
            Future<?> future = threadPool.submit(componentInvocation);
            try {
                return future.get(this.timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Thread [" + Thread.currentThread().getName() + "] was interrupted.");
                if (this.transferInterrupt) {
                    future.cancel(true);
                    componentInvocation.markContextInterrupted();
                }
                throw new RuntimeException(invokedMethodString(method, args, "was rudely interrupted."), e);
            } catch (ExecutionException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(invokedMethodString(method, args, "failed."), e);
                }
                throw e.getCause();
            } catch (java.util.concurrent.TimeoutException e) {
                String msg = invokedMethodString(method, args, "timed out after " + timeout
                    + " milliseconds - invocation thread will be interrupted.");
                LOG.debug(msg);
                Throwable cause = new Throwable();
                cause.setStackTrace(componentInvocation.getStackTrace());
                future.cancel(true);
                componentInvocation.markContextInterrupted();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this.container.getFacetLockStatus());
                }
                throw new TimeoutException(msg).initCause(cause);
            }
        }

        private String invokedMethodString(Method method, Object[] methodArgs, String extraMsg) {
            String name = this.container.getResourceComponent().getClass().getName() + '.' + method.getName() + "()";
            String args = ((methodArgs != null) ? Arrays.asList(methodArgs).toString() : "");
            return "Call to [" + name + "] with args [" + args + "] " + extraMsg;
        }

        private Object invokeInCurrentThreadWithoutLock(Method method, Object[] args) throws Throwable {
            Thread thread = Thread.currentThread();
            ClassLoader originalContextClassLoader = thread.getContextClassLoader();
            try {
                ClassLoader pluginClassLoader = this.container.getResourceClassLoader();
                if (pluginClassLoader == null) {
                    throw new IllegalStateException("No plugin classloader was specified for " + this + ".");
                }
                thread.setContextClassLoader(pluginClassLoader);
                // This is the actual call into the resource component.
                return method.invoke(this.container.getResourceComponent(), args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            } finally {
                thread.setContextClassLoader(originalContextClassLoader);
            }
        }
    }

    private static class ComponentInvocation implements Callable {
        private static final Log LOG = LogFactory.getLog(ComponentInvocation.class);

        private final ResourceContainer resourceContainer;
        private final Method method;
        private final Object[] args;
        private final Lock lock;
        private final ComponentInvocationContextImpl componentInvocationContext;
        private final LocalContext localContext;
        private volatile Thread thread;

        ComponentInvocation(ResourceContainer resourceContainer, Method method, Object[] args, Lock lock) {
            this.resourceContainer = resourceContainer;
            this.method = method;
            this.args = args;
            this.lock = lock;
            this.componentInvocationContext = (ComponentInvocationContextImpl) resourceContainer.getResourceContext()
                .getComponentInvocationContext();
            localContext = new LocalContext();
        }

        /**
         * Return the stack trace for the thread executing this call.
         * Returns an empty stack trace if not called.
         */
        public StackTraceElement[] getStackTrace() throws Exception {
            if (thread == null)
                return new StackTraceElement[0];
            return thread.getStackTrace();
        }

        public Object call() throws Exception {
            this.thread = Thread.currentThread();
            if (this.lock != null) {
                try {
                    this.lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                // If we made it here, we have acquired the lock.
            }

            componentInvocationContext.setLocalContext(localContext);
            ClassLoader originalContextClassLoader = thread.getContextClassLoader();

            // The thread needs to run with a fresh invocation context
            try {
                ClassLoader pluginClassLoader = this.resourceContainer.getResourceClassLoader();
                if (pluginClassLoader == null) {
                    throw new IllegalStateException("No plugin class loader was specified for " + this + ".");
                }
                thread.setContextClassLoader(pluginClassLoader);
                // This is the actual call into the resource component's facet interface.
                ResourceComponent resourceComponent = this.resourceContainer.getResourceComponent();
                return this.method.invoke(resourceComponent, this.args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                //noinspection ThrowableInstanceNeverThrown
                throw (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (this.lock != null) {
                    this.lock.unlock();
                }
                this.thread.setContextClassLoader(originalContextClassLoader);
                this.thread = null;
            }
        }

        public void markContextInterrupted() {
            localContext.markInterrupted();
            LOG.warn(getContextInterruptedWarningMessage(LOG.isDebugEnabled()));
        }

        private String getContextInterruptedWarningMessage(boolean detailed) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invocation has been marked interrupted for method [");
            if (detailed) {
                sb.append(method.toGenericString());
            } else {
                sb.append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName());
            }
            sb.append("] on resource [").append(resourceContainer.getResource()).append("]");
            return sb.toString();
        }
    }
}
