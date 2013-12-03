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
package org.rhq.enterprise.agent;

import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

/**
 * These are the names of the known agent configuration preferences. All configuration preferences are stored in flat
 * properties (there is no hierarchy - simply a set of name/value pairs). This makes the configuration settings
 * conducive to being overridden by system properties.
 *
 * @author John Mazzitelli
 */
public interface AgentConfigurationConstants {
    /**
     * This is the top level parent node of all agent preferences and is directly under the userRoot preferences node.
     */
    String PREFERENCE_NODE_PARENT = "rhq-agent";

    /**
     * This is the name of the preference node under the {@link #PREFERENCE_NODE_PARENT} where all agent configuration
     * is stored by default.
     */
    String DEFAULT_PREFERENCE_NODE = "default";

    /**
     * This is the name of the agent configuration file.
     */
    String DEFAULT_AGENT_CONFIGURATION_FILE = "agent-configuration.xml";

    /**
     * The prefix that all agent configuration property names start with.
     */
    String PROPERTY_NAME_PREFIX = "rhq.agent.";

    /**
     * The configuration schema version.
     */
    String CONFIG_SCHEMA_VERSION = PROPERTY_NAME_PREFIX + "configuration-schema-version";

    /**
     * This is the current schema version that our agent configuration knows about.
     */
    int CURRENT_CONFIG_SCHEMA_VERSION = 8;

    /**
     * Flag to indicate if the agent's configuration has been setup.
     */
    String CONFIG_SETUP = PROPERTY_NAME_PREFIX + "configuration-setup-flag";

    /**
     * If defined, this configuration item is what the agent's name is. This is usually (but doesn't have to be) the
     * agent platform's fully qualified domain name.
     */
    String NAME = PROPERTY_NAME_PREFIX + "name";

    /**
     * The transport that the JON server expects its messages to flow over.
     */
    String SERVER_TRANSPORT = PROPERTY_NAME_PREFIX + "server.transport";

    /**
     * The default server transport if one was not specified.
     */
    String DEFAULT_SERVER_TRANSPORT = "servlet";

    /**
     * The address that identifies where the JON server is.
     */
    String SERVER_BIND_ADDRESS = PROPERTY_NAME_PREFIX + "server.bind-address";

    /**
     * The default server transport if one was not specified.
     */
    String DEFAULT_SERVER_BIND_ADDRESS = "127.0.0.1";

    /**
     * The port that the JON server is listening to.
     */
    String SERVER_BIND_PORT = PROPERTY_NAME_PREFIX + "server.bind-port";

    /**
     * The default server transport if one was not specified.
     */
    int DEFAULT_SERVER_BIND_PORT = 7080;

    /**
     * The transport params that further define how to communicate with the JON server.
     */
    String SERVER_TRANSPORT_PARAMS = PROPERTY_NAME_PREFIX + "server.transport-params";

    /**
     * The default server transport params if none were specified.
     */
    String DEFAULT_SERVER_TRANSPORT_PARAMS = "/jboss-remoting-servlet-invoker/ServerInvokerServlet";

    /**
     * The DNS alias for the RHQ Server - used when the server IP address is not explicitly set.
     */
    String SERVER_ALIAS = PROPERTY_NAME_PREFIX + "server.alias";

    /**
     * The flag that, if <code>true</code>, will tell the agent to attempt to auto-detect the JON Server coming online
     * and going offline.
     */
    String SERVER_AUTO_DETECTION = PROPERTY_NAME_PREFIX + "server-auto-detection";

    /**
     * If the server auto-detection property is not specified, this is the default. It is the same default as that of
     * the enable flag for the
     * {@link ServiceContainerConfigurationConstants#DEFAULT_MULTICASTDETECTOR_ENABLED server-side multicast detector service}
     */
    boolean DEFAULT_SERVER_AUTO_DETECTION = ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED;

    /**
     * The flag that, if <code>true</code>, will tell the agent to attempt to register itself with the JON Server at
     * startup.
     */
    String REGISTER_WITH_SERVER_AT_STARTUP = PROPERTY_NAME_PREFIX + "register-with-server-at-startup";

