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

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.pc.plugin.PluginFinder;
import org.rhq.core.pc.plugin.RootPluginClassLoader;

/**
 * Configuration properties for the plugin container and all its internal managers.
 *
 * @author John Mazzitelli
 */
public class PluginContainerConfiguration {
    private static final String PROP_PREFIX = "rhq.pc.";
    private static final String PLUGIN_FINDER_PROP = PROP_PREFIX + "plugin-finder";
    private static final String PLUGIN_DIRECTORY_PROP = PROP_PREFIX + "plugin-directory";
    private static final String IS_INSIDE_AGENT_PROP = PROP_PREFIX + "is-inside-agent";
    private static final String CONTAINER_NAME_PROP = PROP_PREFIX + "container-name";
    private static final String DATA_DIRECTORY_PROP = PROP_PREFIX + "data-directory";
    private static final String TEMP_DIRECTORY_PROP = PROP_PREFIX + "temp-directory";
    private static final String ROOT_PLUGIN_CLASSLOADER_REGEX_PROP = PROP_PREFIX + "root-plugin-classloader-regex";

    // The following configuration settings have hardcoded default values. These defaults are publicly
    // accessible so the entity that embeds the plugin container can know what its default values are.

    // Inventory ----------

    private static final String SERVER_DISCOVERY_INITIAL_DELAY_PROP = PROP_PREFIX + "server-discovery-initial-delay";
    public static final long SERVER_DISCOVERY_INITIAL_DELAY_DEFAULT = 10L; // in seconds
    private static final String SERVER_DISCOVERY_PERIOD_PROP = PROP_PREFIX + "server-discovery-period";
    public static final long SERVER_DISCOVERY_PERIOD_DEFAULT = 15 * 60L; // in seconds
    private static final String SERVICE_DISCOVERY_INITIAL_DELAY_PROP = PROP_PREFIX + "service-discovery-initial-delay";
    public static final long SERVICE_DISCOVERY_INITIAL_DELAY_DEFAULT = 20L; // in seconds
    private static final String SERVICE_DISCOVERY_PERIOD_PROP = PROP_PREFIX + "service-discovery-period";
    public static final long SERVICE_DISCOVERY_PERIOD_DEFAULT = 24 * 60 * 60L; // in seconds
    private static final String RESOURCE_FACTORY_CORE_POOL_SIZE_PROP = PROP_PREFIX + "resource-factory-core-pool-size";
    public static final int RESOURCE_FACTORY_CORE_POOL_SIZE_DEFAULT = 1;
    private static final String RESOURCE_FACTORY_MAX_POOL_SIZE_PROP = PROP_PREFIX + "resource-factory-max-pool-size";
    public static final int RESOURCE_FACTORY_MAX_POOL_SIZE_DEFAULT = 100;
    private static final String RESOURCE_FACTORY_KEEP_ALIVE_PROP = PROP_PREFIX + "resource-factory-keep-alive";
    public static final int RESOURCE_FACTORY_KEEP_ALIVE_DEFAULT = 1000;

    // Availability ----------

    private static final String AVAILABILITY_SCAN_INITIAL_DELAY_PROP = PROP_PREFIX + "availability-scan-initial-delay";
    public static final long AVAILABILITY_SCAN_INITIAL_DELAY_DEFAULT = 5L; // in seconds
    private static final String AVAILABILITY_SCAN_PERIOD_PROP = PROP_PREFIX + "availability-scan-period";
    public static final long AVAILABILITY_SCAN_PERIOD_DEFAULT = 60L; // in seconds

    // Measurement ----------

    private static final String MEASUREMENT_COLLECTION_INITIAL_DELAY_PROP = PROP_PREFIX
        + "measurement-collection-initial-delay";
    public static final long MEASUREMENT_COLLECTION_INITIAL_DELAY_DEFAULT = 30L; // in seconds
    private static final String MEASUREMENT_COLLECTION_THREADCOUNT_PROP = PROP_PREFIX
        + "measurement-collection-threadpoolsize";
    public static final int MEASUREMENT_COLLECTION_THREADCOUNT_DEFAULT = 5;

