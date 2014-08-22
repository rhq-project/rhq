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

import org.rhq.core.util.obfuscation.ObfuscatedPreferences.Restricted;

/**
 * These are the names of the known server configuration preferences. All configuration preferences are stored in flat
 * properties (there is no hierarchy - simply a set of name/value pairs).
 *
 * @author John Mazzitelli
 */
public interface ServerConfigurationConstants {
    /**
     * This is the top leve parent node of all server preferences and is directly under the userRoot preferences node.
     */
    String PREFERENCE_NODE_PARENT = "rhq-server";

    /**
     * This is the name of the preference node under the {@link #PREFERENCE_NODE_PARENT} where all server configuration
     * is stored by default.
     */
    String DEFAULT_PREFERENCE_NODE = "default";

    /**
     * This is the name of the default server configuration file.
     */
    String DEFAULT_SERVER_CONFIGURATION_FILE = "server-comm-configuration.xml";

    /**
     * The prefix that all server configuration property names start with.
     */
    String PROPERTY_NAME_PREFIX = "rhq.server.";

    /**
     * The prefix that all server configuration property names start with.
     */
    String COMMUNICATIONS_PROPERTY_NAME_PREFIX = "rhq.communications.";

    /**
     * The configuration schema version.
     */
    String CONFIG_SCHEMA_VERSION = PROPERTY_NAME_PREFIX + "configuration-schema-version";

    /**
     * This is the current schema version that our server configuration knows about.
     */
    int CURRENT_CONFIG_SCHEMA_VERSION = 1;

    /**
     * The directory location that contains all files that can be distributed to agents.
     */
    String AGENT_FILES_DIRECTORY = PROPERTY_NAME_PREFIX + "agent-files-directory";

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
    int DEFAULT_CLIENT_SENDER_MAX_CONCURRENT = 1;

    /**
     * The time in milliseconds that the client sender will wait before aborting a command. This is the amount of time
     * in milliseconds that the server has in order to process commands. A command can override this by setting its own
     * timeout in the command's configuration.
     */
    String CLIENT_SENDER_COMMAND_TIMEOUT = PROPERTY_NAME_PREFIX + "client.command-timeout-msecs";

    /**
     * If the client sender command timeout is not specified, this is the default.
     */
    long DEFAULT_CLIENT_SENDER_COMMAND_TIMEOUT = -1L;

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
     * A null value means by default, the server has guaranteed delivery disabled.
     */
    String DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME = null;

    /**
     * Property that provides the spool file parameters.
     */
    String CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS = PROPERTY_NAME_PREFIX + "client.command-spool-file.params";

    /**
     * If the client sender command spool file parameters are not specified, this is the default.
     */
    String DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS = "1000000:75";

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
     * Number of times a guaranteed message is retried, if it failed for some reason other than a "cannot connect". If
     * it cannot connect, it always retries and this is ignored.
     */
    String CLIENT_SENDER_MAX_RETRIES = PROPERTY_NAME_PREFIX + "client.max_retries";

    /**
     * If the client sender max retries is not specified, this is the default.
     */
    int DEFAULT_CLIENT_SENDER_MAX_RETRIES = 10;

    /**
     * The secure protocol used by the server's communications layer to the remote agents.
     */
    String CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL = PROPERTY_NAME_PREFIX + "client.security.secure-socket-protocol";

    /**
     * The alias to the client's key found in the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS = PROPERTY_NAME_PREFIX + "client.security.keystore.alias";

    /**
     * The path to the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_FILE = PROPERTY_NAME_PREFIX + "client.security.keystore.file";

    /**
     * The algorithm used to manage the keys in the keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "client.security.keystore.algorithm";

    /**
     * The type of keystore file.
     */
    String CLIENT_SENDER_SECURITY_KEYSTORE_TYPE = PROPERTY_NAME_PREFIX + "client.security.keystore.type";

    /**
     * The password used to access the keystore file.
     */
    @Restricted
    String CLIENT_SENDER_SECURITY_KEYSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "client.security.keystore.password";

    /**
     * The password to gain access to the key found in the keystore.
     */
    @Restricted
    String CLIENT_SENDER_SECURITY_KEYSTORE_KEY_PASSWORD = PROPERTY_NAME_PREFIX
        + "client.security.keystore.key-password";

    /**
     * The path to the truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE = PROPERTY_NAME_PREFIX + "client.security.truststore.file";

    /**
     * The algorithm used to manage the keys in the truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM = PROPERTY_NAME_PREFIX + "client.security.truststore.algorithm";

    /**
     * The type of truststore file.
     */
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE = PROPERTY_NAME_PREFIX + "client.security.truststore.type";

    /**
     * The password used to access the truststore file.
     */
    @Restricted
    String CLIENT_SENDER_SECURITY_TRUSTSTORE_PASSWORD = PROPERTY_NAME_PREFIX + "client.security.truststore.password";

    /**
     * The server authentication mode that, when enabled, forces the server to authenticate the remote agent's
     * certificate with one in the server's trust store. If <code>false</code>, no server authentication is performed;
     * any remote agent is allowed to communicate with the server.
     */
    String CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE = PROPERTY_NAME_PREFIX + "client.security.server-auth-mode-enabled";

    /**
     * If the client sender server auth mode is not specified, this is the default.
     */
    boolean DEFAULT_CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE = false;

    /**
     * Communications security keystore key password
    */
    @Restricted
    String COMMUNICATIONS_CONNECTOR_SECURITY_KEYSTORE_KEY_PASSWORD = COMMUNICATIONS_PROPERTY_NAME_PREFIX + "connector.security.keystore.key-password";

    /**
     * Communications security keystore password
     */
    @Restricted
    String COMMUNICATIONS_CONNECTOR_SECURITY_KEYSTORE_PASSWORD = COMMUNICATIONS_PROPERTY_NAME_PREFIX + "connector.security.keystore.password";

    /**
     * Communications security truststore password
     */
    @Restricted
    String COMMUNICATIONS_CONNECTOR_SECURITY_TRUSTSTORE_PASSWORD = COMMUNICATIONS_PROPERTY_NAME_PREFIX + "connector.security.truststore.password";
}