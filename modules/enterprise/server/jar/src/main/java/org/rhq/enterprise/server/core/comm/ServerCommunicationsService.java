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
package org.rhq.enterprise.server.core.comm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import mazz.i18n.Logger;

import org.jboss.remoting.InvokerLocator;
import org.jboss.system.server.ServerConfig;
import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.GlobalConcurrencyLimitCommandListener;
import org.rhq.enterprise.communications.Ping;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.Calltime;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;
import org.rhq.enterprise.communications.util.ConcurrencyManager;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.agentclient.impl.AgentClientImpl;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.remote.RemoteSafeInvocationHandler;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is an MBean service that can be used to bootstrap the {@link ServiceContainer}. The main purpose for the
 * existence of this class is to bootstrap the comm services for the Server so remote CLI and Agent clients
 * can talk to the server.
 *
 * @author John Mazzitelli
 */
public class ServerCommunicationsService implements ServerCommunicationsServiceMBean, MBeanRegistration {

    /**
     * A log for subclasses to be able to use.
     */
    private static Logger LOG = ServerI18NFactory.getLogger(ServerCommunicationsService.class);

    /**
     * The remoting subsystem name that identifies remote API client messages.
     */
    private static final String REMOTE_API_SUBSYSTEM = "REMOTEAPI";

    /**
     * The MBeanServer where this bootstrap service is registered and where the server-side comm services will be
     * registered.
     */
    private MBeanServer m_mbs = null;

    /**
     * The container that manages the server-side services.
     */
    private ServiceContainer m_container = null;

    /**
     * The configuration of the server once started.
     */
    private ServerConfiguration m_configuration = null;

    /**
     * The location of the configuration file - can be a URL, file path or path within classloader.
     */
    private String m_configFile = ServerConfigurationConstants.DEFAULT_SERVER_CONFIGURATION_FILE;

    /**
     * The preferences node name that identifies the configuration set used to configure the services.
     */
    private String m_preferencesNodeName = ServerConfigurationConstants.DEFAULT_PREFERENCE_NODE;

    /**
     * Properties that will be used to override preferences found in the preferences node and the configuration
     * preferences file.
     */
    private Properties m_configurationOverrides = null;

    /**
     * A list of known agents.
     */
    private KnownAgents m_knownAgents = new KnownAgents();

    /**
     * A map of clients of known agents. This effectively caches our clients so we don't create more than one sender
     * object to the same agent. The key is a string that is produced by {@link #getEndpointKey(String, int)}.
     */
    private Map<String, AgentClient> m_knownAgentClients = new HashMap<String, AgentClient>();

    /**
     * Where server properties are persisted.
     */
    private File m_serverPropertiesFile = null;

    /**
     * Have the communication services been started
     */
    private boolean m_started = false;

    /**
     * The invocation handler used to process incoming remote API requests from things such as the CLI.
     */
    private RemoteSafeInvocationHandler m_remoteApiHandler;

    /**
     * Sets up some internal state.
     *
     * @see MBeanRegistration#preRegister(MBeanServer, ObjectName)
     */
    public ObjectName preRegister(MBeanServer mbs, ObjectName name) throws Exception {
        m_mbs = mbs;

        return name;
    }

    /**
     * This method does nothing - it is a no-op.
     *
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean arg0) {
        return; // NO-OP
    }

    /**
     * Actually starts the communications services. Once this returns, agents can communicate with the server. This
     * method exists (as opposed to "start()") because we do not want these communications services initialized until
     * after we are assured the EJBs are all deployed and we are ready to begin processing incoming agent messages.
     *
     * Synchronized to ensure that the start operation completes atomically.
     * 
     * @see ServerCommunicationsServiceMBean#startCommunicationServices()
     */
    public synchronized void startCommunicationServices() throws Exception {
        if (m_started == false) {
            // do not rely on the configuration that has been persisted
            // we are forcing the configuration file to be reloaded
            // if we ever want to change this so the server does pick up
            // persisted preferences, replace the following line with:
            // config = prepareConfigurationPreferences();
            ServerConfiguration config = reloadConfiguration();

            ServiceContainer container = (null == m_container) ? new ServiceContainer() : m_container;
            AutoDiscoveryListener listener = new ServerAutoDiscoveryListener(m_knownAgents);
            container.addDiscoveryListener(listener);

            container.start(config.getServiceContainerPreferences().getPreferences(), config
                .getClientCommandSenderConfiguration(), m_mbs);

            // now let's add our additional handler to support the remote clients (e.g. CLI)
            m_remoteApiHandler = new RemoteSafeInvocationHandler();
            m_remoteApiHandler.registerMetricsMBean(container.getMBeanServer());
            container.addInvocationHandler(REMOTE_API_SUBSYSTEM, m_remoteApiHandler);

            m_container = container;
            m_configuration = config;
            m_started = true;
        }

        return;
    }