    // Content ----------

    private static final String CONTENT_DISCOVERY_INITIAL_DELAY_PROP = PROP_PREFIX + "content-discovery-initial-delay";
    public static final long CONTENT_DISCOVERY_INITIAL_DELAY_DEFAULT = 60L; // in seconds
    private static final String CONTENT_DISCOVERY_PERIOD_PROP = PROP_PREFIX + "content-discovery-period";
    public static final long CONTENT_DISCOVERY_PERIOD_DEFAULT = 30L; // in seconds
    private static final String CONTENT_DISCOVERY_THREADCOUNT_PROP = PROP_PREFIX + "content-discovery-threadpoolsize";
    public static final int CONTENT_DISCOVERY_THREADCOUNT_DEFAULT = 10;

    // Configuration -------

    private static final String CONFIGURATION_DISCOVERY_INITIAL_DELAY_PROP = PROP_PREFIX
        + "configuration-discovery-initial-delay";
    public static final long CONFIGURATION_DISCOVERY_INITIAL_DELAY_DEFAULT = 300L; // in seconds
    private static final String CONFIGURATION_DISCOVERY_PERIOD_PROP = PROP_PREFIX + "configuration-discovery-period";
    public static final long CONFIGURATION_DISCOVERY_PERIOD_DEFAULT = 3600L; // in seconds

    // Operation ----------

    private static final String OPERATION_INVOKER_THREADCOUNT_PROP = PROP_PREFIX + "operation-invoker-threadpoolsize";
    public static final int OPERATION_INVOKER_THREADCOUNT_DEFAULT = 10;
    private static final String OPERATION_INVOCATION_TIMEOUT = PROP_PREFIX + "operation-invocation-timeout";
    public static final long OPERATION_INVOCATION_TIMEOUT_DEFAULT = 600L; // in seconds

    // Event -------

    private static final String EVENT_SENDER_INITIAL_DELAY_PROP = PROP_PREFIX + "event-sender-initial-delay";
    public static final long EVENT_SENDER_INITIAL_DELAY_DEFAULT = 30L; // in seconds
    private static final String EVENT_SENDER_PERIOD_PROP = PROP_PREFIX + "event-sender-period";
    public static final long EVENT_SENDER_PERIOD_DEFAULT = 30L; // in seconds
    private static final String EVENT_REPORT_MAX_PER_SOURCE_PROP = PROP_PREFIX + "event-report-max-per-source";
    public static final int EVENT_REPORT_MAX_PER_SOURCE_DEFAULT = 200;
    private static final String EVENT_REPORT_MAX_TOTAL_PROP = PROP_PREFIX + "event-report-max-total";
    public static final int EVENT_REPORT_MAX_TOTAL_DEFAULT = 400;

    /**
     * Contains all remote POJO services that the server exposes to the plugin container.
     */
    private ServerServices serverServices = null;

    /**
     * This is our hash map that contains the actual properties. We use a map (as opposed to individual data member
     * variables) to support a future enhancement by which our plugins can squirrel away their own custom global
     * properties here.
     */
    private Map<String, Object> configuration = new HashMap<String, Object>();

    /**
     * Returns the directory where all plugin jars can be found.
     *
     * @return plugin jar location
     */
    public File getPluginDirectory() {
        return (File) configuration.get(PLUGIN_DIRECTORY_PROP);
    }

    /**
     * Sets the location where the plugin jars can be found.
     *
     * @param pluginDir plugin jar location
     */
    public void setPluginDirectory(File pluginDir) {
        configuration.put(PLUGIN_DIRECTORY_PROP, pluginDir);
    }

    /**
     * Returns the directory where the plugins can squirrel away files with data they want to persist.
     *
     * @return data location
     */
    public File getDataDirectory() {
        return (File) configuration.get(DATA_DIRECTORY_PROP);
    }

    /**
     * Sets the directory where the plugins can squirrel away files with data they want to persist.
     *
     * @param dataDir data location
     */
    public void setDataDirectory(File dataDir) {
        configuration.put(DATA_DIRECTORY_PROP, dataDir);
    }