    /**
     * If the register with server at startup property is not specified, this is the default.
     */
    boolean DEFAULT_REGISTER_WITH_SERVER_AT_STARTUP = true;

    /**
     * The flag that, if <code>true</code>, means the agent is allowed to apply updates to itself.
     */
    String AGENT_UPDATE_ENABLED = PROPERTY_NAME_PREFIX + "agent-update.enabled";

    /**
     * If the agent update enabled flag is not set, this is the default.
     */
    boolean DEFAULT_AGENT_UPDATE_ENABLED = true;

    /**
     * If this preference is defined (its default is null), this will be the URL that contains the agent update version info.
     */
    String AGENT_UPDATE_VERSION_URL = PROPERTY_NAME_PREFIX + "agent-update.version-url";

    /**
     * If this preference is defined (its default is null), this will be the URL the agent downloads the agent update from.
     */
    String AGENT_UPDATE_DOWNLOAD_URL = PROPERTY_NAME_PREFIX + "agent-update.download-url";

    /**
     * The amount of milliseconds the agent will wait at startup for the server to be detected.
     */
    String WAIT_FOR_SERVER_AT_STARTUP_MSECS = PROPERTY_NAME_PREFIX + "wait-for-server-at-startup-msecs";

    /**
     * If the wait-for-server wait time is not defined, this is the default.
     */
    long DEFAULT_WAIT_FOR_SERVER_AT_STARTUP_MSECS = 60000L;

    /**
     * The amount of milliseconds between checks that ensure the agent is pointing to the primary server.
     */
    String PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS = PROPERTY_NAME_PREFIX
        + "primary-server-switchover-check-interval-msecs";

    /**
     * If the switchover-check-interval is not defined, this is the default.
     */
    long DEFAULT_PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS = 1000L * 60 * 60;

    /**
     * The amount of milliseconds between checks of the agent's VM health.
     */
    String VM_HEALTH_CHECK_INTERVAL_MSECS = PROPERTY_NAME_PREFIX + "vm-health-check.interval-msecs";

    /**
     * If the vm-health-check interval is not defined, this is its default.
     */
    long DEFAULT_VM_HEALTH_CHECK_INTERVAL_MSECS = 5000L;

    /**
     * The percentage threshold (as a float) of used heap to max that is considered to be critically low free mem.
     */
    String VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD = PROPERTY_NAME_PREFIX + "vm-health-check.low-heap-mem-threshold";

    /**
     * If the heap threshold is not defined, this is its default.
     */
    float DEFAULT_VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD = 0.90f;

    /**
     * The percentage threshold (as a float) of used nonheap to max that is considered to be critically low free mem.
     */
    String VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD = PROPERTY_NAME_PREFIX
        + "vm-health-check.low-nonheap-mem-threshold";

    /**
     * If the nonheap threshold is not defined, this is its default.
     */
    float DEFAULT_VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD = 0.90f;

    /**
     * The flag that, if <code>true</code>, will tell the agent to attempt to update its plugins at startup.
     */
    String UPDATE_PLUGINS_AT_STARTUP = PROPERTY_NAME_PREFIX + "update-plugins-at-startup";

    /**
     * If the update plugins at startup property is not specified, this is the default.
     */
    boolean DEFAULT_UPDATE_PLUGINS_AT_STARTUP = true;

    /**
     * The flag that, if <code>true</code>, will tell the agent to test connectivity to all servers in the failover list.
     */
    String TEST_FAILOVER_LIST_AT_STARTUP = PROPERTY_NAME_PREFIX + "test-failover-list-at-startup";

    /**
     * If the test failover list at startup property is not specified, this is the default.
     */
    boolean DEFAULT_TEST_FAILOVER_LIST_AT_STARTUP = false;

    /**
     * The file path to the location of the agent's data directory (where the agent will persist its internal data).
     */
    String DATA_DIRECTORY = PROPERTY_NAME_PREFIX + "data-directory";

    /**
     * If the data directory property is not specified, this is the default.
     */
    String DEFAULT_DATA_DIRECTORY = "data";

