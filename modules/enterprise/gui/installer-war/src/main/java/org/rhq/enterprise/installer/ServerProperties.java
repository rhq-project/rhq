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
package org.rhq.enterprise.installer;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

public class ServerProperties {
    public static final String PROP_DATABASE_CONNECTION_URL = "rhq.server.database.connection-url";
    public static final String PROP_DATABASE_DRIVER_CLASS = "rhq.server.database.driver-class";
    public static final String PROP_DATABASE_USERNAME = "rhq.server.database.user-name";
    public static final String PROP_DATABASE_PASSWORD = "rhq.server.database.password";
    public static final String PROP_DATABASE_TYPE = "rhq.server.database.type-mapping";

    public static final String PROP_SERVER_BIND_ADDRESS = "jboss.bind.address";
    public static final String PROP_HTTP_PORT = "rhq.server.startup.web.http.port";
    public static final String PROP_HTTPS_PORT = "rhq.server.startup.web.https.port";
    public static final String PROP_WEB_SERVICE_PORT = "rhq.server.startup.webservice.port";
    public static final String PROP_NAMING_SERVICE_PORT = "rhq.server.startup.namingservice.port";
    public static final String PROP_NAMING_SERVICE_RMI_PORT = "rhq.server.startup.namingservice.rmiport";
    public static final String PROP_JRMP_INVOKER_RMI_PORT = "rhq.server.startup.jrmpinvoker.rmiport";
    public static final String PROP_POOLED_INVOKER_RMI_PORT = "rhq.server.startup.pooledinvoker.rmiport";
    public static final String PROP_AJP_PORT = "rhq.server.startup.ajp.port";
    public static final String PROP_UNIFIED_INVOKER_PORT = "rhq.server.startup.unifiedinvoker.port";

    public static final String PROP_TOMCAT_KEYSTORE_FILENAME = "rhq.server.startup.keystore.filename";
    public static final String PROP_TOMCAT_KEYSTORE_PASSWORD = "rhq.server.startup.keystore.password";
    public static final String PROP_TOMCAT_SSL_PROTOCOL = "rhq.server.startup.keystore.sslprotocol";

    public static final String PROP_CONNECTOR_TRANSPORT = "rhq.communications.connector.transport";
    public static final String PROP_CONNECTOR_BIND_ADDRESS = "rhq.communications.connector.bind-address";
    public static final String PROP_CONNECTOR_BIND_PORT = "rhq.communications.connector.bind-port";
    public static final String PROP_CONNECTOR_TRANSPORT_PARAMS = "rhq.communications.connector.transport-params";

    public static final String PROP_AGENT_MULTICAST_DETECTOR_ENABLED = "rhq.communications.multicast-detector.enabled";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS = "rhq.communications.multicast-detector.bind-address";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS = "rhq.communications.multicast-detector.multicast-address";
    public static final String PROP_AGENT_MULTICAST_DETECTOR_PORT = "rhq.communications.multicast-detector.port";

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

    public static final String PROP_EMBEDDED_AGENT_ENABLED = "rhq.server.embedded-agent.enabled";
    public static final String PROP_EMBEDDED_AGENT_NAME = "rhq.server.embedded-agent.name";
    public static final String PROP_EMBEDDED_AGENT_DISABLE_NATIVE_SYSTEM = "rhq.server.embedded-agent.disable-native-system";
    public static final String PROP_EMBEDDED_AGENT_RESET_CONFIGURATION = "rhq.server.embedded-agent.reset-configuration";

    public static final String PROP_EMAIL_SMTP_HOST = "rhq.server.email.smtp-host";
    public static final String PROP_EMAIL_SMTP_PORT = "rhq.server.email.smtp-port";
    public static final String PROP_EMAIL_FROM_ADDRESS = "rhq.server.email.from-address";

    public static final String PROP_OPERATION_TIMEOUT = "rhq.server.operation-timeout";