    /**
     * Synchronized to ensure that the stop operation completes atomically.
     * 
     * @see ServerCommunicationsServiceMBean#stop()
     */
    public synchronized void stop() {
        if (m_container != null) {

            // be a good citizen and remove the handler, but ignore if any errors occur and keep going
            try {
                m_remoteApiHandler.unregisterMetricsMBean(m_container.getMBeanServer());
                m_container.removeInvocationHandler(REMOTE_API_SUBSYSTEM);
            } catch (Exception e) {
                LOG.warn(ServerI18NResourceKeys.REMOTE_API_REMOVAL_FAILURE, ThrowableUtil.getAllMessages(e));
            }

            m_container.shutdown();
            m_container = null;
            m_remoteApiHandler = null;
            m_started = false;
        }

        // stop all our clients - any messages flagged with guaranteed delivery will be spooled
        synchronized (m_knownAgentClients) {
            for (AgentClient client : m_knownAgentClients.values()) {
                client.stopSending();
            }

            m_knownAgentClients.clear();
        }

        return;
    }

    /**
     * @see ServerCommunicationsServiceMBean#isStarted()
     */
    public boolean isStarted() {
        return m_started;
    }

    /**
     * This method does nothing - it is a no-op.
     *
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        return; // NO-OP
    }

    /**
     * Cleans up the internal state of this service.
     *
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        m_mbs = null;
        m_container = null;
        m_configuration = null;
        m_configFile = ServerConfigurationConstants.DEFAULT_SERVER_CONFIGURATION_FILE;
        m_preferencesNodeName = ServerConfigurationConstants.DEFAULT_PREFERENCE_NODE;
        m_configurationOverrides = null;
        m_knownAgents.removeAllAgents();
        m_knownAgentClients.clear();
        m_started = false;

        return;
    }

    /*
     * @see ServerCommunicationsServiceMBean#getConfigurationFile()
     */
    public String getConfigurationFile() {
        return m_configFile;
    }

    /*
     * @see ServerCommunicationsServiceMBean#setConfigurationFile(String)
     */
    public void setConfigurationFile(String location) {
        m_configFile = StringPropertyReplacer.replaceProperties(location);
    }

    /*
     * @see ServerCommunicationsServiceMBean#getPreferencesNodeName()
     */
    public String getPreferencesNodeName() {
        return m_preferencesNodeName;
    }

    /*
     * @see ServerCommunicationsServiceMBean#setPreferencesNodeName(String)
     */
    public void setPreferencesNodeName(String node) {
        m_preferencesNodeName = node;
    }

    /*
     * @see ServerCommunicationsServiceMBean#getConfigurationOverrides()
     */
    public Properties getConfigurationOverrides() {
        return m_configurationOverrides;
    }

    /*
     * @see ServerCommunicationsServiceMBean#setConfigurationOverrides(Properties)
     */
    public void setConfigurationOverrides(Properties overrides) {
        m_configurationOverrides = overrides;
    }

    /*
     * @see ServerCommunicationsServiceMBean#reloadConfiguration()
     */
    public ServerConfiguration reloadConfiguration() throws Exception {
        getPreferencesNode().clear();

        return prepareConfigurationPreferences();
    }

