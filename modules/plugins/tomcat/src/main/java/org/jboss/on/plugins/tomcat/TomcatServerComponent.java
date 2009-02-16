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
package org.jboss.on.plugins.tomcat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.plugins.tomcat.helper.FileContentDelegate;
import org.jboss.on.plugins.tomcat.helper.TomcatApplicationDeployer;
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ApplicationServerComponent;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * Management for an Apache or JBoss EWS Tomcat server
 *
 * @author Jay Shaughnessy
 */
public class TomcatServerComponent implements JMXComponent<PlatformComponent>, ApplicationServerComponent, MeasurementFacet, OperationFacet, CreateChildResourceFacet {

    public enum SupportedOperations {
        /**
         * Shuts down a Tomcat instance via a shutdown script, depending on plug-in configuration
         */
        SHUTDOWN,

        /**
         * Starts a Tomcat instance by calling a configurable start script.
         */
        START,

        /**
         * Restarts a Tomcat instance by calling a configurable restart script.
         */
        RESTART
    }

    /**
     * Plugin configuration properties.
     */
    static final String PROP_INSTALLATION_PATH = "installationPath";
    static final String PROP_SCRIPT_PREFIX = "scriptPrefix";
    static final String PROP_SHUTDOWN_MBEAN_NAME = "shutdownMBeanName";
    static final String PROP_SHUTDOWN_MBEAN_OPERATION = "shutdownMBeanOperation";
    static final String PROP_SHUTDOWN_METHOD = "shutdownMethod";
    static final String PROP_SHUTDOWN_SCRIPT = "shutdownScript";
    static final String PROP_START_SCRIPT = "startScript";

    static final String CONTENT_PROP_EXPLODE_ON_DEPLOY = "explodeOnDeploy";

    private Log log = LogFactory.getLog(this.getClass());

    private EmsConnection connection;

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log
     * file. Each time a connection error is encountered, this will be incremented. When the connection
     * is finally established, this will be reset to zero.
     */
    private int consecutiveConnectionErrors;

    private TomcatApplicationDeployer deployer;

    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private TomcatServerOperationsDelegate operationsDelegate;

    private ResourceContext<PlatformComponent> resourceContext;

    // JMXComponent Implementation  --------------------------------------------

    public EmsConnection getEmsConnection() {
        EmsConnection emsConnection = null;

        try {
            emsConnection = loadConnection();
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded");
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
        if (this.connection == null) {
            try {
                Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
                String installationPath = pluginConfig.getSimpleValue(PROP_INSTALLATION_PATH, null);

                ConnectionSettings connectionSettings = new ConnectionSettings();

                String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE).getStringValue();
                PropertySimple serverUrl = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY);

                connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(connectionTypeDescriptorClass).newInstance());
                // if not provided use the default serverUrl
                if (null != serverUrl) {
                    connectionSettings.setServerUrl(serverUrl.getStringValue());
                }
                connectionSettings.setPrincipal(pluginConfig.getSimpleValue(PRINCIPAL_CONFIG_PROP, null));
                connectionSettings.setCredentials(pluginConfig.getSimpleValue(CREDENTIALS_CONFIG_PROP, null));
                connectionSettings.setLibraryURI(installationPath);

                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.discoverServerClasses(connectionSettings);

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                // Tell EMS to make copies of jar files so that the ems classloader doesn't lock
                // application files (making us unable to update them)  Bug: JBNADM-670
                connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP, String.valueOf(Boolean.TRUE));

