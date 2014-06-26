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
package org.rhq.enterprise.server.core.comm;

import java.util.List;

import javax.management.ObjectName;

import org.jboss.remoting.InvokerLocator;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.server.agentclient.AgentClient;

/**
 * The interface to the MBean that manages the server-side communications services. Note that this also includes the
 * {@link ServiceContainerMetricsMBean} interface. In order to put all of our comm subsystem metrics and configuration
 * in a single service resource that our plugin can expose, we'll pass through those metrics from that MBean through our
 * interface, too.
 *
 * @author John Mazzitelli
 */
public interface ServerCommunicationsServiceMBean extends ServiceContainerMetricsMBean {
    /**
     * The object name that the MBean service will be registered under.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=ServerCommunications");

    /**
     * Returns the location of the configuration file where all preferences are defined for the server-side services.
     * The file location can be either a URL, a local file system path or a path within this service's classloader.
     *
     * @return configuration file location
     */
    String getConfigurationFile();

    /**
     * Defines the location of the configuration file where all preferences are defined for the server-side services.
     * The file location can be either a URL, a local file system path or a path within this service's classloader.
     *
     * @param location
     */
    void setConfigurationFile(String location);

    /**
     * Returns the preferences node name used to identify the configuration set to use. See the Java Preferences API for
     * the definition of a preference node name.
     *
     * @return the name of the Java Preferences node where the server's configuration lives
     */
    String getPreferencesNodeName();

    /**
     * Defines the preferences node name used to identify the configuration set to use. See the Java Preferences API for
     * the definition of a preference node name.
     *
     * <p>If this isn't specified, a suitable default will be used.</p>
     *
     * @param node the name of the Java Preferences node where the server's configuration will or already lives
     */
    void setPreferencesNodeName(String node);

    /**
     * This allows you to explicitly override configuration preferences found in the configuration file. If this isn't
     * set, then the settings specified by the configuration preferences file take effect as-is. If this is set,
     * this file is a properties file whose values will override the config file prefs.
     *
     * @param overridesFile configuration settings file that override the configuration preferences (may be<code>null</code>)
     */
    void setConfigurationOverridesFile(String overridesFile);

    String getConfigurationOverridesFile();

    /**
     * This will clear any and all current configuration preferences and then reload the
     * {@link #getConfigurationFile() configuration file}.
     *
     * @return the new server configuration
     *
     * @throws Exception if failed to clear and reload the configuration
     */
    ServerConfiguration reloadConfiguration() throws Exception;

    /**
     * Returns the configuration of the server-side communication components. If the configuration has not yet been
     * loaded, this will return <code>null</code>.
     *
     * @return the server configuration
     */
    ServerConfiguration getConfiguration();

    /**
     * Starts the communications services used by the server to send and receive messages to/from agents.
     *
     * @throws Exception if failed to start the server-side services successfully
     */
    void startCommunicationServices() throws Exception;

    /**
     * Stops the service which will stop all server-side services. Incoming messages will no longer be able to be
     * received after this method is called.
     *
     * @throws Exception if failed to stop the server-side services
     */
    void stop() throws Exception;

    /**
     * @return true if communication services have been started. False if not initialized or stopped.
     */
    boolean isStarted();

    /**
     * Returns the service container that houses all the server-side communications services. Will create
     * a service container for the comm services if one has not yet been created. This is typically only called
     * to add or remove listeners prior to comm service startup. Use isStarted() as necessary.
     *
     * @return service container object
     */
    ServiceContainer safeGetServiceContainer();

    /**
     * Returns the service container that houses all the server-side communications services. Will return <code>
     * null</code> if the serviceContainer has not been initialized. It is possible for this to be non-null
     * prior to server-side comm services initialization. Use isStarted() as necessary.
     *
     * @return service container object
     */
    ServiceContainer getServiceContainer();

    /**
     * If the server is currently listening for requests, this will return the endpoint the agents should use to connect
     * to it. If the server's underlying communications services are not deployed and running, this returns <code>
     * null</code>.
     *
     * @return the server's remote endpoint that agents use to connect to the server
     */
    String getStartedServerEndpoint();

    /**
     * Given an agent domain object, this will see if that agent is known by looking up its host and port from the list
     * of all {@link #getAllKnownAgents() known agents} and returns a client to that agent. An agent can become known if
     * it has been auto discovered.
     *
     * @param  agent the agent whose client is to be returned
     *
     * @return the agent client; will be <code>null</code> if that agent is not known
     */
    AgentClient getKnownAgentClient(Agent agent);

    /**
     * This will stop the client, remove it from the cache and clean up any resources used by the client. This is
     * normally called when an agent has been completely removed from inventory.
     *
     * @param agent the agent whose client is to be destroyed
     */
    void destroyKnownAgentClient(Agent agent);

    /**
     * This returns a list of all known agents that the server is in communications with.
     *
     * @return the list of agents (which may or may not be empty)
     */
    List<InvokerLocator> getAllKnownAgents();