    /**
     * The flag that, if <code>true</code>, will turn off/disable the use of the native system libraries.
     */
    String DISABLE_NATIVE_SYSTEM = "rhq.agent.disable-native-system";

    /**
     * By default, we will allow the agent to use the native system if it is available.
     */
    boolean DEFAULT_DISABLE_NATIVE_SYSTEM = false;

    /**
     * The maximum size of the command queue - this is the maximum number of commands that can be queued for sending.
     */
    String CLIENT_SENDER_QUEUE_SIZE = PROPERTY_NAME_PREFIX + "client.queue-size";

    /**
     * If the client sender queue size is not specified, this is the default.
     */
    int DEFAULT_CLIENT_SENDER_QUEUE_SIZE = 50000;

    /**
     * The maximum number of concurrent commands that can be in the process of being sent at any one time.
     */
    String CLIENT_SENDER_MAX_CONCURRENT = PROPERTY_NAME_PREFIX + "client.max-concurrent";

    /**
     * If the client sender max concurrent value is not specified, this is the default.
     */
    int DEFAULT_CLIENT_SENDER_MAX_CONCURRENT = 5;

    /**
     * The time in milliseconds that the client sender will wait before aborting a command. This is the amount of time
     * in milliseconds that the server has in order to process commands. A command can override this by setting its own
     * timeout in the command's configuration.
     */
    String CLIENT_SENDER_COMMAND_TIMEOUT = PROPERTY_NAME_PREFIX + "client.command-timeout-msecs";

    /**
     * If the client sender command timeout is not specified, this is the default.
     */
    long DEFAULT_CLIENT_SENDER_COMMAND_TIMEOUT = 600000L;

    /**
     * The time in milliseconds that the client sender will wait in between polling the JON Server. If this is 0 or
     * less, server polling will be disabled.
     */
    String CLIENT_SENDER_SERVER_POLLING_INTERVAL = PROPERTY_NAME_PREFIX + "client.server-polling-interval-msecs";

    /**
     * If the client sender server polling interval is not specified, this is the default.
     */
    long DEFAULT_CLIENT_SENDER_SERVER_POLLING_INTERVAL = 60000L;

    /**
     * If this property is defined, it indicates the send throttling configuration. For example, maximum number of
     * commands before a quiet period must begin, the length of time of each quiet period).
     */
    String CLIENT_SENDER_SEND_THROTTLING = PROPERTY_NAME_PREFIX + "client.send-throttling";

    /**
     * If this property is defined, it indicates the queue throttling configuration For example, maximum number of
     * commands per burst, the length of time of each burst period).
     */
    String CLIENT_SENDER_QUEUE_THROTTLING = PROPERTY_NAME_PREFIX + "client.queue-throttling";

    /**
     * Property that provides the spool file name (as it is found in the data directory).
     */
    String CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME = PROPERTY_NAME_PREFIX + "client.command-spool-file.name";

    /**
     * If the client sender command spool file name is not specified, this is the default.
     */
    String DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME = "command-spool.dat";

    /**
     * Property that provides the spool file parameters.
     */
    String CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS = PROPERTY_NAME_PREFIX + "client.command-spool-file.params";

    /**
     * If the client sender command spool file parameters are not specified, this is the default.
     */
    String DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS = "10000000:75";

    /**
     * Property that indicates if the spool file's data is compressed.
     */
    String CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED = PROPERTY_NAME_PREFIX + "client.command-spool-file.compressed";

    /**
     * If the client sender command spool file compress flag is not specified, this is the default.
     */
    boolean DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED = false;

    /**
     * Property that provides the amount of time, in milliseconds, that the sender will pause before attempting to retry
     * a failed command whose delivery is to be guaranteed.
     */
    String CLIENT_SENDER_RETRY_INTERVAL = PROPERTY_NAME_PREFIX + "client.retry-interval-msecs";

    /**
     * If the client sender retry pause is not specified, this is the default.
     */
    long DEFAULT_CLIENT_SENDER_RETRY_INTERVAL = 15000L;

