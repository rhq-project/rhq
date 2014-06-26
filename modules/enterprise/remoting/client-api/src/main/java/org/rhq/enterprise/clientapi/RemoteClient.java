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
package org.rhq.enterprise.clientapi;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;

import org.rhq.bindings.client.AbstractRhqFacade;
import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;

/**
 * A remote access client that provides transparent servlet-based proxies to an RHQ Server.
 *
 * @author Greg Hinkle
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class RemoteClient extends AbstractRhqFacade {

    private static final Log LOG = LogFactory.getLog(RemoteClient.class);

    public static final String NONSECURE_TRANSPORT = "servlet";
    public static final String SECURE_TRANSPORT = "sslservlet";

    private String transport;
    private final String host;
    private final int port;
    private boolean loggedIn;
    private boolean connected;
    private Map<RhqManager, Object> managers;
    private Subject subject;
    private Client remotingClient;
    private String subsystem = null;
    private ProductInfo serverInfo = null;

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

    public <T> T remoteInvoke(RhqManager manager, Method method, Class<T> expectedReturnType, Object... parameters)
        throws Throwable {

        String methodSig = manager.remote().getName() + ":" + method.getName();

        Class<?>[] paramTypes = method.getParameterTypes();
        String[] paramSig = new String[paramTypes.length];
        for (int x = 0; x < paramTypes.length; x++) {
            paramSig[x] = paramTypes[x].getName();
        }

        NameBasedInvocation request = new NameBasedInvocation(methodSig, parameters, paramSig);

        Object response = getRemotingClient().invoke(request);

        if (response instanceof Throwable) {
            throw (Throwable) response;
        }

        return response == null ? null : expectedReturnType.cast(response);

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

        Method loginMethod = SubjectManagerRemote.class.getDeclaredMethod("login", String.class, String.class);

        try {
            this.subject = remoteInvoke(RhqManager.SubjectManager, loginMethod, Subject.class, user, password);
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception("Failed to login due to a throwable of type " + e.getClass().getName(), e);
        }

        this.loggedIn = true;

        return this.subject;
    }

    /**
     * Logs out from the server and disconnects this client.
     */
    public void logout() {
        if (this.loggedIn && this.subject != null) {
            try {
                Method logoutMethod = SubjectManagerRemote.class.getDeclaredMethod("logout", Subject.class);
                remoteInvoke(RhqManager.SubjectManager, logoutMethod, Void.class, this.subject);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                    "Couldn't find the logout method on the SubjectManagerRemote interface.", e);
            } catch (Throwable e) {
                // just keep going so we can disconnect this client
            }
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

    public URI getRemoteURI() {
        try {
            return new URI(getTransport(), null, getHost(), getPort(), null, null, null);
        } catch (URISyntaxException e) {
            //does not happen, but hey
            LOG.error("Error creating the remote URI with transport, host and port: " + getTransport() + ", "
                + getHost() + " and " + getPort(), e);
            return null;
        }
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

    /**
     * Returns the map of all remote managers running in the server that this
     * client can talk to.
     *
     * @return Map K=manager name V=remote proxy
     */
    public Map<RhqManager, Object> getScriptingAPI() {
        if (this.managers == null) {

            this.managers = new HashMap<RhqManager, Object>();

            for (RhqManager manager : RhqManager.values()) {
                if (manager.enabled()) {
                    try {
                        Object proxy = getProcessor(this, manager, true);
                        this.managers.put(manager, proxy);
                    } catch (Throwable e) {
                        LOG.error("Failed to load manager " + manager + " due to missing class.", e);
                    }
                }
            }
        }

        return this.managers;
    }

    @Override
    public <T> T getProxy(Class<T> remoteApiIface) {
        RhqManager manager = RhqManager.forInterface(remoteApiIface);

        if (manager == null) {
            throw new IllegalArgumentException("Unknown remote interface " + remoteApiIface);
        }

        return getProcessor(this, manager, false);
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

    /**
     * If the client is connected, this is version of the server that the client is talking to.
     *
     * @return remote server version
     */
    public String getServerVersion() {
        return (this.serverInfo != null) ? this.serverInfo.getVersion() : null;
    }

    /**
     * If the client is connected, this is update version of the server that the client is talking to. Ex. Update 02
     *
     * @return remote server version
     */
    public String getServerVersionUpdate() {
        return (this.serverInfo != null) ? this.serverInfo.getVersionUpdate() : null;
    }

    /**
     * If the client is connected, this is build number of the server that the client is talking to.
     *
     * @return remote server build number
     */
    public String getServerBuildNumber() {
        return (this.serverInfo != null) ? this.serverInfo.getBuildNumber() : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getProcessor(RemoteClient remoteClient, RhqManager manager, boolean simplify) {
        try {
            RemoteClientProxy gpc = new RemoteClientProxy(remoteClient, manager);

            Class<?> intf = simplify ? InterfaceSimplifier.simplify(manager.remote()) : manager.remote();

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { intf },
                gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote connection proxy", e);
        }
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
            this.serverInfo = null;
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

        // make sure the remote server can support this client
        try {
            Method getProductInfoMethod = SystemManagerRemote.class.getDeclaredMethod("getProductInfo", Subject.class);

            this.serverInfo = remoteInvoke(RhqManager.SystemManager, getProductInfoMethod, ProductInfo.class,
                this.subject);
            checkServerSupported(this.serverInfo);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find the getProductInfo(Subject) method on the SystemManager.",
                e);
        } catch (Exception e) {
            // our client cannot be supported by the server - disconnect and rethrow the exception
            doDisconnect();
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Unknown error occured during connect.", e);
        }
    }

    private Map<String, String> buildRemotingConfig(String locatorURI) {
        Map<String, String> config = new HashMap<String, String>();
        if (SecurityUtil.isTransportSecure(locatorURI)) {
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "data/keystore.dat");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, (System.getProperty("java.vendor", "")
                .contains("IBM") ? "IbmX509" : "SunX509"));
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
                SecurityUtil.createKeyStore(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH),
                    config.get(SSLSocketBuilder.REMOTING_KEY_ALIAS), "CN=RHQ, OU=RedHat, O=redhat.com, C=US",
                    config.get(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD),
                    config.get(SSLSocketBuilder.REMOTING_KEY_PASSWORD), "DSA", 36500);

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

    /**
     * Checks to see if the server (whose version information is passed in) supports this client.
     * This performs version checks and throws an IllegalStateException if the server does not
     * support this client.
     *
     * @param serverVersionInfo the information about the remote server
     *
     * @throws IllegalStateException if the remote server does not support this client
     */
    private void checkServerSupported(ProductInfo serverVersionInfo) throws IllegalStateException {
        boolean supported;
        String serverVersionString;
        final String propName = "rhq.client.version-check";
        final String versionCheckProp = System.getProperty(propName, "true");

        if (!versionCheckProp.equalsIgnoreCase("true")) {
            return;
        }
        String clientVersionString = System.getProperty("rhq.client.version", null);
        try {
            if (clientVersionString == null) {
                clientVersionString = getClass().getPackage().getImplementationVersion();
            }
            if (clientVersionString == null) {
                clientVersionString = " undefined ";
            }
            serverVersionString = this.serverInfo.getVersion();
            ComparableVersion clientVersion = new ComparableVersion(clientVersionString);
            ComparableVersion serverVersion = new ComparableVersion(serverVersionString);
            int laterVersionCheck = clientVersion.compareTo(serverVersion);
            if (laterVersionCheck >= 0) {
                supported = true; //Ex. 3.2.0.GA-redhat-N represent supported non-breaking api patches/changes.
            } else {
                supported = false;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine if server version is supported.", e); // assume we can't talk to it
        }

        if (!supported) {
            String errMsg = "This client [" + clientVersionString + "] does not support the remote server ["
                + serverVersionString + "]";
            throw new IllegalStateException(errMsg);
        }
    }
}
