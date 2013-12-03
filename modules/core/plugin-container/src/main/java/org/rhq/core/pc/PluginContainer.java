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
package org.rhq.core.pc;

import java.beans.Introspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.security.auth.login.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.pc.agent.AgentRegistrar;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceLifecycleListener;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;
import org.rhq.core.pc.bundle.BundleManager;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.configuration.ConfigurationManagerInitializer;
import org.rhq.core.pc.content.ContentManager;
import org.rhq.core.pc.drift.DriftManager;
import org.rhq.core.pc.event.EventManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.support.SupportManager;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.util.FileUtils;

/**
 * This is the embeddable container that houses all plugins and the infrastructure that binds them together. It contains
 * all the managers such as {@link PluginManager} and {@link InventoryManager}.
 *
 * <p>This container is controlled by its lifecycle methods ({@link #initialize()} and {@link #shutdown()}. Prior to
 * initialization, this container's configuration should be set via
 * {@link #setConfiguration(PluginContainerConfiguration)}. If this is not done, a default configuration will be
 * created.</p>
 *
 * @author John Mazzitelli
 * @author Greg Hinkle
 */
public class PluginContainer {

    private static final PluginContainer INSTANCE = new PluginContainer();

    private final Log log = LogFactory.getLog(PluginContainer.class);

    private static final class NullRebootRequestListener implements RebootRequestListener {
        @Override
        public void reboot() {
        }
    }

    /**
     * Invoked by the plugin container immediately after it is initialized
     */
    public static interface InitializationListener {
        /**
         * Notifies the listener that the plugin container has been initialized. This method executes in the same
         * thread in which {@link PluginContainer#initialize()} is executing.
         */
        void initialized();
    }

    /**
     * Invoked by the plugin container immediately after it is shutdown
     */
    public static interface ShutdownListener {
        /**
         * Notifies the listener that the plugin container has been shutdown. This method executes in the same
         * thread in which {@link PluginContainer#shutdown()} is executing.
         */
        void shutdown();
    }

    // our management interface
    private PluginContainerMBeanImpl mbean;

    private PluginContainerConfiguration configuration;
    private String version;
    private boolean started = false;

    private PluginManager pluginManager;
    private PluginComponentFactory pluginComponentFactory;
    private InventoryManager inventoryManager;
    private MeasurementManager measurementManager;
    private ConfigurationManager configurationManager;
    private OperationManager operationManager;
    private ResourceFactoryManager resourceFactoryManager;
    private ContentManager contentManager;
    private EventManager eventManager;
    private SupportManager supportManager;
    private BundleManager bundleManager;
    private DriftManager driftManager;

    private Collection<AgentServiceLifecycleListener> agentServiceListeners = new LinkedHashSet<AgentServiceLifecycleListener>();
    private AgentServiceStreamRemoter agentServiceStreamRemoter = null;
    private AgentRegistrar agentRegistrar = null;

    private RebootRequestListener rebootListener = new NullRebootRequestListener();

    // this is to prevent race conditions on startup between components from all the different managers
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private List<InitializationListener> initListeners = new ArrayList<InitializationListener>();
    private List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();
    private Object initListenersLock = new Object();
    private Object shutdownListenersLock = new Object();
    private boolean shuttingDown;
    private long shutdownStartTime;
    private boolean shutdownGracefully;

    /**
     * Returns the singleton instance.
     *
     * @return the plugin container
     */
    public static PluginContainer getInstance() {
        return INSTANCE;
    }

    private PluginContainer() {
        // for why we need to do this, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6727821
        try {
            Configuration.getConfiguration();
        } catch (Throwable t) {
        }
    }

    /**
     * Sets this plugin container's configuration which also provides configuration settings for all the internal
     * services.
     *
     * @param configuration
     */
    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Adds a listener that will be notified whenever an {@link AgentService} hosted within this plugin container is
     * started or stopped.
     *
     * @param listener
     */
    public void addAgentServiceLifecycleListener(AgentServiceLifecycleListener listener) {
        agentServiceListeners.add(listener);
    }