    public static final String PROP_CLUSTER_PARTITION_NAME = "jboss.partition.name";
    public static final String PROP_CLUSTER_BIND_ADDRESS = "jgroups.bind_addr";
    public static final String PROP_CLUSTER_UDP_GROUP = "jgroups.udp.mcast_addr";
    public static final String PROP_CLUSTER_HAPARTITION_PORT = "jboss.hapartition.mcast_port";
    public static final String PROP_CLUSTER_EJB3CACHE_PORT = "jboss.ejb3entitypartition.mcast_port";
    public static final String PROP_CLUSTER_ALERTCACHE_PORT = "jboss.alertcachepartition.mcast_port";
    public static final String PROP_CLUSTER_UDP_LOOPBACK = "rhq.server.startup.partition.udpLoopback";
    public static final String PROP_CLUSTER_HAJNDI_PORT = "rhq.server.startup.hajndi.port";
    public static final String PROP_CLUSTER_HAJNDI_RMIPORT = "rhq.server.startup.hajndi.rmiport";
    public static final String PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT = "rhq.server.startup.hajndi.autodiscoverygroupport";
    public static final String PROP_CLUSTER_HAJRMPINVOKER_RMIPORT = "rhq.server.startup.hajrmpinvoker.rmiport";
    public static final String PROP_CLUSTER_HAPOOLEDINVOKER_PORT = "rhq.server.startup.hapooledinvoker.port";
    public static final String PROP_CLUSTER_JGROUPS_UDP_IP_TTL = "jgroups.udp.ip_ttl";

    public static final String PROP_CONCURRENCY_LIMIT_WEBCONNS = "rhq.server.startup.web.max-connections";
    public static final String PROP_CONCURRENCY_LIMIT_GLOBAL = "rhq.communications.global-concurrency-limit";
    public static final String PROP_CONCURRENCY_LIMIT_INV_REPORT = "rhq.server.concurrency-limit.inventory-report";
    public static final String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT = "rhq.server.concurrency-limit.availability-report";
    public static final String PROP_CONCURRENCY_LIMIT_INV_SYNC = "rhq.server.concurrency-limit.inventory-sync";
    public static final String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT = "rhq.server.concurrency-limit.content-report";
    public static final String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD = "rhq.server.concurrency-limit.content-download";
    public static final String PROP_CONCURRENCY_LIMIT_MEAS_REPORT = "rhq.server.concurrency-limit.measurement-report";
    public static final String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ = "rhq.server.concurrency-limit.measurement-schedule-request";

