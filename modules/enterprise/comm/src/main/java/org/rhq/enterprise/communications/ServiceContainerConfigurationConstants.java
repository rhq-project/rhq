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

import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.enterprise.communications.command.server.CommandAuthenticator;

/**
 * These are the names of the known communications services configuration properties.
 *
 * @author John Mazzitelli
 */
public interface ServiceContainerConfigurationConstants {
    /**
     * The rhqtype that indicates the communciations server is running in a RHQ Agent.
     */
    String RHQTYPE_AGENT = "agent";

    /**
     * The rhqtype that indicates the communciations server is running in a RHQ Server.
     */
    String RHQTYPE_SERVER = "server";

    /**
     * The prefix that all service configuration property names start with.
     */
    String PROPERTY_NAME_PREFIX = "rhq.communications.";

    /**
     * The configuration schema version.
     */
    String CONFIG_SCHEMA_VERSION = PROPERTY_NAME_PREFIX + "configuration-schema-version";

    /**
     * This is the current schema version that our service container configuration knows about.
     */
    int CURRENT_CONFIG_SCHEMA_VERSION = 1;

    /**
     * The global concurrency limit setting. This defines the max number of messages that can be received concurrently.
     */
    String GLOBAL_CONCURRENCY_LIMIT = PROPERTY_NAME_PREFIX + "global-concurrency-limit";

    /**
     * The name of the MBeanServer that will be created to house all the services. This is actually the name of the
     * default domain of the MBeanServer.
     */
    String MBEANSERVER_NAME = PROPERTY_NAME_PREFIX + "service-container.mbean-server-name";

    /**
     * The flag to indicate if the multicast detector should be enabled or not.
     */
    String MULTICASTDETECTOR_ENABLED = PROPERTY_NAME_PREFIX + "multicast-detector.enabled";

    /**
     * By default, the multicast detector is disabled.
     */
    boolean DEFAULT_MULTICASTDETECTOR_ENABLED = false;

    /**
     * The multicast detector's multicast address used for network registry messages (servers coming and going). This is
     * the IP of the multicast group that the detector will join.
     */
    String MULTICASTDETECTOR_ADDRESS = PROPERTY_NAME_PREFIX + "multicast-detector.multicast-address";

    /**
     * The multicast detector's bind address.
     */
    String MULTICASTDETECTOR_BINDADDRESS = PROPERTY_NAME_PREFIX + "multicast-detector.bind-address";

    /**
     * The port (over the multicast address) where detection messages are sent to.
     */
    String MULTICASTDETECTOR_PORT = PROPERTY_NAME_PREFIX + "multicast-detector.port";

    /**
     * The amount of time (milliseconds) which can elapse without receiving a detection event before a server will be
     * suspected as being dead and performing an explicit invocation on it to verify it is alive.
     */
    String MULTICASTDETECTOR_DEFAULT_TIMEDELAY = PROPERTY_NAME_PREFIX + "multicast-detector.default-time-delay";

    /**
     * The amount of time (milliseconds) to wait between sending (and sometimes receiving) detection messages.
     */
    String MULTICASTDETECTOR_HEARTBEAT_TIMEDELAY = PROPERTY_NAME_PREFIX + "multicast-detector.heartbeat-time-delay";

    /**
     * The connector's "type" which will indicate if the connector lives in a RHQ Agent or RHQ Server.
     */
    String CONNECTOR_RHQTYPE = PROPERTY_NAME_PREFIX + "connector.rhqtype";

    /**
     * The default connector RHQ type if not specified.
     */
    String DEFAULT_CONNECTOR_RHQTYPE = RHQTYPE_AGENT;

    /**
     * The remoting transport protocol of the server endpoint.
     */
    String CONNECTOR_TRANSPORT = PROPERTY_NAME_PREFIX + "connector.transport";

    /**
     * The default remoting transport protocol.
     */
    String DEFAULT_CONNECTOR_TRANSPORT = "socket";

