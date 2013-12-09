/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;
import com.jboss.jbossnetwork.product.jbpm.handlers.InPluginControlActionFacade;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnectException;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;

import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.jboss.on.common.jbossas.JBossASPaths;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.ApplicationServerComponent;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pluginapi.util.SelectiveSkippingEntityResolver;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jbossas.helper.JavaSystemProperties;
import org.rhq.plugins.jbossas.helper.MainDeployer;
import org.rhq.plugins.jbossas.util.ConnectionFactoryConfigurationEditor;
import org.rhq.plugins.jbossas.util.DatasourceConfigurationEditor;
import org.rhq.plugins.jbossas.util.DeploymentUtility;
import org.rhq.plugins.jbossas.util.FileContentDelegate;
import org.rhq.plugins.jbossas.util.FileNameUtility;
import org.rhq.plugins.jbossas.util.JBossASContentFacetDelegate;
import org.rhq.plugins.jbossas.util.JBossASSnapshotReport;
import org.rhq.plugins.jbossas.util.XMLConfigurationEditor;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.jmx.util.ObjectNameQueryUtility;

/**
* Resource component for managing JBoss AS 3.2.3 through 4.2.x, and JBoss EAP and SOA-P 4.x.
*
* @author Greg Hinkle
* @author John Mazzitelli
* @author Jason Dobies
* @author Ian Springer
*/
public class JBossASServerComponent<T extends ResourceComponent<?>> implements MeasurementFacet, OperationFacet,
    JMXComponent<T>, CreateChildResourceFacet, ApplicationServerComponent, ContentFacet, SupportFacet {
    // Constants  --------------------------------------------

    private static final String LOCALHOST = "localhost";
    public static final String NAMING_URL_CONFIG_PROP = "namingURL";
    public static final String JBOSS_HOME_DIR_CONFIG_PROP = "jbossHomeDir";
    public static final String CONFIGURATION_PATH_CONFIG_PROP = "configurationPath";
    public static final String SCRIPT_PREFIX_CONFIG_PROP = "scriptPrefix";
    public static final String CONFIGURATION_SET_CONFIG_PROP = "configurationSet";
    public static final String START_SCRIPT_CONFIG_PROP = "startScript";
    public static final String START_WAIT_MAX_PROP = "startWaitMax";
    public static final String STOP_WAIT_MAX_PROP = "stopWaitMax";
    public static final String SHUTDOWN_SCRIPT_CONFIG_PROP = "shutdownScript";
    public static final String SHUTDOWN_MBEAN_CONFIG_PROP = "shutdownMbeanName";
    public static final String SHUTDOWN_MBEAN_OPERATION_CONFIG_PROP = "shutdownMbeanOperation";
    public static final String SHUTDOWN_METHOD_CONFIG_PROP = "shutdownMethod";
    public static final String JAVA_HOME_PATH_CONFIG_PROP = "javaHomePath";
    @Deprecated
    public static final String AVAIL_CHECK_PERIOD_CONFIG_PROP = "availabilityCheckPeriod";

    public static final String BINDING_ADDRESS_CONFIG_PROP = "bindingAddress";

    static final String DEFAULT_START_SCRIPT = "bin" + File.separator + "run."
        + ((File.separatorChar == '/') ? "sh" : "bat");
    static final String DEFAULT_SHUTDOWN_SCRIPT = "bin" + File.separator + "shutdown."
        + ((File.separatorChar == '/') ? "sh" : "bat");
    static final String DEFAULT_JAVA_HOME = System.getProperty(JavaSystemProperties.JAVA_HOME);
    static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    // The following constants reference the exact name of the resource types as defined in the plugin descriptor
    private static final String RESOURCE_TYPE_DATASOURCE = "Datasource";
    private static final String RESOURCE_TYPE_CONNECTION_FACTORY = "ConnectionFactory";
    private static final String RESOURCE_TYPE_EAR = "Enterprise Application (EAR)";
    private static final String RESOURCE_TYPE_WAR = "Web Application (WAR)";
    private static final String RESOURCE_TYPE_SAR = "Service Archive (SAR)"; // Not yet used

    private static final String JNP_DISABLE_DISCOVERY_JNP_INIT_PROP = "jnp.disableDiscovery";

    private static final String DISTRIBUTED_REPLICANT_MANAGER_MBEAN_NAME_TEMPLATE = "jboss:partitionName=%partitionName%,service=DistributedReplicantManager";

    /**
     * This is the timeout for the initial connection to the MBeanServer that is made by {@link #start(ResourceContext)}.
     */
    private static final int JNP_TIMEOUT = 30 * 1000; // 30 seconds
    /**
     * This is the timeout for MBean attribute gets/sets and operations invoked on the remote MBeanServer.
     * NOTE: This timeout comes into play if the JBossAS instance has gone down since the original JNP connection was made.
     */
    private static final int JNP_SO_TIMEOUT = 15 * 1000; // 15 seconds

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(JBossASServerComponent.class);

    private ResourceContext resourceContext;
    private ContentContext contentContext;

    private JBossASContentFacetDelegate contentFacetDelegate;

    private EmsConnection connection;
    private File configPath;
    private String configSet;

    private final Map<PackageType, FileContentDelegate> contentDelegates = new HashMap<PackageType, FileContentDelegate>();

    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private JBossASServerOperationsDelegate operationsDelegate;

    private LogFileEventResourceComponentHelper logFileEventDelegate;

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log
     * file. Each time a connection error is encountered, this will be incremented. When the connection
     * is finally established, this will be reset to zero.
     */
    private int consecutiveConnectionErrors;

    private MainDeployer mainDeployer;

    private boolean loggedHijackedJnpUrlError;

    // ResourceComponent Implementation  --------------------------------------------

    public void start(ResourceContext context) throws Exception {
        this.resourceContext = context;
        this.contentContext = context.getContentContext();

        this.operationsDelegate = new JBossASServerOperationsDelegate(this, resourceContext.getSystemInformation());

        validatePluginConfiguration();

        Configuration pluginConfig = context.getPluginConfiguration();

        this.configPath = resolvePathRelativeToHomeDir(getRequiredPropertyValue(pluginConfig,
            CONFIGURATION_PATH_CONFIG_PROP));
        if (!this.configPath.exists()) {
            throw new InvalidPluginConfigurationException("Configuration path '" + configPath + "' does not exist.");
        }
        this.configSet = pluginConfig.getSimpleValue(CONFIGURATION_SET_CONFIG_PROP, this.configPath.getName());

        // Until the bugs get worked out of the calls back into the PC's operation framework, use the implementation
        // that will simply make calls directly in the plugin.
        // controlFacade = new PluginContainerControlActionFacade(operationContext, this);
        ControlActionFacade controlFacade = new InPluginControlActionFacade(this);

        JBossASPaths jbossPaths = new JBossASPaths();
        jbossPaths.setHomeDir(getPluginConfiguration().getSimpleValue(JBOSS_HOME_DIR_CONFIG_PROP, null));
        jbossPaths.setServerDir(getPluginConfiguration().getSimpleValue(CONFIGURATION_PATH_CONFIG_PROP, null));

        JBPMWorkflowManager workflowManager = new JBPMWorkflowManager(contentContext, controlFacade, jbossPaths);

        this.contentFacetDelegate = new JBossASContentFacetDelegate(workflowManager, this.configPath);

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

        this.logFileEventDelegate = new LogFileEventResourceComponentHelper(this.resourceContext);
        this.logFileEventDelegate.startLogFileEventPollers();

        return;
    }

    public void stop() {
        this.logFileEventDelegate.stopLogFileEventPollers();
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Error closing JBoss AS connection: " + e);
            }
            this.connection = null;
        }

        this.loggedHijackedJnpUrlError = false;
    }

    public AvailabilityType getAvailability() {
        try {
            File serverHomeViaJnp = getServerHome();
            if (this.configPath.getCanonicalPath().equals(serverHomeViaJnp.getCanonicalPath())) {
                this.loggedHijackedJnpUrlError = false;
                return AvailabilityType.UP;
            } else {
                // A different server must have been started on our JNP URL - this is definitely something about which
                // the user should be informed.
                if (!this.loggedHijackedJnpUrlError) {
                    String namingURL = this.resourceContext.getPluginConfiguration().getSimpleValue(
                        NAMING_URL_CONFIG_PROP, null);
                    String message = "Availability check for JBoss AS Resource with configPath [" + this.configPath
                        + "] has connected to a different running JBoss AS instance which is installed at ["
                        + serverHomeViaJnp + "] using namingURL [" + namingURL
                        + "] - returning AvailabilityType.DOWN...";
                    log.error(message);
                    this.loggedHijackedJnpUrlError = true;
                    // Throw an exception, so the PC can send the message to the Server for display in the GUI.
                    throw new RuntimeException(message);
                }
                return AvailabilityType.DOWN;
            }
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    private File getServerHome() throws Exception {
        EmsConnection connection = loadConnection();
        EmsBean bean = connection.getBean("jboss.system:type=ServerConfig");
        File serverHomeViaJnp;
        EmsAttribute serverHomeDirAttrib = bean.getAttribute("ServerHomeDir");
        if (serverHomeDirAttrib != null) {
            serverHomeViaJnp = (File) serverHomeDirAttrib.refresh();
        } else {
            // We have a non-null MBean but a null ServerHomeDir attribute. This most likely means we're
            // connected to a JBoss 5.x or 6.x instance, because in those versions the ServerConfig MBean no
            // longer has a ServerHomeDir attribute. It instead has a ServerHomeLocation attribute, so give
            // that a try, so getAvailabilityNow() can print a more intelligent warning.
            EmsAttribute serverHomeLocationAttrib = bean.getAttribute("ServerHomeLocation");
            URL serverHomeLocation = (URL) serverHomeLocationAttrib.refresh();
            serverHomeViaJnp = toFile(serverHomeLocation);
        }
        return serverHomeViaJnp;
    }

    private static File toFile(URL url) {
        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            file = new File(url.getPath());
        }
        return file;
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            if (name.equals("partitionName")) {
                String partitionName = getPartitionName();
                if (partitionName != null) {
                    report.addData(new MeasurementDataTrait(request, partitionName));
                }
            } else {
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
                        report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
                    } else {
                        report.addData(new MeasurementDataTrait(request, valueObject.toString()));
                    }
                } catch (Exception e) {
                    log.error("Failed to obtain measurement [" + name + "]", e);
                }
            }
        }
    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration configuration) throws InterruptedException {
        JBossASServerSupportedOperations operation = Enum.valueOf(JBossASServerSupportedOperations.class,
            name.toUpperCase());
        return operationsDelegate.invoke(operation, configuration);
    }

    // ContentFacet Implementation  --------------------------------------------

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return contentFacetDelegate.generateInstallationSteps(packageDetails);
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return contentFacetDelegate.deployPackages(packages, contentServices);
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return contentFacetDelegate.removePackages(packages);
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return contentFacetDelegate.discoverDeployedPackages(type);
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return contentFacetDelegate.retrievePackageBits(packageDetails);
    }

    // CreateChildResourceFacet Implementation  --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport report) {
        String resourceTypeName = report.getResourceType().getName();
        try {
            if (resourceTypeName.equals(RESOURCE_TYPE_DATASOURCE)) {
                datasourceCreate(report);
            } else if (resourceTypeName.equals(RESOURCE_TYPE_CONNECTION_FACTORY)) {
                connectionFactoryCreate(report);
            } else if (resourceTypeName.equals(RESOURCE_TYPE_EAR) || resourceTypeName.equals(RESOURCE_TYPE_WAR)) {
                earWarCreate(report, resourceTypeName);
            } else {
                throw new UnsupportedOperationException("Unknown Resource type: " + resourceTypeName);
            }
        } catch (Exception e) {
            setErrorOnCreateResourceReport(report, e);
        }
        return report;
    }

    // SupportFacet Implementation  --------------------------------------------

    public SnapshotReportResults getSnapshotReport(SnapshotReportRequest request) throws Exception {
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        String tmpDir = resourceContext.getTemporaryDirectory().getAbsolutePath();
        JBossASSnapshotReport report = new JBossASSnapshotReport(request.getName(), request.getDescription(),
            pluginConfig, this.configPath.getCanonicalPath(), tmpDir);
        File reportFile = report.generate();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(reportFile));
        SnapshotReportResults results = new SnapshotReportResults(inputStream);
        return results;
    }

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

    // ApplicationComponent Implementation  --------------------------------------------

    /**
     * Return the absolute path of this JBoss server's configuration directory (e.g.
     * "C:\opt\jboss-4.2.2.GA\server\default").
     *
     * @return the absolute path of this JBoss server's configuration directory (e.g.
     *         "C:\opt\jboss-4.2.2.GA\server\default")
     */
    @NotNull
    public File getConfigurationPath() {
        return this.configPath;
    }

    // Public  --------------------------------------------

    public Configuration getPluginConfiguration() {
        return resourceContext.getPluginConfiguration();
    }

    public File getDeploymentFilePath(String objectName) {
        return DeploymentUtility.getDescriptorFile(connection, objectName);
    }

    public String getConfigurationSet() {
        return this.configSet;
    }

    /**
     * Return the absolute path of this JBoss server's start script (e.g. "C:\opt\jboss-4.2.1.GA\bin\run.sh").
     *
     * @return the absolute path of this JBoss server's start script (e.g. "C:\opt\jboss-4.2.1.GA\bin\run.sh")
     */
    @NotNull
    public File getStartScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String startScript = pluginConfig.getSimpleValue(START_SCRIPT_CONFIG_PROP, DEFAULT_START_SCRIPT);
        File startScriptFile = resolvePathRelativeToHomeDir(startScript);
        return startScriptFile;
    }

    /**
     * Return the absolute path of this JBoss server's shutdown script (e.g. "C:\opt\jboss-4.2.1.GA\bin\shutdown.sh").
     *
     * @return the absolute path of this JBoss server's shutdown script (e.g. "C:\opt\jboss-4.2.1.GA\bin\shutdown.sh")
     */
    @NotNull
    public File getShutdownScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String shutdownScript = pluginConfig.getSimpleValue(SHUTDOWN_SCRIPT_CONFIG_PROP, DEFAULT_SHUTDOWN_SCRIPT);
        File shutdownScriptFile = resolvePathRelativeToHomeDir(shutdownScript);
        return shutdownScriptFile;
    }

    /**
     * Return the absolute path of this JBoss server's JAVA_HOME directory (e.g. "C:\opt\jdk1.5.0_14"); will only return
     * null in the rare case when the "java.home" system property is not set, and when this is the case, a warning will
     * be logged.
     *
     * @return the absolute path of this JBoss server's JAVA_HOME directory (e.g. "C:\opt\jdk1.5.0_14"); will only be
     *         null in the rare case when the "java.home" system property is not set
     */
    @Nullable
    public File getJavaHomePath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String javaHomePath = pluginConfig.getSimpleValue(JAVA_HOME_PATH_CONFIG_PROP, DEFAULT_JAVA_HOME);
        if (javaHomePath == null) {
            log.warn("The '" + JavaSystemProperties.JAVA_HOME
                + "' System property is not set - unable to set default value for the '" + JAVA_HOME_PATH_CONFIG_PROP
                + "' connection property.");
        }

        File javaHome = (javaHomePath != null) ? new File(javaHomePath) : null;
        return javaHome;
    }

    @NotNull
    public String getBindingAddress() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String bindingAddress = pluginConfig.getSimpleValue(BINDING_ADDRESS_CONFIG_PROP, DEFAULT_BIND_ADDRESS);
        return bindingAddress;
    }

    public MainDeployer getMainDeployer() {
        return this.mainDeployer;
    }

    // Here we do any validation that couldn't be achieved via the metadata-based constraints.
    private void validatePluginConfiguration() {
        validateJBossHomeDirProperty();
        validateJavaHomePathProperty();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String principal = pluginConfig.getSimpleValue(JBossASServerComponent.PRINCIPAL_CONFIG_PROP, null);
        String credentials = pluginConfig.getSimpleValue(JBossASServerComponent.CREDENTIALS_CONFIG_PROP, null);
        if ((principal != null) && (credentials == null)) {
            throw new InvalidPluginConfigurationException("If the '" + JBossASServerComponent.PRINCIPAL_CONFIG_PROP
                + "' connection property is set, the '" + JBossASServerComponent.CREDENTIALS_CONFIG_PROP
                + "' connection property must also be set.");
        }

        if ((credentials != null) && (principal == null)) {
            throw new InvalidPluginConfigurationException("If the '" + JBossASServerComponent.CREDENTIALS_CONFIG_PROP
                + "' connection property is set, the '" + JBossASServerComponent.PRINCIPAL_CONFIG_PROP
                + "' connection property must also be set.");
        }
    }

    void validateJBossHomeDirProperty() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String jbossHome = getRequiredPropertyValue(pluginConfig, JBOSS_HOME_DIR_CONFIG_PROP);
        File jbossHomeDir = new File(jbossHome);
        if (!jbossHomeDir.isAbsolute()) {
            throw new InvalidPluginConfigurationException(
                JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP
                    + " connection property ('"
                    + jbossHomeDir
                    + "') is not an absolute path. Note, on Windows, absolute paths must start with the drive letter (e.g. C:).");
        }

        if (!jbossHomeDir.exists()) {
            throw new InvalidPluginConfigurationException(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP
                + " connection property ('" + jbossHomeDir + "') does not exist.");
        }

        if (!jbossHomeDir.isDirectory()) {
            throw new InvalidPluginConfigurationException(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP
                + " connection property ('" + jbossHomeDir + "') is a file, not a directory.");
        }
    }

    void validateJavaHomePathProperty() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String javaHome = pluginConfig.getSimpleValue(JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP, null);
        if (javaHome != null) {
            File javaHomeDir = new File(javaHome);
            if (!javaHomeDir.isAbsolute()) {
                throw new InvalidPluginConfigurationException(
                    JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP
                        + " connection property ('"
                        + javaHomeDir
                        + "') is not an absolute path. Note, on Windows, absolute paths must start with the drive letter (e.g. C:).");
            }

            if (!javaHomeDir.exists()) {
                throw new InvalidPluginConfigurationException(JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP
                    + " connection property ('" + javaHomeDir + "') does not exist.");
            }

            if (!javaHomeDir.isDirectory()) {
                throw new InvalidPluginConfigurationException(JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP
                    + " connection property ('" + javaHomeDir + "') is not a directory.");
            }
        }
    }

    private void datasourceCreate(CreateResourceReport report) throws Exception {
        Configuration config = report.getResourceConfiguration();
        String name = config.getSimple("jndi-name").getStringValue();
        if (DeploymentUtility.isDuplicateJndiName(connection, XMLConfigurationEditor.DATASOURCE_MBEAN_NAME, name)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            String errorMessage = getDuplicateJndiNameErrorMessage(report.getResourceType().getName(), name);
            report.setErrorMessage(errorMessage);
            return;
        }
        File deployDir = new File(getConfigurationPath(), "deploy");
        File dsFile = new File(deployDir, FileNameUtility.formatFileName(name) + "-ds.xml");
        DatasourceConfigurationEditor.updateDatasource(dsFile, name, report);

        deployFile(dsFile);

        String objectName = String.format("jboss.jca:name=%s,service=DataSourceBinding", name);
        // IMPORTANT: The object name must be canonicalized so it matches the resource key that
        //            MBeanResourceDiscoveryComponent uses, which is the canonical object name.
        report.setResourceKey(getCanonicalName(objectName));
        setResourceName(report, name);
    }

    private void connectionFactoryCreate(CreateResourceReport report) throws MainDeployer.DeployerException {
        Configuration config = report.getResourceConfiguration();
        String name = config.getSimple("jndi-name").getStringValue();

        if (DeploymentUtility.isDuplicateJndiName(connection, XMLConfigurationEditor.CONNECTION_MBEAN_NAME, name)) {
            String errorMessage = getDuplicateJndiNameErrorMessage(report.getResourceType().getName(), name);
            setErrorOnCreateResourceReport(report, errorMessage, null);
            return;
        }
        File deployDir = new File(getConfigurationPath(), "deploy");
        File dsFile = new File(deployDir, FileNameUtility.formatFileName(name) + "-ds.xml");
        ConnectionFactoryConfigurationEditor.updateConnectionFactory(dsFile, name, report);

        deployFile(dsFile);

        String objectName = String.format("jboss.jca:name=%s,service=ConnectionFactoryBinding", name);
        // IMPORTANT: The object name must be canonicalized so it matches the resource key that
        //            MBeanResourceDiscoveryComponent uses, which is the canonical object name.
        report.setResourceKey(getCanonicalName(objectName));
        setResourceName(report, name);
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

    void deployFile(File file) throws MainDeployer.DeployerException {
        getEmsConnection();
        if (this.connection == null) {
            log.warn("Unable to deploy " + file + ", because we could not connect to the JBoss instance.");
            return;
        }
        if (this.mainDeployer == null) {
            throw new IllegalStateException("Unable to deploy " + file + ", because MainDeployer MBean could "
                + "not be accessed - this should never happen.");
        }
        this.mainDeployer.deploy(file);
    }

    void redeployFile(File file) throws MainDeployer.DeployerException {
        getEmsConnection();
        if (this.connection == null) {
            log.warn("Unable to redeploy " + file + ", because we could not connect to the JBoss instance.");
            return;
        }
        if (this.mainDeployer == null) {
            throw new IllegalStateException("Unable to redeploy " + file + ", because MainDeployer MBean could "
                + "not be accessed - this should never happen.");
        }
        this.mainDeployer.redeploy(file);
    }

    void undeployFile(File file) throws MainDeployer.DeployerException {
        getEmsConnection();
        if (this.connection == null) {
            log.warn("Unable to undeploy " + file + ", because we could not connect to the JBoss instance.");
            return;
        }
        if (this.mainDeployer == null) {
            throw new IllegalStateException("Unable to undeploy " + file + ", because MainDeployer MBean could "
                + "not be accessed - this should never happen.");
        }
        this.mainDeployer.undeploy(file);
    }

    private String getDuplicateJndiNameErrorMessage(String resourceTypeName, String name) {
        return "Duplicate JNDI Name: " + name + " - a " + resourceTypeName + " with that name already exists.";
    }

    private static String getCanonicalName(String objectName) {
        ObjectName on;
        try {
            on = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Malformed JMX object name: " + objectName + " - "
                + e.getLocalizedMessage());
        }
        return on.getCanonicalName();
    }

    private void setResourceName(CreateResourceReport report, String baseName) {
        String resourceName;
        if (report.getUserSpecifiedResourceName() != null) {
            resourceName = report.getUserSpecifiedResourceName();
        } else {
            resourceName = baseName + " " + report.getResourceType().getName();
        }
        report.setResourceName(resourceName);
    }

    @NotNull
    private File resolvePathRelativeToHomeDir(@NotNull String path) {
        return resolvePathRelativeToHomeDir(this.resourceContext.getPluginConfiguration(), path);
    }

    @NotNull
    static File resolvePathRelativeToHomeDir(Configuration pluginConfig, @NotNull String path) {
        File configDir = new File(path);
        if (!FileUtil.isAbsolutePath(path)) {
            String jbossHomeDir = getRequiredPropertyValue(pluginConfig, JBOSS_HOME_DIR_CONFIG_PROP);
            configDir = new File(jbossHomeDir, path);
        }

        // BZ 903402 - get the real absolute path - under most conditions, it's the same thing, but if on windows
        //             the drive letter might not have been specified - this makes sure the drive letter is specified.
        return configDir.getAbsoluteFile();
    }

    @NotNull
    private static String getRequiredPropertyValue(@NotNull Configuration config, @NotNull String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    /**
     * Returns the operation delegate configured against the resource represented by this component.
     *
     * @return will not be <code>null</code>
     */
    @NotNull
    public JBossASServerOperationsDelegate getOperationsDelegate() {
        return operationsDelegate;
    }

    private void earWarCreate(CreateResourceReport report, String resourceTypeName) throws Exception {
        ResourcePackageDetails details = report.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        String archiveName = key.getName();

        // First check to see if the file name has the correct extension. Reject if the user attempts to
        // deploy a WAR file with a bad extension.
        String expectedExtension = resourceTypeName.equals(RESOURCE_TYPE_EAR) ? "ear" : "war";

        int lastPeriod = archiveName.lastIndexOf(".");
        String extension = archiveName.substring(lastPeriod + 1);
        if (lastPeriod == -1 || !expectedExtension.equals(extension)) {
            setErrorOnCreateResourceReport(report, "Incorrect extension specified on filename [" + archiveName
                + "]. Expected [" + expectedExtension + "]");
            return;
        }

        Configuration deployTimeConfiguration = details.getDeploymentTimeConfiguration();
        String deployDirectory = deployTimeConfiguration.getSimple("deployDirectory").getStringValue();
        if (deployDirectory == null) {
            // should not be null, but you never know ..
            setErrorOnCreateResourceReport(report, "Property 'deployDirectory' was unexpectedly null");
            return;
        }

        // Verify the user did not enter a path that represents a security issue:
        // - No absolute directories; must be relative to the configuration path
        // - Cannot contain parent directory references
        File relativeDeployDir = new File(deployDirectory);

        if (relativeDeployDir.isAbsolute()) {
            setErrorOnCreateResourceReport(report, "Path to deploy (deployDirectory) must be a relative path. "
                + "Path specified: " + deployDirectory);
            return;
        }

        if (deployDirectory.contains("..")) {
            setErrorOnCreateResourceReport(report,
                "Path to deploy (deployDirectory) may not reference the parent directory. " + "Path specified: "
                    + deployDirectory);
            return;
        }

        boolean createBackup = false;
        PropertySimple backupProperty = deployTimeConfiguration.getSimple("createBackup");
        if (backupProperty != null && backupProperty.getBooleanValue() != null && backupProperty.getBooleanValue())
            createBackup = true;

        // Perform the deployment
        File deployDir = new File(getConfigurationPath(), deployDirectory);
        FileContentDelegate deployer = new FileContentDelegate(deployDir, "", details.getPackageTypeName());

        File path = deployer.getPath(details);
        if (!createBackup && path.exists()) {
            setErrorOnCreateResourceReport(report, "A " + resourceTypeName + " file named " + path.getName()
                + " is already deployed with path " + path + ".");
            return;
        }

        PropertySimple zipProperty = deployTimeConfiguration.getSimple("deployZipped");

        if (zipProperty == null || zipProperty.getBooleanValue() == null) {
            setErrorOnCreateResourceReport(report, "Zipped property is required.");
            return;
        }

        boolean zip = zipProperty.getBooleanValue();

        File tempDir = resourceContext.getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), "ear_war.bin");
        OutputStream osForTempDir = new BufferedOutputStream(new FileOutputStream(tempFile));

        ContentServices contentServices = contentContext.getContentServices();
        contentServices.downloadPackageBitsForChildResource(contentContext, resourceTypeName, key, osForTempDir);

        osForTempDir.close();

        // check for content
        boolean valid = isOfType(tempFile, resourceTypeName);
        if (!valid) {
            setErrorOnCreateResourceReport(report, "Expected a " + resourceTypeName
                + " file, but its format/content did not match");
            return;
        }

        deployer.createContent(details, tempFile, !zip, createBackup);

        String vhost = null;
        if (resourceTypeName.equals(RESOURCE_TYPE_WAR)) {
            vhost = getVhostFromWarFile(tempFile);
        }

        // Resource key should match the following:
        // EAR: jboss.management.local:J2EEServer=Local,j2eeType=J2EEApplication,name=rhq.ear
        // WAR: jboss.management.local:J2EEApplication=null,J2EEServer=Local,j2eeType=WebModule,name=embedded-console.war

        String resourceKey;
        if (resourceTypeName.equals(RESOURCE_TYPE_EAR)) {
            resourceKey = "jboss.management.local:J2EEServer=Local,j2eeType=J2EEApplication,name=" + archiveName;
        } else {
            resourceKey = "jboss.management.local:J2EEApplication=null,J2EEServer=Local,j2eeType=WebModule,name="
                + archiveName;
            if (!LOCALHOST.equals(vhost))
                resourceKey += ",vhost=" + vhost;
        }

        report.setResourceName(archiveName);
        report.setResourceKey(resourceKey);
        report.setStatus(CreateResourceStatus.SUCCESS);

        try {
            deployFile(path);
        } catch (MainDeployer.DeployerException e) {
            log.debug("Failed to deploy [" + path + "] - undeploying and deleting [" + path + "]...");
            report.setStatus(CreateResourceStatus.FAILURE);
            try {
                undeployFile(path);
                FileUtils.purge(path, true);
            } catch (Exception e1) {
                log.error("Failed to rollback deployment of [" + path + "].", e1);
            }
            throw e;
        }
    }

    /**
     * Parse the passed war file, try to read an enclosed jboss-web.xml and look for
     * virtual-hosts in it. If found, return one virtual host name. Else return localhost.
     * @param warFile File pointer pointing to a .war file
     * @return The name of a defined virtual host or localhost
     */
    private String getVhostFromWarFile(File warFile) {

        JarFile jfile = null;
        InputStream is = null;
        try {
            jfile = new JarFile(warFile);
            JarEntry entry = jfile.getJarEntry("WEB-INF/jboss-web.xml");
            if (entry != null) {
                is = jfile.getInputStream(entry);
                SAXBuilder saxBuilder = new SAXBuilder();
                SelectiveSkippingEntityResolver entityResolver = SelectiveSkippingEntityResolver
                    .getDtdAndXsdSkippingInstance();
                saxBuilder.setEntityResolver(entityResolver);

                Document doc = saxBuilder.build(is);
                Element root = doc.getRootElement(); // <jboss-web>
                List<Element> vHosts = root.getChildren("virtual-host");
                if (vHosts == null || vHosts.isEmpty()) {
                    if (log.isDebugEnabled())
                        log.debug("No vhosts found in war file, using " + LOCALHOST);
                    return LOCALHOST;
                }

                // So we have vhost, just return one of them, this is enough
                Element vhost = vHosts.get(0);
                return vhost.getText();
            }
        } catch (Exception ioe) {
            log.warn("Exception when getting vhost from war file : " + ioe.getMessage());
        } finally {
            if (jfile != null) {
                if (is != null) {
                    try {
                        // see http://bugs.sun.com/view_bug.do?bug_id=6735255 for why we do this
                        is.close();
                    } catch (IOException e) {
                        log.info("Exception when trying to close the war file stream: " + e.getMessage());
                    }
                }
                try {
                    jfile.close();
                } catch (IOException e) {
                    log.info("Exception when trying to close the war file: " + e.getMessage());
                }
            }
        }

        // We're not able to determine a vhost, so return localhost
        return LOCALHOST;
    }

    /**
     * Check to see if the passed file is actually in jar format and contains a
     * <ul>
     * <li>WEB-INF/web.xml for .war </li>
     * <li>META-INF/application.xml for .ear</li>
     * <li>META-INF/jboss.service.xml for .sar</li>
     * </ul>
     * @param file File to check
     * @param type Type to match - see RESOURCE_TYPE_SAR, RESOURCE_TYPE_WAR and RESOURCE_TYPE_EAR
     * @return true is the file is in jar format and matches the type
     */
    private boolean isOfType(File file, String type) {
        JarFile jfile = null;
        try {
            jfile = new JarFile(file);
            JarEntry entry;
            if (RESOURCE_TYPE_WAR.equals(type))
                entry = jfile.getJarEntry("WEB-INF/web.xml");
            else if (RESOURCE_TYPE_EAR.equals(type))
                entry = jfile.getJarEntry("META-INF/application.xml");
            else if (RESOURCE_TYPE_SAR.equals(type)) // Not yet used
                entry = jfile.getJarEntry("META-INF/jboss-service.xml");
            else {
                entry = null; // unknown type
                log.warn("isOfType: " + type + " is unknown - not a valid file");
            }

            if (entry != null)
                return true;

            return false;
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

    @Nullable
    private String getPartitionName() {
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
            DISTRIBUTED_REPLICANT_MANAGER_MBEAN_NAME_TEMPLATE);
        try {
            List<EmsBean> mBeans = loadConnection().queryBeans(queryUtility.getTranslatedQuery());
            if (mBeans.size() == 1) {
                if (queryUtility.setMatchedKeyValues(mBeans.get(0).getBeanName().getKeyProperties())) {
                    return queryUtility.getVariableValues().get("partitionName");
                }
            }
        } catch (Exception e) {
            log.error("Could not load partition name as connection could not be loaded");
        }
        return null;
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
    // TODO (ips): Refactor this method to use the JmxConnectionHelper class from the jboss-as-common module, which is
    //             what the jboss-as-5 plugin uses.
    private synchronized EmsConnection loadConnection() throws Exception {
        if (this.connection == null) {
            try {
                Configuration pluginConfig = resourceContext.getPluginConfiguration();
                String jbossHomeDir = pluginConfig.getSimpleValue(JBOSS_HOME_DIR_CONFIG_PROP, null);

                ConnectionSettings connectionSettings = new ConnectionSettings();

                String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                    .getStringValue();
                connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
                    connectionTypeDescriptorClass).newInstance());
                connectionSettings.setServerUrl(pluginConfig.getSimpleValue(NAMING_URL_CONFIG_PROP, null));
                connectionSettings.setPrincipal(pluginConfig.getSimpleValue(PRINCIPAL_CONFIG_PROP, null));
                connectionSettings.setCredentials(pluginConfig.getSimpleValue(CREDENTIALS_CONFIG_PROP, null));
                connectionSettings.setLibraryURI(jbossHomeDir);

                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.discoverServerClasses(connectionSettings);

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                connectionSettings.getAdvancedProperties().setProperty(JNP_DISABLE_DISCOVERY_JNP_INIT_PROP, "true");

                // Make sure the timeout always happens, even if the JBoss server is hung.
                connectionSettings.getAdvancedProperties().setProperty("jnp.timeout", String.valueOf(JNP_TIMEOUT));
                connectionSettings.getAdvancedProperties().setProperty("jnp.sotimeout", String.valueOf(JNP_SO_TIMEOUT));

                // Tell EMS to make copies of jar files so that the ems classloader doesn't lock
                // application files (making us unable to update them)  Bug: JBNADM-670
                // TODO GH: turn this off in the embedded case
                connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP,
                    String.valueOf(Boolean.TRUE));

                // But tell it to put them in a place that we clean up when shutting down the agent
                connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                    resourceContext.getTemporaryDirectory().getAbsolutePath());

                connectionSettings.getAdvancedProperties().setProperty(InternalVMTypeDescriptor.DEFAULT_DOMAIN_SEARCH,
                    "jboss");

                log.info("Loading JBoss connection [" + connectionSettings.getServerUrl() + "] with install path ["
                    + connectionSettings.getLibraryURI() + "]...");

                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                this.connection = connectionProvider.connect();

                this.connection.loadSynchronous(false); // this loads all the MBeans

                this.consecutiveConnectionErrors = 0;

                try {
                    this.mainDeployer = new MainDeployer(this.connection);
                } catch (Exception e) {
                    log.error("Unable to access MainDeployer MBean required for creation and deletion of managed "
                        + "resources - this should never happen. Cause: " + e);
                }

                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the AS instance for resource ["
                        + this.resourceContext.getResourceKey() + "]");
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
                    log.warn(
                        "Could not establish connection to the JBoss AS instance [" + (consecutiveConnectionErrors + 1)
                            + "] times for resource [" + resourceContext.getResourceKey() + "]", e);
                }

                if (log.isDebugEnabled())
                    log.debug(
                        "Could not connect to the JBoss AS instance for resource [" + resourceContext.getResourceKey()
                            + "]", e);

                consecutiveConnectionErrors++;

                throw e;
            }
        }

        return connection;
    }
}