    /*
     * @see ServerCommunicationsServiceMBean#getConfiguration()
     */
    public ServerConfiguration getConfiguration() {
        return m_configuration;
    }

    /*
     * @see ServerCommunicationsServiceMBean#safeGetServiceContainer()
     */
    public synchronized ServiceContainer safeGetServiceContainer() {
        if (null == m_container) {
            m_container = new ServiceContainer();
        }

        return m_container;
    }

    /*
     * @see ServerCommunicationsServiceMBean#getServiceContainer()
     */
    public ServiceContainer getServiceContainer() {
        return m_container;
    }

    /*
     * @see ServerCommunicationsServiceMBean#getStartedServerEndpoint()
     */
    public String getStartedServerEndpoint() {
        if (m_container == null) {
            return null;
        }

        return m_container.getServerEndpoint();
    }

    /*
     * @see ServerCommunicationsServiceMBean#getKnownAgentClient(org.jboss.on.domain.resource.Agent)
     */
    public AgentClient getKnownAgentClient(Agent agent) {
        AgentClient agent_client;

        if (agent == null) {
            throw new IllegalStateException("Agent must be non-null - is a resource not assigned an agent?");
        }

        // first see if its already cached, if not we need to create one
        synchronized (m_knownAgentClients) {
            String agent_address = agent.getAddress();
            int agent_port = agent.getPort();

            agent_client = m_knownAgentClients.get(getEndpointKey(agent_address, agent_port));

            if (agent_client == null) {
                String remote_uri;
                InvokerLocator locator = m_knownAgents.getAgent(agent_address, agent_port);

                if (locator != null) {
                    remote_uri = locator.getLocatorURI();
                } else {
                    // it isn't known via remoting auto-discovery, let's look at the domain object to figure out how to talk to it
                    remote_uri = agent.getRemoteEndpoint();

                    if (remote_uri == null) {
                        remote_uri = "socket://" + agent_address + ":" + agent_port;
                    }
                }

                ClientCommandSenderConfiguration client_config = getSenderConfiguration(agent);
                ClientCommandSender sender = getServiceContainer().createClientCommandSender(remote_uri, client_config);
                agent_client = new AgentClientImpl(agent, sender);

                m_knownAgentClients.put(getEndpointKey(agent_address, agent_port), agent_client);
            }
        }

        return agent_client;
    }

    /*
     * @see ServerCommunicationsServiceMBean#destroyKnownAgentClient(Agent)
     */
    public void destroyKnownAgentClient(Agent agent) {
        AgentClient agent_client;

        // first see if its already cached, if not we need to create one
        synchronized (m_knownAgentClients) {
            String agent_address = agent.getAddress();
            int agent_port = agent.getPort();

            agent_client = m_knownAgentClients.remove(getEndpointKey(agent_address, agent_port));

            if (agent_client != null) {
                agent_client.stopSending();
            }

            // purge the spool file, if it exists
            File spool_file = null;

            try {
                ClientCommandSenderConfiguration sender_config = getSenderConfiguration(agent);
                spool_file = new File(sender_config.dataDirectory, sender_config.commandSpoolFileName);
                if (spool_file.exists()) {
                    // first truncate it, in case Windows is locking it; then try to delete
                    new FileOutputStream(spool_file, false).close();
                    spool_file.delete();
                }
            } catch (Exception e) {
                LOG.warn("Failed to truncate/delete spool for deleted agent [" + agent + "]"
                    + " please manually remove the file: " + spool_file, e);
            }
        }

        return;
    }

    /*
     * @see ServerCommunicationsServiceMBean#getAllKnownAgents()
     */
    public List<InvokerLocator> getAllKnownAgents() {
        return m_knownAgents.getAllAgents();
    }

    /*
     * @see ServerCommunicationsServiceMBean#addStartedAgent(Agent)
     */
    public void addStartedAgent(Agent agent) {
        String endpoint = agent.getRemoteEndpoint();

        m_knownAgents.addAgent(endpoint);

        AgentClient client = getKnownAgentClient(agent);

        if (client != null) {
            client.startSending(); // we start it now because it allows it to start sending persisted guaranteed delivery messages
        }

        return;
    }