    /**
     * The server endpoint will bind to this address. To bind to all addresses you use "0.0.0.0".
     */
    String CONNECTOR_BIND_ADDRESS = PROPERTY_NAME_PREFIX + "connector.bind-address";

    /**
     * The server endpoint will listen to this port for incoming requests.
     */
    String CONNECTOR_BIND_PORT = PROPERTY_NAME_PREFIX + "connector.bind-port";

    /**
     * The default connector bind port.
     */
    int DEFAULT_CONNECTOR_BIND_PORT = 16163;

    /**
     * Additional transport configuration parameters can be specified in this property. See JBoss/Remoting documentation
     * on the transport parameters. Example: enableTcpNoDelay=false&amp;clientMaxPoolSize=30
     */
    String CONNECTOR_TRANSPORT_PARAMS = PROPERTY_NAME_PREFIX + "connector.transport-params";

    /**
     * The default transport parameters.
     */
    String DEFAULT_CONNECTOR_TRANSPORT_PARAMS = "numAcceptThreads=1&maxPoolSize=303&clientMaxPoolSize=304&socketTimeout=60000&enableTcpNoDelay=true&backlog=200&generalizeSocketException=true";

    /**
     * The number of milliseconds that should be used when establishing the client lease period (meaning the client will
     * need to update its lease within this amount of time or will be considered dead).
     */
    String CONNECTOR_LEASE_PERIOD = PROPERTY_NAME_PREFIX + "connector.lease-period";

    /**
     * The secure protocol used by the socket communications layer.
     */
    String CONNECTOR_SECURITY_SOCKET_PROTOCOL = PROPERTY_NAME_PREFIX + "connector.security.secure-socket-protocol";

    /**
     * The default protocol when securing the communications.
     */
    String DEFAULT_CONNECTOR_SECURITY_SOCKET_PROTOCOL = "TLS";

    /**
     * The client authentication mode which indicates if the client does not need to be authenticated, the client
     * should, but is not required to, be authenticated, or the client is required to be authenticated. The actual
     * property value must be one of {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE},
     * {@link SSLSocketBuilder#CLIENT_AUTH_MODE_WANT}, or {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NEED}.
     */
    String CONNECTOR_SECURITY_CLIENT_AUTH_MODE = PROPERTY_NAME_PREFIX + "connector.security.client-auth-mode";

    /**
     * The default client auth mode.
     */
    String DEFAULT_CONNECTOR_SECURITY_CLIENT_AUTH_MODE = SSLSocketBuilder.CLIENT_AUTH_MODE_NONE;

    /**
     * The path to the keystore file containing the server-side key.
     */
    String CONNECTOR_SECURITY_KEYSTORE_FILE = PROPERTY_NAME_PREFIX + "connector.security.keystore.file";

    /**
     * Default keystore file <b>name</b> - when building a default keystore file string, this string will be relative to
     * the data directory preference.
     */
    String DEFAULT_CONNECTOR_SECURITY_KEYSTORE_FILE_NAME = "keystore.dat";

    /**
     * The algorithm used to manage the keys in the keystore.
     */
    String CONNECTOR_SECURITY_KEYSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "connector.security.keystore.algorithm";

    /**
     * Default algorithm for the keystore.
     */
    String DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALGORITHM = (System.getProperty("java.vendor", "").contains("IBM") ? "IbmX509"
        : "SunX509");

    /**
     * The type of keystore which defines the keystore file format.
     */
    String CONNECTOR_SECURITY_KEYSTORE_TYPE = PROPERTY_NAME_PREFIX + "connector.security.keystore.type";

    /**
     * Default keystore file format.
     */
    String DEFAULT_CONNECTOR_SECURITY_KEYSTORE_TYPE = "JKS";

    /**
     * The password to keystore file itself.
     */
    String CONNECTOR_SECURITY_KEYSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "connector.security.keystore.password";