    /**
     * Removes the given listener such that it will no longer receive {@link AgentService} notifications.
     *
     * @param listener
     */
    public void removeAgentServiceLifecycleListener(AgentServiceLifecycleListener listener) {
        agentServiceListeners.remove(listener);
    }

    /**
     * Returns a remoter object that can be used to remote streams. If <code>null</code>, the plugin container will not
     * be able to remote streams to external clients, as in the case when the plugin container is not running inside an
     * agent (i.e. embedded mode).
     *
     * @return remoter the object that will prepare a stream to be accessed by remote clients (may be <code>null</code>)
     */
    public AgentServiceStreamRemoter getAgentServiceStreamRemoter() {
        return agentServiceStreamRemoter;
    }

    /**
     * Adds a remoter object that is responsible for remoting streams. If <code>null</code>, the plugin container will
     * not be able to remote streams to external clients, as in the case when the plugin container is not running inside
     * an agent (i.e. embedded mode).
     *
     * @param streamRemoter
     */
    public void setAgentServiceStreamRemoter(AgentServiceStreamRemoter streamRemoter) {
        agentServiceStreamRemoter = streamRemoter;
    }

    /**
     * Sets the given registrar as the object responsible for registering the plugin container with a remote server. If
     * <code>null</code>, the plugin container will not be considered running in an agent containing needing to be
     * registered with a remote server.
     *
     * @return the object that can be used to register this plugin container (may be <code>null</code>)
     */
    public AgentRegistrar getAgentRegistrar() {
        return agentRegistrar;
    }

    /**
     * Sets the given registrar as the object responsible for registering the plugin container with a remote server. If
     * <code>null</code>, the plugin container will not be considered running in an agent containing needing to be
     * registered with a remote server.
     *
     * @param registrar
     */
    public void setAgentRegistrar(AgentRegistrar registrar) {
        agentRegistrar = registrar;
    }

