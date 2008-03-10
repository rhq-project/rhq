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
package org.rhq.core.pc.configuration;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.util.exception.WrappedRemotingException;

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

    private PluginContainerConfiguration pluginContainerConfiguration;
    private ExecutorService threadPool;

    public ConfigurationManager() {
        super(ConfigurationAgentService.class);
    }

    public void initialize() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        LoggingThreadFactory threadFactory = new LoggingThreadFactory(SENDER_THREAD_POOL_NAME, true);
        threadPool = new ThreadPoolExecutor(1, 100, // TODO [mazz]: do we want to make this configurable?
            1000, TimeUnit.MILLISECONDS, queue, threadFactory);
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateRequest request) {
        ConfigurationServerService configurationServerService = getConfigurationServerService();

        try {
            ResourceType resourceType = getResourceType(request.getResourceId());
            ConfigurationFacet configurationFacet = getConfigurationFacet(request.getResourceId(), FacetLockType.WRITE);

            Runnable runnable = new UpdateResourceConfigurationRunner(configurationServerService, resourceType,
                configurationFacet, request);
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
            ConfigurationFacet configurationFacet = getConfigurationFacet(request.getResourceId(), FacetLockType.WRITE);

            Callable<ConfigurationUpdateResponse> runner;

            runner = new UpdateResourceConfigurationRunner(configurationServerService, resourceType,
                configurationFacet, request);

            response = getThreadPool().submit(runner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Error occurred in delete resource thread", e);
        }

        return response;
    }

    public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
        ResourceType resourceType = getResourceType(resourceId);
        ConfigurationFacet configComponent = getConfigurationFacet(resourceId, FacetLockType.READ);
        try {
            Configuration configuration = configComponent.loadResourceConfiguration();
            if (configuration == null) {
                throw new PluginContainerException("Component returned a null configuration");
            }

            // If the plugin didn't already set the notes field, set it to something useful.
            if (configuration.getNotes() == null) {
                configuration.setNotes("resource config for " + resourceType + " resource w/ id " + resourceId);
            }

            ConfigurationDefinition configurationDefinition = resourceType.getResourceConfigurationDefinition();

            // Normalize and validate the config.
            ConfigurationUtility.normalizeConfiguration(configuration, configurationDefinition);
            List<String> errorMessages = ConfigurationUtility.validateConfiguration(configuration,
                configurationDefinition);
            for (String errorMessage : errorMessages) {
                log.warn("Plugin Error: Invalid " + resourceType.getName() + " resource configuration returned by "
                    + resourceType.getPlugin() + " plugin - " + errorMessage);
            }

            return configuration;
        } catch (Throwable t) {
            throw new PluginContainerException("Cannot load resource configuration for [" + resourceId + "]",
                new WrappedRemotingException(t));
        }
    }

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
     * Given a resource ID, this obtains that resource's ConfigurationFacet interface. If it does not support the
     * configuration facet, an exception is thrown.
     *
     * @param  resourceId identifies the resource whose facet is to be returned
     * @param  lockType   how access to the facet is synchronized
     *
     * @return the resource's configuration facet component
     *
     * @throws PluginContainerException
     */
    protected ConfigurationFacet getConfigurationFacet(int resourceId, FacetLockType lockType)
        throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, ConfigurationFacet.class, lockType, 0, true);
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
}