    /**
     * Number of maximum retry attempts are made for messages with guaranteed delivery enabled (unless the reason for
     * the failure was a failure to connect to the server, in which case, the messages are retried regardless of this
     * setting).
     */
    String CLIENT_SENDER_MAX_RETRIES = PROPERTY_NAME_PREFIX + "client.max-retries";

    /**
     * If the client sender max retries is not specified, this is the default.
     */
    int DEFAULT_CLIENT_SENDER_MAX_RETRIES = 10;

    /**
     * Property whose value is the fully qualified class name of the command preprocessor that the sender will use to
     * preprocess commands that are to be queued and sent. The value may actually define 0, 1 or more preprocessor
     * classnames.
     */
    String CLIENT_SENDER_COMMAND_PREPROCESSORS = PROPERTY_NAME_PREFIX + "client.command-preprocessors";

    /**
     * The default preprocessor which is the security token preprocessor.
     */
    String DEFAULT_CLIENT_SENDER_COMMAND_PREPROCESSORS = SecurityTokenCommandPreprocessor.class.getName() + ":"
        + ExternalizableStrategyCommandPreprocessor.class.getName();

    /**
     * The secure protocol used by the agent's communications layer to the remote server.
     */
    String CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL = PROPERTY_NAME_PREFIX + "client.security.secure-socket-protocol";

    /**
     * The default protocol when securing the communications.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL = "TLS";

    /**
     * The alias to the client's key found in the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS = PROPERTY_NAME_PREFIX + "client.security.keystore.alias";

    /**
     * Default alias of keystore's key.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS = "rhq";

    /**
     * The path to the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_FILE = PROPERTY_NAME_PREFIX + "client.security.keystore.file";

    /**
     * Default keystore file <b>name</b> - when building a default keystore file string, this string will be relative to
     * the data directory preference.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_FILE_NAME = "keystore.dat";

    /**
     * The algorithm used to manage the keys in the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "client.security.keystore.algorithm";

    /**
     * Default algorithm for the keystore.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM = (System.getProperty("java.vendor", "").contains("IBM") ? "IbmX509"
        : "SunX509");

    /**
     * The type of keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_TYPE = PROPERTY_NAME_PREFIX + "client.security.keystore.type";

    /**
     * Default keystore file format.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_TYPE = "JKS";

    /**
     * The password used to access the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "client.security.keystore.password";

    /**
     * The password to gain access to the key found in the keystore.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_KEY_PASSWORD = PROPERTY_NAME_PREFIX
        + "client.security.keystore.key-password";

    /**
     * The path to the truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE = PROPERTY_NAME_PREFIX + "client.security.truststore.file";

    /**
     * Default truststore file <b>name</b> - when building a default truststore file string, this string will be
     * relative to the data directory preference.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE_NAME = "truststore.dat";

    /**
     * The algorithm used to manage the keys in the truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "client.security.truststore.algorithm";

    /**
     * Default algorithm for the truststore.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM = (System.getProperty("java.vendor", "").contains("IBM") ? "IbmX509"
        : "SunX509");

    /**
     * The type of truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE = PROPERTY_NAME_PREFIX + "client.security.truststore.type";

    /**
     * Default truststore file format.
     */
    String DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE = "JKS";

    /**
     * The password used to access the truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "client.security.truststore.password";

    /**
     * The server authentication mode that, when enabled, forces the agent to authenticate the remote server's
     * certificate with one in the agent's trust store. If <code>false</code>, no server authentication is performed;
     * any remote server is allowed to communicate with the agent.
     */
    String CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE = PROPERTY_NAME_PREFIX + "client.security.server-auth-mode-enabled";

    /**
     * If the client sender server auth mode is not specified, this is the default.
     */
    boolean DEFAULT_CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE = false;

    /**
     * When this configuration item is defined, it is the security token string the agent needs to include in its
     * commands to the server in order for those commands to be accepted by the server.
     */
    String AGENT_SECURITY_TOKEN = PROPERTY_NAME_PREFIX + "security-token";