    private PropertyItem[] allPropertyItems = {
        new PropertyItem(PROP_DATABASE_TYPE, String.class, InstallerI18NResourceKeys.PROP_DATABASE_TYPE,
            InstallerI18NResourceKeys.PROP_DATABASE_TYPE_HELP, false, false, false),
        new PropertyItem(PROP_DATABASE_CONNECTION_URL, String.class,
            InstallerI18NResourceKeys.PROP_DATABASE_CONNECTION_URL,
            InstallerI18NResourceKeys.PROP_DATABASE_CONNECTION_URL_HELP, false, false, false),
        new PropertyItem(PROP_DATABASE_DRIVER_CLASS, String.class,
            InstallerI18NResourceKeys.PROP_DATABASE_DRIVER_CLASS,
            InstallerI18NResourceKeys.PROP_DATABASE_DRIVER_CLASS_HELP, false, false, false),
        new PropertyItem(PROP_DATABASE_USERNAME, String.class, InstallerI18NResourceKeys.PROP_DATABASE_USERNAME,
            InstallerI18NResourceKeys.PROP_DATABASE_USERNAME_HELP, false, false, false),
        new PropertyItem(PROP_DATABASE_PASSWORD, String.class, InstallerI18NResourceKeys.PROP_DATABASE_PASSWORD,
            InstallerI18NResourceKeys.PROP_DATABASE_PASSWORD_HELP, false, true, false),
        new PropertyItem(PROP_SERVER_BIND_ADDRESS, String.class, InstallerI18NResourceKeys.PROP_SERVER_BIND_ADDRESS,
            InstallerI18NResourceKeys.PROP_SERVER_BIND_ADDRESS_HELP, true, false, false),
        new PropertyItem(PROP_HTTP_PORT, Integer.class, InstallerI18NResourceKeys.PROP_HTTP_PORT,
            InstallerI18NResourceKeys.PROP_HTTP_PORT_HELP, true, false, false),
        new PropertyItem(PROP_HTTPS_PORT, Integer.class, InstallerI18NResourceKeys.PROP_HTTPS_PORT,
            InstallerI18NResourceKeys.PROP_HTTPS_PORT_HELP, true, false, false),
        new PropertyItem(PROP_WEB_SERVICE_PORT, Integer.class, InstallerI18NResourceKeys.PROP_WEB_SERVICE_PORT,
            InstallerI18NResourceKeys.PROP_WEB_SERVICE_PORT_HELP, true, false, true),
        new PropertyItem(PROP_NAMING_SERVICE_PORT, Integer.class, InstallerI18NResourceKeys.PROP_NAMING_SERVICE_PORT,
            InstallerI18NResourceKeys.PROP_NAMING_SERVICE_PORT_HELP, true, false, true),
        new PropertyItem(PROP_NAMING_SERVICE_RMI_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_NAMING_SERVICE_RMI_PORT,
            InstallerI18NResourceKeys.PROP_NAMING_SERVICE_RMI_PORT_HELP, true, false, true),
        new PropertyItem(PROP_JRMP_INVOKER_RMI_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_JRMP_INVOKER_RMI_PORT,
            InstallerI18NResourceKeys.PROP_JRMP_INVOKER_RMI_PORT_HELP, true, false, true),
        new PropertyItem(PROP_POOLED_INVOKER_RMI_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_POOLED_INVOKER_RMI_PORT,
            InstallerI18NResourceKeys.PROP_POOLED_INVOKER_RMI_PORT_HELP, true, false, true),
        new PropertyItem(PROP_AJP_PORT, Integer.class, InstallerI18NResourceKeys.PROP_AJP_PORT,
            InstallerI18NResourceKeys.PROP_AJP_PORT_HELP, true, false, true),
        new PropertyItem(PROP_UNIFIED_INVOKER_PORT, Integer.class, InstallerI18NResourceKeys.PROP_UNIFIED_INVOKER_PORT,
            InstallerI18NResourceKeys.PROP_UNIFIED_INVOKER_PORT_HELP, true, false, true),
        new PropertyItem(PROP_TOMCAT_KEYSTORE_FILENAME, String.class,
            InstallerI18NResourceKeys.PROP_TOMCAT_KEYSTORE_FILENAME,
            InstallerI18NResourceKeys.PROP_TOMCAT_KEYSTORE_FILENAME_HELP, true, false, true),
        new PropertyItem(PROP_TOMCAT_KEYSTORE_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_TOMCAT_KEYSTORE_PASSWORD,
            InstallerI18NResourceKeys.PROP_TOMCAT_KEYSTORE_PASSWORD_HELP, true, false, true),
        new PropertyItem(PROP_TOMCAT_SSL_PROTOCOL, String.class, InstallerI18NResourceKeys.PROP_TOMCAT_SSL_PROTOCOL,
            InstallerI18NResourceKeys.PROP_TOMCAT_SSL_PROTOCOL_HELP, true, false, true),
        new PropertyItem(PROP_CONNECTOR_TRANSPORT, String.class, InstallerI18NResourceKeys.PROP_CONNECTOR_TRANSPORT,
            InstallerI18NResourceKeys.PROP_CONNECTOR_TRANSPORT_HELP, false, false, true),
        new PropertyItem(PROP_CONNECTOR_BIND_ADDRESS, String.class,
            InstallerI18NResourceKeys.PROP_CONNECTOR_BIND_ADDRESS,
            InstallerI18NResourceKeys.PROP_CONNECTOR_BIND_ADDRESS_HELP, false, false, true),
        new PropertyItem(PROP_CONNECTOR_BIND_PORT, Integer.class, InstallerI18NResourceKeys.PROP_CONNECTOR_BIND_PORT,
            InstallerI18NResourceKeys.PROP_CONNECTOR_BIND_PORT_HELP, false, false, true),
        new PropertyItem(PROP_CONNECTOR_TRANSPORT_PARAMS, String.class,
            InstallerI18NResourceKeys.PROP_CONNECTOR_TRANSPORT_PARAMS,
            InstallerI18NResourceKeys.PROP_CONNECTOR_TRANSPORT_PARAMS_HELP, false, false, true),
        new PropertyItem(PROP_AGENT_MULTICAST_DETECTOR_ENABLED, Boolean.class,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_ENABLED,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_ENABLED_HELP, false, false, true),
        new PropertyItem(PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS, String.class,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS_HELP, false, false, true),
        new PropertyItem(PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS, String.class,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS_HELP, false, false, true),
        new PropertyItem(PROP_AGENT_MULTICAST_DETECTOR_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_PORT,
            InstallerI18NResourceKeys.PROP_AGENT_MULTICAST_DETECTOR_PORT_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_FILE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_FILE,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_FILE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_TYPE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_TYPE,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_TYPE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_SERVER_KEYSTORE_ALIAS, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_ALIAS,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_KEYSTORE_ALIAS_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_TRUSTSTORE_FILE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_FILE,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_FILE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_TRUSTSTORE_TYPE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_TYPE,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_TYPE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_SERVER_CLIENT_AUTH_MODE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_CLIENT_AUTH_MODE,
            InstallerI18NResourceKeys.PROP_SECURITY_SERVER_CLIENT_AUTH_MODE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_FILE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_FILE,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_FILE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_TYPE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_TYPE,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_TYPE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_CLIENT_KEYSTORE_ALIAS, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_ALIAS,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_KEYSTORE_ALIAS_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_TRUSTSTORE_FILE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_FILE,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_FILE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE_HELP, false, false, true),
        new PropertyItem(PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD, String.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD_HELP, false, true, true),
        new PropertyItem(PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED, Boolean.class,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED,
            InstallerI18NResourceKeys.PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED_HELP, false, false, true),
        new PropertyItem(PROP_EMBEDDED_AGENT_ENABLED, Boolean.class,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_ENABLED,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_ENABLED_HELP, false, false, false),
        new PropertyItem(PROP_EMBEDDED_AGENT_NAME, String.class,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_NAME,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_NAME_HELP, false, false, true),
        new PropertyItem(PROP_EMBEDDED_AGENT_DISABLE_NATIVE_SYSTEM, Boolean.class,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_DISABLE_NATIVE_SYSTEM,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_DISABLE_NATIVE_SYSTEM_HELP, true, false, true),
        new PropertyItem(PROP_EMBEDDED_AGENT_RESET_CONFIGURATION, Boolean.class,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_RESET_CONFIGURATION,
            InstallerI18NResourceKeys.PROP_EMBEDDED_JON_AGENT_RESET_CONFIGURATION_HELP, false, false, true),
        new PropertyItem(PROP_EMAIL_SMTP_HOST, String.class, InstallerI18NResourceKeys.PROP_EMAIL_SMTP_HOST,
            InstallerI18NResourceKeys.PROP_EMAIL_SMTP_HOST_HELP, false, false, true),
        new PropertyItem(PROP_EMAIL_SMTP_PORT, Integer.class, InstallerI18NResourceKeys.PROP_EMAIL_SMTP_PORT,
            InstallerI18NResourceKeys.PROP_EMAIL_SMTP_PORT_HELP, false, false, true),
        new PropertyItem(PROP_EMAIL_FROM_ADDRESS, String.class, InstallerI18NResourceKeys.PROP_EMAIL_FROM_ADDRESS,
            InstallerI18NResourceKeys.PROP_EMAIL_FROM_ADDRESS_HELP, false, false, false),
        new PropertyItem(PROP_OPERATION_TIMEOUT, Integer.class, InstallerI18NResourceKeys.PROP_OPERATION_TIMEOUT,
            InstallerI18NResourceKeys.PROP_OPERATION_TIMEOUT_HELP, false, false, true),
        new PropertyItem(PROP_CLUSTER_PARTITION_NAME, String.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_PARTITION_NAME,
            InstallerI18NResourceKeys.PROP_CLUSTER_PARTITION_NAME_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_BIND_ADDRESS, InetAddress.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_PARTITION_BIND_ADDRESS,
            InstallerI18NResourceKeys.PROP_CLUSTER_PARTITION_BIND_ADDRESS_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_UDP_GROUP, String.class, InstallerI18NResourceKeys.PROP_CLUSTER_UDP_GROUP,
            InstallerI18NResourceKeys.PROP_CLUSTER_UDP_GROUP_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAPARTITION_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAPARTITION_PORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAPARTITION_PORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_EJB3CACHE_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_EJB3CACHE_PORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_EJB3CACHE_PORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_ALERTCACHE_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_ALERTCACHE_PORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_ALERTCACHE_PORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_UDP_LOOPBACK, Boolean.class, InstallerI18NResourceKeys.PROP_CLUSTER_UDP_LOOPBACK,
            InstallerI18NResourceKeys.PROP_CLUSTER_UDP_LOOPBACK_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAJNDI_PORT, Integer.class, InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_PORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_PORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAJNDI_RMIPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_RMIPORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_RMIPORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAJRMPINVOKER_RMIPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJRMPINVOKER_RMIPORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAJRMPINVOKER_RMIPORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_HAPOOLEDINVOKER_PORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAPOOLEDINVOKER_PORT,
            InstallerI18NResourceKeys.PROP_CLUSTER_HAPOOLEDINVOKER_PORT_HELP, true, false, true),
        new PropertyItem(PROP_CLUSTER_JGROUPS_UDP_IP_TTL, Integer.class,
            InstallerI18NResourceKeys.PROP_CLUSTER_JGROUPS_UDP_IP_TTL,
            InstallerI18NResourceKeys.PROP_CLUSTER_JGROUPS_UDP_IP_TTL_HELP, true, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_WEBCONNS, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_WEBCONNS,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_WEBCONNS_HELP, true, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_GLOBAL, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_GLOBAL,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_GLOBAL_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_INV_REPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_INV_REPORT,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_INV_REPORT_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_AVAIL_REPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_AVAIL_REPORT,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_AVAIL_REPORT_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_INV_SYNC, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_INV_SYNC,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_INV_SYNC_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_CONTENT_REPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_CONTENT_REPORT,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_CONTENT_REPORT_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_MEAS_REPORT, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_MEAS_REPORT,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_MEAS_REPORT_HELP, false, false, true),
        new PropertyItem(PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ, Integer.class,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ,
            InstallerI18NResourceKeys.PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ_HELP, false, false, true) };

