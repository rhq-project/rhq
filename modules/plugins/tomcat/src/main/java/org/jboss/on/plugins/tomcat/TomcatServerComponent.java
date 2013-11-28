/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.jboss.on.plugins.tomcat;

import java.io.File;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnectException;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Management for an Apache or JBoss EWS Tomcat server
 *
 * @author Jay Shaughnessy
 */
public class TomcatServerComponent<T extends ResourceComponent<?>> implements JMXComponent<T>, MeasurementFacet,
    OperationFacet {

    public enum SupportedOperations {
        /**
         * Restarts a Tomcat instance by calling a configurable restart script.
         */
        RESTART,

        /**
         * Shuts down a Tomcat instance via a shutdown script, depending on plug-in configuration
         */
        SHUTDOWN,

        /**
         * Starts a Tomcat instance by calling a configurable start script.
         */
        START,

        /**
         * Physically writes out the latest state to server.xml.
         */
        STORECONFIG
    }

    public enum ControlMethod {

        /** Control operations should be performed via System V init script. */
        RPM,

        /** Control operations should be performed via the scripts set in the plugin configuration. */
        SCRIPT
    }

    /**
     * Plugin configuration properties.
     */
    public static final String PLUGIN_CONFIG_CONTROL_METHOD = "controlMethod";
    // has legacy property name
    public static final String PLUGIN_CONFIG_CATALINA_HOME_PATH = "installationPath";
    public static final String PLUGIN_CONFIG_CATALINA_BASE_PATH = "catalinaBase";
    public static final String PLUGIN_CONFIG_SCRIPT_PREFIX = "scriptPrefix";
    public static final String PLUGIN_CONFIG_SHUTDOWN_SCRIPT = "shutdownScript";
    public static final String PLUGIN_CONFIG_START_SCRIPT = "startScript";
    public static final String START_WAIT_MAX_PROP = "startWaitMax";
    public static final String STOP_WAIT_MAX_PROP = "stopWaitMax";

    private Log log = LogFactory.getLog(this.getClass());

    private EmsConnection connection;

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log
     * file. Each time a connection error is encountered, this will be incremented. When the connection
     * is finally established, this will be reset to zero.
     */
    private int consecutiveConnectionErrors;

    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private TomcatServerOperationsDelegate operationsDelegate;

    private ResourceContext<T> resourceContext;

    // JMXComponent Implementation  --------------------------------------------

    public EmsConnection getEmsConnection() {
        EmsConnection emsConnection = null;

        try {
            emsConnection = loadConnection();
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.debug("Component attempting to access a connection that could not be loaded:" + e.getMessage());
            }
        }

        return emsConnection;
    }

    /**
     * This is the preferred way to use a connection from within this class; methods should not access the connection
     * property directly as it may not have been instantiated if the connection could not be made.
     *
     * <p>If the connection has already been established, return the object reference to it. If not, attempt to make
     * a live connection to the JMX server.</p>
     *
     * <p>If the connection could not be made in the {@link #start(org.rhq.core.pluginapi.inventory.ResourceContext)}
     * method, this method will effectively try to load the connection on each attempt to use it. As such, multiple
     * threads may attempt to access the connection through this means at a time. Therefore, the method has been
     * made synchronized on instances of the class.</p>
     *
     * <p>If any errors are encountered, this method will log the error, taking into account logic to prevent spamming
     * the log file. Calling methods should take care to not redundantly log the exception thrown by this method.</p>
     *
     * @return live connection to the JMX server; this will not be <code>null</code>
     *
     * @throws Exception if there are any issues at all connecting to the server
     */
    private synchronized EmsConnection loadConnection() throws Exception {

        if (this.connection != null && !this.connection.getConnectionProvider().isConnected()) {
            try {
                this.connection.close();
            } catch (Exception ignore) {
            } finally {
                this.connection = null;
            }
        }

        if (this.connection == null) {
            try {
                Configuration pluginConfig = this.resourceContext.getPluginConfiguration();

                ConnectionSettings connectionSettings = new ConnectionSettings();

                String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                    .getStringValue();
                PropertySimple serverUrl = pluginConfig
                    .getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY);

                connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
                    connectionTypeDescriptorClass).newInstance());
                // if not provided use the default serverUrl
                if (null != serverUrl) {
                    connectionSettings.setServerUrl(serverUrl.getStringValue());
                }

                connectionSettings.setPrincipal(pluginConfig.getSimpleValue(PRINCIPAL_CONFIG_PROP, null));
                connectionSettings.setCredentials(pluginConfig.getSimpleValue(CREDENTIALS_CONFIG_PROP, null));

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                ConnectionFactory connectionFactory = new ConnectionFactory();

                // EMS can connect to a Tomcat Server without using version-compatible jars from a local TC install. So,
                // If we are connecting to a remote TC Server and the install path is not valid on the local host, don't
                // configure to use the local jars. But, to be safe, if for some overlooked or future reason we require
                // the jars then use them if they are available. Note, for a remote TC Server that would mean you'd have
                // to have a version compatible local install and set the install path to the local path, even though
                // the server url was remote.
                String catalinaHome = pluginConfig.getSimpleValue(PLUGIN_CONFIG_CATALINA_HOME_PATH, null);
                File libDir = getLibDir(catalinaHome);
                if (libDir != null) {
                    connectionSettings.setLibraryURI(libDir.getAbsolutePath());
                    connectionFactory.discoverServerClasses(connectionSettings);

                    // Tell EMS to make copies of jar files so that the ems classloader doesn't lock
                    // application files (making us unable to update them)  Bug: JBNADM-670
                    // TODO (ips): Turn this off in the embedded case.
                    connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP,
                        String.valueOf(Boolean.TRUE));

                    // But tell it to put them in a place that we clean up when shutting down the agent (make sure tmp dir exists)
                    File tempDir = resourceContext.getTemporaryDirectory();
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                    connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                        tempDir.getAbsolutePath());

                    log.info("Loading connection [" + connectionSettings.getServerUrl() + "] with install path ["
                        + connectionSettings.getLibraryURI() + "] and temp directory [" + tempDir.getAbsolutePath()
                        + "]");
                } else {
                    log.info("Loading connection [" + connectionSettings.getServerUrl()
                        + "] ignoring remote install path [" + catalinaHome + "]");
                }

                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                this.connection = connectionProvider.connect();

                this.connection.loadSynchronous(false); // this loads all the MBeans

                this.consecutiveConnectionErrors = 0;

                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the Tomcat Server for resource ["
                        + this.resourceContext.getResourceKey() + "]");

            } catch (Exception e) {

                // The connection will be established even in the case that the principal cannot be authenticated,
                // but the connection will not work. That failure seems to come from the call to loadSynchronous after
                // the connection is established. If we get to this point that an exception was thrown, close any
                // connection that was made and null it out so we can try to establish it again.
                if (this.connection != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Connection created but an exception was thrown. Closing the connection.", e);
                    }
                    try {
                        this.connection.close();
                    } catch (Exception e2) {
                        log.error("Error closing Tomcat EMS connection: " + e2);
                    }
                    this.connection = null;
                }

                // Since the connection is attempted each time it's used, failure to connect could result in log
                // file spamming. Log a warning only one time outside of debug mode, and throttle even in debug
                // mode (once for every 10 connect errors).
                if (0 == consecutiveConnectionErrors) {
                    log.warn("Could not connect to the Tomcat instance for resource ["
                        + resourceContext.getResourceKey() + "] (enable debug logging for more info): "
                        + e.getMessage());
                }

                if (log.isDebugEnabled() && (consecutiveConnectionErrors % 10 == 0)) {
                    log.debug(
                        "Could not establish connection to the Tomcat instance [" + (consecutiveConnectionErrors + 1)
                            + "] times for resource [" + resourceContext.getResourceKey() + "]", e);
                }

                ++consecutiveConnectionErrors;

                throw e;
            }
        }

        return connection;
    }

    private File getLibDir(String catalinaHome) {
        if (catalinaHome != null) {
            // Tomcat 6 and Tomcat 7 have Catalina JARS in CATALINA_HOME/lib
            File libDir = new File(catalinaHome, "lib");
            if (libDir.isDirectory()) {
                return libDir;
            }
            // Tomcat 5.5 has Catalina JARS in CATALINA_HOME/server/lib
            libDir = new File(catalinaHome, "server" + File.separator + "lib");
            if (libDir.isDirectory()) {
                return libDir;
            }
        }
        return null;
    }

    public Configuration getPluginConfiguration() {
        return resourceContext.getPluginConfiguration();
    }

    // Here we do any validation that couldn't be achieved via the metadata-based constraints.
    private void validatePluginConfiguration() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String principal = pluginConfig.getSimpleValue(TomcatServerComponent.PRINCIPAL_CONFIG_PROP, null);
        String credentials = pluginConfig.getSimpleValue(TomcatServerComponent.CREDENTIALS_CONFIG_PROP, null);
        if ((principal != null) && (credentials == null)) {
            throw new InvalidPluginConfigurationException("If the '" + TomcatServerComponent.PRINCIPAL_CONFIG_PROP
                + "' connection property is set, the '" + TomcatServerComponent.CREDENTIALS_CONFIG_PROP
                + "' connection property must also be set.");
        }

        if ((credentials != null) && (principal == null)) {
            throw new InvalidPluginConfigurationException("If the '" + TomcatServerComponent.CREDENTIALS_CONFIG_PROP
                + "' connection property is set, the '" + TomcatServerComponent.PRINCIPAL_CONFIG_PROP
                + "' connection property must also be set.");
        }
    }

    @Override
    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;
        this.operationsDelegate = new TomcatServerOperationsDelegate(this, resourceContext.getSystemInformation());

        validatePluginConfiguration();

        // Attempt to load the connection now. If we cannot, do not consider the start operation as failed. The only
        // exception to this rule is if the connection cannot be made due to a JMX security exception. In this case,
        // we treat it as an invalid plugin configuration and throw the appropriate exception (see the javadoc for
        // ResourceComponent)
        try {
            loadConnection();
        } catch (Exception e) {

            // Explicit checking for security exception (i.e. invalid credentials for connecting to JMX)
            if (e instanceof EmsConnectException) {
                Throwable cause = e.getCause();

                if (cause instanceof SecurityException) {
                    throw new InvalidPluginConfigurationException(
                        "Invalid JMX credentials specified for connecting to this server.", e);
                }
            }
        }

        // TODO: If we add event checking by default
        //startLogFileEventPollers();
    }

    public void stop() {
        // TODO: If we add event checking by default
        // stopLogFileEventPollers();
        closeConnection();
    }

    /**
     * If necessary attempt to close the EMS connection, then set this.connection null.  Synchronized ensure we play well
     * with loadConnection.
     */
    private synchronized void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Error closing Tomcat EMS connection: " + e);
            }
            this.connection = null;
        }
    }

    public AvailabilityType getAvailability() {
        AvailabilityType avail;
        try {
            EmsConnection connection = loadConnection();
            EmsBean bean = connection.getBean("Catalina:type=Server");

            // this is necessary to prove that that not only the connection exists but is servicing requests.
            bean.getAttribute("serverInfo").refresh();
            avail = AvailabilityType.UP;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("An exception occurred during availability check for Tomcat Server Resource with key ["
                    + this.getResourceContext().getResourceKey() + "] and plugin config ["
                    + this.getPluginConfiguration().getAllProperties() + "].", e);
            }
            // If the connection is not servicing requests, then close it. this seems necessary for the
            // Tomcat connection, as, when Tomcat does come up again, it seems a new EMS connection is required,
            // otherwise EMS is not able to pick up the new process.
            closeConnection();
            avail = AvailabilityType.DOWN;
        }
        return avail;
    }

    ResourceContext<T> getResourceContext() {
        return this.resourceContext;
    }

    public File getStartScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String script = pluginConfig.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, "");
        File scriptFile = resolvePathRelativeToHomeDir(script);
        return scriptFile;
    }

    public File getCatalinaHome() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        return new File(pluginConfig.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH, ""));
    }

    public File getCatalinaBase() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String base = pluginConfig.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_BASE_PATH, null);

        return (null != base) ? new File(base) : getCatalinaHome();
    }

    public File getShutdownScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String script = pluginConfig.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT, "");
        File scriptFile = resolvePathRelativeToHomeDir(script);
        return scriptFile;
    }

    private File resolvePathRelativeToHomeDir(@NotNull String path) {
        return resolvePathRelativeToHomeDir(this.resourceContext.getPluginConfiguration(), path);
    }

    static File resolvePathRelativeToHomeDir(Configuration pluginConfig, String path) {
        File configDir = new File(path);
        if (!FileUtil.isAbsolutePath(path)) {
            String jbossHomeDir = getRequiredPropertyValue(pluginConfig,
                TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH);
            configDir = new File(jbossHomeDir, path);
        }

        // BZ 903402 - get the real absolute path - under most conditions, it's the same thing, but if on windows
        //             the drive letter might not have been specified - this makes sure the drive letter is specified.
        return configDir.getAbsoluteFile();
    }

    private static String getRequiredPropertyValue(Configuration config, String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    /** Persist local changes to the server.xml */
    void storeConfig() throws Exception {
        invokeOperation(SupportedOperations.STORECONFIG.name(), new Configuration());
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        SupportedOperations operation = Enum.valueOf(SupportedOperations.class, name.toUpperCase());

        addScriptsEnvironment(parameters);

        return operationsDelegate.invoke(operation, parameters);
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest schedule : metrics) {
            String name = schedule.getName();

            int delimIndex = name.lastIndexOf(':');
            String beanName = name.substring(0, delimIndex);
            String attributeName = name.substring(delimIndex + 1);
            try {
                // Bean is cached by EMS, so no problem with getting the bean from the connection on each call
                EmsConnection emsConnection = loadConnection();
                EmsBean bean = emsConnection.getBean(beanName);
                EmsAttribute attribute = bean.getAttribute(attributeName);

                Object valueObject = attribute.refresh();
                if (valueObject instanceof Number) {
                    Number value = (Number) valueObject;
                    report.addData(new MeasurementDataNumeric(schedule, value.doubleValue()));
                } else {
                    report.addData(new MeasurementDataTrait(schedule, valueObject.toString()));
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    private void addScriptsEnvironment(Configuration operationParameters) {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertyList startScriptEnv = pluginConfiguration
            .getList(TomcatServerOperationsDelegate.START_SCRIPT_ENVIRONMENT_PROPERTY);

        PropertyList shutdownScriptEnv = pluginConfiguration
            .getList(TomcatServerOperationsDelegate.SHUTDOWN_SCRIPT_ENVIRONMENT_PROPERTY);

        if (startScriptEnv != null) {
            operationParameters.put(startScriptEnv);
        }

        if (shutdownScriptEnv != null) {
            operationParameters.put(shutdownScriptEnv);
        }
    }
}
