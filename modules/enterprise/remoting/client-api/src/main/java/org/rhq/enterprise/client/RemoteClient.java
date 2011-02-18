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
package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;

import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.client.RhqManagers;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;

/**
 * A remote access client that provides transparent servlet-based proxies to an RHQ Server.
 *
 * @author Greg Hinkle
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class RemoteClient implements RhqFacade {

    private static final Log LOG = LogFactory.getLog(RemoteClient.class);
    
    public static final String NONSECURE_TRANSPORT = "servlet";
    public static final String SECURE_TRANSPORT = "sslservlet";

    private String transport;
    private final String host;
    private final int port;
    private boolean loggedIn;
    private boolean connected;
    private Map<String, Object> managers;
    private Subject subject;
    private Client remotingClient;
    private String subsystem = null;

    /**
     * Creates a client that will communicate with the server running on the given host
     * listening on the given port. This constructor will not attempt to connect or login
     * to the remote server - use {@link #login(String, String)} for that.
     *
     * @param host
     * @param port
     */
    public RemoteClient(String host, int port) {
        this(null, host, port);
    }

    /**
     * Creates a client that will communicate with the server running on the given host
     * listening on the given port over the given transport.
     * This constructor will not attempt to connect or login
     * to the remote server - use {@link #login(String, String)} for that.
     *
     * @param transport valid values are "servlet" and "sslservlet" - if <code>null</code>,
     *                  sslservlet will be used for ports that end with "443", servlet otherwise
     * @param host
     * @param port
     */
    public RemoteClient(String transport, String host, int port) {
        this(transport, host, port, null);
    }

    public RemoteClient(String transport, String host, int port, String subsystem) {
        this.transport = (transport != null) ? transport : guessTransport(port);
        this.host = host;
        this.port = port;
        this.subsystem = subsystem;
    }

    /**
     * Connects to the remote server and logs in with the given credentials.
     * After successfully executing this, {@link #isLoggedIn()} will be <code>true</code>
     * and {@link #getSubject()} will return the subject that this method returns.
     *
     * @param user
     * @param password
     *
     * @return the logged in user
     *
     * @throws Exception if failed to connect to the server or log in
     */
    public Subject login(String user, String password) throws Exception {

        logout();
        doConnect();

        this.subject = getSubjectManager().login(user, password);
        this.loggedIn = true;

        return this.subject;
    }

    /**
     * Logs out from the server and disconnects this client.
     */
    public void logout() {
        try {
            if (this.loggedIn && this.subject != null) {
                getSubjectManager().logout(this.subject);
            }
        } catch (Exception e) {
            // just keep going so we can disconnect this client
        }

        doDisconnect();

        this.subject = null;
        this.loggedIn = false;
    }

    /**
     * Connects to the remote server but does not establish a user session. This can be used
     * with the limited API that does not require a Subject.
     *
     * After successfully executing this, {@link #isConnected()} will be <code>true</code>
     * and {@link #getSubject()} will return the subject that this method returns.
     * @throws Exception if failed to connect to the server or log in
     */
    public void connect() throws Exception {
        if (this.loggedIn) {
            String name = (null == this.subject) ? "" : this.subject.getName();
            throw new IllegalStateException("User " + name + " must log out before connection can be established.");
        }

        doDisconnect();
        doConnect();
        this.connected = true;
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if (this.loggedIn) {
            String name = (null == this.subject) ? "" : this.subject.getName();
            throw new IllegalStateException("User " + name + " is logged in. Call logout() instead of disconnect().");
        }

        doDisconnect();
        this.connected = false;
    }

    /**
     * Returns <code>true</code> if and only if this client successfully connected
     * to the remote server and the user successfully logged in.
     *
     * @return if the user was able to connect and log into the server
     */
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    /**
     * Returns <code>true</code> if and only if this client successfully connected
     * to the remote server.
     *
     * @return if the user was able to connect and log into the server
     */
    public boolean isConnected() {
        return this.connected;
    }

    /**
     * Returns the information on the user that is logged in.
     * May be <code>null</code> if the user never logged in successfully.
     *
     * @return user information or <code>null</code>
     */
    public Subject getSubject() {
        return this.subject;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getTransport() {
        return transport;
    }

    protected String guessTransport(int port) {
        return String.valueOf(port).endsWith("443") ? SECURE_TRANSPORT : NONSECURE_TRANSPORT;
    }

    /**
     * Sets the underlying transport to use to communicate with the server.
     * Available transports are "servlet" and "sslservlet".
     * If you set it to <code>null</code>, then the transport to be used will
     * be set appropriately for the {@link #getPort()} (e.g. a secure transport
     * will be used for ports that end with 443, a non-secure transport will be
     * used for all other ports).
     *
     * @param transport
     */
    public void setTransport(String transport) {
        this.transport = transport;
    }

    public AlertManagerRemote getAlertManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.AlertManager);
    }

    public AlertDefinitionManagerRemote getAlertDefinitionManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.AlertDefinitionManager);
    }

    public AvailabilityManagerRemote getAvailabilityManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.AvailabilityManager);
    }

    public BundleManagerRemote getBundleManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.BundleManager);
    }

    public CallTimeDataManagerRemote getCallTimeDataManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.CallTimeDataManager);
    }

    public RepoManagerRemote getRepoManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.RepoManager);
    }

    public ConfigurationManagerRemote getConfigurationManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ConfigurationManager);
    }

    public ContentManagerRemote getContentManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ContentManager);
    }

    public DataAccessManagerRemote getDataAccessManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.DataAccessManager);
    }

    public DiscoveryBossRemote getDiscoveryBoss() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.DiscoveryBoss);
    }

    public EventManagerRemote getEventManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.EventManager);
    }

    public MeasurementBaselineManagerRemote getMeasurementBaselineManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.MeasurementBaselineManager);
    }

    public MeasurementDataManagerRemote getMeasurementDataManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.MeasurementDataManager);
    }

    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.MeasurementDefinitionManager);
    }

    public MeasurementScheduleManagerRemote getMeasurementScheduleManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.MeasurementScheduleManager);
    }

    public OperationManagerRemote getOperationManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.OperationManager);
    }

    public ResourceManagerRemote getResourceManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ResourceManager);
    }

    public ResourceFactoryManagerRemote getResourceFactoryManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ResourceFactoryManager);
    }

    public ResourceGroupManagerRemote getResourceGroupManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ResourceGroupManager);
    }

    public ResourceTypeManagerRemote getResourceTypeManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.ResourceTypeManager);
    }

    public RoleManagerRemote getRoleManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.RoleManager);
    }

    public SavedSearchManagerRemote getSavedSearchManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.SavedSearchManager);
    }

    public SubjectManagerRemote getSubjectManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.SubjectManager);
    }

    public SupportManagerRemote getSupportManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.SupportManager);
    }

    public SystemManagerRemote getSystemManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.SystemManager);
    }

    public RemoteInstallManagerRemote getRemoteInstallManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.RemoteInstallManager);
    }

    public TagManagerRemote getTagManager() {
        return RemoteClientProxy.getProcessor(this, RhqManagers.TagManager);
    }

    /**
     * Returns the map of all remote managers running in the server that this
     * client can talk to.
     *
     * @return Map K=manager name V=remote proxy
     */
    public Map<String, Object> getManagers() {
        if (this.managers == null) {

            this.managers = new HashMap<String, Object>();

            for (RhqManagers manager : RhqManagers.values()) {
                try {
                    Method m = this.getClass().getMethod("get" + manager.name());
                    this.managers.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    LOG.error("Failed to load manager " + manager + " due to missing class.", e);
                }
            }
        }

        return this.managers;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "transport=" + transport + ", host=" + host + ", port=" + port
            + ", subsystem=" + subsystem + ", connected=" + connected + ", loggedIn=" + loggedIn + ", subject="
            + subject + ']';
    }

    /**
     * Returns the internal JBoss/Remoting client used to perform the low-level
     * comm with the server.
     *
     * This is package-scoped so the proxy can use it.
     *
     * @return remoting client used to talk to the server
     */
    Client getRemotingClient() {
        return this.remotingClient;
    }

    private void doDisconnect() {
        try {
            if (this.remotingClient != null && this.remotingClient.isConnected()) {
                this.remotingClient.disconnect();
            }
        } catch (Exception e) {
            LOG.warn(e); // TODO what to do here?
        } finally {
            this.remotingClient = null;
        }
    }

    private void doConnect() throws Exception {
        String locatorURI = this.transport + "://" + this.host + ":" + this.port
            + "/jboss-remoting-servlet-invoker/ServerInvokerServlet";
        InvokerLocator locator = new InvokerLocator(locatorURI);

        String subsystem = "REMOTEAPI";
        if ((this.subsystem != null) && (this.subsystem.trim().equalsIgnoreCase("WSREMOTEAPI"))) {
            subsystem = "WSREMOTEAPI";
        }
        Map<String, String> remotingConfig = buildRemotingConfig(locatorURI);
        this.remotingClient = new Client(locator, subsystem, remotingConfig);
        this.remotingClient.connect();
    }

    private Map<String, String> buildRemotingConfig(String locatorURI) {
        Map<String, String> config = new HashMap<String, String>();
        if (SecurityUtil.isTransportSecure(locatorURI)) {
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "data/keystore.dat");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "password");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_PASSWORD, "password");
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_SSL_PROTOCOL, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_ALIAS, "self");
            setConfigProp(config, SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE, "false");
            config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");

            // since we do not know the server's client-auth mode, assume we need a keystore and let's make sure we have one
            SSLSocketBuilder dummy_sslbuilder = new SSLSocketBuilder(); // just so we can test finding our keystore
            try {
                // this allows the configured keystore file to be a URL, file path or a resource relative to our classloader
                dummy_sslbuilder.setKeyStoreURL(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH));
            } catch (Exception e) {
                // this probably is due to the fact that the keystore doesn't exist yet - let's prepare one now
                SecurityUtil.createKeyStore(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH), config
                    .get(SSLSocketBuilder.REMOTING_KEY_ALIAS), "CN=RHQ, OU=RedHat, O=redhat.com, C=US", config
                    .get(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD), config
                    .get(SSLSocketBuilder.REMOTING_KEY_PASSWORD), "DSA", 36500);

                // now try to set it again, if an exception is still thrown, it's an unrecoverable error
                dummy_sslbuilder.setKeyStoreURL(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH));
            }

            // in case the transport floats over https - we want to make sure a hostname verifier is installed and allows all hosts
            config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
        }
        return config;
    }

    /**
     * Looks up the prop name in system properties and puts the value in the map. If the property
     * isn't set, the given default is used. If the given default is null and the property isn't
     * set, then the map is not populated.
     *
     * @param configMap
     * @param propName
     * @param defaultValue
     */
    private void setConfigProp(Map<String, String> configMap, String propName, String defaultValue) {
        String propValue = System.getProperty(propName, defaultValue);
        if (propValue != null) {
            configMap.put(propName, propValue);
        }
        return;
    }
}