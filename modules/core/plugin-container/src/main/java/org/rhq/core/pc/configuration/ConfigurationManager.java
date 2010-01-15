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
package org.rhq.core.pc.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages configuration of all resources across all plugins.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Jason Dobies
 */
public class ConfigurationManager extends AgentService implements ContainerService, ConfigurationAgentService {
    private final Log log = LogFactory.getLog(ConfigurationManager.class);

    private static final String SENDER_THREAD_POOL_NAME = "ConfigurationManager.threadpool";

    private static final int FACET_METHOD_TIMEOUT = 60 * 1000; // 60 seconds

    private static final ComparableVersion NON_LEGACY_VERSION = new ComparableVersion("2.1");

    private PluginContainerConfiguration pluginContainerConfiguration;
    private ScheduledExecutorService threadPool;

    private ComponentService componentService;

    private ConfigManagementFactory configMgmtFactory;

    public ConfigurationManager() {
        super(ConfigurationAgentService.class);
    }

    public void initialize() {
        LoggingThreadFactory threadFactory = new LoggingThreadFactory(SENDER_THREAD_POOL_NAME, true);
        threadPool = new ScheduledThreadPoolExecutor(1, threadFactory);

        ConfigurationCheckExecutor configurationChecker = new ConfigurationCheckExecutor(this,
            getConfigurationServerService(), PluginContainer.getInstance().getInventoryManager());

        if (pluginContainerConfiguration.getConfigurationDiscoveryPeriod() > 0
            && pluginContainerConfiguration.isInsideAgent()) {
            threadPool.scheduleAtFixedRate(configurationChecker, pluginContainerConfiguration
                .getConfigurationDiscoveryInitialDelay(), pluginContainerConfiguration
                .getConfigurationDiscoveryPeriod(), TimeUnit.SECONDS);
        }
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
    }

    public void setComponentService(ComponentService componentService) {
        this.componentService = componentService;
    }

    public void setConfigManagementFactory(ConfigManagementFactory factory) {
        configMgmtFactory = factory;
    }

    public void updateResourceConfiguration(ConfigurationUpdateRequest request) {
        ConfigurationServerService configurationServerService = getConfigurationServerService();

        try {
            ConfigManagement configMgmt = configMgmtFactory.getStrategy(request.getResourceId());
            ResourceType resourceType = componentService.getResourceType(request.getResourceId());

            Runnable runnable = new UpdateResourceConfigurationRunner(configurationServerService, resourceType,
                configMgmt, request);
            getThreadPool().submit(runnable);
        } catch (PluginContainerException e) {
            log.error("Failed to submit config update task. Cause: " + e);

            if (configurationServerService != null) {
                ConfigurationUpdateResponse error;

                error = new ConfigurationUpdateResponse(request.getConfigurationUpdateId(), request.getConfiguration(),
                    e);

                configurationServerService.completeConfigurationUpdate(error);
            }
        }

        return;
    }