    /**
     * Returns the directory where the plugins can squirrel away temporary files. This directory may be deleted or
     * cleaned out while the plugin container is down, so plugins cannot assume data they write in this directory will
     * live past the lifetime of the current plugin container instance.
     *
     * @return temporary directory location
     */
    public File getTemporaryDirectory() {
        return (File) configuration.get(TEMP_DIRECTORY_PROP);
    }

    /**
     * Sets the directory where the plugins can squirrel away temporary files.
     *
     * @param tmpDir temporary directory location
     */
    public void setTemporaryDirectory(File tmpDir) {
        configuration.put(TEMP_DIRECTORY_PROP, tmpDir);
    }

    /**
     * Returns the regex that defines what classes the plugin container can provide to its
     * plugins from its own classloader and its parents. If not <code>null</code>, any classes
     * found in the plugin container's classloader (and its parent classloaders) that do
     * NOT match this regex will be hidden from the plugins. If <code>null</code>, there
     * are no hidden classes and any class the plugin container's classloader has is visible
     * to all plugins.
     *
     * @return regular expression (may be <code>null</code>)
     * 
     * @see RootPluginClassLoader
     */
    public String getRootPluginClassLoaderRegex() {
        return (String) configuration.get(ROOT_PLUGIN_CLASSLOADER_REGEX_PROP);
    }

    /**
     * Sets the regex that defines what classes the plugin container should hide from its plugins.
     * 
     * @param regex regular expression
     *
     * @see RootPluginClassLoader
     */
    public void setRootPluginClassLoaderRegex(String regex) {
        if (regex != null) {
            configuration.put(ROOT_PLUGIN_CLASSLOADER_REGEX_PROP, regex);
        } else {
            configuration.remove(ROOT_PLUGIN_CLASSLOADER_REGEX_PROP);
        }
    }