    /**
     * When this configuration item is defined, it is the list of public endpoints to use to connect to the server
     * cloud.  This list may be a single element in a single-server environment, or may be a list of elements in a
     * high availability setup.
     */
    String AGENT_FAILOVER_LIST = PROPERTY_NAME_PREFIX + "failover-list";

    /**
     * Flag, if set to <code>true</code>, will tell the agent it should not create and register any management services
     * (thus making the agent unable to manage or monitor itself).
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     */
    String DO_NOT_ENABLE_MANAGEMENT_SERVICES = PROPERTY_NAME_PREFIX + "do-not-enable-management-services";

    /**
     * Flag, if set to <code>true</code>, will tell the agent it should not start the Plugin Container at startup. In
     * this case, the only way for the agent to start the plugin container would be with the plugin container prompt
     * command via the agent command line.
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     */
    String DO_NOT_START_PLUGIN_CONTAINER_AT_STARTUP = PROPERTY_NAME_PREFIX + "do-not-start-pc-at-startup";

    /**
     * Flag, if set to <code>true</code>, will tell the agent it should not tell the server that the agent is shutting
     * down. In this case, the agent shutdown will be faster because it won't try to send a message to the server,
     * however, it will cause the server to not know of the agent's unavailability.
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     */
    String DO_NOT_NOTIFY_SERVER_OF_SHUTDOWN = PROPERTY_NAME_PREFIX + "do-not-notify-server-of-shutdown";

    /**
     * Flag, if set to <code>true</code>, will tell the agent to ignore system properties and not override preferences
     * with any system property value.
     */
    String DO_NOT_OVERRIDE_PREFS_WITH_SYSPROPS = PROPERTY_NAME_PREFIX + "do-not-override-prefs-with-sysprops";

    /**
     * The location where the plugins can be found.
     */
    String PLUGINS_DIRECTORY = PROPERTY_NAME_PREFIX + "plugins.directory";

    /**
     * The default directory where the plugins can be found.
     */
    String DEFAULT_PLUGINS_DIRECTORY = "plugins";

    /**
     * The regular expression to indicate what agent/plugin container classes the plugins cannot access.
     */
    String PLUGINS_ROOT_PLUGIN_CLASSLOADER_REGEX = PROPERTY_NAME_PREFIX + "plugins.root-plugin-classloader-regex";

    /**
     * The comma separated list of names of plugins that are to be disabled at startup
     */
    String PLUGINS_DISABLED = PROPERTY_NAME_PREFIX + "plugins.disabled";

    /**
     * The |-separated list of names of resource types that are to be disabled at startup.
     * Values are things like "plugin name>parent>child>grandchild|plugin name2>type".
     */
    String PLUGINS_DISABLED_RESOURCE_TYPES = PROPERTY_NAME_PREFIX + "plugins.disabled-resource-types";

    /**
     * Defines, in seconds, the initial delay before the first server discovery scan is run.
     */
    String PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.server-discovery.initial-delay-secs";

    /**
     * The default initial delay, in seconds.
     */
    long DEFAULT_PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY = PluginContainerConfiguration.SERVER_DISCOVERY_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, how often a server discovery scan is run.
     */
    String PLUGINS_SERVER_DISCOVERY_PERIOD = PROPERTY_NAME_PREFIX + "plugins.server-discovery.period-secs";

    /**
     * The default time period between each server discovery scan, in seconds.
     */
    long DEFAULT_PLUGINS_SERVER_DISCOVERY_PERIOD = PluginContainerConfiguration.SERVER_DISCOVERY_PERIOD_DEFAULT;

    /**
     * Defines, in seconds, the initial delay before the first service discovery scan is run.
     */
    String PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.service-discovery.initial-delay-secs";

    /**
     * The default initial delay, in seconds.
     */
    long DEFAULT_PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY = PluginContainerConfiguration.SERVICE_DISCOVERY_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, how often a service discovery scan is run.
     */
    String PLUGINS_SERVICE_DISCOVERY_PERIOD = PROPERTY_NAME_PREFIX + "plugins.service-discovery.period-secs";

    /**
     * The default time period between each service discovery scan, in seconds.
     */
    long DEFAULT_PLUGINS_SERVICE_DISCOVERY_PERIOD = PluginContainerConfiguration.SERVICE_DISCOVERY_PERIOD_DEFAULT;