    /*
     * @see ServerCommunicationsServiceMBean#removeDownedAgent(String)
     */
    public void removeDownedAgent(String endpoint) {
        m_knownAgents.removeAgent(endpoint);

        // since we have been told the agent is down, clear its agent client from cache
        // and stop any messages currently being sent or queued to be sent to that agent
        AgentClient client;
        InvokerLocator locator;

        try {
            locator = new InvokerLocator(endpoint);
        } catch (MalformedURLException e) {
            // this should never happen - our endpoint URLs must always be valid
            throw new IllegalArgumentException(e);
        }

        synchronized (m_knownAgentClients) {
            client = m_knownAgentClients.remove(getEndpointKey(locator.getHost(), locator.getPort()));
        }

        if (client != null) {
            client.stopSending();
        }

        return;
    }

    /*
     * @see ServerCommunicationsServiceMBean#pingEndpoint(java.lang.String)
     */
    public boolean pingEndpoint(String endpoint, long timeoutMillis) {
        ClientCommandSender sender = null;
        boolean pinged = false;

        try {
            ServerConfiguration server_config = getConfiguration();
            ClientCommandSenderConfiguration client_config = server_config.getClientCommandSenderConfiguration();

            // prepare this sender to simply send a ping - do not need any advanced features
            client_config.commandSpoolFileName = null;
            client_config.enableQueueThrottling = false;
            client_config.enableSendThrottling = false;
            client_config.serverPollingIntervalMillis = -1;

            sender = getServiceContainer().createClientCommandSender(endpoint, client_config);
            sender.startSending();

            // create our own factory so we can customize the timeout
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            factory.setTimeout(timeoutMillis);
            Ping pinger = factory.getRemotePojo(Ping.class);
            pinger.ping("", null);
            return true;
        } catch (Exception e) {
            LOG.debug(ServerI18NResourceKeys.PING_FAILED, endpoint, ThrowableUtil.getAllMessages(e, true));
            pinged = false;
        } finally {
            if (sender != null) {
                sender.stopSending(false);
            }
        }

        return pinged;
    }

    public Integer getGlobalConcurrencyLimit() {
        return getServiceContainer().getConfiguration().getGlobalConcurrencyLimit();
    }

    public void setGlobalConcurrencyLimit(Integer maxConcurrency) {
        if (maxConcurrency == null) {
            maxConcurrency = Integer.valueOf(-1);
        }

        setConcurrencyLimit(GlobalConcurrencyLimitCommandListener.CONCURRENCY_LIMIT_NAME, maxConcurrency, false);
        persistServerProperty(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT, String
            .valueOf(maxConcurrency));
        getServiceContainer().getConfiguration().getPreferences().putInt(
            ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT, maxConcurrency);
    }