    /**
     * Given an {@link Agent agent} (which includes its remote endpoint and name), this will see if it is not yet a
     * known agent and if so, will add it. This should be called only when you know an agent has started.
     *
     * @param agent the agent (which has the endpoint of the agent that has started and its name)
     */
    void addStartedAgent(Agent agent);

    /**
     * Given an {@link Agent#getRemoteEndpoint() agent remote endpoint}, this will see if it is a known agent and if so,
     * will remove it. This should be called only when you know an agent has gone down.
     *
     * @param endpoint the endpoint of the agent that has gone down
     */
    void removeDownedAgent(String endpoint);

    /**
     * This will perform an ad-hoc, low-level ping to the given endpoint. This is usually reserved for those callers
     * that need to confirm that an endpoint exists and can be communicated with, before being added as an official
     * agent endpoint.
     *
     * @param  endpoint      the endpoint to ping
     * @param  timeoutMillis the timeout, in milliseconds, to wait for the ping to return
     *
     * @return <code>true</code> if connectivity to the given endpoint was verified; <code>false</code> if for some
     *         reason the endpoint could not be pinged
     */
    boolean pingEndpoint(String endpoint, long timeoutMillis);

    /**
     * Gets the global concurrency limit. This is the amount of messages that the server will allow to be processed
     * concurrently - the type of message doesn't matter, this is the global maximum of any and all messages that are
     * allowed to be concurrently accepted.
     *
     * @return number of concurrent calls allowed
     */
    Integer getGlobalConcurrencyLimit();

    /**
     * Sets the global concurrency limit. This is the amount of messages that the server will allow to be processed
     * concurrently - the type of message doesn't matter, this is the global maximum of any and all messages that are
     * allowed to be concurrently accepted.
     *
     * @param maxConcurrency
     */
    void setGlobalConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for inventory reports. This is the amount of inventory reports that the server will
     * allow to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getInventoryReportConcurrencyLimit();

    /**
     * Sets the new concurrency limit for inventory reports. This new number is the amount of inventory reports that the
     * server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setInventoryReportConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for availability reports. This is the amount of availability reports that the server
     * will allow to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getAvailabilityReportConcurrencyLimit();

    /**
     * Sets the new concurrency limit for availability reports. This new number is the amount of availability reports
     * that the server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setAvailabilityReportConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for inventory sync requests. This is the amount of inventory sync requests that the
     * server will allow to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getInventorySyncConcurrencyLimit();

    /**
     * Sets the new concurrency limit for inventory sync requests. This new number is the amount of inventory sync
     * requests that the server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setInventorySyncConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for content reports. This is the amount of content reports that the server will allow
     * to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getContentReportConcurrencyLimit();

    /**
     * Sets the new concurrency limit for content reports. This new number is the amount of content reports that the
     * server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setContentReportConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for content downloads. This is the amount of downloads that the server will allow to
     * be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getContentDownloadConcurrencyLimit();

    /**
     * Sets the new concurrency limit for content downloads. This new number is the amount of downloads that the server
     * will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setContentDownloadConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for measurement reports. This is the amount of measurement reports that the server
     * will allow to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getMeasurementReportConcurrencyLimit();

    /**
     * Sets the new concurrency limit for measurement reports. This new number is the amount of measurement reports that
     * the server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setMeasurementReportConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for measurement reports. This is the amount of measurement reports that the server
     * will allow to be processed concurrently.
     *
     * @return number of concurrent calls allowed
     */
    Integer getMeasurementScheduleRequestConcurrencyLimit();

    /**
     * Sets the new concurrency limit for measurement schedule requests. This new number is the amount of measurement
     * schedule requests that the server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setMeasurementScheduleRequestConcurrencyLimit(Integer maxConcurrency);

    /**
     * Gets the concurrency limit for configuration updates. This is the amount of new configuration updates that the server
     * will allow to be processed concurrently. A configuration update is something the agent originates when it needs to
     * let the server know when a resource's configuration has changed.
     *
     * @return number of concurrent calls allowed
     */
    Integer getConfigurationUpdateConcurrencyLimit();

    /**
     * Sets the new concurrency limit for configuration updates. This new number is the amount of configuration updates
     * that the server will allow to be processed concurrently.
     *
     * @param maxConcurrency
     */
    void setConfigurationUpdateConcurrencyLimit(Integer maxConcurrency);

    /**
     * Returns <code>true</code> if the server should always start up in maintenance mode.
     * If <code>false</code>, the server will startup in the same state it was in when it
     * was shutdown.
     *
     * @return <code>true</code> if the server should always start up in MM
     */
    Boolean getMaintenanceModeAtStartup();

    /**
     * Same as {@link #getMaintenanceModeAtStartup()}.
     * @return <code>true</code> if server starts up in MM
     */
    Boolean isMaintenanceModeAtStartup();

    /**
     * Sets the flag to indicate if the server should always start up in maintenance mode.
     * See {@link #getMaintenanceModeAtStartup()} for more.
     *
     * @param flag
     */
    void setMaintenanceModeAtStartup(Boolean flag);
}