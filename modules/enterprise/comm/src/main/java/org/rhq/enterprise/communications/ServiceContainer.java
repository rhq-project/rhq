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
package org.rhq.enterprise.communications;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import mazz.i18n.Logger;

import org.jboss.remoting.InvalidConfigurationException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.security.SSLServerSocketFactoryService;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.coyote.ssl.RemotingSSLImplementation;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.JBossRemotingRemoteCommunicator;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;
import org.rhq.enterprise.communications.command.impl.remotepojo.server.RemotePojoInvocationCommandService;
import org.rhq.enterprise.communications.command.impl.stream.RemoteInputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.server.RemoteInputStreamCommandService;
import org.rhq.enterprise.communications.command.impl.stream.server.RemoteOutputStreamCommandService;
import org.rhq.enterprise.communications.command.server.CommandAuthenticator;
import org.rhq.enterprise.communications.command.server.CommandListener;
import org.rhq.enterprise.communications.command.server.CommandProcessor;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceDirectory;
import org.rhq.enterprise.communications.command.server.CommandServiceId;
import org.rhq.enterprise.communications.command.server.KeyProperty;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.ConcurrencyManager;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * This is the main container that manages the communications services required by the server to accept incoming command
 * requests.
 *
 * @author John Mazzitelli
 */
public class ServiceContainer {
    /**
     * The JMX domain where all the services are registered.
     */
    public static final String JMX_DOMAIN = "rhq.remoting";

    /**
     * The default name of the network registry service.
     */
    public static final ObjectName OBJECTNAME_CMDSERVICE_DIRECTORY = ObjectNameFactory.create(JMX_DOMAIN
        + ":type=directory");

    /**
     * The default name of the network registry service.
     */
    public static final ObjectName OBJECTNAME_NETWORK_REGISTRY = ObjectNameFactory.create(JMX_DOMAIN
        + ":type=networkregistry");

    /**
     * The default name of the multicast detector service.
     */
    public static final ObjectName OBJECTNAME_MULTICAST_DETECTOR = ObjectNameFactory.create(JMX_DOMAIN
        + ":type=detector,transport=multicast");

    /**
     * The default name of the multicast detector service.
     */
    public static final ObjectName OBJECTNAME_CONNECTOR = ObjectNameFactory.create(JMX_DOMAIN + ":type=connector");

    /**
     * The default name of the SSL server socket factory service.
     */
    public static final ObjectName OBJECTNAME_SSL_SERVERSOCKET_FACTORY = ObjectNameFactory.create(JMX_DOMAIN
        + ":type=sslserversocketfactory");

    /**
     * This is the JBoss/Remoting subsystem that our invoker handlers/command services will be listening to.
     */
    private static final String SUBSYSTEM = "RHQ";