                // But tell it to put them in a place that we clean up when shutting down the agent (make sure tmp dir exists)
                File tempDir = resourceContext.getTemporaryDirectory();
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR, tempDir.getAbsolutePath());

                log.info("Loading connection [" + connectionSettings.getServerUrl() + "] with install path [" + connectionSettings.getLibraryURI() + "] and temp directory ["
                    + tempDir.getAbsolutePath() + "]");

                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                this.connection = connectionProvider.connect();

                this.connection.loadSynchronous(false); // this loads all the MBeans

                this.consecutiveConnectionErrors = 0;

                try {
                    this.deployer = new TomcatApplicationDeployer(this.connection);
                } catch (Throwable e) {
                    log.error("Unable to access MainDeployer MBean required for creation and deletion of managed " + "resources - this should never happen. Cause: " + e);
                }

                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the AS instance for resource [" + this.resourceContext.getResourceKey() + "]");
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
                    log.warn("Could not establish connection to the Tomcat instance [" + (consecutiveConnectionErrors + 1) + "] times for resource [" + resourceContext.getResourceKey() + "]", e);
                }

                if (log.isDebugEnabled())
                    log.debug("Could not connect to the Tomcat instance for resource [" + resourceContext.getResourceKey() + "]", e);

                consecutiveConnectionErrors++;

                throw e;
            }
        }

        return connection;
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
            throw new InvalidPluginConfigurationException("If the '" + TomcatServerComponent.PRINCIPAL_CONFIG_PROP + "' connection property is set, the '"
                + TomcatServerComponent.CREDENTIALS_CONFIG_PROP + "' connection property must also be set.");
        }

        if ((credentials != null) && (principal == null)) {
            throw new InvalidPluginConfigurationException("If the '" + TomcatServerComponent.CREDENTIALS_CONFIG_PROP + "' connection property is set, the '"
                + TomcatServerComponent.PRINCIPAL_CONFIG_PROP + "' connection property must also be set.");
        }
    }

    public void start(ResourceContext<PlatformComponent> context) throws SQLException {
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
                    throw new InvalidPluginConfigurationException("Invalid JMX credentials specified for connecting to this server.", e);
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
        try {
            EmsConnection connection = loadConnection();
            EmsBean bean = connection.getBean("Catalina:type=Server");

            // this is necessary to prove that that not only the connection exists but is servicing requests.
            bean.getAttribute("serverInfo").refresh();
            return AvailabilityType.UP;
        } catch (Exception e) {
            // If the connection is not servicing requests then close it. this seems necessary for the
            // Tomcat connection as when Tomcat does come up again it seems a new EMS connection is required,
            // otherwise we're not seeing EMS be able to pick up the new process.
            closeConnection();
            return AvailabilityType.DOWN;
        }
    }

    @SuppressWarnings("unchecked")
    ResourceContext getResourceContext() {
        return this.resourceContext;
    }

    public File getStartScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String script = pluginConfig.getSimpleValue(TomcatServerComponent.PROP_START_SCRIPT, "");
        File scriptFile = resolvePathRelativeToHomeDir(script);
        return scriptFile;
    }

    public File getInstallationPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        return new File(pluginConfig.getSimpleValue(TomcatServerComponent.PROP_INSTALLATION_PATH, ""));
    }

    public File getShutdownScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String script = pluginConfig.getSimpleValue(TomcatServerComponent.PROP_SHUTDOWN_SCRIPT, "");
        File scriptFile = resolvePathRelativeToHomeDir(script);
        return scriptFile;
    }

    private File resolvePathRelativeToHomeDir(@NotNull
    String path) {
        return resolvePathRelativeToHomeDir(this.resourceContext.getPluginConfiguration(), path);
    }

    static File resolvePathRelativeToHomeDir(Configuration pluginConfig, String path) {
        File configDir = new File(path);
        if (!configDir.isAbsolute()) {
            String jbossHomeDir = getRequiredPropertyValue(pluginConfig, TomcatServerComponent.PROP_INSTALLATION_PATH);
            configDir = new File(jbossHomeDir, path);
        }

        return configDir;
    }

    private static String getRequiredPropertyValue(Configuration config, String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
        SupportedOperations operation = Enum.valueOf(SupportedOperations.class, name.toUpperCase());

        return operationsDelegate.invoke(operation, parameters);
    }

    TomcatApplicationDeployer getDeployer() {
        return this.deployer;
    }

    void undeployWar(String contextRoot) throws TomcatApplicationDeployer.DeployerException {
        // As it stands Tomcat will respond to the placement or removal of the physical Web App itself. We
        // call removeServiced prior to the file delete to let TC know to stop servicing the app, hopefully
        // for a cleaner removal.
        // There is no additional MBean interaction required 
        getEmsConnection();
        if (null == this.connection) {
            log.warn("Unable to undeploy " + contextRoot + ", because we could not connect to the Tomcat instance.");
            return;
        }
        if (null == this.deployer) {
            throw new IllegalStateException("Unable to undeploy " + contextRoot + ", because MainDeployer MBean could " + "not be accessed - this should never happen.");
        }
        this.deployer.undeploy(contextRoot);
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

    public File getConfigurationPath() {
        return new File(this.getInstallationPath(), "webapps");
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        String resourceTypeName = report.getResourceType().getName();
        try {
            if (TomcatWarComponent.RESOURCE_TYPE_NAME.equals(resourceTypeName)) {
                warCreate(report);
            } else {
                throw new UnsupportedOperationException("Unsupported Resource type: " + resourceTypeName);
            }
        } catch (Exception e) {
            setErrorOnCreateResourceReport(report, e);
        }
        return report;
    }

    private void warCreate(CreateResourceReport report) throws Exception {
        ResourcePackageDetails details = report.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        String archiveName = key.getName();

        if (!archiveName.toLowerCase().endsWith(".war")) {
            setErrorOnCreateResourceReport(report, "Deployed file must have a .war extension");
            return;
        }

        Configuration deployTimeConfiguration = details.getDeploymentTimeConfiguration();
        PropertySimple explodeOnDeployProp = deployTimeConfiguration.getSimple(CONTENT_PROP_EXPLODE_ON_DEPLOY);

        if (explodeOnDeployProp == null || explodeOnDeployProp.getBooleanValue() == null) {
            setErrorOnCreateResourceReport(report, "Explode On Deploy property is required.");
            return;
        }
        boolean explodeOnDeploy = explodeOnDeployProp.getBooleanValue();

        // Perform the deployment        
        File deployDir = getConfigurationPath();
        FileContentDelegate fileContent = new FileContentDelegate(deployDir, details.getPackageTypeName());

        if (explodeOnDeploy) {
            // trim off the .war suffix because we want to deploy into a root directory named after the app name
            archiveName = archiveName.substring(0, archiveName.length() - 4);
        }

        File path = new File(deployDir, archiveName);
        if (path.exists()) {
            setErrorOnCreateResourceReport(report, "A web application named " + path.getName() + " is already deployed with path " + path + ".");
            return;
        }

        File tempDir = resourceContext.getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), "tomcat-war.bin");
        OutputStream osForTempDir = new BufferedOutputStream(new FileOutputStream(tempFile));
        ContentContext contentContext = this.resourceContext.getContentContext();

        ContentServices contentServices = contentContext.getContentServices();
        contentServices.downloadPackageBitsForChildResource(contentContext, TomcatWarComponent.RESOURCE_TYPE_NAME, key, osForTempDir);

        osForTempDir.close();

        // check for content
        boolean valid = isWebApplication(tempFile);
        if (!valid) {
            setErrorOnCreateResourceReport(report, "Expected a " + TomcatWarComponent.RESOURCE_TYPE_NAME + " file, but its format/content did not match");
            return;
        }

        InputStream isForTempDir = new BufferedInputStream(new FileInputStream(tempFile));
        fileContent.createContent(path, isForTempDir, explodeOnDeploy);

        // Resource key should match the following:        
        // Catalina:j2eeType=WebModule,name=//localhost/<archiveName>,J2EEApplication=none,J2EEServer=none        

        String resourceKey = "Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=//localhost/" + archiveName;

        report.setResourceName(archiveName);
        report.setResourceKey(resourceKey);
        report.setStatus(CreateResourceStatus.SUCCESS);
    }

    static void setErrorOnCreateResourceReport(CreateResourceReport report, String message) {
        setErrorOnCreateResourceReport(report, message, null);
    }

    static void setErrorOnCreateResourceReport(CreateResourceReport report, Exception e) {
        setErrorOnCreateResourceReport(report, null, e);
    }

    static void setErrorOnCreateResourceReport(CreateResourceReport report, String message, Exception e) {
        report.setStatus(CreateResourceStatus.FAILURE);
        report.setErrorMessage(message);
        report.setException(e);
    }

    private boolean isWebApplication(File file) {
        JarFile jfile = null;
        try {
            jfile = new JarFile(file);
            JarEntry entry = jfile.getJarEntry("WEB-INF/web.xml");

            return (null != entry);
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        } finally {
            if (jfile != null)
                try {
                    jfile.close();
                } catch (IOException e) {
                    log.info("Exception when trying to close the war file: " + e.getMessage());
                }
        }
    }
}