    public Integer getInventoryReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_REPORT);
    }

    public void setInventoryReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_REPORT, maxConcurrency, true);
    }

    public Integer getAvailabilityReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_AVAILABILITY_REPORT);
    }

    public void setAvailabilityReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_AVAILABILITY_REPORT, maxConcurrency, true);
    }

    public Integer getInventorySyncConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_SYNC);
    }

    public void setInventorySyncConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_SYNC, maxConcurrency, true);
    }

    public Integer getContentReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            ContentServerService.CONCURRENCY_LIMIT_CONTENT_REPORT);
    }

    public void setContentReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(ContentServerService.CONCURRENCY_LIMIT_CONTENT_REPORT, maxConcurrency, true);
    }

    public Integer getContentDownloadConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            ContentServerService.CONCURRENCY_LIMIT_CONTENT_DOWNLOAD);
    }

    public void setContentDownloadConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(ContentServerService.CONCURRENCY_LIMIT_CONTENT_DOWNLOAD, maxConcurrency, true);
    }

    public Integer getMeasurementReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_REPORT);
    }

    public void setMeasurementReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_REPORT, maxConcurrency, true);
    }

    public Integer getMeasurementScheduleRequestConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST);
    }

    public void setMeasurementScheduleRequestConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST, maxConcurrency,
            true);
    }

    public Boolean getMaintenanceModeAtStartup() {
        InputStream inputStream = null;
        Boolean flag;
        try {
            File file = getServerPropertiesFile();
            Properties props = new Properties();
            inputStream = new FileInputStream(file);
            props.load(inputStream);
            flag = Boolean.parseBoolean(props.getProperty(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY,
                "false"));
        } catch (Exception e) {
            LOG.error("Cannot read MM-on-startup property from file, will use the sysprop instead", e);
            flag = Boolean.getBoolean(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
        return flag;
    }

    public Boolean isMaintenanceModeAtStartup() {
        return getMaintenanceModeAtStartup();
    }

    public void setMaintenanceModeAtStartup(Boolean flag) {
        if (flag == null) {
            flag = Boolean.FALSE;
        }
        persistServerProperty(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY, flag.toString());
        System.setProperty(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY, flag.toString());
        return;
    }

    /*
     * @see ServiceContainerMetricsMBean#clear()
     */
    public void clear() {
        getServiceContainerMetricsMBean().clear();
    }

    /*
     * @see ServiceContainerMetricsMBean#getNumberDroppedCommandsReceived()
     */
    public long getNumberDroppedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberDroppedCommandsReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getNumberNotProcessedCommandsReceived()
     */
    public long getNumberNotProcessedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberNotProcessedCommandsReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getNumberFailedCommandsReceived()
     */
    public long getNumberFailedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberFailedCommandsReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getNumberSuccessfulCommandsReceived()
     */
    public long getNumberSuccessfulCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberSuccessfulCommandsReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getNumberTotalCommandsReceived()
     */
    public long getNumberTotalCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberTotalCommandsReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getAverageExecutionTimeReceived()
     */
    public long getAverageExecutionTimeReceived() {
        return getServiceContainerMetricsMBean().getAverageExecutionTimeReceived();
    }

    /*
     * @see ServiceContainerMetricsMBean#getCallTimeDataReceived()
     */
    public Map<String, Calltime> getCallTimeDataReceived() {
        return getServiceContainerMetricsMBean().getCallTimeDataReceived();
    }

    /**
     * Returns a proxy to the {@link ServiceContainerMetricsMBean} that we wrap. We'll pass through its metrics as part
     * of our interface. We do this because a plugin's service resource is a one-to-one with a single MBean and we want
     * all metrics under a single service.
     *
     * @return a proxy to the MBean
     */
    private ServiceContainerMetricsMBean getServiceContainerMetricsMBean() {
        MBeanServer mbs = getServiceContainer().getMBeanServer();
        Object proxy = MBeanServerInvocationHandler.newProxyInstance(mbs,
            ServiceContainerMetricsMBean.OBJECTNAME_METRICS, ServiceContainerMetricsMBean.class, false);
        return (ServiceContainerMetricsMBean) proxy;
    }

    /**
     * Returns the endpoint key based on the given host and port. This is the format of the key to
     * {@link #m_knownAgentClients}.
     *
     * @param  host the agent's host
     * @param  port the agent's port
     *
     * @return the endpoint's base information that is combined into a single string
     */
    private String getEndpointKey(String host, int port) {
        return host + ":" + port;
    }

    /**
     * Returns a configuration that can be used for the the client command sender that will talk to the given agent.
     *
     * @param  agent
     *
     * @return configuration for a {@link ClientCommandSender} that will talk to the given agent
     */
    private ClientCommandSenderConfiguration getSenderConfiguration(Agent agent) {
        ServerConfiguration server_config = getConfiguration();
        ClientCommandSenderConfiguration client_config = server_config.getClientCommandSenderConfiguration();

        // make sure it uses a unique spool file.  Senders cannot share spool files; each remote endpoint
        // must have its own spool file since the spool file contains command requests for a single, specific, agent.
        // (a null spool filename means we will never guarantee message delivery to agents)
        if (client_config.commandSpoolFileName != null) {
            File spool_file = new File(client_config.commandSpoolFileName);
            String file_name = spool_file.getName();
            String parent_path = spool_file.getParent();
            spool_file = new File(parent_path, agent.getName() + "_" + file_name);
            client_config.commandSpoolFileName = spool_file.getPath();
        }

        return client_config;
    }

    /**
     * This will ensure the server's configuration preferences are populated. If need be, the configuration file is
     * loaded and all overrides are overlaid on top of the preferences. The preferences are also upgraded to ensure they
     * conform to the latest configuration schema version.
     *
     * @return the server configuration
     *
     * @throws Exception
     */
    private ServerConfiguration prepareConfigurationPreferences() throws Exception {
        // load the configuration and start the service container with that configuration
        // only load the configuration file if there are no preferences yet - previously existing preferences are reused
        Preferences preferences_node = getPreferencesNode();
        ServerConfiguration config = new ServerConfiguration(preferences_node);

        if (config.getServerConfigurationVersion() == 0) {
            config = loadConfigurationFile();
        } else {
            LOG.debug(ServerI18NResourceKeys.PREFERENCES_ALREADY_EXIST, config.getPreferences());
        }

        // now that the configuration preferences are loaded, we need to override them with any bootstrap override properties
        Properties overrides = getConfigurationOverrides();
        if (overrides != null) {
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                // allow ${var} notation in the values so we can provide variable replacements in the values
                value = StringPropertyReplacer.replaceProperties(value);

                preferences_node.put(key, value);
                LOG.debug(ServerI18NResourceKeys.CONFIG_PREFERENCE_OVERRIDE, key, value);
            }
        }

        // finally, we need to set connector bind address and bind port if they are not set in the properties. Starting
        // in 1.1 these fields will typically not be set initially and will need to be overridden here with the
        // information defined in the server table for this server.
        try {
            Preferences preferences = config.getPreferences();
            Server server = LookupUtil.getServerManager().getServer();
            String bindAddress = preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "");
            int bindPort = preferences.getInt(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, -1);

            if ("".equals(bindAddress)) {
                bindAddress = server.getAddress();
                preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, bindAddress);
            }

            if (-1 == bindPort) {
                String transport = config.getPreferences().get(
                    ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
                    ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT);
                bindPort = (SecurityUtil.isTransportSecure(transport)) ? server.getSecurePort() : server.getPort();

                preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, String
                    .valueOf(bindPort));
            }
        } catch (Exception e) {
            LOG.error("Unable to set explicit connector address/port, using defaults: ", e);

            ServiceContainerConfiguration scConfig = new ServiceContainerConfiguration(config.getPreferences());
            preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, scConfig
                .getConnectorBindAddress());
            preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, String.valueOf(scConfig
                .getConnectorBindPort()));
        }

        // let's make sure our configuration is upgraded to the latest schema
        ServerConfigurationUpgrade.upgradeToLatest(config.getPreferences());

        LOG.debug(ServerI18NResourceKeys.CONFIG_PREFERENCES, config);

        return config;
    }

    /**
     * Loads the {@link #getConfigurationFile() configuration file}. The file location will first be checked for
     * existence on the file system and then as a URL. If it cannot be found, it will be assumed the file location
     * specifies the file as found in the current class loader and the file will be searched there. An exception is
     * thrown if the file cannot be found anywhere.
     *
     * @return the configuration that was loaded
     *
     * @throws IOException                       if failed to load the configuration file
     * @throws InvalidPreferencesFormatException if the configuration file had an invalid format
     * @throws BackingStoreException             if failed to access the preferences persistence store
     * @throws Exception                         on other failures
     */
    private ServerConfiguration loadConfigurationFile() throws Exception {
        String file_name = getConfigurationFile();
        String preferences_node_name = getPreferencesNodeName();
        InputStream config_file_input_stream = null;

        LOG.debug(ServerI18NResourceKeys.PREFERENCES_NODE_NAME, preferences_node_name);
        LOG.debug(ServerI18NResourceKeys.LOADING_CONFIG_FILE, file_name);

        // first see if the file was specified as a path on the local file system
        try {
            File config_file = new File(file_name);

            if (config_file.exists()) {
                config_file_input_stream = new FileInputStream(config_file);
            }
        } catch (Exception e) {
            // isn't really an error - this just isn't a file on the local file system
        }

        // see if the file was specified as a URL
        if (config_file_input_stream == null) {
            try {
                URL config_file = new URL(file_name);

                config_file_input_stream = config_file.openStream();
            } catch (Exception e) {
                // isn't really an error - this just isn't a URL
            }
        }

        // if neither a file path or URL, assume the config file can be found in the classloader
        if (config_file_input_stream == null) {
            config_file_input_stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file_name);
        }

        if (config_file_input_stream == null) {
            throw new IOException(LOG.getMsgString(ServerI18NResourceKeys.CANNOT_FIND_CONFIG_FILE, file_name));
        }

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        Preferences preferences_node = getPreferencesNode();
        ByteArrayOutputStream backup = new ByteArrayOutputStream();
        preferences_node.exportSubtree(backup);
        preferences_node.clear();

        // now load in the preferences
        try {
            Preferences.importPreferences(config_file_input_stream);

            if (new ServerConfiguration(preferences_node).getServerConfigurationVersion() == 0) {
                throw new IllegalArgumentException(LOG.getMsgString(
                    ServerI18NResourceKeys.BAD_NODE_NAME_IN_CONFIG_FILE, file_name, preferences_node_name));
            }
        } catch (Exception e) {
            // a problem occurred importing the config file; let's restore our original values
            try {
                Preferences.importPreferences(new ByteArrayInputStream(backup.toByteArray()));
            } catch (Exception e1) {
                // its conceivable the same problem occurred here as with the original exception (backing store problem?)
                // let's throw the original exception, not this one
            }

            throw e;
        }

        ServerConfiguration server_configuration = new ServerConfiguration(preferences_node);

        LOG.debug(ServerI18NResourceKeys.LOADED_CONFIG_FILE, file_name);

        return server_configuration;
    }

    /**
     * Returns the preferences for this server. The node returned is where all preferences are to be stored.
     *
     * @return the server preferences
     */
    private Preferences getPreferencesNode() {
        Preferences topNode = Preferences.userRoot().node(ServerConfigurationConstants.PREFERENCE_NODE_PARENT);
        Preferences preferencesNode = topNode.node(getPreferencesNodeName());

        return preferencesNode;
    }

    private File getServerPropertiesFile() {
        if (m_serverPropertiesFile == null) {
            ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(m_mbs, name, ServerConfig.class, false);

            File homeDir = ((ServerConfig) mbean).getHomeDir();
            File binDir = new File(homeDir.getParentFile(), "bin");
            m_serverPropertiesFile = new File(binDir, "rhq-server.properties");
        }

        return m_serverPropertiesFile;
    }

    private void persistServerProperty(String name, String value) {
        String filePath = getServerPropertiesFile().getAbsolutePath();
        PropertiesFileUpdate updater = new PropertiesFileUpdate(filePath);
        try {
            updater.update(name, value);
        } catch (IOException e) {
            String msgString = LOG.getMsgString(ServerI18NResourceKeys.SERVER_PROPERTY_SAVE_FAILED, name, value,
                filePath);
            throw new RuntimeException(msgString, e);
        }

        return;
    }

    private void setConcurrencyLimit(String limitName, Integer maxConcurrency, boolean persist) {
        if (maxConcurrency == null) {
            maxConcurrency = Integer.valueOf(-1);
        }

        if (persist) {
            persistServerProperty(limitName, String.valueOf(maxConcurrency));
        }

        ConcurrencyManager concurrencyManager = getServiceContainer().getConcurrencyManager();
        Map<String, Integer> limits = concurrencyManager.getAllConfiguredNumberOfPermitsAllowed();
        limits.put(limitName, maxConcurrency);

        getServiceContainer().setConcurrencyManager(new ConcurrencyManager(limits));

        LOG.info(ServerI18NResourceKeys.NEW_CONCURRENCY_LIMIT, limitName, maxConcurrency);
    }
}