    /**
     * The password to gain access to the key found in the keystore.
     */
    String CONNECTOR_SECURITY_KEYSTORE_KEY_PASSWORD = PROPERTY_NAME_PREFIX + "connector.security.keystore.key-password";

    /**
     * The alias of the key in the keystore.
     */
    String CONNECTOR_SECURITY_KEYSTORE_ALIAS = PROPERTY_NAME_PREFIX + "connector.security.keystore.alias";

    /**
     * Default alias of keystore's key.
     */
    String DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALIAS = "rhq";

    /**
     * The path to the truststore file.
     */
    String CONNECTOR_SECURITY_TRUSTSTORE_FILE = PROPERTY_NAME_PREFIX + "connector.security.truststore.file";

    /**
     * Default truststore file <b>name</b> - when building a default truststore file string, this string will be
     * relative to the data directory preference.
     */
    String DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_FILE_NAME = "truststore.dat";

    /**
     * The algorithm used to manage the keys in the truststore file.
     */
    String CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "connector.security.truststore.algorithm";

    /**
     * Default algorithm for the truststore.
     */
    String DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM = (System.getProperty("java.vendor", "").contains("IBM") ? "IbmX509"
        : "SunX509");

    /**
     * The type of truststore file.
     */
    String CONNECTOR_SECURITY_TRUSTSTORE_TYPE = PROPERTY_NAME_PREFIX + "connector.security.truststore.type";

    /**
     * Default truststore file format.
     */
    String DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_TYPE = "JKS";

    /**
     * The password used to access the truststore file.
     */
    String CONNECTOR_SECURITY_TRUSTSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "connector.security.truststore.password";

    /**
     * If <code>true</code>, the command service directory will automatically detect new command services and will use
     * them as soon as they get hot deployed.
     */
    String CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY = PROPERTY_NAME_PREFIX
        + "command-service-directory.allow-dynamic-discovery";

    /**
     * The default that enables the command service directory to perform dynamic discovery.
     */
    boolean DEFAULT_CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY = true;

    /**
     * The comma-separated list of command service class names - these are the command services that are immediately
     * deployed when the server communications are started.
     */
    String CMDSERVICES = PROPERTY_NAME_PREFIX + "command-services";

    /**
     * The comma-separated list of POJOs that are to be remoted. These define what POJOs to instantiate and immediately
     * remote when the server communications are started. The format of value is: "pojo-class:remote-interface,..."
     */
    String REMOTE_POJOS = PROPERTY_NAME_PREFIX + "remote-pojos";

    /**
     * If this property is <code>true</code>, then the remote communications services will be disabled. That means that
     * the service container will not start any remoting services.
     */
    String DISABLE_COMMUNICATIONS = PROPERTY_NAME_PREFIX + "disable-communications";

    /**
     * By default, communications are not disabled.
     */
    boolean DEFAULT_DISABLE_COMMUNICATIONS = false;

    /**
     * This is the location where we want to store internal data files.
     */
    String DATA_DIRECTORY = PROPERTY_NAME_PREFIX + "data-directory";

    /**
     * The default data directory.
     */
    String DEFAULT_DATA_DIRECTORY = "data";

    /**
     * The maximum amount of milliseconds a remoted stream hosted in a server is allowed to be idle before it is
     * automatically closed and removed.
     */
    String REMOTE_STREAM_MAX_IDLE_TIME = PROPERTY_NAME_PREFIX + "remote-stream-max-idle-time-msecs";

    /**
     * The default max idle time for the remote streams.
     */
    long DEFAULT_REMOTE_STREAM_MAX_IDLE_TIME = 300000L;

    /**
     * A fully qualified class name of the {@link CommandAuthenticator} implementation that will be used to authenticate
     * incoming commands.
     */
    String COMMAND_AUTHENTICATOR = PROPERTY_NAME_PREFIX + "command-authenticator";
}