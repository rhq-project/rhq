/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.helper;

import java.io.File;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Shared helper class to connect to a remote server
 *
 * @author Heiko W. Rupp
 */
public class JmxConnectionHelper {

    public static final String CONNECTOR_DESCRIPTOR_TYPE = "connectorDescriptorType";
    public static final String CONNECTOR_ADDRESS = "connectorAddress";
    public static final String CONNECTOR_PRINCIPAL = "connectorPrincipal";
    public static final String CONNECTOR_CREDENTIALS = "connectorCredentials";
    public static final String JBOSS_HOME_DIR = "jbossHomeDir";

    private static final Log log = LogFactory.getLog(JmxConnectionHelper.class);

    private static EmsConnection connection;
    private static Configuration configuration;

    private static final String JNP_DISABLE_DISCOVERY_JNP_INIT_PROP = "jnp.disableDiscovery";

    /**
     * This is the timeout for the initial connection to the MBeanServer that is made by {@link #start(ResourceContext)}.
     */
    private static final int JNP_TIMEOUT = 30 * 1000; // 30 seconds
    /**
     * This is the timeout for MBean attribute gets/sets and operations invoked on the remote MBeanServer.
     * NOTE: This timeout comes into play if the JBossAS instance has gone down since the original JNP connection was made.
     */
    private static final int JNP_SO_TIMEOUT = 15 * 1000; // 15 seconds

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log file. Each time
     * a connection error is encountered, this will be incremented. When the connection is finally established, this
     * will be reset to zero.
     */
    private static int consecutiveConnectionErrors;

    private boolean copyConnectionLibraries;
    private File tmpDir;

    /**
     * Constructs a new connection helper.
     * 
     * @param copyConnectionLibraries whether to copy the libraries need for the connection so that
     * the ems classloader doesn't block the application access to them.
     * @param tmpDir the temporary directory to use when copying the libraries
     */
    public JmxConnectionHelper(boolean copyConnectionLibraries, File tmpDir) {
        this.copyConnectionLibraries = copyConnectionLibraries;
        this.tmpDir = tmpDir;
    }

    /**
     * Obtain an EmsConnection for the passed connection properties. The properties will be retained.
     * To create a connection with different properties, use this method again with a different set
     * of properties.
     * @param config Configuration properties for this connection
     * @return an EmsConnection or null in case of failure
     * @see #getEmsConnection()
     */
    public EmsConnection getEmsConnection(Configuration config) {
        EmsConnection emsConnection = null;
        configuration = config;

        try {
            emsConnection = loadConnection(config, copyConnectionLibraries, tmpDir);
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded");
        }

        return emsConnection;
    }

    /**
     * Obtain an EmsConnection. This will only work if the connection properties have passed
     * before via a call to {@link #getEmsConnection(Configuration)}
     * @return an EmsConnection or null in case of failure
     * @see #getEmsConnection(org.rhq.core.domain.configuration.Configuration)
     */
    public EmsConnection getEmsConnection() {
        EmsConnection emsConnection = null;
        if (configuration == null) {
            throw new RuntimeException("No configuration set");
        }

        try {
            emsConnection = loadConnection(configuration, copyConnectionLibraries, tmpDir);
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded");
        }

        return emsConnection;
    }