    /**
     * Returns the list of all known property items the server supports, in an order that is appropriate for display.
     *
     * @return ordered list of property item definitions
     */
    public List<PropertyItem> getPropertyItems() {
        return new ArrayList<PropertyItem>(Arrays.asList(allPropertyItems));
    }

    /**
     * Static convienence method that returns a valid server bind address as defined in the given properties. If not
     * found in the props or it is "0.0.0.0", then <code>InetAddress.getLocalHost().getHostAddress()</code> is used.
     * Therefore, keep in mind that this might return a value that is different than the value actually specified in the
     * given properties.
     *
     * @param  props
     *
     * @return the server bind address string
     */
    public static String getValidServerBindAddress(Properties props) {
        String bindAddr;

        try {
            bindAddr = props.getProperty(ServerProperties.PROP_SERVER_BIND_ADDRESS, "0.0.0.0");
            if (bindAddr.trim().equals("0.0.0.0")) {
                bindAddr = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            bindAddr = "127.0.0.1";
        }

        return bindAddr;
    }

    /**
     * Static convienence method that returns the HTTP port as defined in the given properties. If not found in the
     * props, it defaults to 7080. Therefore, keep in mind that this might return a value that is not actually specified
     * in the given properties.
     *
     * @param  props
     *
     * @return the http port
     */
    public static String getHttpPort(Properties props) {
        return props.getProperty(ServerProperties.PROP_HTTP_PORT, "7080");
    }
}