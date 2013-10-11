/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.server;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.JSR160ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * This resource serves as the parent to MBeans being exposed for management via plugins injecting child resources.
 * This resource provides the connection to the parent AS7's MBeanServer.
 *
 * Note that this resource must be configured to have its own hostname - this is because this could be different
 * than the public binding address. The hostname this resource needs is called "jboss.bind.address.management" in the AS7
 * configuration.
 *
 * Additionally, the username and password are that of a valid management user defined for the AS7 instance.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class JBossAS7JMXComponent<T extends ResourceComponent<?>> implements ResourceComponent<T>, JMXComponent<T> {

    public static final String PLUGIN_CONFIG_CLIENT_JAR_LOCATION = "clientJarLocation";
    public static final String PLUGIN_CONFIG_PORT = "port";
    public static final String PLUGIN_CONFIG_HOSTNAME = "hostname"; // jboss.bind.address.management
    public static final String PLUGIN_CONFIG_USERNAME = "username";
    public static final String PLUGIN_CONFIG_PASSWORD = "password";
    public static final String DEFAULT_PLUGIN_CONFIG_PORT = "9999";

    private Log log = LogFactory.getLog(JBossAS7JMXComponent.class);
    private ResourceContext<T> resourceContext;
    private EmsConnection connection;

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log
     * file. Each time a connection error is encountered, this will be incremented. When the connection
     * is finally established, this will be reset to zero.
     */
    private int consecutiveConnectionErrors;

    @Override
    public AvailabilityType getAvailability() {

        try {
            EmsConnection conn = getEmsConnection();
            if (conn == null) {
                return AvailabilityType.DOWN;
            }
            conn.queryBeans("java.lang:*"); // just make a request over the connection to make sure its valid
            return AvailabilityType.UP;
        } catch (Throwable t) {
            try {
                this.connection.close(); // try to clean up
            } catch (Throwable ignore) {
            }
            this.connection = null;
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;
        loadConnection();
    }

    @Override
    public void stop() {
        return;
    }

    @Override
    public EmsConnection getEmsConnection() {
        EmsConnection emsConnection = null;

        try {
            emsConnection = loadConnection();
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded: " + e.getCause());
        }

        return emsConnection;
    }

    private EmsConnection loadConnection() throws Exception {
        if (connection != null) {
            return connection;
        }

        try {
            Configuration pluginConfig = resourceContext.getPluginConfiguration();
            String hostname = pluginConfig.getSimpleValue(PLUGIN_CONFIG_HOSTNAME, "127.0.0.1");
            String port = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PORT, DEFAULT_PLUGIN_CONFIG_PORT);
            String username = pluginConfig.getSimpleValue(PLUGIN_CONFIG_USERNAME, "rhqadmin");
            String password = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PASSWORD, "rhqadmin");

            ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.initializeConnectionType(new JSR160ConnectionTypeDescriptor());
            connectionSettings.setServerUrl("service:jmx:remoting-jmx://" + hostname + ":" + port);
            connectionSettings.setPrincipal(username);
            connectionSettings.setCredentials(password);

            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.discoverServerClasses(connectionSettings);

            if (connectionSettings.getAdvancedProperties() == null) {
                connectionSettings.setAdvancedProperties(new Properties());
            }

            log.info("Loading AS7 connection [" + connectionSettings.getServerUrl() + "] with install path ["
                + connectionSettings.getLibraryURI() + "]...");

            ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
            this.connection = connectionProvider.connect();

            if (log.isDebugEnabled()) {
                log.debug("Successfully made connection to the AS7 instance for resource ["
                    + resourceContext.getResourceKey() + "]");
            }

            return connection;

        } catch (Exception e) {
            // The connection will be established even in the case that the principal cannot be authenticated,
            // but the connection will not work. That failure seems to come from the call to loadSynchronous after
            // the connection is established. If we get to this point that an exception was thrown, close any
            // connection that was made and null it out so we can try to establish it again.
            if (connection != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Connection created but an exception was thrown. Closing the connection.", e);
                }
                connection.close();
                connection = null;
            }

            // Since the connection is attempted each time it's used, failure to connect could result in log
            // file spamming. Log it once for every 10 consecutive times it's encountered.
            if (consecutiveConnectionErrors % 10 == 0) {
                log.warn("Could not establish connection to the JBoss AS instance ["
                    + (consecutiveConnectionErrors + 1) + "] times for resource [" + resourceContext.getResourceKey()
                    + "]", e);
            }
            consecutiveConnectionErrors++;

            if (log.isDebugEnabled()) {
                log.debug("Can't connect to JBoss AS resource  [" + resourceContext.getResourceKey() + "]", e);
            }

            throw e;
        }
    }
}