    /**
     * This is the preferred way to use a connection from within this class; methods should not access the connection
     * property directly as it may not have been instantiated if the connection could not be made.
     * <p/>
     * <p>If the connection has already been established, return the object reference to it. If not, attempt to make a
     * live connection to the JMX server.</p>
     * <p/>
     * <p>If the connection could not be made in the start(org.rhq.core.pluginapi.inventory.ResourceContext) method,
     * this method will effectively try to load the connection on each attempt to use it. As such, multiple threads may
     * attempt to access the connection through this means at a time. Therefore, the method has been made synchronized
     * on instances of the class.</p>
     * <p/>
     * <p>If any errors are encountered, this method will log the error, taking into account logic to prevent spamming
     * the log file. Calling methods should take care to not redundantly log the exception thrown by this method.</p>
     *
     * @param pluginConfig
     * @return live connection to the JMX server; this will not be <code>null</code>
     * @throws Exception if there are any issues at all connecting to the server
     */
    private static synchronized EmsConnection loadConnection(Configuration pluginConfig,
        boolean copyConnectionLibraries, File tmpDir) throws Exception {
        if (connection == null) {
            try {
                //Configuration pluginConfig = this.resourceContext.getPluginConfiguration();

                ConnectionSettings connectionSettings = new ConnectionSettings();

                String connectionTypeDescriptorClass = pluginConfig.getSimple(CONNECTOR_DESCRIPTOR_TYPE)
                    .getStringValue();
                PropertySimple serverUrl = pluginConfig.getSimple(CONNECTOR_ADDRESS);

                connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
                    connectionTypeDescriptorClass).newInstance());
                // if not provided use the default serverUrl
                if (null != serverUrl) {
                    connectionSettings.setServerUrl(serverUrl.getStringValue());
                }

                connectionSettings.setPrincipal(pluginConfig.getSimpleValue(CONNECTOR_PRINCIPAL, null));
                connectionSettings.setCredentials(pluginConfig.getSimpleValue(CONNECTOR_CREDENTIALS, null));
                connectionSettings.setLibraryURI(pluginConfig.getSimpleValue(JBOSS_HOME_DIR, null));

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                connectionSettings.getAdvancedProperties().setProperty(JNP_DISABLE_DISCOVERY_JNP_INIT_PROP, "true");

                // Make sure the timeout always happens, even if the JBoss server is hung.
                connectionSettings.getAdvancedProperties().setProperty("jnp.timeout", String.valueOf(JNP_TIMEOUT));
                connectionSettings.getAdvancedProperties().setProperty("jnp.sotimeout", String.valueOf(JNP_SO_TIMEOUT));

                if (copyConnectionLibraries) {
                    // Tell EMS to make copies of jar files so that the ems classloader doesn't lock
                    // application files (making us unable to update them)  Bug: JBNADM-670
                    connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP,
                        String.valueOf(Boolean.TRUE));

                    // But tell it to put them in a place that we clean up when shutting down the agent
                    connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                        tmpDir.getAbsolutePath());
                }

                connectionSettings.getAdvancedProperties().setProperty(InternalVMTypeDescriptor.DEFAULT_DOMAIN_SEARCH,
                    "jboss");

                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.discoverServerClasses(connectionSettings);

                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                connection = connectionProvider.connect();

                connection.loadSynchronous(false); // this loads all the MBeans

                consecutiveConnectionErrors = 0;

                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the remote server instance");
            } catch (Exception e) {

                // The connection will be established even in the case that the principal cannot be authenticated,
                // but the connection will not work. That failure seems to come from the call to loadSynchronous after
                // the connection is established. If we get to this point that an exception was thrown, close any
                // connection that was made and null it out so we can try to establish it again.
                if (connection != null) {
                    if (log.isDebugEnabled())
                        log.debug("Connection created but an exception was thrown. Closing the connection.", e);
                    connection.close();
                    connection = null;
                }

                // Since the connection is attempted each time it's used, failure to connect could result in log
                // file spamming. Log it once for every 10 consecutive times it's encountered.
                if (consecutiveConnectionErrors % 10 == 0) {
                    log.warn("Could not establish connection to the instance [" + (consecutiveConnectionErrors + 1)
                        + "] times.", e);
                }

                if (log.isDebugEnabled())
                    log.debug("Could not connect to the instance for resource ", e);

                consecutiveConnectionErrors++;

                throw e;
            }
        }

        return connection;
    }

    /**
     * If necessary attempt to close the EMS connection, then set this.connection null.  Synchronized ensure we play
     * well with loadConnection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Error closing EMS connection: " + e);
            }
            connection = null;
        }
    }

}