    /**
     * Defines, in seconds, the delay between resource committed into the inventory and child resource discovery scan
     */
    String PLUGINS_CHILD_RESOURCE_DISOVERY_PERIOD = PROPERTY_NAME_PREFIX + "plugins.child-discovery.delay-secs";

    /**
     * The default time period between resource committed into the inventory and child discovery scan, in seconds.
     */
    long DEFAULT_PLUGINS_CHILD_RESOURCE_DISCOVERY_PERIOD = PluginContainerConfiguration.CHILD_RESOURCE_DISCOVERY_DELAY_DEFAULT;

    /**
     * Defines, in seconds, the initial delay before the first availability scan is run.
     */
    String PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.availability-scan.initial-delay-secs";

    /**
     * The default initial delay, in seconds.
     */
    long DEFAULT_PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY = PluginContainerConfiguration.AVAILABILITY_SCAN_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, how often a availability scan is run.
     */
    String PLUGINS_AVAILABILITY_SCAN_PERIOD = PROPERTY_NAME_PREFIX + "plugins.availability-scan.period-secs";

    /**
     * The default time period between each availability scan, in seconds.
     */
    long DEFAULT_PLUGINS_AVAILABILITY_SCAN_PERIOD = PluginContainerConfiguration.AVAILABILITY_SCAN_PERIOD_DEFAULT;

    /**
     * Defines how many threads can be concurrently scanning for resource availabilities.
     */
    String PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE = PROPERTY_NAME_PREFIX
        + "plugins.availability-scan.threadpool-size";

    /**
     * The default threadpool size for availability scanning.
     */
    int DEFAULT_PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE = PluginContainerConfiguration.AVAILABILITY_SCAN_THREADPOOL_SIZE_DEFAULT;

    /**
     * If defined, this is to be the size of the measurement collection thread pool. If not defined, the plugin
     * container should default to something it considers appropriate.
     */
    String PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE = PROPERTY_NAME_PREFIX
        + "plugins.measurement-collection.threadpool-size";

    /**
     * The default number of measurements that can be collected concurrently.
     */
    int DEFAULT_PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE = PluginContainerConfiguration.MEASUREMENT_COLLECTION_THREADCOUNT_DEFAULT;

    /**
     * Defines, in seconds, the initial delay before the first measurement collection is run.
     */
    String PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.measurement-collection.initial-delay-secs";

    /**
     * The default initial delay, in seconds.
     */
    long DEFAULT_PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY = PluginContainerConfiguration.MEASUREMENT_COLLECTION_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, the initial delay before the first drift detection scan is run.
     */
    String PLUGINS_DRIFT_DETECTION_INITIAL_DELAY = PROPERTY_NAME_PREFIX + "plugins.drift-detection.initial-delay-secs";

    /**
     * The default initial delay of the first drift detection scan, in seconds.
     */
    long DEFAULT_PLUGINS_DRIFT_DETECTION_INITIAL_DELAY = PluginContainerConfiguration.DRIFT_DETECTION_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, how often a drift detection scan is run.
     */
    String PLUGINS_DRIFT_DETECTION_PERIOD = PROPERTY_NAME_PREFIX + "plugins.drift-detection.period-secs";

    /**
     * The default time period between each drift detection scan, in seconds.
     */
    long DEFAULT_PLUGINS_DRIFT_DETECTION_PERIOD = PluginContainerConfiguration.DRIFT_DETECTION_PERIOD_DEFAULT;

    /**
     * If defined, this is to be the size of the content discovery thread pool. If not defined, the plugin container
     * should default to something it considers appropriate.
     */
    String PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE = PROPERTY_NAME_PREFIX
        + "plugins.content-discovery.threadpool-size";

    int DEFAULT_PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE = PluginContainerConfiguration.CONTENT_DISCOVERY_THREADCOUNT_DEFAULT;

    /**
     * Defines, in seconds, the initial delay before the first content discovery is run.
     */
    String PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.content-discovery.initial-delay-secs";

