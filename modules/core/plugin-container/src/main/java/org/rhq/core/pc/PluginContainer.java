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
package org.rhq.core.pc;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.pc.agent.AgentRegistrar;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceLifecycleListener;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.content.ContentManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.event.EventManager;
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
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class PluginContainer implements ContainerService {
    private static final PluginContainer INSTANCE = new PluginContainer();

    private static final Log log = LogFactory.getLog(PluginContainer.class);

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

    private Collection<AgentServiceLifecycleListener> agentServiceListeners = new LinkedHashSet<AgentServiceLifecycleListener>();
    private AgentServiceStreamRemoter agentServiceStreamRemoter = null;
    private AgentRegistrar agentRegistrar = null;

    /**
     * Returns the singleton instance.
     *
     * @return the plugin container
     */
    public static PluginContainer getInstance() {
        return INSTANCE;
    }

    private PluginContainer() {
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
        synchronized (INSTANCE) {
            return started;
        }
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
        synchronized (INSTANCE) {
            if (!started) {
                version = PluginContainer.class.getPackage().getImplementationVersion();
                log.info("Initializing Plugin Container" + ((version != null) ? (" v" + version) : "") + "...");

                if (configuration == null) {
                    configuration = new PluginContainerConfiguration();
                }

                purgeTmpDirectoryContents();

                pluginManager = new PluginManager();
                pluginComponentFactory = new PluginComponentFactory();
                inventoryManager = new InventoryManager();
                measurementManager = new MeasurementManager();
                configurationManager = new ConfigurationManager();
                operationManager = new OperationManager();
                resourceFactoryManager = new ResourceFactoryManager();
                contentManager = new ContentManager();
                eventManager = new EventManager();

                startContainerService(pluginManager);
                startContainerService(pluginComponentFactory);
                startContainerService(inventoryManager);
                startContainerService(measurementManager);
                startContainerService(configurationManager);
                startContainerService(operationManager);
                startContainerService(resourceFactoryManager);
                startContainerService(contentManager);
                startContainerService(eventManager);

                started = true;
            }
        }

        return;
    }

    /**
     * Shuts down the plugin container and all its internal services. If the plugin container has already been shutdown,
     * this method does nothing and returns.
     */
    public void shutdown() {
        synchronized (INSTANCE) {
            if (started) {
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
                agentServiceStreamRemoter = null;

                purgeTmpDirectoryContents();

                started = false;
            }
        }

        return;
    }

    private void purgeTmpDirectoryContents() {
        FileUtils.purge(configuration.getTemporaryDirectory(), false);
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
        }

        containerService.initialize();

        if (agentService != null) {
            agentService.notifyLifecycleListenersOfNewState(AgentService.LifecycleState.STARTED);
        }
    }

    // The methods below return the actual manager implementation objects.
    // Only those objects inside the plugin container should be calling these getXXXManager() methods.

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public PluginComponentFactory getPluginComponentFactory() {
        return pluginComponentFactory;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public MeasurementManager getMeasurementManager() {
        return measurementManager;
    }

    public OperationManager getOperationManager() {
        return operationManager;
    }

    public ResourceFactoryManager getResourceFactoryManager() {
        return resourceFactoryManager;
    }

    public ContentManager getContentManager() {
        return contentManager;
    }

    public EventManager getEventManager() {
        return eventManager;
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
}