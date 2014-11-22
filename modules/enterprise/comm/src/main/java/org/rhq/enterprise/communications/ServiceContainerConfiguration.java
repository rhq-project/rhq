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
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mazz.i18n.Logger;

import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;
import org.rhq.enterprise.communications.command.impl.echo.server.EchoCommandService;
import org.rhq.enterprise.communications.command.impl.identify.server.IdentifyCommandService;
import org.rhq.enterprise.communications.command.impl.stream.server.RemoteInputStreamCommandService;
import org.rhq.enterprise.communications.command.impl.stream.server.RemoteOutputStreamCommandService;
import org.rhq.enterprise.communications.command.server.CommandAuthenticator;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.ConcurrencyManager;

/**
 * Just provides some convienence methods to extract the service container configuration properties. These are remoting
 * server configuration properties that are common on both the RHQ Server and RHQ Agent used to configure the
 * communications services.
 *
 * @author John Mazzitelli
 */
public class ServiceContainerConfiguration {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ServiceContainerConfiguration.class);

    /**
     * The configuration properties this object wraps. This must be either the RHQ Server or RHQ Agent preferences node.
     */
    private final Preferences m_preferences;

    /**
     * Wraps a preferences object in this instance.
     *
     * @param  prefs the service container configuration preferences
     *
     * @throws IllegalArgumentException if <code>prefs</code> is <code>null</code>
     */
    public ServiceContainerConfiguration(Preferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs=null");
        }

        m_preferences = prefs;
    }

    /**
     * Returns the raw preferences containing the service container configuration.
     *
     * @return the configuration preferences
     */
    public Preferences getPreferences() {
        return m_preferences;
    }

    /**
     * Returns the version of the configuration schema.
     *
     * @return configuration version; if the configuration isn't versioned, 0 is returned
     */
    public int getServiceContainerConfigurationVersion() {
        int value = m_preferences.getInt(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, 0);

        return value;
    }

    /**
     * This tags the existing preferences by setting the configuration schema version preference appropriately.
     */
    public void tagWithServiceContainerConfigurationVersion() {
        m_preferences.putInt(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION,
            ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        flush(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION);
    }

    /**
     * Returns the global concurrency limit on the number of commands that are allowed to be received concurrently (i.e.
     * the counting semaphore name as known to the {@link ConcurrencyManager}).
     *
     * <p>If this returns a value of 0 or less, there will be no global limit on the number of messages that can be
     * received and processed concurrently.</p>
     *
     * @return the global concurrency limit value
     */
    public int getGlobalConcurrencyLimit() {
        int value = m_preferences.getInt(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT, 0);
        return value;
    }

    /**
     * Returns the name of the MBeanServer that will be created to house all the services. This is actually the default
     * domain name of the MBeanServer. If an MBeanServer already exists and is registered with this default domain name,
     * it will be used to house the communications services. If the MBeanServer name does not exist in the
     * configuration, <code>null</code> will be returned, which indicates that the
     * {@link ManagementFactory#getPlatformMBeanServer() JVM's platform MBeanServer} should be used.
     *
     * @return the MBeanServer name
     */
    public String getMBeanServerName() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.MBEANSERVER_NAME, null);
        return value;
    }

    /**
     * Returns <code>true</code> if the communication services should be disabled, <code>false</code> if they should be
     * created normally. It is rare that you would want to configure this setting to be <code>true</code>.
     *
     * @return the flag to indicate if communications should be disabled
     */
    public boolean isCommunicationsDisabled() {
        boolean value = m_preferences.getBoolean(ServiceContainerConfigurationConstants.DISABLE_COMMUNICATIONS,
            ServiceContainerConfigurationConstants.DEFAULT_DISABLE_COMMUNICATIONS);
        return value;
    }

    /**
     * Returns the data directory where all internally persisted data can be written to. If the data directory does not
     * exist, it will be created.
     *
     * <p>Because this does alittle extra work, it may not be suitable to call if you just want to get the value of the
     * data directory or if you just want to know if its defined or not. In those instances, use
     * {@link #getDataDirectoryIfDefined()} instead.</p>
     *
     * @return the data directory
     */
    public File getDataDirectory() {
        String dirStr = m_preferences.get(ServiceContainerConfigurationConstants.DATA_DIRECTORY,
            ServiceContainerConfigurationConstants.DEFAULT_DATA_DIRECTORY);
        File dir = new File(dirStr);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    /**
     * This will return the data directory string as found in the preferences. If the data directory is not defined in
     * the preferences, <code>null</code> is returned.
     *
     * @return the data directory string as defined in the preferences or <code>null</code> if it is not defined
     *
     * @see    #getDataDirectory()
     */
    public String getDataDirectoryIfDefined() {
        String dir_str = m_preferences.get(ServiceContainerConfigurationConstants.DATA_DIRECTORY, null);

        return dir_str;
    }

    /**
     * Returns the flag to indicate if the multicast detector should be enabled or not.
     *
     * @return the multicast detector enable flag (<code>true</code> means the detector should be enabled)
     */
    public boolean isMulticastDetectorEnabled() {
        boolean value = m_preferences.getBoolean(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED);
        return value;
    }

    /**
     * Returns the multicast detector's multicast address used for network registry messages (servers coming and going).
     * This is the IP of the multicast group that the detector will join.
     *
     * @return the multicast detector multicast address (<code>null</code> if not set in the preferences)
     */
    public String getMulticastDetectorMulticastAddress() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ADDRESS, null);
        return value;
    }

    /**
     * Returns the multicast detector's bind address which is what the network interface binds to.
     *
     * @return the multicast detector bind address (<code>null</code> if not set in the preferences)
     */
    public String getMulticastDetectorBindAddress() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_BINDADDRESS, null);
        return value;
    }

    /**
     * Returns the multicast detector's default time delay. This is the amount of time, in milliseconds, which can
     * elapse without receiving a detection event before suspecting that a server is dead and performing an explicit
     * invocation on it to verify it is alive. If this invocation, or ping, fails, the server will be removed from the
     * network registry. This should be greater than {@link #getMulticastDetectorHeartbeatTimeDelay()}.
     *
     * @return the multicast detector default time delay (<code>Long.MIN_VALUE</code> if not set in the preferences)
     */
    public long getMulticastDetectorDefaultTimeDelay() {
        long value = m_preferences.getLong(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_DEFAULT_TIMEDELAY,
            Long.MIN_VALUE);
        return value;
    }

    /**
     * Returns the multicast detector's heartbeat time delay. This is the amount of time to wait between sending (and
     * sometimes receiving) detection messages. This should be less than {@link #getMulticastDetectorDefaultTimeDelay()}
     * .
     *
     * @return the multicast detector heartbeat time delay (<code>Long.MIN_VALUE</code> if not set in the preferences)
     */
    public long getMulticastDetectorHeartbeatTimeDelay() {
        long value = m_preferences.getLong(
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_HEARTBEAT_TIMEDELAY, Long.MIN_VALUE);
        return value;
    }

    /**
     * Returns the multicast detector's port.
     *
     * @return the multicast detector port (<code>Integer.MIN_VALUE</code> if not set in the preferences)
     */
    public int getMulticastDetectorPort() {
        int value = m_preferences.getInt(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_PORT,
            Integer.MIN_VALUE);
        return value;
    }

    /**
     * Returns the connector's RHQ type (indicating if the remoting server resides in the RHQ Server or RHQ Agent).
     *
     * @return the RHQ type
     */
    public String getConnectorRHQType() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_RHQTYPE,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_RHQTYPE);
        return value;
    }

    /**
     * Returns the connector's transport type (socket, http, etc).
     *
     * @return the connector transport type
     */
    public String getConnectorTransport() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT);
        return value;
    }

    /**
     * Returns the connector's bind address.
     *
     * @return the connector bind address
     */
    public String getConnectorBindAddress() {
        String defaultBindAddress;

        try {
            defaultBindAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            defaultBindAddress = "127.0.0.1";
        }

        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS,
            defaultBindAddress);
        return value;
    }

    /**
     * Returns the connector's bind port.
     *
     * @return the connector's bind port
     */
    public int getConnectorBindPort() {
        int value = m_preferences.getInt(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_BIND_PORT);
        return value;
    }

    /**
     * Returns the connector's transport parameters which are used to further customize the connector and its transport
     * subsystem.
     *
     * @return the connector transport parameters
     */
    public String getConnectorTransportParams() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT_PARAMS);

        // see BZ 1166383
        final String requiredParamName = "generalizeSocketException";
        if (value != null) {
            if (!value.contains(requiredParamName)) {
                if (value.startsWith("/")) {
                    if (!value.contains("/?")) {
                        if (!value.endsWith("/")) {
                            value += "/";
                        }
                        value += "?";
                    } else {
                        value += "&";
                    }
                } else {
                    value += "&";
                }
                value += requiredParamName + "=true";
            }
        } else {
            value = requiredParamName + "=true";
        }

        return value;
    }

    /**
     * This is a convienence method that builds a full remote endpoint string that combines
     * {@link #getConnectorTransport()}, {@link #getConnectorBindAddress()}, {@link #getConnectorBindPort()},
     * {@link #getConnectorTransportParams()} and {@link #getConnectorRHQType()}.
     *
     * @return a remote endpoint string that can be used by remote clients to connect to the connector.
     */
    public String getConnectorRemoteEndpoint() {
        String rhqtype = getConnectorRHQType();

        if (!rhqtype.equals(ServiceContainerConfigurationConstants.RHQTYPE_AGENT)
            && !rhqtype.equals(ServiceContainerConfigurationConstants.RHQTYPE_SERVER)) {
            LOG.warn(CommI18NResourceKeys.SERVICE_CONTAINER_INVALID_RHQTYPE, rhqtype,
                ServiceContainerConfigurationConstants.RHQTYPE_SERVER,
                ServiceContainerConfigurationConstants.RHQTYPE_AGENT);

            rhqtype = ServiceContainerConfigurationConstants.RHQTYPE_AGENT;
        }

        String rhqtype_param = ServiceContainerConfigurationConstants.CONNECTOR_RHQTYPE + "=" + rhqtype;
        String transport = getConnectorTransport();
        String bind_address = getConnectorBindAddress();
        int bind_port = getConnectorBindPort();
        String transport_params = getConnectorTransportParams().trim();

        // build the locator URI from the configuration properties
        String locator_uri = transport + "://" + bind_address + ":" + bind_port;

        if (transport_params.length() == 0) {
            transport_params = "/?" + rhqtype_param;
        } else {
            // for some transports (e.g. servlet) the params will actually be the rest of the URL with options query string
            // to denote that we don't want to start the query string immediately after host:port, the params can start with /
            if (transport_params.startsWith("/")) {
                if (transport_params.indexOf('?') == -1) {
                    transport_params += "?" + rhqtype_param;
                } else {
                    transport_params += "&" + rhqtype_param;
                }
            } else {
                transport_params = "/?" + rhqtype_param + "&" + transport_params;
            }
        }

        locator_uri += transport_params;

        return locator_uri;
    }

    /**
     * Returns the connector's lease period, in milliseconds. When this period is greater than 0, it enables the
     * capability for the connector to detect when a client is no longer available. This is done by estabilishing a
     * lease with the remoting clients.
     *
     * @return the connector's lease period (<code>Long.MIN_VALUE</code> if not set in the preferences)
     */
    public long getConnectorLeasePeriod() {
        long value = m_preferences.getLong(ServiceContainerConfigurationConstants.CONNECTOR_LEASE_PERIOD,
            Long.MIN_VALUE);
        return value;
    }

    /**
     * Returns the protocol used over the secure socket.
     *
     * @return protocol name
     */
    public String getConnectorSecuritySocketProtocol() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_SOCKET_PROTOCOL);
        return value;
    }

    /**
     * Returns the client authentication mode. It is either:}
     *
     * <ul>
     *   <li>{@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE} - clients are not authenticated</li>
     *   <li>{@link SSLSocketBuilder#CLIENT_AUTH_MODE_WANT} - clients are authenticated if they provide a cert; if they
     *     do not, anonymous connections are allowed</li>
     *   <li>{@link SSLSocketBuilder#CLIENT_AUTH_MODE_NEED} - clients must supply a cert and they must be
     *     authenticated</li>
     * </ul>
     *
     * The default is {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE} - that will be returned if the client auth mode was
     * not found in the configuration.
     *
     * <i>Note:</i> To support Tomcat syntax, the client authentication mode value is allowed to be "true" or "false".
     * "true" maps to {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NEED},
     * "false" maps to {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE}.
     *
     * @return the client authentication mode
     */
    public String getConnectorSecurityClientAuthMode() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_CLIENT_AUTH_MODE);

        if (value.equalsIgnoreCase("true")) {
            value = SSLSocketBuilder.CLIENT_AUTH_MODE_NEED;
        } else if (value.equalsIgnoreCase("false")) {
            value = SSLSocketBuilder.CLIENT_AUTH_MODE_NONE;
        }

        if (!value.equals(SSLSocketBuilder.CLIENT_AUTH_MODE_NONE)
            && !value.equals(SSLSocketBuilder.CLIENT_AUTH_MODE_WANT)
            && !value.equals(SSLSocketBuilder.CLIENT_AUTH_MODE_NEED)) {
            LOG.warn(CommI18NResourceKeys.SERVICE_CONTAINER_CONFIGURATION_INVALID_CLIENT_AUTH,
                ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE, value,
                SSLSocketBuilder.CLIENT_AUTH_MODE_NONE, SSLSocketBuilder.CLIENT_AUTH_MODE_WANT,
                SSLSocketBuilder.CLIENT_AUTH_MODE_NEED, SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

            value = SSLSocketBuilder.CLIENT_AUTH_MODE_NEED;
        }

        return value;
    }

    /**
     * Returns the path to the keystore file. This returns a <code>String</code> as opposed to <code>File</code> since
     * some underlying remoting code may allow for this filepath to be relative to a jar inside the classloader.
     *
     * @return keystore file path
     */
    public String getConnectorSecurityKeystoreFile() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_FILE, null);

        if (value == null) {
            value = new File(getDataDirectory(),
                ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_FILE_NAME).getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the keystore.
     *
     * @return key manager algorithm
     */
    public String getConnectorSecurityKeystoreAlgorithm() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_ALGORITHM,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALGORITHM);
        return value;
    }

    /**
     * Returns the type of keystore.
     *
     * @return keystore type
     */
    public String getConnectorSecurityKeystoreType() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_TYPE,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_TYPE);
        return value;
    }

    /**
     * Returns the password to the keystore file itself.
     *
     * @return keystore password
     */
    public String getConnectorSecurityKeystorePassword() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD,
            "rhqpwd");
        return value;
    }

    /**
     * Returns the password to gain access to the key in the keystore. If no key password is configured, this returns
     * the {@link #getConnectorSecurityKeystorePassword() keystore password}.
     *
     * @return password to the key
     */
    public String getConnectorSecurityKeystoreKeyPassword() {
        String value = m_preferences.get(
            ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_KEY_PASSWORD, null);
        if (value == null) {
            value = getConnectorSecurityKeystorePassword();
        }

        return value;
    }

    /**
     * Returns the key alias of the key in the keystore.
     *
     * @return key alias name
     */
    public String getConnectorSecurityKeystoreAlias() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_ALIAS,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALIAS);
        return value;
    }

    /**
     * Returns the path to the truststore file. This returns a <code>String</code> as opposed to <code>File</code> since
     * some underlying remoting code may allow for this filepath to be relative to a jar inside the classloader.
     *
     * @return truststore file path
     */
    public String getConnectorSecurityTruststoreFile() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            null);

        if (value == null) {
            value = new File(getDataDirectory(),
                ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_FILE_NAME)
                .getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the truststore.
     *
     * @return algorithm name
     */
    public String getConnectorSecurityTruststoreAlgorithm() {
        String value = m_preferences.get(
            ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM);
        return value;
    }

    /**
     * Returns the type of the truststore file.
     *
     * @return truststore file type
     */
    public String getConnectorSecurityTruststoreType() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_TYPE,
            ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_TYPE);
        return value;
    }

    /**
     * Returns the password of the truststore file itself.
     *
     * @return truststore file password
     */
    public String getConnectorSecurityTruststorePassword() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_PASSWORD,
            null);
        return value;
    }

    /**
     * Returns the command services that are to be automatically deployed when the services are started up.
     *
     * @return the set of command services to be automatically deployed at startup
     */
    public String getStartupCommandServices() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.CMDSERVICES, EchoCommandService.class
            .getName()
            + "," + IdentifyCommandService.class.getName());
        return value;
    }

    /**
     * Returns the definitions of the POJOs that are to be automatically deployed as remote services at start up.
     *
     * @return the set of POJOs to remote at startup
     */
    public String getStartupRemotePojos() {
        String value = m_preferences.get(ServiceContainerConfigurationConstants.REMOTE_POJOS, PingImpl.class.getName()
            + ":" + Ping.class.getName());
        return value;
    }

    /**
     * Returns the maximum amount of milliseconds a remote {@link RemoteOutputStream output} or
     * {@link RemoteInputStream input} stream server-side component hosted in a {@link RemoteOutputStreamCommandService}
     * or {@link RemoteInputStreamCommandService} is allowed to be idle before it is automatically closed and removed.
     * This means that a client must attempt to access the remote stream every X milliseconds (where X is the value this
     * method returns) or that stream will no longer be available. Note that this does not mean a client must read/write
     * the entire stream in this amount of time, it only means a client must make a request on the stream every X
     * milliseconds (be it to read or write a byte, mark it, see how many bytes are available, etc).
     *
     * @return the max idle time in milliseconds
     */
    public long getRemoteStreamMaxIdleTime() {
        long value = m_preferences.getLong(ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME,
            ServiceContainerConfigurationConstants.DEFAULT_REMOTE_STREAM_MAX_IDLE_TIME);

        return value;
    }

    /**
     * Returns the flag to indicate if the command service directory has been enabled to perform dynamic discovery of
     * new command services that are added to (or old services removed from) the system after the initial startup
     * discovery.
     *
     * @return <code>true</code> if dynamic discovery of command services is enabled; <code>false</code> if the initial
     *         set of {@link #getStartupCommandServices() } are the only ones that will be enabled during the lifetime
     *         of the service container
     */
    public boolean isCommandServiceDirectoryDynamicDiscoveryEnabled() {
        boolean value = m_preferences.getBoolean(
            ServiceContainerConfigurationConstants.CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY,
            ServiceContainerConfigurationConstants.DEFAULT_CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY);
        return value;
    }

    /**
     * Returns an implementation of the configured {@link CommandAuthenticator} that should be used to authenticate
     * incoming commands.
     *
     * @return command authenticator object instance or <code>null</code> if no authenticator is configured
     *
     * @throws Exception if failed to instantiate the configured class name
     */
    public CommandAuthenticator getCommandAuthenticator() throws Exception {
        CommandAuthenticator obj = null;
        String value = m_preferences.get(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR, "");

        if ((value != null) && (value.length() > 0)) {
            obj = (CommandAuthenticator) Class.forName(value).newInstance();
        }

        return obj;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(m_preferences.absolutePath());

        buf.append('[');

        try {
            String[] keys = m_preferences.keys();

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                buf.append(key);
                buf.append('=');
                buf.append(m_preferences.get(key, LOG
                    .getMsgString(CommI18NResourceKeys.SERVICE_CONTAINER_CONFIGURATION_UNKNOWN)));

                if ((i + 1) < keys.length) {
                    buf.append(',');
                }
            }
        } catch (BackingStoreException e) {
            buf.append('<');
            buf.append(LOG.getMsgString(CommI18NResourceKeys.SERVICE_CONTAINER_CONFIGURATION_CANNOT_GET_PREFS, e));
            buf.append('>');
        }

        buf.append(']');

        return buf.toString();
    }

    /**
     * Forces the preferences to flush so they get written to the backing store.
     *
     * @param changedPreference the name of the preference that was changed to
     *                          cause flush to be called (used for error log message)
     */
    private void flush(String changedPreference) {
        try {
            m_preferences.flush();
        } catch (Exception e) {
            LOG.warn(LOG.getMsgString(CommI18NResourceKeys.CANNOT_STORE_PREFERENCES), changedPreference, e);
        }
    }
}