    /**
     * The default initial delay, in seconds.
     */
    long DEFAULT_PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY = PluginContainerConfiguration.CONTENT_DISCOVERY_INITIAL_DELAY_DEFAULT;

    /**
     * Defines, in seconds, how often an content discovery is run.
     */
    String PLUGINS_CONTENT_DISCOVERY_PERIOD = PROPERTY_NAME_PREFIX + "plugins.content-discovery.period-secs";

    /**
     * The default time period between each content discovery, in seconds.
     */
    long DEFAULT_PLUGINS_CONTENT_DISCOVERY_PERIOD = PluginContainerConfiguration.CONTENT_DISCOVERY_PERIOD_DEFAULT;

    /** Defines the delay to starting Configuration discoveries */
    String PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY = PROPERTY_NAME_PREFIX
        + "plugins.configuration-discovery.initial-delay-secs";

    long DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY = PluginContainerConfiguration.CONFIGURATION_DISCOVERY_INITIAL_DELAY_DEFAULT;

    /* Defines the period of configuration chnage detection checks */
    String PLUGINS_CONFIGURATION_DISCOVERY_PERIOD = PROPERTY_NAME_PREFIX
        + "plugins.configuration-discovery.period-secs";

    long DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_PERIOD = PluginContainerConfiguration.CONFIGURATION_DISCOVERY_PERIOD_DEFAULT;

    /**
     * If defined, this is to be the size of the operation invoker thread pool. If not defined, the plugin container
     * should default to something it considers appropriate.
     */
    String PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE = PROPERTY_NAME_PREFIX
        + "plugins.operation-invoker.threadpool-size";

    /**
     * The default number of operations that can be invoked concurrently.
     */
    int DEFAULT_PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE = PluginContainerConfiguration.OPERATION_INVOKER_THREADCOUNT_DEFAULT;

    /**
     * If defines, this is the number of seconds an operation invocation is aborted if it hasn't completed yet.
     */
    String PLUGINS_OPERATION_INVOCATION_TIMEOUT = PROPERTY_NAME_PREFIX + "plugins.operation-invocation-timeout-secs";

    /**
     * The default number of seconds that an operation invocation has to complete until it is aborted.
     */
    long DEFAULT_PLUGINS_OPERATION_INVOCATION_TIMEOUT = PluginContainerConfiguration.OPERATION_INVOCATION_TIMEOUT_DEFAULT;

    /**
     * The time in seconds before the event sender thread will start to send event reports.
     */
    String PLUGINS_EVENT_SENDER_INITIAL_DELAY = PROPERTY_NAME_PREFIX + "plugins.event-sender.initial-delay-secs";
    long DEFAULT_PLUGINS_EVENT_SENDER_INITIAL_DELAY = PluginContainerConfiguration.EVENT_SENDER_INITIAL_DELAY_DEFAULT;

    /**
     * Defines how often an event report is sent to the server.
     */
    String PLUGINS_EVENT_SENDER_PERIOD = PROPERTY_NAME_PREFIX + "plugins.event-sender.period-secs";
    long DEFAULT_PLUGINS_EVENT_SENDER_PERIOD = PluginContainerConfiguration.EVENT_SENDER_PERIOD_DEFAULT;

    /**
     * The maximum number of events for any one event source that can exist in a single event report.
     */
    String PLUGINS_EVENT_REPORT_MAX_PER_SOURCE = PROPERTY_NAME_PREFIX + "plugins.event-report.max-per-source";
    int DEFAULT_PLUGINS_EVENT_REPORT_MAX_PER_SOURCE = PluginContainerConfiguration.EVENT_REPORT_MAX_PER_SOURCE_DEFAULT;

    /**
     * The maximum number of events total that can exist in a single event report.
     */
    String PLUGINS_EVENT_REPORT_MAX_TOTAL = PROPERTY_NAME_PREFIX + "plugins.event-report.max-total";
    int DEFAULT_PLUGINS_EVENT_REPORT_MAX_TOTAL = PluginContainerConfiguration.EVENT_REPORT_MAX_TOTAL_DEFAULT;
}