    public ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(ConfigurationUpdateRequest request)
        throws PluginContainerException {
        ConfigurationUpdateResponse response;

        try {
            ConfigurationServerService configurationServerService = getConfigurationServerService();
            ResourceType resourceType = getResourceType(request.getResourceId());

            ConfigManagement configMgmt = new LegacyConfigManagement();
            configMgmt.setComponentService(componentService);

            Callable<ConfigurationUpdateResponse> runner;

            runner = new UpdateResourceConfigurationRunner(configurationServerService, resourceType, configMgmt,
                request);

            response = getThreadPool().submit(runner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Error occurred in delete resource thread", e);
        }

        return response;
    }

    public Configuration merge(Configuration configuration, int resourceId, boolean fromStructured)
        throws PluginContainerException {

        // TODO Throw an exception if the resource does not support structured and raw

        boolean daemonOnly = true;
        boolean onlyIfStarted = true;

        ResourceConfigurationFacet facet = componentService.getComponent(resourceId, ResourceConfigurationFacet.class,
            FacetLockType.READ, FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);

        if (fromStructured) {
            mergedStructuredIntoRaws(configuration, facet);
        } else {
            mergeRawsIntoStructured(configuration, facet);
        }

        return configuration;
    }

    private void mergeRawsIntoStructured(Configuration configuration, ResourceConfigurationFacet facet) {
        Configuration structuredConfig = facet.loadStructuredConfiguration();

        if (structuredConfig != null) {
            prepareConfigForMergeIntoStructured(configuration, structuredConfig);

            for (RawConfiguration rawConfig : configuration.getRawConfigurations()) {
                structuredConfig.addRawConfiguration(rawConfig);
                facet.mergeStructuredConfiguration(rawConfig, configuration);
            }
        }
    }

    private void prepareConfigForMergeIntoStructured(Configuration config, Configuration latestStructured) {
        config.getAllProperties().clear();
        for (Property property : latestStructured.getProperties()) {
            config.put(property);
        }
    }

    private void mergedStructuredIntoRaws(Configuration configuration, ResourceConfigurationFacet facet) {
        Set<RawConfiguration> rawConfigs = facet.loadRawConfigurations();

        if (rawConfigs == null) {
            return;
        }

        prepareConfigForMergeIntoRaws(configuration, rawConfigs);

        Queue<RawConfiguration> queue = new LinkedList<RawConfiguration>(rawConfigs);

        while (!queue.isEmpty()) {
            RawConfiguration originalRaw = queue.poll();
            RawConfiguration mergedRaw = facet.mergeRawConfiguration(configuration, originalRaw);

            if (mergedRaw != null) {
                updateRawConfig(configuration, originalRaw, mergedRaw);
            }
        }
    }

    private void prepareConfigForMergeIntoRaws(Configuration config, Set<RawConfiguration> latestRaws) {
        config.getRawConfigurations().clear();
        for (RawConfiguration raw : latestRaws) {
            config.addRawConfiguration(raw);
        }
    }

    private void updateRawConfig(Configuration configuration, RawConfiguration originalRaw, RawConfiguration mergedRaw) {

        configuration.removeRawConfiguration(originalRaw);
        configuration.addRawConfiguration(mergedRaw);
    }

    public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {

        ConfigManagement loadConfig = configMgmtFactory.getStrategy(resourceId);
        Configuration configuration = null;

        try {
            configuration = loadConfig.executeLoad(resourceId);
        } catch (Throwable t) {
            throw new PluginContainerException(createErrorMsg(resourceId, "An exception was thrown."), t);
        }

        if (configuration == null) {
            throw new PluginContainerException(createErrorMsg(resourceId, "returned a null Configuration."));
        }

        return configuration;
    }

    private String createErrorMsg(int resourceId, String msg) throws PluginContainerException {
        ResourceType resourceType = componentService.getResourceType(resourceId);

        return "Plugin Error: Resource Component for [" + resourceType.getName() + "] Resource with id [" + resourceId
            + "]: " + msg;
    }

    //    public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
    //        ResourceType resourceType = getResourceType(resourceId);
    //        ConfigurationFacet configComponent = getConfigurationFacet(resourceId, FacetLockType.READ);
    //        try {
    //            Configuration configuration = configComponent.loadResourceConfiguration();
    //            if (configuration == null) {
    //                throw new PluginContainerException("Plugin Error: Resource Component for [" + resourceType.getName()
    //                        + "] Resource with id [" + resourceId + "] returned a null Configuration.");
    //            }
    //
    //            // If the plugin didn't already set the notes field, set it to something useful.
    //            if (configuration.getNotes() == null) {
    //                configuration.setNotes("Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId);
    //            }
    //
    //            ConfigurationDefinition configurationDefinition = resourceType.getResourceConfigurationDefinition();
    //
    //            // Normalize and validate the config.
    //            ConfigurationUtility.normalizeConfiguration(configuration, configurationDefinition);
    //            List<String> errorMessages = ConfigurationUtility.validateConfiguration(configuration,
    //                configurationDefinition);
    //            for (String errorMessage : errorMessages) {
    //                log.warn("Plugin Error: Invalid " + resourceType.getName() + " Resource configuration returned by "
    //                    + resourceType.getPlugin() + " plugin - " + errorMessage);
    //            }
    //
    //            return configuration;
    //        } catch (Throwable t) {
    //            //noinspection ThrowableInstanceNeverThrown
    //            throw new PluginContainerException("Cannot load Resource configuration for [" + resourceId + "]",
    //                new WrappedRemotingException(t));
    //        }
    //    }

    /**
     * Returns a thread pool that this object will use when asychronously executing configuration operations on a
     * component.
     *
     * @return a thread pool this object will use
     */
    protected ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * This setter is here to provide a test hook
     *
     * @param threadPool A fake object such as a mock
     */
    void setThreadPool(ScheduledExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Given a resource ID, this obtains that resource's ConfigurationFacet interface. If it does not support the
     * configuration facet, an exception is thrown.
     *
     * @param  resourceId identifies the resource whose facet is to be returned
     * @param  lockType   how access to the facet is synchronized
     * @return the resource's configuration facet component
     *
     * @throws PluginContainerException on error
     */
    protected ConfigurationFacet getConfigurationFacet(int resourceId, FacetLockType lockType)
        throws PluginContainerException {
        boolean daemonThread = (lockType != FacetLockType.WRITE);
        return ComponentUtil.getComponent(resourceId, ConfigurationFacet.class, lockType, FACET_METHOD_TIMEOUT,
            daemonThread, true);
    }

    /**
     * Given a resource ID, this obtains that resource's type.
     *
     * @param  resourceId identifies the resource whose type is to be returned
     *
     * @return the resource's type, if known
     *
     * @throws PluginContainerException if cannot determine the resource's type
     */
    protected ResourceType getResourceType(int resourceId) throws PluginContainerException {
        return ComponentUtil.getResourceType(resourceId);
    }

    /**
     * If this manager can talk to a server-side {@link ConfigurationServerService}, a proxy to that service is
     * returned.
     *
     * @return the server-side proxy; <code>null</code> if this manager doesn't have a server to talk to
     */
    protected ConfigurationServerService getConfigurationServerService() {
        if (pluginContainerConfiguration.getServerServices() != null) {
            return pluginContainerConfiguration.getServerServices().getConfigurationServerService();
        }

        return null;
    }

    public void validate(Configuration configuration, int resourceId, boolean isStructured)
    throws PluginContainerException {
        boolean daemonOnly = true;
        boolean onlyIfStarted = true;
        ResourceConfigurationFacet facet = componentService.getComponent(resourceId, ResourceConfigurationFacet.class,
            FacetLockType.READ, FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
        if (isStructured) {
            return;
        } else {
            StringBuilder  errorMessage = null;
            try{
                for (RawConfiguration rawConfiguration : configuration.getRawConfigurations()) {
                    try {
                        facet.validateRawConfiguration(rawConfiguration);
                    } catch (IllegalArgumentException e) {
                        if (null == errorMessage){
                            errorMessage = new StringBuilder();
                        }
                        errorMessage.append("file " + rawConfiguration.getPath() +" failed validation with " + e.getMessage()+".  "  );                  
                    }
                }
            }catch(Throwable t){
                errorMessage.append("configuation validation failed with" + t.getMessage()+".  "  );                
                    }
                if (null != errorMessage){
                    throw new PluginContainerException(errorMessage.toString());
                }
            }
        }
    }