    /**
     * This is what all command service instances will be named with the exception that all names must also have an
     * additional key property which is an index number to make each name unique ({@link KeyProperty#ID}). This object
     * name predefines the default subsystem name.
     */
    private static final ObjectName OBJECTNAME_CMDSERVICE_INSTANCE = ObjectNameFactory.create(JMX_DOMAIN + ":"
        + KeyProperty.TYPE + "=" + KeyProperty.TYPE_COMMAND + "," + KeyProperty.SUBSYSTEM + "=" + SUBSYSTEM);

    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ServiceContainer.class);

    /**
     * The MBeanServer that will contain all of our services.
     */
    private MBeanServer m_mbs;

    /**
     * If the <code>m_mbs</code> MBeanServer that contains all of our services was
     * {@link #findOrCreateMBeanServer(String) created by this container}, this will be <code>true</code>. If the
     * MBeanServer was given to the container (either via the
     * {@link #start(Preferences, ClientCommandSenderConfiguration, MBeanServer)} method or it already existed and was
     * {@link #findOrCreateMBeanServer(String) found by its name}, this will be <code>false</code>.
     */
    private boolean m_mbsCreated;

    /**
     * The configuration that was used to initialize the container.
     */
    private ServiceContainerConfiguration m_configuration;

    /**
     * The Network Registry service which is used to register known servers and emit notifications about servers coming
     * on and offline.
     */
    private NetworkRegistry m_registry;

    /**
     * The detector service that can listen for new servers coming online and old servers going offline.
     */
    private MulticastDetector m_detector;

    /**
     * The directory that lists all known command services.
     */
    private CommandServiceDirectory m_commandServiceDirectory;

    /**
     * The special command service that provides the server-side functionality needed to remote POJOs. We will add POJOs
     * to this command service in order to remote them.
     */
    private RemotePojoInvocationCommandService m_remotePojoCommandService;

    /**
     * The special command service that provides the server-side functionality needed to remote input streams. We will
     * add input streams to this command service in order to remote them.
     */
    private RemoteInputStreamCommandService m_remoteInputStreamCommandService;

    /**
     * The special command service that provides the server-side functionality needed to remote output streams. We will
     * add output streams to this command service in order to remote them.
     */
    private RemoteOutputStreamCommandService m_remoteOutputStreamCommandService;

    /**
     * The server connector - this is the service that actually accepts incoming messages.
     */
    private Connector m_connector;

    /**
     * The factory service that will be used to create secure server sockets. Will be <code>null</code> if the
     * connector's transport does not require a secure protocol.
     */
    private SSLServerSocketFactoryService m_sslServerSocketFactoryService;

    /**
     * The listener of network notifications that will receive notifications when new servers are discovered or old
     * servers have died.
     */
    private ServiceContainerNetworkNotificationListener m_discoveryListener;

    /**
     * If this server-side object needs to act as a client and send commands to an external server, this is the
     * configuration that should be used to configure the client.
     */
    private ClientCommandSenderConfiguration m_clientConfiguration;

    /**
     * This is the current index that will be assigned to the next service to be added. It is a constantly growing
     * number, incremented each time a new service has been added to the system, thus making each service name unique.
     */
    private AtomicLong m_servicesIndex;

    /**
     * Listeners that will be added to the {@link CommandProcessor} when started.
     */
    private final List<CommandListener> m_commandListeners;

    /**
     * The listeners that will receive notifications when new {@link ClientCommandSender sender} objects are created by
     * this service container.
     */
    private final List<ServiceContainerSenderCreationListener> m_senderCreationListeners;

    /**
     * This is a manager that can be used by objects that either own, use or have access to {@link ServiceContainer}
     * object. It can be used, for example, by command services to restrict concurrent access. The concurrency manager
     * is not used by this object directly; it here just as a place to hold it so other objects (like command services
     * and management MBeans) can get to it.
     */
    private ConcurrencyManager m_concurrencyManager;

    /**
     * Custom data is a way to share information across disparate components so long as those components
     * have access to this service container object. This data is never used by the service container - it
     * can be null, empty, or chock full of data. No validation is performed on this data.
     */
    private Map<String, Object> m_customData;

    /**
     * Private to prevent external instantiation.
     */
    public ServiceContainer() {
        m_discoveryListener = new ServiceContainerNetworkNotificationListener();
        m_senderCreationListeners = new Vector<ServiceContainerSenderCreationListener>(); // synchronized
        m_commandListeners = new ArrayList<CommandListener>();
        m_customData = new HashMap<String, Object>();
    }

    /**
     * Returns custom data identified with the given key. If no data exists that is identified by that key,
     * null is returned.
     *
     * @param key identifies the custom data to return - if null, this method returns null
     * @return the custom data or null if there is no custom data associated with the given key
     */
    public Object getCustomData(String key) {
        if (key == null) {
            return null;
        }
        synchronized (this.m_customData) {
            return this.m_customData.get(key);
        }
    }

    /**
     * Stores the given custom data to be identified with the given key. You can retrieve this data
     * via {@link #getCustomData(String)}.
     *
     * @param key identifies the custom data to store - if null, this method does nothing and returns.
     * @param data custom data to add - it will be associated with the given key.
     */
    public void addCustomData(String key, Object data) {
        if (key == null) {
            return;
        }
        synchronized (this.m_customData) {
            if (data != null) {
                this.m_customData.put(key, data);
            } else {
                this.m_customData.remove(key);
            }
        }
        return;
    }

    /**
     * Returns the configuration preferences that were used when the service container was
     * {@link #start(Preferences, ClientCommandSenderConfiguration) started}.
     *
     * <p>If this container is not started (e.g. it has been {@link #shutdown()}), this will return <code>
     * null</code>.</p>
     *
     * @return configuration properties
     */
    public ServiceContainerConfiguration getConfiguration() {
        return m_configuration;
    }

    /**
     * Returns the MBeanServer that is housing all the services.
     *
     * <p>If this container is not initialized (e.g. it has been {@link #shutdown()}), this will return <code>
     * null</code>.</p>
     *
     * @return mbs
     */
    public MBeanServer getMBeanServer() {
        return m_mbs;
    }

    /**
     * Returns a string that identifies the server's socket endpoint.
     *
     * @return server endpoint
     */
    public String getServerEndpoint() {
        String locator = null;

        if (m_connector != null) {
            try {
                locator = m_connector.getInvokerLocator();
            } catch (Exception e) {
                RuntimeException re = new RuntimeException(e);
                throw re; // this never should happen;
            }
        }

        return locator;
    }

    /**
     * Returns the configuration that the server-side services will use when it needs to be a client itself and send
     * commands to a remote server. The returned client configuration is a copy - changes made to it will not affect
     * this container's clients.
     *
     * @return configuration of the clients that are created by this container
     */
    public ClientCommandSenderConfiguration getClientConfiguration() {
        return m_clientConfiguration.copy();
    }

    /**
     * Returns a manager that can be used by objects that either own, use or have access to this
     * {@link ServiceContainer} object. It can be used, for example, by command services to restrict concurrent access.
     * Callers must not cache this object for more than a single permit/release cycle or otherwise assume they know the
     * specific concurrency manager instance that is being used, in case a new manager has been installed with different
     * configuration (see {@link #setConcurrencyManager(ConcurrencyManager)}).
     *
     * @return concurrency manager object
     */
    public ConcurrencyManager getConcurrencyManager() {
        return m_concurrencyManager;
    }

    /**
     * Allows the caller to provide a reconfigured concurrency manager. This is useful if new concurrency limits should
     * be imposed on users of this manager. It is assumed clients are not caching the object returned by
     * {@link #getConcurrencyManager()}, otherwise, they will not pick up the changes in any new managers that are
     * installed by calls to this setter.
     *
     * <p>We can't easily reconfigure existing concurrency manager objects due to the nature of the counting semaphore
     * objects and not being able to nicely resize them to a specific value - hence we ask clients to get
     * {@link #getConcurrencyManager()} when they need to get a permit.</p>
     *
     * @param concurrencyManager the new concurrency manager object that is being installed
     */
    public void setConcurrencyManager(ConcurrencyManager concurrencyManager) {
        m_concurrencyManager = concurrencyManager;
    }

    /**
     * A convienence method that takes a {@link #getClientConfiguration() client configuration} and builds a sender
     * object with it. Note that regardless of the transport specified in <code>server_endpoint</code>, this method will
     * always place security configuration into the underlying remote communicator.
     *
     * @param  server_endpoint a string that represents the remote endpoint to which the sender will connect and send
     *                         commands
     * @param  client_config   the client configuration which with to configure the sender
     *
     * @return a sender with the client configuration as defined in this container
     *
     * @throws RuntimeException if failed to create the sender
     */
    public ClientCommandSender createClientCommandSenderWithSecurity(String server_endpoint,
        ClientCommandSenderConfiguration client_config) {
        Map<String, String> ssl_config = new HashMap<String, String>();

        // these config settings are only used if the transport uses SSL, but since we don't know what remote endpoint
        // the caller will really end up wanting to use, let's put these SSL config settings in here just in case

        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, client_config.securityKeystoreFile);
        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, client_config.securityKeystoreAlgorithm);
        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, client_config.securityKeystoreType);
        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, client_config.securityKeystorePassword);
        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_PASSWORD, client_config.securityKeystoreKeyPassword);
        ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, client_config.securityTruststoreFile);
        ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, client_config.securityTruststoreAlgorithm);
        ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, client_config.securityTruststoreType);
        ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, client_config.securityTruststorePassword);
        ssl_config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, client_config.securitySecureSocketProtocol);
        ssl_config.put(SSLSocketBuilder.REMOTING_KEY_ALIAS, client_config.securityKeystoreAlias);
        ssl_config.put(SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE, Boolean
            .toString(client_config.securityServerAuthMode));
        ssl_config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");

        try {
            RemoteCommunicator remote_comm = new JBossRemotingRemoteCommunicator(server_endpoint, ssl_config);

            for (ServiceContainerSenderCreationListener listener : m_senderCreationListeners) {
                listener.preCreate(this, remote_comm, client_config);
            }

            ClientCommandSender sender = new ClientCommandSender(remote_comm, client_config);

            for (ServiceContainerSenderCreationListener listener : m_senderCreationListeners) {
                listener.postCreate(this, sender);
            }

            return sender;
        } catch (Exception e) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.FAILED_TO_CREATE_SENDER, server_endpoint),
                e);
        }
    }

    /**
     * A convienence method that takes a {@link #getClientConfiguration() client configuration} and builds a sender
     * object with it. Note that this method will examine the transport specified in <code>server_endpoint</code>, and
     * if (and only if) that transport requires security, the underlying remote communicator will get configured with
     * security information found in the given client configuration. This is different than
     * {@link #createClientCommandSenderWithSecurity(String, ClientCommandSenderConfiguration)}.
     *
     * <p>Note that the given <code>client_config</code> may have its values modified by any registered
     * {@link ServiceContainerSenderCreationListener sender creation listeners}, so callers should pass in a copy of
     * their config object if they do not want it altered.</p>
     *
     * @param  server_endpoint a string that represents the remote endpoint to which the sender will connect and send
     *                         commands
     * @param  client_config   the client configuration which with to configure the sender
     *
     * @return a sender with the client configuration as defined in this container
     *
     * @throws RuntimeException if failed to create the sender
     */
    public ClientCommandSender createClientCommandSender(String server_endpoint,
        ClientCommandSenderConfiguration client_config) {
        Map<String, String> ssl_config = null;

        if (SecurityUtil.isTransportSecure(server_endpoint)) {
            ssl_config = new HashMap<String, String>();
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, client_config.securityKeystoreFile);
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, client_config.securityKeystoreAlgorithm);
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, client_config.securityKeystoreType);
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, client_config.securityKeystorePassword);
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_PASSWORD, client_config.securityKeystoreKeyPassword);
            ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, client_config.securityTruststoreFile);
            ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, client_config.securityTruststoreAlgorithm);
            ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, client_config.securityTruststoreType);
            ssl_config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, client_config.securityTruststorePassword);
            ssl_config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, client_config.securitySecureSocketProtocol);
            ssl_config.put(SSLSocketBuilder.REMOTING_KEY_ALIAS, client_config.securityKeystoreAlias);
            ssl_config.put(SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE, Boolean
                .toString(client_config.securityServerAuthMode));
            ssl_config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
        }

        try {
            RemoteCommunicator remote_comm = new JBossRemotingRemoteCommunicator(server_endpoint, ssl_config);
            for (ServiceContainerSenderCreationListener listener : m_senderCreationListeners) {
                listener.preCreate(this, remote_comm, client_config);
            }

            ClientCommandSender sender = new ClientCommandSender(remote_comm, client_config);

            for (ServiceContainerSenderCreationListener listener : m_senderCreationListeners) {
                listener.postCreate(this, sender);
            }

            return sender;
        } catch (Exception e) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.FAILED_TO_CREATE_SENDER, server_endpoint),
                e);
        }
    }

    /**
     * This initializes the container with the given set of configuration preferences and starts the communications
     * services. The configuration preferences are used to configure the internal services. Note that if
     * {@link ServiceContainerConfigurationConstants#DISABLE_COMMUNICATIONS} is <code>true</code>, this method does
     * nothing - all communications services will not be started.
     *
     * <p>The <code>client_configuration</code> is used in case any of the server-side services need to act as a client
     * and send commands out to another external server (as is the case when input streams are being remoted).</p>
     *
     * @param  configuration        set of configuration preferences used to configure the internal server-side services
     * @param  client_configuration set of configuration preferences used to configure the internal client-side services
     *
     * @throws Exception if failed to initialize all of the services successfully
     *
     * @see    #start(Preferences, ClientCommandSenderConfiguration, MBeanServer)
     */
    public void start(Preferences configuration, ClientCommandSenderConfiguration client_configuration)
        throws Exception {
        start(configuration, client_configuration, null);
    }

    /**
     * This initializes the container with the given set of configuration preferences and starts the communications
     * services using the given MBeanServer to register the services. The configuration preferences are used to
     * configure the internal services. Note that if
     * {@link ServiceContainerConfigurationConstants#DISABLE_COMMUNICATIONS} is <code>true</code>, this method does
     * nothing - all communications services will not be started.
     *
     * <p>The <code>client_configuration</code> is used in case any of the server-side services need to act as a client
     * and send commands out to another external server (as is the case when input streams are being remoted).</p>
     *
     * <p>If <code>mbs</code> is <code>null</code>, one will be provided. First, if
     * {@link ServiceContainerConfiguration#getMBeanServerName()} returns <code>null</code> (meaning, it is not
     * configured), then the {@link ManagementFactory#getPlatformMBeanServer() built-in JVM platform MBeanServer} will
     * be used. If the configured MBeanServer name is non-<code>null</code>, a scan of all registered MBeanServers will
     * be performed to see if an MBeanServer is registered with a default domain name equal to that configured
     * {@link ServiceContainerConfiguration#getMBeanServerName() MBeanServer name}. If one exists, it will be used. If
     * one does not yet exist, one will be created.</p>
     *
     * @param  configuration        configuration preferences used to configure the internal server-side services
     * @param  client_configuration configuration preferences used to configure the internal client-side services
     * @param  mbs                  the MBeanServer where the services will be registered (may be <code>null</code>)
     *
     * @throws Exception if failed to initialize all of the services successfully
     */
    public void start(Preferences configuration, ClientCommandSenderConfiguration client_configuration, MBeanServer mbs)
        throws Exception {
        // create our own copy of the configuration properties
        m_configuration = new ServiceContainerConfiguration(configuration);

        // make sure the configuration is up to date with the latest known, supported schema version
        ServiceContainerConfigurationUpgrade.upgradeToLatest(m_configuration.getPreferences());

        // if a concurrency manager was not provided yet, create one. if a global concurrency limit
        // was defined, add our listener that will drop commands when the limit is exceeded
        if (m_concurrencyManager == null) {
            m_concurrencyManager = new ConcurrencyManager(null);
        }

        Integer globalConcurrencyLimit = m_configuration.getGlobalConcurrencyLimit();
        if (globalConcurrencyLimit > 0) {
            // create a new concurrency manager, add our global concurrency limit and replace the existing manager
            Map<String, Integer> limits = m_concurrencyManager.getAllConfiguredNumberOfPermitsAllowed();
            limits.put(GlobalConcurrencyLimitCommandListener.CONCURRENCY_LIMIT_NAME, globalConcurrencyLimit);
            m_concurrencyManager = new ConcurrencyManager(limits);

            // add our listener that will drop commands if we reach our limit
            addCommandListener(new GlobalConcurrencyLimitCommandListener(this));
            LOG.info(CommI18NResourceKeys.GLOBAL_CONCURRENCY_LIMIT_SET, globalConcurrencyLimit);
        } else {
            LOG.info(CommI18NResourceKeys.GLOBAL_CONCURRENCY_LIMIT_DISABLED);
        }

        // now startup all the internal services necessary to begin receiving commands
        boolean disabled = m_configuration.isCommunicationsDisabled();

        if (!disabled) {
            // create our own copy of the client configuration
            m_clientConfiguration = client_configuration.copy();

            // define the system property that tells JBoss/Remoting to put the jboss.identity file in our data directory
            File jboss_identity_dir = m_configuration.getDataDirectory();
            if (jboss_identity_dir != null) {
                System.setProperty("jboss.identity.dir", jboss_identity_dir.getAbsolutePath());
            }

            // get the MBeanServer where we will put all of our services
            if (mbs == null) {
                findOrCreateMBeanServer(m_configuration.getMBeanServerName());
            } else {
                m_mbs = mbs;
                m_mbsCreated = false;
            }

            // all services will get a unique index assigned from the current value of this counter
            m_servicesIndex = new AtomicLong(0L);

            // setup our services
            setupDetector();
            setupCommandServices();
            setupServerConnector();

            LOG.info(CommI18NResourceKeys.SERVICE_CONTAINER_STARTED);
        } else {
            LOG.info(CommI18NResourceKeys.SERVICE_CONTAINER_DISABLED);
        }

        return;
    }

    /**
     * This method should be called when the services are no longer needed (for example, when the VM is shutting down).
     * All started services will be stopped.
     */
    public void shutdown() {
        LOG.info(CommI18NResourceKeys.SERVICE_CONTAINER_SHUTTING_DOWN);

        // stop connector
        try {
            if (m_connector != null) {
                m_connector.stop();
                m_connector.destroy();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_CONNECTOR_FAILURE);
        }

        // stop detector
        try {
            if (m_detector != null) {
                m_detector.stop();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_DETECTOR_FAILURE);
        }

        // stop listening to registry notifications
        try {
            if (m_registry != null) {
                m_registry.removeNotificationListener(m_discoveryListener);
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_UNREGISTER_LISTENER_FAILURE);
        }

        // stop the SSL server socket factory service
        try {
            if (m_sslServerSocketFactoryService != null) {
                m_sslServerSocketFactoryService.stop();
                m_sslServerSocketFactoryService.destroy();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_SSL_FACTORY_SERVICE_FAILURE);
        }

        // stop the remote pojo service
        try {
            if (m_remotePojoCommandService != null) {
                m_remotePojoCommandService.stopService();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_REMOTE_POJO_SERVICE_FAILURE);
        }

        // stop the remote stream services
        try {
            if (m_remoteInputStreamCommandService != null) {
                m_remoteInputStreamCommandService.stopService();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_REMOTE_STREAM_SERVICE_FAILURE);
        }

        try {
            if (m_remoteOutputStreamCommandService != null) {
                m_remoteOutputStreamCommandService.stopService();
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_REMOTE_OUTSTREAM_SERVICE_FAILURE);
        }

        // clean up the MBeanServer
        if (m_mbs != null) {
            // remove the mbean server but only if we created it originally
            // if the mbean server already existed, just unregister all our services from our JMX domain
            try {
                if (m_mbsCreated) {
                    MBeanServerFactory.releaseMBeanServer(m_mbs);
                } else {
                    // unregister the services we know are registered, if we fail in any step, just keep going with the rest
                    // its possible we didn't fully initialize our MBS at startup; we just have to clean up the remaining
                    Set obj_names = m_mbs.queryNames(new ObjectName(JMX_DOMAIN + ":*"), null);

                    for (Iterator iter = obj_names.iterator(); iter.hasNext();) {
                        try {
                            ObjectName obj_name = (ObjectName) iter.next();
                            m_mbs.unregisterMBean(obj_name);
                        } catch (Exception e) {
                            LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_MBS_FAILURE);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN_MBS_FAILURE);
            }
        }

        // null out all the fields - we are effectively shutdown right now
        m_mbs = null;
        m_mbsCreated = false;
        m_configuration = null;
        m_registry = null;
        m_detector = null;
        m_connector = null;
        m_sslServerSocketFactoryService = null;
        m_remotePojoCommandService = null;
        m_remoteInputStreamCommandService = null;
        m_remoteOutputStreamCommandService = null;
        m_clientConfiguration = null;
        m_servicesIndex = null;
        m_concurrencyManager = null;
        m_discoveryListener.removeAll();
        m_commandListeners.clear();
        m_senderCreationListeners.clear();
        m_customData.clear();

        LOG.info(CommI18NResourceKeys.SERVICE_CONTAINER_SHUTDOWN);

        return;
    }

    /**
     * Adds the given object as a listener that will receive notifications when new servers coming online have been
     * discovered or old servers were discovered to have gone offline. You can add listeners at any time; regardless of
     * whether this object is {@link #start(Preferences, ClientCommandSenderConfiguration) started} or
     * {@link #shutdown() stopped}.
     *
     * @param listener the object that will receive the notifications
     */
    public void addDiscoveryListener(AutoDiscoveryListener listener) {
        m_discoveryListener.add(listener);

        return;
    }

    /**
     * Removes the given object as a listener so it will no longer receive discovery notifications. You can remove
     * listeners at any time; regardless of whether this object is
     * {@link #start(Preferences, ClientCommandSenderConfiguration) started} or {@link #shutdown() stopped}.
     *
     * @param listener the object that will no longer receive the notifications
     */
    public void removeDiscoveryListener(AutoDiscoveryListener listener) {
        m_discoveryListener.remove(listener);

        return;
    }

    /**
     * Adds the given listener so it can be notified everytime a new command is received. If the comm services are not
     * already started, this listener will be added once the services are started.
     *
     * @param listener
     */
    public void addCommandListener(CommandListener listener) {
        synchronized (m_commandListeners) {
            m_commandListeners.add(listener);

            // if we are already started, hot-deploy the listener to the existing CommandProcessor
            Connector connector = m_connector; // put in local var so we don't have to worry about synchronizing

            if (connector != null) {
                ServerInvocationHandler[] handlers = connector.getInvocationHandlers();
                if (handlers != null) {
                    for (ServerInvocationHandler handler : handlers) {
                        if (handler instanceof CommandProcessor) {
                            ((CommandProcessor) handler).addCommandListener(listener);
                            return;
                        }
                    }
                }
            }
        }

        return;
    }

    /**
     * Removes the given listener so it no longer is notified when a new command is received.
     *
     * @param listener
     */
    public void removeCommandListener(CommandListener listener) {
        synchronized (m_commandListeners) {
            m_commandListeners.remove(listener);

            // if we are already started, remove the listener from the existing CommandProcessor
            Connector connector = m_connector; // put in local var so we don't have to worry about synchronizing

            if (connector != null) {
                ServerInvocationHandler[] handlers = connector.getInvocationHandlers();
                if (handlers != null) {
                    for (ServerInvocationHandler handler : handlers) {
                        if (handler instanceof CommandProcessor) {
                            ((CommandProcessor) handler).removeCommandListener(listener);
                            return;
                        }
                    }
                }
            }
        }

        return;
    }

    /**
     * Adds the given listener so it will be notified everytime this service container creates a new
     * {@link ClientCommandSender sender}.
     *
     * @param listener
     */
    public void addServiceContainerSenderCreationListener(ServiceContainerSenderCreationListener listener) {
        m_senderCreationListeners.add(listener);
    }

    /**
     * Removes the given listener so it no longer is notified when this service container creates a new
     * {@link ClientCommandSender sender}.
     *
     * @param listener
     */
    public void removeServiceContainerSenderCreationListener(ServiceContainerSenderCreationListener listener) {
        m_senderCreationListeners.remove(listener);
    }

    /**
     * Adds the given command service to the service container. Once added, the command service will be able to handle
     * incoming requests for those commands it supports.
     *
     * @param  command_service the new command service to add
     *
     * @return an ID that can be used by this object to later find the service again
     *
     * @throws Exception if failed to add the command service - it will not be able to process commands
     */
    public CommandServiceId addCommandService(CommandService command_service) throws Exception {
        long next_index = m_servicesIndex.incrementAndGet();
        ObjectName new_name = ObjectNameFactory.create(OBJECTNAME_CMDSERVICE_INSTANCE + "," + KeyProperty.ID + "="
            + next_index);

        command_service.setServiceContainer(this);

        m_mbs.registerMBean(command_service, new_name);

        CommandServiceId id = new CommandServiceId(Long.toString(next_index));

        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_ADDED_COMMAND_SERVICE, command_service.getClass().getName(),
            id);

        return id;
    }

    /**
     * Adds the given command service to the service container where the command service classname is specified. Once
     * added, the command service will be able to handle incoming requests for those commands it supports.
     *
     * @param  command_service_class_name the class name of the new command service to add
     *
     * @return an ID that can be used by this object to later find the service again
     *
     * @throws Exception if failed to instantiate and add the command service - it will not be able to process commands
     *
     * @see    #addCommandService(CommandService)
     */
    public CommandServiceId addCommandService(String command_service_class_name) throws Exception {
        CommandService commandService = (CommandService) Class.forName(command_service_class_name).newInstance();

        return addCommandService(commandService);
    }

    /**
     * Removes a command service that is identified by the given ID.
     *
     * @param id identifies the command service to remove
     */
    public void removeCommandService(CommandServiceId id) {
        ObjectName name = ObjectNameFactory.create(OBJECTNAME_CMDSERVICE_INSTANCE + "," + KeyProperty.ID + "=" + id);

        try {
            m_mbs.unregisterMBean(name);

            LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_REMOVED_COMMAND_SERVICE, id, name);
        } catch (InstanceNotFoundException e) {
            LOG.debug(CommI18NResourceKeys.CANNOT_REMOVE_UNREGISTERED_CMDSERVICE, id, name, e);
        } catch (MBeanRegistrationException e) {
            LOG.warn(CommI18NResourceKeys.CANNOT_REMOVE_CMDSERVICE, id, name, e);
        }

        return;
    }

    /**
     * Enables the POJO to receive remote invocations. This method will add the appropriate command service if one is
     * needed. Once this method returns, the given POJO can be remotely invoked via a {@link ClientRemotePojoFactory}
     * generated proxy object.
     *
     * <p>Note that only one POJO of the given interface can be remoted.</p>
     *
     * @param  pojo           the object to make remotely accessible
     * @param  interface_name the name of one of the <code>pojo</code>'s interfaces that is to be exposed as its remote
     *                        interface
     *
     * @throws Exception if failed to instantiate and add the command service - it will not be able to process remote
     *                   invocation requests to the POJO
     *
     * @see    ClientRemotePojoFactory#getRemotePojo(Class)
     */
    public void addRemotePojo(Object pojo, String interface_name) throws Exception {
        synchronized (this) {
            if (m_remotePojoCommandService == null) {
                // the remote POJO command service hasn't been created yet, let's do it now
                m_remotePojoCommandService = new RemotePojoInvocationCommandService();
                addCommandService(m_remotePojoCommandService);
            }
        }

        m_remotePojoCommandService.addPojo(pojo, interface_name);

        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_ADDED_REMOTE_POJO, pojo.getClass().getName(), interface_name);

        return;
    }

    /**
     * Similar to {@link #addRemotePojo(Object, String)} except this method allows you to provide the actual interface
     * class representation, as opposed to its name as a String.
     *
     * @param  pojo           the object to make remotely accessible
     * @param  pojo_interface the interface that is to be exposed as its remote interface
     *
     * @throws Exception if failed to instantiate and add the command service - it will not be able to process remote
     *                   invocation requests to the POJO
     */
    public <T> void addRemotePojo(T pojo, Class<T> pojo_interface) throws Exception {
        synchronized (this) {
            if (m_remotePojoCommandService == null) {
                // the remote POJO command service hasn't been created yet, let's do it now
                m_remotePojoCommandService = new RemotePojoInvocationCommandService();
                addCommandService(m_remotePojoCommandService);
            }
        }

        m_remotePojoCommandService.addPojo(pojo, pojo_interface);

        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_ADDED_REMOTE_POJO, pojo.getClass().getName(), pojo_interface);

        return;
    }

    /**
     * Removes the given remote interface from the remote framework such that the POJO is no longer remoted. Once this
     * method returns, the POJO that was servicing requests for that interface is no longer remotely accessible.
     *
     * @param interface_name the name of the interface that was exposed remotely and is to be removed
     */
    public void removeRemotePojo(String interface_name) {
        m_remotePojoCommandService.removePojo(interface_name);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_REMOVED_REMOTE_POJO, interface_name);
        return;
    }

    /**
     * Removes the given remote interface from the remote framework such that the POJO is no longer remoted. Once this
     * method returns, the POJO that was servicing requests for that interface is no longer remotely accessible.
     *
     * @param remote_interface the interface that was exposed remotely and is to be removed
     */
    public void removeRemotePojo(Class remote_interface) {
        m_remotePojoCommandService.removePojo(remote_interface);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_REMOVED_REMOTE_POJO, remote_interface);
        return;
    }

    /**
     * Enables the input stream to receive remote invocations. This method will add the appropriate command service if
     * one is needed. Once this method returns, the given stream can be remotely invoked via a {@link RemoteInputStream}
     * proxy object.
     *
     * @param  in the input stream to expose to remote clients
     *
     * @return an identification object that is to be used as the
     *         {@link RemoteInputStreamCommand#setStreamId(Long) stream ID} and can also be used to
     *         {@link #removeRemoteInputStream(Long) remove the input stream service}.
     *
     * @throws Exception if failed to instantiate and add the command service - it will not be able to process remote
     *                   invocation requests to access input streams
     *
     * @see    RemoteInputStreamCommandService#addInputStream(InputStream)
     */
    public Long addRemoteInputStream(InputStream in) throws Exception {
        synchronized (this) {
            if (m_remoteInputStreamCommandService == null) {
                m_remoteInputStreamCommandService = new RemoteInputStreamCommandService();
                addCommandService(m_remoteInputStreamCommandService);
            }
        }

        Long stream_id = m_remoteInputStreamCommandService.addInputStream(in);

        return stream_id;
    }

    /**
     * Enables the output stream to receive remote invocations. This method will add the appropriate command service if
     * one is needed. Once this method returns, the given stream can be remotely invoked via a
     * {@link RemoteOutputStream} proxy object.
     *
     * @param  out the output stream to expose to remote clients
     *
     * @return an identification object that is to be used as the
     *         {@link RemoteOutputStreamCommand#setStreamId(Long) stream ID} and can also be used to
     *         {@link #removeRemoteOutputStream(Long) remove the output stream service}.
     *
     * @throws Exception if failed to instantiate and add the command service - it will not be able to process remote
     *                   invocation requests to access output streams
     *
     * @see    RemoteOutputStreamCommandService#addOutputStream(OutputStream)
     */
    public Long addRemoteOutputStream(OutputStream out) throws Exception {
        synchronized (this) {
            if (m_remoteOutputStreamCommandService == null) {
                m_remoteOutputStreamCommandService = new RemoteOutputStreamCommandService();
                addCommandService(m_remoteOutputStreamCommandService);
            }
        }

        Long stream_id = m_remoteOutputStreamCommandService.addOutputStream(out);

        return stream_id;
    }

    /**
     * Removes a remoted input stream that has the given ID from the remote framework such that the stream is no longer
     * remotely accessible.
     *
     * @param  stream_id the ID of the stream that was exposed remotely and is to be removed
     *
     * @return <code>true</code> if the stream ID was valid and a stream was removed; <code>false</code> if the ID
     *         referred to a non-existent stream (which could mean either the stream was never registered at all or it
     *         was registered but has already been removed)
     *
     * @see    RemoteInputStreamCommandService#removeInputStream(Long)
     */
    public boolean removeRemoteInputStream(Long stream_id) {
        return m_remoteInputStreamCommandService.removeInputStream(stream_id);
    }

    /**
     * Removes a remoted output stream that has the given ID from the remote framework such that the stream is no longer
     * remotely accessible.
     *
     * @param  stream_id the ID of the stream that was exposed remotely and is to be removed
     *
     * @return <code>true</code> if the stream ID was valid and a stream was removed; <code>false</code> if the ID
     *         referred to a non-existent stream (which could mean either the stream was never registered at all or it
     *         was registered but has already been removed)
     *
     * @see    RemoteOutputStreamCommandService#removeOutputStream(Long)
     */
    public boolean removeRemoteOutputStream(Long stream_id) {
        return m_remoteOutputStreamCommandService.removeOutputStream(stream_id);
    }

    /**
     * Allows a caller to install their own invocation handler for another subsystem.
     * This allows the caller to use this ServiceContainer to set up all the infrastructure so the caller
     * doesn't have to do it all. All the caller needs is an invocation handler to handle their own remote
     * messages.  To stop the handler from processing messages, call {@link #removeInvocationHandler(String)}.
     * 
     * @param subsystem the new subsystem whose messages will be handled by the given handler
     * @param handler used to handle incoming messages for the given subsystem
     * @throws Exception if the remote connector hasn't been created/started or the handler failed to get added for some reason
     */
    public void addInvocationHandler(String subsystem, ServerInvocationHandler handler) throws Exception {
        if (m_connector != null) {
            m_connector.addInvocationHandler(subsystem, handler);
        } else {
            throw new IllegalStateException("m_connector==null"); // the connector isn't created/started
        }
    }

    public void removeInvocationHandler(String subsystem) throws Exception {
        if (m_connector != null) {
            m_connector.removeInvocationHandler(subsystem);
        } else {
            throw new IllegalStateException("m_connector==null"); // the connector isn't created/started
        }
    }

    /**
     * Given the name of the MBeanServer (i.e. its default domain name) that identifies the server to use, this method
     * finds it or, if it is not found, creates one. This method should only be used when starting the container; to get
     * the MBeanServer this container is currently using, call {@link #getMBeanServer()} instead.
     *
     * <p>If the MBeanServer was found, {@link #m_mbsCreated} will be set to <code>false</code> - otherwise, the
     * MBeanServer will be created in this method and thus {@link #m_mbsCreated} will be set to <code>true</code>.</p>
     *
     * <p>If the given name is <code>null</code>, then the JVM's
     * {@link ManagementFactory#getPlatformMBeanServer() platform MBeanServer} will be used.</p>
     *
     * @param mbs_name the name of the MBeanServer (which is actually the default domain name) (may be <code>
     *                 null</code>)
     */
    private void findOrCreateMBeanServer(String mbs_name) {
        MBeanServer mbs = null;

        if (mbs_name != null) {
            ArrayList all_mbs = MBeanServerFactory.findMBeanServer(null);

            for (Iterator iter = all_mbs.iterator(); iter.hasNext();) {
                MBeanServer cur_mbs = (MBeanServer) iter.next();
                if ((cur_mbs != null) && mbs_name.equals(cur_mbs.getDefaultDomain())) {
                    LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_USING_EXISTING_MBS, mbs_name);
                    mbs = cur_mbs;
                    m_mbsCreated = false;
                    break;
                }
            }

            if (mbs == null) {
                LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_CREATING_MBS, mbs_name);
                mbs = MBeanServerFactory.createMBeanServer(mbs_name);
                m_mbsCreated = true;
            }
        } else {
            mbs = ManagementFactory.getPlatformMBeanServer();
            LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_USING_EXISTING_MBS, mbs.getDefaultDomain());
            m_mbsCreated = false;
        }

        m_mbs = mbs;

        return;
    }

    /**
     * Sets up NetworkRegistry and MulticastDetector so will can register ourselves on the network.
     *
     * @throws Exception if failed to create or register all the services successfully
     */
    private void setupDetector() throws Exception {
        // the network registry listens for other servers coming online and going offline
        // we will also register this service container as a listener to the registry so it can be told about servers coming and going
        m_registry = new NetworkRegistry();

        // under very rare conditions (specifically, the hostname on the box is not resolvable via DNS), the remoting
        // identity generation will fail.  If it fails, we will generate our own jboss.identity here
        try {
            Identity.get(m_mbs);
        } catch (Exception e) {
            LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_IDENTITY_FAILURE, e);
            System.setProperty("jboss.identity", Identity.createUniqueID());
        }

        try {
            m_mbs.registerMBean(m_registry, OBJECTNAME_NETWORK_REGISTRY);
        } catch (Exception e) {
            LOG.warn(CommI18NResourceKeys.SERVICE_CONTAINER_NETWORK_REGISTRY_FAILURE, e);
            m_registry = null;
            return; // not much we can do, user probably doesn't have his hostname DNS resolvable - add it to /etc/hosts
        }

        m_registry.addNotificationListener(m_discoveryListener, null, null);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_REGISTRY_CREATED, OBJECTNAME_NETWORK_REGISTRY);

        if (m_configuration.isMulticastDetectorEnabled()) {
            try {
                // multicast detector will detect new network registries that come online
                m_detector = new MulticastDetector();

                String address = m_configuration.getMulticastDetectorMulticastAddress();
                String bind_address = m_configuration.getMulticastDetectorBindAddress();
                long default_delay = m_configuration.getMulticastDetectorDefaultTimeDelay();
                long heartbeat_delay = m_configuration.getMulticastDetectorHeartbeatTimeDelay();
                int port = m_configuration.getMulticastDetectorPort();

                if (address != null) {
                    InetAddress inet_addr = InetAddress.getByName(address);
                    m_detector.setAddress(inet_addr);
                    m_detector.setDefaultIP(inet_addr.getHostAddress());
                }

                if (bind_address != null) {
                    m_detector.setBindAddress(InetAddress.getByName(bind_address));
                }

                if (default_delay != Long.MIN_VALUE) {
                    m_detector.setDefaultTimeDelay(default_delay);
                }

                if (heartbeat_delay != Long.MIN_VALUE) {
                    m_detector.setHeartbeatTimeDelay(heartbeat_delay);
                }

                if (port != Integer.MIN_VALUE) {
                    m_detector.setPort(port);
                }

                LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_MULTICAST_DETECTOR_CONFIG, m_detector.getAddress(),
                    m_detector.getPort(), m_detector.getBindAddress(), m_detector.getDefaultIP(), m_detector
                        .getDefaultTimeDelay(), m_detector.getHeartbeatTimeDelay());

                m_mbs.registerMBean(m_detector, OBJECTNAME_MULTICAST_DETECTOR);
                m_detector.start();
                LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_MULTICAST_DETECTOR_CREATED,
                    OBJECTNAME_MULTICAST_DETECTOR);
            } catch (Exception e) {
                LOG.warn(e, CommI18NResourceKeys.SERVICE_CONTAINER_MULTICAST_DETECTOR_START_ERROR, e);
            }
        } else {
            LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_MULTICAST_DETECTOR_DISABLED);
        }

        return;
    }

    /**
     * Sets up the appropriate server connector services so incoming commands can be received.
     *
     * @throws Exception if failed to create or register all the services successfully
     */
    private void setupServerConnector() throws Exception {
        String locator_uri = m_configuration.getConnectorRemoteEndpoint();
        InvokerLocator locator = new InvokerLocator(locator_uri);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_CONNECTOR_URI, locator);

        // now initialize our security services but only if the connector's transport is secure and needs them.
        // note: we must do this before connector.create()
        String transport = m_configuration.getConnectorTransport();
        Map<String, String> connector_config = new HashMap<String, String>();

        if (SecurityUtil.isTransportSecure(transport)) {
            LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_NEEDS_SECURITY_SERVICES, transport);

            if (transport.equals("sslsocket")) {
                initializeSecurityServices();
            }

            connector_config.put(ServerInvoker.SERVER_SOCKET_FACTORY, OBJECTNAME_SSL_SERVERSOCKET_FACTORY.toString());

            if (transport.equals("https")) {
                connector_config.put("SSLImplementation", RemotingSSLImplementation.class.getName());
            }

            if (transport.equals("sslservlet") || transport.equals("https")) {
                String tomcatAuthMode = "rhq.server.tomcat.security.client-auth-mode";
                String tomcatAuthModeValue = System.getProperty("rhq.server.tomcat.security.client-auth-mode");
                if (!tomcatAuthModeValue.equals("true") && !tomcatAuthModeValue.equals("false")) {
                    LOG.warn(CommI18NResourceKeys.SERVICE_CONTAINER_CONFIGURATION_INVALID_TOMCAT_CLIENT_AUTH,
                            tomcatAuthMode, tomcatAuthModeValue, "true", "false");
                }
            }
        }

        // we can now instantiate our connector with the locator URI and other configuration
        m_connector = new Connector(connector_config);
        m_connector.setInvokerLocator(locator.getLocatorURI());

        long lease_period = m_configuration.getConnectorLeasePeriod();
        if (lease_period != Long.MIN_VALUE) {
            m_connector.setLeasePeriod(lease_period);
        }

        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_CONNECTOR_LEASE_PERIOD, m_connector.getLeasePeriod());

        // register our connector in our MBeanServer, add our command processor as its own invocation handler to it and start it
        // after this, our connector will be active and a server socket is accepting requests
        m_mbs.registerMBean(m_connector, OBJECTNAME_CONNECTOR);
        m_connector.create();

        CommandProcessor handler = new CommandProcessor();

        for (CommandListener listener : m_commandListeners) {
            handler.addCommandListener(listener);
        }

        CommandAuthenticator commandAuthenticator = m_configuration.getCommandAuthenticator();
        if (commandAuthenticator != null) {
            commandAuthenticator.setServiceContainer(this);
            handler.setCommandAuthenticator(commandAuthenticator);
        }
        m_connector.addInvocationHandler(SUBSYSTEM, handler);
        m_connector.start();

        // create and register our metric MBean so we can emit statistics
        ServiceContainerMetrics metrics_mbean = new ServiceContainerMetrics(this, handler);
        m_mbs.registerMBean(metrics_mbean, ServiceContainerMetricsMBean.OBJECTNAME_METRICS);

        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_PROCESSOR_READY);

        return;
    }

    /**
     * Sets up the command services infrastructure and deploys all configured command services.
     *
     * @throws Exception                     if failed to create or register all the services successfully
     * @throws InvalidConfigurationException if the remote POJOs were misconfigured
     */
    private void setupCommandServices() throws Exception {
        boolean dynamic_discovery = m_configuration.isCommandServiceDirectoryDynamicDiscoveryEnabled();

        if (!dynamic_discovery) {
            // dynamic discovery is turned off - we cannot, therefore, rely on lazy instantiation of our internal services
            // since the directory will not see them - let's create them now before registering the directory
            m_remotePojoCommandService = new RemotePojoInvocationCommandService();
            addCommandService(m_remotePojoCommandService);

            m_remoteInputStreamCommandService = new RemoteInputStreamCommandService();
            addCommandService(m_remoteInputStreamCommandService);

            m_remoteOutputStreamCommandService = new RemoteOutputStreamCommandService();
            addCommandService(m_remoteOutputStreamCommandService);
        }

        // register all the command services that are configured
        String cmdServicesPropStr = m_configuration.getStartupCommandServices();
        String[] cmdServicesClassNames = cmdServicesPropStr.split("\\s*,\\s*");
        for (int i = 0; i < cmdServicesClassNames.length; i++) {
            String cmdServiceClassName = cmdServicesClassNames[i].trim();
            addCommandService(cmdServiceClassName);
        }

        // remote all the POJOs that are configured: format is "pojo-class-name:remote-interface-name,..."
        String remotePojosPropStr = m_configuration.getStartupRemotePojos();
        String[] remotePojosDefs = remotePojosPropStr.split("\\s*,\\s*");

        for (int i = 0; i < remotePojosDefs.length; i++) {
            String[] classAndInterfaceNames = remotePojosDefs[i].split("\\s*:\\s*");
            if (classAndInterfaceNames.length != 2) {
                throw new InvalidConfigurationException(LOG.getMsgString(
                    CommI18NResourceKeys.SERVICE_CONTAINER_REMOTE_POJO_CONFIG_INVALID,
                    ServiceContainerConfigurationConstants.REMOTE_POJOS, remotePojosDefs[i], remotePojosPropStr));
            }

            String pojo_class_name = classAndInterfaceNames[0];
            String remote_interface_name = classAndInterfaceNames[1];
            Object pojo_instance = Class.forName(pojo_class_name).newInstance();

            addRemotePojo(pojo_instance, remote_interface_name);
        }

        // now register the command service directory
        m_commandServiceDirectory = new CommandServiceDirectory();
        m_commandServiceDirectory.setAllowDynamicDiscovery(dynamic_discovery);

        m_mbs.registerMBean(m_commandServiceDirectory, OBJECTNAME_CMDSERVICE_DIRECTORY);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_DIRECTORY_CREATED, OBJECTNAME_CMDSERVICE_DIRECTORY);

        return;
    }

    /**
     * This method creates and initializes the services that are required in order to allow the connector to support
     * secure communications.
     *
     * @throws Exception if failed to initialize all the services
     */
    private void initializeSecurityServices() throws Exception {
        // we need a socket builder - this is the thing that allows us to provide custom keystore information
        SSLSocketBuilder socket_builder = new SSLSocketBuilder();
        socket_builder.setUseSSLServerSocketFactory(false);
        socket_builder.setSecureSocketProtocol(m_configuration.getConnectorSecuritySocketProtocol());
        socket_builder.setKeyStoreAlgorithm(m_configuration.getConnectorSecurityKeystoreAlgorithm());
        socket_builder.setKeyStoreType(m_configuration.getConnectorSecurityKeystoreType());
        socket_builder.setKeyStorePassword(m_configuration.getConnectorSecurityKeystorePassword());
        socket_builder.setKeyPassword(m_configuration.getConnectorSecurityKeystoreKeyPassword());
        socket_builder.setTrustStoreAlgorithm(m_configuration.getConnectorSecurityTruststoreAlgorithm());
        socket_builder.setTrustStoreType(m_configuration.getConnectorSecurityTruststoreType());
        socket_builder.setTrustStorePassword(m_configuration.getConnectorSecurityTruststorePassword());
        socket_builder.setClientAuthMode(m_configuration.getConnectorSecurityClientAuthMode());
        socket_builder.setServerSocketUseClientMode(false);

        try {
            // this allows the configured keystore file to be a URL, file path or a resource relative to our classloader
            socket_builder.setKeyStoreURL(m_configuration.getConnectorSecurityKeystoreFile());
        } catch (Exception e) {
            // this probably is due to the fact that the keystore doesn't exist yet - let's prepare one now
            createKeyStore();

            // now try to set it again, if an exception is still thrown, it's an unrecoverable error
            socket_builder.setKeyStoreURL(m_configuration.getConnectorSecurityKeystoreFile());
        }

        try {
            // this allows the configured keystore file to be a URL, file path or a resource relative to our classloader
            socket_builder.setTrustStoreURL(m_configuration.getConnectorSecurityTruststoreFile());
        } catch (Exception e) {
            // this may or may not be a bad thing - let's just log a message but keep going
            if (!m_configuration.getConnectorSecurityClientAuthMode().equals(SSLSocketBuilder.CLIENT_AUTH_MODE_NONE)) {
                LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_TRUSTSTORE_FAILURE);
            }
        }

        // this is the MBean service used by the connector's server invoker to create SSL-enabled server sockets
        m_sslServerSocketFactoryService = new SSLServerSocketFactoryService();
        m_sslServerSocketFactoryService.setSSLSocketBuilder(socket_builder);
        m_sslServerSocketFactoryService.create();
        m_sslServerSocketFactoryService.start();

        m_mbs.registerMBean(m_sslServerSocketFactoryService, OBJECTNAME_SSL_SERVERSOCKET_FACTORY);
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_SSL_SOCKET_FACTORY_CREATED,
            OBJECTNAME_SSL_SERVERSOCKET_FACTORY);
        return;
    }

    /**
     * Ensures that this server has a keystore created - this will ensure that this server will have a valid
     * certificate.
     *
     * @throws RuntimeException if failed to create the keystore file
     */
    private void createKeyStore() {
        SecurityUtil.createKeyStore(m_configuration.getConnectorSecurityKeystoreFile(), m_configuration
            .getConnectorSecurityKeystoreAlias(), "CN=RHQ, OU=RHQ, O=rhq-project.org, C=US", m_configuration
            .getConnectorSecurityKeystorePassword(), m_configuration.getConnectorSecurityKeystoreKeyPassword(), "DSA",
            36500);

        return;
    }
}