    /**
     * Returns the length of time, in seconds, before resource availability scans are started.
     *
     * @return number of seconds before availability scans start
     */
    public long getAvailabilityScanInitialDelay() {
        Long period = (Long) configuration.get(AVAILABILITY_SCAN_INITIAL_DELAY_PROP);
        return (period == null) ? AVAILABILITY_SCAN_INITIAL_DELAY_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, before resource availability scans first begin.
     *
     * @param period
     */
    public void setAvailabilityScanInitialDelay(long period) {
        configuration.put(AVAILABILITY_SCAN_INITIAL_DELAY_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, between each availability scan.
     *
     * @return number of seconds between each availability scan
     */
    public long getAvailabilityScanPeriod() {
        Long period = (Long) configuration.get(AVAILABILITY_SCAN_PERIOD_PROP);
        return (period == null) ? AVAILABILITY_SCAN_PERIOD_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, between each availability scan.
     *
     * @param period number of seconds between each availability scan
     */
    public void setAvailabilityScanPeriod(long period) {
        configuration.put(AVAILABILITY_SCAN_PERIOD_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, before measurements begin getting collected.
     *
     * @return number of seconds
     */
    public long getMeasurementCollectionInitialDelay() {
        Long period = (Long) configuration.get(MEASUREMENT_COLLECTION_INITIAL_DELAY_PROP);
        return (period == null) ? MEASUREMENT_COLLECTION_INITIAL_DELAY_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, before measurements begin getting collected.
     *
     * @param period
     */
    public void setMeasurementCollectionInitialDelay(long period) {
        configuration.put(MEASUREMENT_COLLECTION_INITIAL_DELAY_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, before auto-discovery of platforms/servers first begins.
     *
     * @return number of seconds before each platform/server auto-discovery starts
     */
    public long getServerDiscoveryInitialDelay() {
        Long period = (Long) configuration.get(SERVER_DISCOVERY_INITIAL_DELAY_PROP);
        return (period == null) ? SERVER_DISCOVERY_INITIAL_DELAY_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, before auto-discovery of platforms/servers first begins.
     *
     * @param period
     */
    public void setServerDiscoveryInitialDelay(long period) {
        configuration.put(SERVER_DISCOVERY_INITIAL_DELAY_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, between each auto-discovery of platforms/servers. In other words, an
     * auto-discovery will run every X seconds, where X is this method's return value.
     *
     * <p>If this value was never {@link #setServerDiscoveryPeriod}, the default will be
     * {@link #SERVER_DISCOVERY_PERIOD_DEFAULT}.</p>
     *
     * @return number of seconds between each platform/server auto-discovery run
     */
    public long getServerDiscoveryPeriod() {
        Long period = (Long) configuration.get(SERVER_DISCOVERY_PERIOD_PROP);
        return (period == null) ? SERVER_DISCOVERY_PERIOD_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, between each auto-discovery of platforms/servers. In other words, an
     * auto-discovery should be run every <code>period</code> seconds.
     *
     * @param period number of seconds between each platform/server auto-discovery run
     */
    public void setServerDiscoveryPeriod(long period) {
        configuration.put(SERVER_DISCOVERY_PERIOD_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, before auto-discovery of services first begins.
     *
     * @return number of seconds before each service auto-discovery starts
     */
    public long getServiceDiscoveryInitialDelay() {
        Long period = (Long) configuration.get(SERVICE_DISCOVERY_INITIAL_DELAY_PROP);
        return (period == null) ? SERVICE_DISCOVERY_INITIAL_DELAY_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, before auto-discovery of services first begins.
     *
     * @param period
     */
    public void setServiceDiscoveryInitialDelay(long period) {
        configuration.put(SERVICE_DISCOVERY_INITIAL_DELAY_PROP, Long.valueOf(period));
    }

    /**
     * Returns the length of time, in seconds, between each auto-discovery of services. In other words, an
     * auto-discovery will run every X seconds, where X is this method's return value.
     *
     * <p>If this value was never {@link #setServiceDiscoveryPeriod}, the default will be
     * {@link #SERVICE_DISCOVERY_PERIOD_DEFAULT}.</p>
     *
     * @return number of seconds between each platform/server auto-discovery run
     */
    public long getServiceDiscoveryPeriod() {
        Long period = (Long) configuration.get(SERVICE_DISCOVERY_PERIOD_PROP);
        return (period == null) ? SERVICE_DISCOVERY_PERIOD_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, between each auto-discovery of services. In other words, an auto-discovery
     * should be run every <code>period</code> seconds.
     *
     * @param period number of seconds between each service auto-discovery run
     */
    public void setServiceDiscoveryPeriod(long period) {
        configuration.put(SERVICE_DISCOVERY_PERIOD_PROP, Long.valueOf(period));
    }

    /**
     * Returns the instance of <code>PluginFinder</code> for the container to use to locate all plugins to be loaded.
     *
     * @return <code>PluginFinder</code> instance.
     */
    public PluginFinder getPluginFinder() {
        PluginFinder pluginFinder = (PluginFinder) configuration.get(PLUGIN_FINDER_PROP);
        return pluginFinder;
    }

    /**
     * Sets the <code>PluginFinder</code> instance used by the container.
     *
     * @param finder
     */
    public void setPluginFinder(PluginFinder finder) {
        configuration.put(PLUGIN_FINDER_PROP, finder);
    }

    /**
     * When measurement's are scheduled for collection, the collection will be performed by threads from a thread pool.
     * This defines the number of threads within that thread pool, effectively defining the number of measurements that
     * can be collected concurrently.
     *
     * @return the size of the thread pool
     */
    public int getMeasurementCollectionThreadPoolSize() {
        Integer size = (Integer) configuration.get(MEASUREMENT_COLLECTION_THREADCOUNT_PROP);
        return (size == null) ? MEASUREMENT_COLLECTION_THREADCOUNT_DEFAULT : size.intValue();
    }

    /**
     * Defines the number of threads that can concurrent collection measurements.
     *
     * @param size the new size of the threadpool
     */
    public void setMeasurementCollectionThreadPoolSize(int size) {
        configuration.put(MEASUREMENT_COLLECTION_THREADCOUNT_PROP, Integer.valueOf(size));
    }

    /**
     * Returns the length of time, in seconds, before auto-discovery of content first begins.
     *
     * @return initial delay in seconds
     */
    public long getContentDiscoveryInitialDelay() {
        Long delay = (Long) configuration.get(CONTENT_DISCOVERY_INITIAL_DELAY_PROP);
        return (delay == null) ? CONTENT_DISCOVERY_INITIAL_DELAY_DEFAULT : delay.longValue();
    }

    /**
     * Sets the length of time, in seconds, before auto-discovery of content first begins.
     *
     * @param delay time in seconds before first auto-discovery
     */
    public void setContentDiscoveryInitialDelay(long delay) {
        configuration.put(CONTENT_DISCOVERY_INITIAL_DELAY_PROP, delay);
    }

    /**
     * Returns the length of time, in seconds, between each auto-discovery of content. In other words, an auto-discovery
     * will run every X seconds, where X is this method's return value. If this value was never set via
     * {@link #setContentDiscoveryPeriod}, the default will be {@link #CONTENT_DISCOVERY_PERIOD_DEFAULT}.
     *
     * @return time in seconds between auto-discoveries
     */
    public long getContentDiscoveryPeriod() {
        Long period = (Long) configuration.get(CONTENT_DISCOVERY_PERIOD_PROP);
        return (period == null) ? CONTENT_DISCOVERY_PERIOD_DEFAULT : period.longValue();
    }

    /**
     * Sets the length of time, in seconds, between each auto-discovery of content. In other words, an auto-discovery
     * should be run every <code>period</code> seconds.
     *
     * @param period time in seconds between auto-discoveries
     */
    public void setContentDiscoveryPeriod(long period) {
        configuration.put(CONTENT_DISCOVERY_PERIOD_PROP, period);
    }

    /**
     * This defines the number of threads within the content discovery thread pool. If this value was never set via
     * {@link #setContentDiscoveryThreadPoolSize(int)}, the default will be
     * {@link #CONTENT_DISCOVERY_THREADCOUNT_DEFAULT}.
     *
     * @return number of threads
     */
    public int getContentDiscoveryThreadPoolSize() {
        Integer size = (Integer) configuration.get(CONTENT_DISCOVERY_THREADCOUNT_PROP);
        return (size == null) ? CONTENT_DISCOVERY_THREADCOUNT_DEFAULT : size.intValue();
    }

    /**
     * Returns the number of threads to use in the content discovery thread pool.
     *
     * @param size number of threads
     */
    public void setContentDiscoveryThreadPoolSize(int size) {
        configuration.put(CONTENT_DISCOVERY_THREADCOUNT_PROP, size);
    }

    public long getConfigurationDiscoveryInitialDelay() {
        Long delay = (Long) configuration.get(CONFIGURATION_DISCOVERY_INITIAL_DELAY_PROP);
        return (delay == null) ? CONFIGURATION_DISCOVERY_INITIAL_DELAY_DEFAULT : delay.longValue();
    }

    public void setConfigurationDiscoveryInitialDelay(long delay) {
        configuration.put(CONFIGURATION_DISCOVERY_INITIAL_DELAY_PROP, delay);
    }

    public long getConfigurationDiscoveryPeriod() {
        Long period = (Long) configuration.get(CONFIGURATION_DISCOVERY_PERIOD_PROP);
        return (period == null) ? CONFIGURATION_DISCOVERY_PERIOD_DEFAULT : period.longValue();
    }

    public void setConfigurationDiscoveryPeriod(long period) {
        configuration.put(CONFIGURATION_DISCOVERY_PERIOD_PROP, period);
    }

    /**
     * When an operation is to be invoked, the execution of the operation will be performed by threads from a thread
     * pool. This defines the number of threads within that thread pool, effectively defining the number of operations
     * that can be invoked concurrently.
     *
     * @return the size of the thread pool
     */
    public int getOperationInvokerThreadPoolSize() {
        Integer size = (Integer) configuration.get(OPERATION_INVOKER_THREADCOUNT_PROP);
        return (size == null) ? OPERATION_INVOKER_THREADCOUNT_DEFAULT : size.intValue();
    }

    /**
     * Defines the number of threads that can concurrent execute operations.
     *
     * @param size the new size of the threadpool
     */
    public void setOperationInvokerThreadPoolSize(int size) {
        configuration.put(OPERATION_INVOKER_THREADCOUNT_PROP, Integer.valueOf(size));
    }

    /**
     * When an operation invocation is made, this is the amount of time, in seconds, that it has to complete before it
     * is aborted. Note that a plugin is free to override this timeout by defining its own in the operation's metadata
     * found in the plugin descriptor.
     *
     * @return operation timeout, in seconds
     */
    public long getOperationInvocationTimeout() {
        Long timeout = (Long) configuration.get(OPERATION_INVOCATION_TIMEOUT);
        return (timeout == null) ? OPERATION_INVOCATION_TIMEOUT_DEFAULT : timeout.longValue();
    }

    /**
     * Sets the default timeout, specified in seconds.
     *
     * @param timeout
     */
    public void setOperationInvocationTimeout(long timeout) {
        configuration.put(OPERATION_INVOCATION_TIMEOUT, Long.valueOf(timeout));
    }

    public long getEventSenderInitialDelay() {
        Long delay = (Long) configuration.get(EVENT_SENDER_INITIAL_DELAY_PROP);
        return (delay == null) ? EVENT_SENDER_INITIAL_DELAY_DEFAULT : delay.longValue();
    }

    public void setEventSenderInitialDelay(long delay) {
        configuration.put(EVENT_SENDER_INITIAL_DELAY_PROP, delay);
    }

    public long getEventSenderPeriod() {
        Long period = (Long) configuration.get(EVENT_SENDER_PERIOD_PROP);
        return (period == null) ? EVENT_SENDER_PERIOD_DEFAULT : period.longValue();
    }

    public void setEventSenderPeriod(long period) {
        configuration.put(EVENT_SENDER_PERIOD_PROP, period);
    }

    public int getEventReportMaxPerSource() {
        Integer value = (Integer) configuration.get(EVENT_REPORT_MAX_PER_SOURCE_PROP);
        return (value == null) ? EVENT_REPORT_MAX_PER_SOURCE_DEFAULT : value.intValue();
    }

    public void setEventReportMaxPerSource(int value) {
        configuration.put(EVENT_REPORT_MAX_PER_SOURCE_PROP, value);
    }

    public int getEventReportMaxTotal() {
        Integer value = (Integer) configuration.get(EVENT_REPORT_MAX_TOTAL_PROP);
        return (value == null) ? EVENT_REPORT_MAX_TOTAL_DEFAULT : value.intValue();
    }

    public void setEventReportMaxTotal(int value) {
        configuration.put(EVENT_REPORT_MAX_TOTAL_PROP, value);
    }

    /**
     * Defines the base number of threads that can concurrently execute resource factory tasks.
     *
     * @param size new size of the thread pool
     */
    public void setResourceFactoryCoreThreadPoolSize(int size) {
        configuration.put(RESOURCE_FACTORY_CORE_POOL_SIZE_PROP, size);
    }

    /**
     * Returns the base number of threads in the resource factory task thread pool, effectively defining the number of
     * concurrent resource factory tasks that may take place at the same time.
     *
     * @return size of the thread pool
     */
    public int getResourceFactoryCoreThreadPoolSize() {
        Integer size = (Integer) configuration.get(RESOURCE_FACTORY_CORE_POOL_SIZE_PROP);
        return (size == null) ? RESOURCE_FACTORY_CORE_POOL_SIZE_DEFAULT : size.intValue();
    }

    /**
     * Defines the maximum number of threads that can concurrently execute resource factory tasks.
     *
     * @param size new maximum size
     */
    public void setResourceFactoryMaxThreadPoolSize(int size) {
        configuration.put(RESOURCE_FACTORY_MAX_POOL_SIZE_PROP, size);
    }

    /**
     * Returns the maximum number of threads in the resource factory task thread pool.
     *
     * @return maximum number of threads
     */
    public int getResourceFactoryMaxThreadPoolSize() {
        Integer size = (Integer) configuration.get(RESOURCE_FACTORY_MAX_POOL_SIZE_PROP);
        return (size == null) ? RESOURCE_FACTORY_MAX_POOL_SIZE_DEFAULT : size.intValue();
    }

    /**
     * Sets the resource factory thread pool keep alive time in milliseconds.
     *
     * @param time in milliseconds
     */
    public void setResourceFactoryKeepAliveTime(int time) {
        configuration.put(RESOURCE_FACTORY_KEEP_ALIVE_PROP, time);
    }

    /**
     * Returns the resource factory thread pool keep alive time in milliseconds.
     *
     * @return thread pool time in milliseconds
     */
    public int getResourceFactoryKeepAliveTime() {
        Integer time = (Integer) configuration.get(RESOURCE_FACTORY_KEEP_ALIVE_PROP);
        return (time == null) ? RESOURCE_FACTORY_KEEP_ALIVE_DEFAULT : time.intValue();
    }

    /**
     * This is the name of the plugin container, as assigned to it by the software component that is embedding the
     * plugin container. This is usually, but doesn't have to be, the fully qualified domain name of the platform where
     * the plugin container is running. Note that if this container name was never explicitly set, the default will be
     * the canonical host name of the platform where this VM is running - and if that default cannot be determine,
     * <code>null</code> will be returned.
     *
     * @return a name that can be used to uniquely identify the plugin container (will be <code>null</code> if the name
     *         is not explicitly configured and it cannot be determined at runtime)
     */
    public String getContainerName() {
        String fqdn = (String) configuration.get(CONTAINER_NAME_PROP);

        if (fqdn == null) {
            try {
                fqdn = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (Exception e) {
                fqdn = null;
            }
        }

        return fqdn;
    }

    /**
     * Defines a name that the container can be known as. This is usually the fully qualified domain name that the
     * platform on which this plugin container is running. However, this does not have to be the case; this name can be
     * anything.
     *
     * @param name the plugin container's name
     */
    public void setContainerName(String name) {
        configuration.put(CONTAINER_NAME_PROP, name);
    }

    /**
     * Returns the object that contains all the remote POJOs that can be used to send data to the remote server. If
     * <code>null</code>, the plugin container is detached from any server and must operate independently.
     *
     * @return the server-exposed interfaces that can be remotely accessed by the plugin container (may be <code>
     *         null</code>)
     */
    public ServerServices getServerServices() {
        return serverServices;
    }

    /**
     * If the plugin container is contained in an embeddor that has access to a remote server, that embeddor will call
     * this method in order to provide the plugin container with all the remote POJO interfaces exposed by that server.
     * If <code>null</code> is passed in, the plugin container is be detached from any server.
     *
     * @param serverServices the server-exposed interfaces that can be remotely accessed by the plugin container (may be
     *                       <code>null</code>)
     */
    public void setServerServices(ServerServices serverServices) {
        this.serverServices = serverServices;
    }

    /**
     * Returns whether or not the plugin container is running inside an agent, which means it is running external to any
     * managed product.
     *
     * @return <code>true</code> if the container is deployed inside an external agent process; <code>false</code> if
     *         the plugin container is embedded directly in a managed product
     */
    public boolean isInsideAgent() {
        Object val = configuration.get(IS_INSIDE_AGENT_PROP);

        if (val == null) {
            return false;
        }

        return ((Boolean) val).booleanValue();
    }

    /**
     * Sets the flag to indicate if the plugin container is inside an external agent or if its embedded in a managed
     * product.
     *
     * @param flag
     */
    public void setInsideAgent(boolean flag) {
        configuration.put(IS_INSIDE_AGENT_PROP, Boolean.valueOf(flag));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return configuration.toString();
    }
}