    /**
     * If the plugin container has been initialized and plugins have started work, this returns <code>true</code>.
     *
     * @return <code>true</code> if the plugin container was initialized and started; <code>false</code> otherwise
     */
    public boolean isStarted() {
        Lock lock = obtainReadLock();
        try {
            return started;
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * If the plugin container has been initialized, the plugins have started work, and the container is
     * not actively shutting down, this returns <code>true</code>.
     *
     * <p>Note! that there is no locking on this method. Therefore it returns quickly and is likely correct, but if
     * called outside of locking the return value is not guaranteed. As such, use this only as a lightweight check.</p>
     *
     * @return <code>true</code> if the plugin container was initialized, started and is not shutting down; <code>false</code> otherwise
     */
    public boolean isRunning() {
        return !shuttingDown && started;
    }

    /**
     * Initializes the plugin container which initializes all internal managers. Once initialized, all plugins will be
     * activated, metrics will begin getting collected and automatic discovery will start.
     *
     * <p>Note that if no configuration was {@link #setConfiguration(PluginContainerConfiguration) set} prior to this
     * method being called, a default configuration will be created and used.</p>
     *
     * <p>If the plugin container has already been initialized, this method does nothing and returns.</p>
     */
    public void initialize() {
        // this quick guard is OK but doesn't prevent several calls to initialize() from stacking up while waiting for the lock
        if (started) {
            log.info("Plugin container is already initialized.");
        }

        Lock lock = obtainWriteLock();
        try {
            // this guard prevents us from executing initialize logic multiple times in a row
            if (started) {
                return;
            }

            version = PluginContainer.class.getPackage().getImplementationVersion();
            log.info("Initializing Plugin Container" + ((version != null) ? (" v" + version) : "") + "...");

            if (configuration == null) {
                configuration = new PluginContainerConfiguration();
            }

            purgeTmpDirectoryContents();

            if (configuration.isStartManagementBean()) {
                mbean = new PluginContainerMBeanImpl(this);
                mbean.register();
            }

            ResourceContainer.initialize(configuration);

            pluginManager = new PluginManager();
            pluginComponentFactory = new PluginComponentFactory();
            inventoryManager = new InventoryManager();
            measurementManager = new MeasurementManager();
            configurationManager = new ConfigurationManager();
            operationManager = new OperationManager();
            resourceFactoryManager = new ResourceFactoryManager();
            contentManager = new ContentManager();
            eventManager = new EventManager(configuration);
            supportManager = new SupportManager();
            bundleManager = new BundleManager();
            driftManager = new DriftManager();

            startContainerService(pluginManager);
            startContainerService(pluginComponentFactory);
            startContainerService(inventoryManager);
            startContainerService(measurementManager);
            startContainerService(configurationManager);
            startContainerService(operationManager);
            startContainerService(resourceFactoryManager);
            startContainerService(contentManager);
            startContainerService(eventManager);
            startContainerService(supportManager);
            startContainerService(bundleManager);
            startContainerService(driftManager);

            started = true;

            log.info("Plugin Container initialized.");
        } finally {
            releaseLock(lock);
        }

        synchronized (initListenersLock) {
            if (started) {
                for (InitializationListener listener : initListeners) {
                    listener.initialized();
                }
            }
        }

        return;
    }

    /**
     * Shuts down the plugin container and all its internal services. If the plugin container has already been shutdown,
     * this method does nothing and returns.
     */
    public boolean shutdown() {
        // this quick guard is OK but doesn't prevent several calls to shutdown() from stacking up while waiting for the lock
        if (!isRunning()) {
            log.info("Plugin container is already shut down.");
        }

        // Don't use a write lock if we're going to wait for executors that are shutdown to terminate, otherwise we'll
        // end up deadlocked.
        Lock lock = (configuration.isWaitForShutdownServiceTermination()) ? obtainReadLock() : obtainWriteLock();
        try {
            // this guard prevents us from executing shutdown logic multiple times in a row
            if (!isRunning()) {
                return true;
            }

            shuttingDown = true;
            shutdownGracefully = true;
            shutdownStartTime = System.currentTimeMillis();

            log.info("Plugin container is being shutdown...");

            if (mbean != null) {
                mbean.unregister();
                mbean = null;
            }

            if (configuration.isWaitForShutdownServiceTermination()) {
                log.info("Plugin container shutdown will wait up to "
                    + configuration.getShutdownServiceTerminationTimeout()
                    + " seconds for shut down background threads to terminate.");
            }

            driftManager.shutdown();
            bundleManager.shutdown();
            supportManager.shutdown();
            eventManager.shutdown();
            contentManager.shutdown();
            resourceFactoryManager.shutdown();
            operationManager.shutdown();
            configurationManager.shutdown();
            measurementManager.shutdown();
            inventoryManager.shutdown();
            pluginComponentFactory.shutdown();
            pluginManager.shutdown();

            agentServiceListeners.clear();
            agentServiceListeners = new LinkedHashSet<AgentServiceLifecycleListener>();
            agentServiceStreamRemoter = null;
            agentRegistrar = null;

            purgeTmpDirectoryContents();

            ResourceContainer.shutdown();

            if (configuration.isWaitForShutdownServiceTermination()) {
                if (shutdownGracefully) {
                    long elapsedMillis = System.currentTimeMillis() - this.shutdownStartTime;
                    String elapsedTimeString = (elapsedMillis >= 1000) ? (elapsedMillis / 1000) + " seconds"
                        : "less than 1 second";
                    log.info("All shut down background threads have terminated (" + elapsedTimeString + " elapsed).");
                } else {
                    log.warn("Timed out after " + configuration.getShutdownServiceTerminationTimeout()
                        + " seconds while waiting for shut down background threads to terminate.");
                }
            }

            driftManager = null;
            bundleManager = null;
            supportManager = null;
            eventManager = null;
            contentManager = null;
            resourceFactoryManager = null;
            operationManager = null;
            configurationManager = null;
            measurementManager = null;
            inventoryManager = null;
            pluginComponentFactory = null;
            pluginManager = null;

            boolean isInsideAgent = configuration.isInsideAgent();
            configuration = null;

            started = false;
            shuttingDown = false;

            log.info("Plugin container is now shutdown.");

            // we typically do not want to do this if embedded somewhere other than the Agent VM
            if (isInsideAgent) {
                cleanMemory();
            }
        } finally {
            releaseLock(lock);
        }

        synchronized (shutdownListenersLock) {
            if (!started) {
                for (ShutdownListener listener : shutdownListeners) {
                    listener.shutdown();
                }
            }
        }

        return shutdownGracefully;
    }

    /**
     * Does things that help the garbage collector clean up the memory.
     * Only call this after the plugin container has been shutdown.
     */
    private void cleanMemory() {
        Introspector.flushCaches();
        LogFactory.releaseAll();

        // for why we need to do this, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6727821
        try {
            Configuration.setConfiguration(null);
        } catch (Throwable t) {
        }

        System.gc();
    }

    private void purgeTmpDirectoryContents() {
        try {
            FileUtils.purge(configuration.getTemporaryDirectory(), false);
        } catch (IOException e) {
            log.warn("Failed to purge contents of temporary directory - cause: " + e);
        }
    }

    private void startContainerService(ContainerService containerService) {
        log.debug("Starting and configuring container service: " + containerService.getClass().getSimpleName());

        containerService.setConfiguration(configuration);

        AgentService agentService = null;

        if (containerService instanceof AgentService) {
            agentService = (AgentService) containerService;

            agentService.setAgentServiceStreamRemoter(agentServiceStreamRemoter);

            for (AgentServiceLifecycleListener agentServiceListener : agentServiceListeners) {
                agentService.addLifecycleListener(agentServiceListener);
            }

            if (containerService instanceof ConfigurationManager) {
                ConfigurationManagerInitializer initializer = new ConfigurationManagerInitializer();
                initializer.initialize((ConfigurationManager) containerService);
            }
        }

        containerService.initialize();

        if (agentService != null) {
            agentService.notifyLifecycleListenersOfNewState(AgentService.LifecycleState.STARTED);
        }
    }

    private Lock obtainReadLock() {
        // try to obtain the lock, but if we can't get the lock in 60 seconds,
        // keep going. The PC is usually fine within seconds after its initializes,
        // so not getting this lock within 60 seconds probably isn't detrimental.
        // But if there is a deadlock, blocking forever here would be detrimental,
        // so we do not do it. We'll just log a warning and let the thread keep going.
        Lock readLock = rwLock.readLock();
        try {
            if (!readLock.tryLock(60L, TimeUnit.SECONDS)) {
                String msg = "There may be a deadlock in the plugin container.";
                //noinspection ThrowableInstanceNeverThrown
                log.warn(msg, new Throwable(msg));
                readLock = null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            readLock = null;
        }
        return readLock;
    }

    private Lock obtainWriteLock() {
        // try to obtain the lock, but if we can't get the lock in 60 seconds,
        // keep going. The PC is usually fine within seconds after its initializes,
        // so not getting this lock within 60 seconds probably isn't detrimental.
        // But if there is a deadlock, blocking forever here would be detrimental,
        // so we do not do it. We'll just log a warning and let the thread keep going.
        Lock writeLock = rwLock.writeLock();
        try {
            if (!writeLock.tryLock(60L, TimeUnit.SECONDS)) {
                String msg = "There may be a deadlock in the plugin container.";
                //noinspection ThrowableInstanceNeverThrown
                log.warn(msg, new Throwable(msg));
                writeLock = null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeLock = null;
        }
        return writeLock;
    }

    private void releaseLock(Lock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    // The methods below return the actual manager implementation objects.
    // Only those objects inside the plugin container should be calling these getXXXManager() methods.

    public PluginManager getPluginManager() {
        Lock lock = obtainReadLock();
        try {
            return pluginManager;
        } finally {
            releaseLock(lock);
        }
    }

    public PluginComponentFactory getPluginComponentFactory() {
        Lock lock = obtainReadLock();
        try {
            return pluginComponentFactory;
        } finally {
            releaseLock(lock);
        }
    }

    public InventoryManager getInventoryManager() {
        Lock lock = obtainReadLock();
        try {
            return inventoryManager;
        } finally {
            releaseLock(lock);
        }
    }

    public ConfigurationManager getConfigurationManager() {
        Lock lock = obtainReadLock();
        try {
            return configurationManager;
        } finally {
            releaseLock(lock);
        }
    }

    public MeasurementManager getMeasurementManager() {
        Lock lock = obtainReadLock();
        try {
            return measurementManager;
        } finally {
            releaseLock(lock);
        }
    }

    public OperationManager getOperationManager() {
        Lock lock = obtainReadLock();
        try {
            return operationManager;
        } finally {
            releaseLock(lock);
        }
    }

    public ResourceFactoryManager getResourceFactoryManager() {
        Lock lock = obtainReadLock();
        try {
            return resourceFactoryManager;
        } finally {
            releaseLock(lock);
        }
    }

    public ContentManager getContentManager() {
        Lock lock = obtainReadLock();
        try {
            return contentManager;
        } finally {
            releaseLock(lock);
        }
    }

    public EventManager getEventManager() {
        Lock lock = obtainReadLock();
        try {
            return eventManager;
        } finally {
            releaseLock(lock);
        }
    }

    public SupportManager getSupportManager() {
        Lock lock = obtainReadLock();
        try {
            return supportManager;
        } finally {
            releaseLock(lock);
        }
    }

    public BundleManager getBundleManager() {
        Lock lock = obtainReadLock();
        try {
            return bundleManager;
        } finally {
            releaseLock(lock);
        }
    }

    public DriftManager getDriftManager() {
        Lock lock = obtainReadLock();
        try {
            return driftManager;
        } finally {
            releaseLock(lock);
        }
    }

    // The methods below return the manager implementations wrapped in their remote client interfaces.
    // External clients to the plugin container should probably use these rather than the getXXXManager() methods.

    public DiscoveryAgentService getDiscoveryAgentService() {
        return getInventoryManager();
    }

    public ConfigurationAgentService getConfigurationAgentService() {
        return getConfigurationManager();
    }

    public MeasurementAgentService getMeasurementAgentService() {
        return getMeasurementManager();
    }

    public OperationAgentService getOperationAgentService() {
        return getOperationManager();
    }

    public ResourceFactoryAgentService getResourceFactoryAgentService() {
        return getResourceFactoryManager();
    }

    public ContentAgentService getContentAgentService() {
        return getContentManager();
    }

    public SupportAgentService getSupportAgentService() {
        return getSupportManager();
    }

    public BundleAgentService getBundleAgentService() {
        return getBundleManager();
    }

    public boolean isInsideAgent() {
        return (this.configuration != null && this.configuration.isInsideAgent());
    }

    public void setRebootRequestListener(RebootRequestListener listener) {
        rebootListener = listener;
    }

    public void notifyRebootRequestListener() {
        //BZ 828938: the thread needs to run as non-daemon so that it's allowed to complete cleanup in daemon mode
        Thread rebootThread = new Thread(new Runnable() {
            @Override
            public void run() {
                rebootListener.reboot();
            }
        });
        rebootThread.setName("Plugin Container Reboot Thread");
        rebootThread.setDaemon(false);
        rebootThread.start();
        try {
            rebootThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while rebooting agent after one or more resource types "
                + " have been marked for deletion. You may need to manually reboot the agent/plugin container to purge "
                + "stale types.");
        }
    }

    /**
     * Add the callback listener to notify when the plugin container is initialized. If this method is invoked and
     * the PC is already initialized, then <code>listener</code> will be invoked immediately.
     *
     * @param listener The callback object to notify. If a listener with the supplied name is registered, it
     * will be replaced with the newly supplied listner.
     */
    public void addInitializationListener(InitializationListener listener) {
        synchronized (initListenersLock) {
            initListeners.add(listener);

            if (started) {
                listener.initialized();
            }
        }
    }

    /**
     * Add the callback listener to notify when the plugin container is shutdown. Unlike
     * {@link #addInitializationListener(String, InitializationListener)} the <code>listener</code> will
     * not be invoked immediately if the PC is already shutdown.  It will only be invoked on future shutdowns.
     *
     * @param listener The callback object to notify. If a listener with the supplied name is registered, it
     * will be replaced with the newly supplied listener.
     */
    public void addShutdownListener(String name, ShutdownListener listener) {
        synchronized (shutdownListenersLock) {
            shutdownListeners.add(listener);
        }
    }

    /**
     * Initiate shutdown of the specified executor service. If the "waitForShutdownServiceTermination" plugin
     * container configuration property is "true" and the "shutdownServiceTerminationTimeout" has not already expired,
     * then wait for the service to terminate before returning. With the exception of test code, this method should only
     * be called during plugin container shutdown
     *
     * @param executorService the executor service to be shut down
     *
     * @return true if the executor service terminated, or false if it is still shutting down
     */
    public boolean shutdownExecutorService(ExecutorService executorService, boolean now) {
        if (now) {
            executorService.shutdownNow();
        } else {
            executorService.shutdown();
        }

        if ((configuration != null) && configuration.isWaitForShutdownServiceTermination()) {
            long elapsedShutdownTimeMillis = System.currentTimeMillis() - shutdownStartTime;
            long shutdownServiceTerminationTimeoutMillis = configuration.getShutdownServiceTerminationTimeout() * 1000;
            long remainingShutdownTimeoutMillis = shutdownServiceTerminationTimeoutMillis - elapsedShutdownTimeMillis;
            if ((remainingShutdownTimeoutMillis > 0) && !executorService.isTerminated()) {
                try {
                    logWaitingForExecutorServiceTerminationDebugMessage(executorService, remainingShutdownTimeoutMillis);
                    boolean executorTerminated = executorService.awaitTermination(remainingShutdownTimeoutMillis,
                        TimeUnit.MILLISECONDS);
                    if (!executorTerminated) {
                        String poolName = getPoolName(executorService);
                        if (poolName != null) {
                            int activeThreadsInPool = getActiveThreadCount(poolName);
                            log.warn("Timed out after [" + (remainingShutdownTimeoutMillis / 1000)
                                + "] seconds while waiting for all threads in pool [" + poolName + "] to terminate - ["
                                + activeThreadsInPool + "] threads in the pool are still active.");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Plugin container was interrupted while waiting for an executor service to terminate.");
                }
            } else {
                logNotWaitingForExecutorServiceTerminationDebugMessage(executorService);
            }
        }

        boolean executorTerminated = executorService.isTerminated();
        shutdownGracefully = (shutdownGracefully && executorTerminated);

        return executorTerminated;
    }

    private void logWaitingForExecutorServiceTerminationDebugMessage(ExecutorService executorService,
        long remainingShutdownTimeoutMillis) {
        if (log.isDebugEnabled()) {
            String poolName = getPoolName(executorService);
            if (poolName != null) {
                int activeThreadsInPool = getActiveThreadCount(poolName);
                log.debug("Waiting up to [" + (remainingShutdownTimeoutMillis / 1000) + "] seconds for ["
                    + activeThreadsInPool + "] threads in pool [" + poolName + "] to terminate...");
            }
        }
    }

    private void logNotWaitingForExecutorServiceTerminationDebugMessage(ExecutorService executorService) {
        String poolName = getPoolName(executorService);
        if (poolName != null) {
            int activeThreadsInPool = getActiveThreadCount(poolName);
            if (activeThreadsInPool > 0) {
                log.debug("Not waiting for all threads in pool [" + poolName
                    + "] to terminate, since the configured plugin container shutdown timeout has already elapsed - ["
                    + activeThreadsInPool + "] threads in the pool are still active.");
            }
        }
    }

    private static String getPoolName(ExecutorService executorService) {
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadFactory threadFactory = ((ThreadPoolExecutor) executorService).getThreadFactory();
            if (threadFactory instanceof LoggingThreadFactory) {
                return ((LoggingThreadFactory) threadFactory).getPoolName();
            }
        }
        return null;
    }

    private static int getActiveThreadCount(String poolName) {
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        int activeThreadsInPool = 0;
        for (Thread thread : allThreads) {
            if (thread.getName().startsWith(poolName + '-') && thread.isAlive()) {
                activeThreadsInPool++;
            }
        }
        return activeThreadsInPool;
    }

}
