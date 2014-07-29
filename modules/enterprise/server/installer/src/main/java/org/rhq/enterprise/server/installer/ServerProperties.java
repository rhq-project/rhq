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
package org.rhq.enterprise.server.installer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.obfuscation.PicketBoxObfuscator;

/**
 * Settings found in the rhq-server.properties file that controls the startup configuration of the server.
 *
 * @author John Mazzitelli
 */
public class ServerProperties {
    public static final String PROP_FILENAME = "rhq-server.properties";

    public static final String PROP_DATABASE_TYPE = "rhq.server.database.type-mapping";
    public static final String PROP_DATABASE_CONNECTION_URL = "rhq.server.database.connection-url";
    public static final String PROP_DATABASE_USERNAME = "rhq.server.database.user-name";
    public static final String PROP_DATABASE_PASSWORD = "rhq.server.database.password";
    public static final String PROP_DATABASE_SERVER_NAME = "rhq.server.database.server-name";
    public static final String PROP_DATABASE_PORT = "rhq.server.database.port";
    public static final String PROP_DATABASE_DB_NAME = "rhq.server.database.db-name";
    public static final String PROP_DATABASE_HIBERNATE_DIALECT = "hibernate.dialect";

    public static final String PROP_QUARTZ_DRIVER_DELEGATE_CLASS = "rhq.server.quartz.driverDelegateClass";
    public static final String PROP_QUARTZ_SELECT_WITH_LOCK_SQL = "rhq.server.quartz.selectWithLockSQL";
    public static final String PROP_QUARTZ_LOCK_HANDLER_CLASS = "rhq.server.quartz.lockHandlerClass";

    public static final String PROP_WEB_HTTP_PORT = "rhq.server.socket.binding.port.http";
    public static final String PROP_WEB_HTTPS_PORT = "rhq.server.socket.binding.port.https";

    public static final String PROP_EMAIL_SMTP_HOST = "rhq.server.email.smtp-host";
    public static final String PROP_EMAIL_SMTP_PORT = "rhq.server.email.smtp-port";
    public static final String PROP_EMAIL_FROM_ADDRESS = "rhq.server.email.from-address";

    public static final String PROP_AUTOINSTALL_ENABLE = "rhq.autoinstall.enabled";
    public static final String PROP_AUTOINSTALL_DATABASE = "rhq.autoinstall.database";
    public static final String PROP_AUTOINSTALL_PUBLIC_ADDR = "rhq.autoinstall.public-endpoint-address";
    public static final String PROP_AUTOINSTALL_ADMIN_PASSWORD = "rhq.autoinstall.server.admin.password";

    public static final String PROP_TOMCAT_SECURITY_CLIENT_AUTH_MOD = "rhq.server.tomcat.security.client-auth-mode";
    public static final String PROP_TOMCAT_SECURITY_SSL_PROTOCOL = "rhq.server.tomcat.security.secure-socket-protocol";
    public static final String PROP_TOMCAT_SECURITY_ALGORITHM = "rhq.server.tomcat.security.algorithm";
    public static final String PROP_TOMCAT_SECURITY_KEYSTORE_ALIAS = "rhq.server.tomcat.security.keystore.alias";
    public static final String PROP_TOMCAT_SECURITY_KEYSTORE_FILENAME = "rhq.server.tomcat.security.keystore.file";
    public static final String PROP_TOMCAT_SECURITY_KEYSTORE_PASSWORD = "rhq.server.tomcat.security.keystore.password";
    public static final String PROP_TOMCAT_SECURITY_KEYSTORE_TYPE = "rhq.server.tomcat.security.keystore.type";
    public static final String PROP_TOMCAT_SECURITY_TRUSTSTORE_FILENAME = "rhq.server.tomcat.security.truststore.file";
    public static final String PROP_TOMCAT_SECURITY_TRUSTSTORE_PASSWORD = "rhq.server.tomcat.security.truststore.password";
    public static final String PROP_TOMCAT_SECURITY_TRUSTSTORE_TYPE = "rhq.server.tomcat.security.truststore.type";

    public static final String PROP_CONNECTOR_TRANSPORT = "rhq.communications.connector.transport";
    public static final String PROP_CONNECTOR_BIND_ADDRESS = "rhq.communications.connector.bind-address";
    public static final String PROP_CONNECTOR_BIND_PORT = "rhq.communications.connector.bind-port";
    public static final String PROP_CONNECTOR_TRANSPORT_PARAMS = "rhq.communications.connector.transport-params";

