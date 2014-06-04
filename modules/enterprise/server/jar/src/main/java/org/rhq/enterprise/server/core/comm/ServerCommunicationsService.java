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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;

import mazz.i18n.Logger;

import org.jboss.remoting.InvokerLocator;
import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;
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
import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is an MBean service that can be used to bootstrap the {@link ServiceContainer}. The main purpose for the
 * existence of this class is to bootstrap the comm services for the Server so remote CLI and Agent clients
 * can talk to the server.
 *
 * @author John Mazzitelli
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ServerCommunicationsService implements ServerCommunicationsServiceMBean {

    /**
     * A log for subclasses to be able to use.
     */
    private static final Logger LOG = ServerI18NFactory.getLogger(ServerCommunicationsService.class);

    private static final String DEFAULT_OVERRIDES_PROPERTIES_FILE = "server-comm-configuration-overrides.properties";

    /**
     * The remoting subsystem name that identifies remote API client messages.
     */
    private static final String REMOTE_API_SUBSYSTEM = "REMOTEAPI";
    private static final String WS_REMOTE_API_SUBSYSTEM = "WSREMOTEAPI";

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
     * The location of the configuration overrides properties file - can be a URL, file path or path within classloader.
     */
    private String m_overridesFile = DEFAULT_OVERRIDES_PROPERTIES_FILE;

    /**
     * The preferences node name that identifies the configuration set used to configure the services.
     */
    private String m_preferencesNodeName = ServerConfigurationConstants.DEFAULT_PREFERENCE_NODE;

    /**
     * Properties that will be used to override preferences found in the preferences node and the configuration
     * preferences file.
     */
    private Properties m_configurationOverrides = new Properties();

    /**
     * A list of known agents.
     */
    private KnownAgents m_knownAgents = new KnownAgents();

    /**
     * A map of clients of known agents. This effectively caches our clients so we don't create more than one sender
     * object to the same agent. The key is a string that is produced by {@link #getEndpointKey(String, int)}. Note
     * that this cache can be dirty in an HA environment.  When deleting an agent (due to a platform uninventory, for
     * example) this cache entry will only be cleaned on the primary HA node.  As such, we need to be careful to
     * ensure cache entries are correct, by doing a quick token match on the cache entry, when accessed.
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

                m_container.removeInvocationHandler(WS_REMOTE_API_SUBSYSTEM);
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

    @PostConstruct
    private void init() {
        m_mbs = JMXUtil.getPlatformMBeanServer();
        JMXUtil.registerMBean(this, OBJECT_NAME);
    }

    /**
     * Cleans up the internal state of this service.
     */
    @PreDestroy
    private void destroy() {
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
        m_mbs = null;
        m_container = null;
        m_configuration = null;
        m_configFile = ServerConfigurationConstants.DEFAULT_SERVER_CONFIGURATION_FILE;
        m_overridesFile = DEFAULT_OVERRIDES_PROPERTIES_FILE;
        m_preferencesNodeName = ServerConfigurationConstants.DEFAULT_PREFERENCE_NODE;
        m_configurationOverrides = new Properties();
        m_knownAgents.removeAllAgents();
        m_knownAgentClients.clear();
        m_started = false;
    }

    @Override
    public String getConfigurationFile() {
        return m_configFile;
    }

    @Override
    public void setConfigurationFile(String location) {
        if (location == null) {
            return;
        }
        m_configFile = replaceProperties(location);
    }

    @Override
    public String getPreferencesNodeName() {
        return m_preferencesNodeName;
    }

    @Override
    public void setPreferencesNodeName(String node) {
        m_preferencesNodeName = node;
    }

    @Override
    public String getConfigurationOverridesFile() {
        return m_overridesFile;
    }

    @Override
    public void setConfigurationOverridesFile(String location) {
        if (location == null) {
            m_overridesFile = null;
        } else {
            // substitute ${} replacement variables found in the location string
            m_overridesFile = replaceProperties(location);
        }
    }

    private void loadConfigurationOverridesFromFile() throws Exception {
        if (m_overridesFile == null) {
            return; // nothing to do
        }

        InputStream is = getFileInputStream(m_overridesFile);
        try {
            Properties props = new Properties();
            props.load(is);
            setConfigurationOverrides(props);
        } finally {
            is.close(); // if we got here, "is" will never be null
        }

        return;
    }

    private Properties getConfigurationOverrides() {
        return m_configurationOverrides;
    }

    private void setConfigurationOverrides(Properties overrides) {
        if (overrides == null) {
            return;
        }
        m_configurationOverrides.putAll(overrides);
    }

    @Override
    public ServerConfiguration reloadConfiguration() throws Exception {
        getPreferencesNode().clear();

        return prepareConfigurationPreferences();
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return m_configuration;
    }

    @Override
    public synchronized ServiceContainer safeGetServiceContainer() {
        if (null == m_container) {
            m_container = new ServiceContainer();
        }

        return m_container;
    }

    @Override
    public ServiceContainer getServiceContainer() {
        return m_container;
    }

    @Override
    public String getStartedServerEndpoint() {
        if (m_container == null) {
            return null;
        }

        return m_container.getServerEndpoint();
    }

    @Override
    public AgentClient getKnownAgentClient(Agent agent) {
        AgentClient agent_client;

        if (agent == null) {
            throw new IllegalStateException("Agent must be non-null - is a resource not assigned an agent?");
        }

        // first see if its already cached, if not we need to create one.
        synchronized (m_knownAgentClients) {
            String agent_address = agent.getAddress();
            int agent_port = agent.getPort();

            agent_client = m_knownAgentClients.get(getEndpointKey(agent_address, agent_port));

            // BZ1071994: Note that this cache can be dirty in an HA environment.  As such, we need to ensure cache
            // entries are correct. Do a quick token match on the cache entry, when accessed, and recreate as needed.
            if ((agent_client == null) || !agent_client.getAgent().getAgentToken().equals(agent.getAgentToken())) {
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

                // add the new cache entry, or replace the dirty cache entry (note that dirty cache entries don't
                // need to be destroyed as the new one is "logically" the same, but with updated auth info.)
                m_knownAgentClients.put(getEndpointKey(agent_address, agent_port), agent_client);
            }
        }

        return agent_client;
    }

    @Override
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
                if (sender_config.commandSpoolFileName != null) {
                    spool_file = new File(sender_config.dataDirectory, sender_config.commandSpoolFileName);
                    if (spool_file.exists()) {
                        // first truncate it, in case Windows is locking it; then try to delete
                        new FileOutputStream(spool_file, false).close();
                        spool_file.delete();
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to truncate/delete spool for deleted agent [" + agent + "]"
                    + " please manually remove the file: " + spool_file, e);
            }
        }

        return;
    }

    @Override
    public List<InvokerLocator> getAllKnownAgents() {
        return m_knownAgents.getAllAgents();
    }

    @Override
    public void addStartedAgent(Agent agent) {
        String endpoint = agent.getRemoteEndpoint();

        m_knownAgents.addAgent(endpoint);

        AgentClient client = getKnownAgentClient(agent);

        if (client != null) {
            client.startSending(); // we start it now because it allows it to start sending persisted guaranteed delivery messages
        }

        return;
    }

    @Override
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

    @Override
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

    @Override
    public Integer getGlobalConcurrencyLimit() {
        return getServiceContainer().getConfiguration().getGlobalConcurrencyLimit();
    }

    @Override
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

    @Override
    public Integer getInventoryReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_REPORT);
    }

    @Override
    public void setInventoryReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_REPORT, maxConcurrency, true);
    }

    @Override
    public Integer getAvailabilityReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_AVAILABILITY_REPORT);
    }

    @Override
    public void setAvailabilityReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_AVAILABILITY_REPORT, maxConcurrency, true);
    }

    @Override
    public Integer getInventorySyncConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_SYNC);
    }

    @Override
    public void setInventorySyncConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(DiscoveryServerService.CONCURRENCY_LIMIT_INVENTORY_SYNC, maxConcurrency, true);
    }

    @Override
    public Integer getContentReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            ContentServerService.CONCURRENCY_LIMIT_CONTENT_REPORT);
    }

    @Override
    public void setContentReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(ContentServerService.CONCURRENCY_LIMIT_CONTENT_REPORT, maxConcurrency, true);
    }

    @Override
    public Integer getContentDownloadConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            ContentServerService.CONCURRENCY_LIMIT_CONTENT_DOWNLOAD);
    }

    @Override
    public void setContentDownloadConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(ContentServerService.CONCURRENCY_LIMIT_CONTENT_DOWNLOAD, maxConcurrency, true);
    }

    @Override
    public Integer getMeasurementReportConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_REPORT);
    }

    @Override
    public void setMeasurementReportConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_REPORT, maxConcurrency, true);
    }

    @Override
    public Integer getMeasurementScheduleRequestConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST);
    }

    @Override
    public void setMeasurementScheduleRequestConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(MeasurementServerService.CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST, maxConcurrency,
            true);
    }

    @Override
    public Integer getConfigurationUpdateConcurrencyLimit() {
        return getServiceContainer().getConcurrencyManager().getConfiguredNumberOfPermitsAllowed(
            ConfigurationServerService.CONCURRENCY_LIMIT_CONFIG_UPDATE);
    }

    @Override
    public void setConfigurationUpdateConcurrencyLimit(Integer maxConcurrency) {
        setConcurrencyLimit(ConfigurationServerService.CONCURRENCY_LIMIT_CONFIG_UPDATE, maxConcurrency, true);
    }

    @Override
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

    @Override
    public Boolean isMaintenanceModeAtStartup() {
        return getMaintenanceModeAtStartup();
    }

    @Override
    public void setMaintenanceModeAtStartup(Boolean flag) {
        if (flag == null) {
            flag = Boolean.FALSE;
        }
        persistServerProperty(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY, flag.toString());
        System.setProperty(ServerManagerLocal.MAINTENANCE_MODE_ON_STARTUP_PROPERTY, flag.toString());
        return;
    }

    @Override
    public void clear() {
        getServiceContainerMetricsMBean().clear();
    }

    @Override
    public long getNumberDroppedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberDroppedCommandsReceived();
    }

    @Override
    public long getNumberNotProcessedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberNotProcessedCommandsReceived();
    }

    @Override
    public long getNumberFailedCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberFailedCommandsReceived();
    }

    @Override
    public long getNumberSuccessfulCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberSuccessfulCommandsReceived();
    }

    @Override
    public long getNumberTotalCommandsReceived() {
        return getServiceContainerMetricsMBean().getNumberTotalCommandsReceived();
    }

    @Override
    public long getAverageExecutionTimeReceived() {
        return getServiceContainerMetricsMBean().getAverageExecutionTimeReceived();
    }

    @Override
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
        loadConfigurationOverridesFromFile();
        Properties overrides = getConfigurationOverrides();
        if (overrides != null) {
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                // allow ${var} notation in the values so we can provide variable replacements in the values
                value = replaceProperties(value);

                // there are a few settings that normally aren't set but can be set at runtime - set them now
                value = determineSecurityAlgorithm(value);

                preferences_node.put(key, value);
                LOG.debug(ServerI18NResourceKeys.CONFIG_PREFERENCE_OVERRIDE, key,
                    (key.toLowerCase().contains("password")) ? "***" : value);
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
            LOG.error(ServerI18NResourceKeys.ERROR_SETTING_CONNECTOR_COMM_PREFS, e);

            ServiceContainerConfiguration scConfig = new ServiceContainerConfiguration(config.getPreferences());
            preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, scConfig
                .getConnectorBindAddress());
            preferences_node.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, String.valueOf(scConfig
                .getConnectorBindPort()));
        } finally {
            try {
                preferences_node.flush();
            } catch (Exception e) {
                LOG.error(ServerI18NResourceKeys.ERROR_FLUSHING_SERVER_PREFS, e);
            }
        }

        // let's make sure our configuration is upgraded to the latest schema
        ServerConfigurationUpgrade.upgradeToLatest(config.getPreferences());

        LOG.debug(ServerI18NResourceKeys.CONFIG_PREFERENCES, config);

        return config;
    }

    private String determineSecurityAlgorithm(String value) {
        String[] algorithmPropNames = new String[] {
            ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM,
            ServerConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM,
            ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_ALGORITHM,
            ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM };

        for (String algorithmPropName : algorithmPropNames) {
            // if the value is still the ${x} token, it means that setting was not set - let's set it now
            if (value.startsWith("${" + algorithmPropName)) {
                return System.getProperty("java.vendor", "").contains("IBM") ? "IbmX509" : "SunX509";
            }
        }

        return value;
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

        LOG.debug(ServerI18NResourceKeys.PREFERENCES_NODE_NAME, preferences_node_name);
        LOG.debug(ServerI18NResourceKeys.LOADING_CONFIG_FILE, file_name);

        InputStream config_file_input_stream = getFileInputStream(file_name);

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        try {
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
        } finally {
            // we know this is not null; if it was, we would have thrown the IOException earlier.
            config_file_input_stream.close();
        }
    }

    /**
     * Loads a file either from file system, from URL or from classloader. If file
     * can't be found, exception is thrown.
     * @param file_name the file whose input stream is to be returned
     * @return input stream of the file - will never be null
     * @throws IOException if the file input stream cannot be obtained
     */
    private InputStream getFileInputStream(String file_name) throws IOException {
        // first see if the file was specified as a path on the local file system
        InputStream config_file_input_stream = null;
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
        return config_file_input_stream;
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
            File installDir = LookupUtil.getCoreServer().getInstallDir();
            File binDir = new File(installDir, "bin");
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

    private String replaceProperties(String str) {
        if (str == null) {
            return null;
        }

        // keep replacing properties until no more ${} tokens are left that are replaceable
        String newValue = "";
        String oldValue = str;
        while (!newValue.equals(oldValue)) {
            oldValue = str;
            newValue = StringPropertyReplacer.replaceProperties(str);
            str = newValue;
        }
        return newValue;
    }
}