    public static final String PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL = "rhq.communications.connector.security.secure-socket-protocol";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_FILE = "rhq.communications.connector.security.keystore.file";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM = "rhq.communications.connector.security.keystore.algorithm";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_TYPE = "rhq.communications.connector.security.keystore.type";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_PASSWORD = "rhq.communications.connector.security.keystore.password";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD = "rhq.communications.connector.security.keystore.key-password";
    public static final String PROP_SECURITY_SERVER_KEYSTORE_ALIAS = "rhq.communications.connector.security.keystore.alias";
    public static final String PROP_SECURITY_SERVER_TRUSTSTORE_FILE = "rhq.communications.connector.security.truststore.file";
    public static final String PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM = "rhq.communications.connector.security.truststore.algorithm";
    public static final String PROP_SECURITY_SERVER_TRUSTSTORE_TYPE = "rhq.communications.connector.security.truststore.type";
    public static final String PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD = "rhq.communications.connector.security.truststore.password";
    public static final String PROP_SECURITY_SERVER_CLIENT_AUTH_MODE = "rhq.communications.connector.security.client-auth-mode";

    public static final String PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL = "rhq.server.client.security.secure-socket-protocol";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_FILE = "rhq.server.client.security.keystore.file";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM = "rhq.server.client.security.keystore.algorithm";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_TYPE = "rhq.server.client.security.keystore.type";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD = "rhq.server.client.security.keystore.password";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD = "rhq.server.client.security.keystore.key-password";
    public static final String PROP_SECURITY_CLIENT_KEYSTORE_ALIAS = "rhq.server.client.security.keystore.alias";
    public static final String PROP_SECURITY_CLIENT_TRUSTSTORE_FILE = "rhq.server.client.security.truststore.file";
    public static final String PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM = "rhq.server.client.security.truststore.algorithm";
    public static final String PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE = "rhq.server.client.security.truststore.type";
    public static final String PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD = "rhq.server.client.security.truststore.password";
    public static final String PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED = "rhq.server.client.security.server-auth-mode-enabled";

    public static final String PROP_AGENT_MULTICAST_DETECTOR_ENABLED = "rhq.communications.multicast-detector.enabled";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS = "rhq.communications.multicast-detector.bind-address";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS = "rhq.communications.multicast-detector.multicast-address";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_PORT = "rhq.communications.multicast-detector.port";

    public static final String PROP_CONCURRENCY_LIMIT_WEBCONNS = "rhq.server.startup.web.max-connections";
    public static final String PROP_CONCURRENCY_LIMIT_GLOBAL = "rhq.communications.global-concurrency-limit";
    public static final String PROP_CONCURRENCY_LIMIT_INV_REPORT = "rhq.server.concurrency-limit.inventory-report";
    public static final String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT = "rhq.server.concurrency-limit.availability-report";
    public static final String PROP_CONCURRENCY_LIMIT_INV_SYNC = "rhq.server.concurrency-limit.inventory-sync";
    public static final String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT = "rhq.server.concurrency-limit.content-report";
    public static final String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD = "rhq.server.concurrency-limit.content-download";
    public static final String PROP_CONCURRENCY_LIMIT_MEAS_REPORT = "rhq.server.concurrency-limit.measurement-report";
    public static final String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ = "rhq.server.concurrency-limit.measurement-schedule-request";

    public static final String PROP_JBOSS_BIND_ADDRESS = "jboss.bind.address";
    public static final String PROP_HIGH_AVAILABILITY_NAME = "rhq.server.high-availability.name";
    public static final String PROP_MM_AT_START = "rhq.server.maintenance-mode-at-startup";
    public static final String PROP_OPERATION_TIMEOUT = "rhq.server.operation-timeout";
    public static final String PROP_LOG_LEVEL = "rhq.server.log-level";

    public static final String PROP_MGMT_USER_PASSWORD = "rhq.server.management.password";

    public static final String PROP_STORAGE_USERNAME = "rhq.storage.username";
    public static final String PROP_STORAGE_PASSWORD = "rhq.storage.password";
    public static final String PROP_STORAGE_NODES = "rhq.storage.nodes";
    public static final String PROP_STORAGE_CQL_PORT = "rhq.storage.cql-port";
    public static final String PROP_STORAGE_GOSSIP_PORT = "rhq.storage.gossip-port";

    // this list contains all the properties that are to have boolean values (true | false)
    private static final Set<String> BOOLEAN_PROPERTIES;
    static {
        BOOLEAN_PROPERTIES = new HashSet<String>();
        BOOLEAN_PROPERTIES.add(PROP_AGENT_MULTICAST_DETECTOR_ENABLED);
        BOOLEAN_PROPERTIES.add(PROP_AUTOINSTALL_ENABLE);
        BOOLEAN_PROPERTIES.add(PROP_MM_AT_START);
        BOOLEAN_PROPERTIES.add(PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED);
    }

    // this list contains all the properties that are to have integer values
    private static final Set<String> INTEGER_PROPERTIES;
    static {
        INTEGER_PROPERTIES = new HashSet<String>();
        INTEGER_PROPERTIES.add(PROP_AGENT_MULTICAST_DETECTOR_PORT);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_AVAIL_REPORT);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_CONTENT_REPORT);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_GLOBAL);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_INV_REPORT);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_INV_SYNC);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_MEAS_REPORT);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ);
        INTEGER_PROPERTIES.add(PROP_CONCURRENCY_LIMIT_WEBCONNS);
        INTEGER_PROPERTIES.add(PROP_CONNECTOR_BIND_PORT);
        INTEGER_PROPERTIES.add(PROP_DATABASE_PORT);
        INTEGER_PROPERTIES.add(PROP_EMAIL_SMTP_PORT);
        INTEGER_PROPERTIES.add(PROP_OPERATION_TIMEOUT);
        INTEGER_PROPERTIES.add(PROP_STORAGE_CQL_PORT);
        INTEGER_PROPERTIES.add(PROP_STORAGE_GOSSIP_PORT);
        INTEGER_PROPERTIES.add(PROP_WEB_HTTP_PORT);
        INTEGER_PROPERTIES.add(PROP_WEB_HTTPS_PORT);
    }

    // this list contains all the properties that are to have non-empty string values
    // note - in special cases some of this may be optional.
    private static final Set<String> STRING_PROPERTIES;
    static {
        STRING_PROPERTIES = new HashSet<String>();
        STRING_PROPERTIES.add(PROP_AUTOINSTALL_DATABASE);
        STRING_PROPERTIES.add(PROP_AUTOINSTALL_ADMIN_PASSWORD);
        STRING_PROPERTIES.add(PROP_DATABASE_TYPE);
        STRING_PROPERTIES.add(PROP_DATABASE_CONNECTION_URL);
        STRING_PROPERTIES.add(PROP_DATABASE_PASSWORD);
        STRING_PROPERTIES.add(PROP_DATABASE_USERNAME);
        STRING_PROPERTIES.add(PROP_DATABASE_SERVER_NAME);
        STRING_PROPERTIES.add(PROP_DATABASE_DB_NAME);
        STRING_PROPERTIES.add(PROP_DATABASE_HIBERNATE_DIALECT);
        STRING_PROPERTIES.add(PROP_EMAIL_FROM_ADDRESS);
        STRING_PROPERTIES.add(PROP_EMAIL_SMTP_HOST);
        STRING_PROPERTIES.add(PROP_JBOSS_BIND_ADDRESS);
        STRING_PROPERTIES.add(PROP_QUARTZ_DRIVER_DELEGATE_CLASS);
        STRING_PROPERTIES.add(PROP_QUARTZ_LOCK_HANDLER_CLASS);
        STRING_PROPERTIES.add(PROP_QUARTZ_SELECT_WITH_LOCK_SQL);
    }

    // this list contains all the STRING properties that are to have obfuscated/encoded values
    private static final Set<String> OBFUSCATED_PROPERTIES;
    static {
        OBFUSCATED_PROPERTIES = new HashSet<String>();
        OBFUSCATED_PROPERTIES.add(PROP_DATABASE_PASSWORD);
        OBFUSCATED_PROPERTIES.add(PROP_MGMT_USER_PASSWORD);
        OBFUSCATED_PROPERTIES.add(PROP_STORAGE_PASSWORD);
    }

    // this list contains all the non-STRING properties that can be unset when verified
    private static final Set<String> OPTIONAL_PROPERTIES;
    static {
        OPTIONAL_PROPERTIES = new HashSet<String>();
        OPTIONAL_PROPERTIES.add(PROP_CONNECTOR_BIND_PORT);
    }

    // this list contains all the properties that can be unset for Oracle
    private static final Set<String> OPTIONAL_PROPERTIES_ORACLE;
    static {
        OPTIONAL_PROPERTIES_ORACLE = new HashSet<String>();
        OPTIONAL_PROPERTIES_ORACLE.add(PROP_DATABASE_DB_NAME);
        OPTIONAL_PROPERTIES_ORACLE.add(PROP_DATABASE_PORT);
        OPTIONAL_PROPERTIES_ORACLE.add(PROP_DATABASE_SERVER_NAME);
    }

    public static final Set<String> CLIENT_AUTH_MODES;
    static {
        CLIENT_AUTH_MODES = new HashSet<String>();
        CLIENT_AUTH_MODES.add("none");
        CLIENT_AUTH_MODES.add("want");
        CLIENT_AUTH_MODES.add("need");
    }

    public static final Set<String> TOMCAT_CLIENT_AUTH_MODES;
    static {
        TOMCAT_CLIENT_AUTH_MODES = new HashSet<String>();
        TOMCAT_CLIENT_AUTH_MODES.add("false");
        TOMCAT_CLIENT_AUTH_MODES.add("want");
        TOMCAT_CLIENT_AUTH_MODES.add("true");
    }

    // all of the settings in this list have to be hardcoded to a specific IBM security algorithm
    // if we are installing a server that is running in an IBM JVM
    public static final Set<String> IBM_ALGOROTHM_SETTINGS;
    static {
        IBM_ALGOROTHM_SETTINGS = new HashSet<String>();
        IBM_ALGOROTHM_SETTINGS.add(PROP_TOMCAT_SECURITY_ALGORITHM);
        IBM_ALGOROTHM_SETTINGS.add(PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM);
        IBM_ALGOROTHM_SETTINGS.add(PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM);
        IBM_ALGOROTHM_SETTINGS.add(PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM);
        IBM_ALGOROTHM_SETTINGS.add(PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM);
    }

    public static void validate(File serverPropertiesFile) throws Exception {
        validate(serverPropertiesFile, null);
    }

    /**
     * @param serverPropertiesFile
     * @param additionalProperties additional properties that should be set (present and not empty). can be null.
     * @throws Exception
     */
    public static void validate(File serverPropertiesFile, Set<String> additionalProperties) throws Exception {
        if (!serverPropertiesFile.isFile()) {
            throw new Exception("Properties file not found: [" + serverPropertiesFile.getAbsolutePath() + "]");
        }

        PropertiesFileUpdate pfu = new PropertiesFileUpdate(serverPropertiesFile);
        Properties props = pfu.loadExistingProperties();
        final HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Object property : props.keySet()) {
            map.put(property.toString(), props.getProperty(property.toString()));
        }

        validate(map, additionalProperties);
    }

    public static void validate(Map<String, String> serverProperties) throws Exception {
        validate(serverProperties, null);
    }

    /**
     * @param serverProperties
     * @param additionalProperties additional properties that should be set (present and not empty). can be null.
     * @throws Exception
     */
    public static void validate(Map<String, String> serverProperties, Set<String> additionalProperties)
        throws Exception {
        final StringBuilder dataErrors = new StringBuilder();

        for (String name : ServerProperties.BOOLEAN_PROPERTIES) {
            String val = serverProperties.get(name);
            if (isOptional(serverProperties, name, val)) {
                continue;
            }
            if (!("true".equals(val) || "false".equals(val))) {
                dataErrors.append("[" + name + "] must exist and be set 'true' or 'false' : [" + val + "]\n");
            }
        }

        for (String name : ServerProperties.INTEGER_PROPERTIES) {
            String val = serverProperties.get(name);
            if (isOptional(serverProperties, name, val)) {
                continue;
            }
            try {
                Integer.parseInt(val);
            } catch (NumberFormatException e) {
                dataErrors.append("[" + name + "] must exist and be set to a number : [" + val + "]\n");
            }
        }

        Set<String> requiredStringProperties = new HashSet<String>();
        requiredStringProperties.addAll(STRING_PROPERTIES);
        if (null != additionalProperties) {
            requiredStringProperties.addAll(additionalProperties);
        }
        for (String name : requiredStringProperties) {
            String val = serverProperties.get(name);
            // in certain configurations a normally required property can be optional
            if (isOptional(serverProperties, name, val)) {
                continue;
            }
            if (StringUtil.isBlank(val)) {
                dataErrors.append("[" + name + "] must exist and be set to a valid string value\n");

            } else if (ServerProperties.OBFUSCATED_PROPERTIES.contains(name)) {
                try {
                    PicketBoxObfuscator.decode(val);
                } catch (Throwable e) {
                    dataErrors
                        .append("["
                            + name
                            + "] must be encoded for security reasons. The value is not valid, perhaps it is set to a plain text value?  : ["
                            + val + "]\n");
                }
            }
        }

        if (dataErrors.length() > 0) {
            throw new Exception("Validation errors:\n" + dataErrors.toString());
        }
    }

    static private boolean isOracle(Map<String, String> serverProperties) {
        String dialect = serverProperties.get(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT);
        return null != dialect && dialect.toLowerCase().contains("oracle");
    }

    static private boolean isOptional(Map<String, String> serverProperties, String name, String val) {
        if (StringUtil.isBlank(val) && OPTIONAL_PROPERTIES.contains(name)) {
            return true;
        }

        // check for oracle-specific optional props including back compat handling of "unused" setting
        if ((StringUtil.isBlank(val) || "unused".equals(val)) && isOracle(serverProperties)
            && ServerProperties.OPTIONAL_PROPERTIES_ORACLE.contains(name)) {
            return true;
        }

        return false;
    }
}
