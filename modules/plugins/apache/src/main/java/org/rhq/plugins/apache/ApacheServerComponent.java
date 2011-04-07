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
package org.rhq.plugins.apache;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.augeas.Augeas;
import net.augeas.AugeasException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.util.Glob;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.event.log.LogFileEventPoller;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.apache.augeas.ApacheAugeasNode;
import org.rhq.plugins.apache.augeas.AugeasConfigurationApache;
import org.rhq.plugins.apache.augeas.AugeasTreeBuilderApache;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.ConfigurationTimestamp;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.plugins.www.snmp.SNMPClient;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;
import org.rhq.plugins.www.util.WWWUtils;
import org.rhq.rhqtransform.AugeasRHQComponent;

/**
 * The resource component for Apache 2.x servers.
 *
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheServerComponent implements AugeasRHQComponent<PlatformComponent>, MeasurementFacet, OperationFacet,
    ConfigurationFacet, CreateChildResourceFacet {

    public static final String CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE = "Configuration is supported only for Apache version 2 and up using Augeas. You either have an old version of Apache or Augeas is not installed.";

    private final Log log = LogFactory.getLog(this.getClass());

    public static final String PLUGIN_CONFIG_PROP_SERVER_ROOT = "serverRoot";
    public static final String PLUGIN_CONFIG_PROP_EXECUTABLE_PATH = "executablePath";
    public static final String PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH = "controlScriptPath";
    public static final String PLUGIN_CONFIG_PROP_URL = "url";
    public static final String PLUGIN_CONFIG_PROP_HTTPD_CONF = "configFile";

    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST = "snmpAgentHost";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT = "snmpAgentPort";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_COMMUNITY = "snmpAgentCommunity";

    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH = "errorLogFilePath";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_EVENTS_ENABLED = "errorLogEventsEnabled";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_MINIMUM_SEVERITY = "errorLogMinimumSeverity";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_INCLUDES_PATTERN = "errorLogIncludesPattern";
    public static final String PLUGIN_CONFIG_PROP_VHOST_FILES_MASK = "vhostFilesMask";
    public static final String PLUGIN_CONFIG_PROP_VHOST_CREATION_POLICY = "vhostCreationPolicy";
    
    public static final String PLUGIN_CONFIG_PROP_RESTART_AFTER_CONFIG_UPDATE = "restartAfterConfigurationUpdate";
    
    public static final String PLUGIN_CONFIG_VHOST_IN_SINGLE_FILE_PROP_VALUE = "single-file";
    public static final String PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE = "vhost-per-file";
    
    public static final String AUXILIARY_INDEX_PROP = "_index";

    public static final String SERVER_BUILT_TRAIT = "serverBuilt";
    public static final String AUGEAS_ENABLED = "augeasEnabled";

    public static final String DEFAULT_EXECUTABLE_PATH = "bin" + File.separator
        + ((File.separatorChar == '/') ? "httpd" : "Apache.exe");

    public static final String DEFAULT_ERROR_LOG_PATH = "logs" + File.separator
        + ((File.separatorChar == '/') ? "error_log" : "error.log");

    private static final String ERROR_LOG_ENTRY_EVENT_TYPE = "errorLogEntry";

    private static final String[] CONTROL_SCRIPT_PATHS = { "bin/apachectl", "sbin/apachectl", "bin/apachectl2",
        "sbin/apachectl2" };
    
    private ResourceContext<PlatformComponent> resourceContext;
    private EventContext eventContext;
    private SNMPClient snmpClient;
    private URL url;
    private ApacheBinaryInfo binaryInfo;
    private long availPingTime = -1;
    
    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private ApacheServerOperationsDelegate operationsDelegate;

    public void start(ResourceContext<PlatformComponent> resourceContext) throws Exception {
        log.info("Initializing server component for server [" + resourceContext.getResourceKey() + "]...");
        this.resourceContext = resourceContext;
        this.eventContext = resourceContext.getEventContext();
        this.snmpClient = new SNMPClient();

        try {
            boolean configured = false;

            SNMPSession snmpSession = getSNMPSession();
            if (!snmpSession.ping()) {
                log
                    .warn("Failed to connect to SNMP agent at "
                        + snmpSession
                        + "\n"
                        + ". Make sure\n1) the managed Apache server has been instrumented with the JON SNMP module,\n"
                        + "2) the Apache server is running, and\n"
                        + "3) the SNMP agent host, port, and community are set correctly in this resource's connection properties.\n"
                        + "The agent will not be able to record metrics from apache httpd without SNMP");
            } else {
                configured = true;
            }

            Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
            String url = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_URL, null);
            if (url != null) {
                try {
                    this.url = new URL(url);
                    if (this.url.getPort() == 0) {
                        log
                            .error("The 'url' connection property is invalid - 0 is not a valid port; please change the value to the "
                                + "port the \"main\" Apache server is listening on. NOTE: If the 'url' property was set this way "
                                + "after autodiscovery, you most likely did not include the port in the ServerName directive for "
                                + "the \"main\" Apache server in httpd.conf.");
                    } else {
                        configured = true;
                    }
                } catch (MalformedURLException e) {
                    throw new InvalidPluginConfigurationException("Value of '" + PLUGIN_CONFIG_PROP_URL
                        + "' connection property ('" + url + "') is not a valid URL.");
                }
            }

            if (!configured) {
                throw new InvalidPluginConfigurationException(
                    "Neither SNMP nor an URL for checking availability has been configured");
            }

            File executablePath = getExecutablePath();
            try {
                this.binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(), this.resourceContext
                    .getSystemInformation());
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("'" + executablePath
                    + "' is not a valid Apache executable (" + e + ").");
            }

            this.operationsDelegate = new ApacheServerOperationsDelegate(this, pluginConfig, this.resourceContext
                .getSystemInformation());

            startEventPollers();
        } catch (Exception e) {
            if (this.snmpClient != null) {
                this.snmpClient.close();
            }
            throw e;
        }
    }

    public void stop() {
        stopEventPollers();
        if (this.snmpClient != null) {
            this.snmpClient.close();
        }
        return;
    }

    public AvailabilityType getAvailability() {
        // TODO: If URL is not set, rather than falling back to pinging the SNMP agent,
        //       try to find a pid file under the server root, and then check if the
        //       process is running.
        boolean available;
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        PropertySimple disableCertValidation = pluginConfig.getSimple("disableCertificateValidation");
        try {
            if (this.url != null) {
                long t1 = System.currentTimeMillis();
                available = WWWUtils.isAvailable(this.url, disableCertValidation.getBooleanValue());
                availPingTime = System.currentTimeMillis() - t1;
            } else {
                available = getSNMPSession().ping();
                availPingTime = -1;
            }
        } catch (Exception e) {
            available = false;
        }

        return (available) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
        SNMPSession snmpSession = getSNMPSession();
        boolean snmpPresent = snmpSession.ping();

        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            if (metricName.equals(SERVER_BUILT_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(schedule, this.binaryInfo.getBuilt());
                report.addData(trait);
            } else if (metricName.equals("rhq_avail_ping_time")) {
                if (availPingTime == -1)
                    continue; // Skip if we have no data
                MeasurementDataNumeric num = new MeasurementDataNumeric(schedule, (double) availPingTime);
                report.addData(num);
            } else {
                // Assume anything else is an SNMP metric.
                if (!snmpPresent)
                    continue; // Skip this metric if no SNMP present

                try {
                    //noinspection UnnecessaryLocalVariable
                    String mibName = metricName;
                    List<SNMPValue> snmpValues = snmpSession.getColumn(mibName);
                    if (snmpValues.isEmpty()) {
                        log.error("No values found for MIB name [" + mibName + "].");
                        continue;
                    }

                    SNMPValue snmpValue = snmpValues.get(0);
                    boolean valueIsTimestamp = isValueTimestamp(mibName);

                    log.debug("Collected SNMP metric [" + mibName + "], value = " + snmpValue);

                    addSnmpMetricValueToReport(report, schedule, snmpValue, valueIsTimestamp);
                } catch (SNMPException e) {
                    log.error("An error occurred while attempting to collect an SNMP metric.", e);
                }
            }
        }
    }

    private boolean isValueTimestamp(String mibName) {
        return (mibName.equals("wwwServiceStartTime"));
    }

    @Nullable
    public OperationResult invokeOperation(@NotNull String name, @NotNull Configuration params) throws Exception {
        log.info("Invoking operation [" + name + "] on server [" + this.resourceContext.getResourceKey() + "]...");
        return this.operationsDelegate.invokeOperation(name, params);
    }

    public Configuration loadResourceConfiguration() throws Exception {
        if (!isAugeasEnabled())
            throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);

        try {
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();

            AugeasTree tree = getAugeasTree();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            return mapping.updateConfiguration(tree.getRootNode(), resourceConfigDef);
        } catch (Exception e) {
            log.error("Failed to load Apache configuration.", e);
            throw e;
        }
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        if (!isAugeasEnabled()){
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            return;
        }
        Configuration originalConfig = report.getConfiguration().deepCopy(true);
        AugeasTree tree = null;
        try {
            tree = getAugeasTree();
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);

            mapping.updateAugeas(tree.getRootNode(), report.getConfiguration(), resourceConfigDef);
            tree.save();

            log.info("Apache configuration was updated");
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            
            finishConfigurationUpdate(report);
        } catch (Exception e) {
            if (tree != null) {
                log.error("Augeas failed to save configuration " + tree.summarizeAugeasError());
                e = new AugeasException("Failed to save configuration: " + tree.summarizeAugeasError() + " ", e);
            }                
            else
                log.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessageFromThrowable(e);
            if (!originalConfig.equals(report.getConfiguration())) {
                log.error("Configuration has changed");
            }
            else {
                log.error("Configuratio has not changed");
            }
        }
   }

    public AugeasProxy getAugeasProxy() throws AugeasException {
        File tempDir = resourceContext.getDataDirectory();
        if (!tempDir.exists())
            throw new RuntimeException("Loading of lens failed");
        AugeasConfigurationApache config = new AugeasConfigurationApache(tempDir.getAbsolutePath(),resourceContext.getPluginConfiguration());
        AugeasTreeBuilderApache builder = new AugeasTreeBuilderApache();
        AugeasProxy augeasProxy = new AugeasProxy(config, builder);
        augeasProxy.load();
        return augeasProxy;
    }

    public AugeasTree getAugeasTree() throws AugeasException {
        AugeasProxy proxy = getAugeasProxy();
        String module = ((AugeasConfigurationApache)proxy.getConfiguration()).getAugeasModuleName();
        
        return proxy.getAugeasTree(module, true);
    }
    
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (!isAugeasEnabled()){
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage("Resources can be created only when augeas is enabled.");
            return report;
        }
        if (ApacheVirtualHostServiceComponent.RESOURCE_TYPE_NAME.equals(report.getResourceType().getName())) {
            Configuration vhostResourceConfig = report.getResourceConfiguration();
            ConfigurationDefinition vhostResourceConfigDef = report.getResourceType().getResourceConfigurationDefinition();
            Configuration vhostPluginConfig = report.getPluginConfiguration();
            
            String vhostDef = report.getUserSpecifiedResourceName();
            String serverName = vhostResourceConfig.getSimpleValue(ApacheVirtualHostServiceComponent.SERVER_NAME_CONFIG_PROP, null);
            
            //determine the resource key
            String resourceKey = vhostDef;
            if (serverName != null) {
                resourceKey = serverName + "|" + resourceKey;
            }
            
            //determine the resource name
            AugeasProxy proxy = getAugeasProxy();
            AugeasTree tree = getAugeasTree();
            String[] vhostDefs = vhostDef.split(" ");
            HttpdAddressUtility.Address addr;            
            try{   
                ApacheDirectiveTree parserTree = new ApacheDirectiveTree();
                ApacheParser parser = new ApacheParserImpl(parserTree,getServerRoot().getAbsolutePath());
         
                ApacheConfigReader.buildTree(getHttpdConfFile().getAbsolutePath(), parser);
                addr = getAddressUtility().getVirtualHostSampleAddress(parserTree, vhostDefs[0], serverName, false);
            } catch (Exception e) {
              report.setStatus(CreateResourceStatus.FAILURE);
              report.setErrorMessage("Wrong format of virtual host resource name.");
              report.setException(e);
              return report;
          }
            
            String resourceName;
            if (serverName != null) {
                resourceName = addr.host + ":" + addr.port;
            } else {
                resourceName = resourceKey;
            }
            
            report.setResourceKey(resourceKey);
            report.setResourceName(resourceName);

            //fill in the plugin config
            String url = "http://" + addr.host + ":" + addr.port + "/";
            vhostPluginConfig.put(new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url));
            
            //determine the sequence number of the new vhost
            List<AugeasNode> existingVhosts = tree.matchRelative(tree.getRootNode(), "<VirtualHost");
            int seq = existingVhosts.size() + 1;
            
            Configuration pluginConfig = resourceContext.getPluginConfiguration();
            String creationType = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_VHOST_CREATION_POLICY,
                PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE);

            AugeasNode vhost = null;
            String vhostFile = proxy.getConfiguration().getModules().get(0).getConfigFiles().get(0);
            
            if (PLUGIN_CONFIG_VHOST_IN_SINGLE_FILE_PROP_VALUE.equals(creationType)) {
                vhost = tree.createNode(tree.getRootNode(), "<VirtualHost", null, seq);
            } else if (PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE.equals(creationType)) {
                String mask = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_VHOST_FILES_MASK, null);
                if (mask == null) {
                    report.setErrorMessage("No virtual host file mask configured.");
                } else {
                    vhostFile = getNewVhostFileName(addr, mask);
                    File vhostFileFile = new File(vhostFile);
                    
                    //we're creating a new file here, so we must ensure that Augeas does have this file
                    //on its load path, otherwise it will refuse to create it.
                    AugeasConfigurationApache config = (AugeasConfigurationApache) proxy.getConfiguration();
                    AugeasModuleConfig moduleConfig = config.getModuleByName(config.getAugeasModuleName());
                    boolean willPersist = false;
                    for(String glob : moduleConfig.getIncludedGlobs()) {
                        if (Glob.matches(getServerRoot(), glob, vhostFileFile)) {
                            willPersist = true;
                            break;
                        }
                    }
                    
                    if (!willPersist) {
                        //the file wouldn't be loaded by augeas
                        moduleConfig.addIncludedGlob(vhostFile);
                    }
                    
                    try {
                        vhostFileFile.createNewFile();
                    } catch (IOException e) {
                        log.error("Failed to create a new vhost file: " + vhostFile, e);
                    }
                    
                    proxy.load();
                    tree = proxy.getAugeasTree(moduleConfig.getModuletName(), true);
                    
                    vhost = tree.createNode(AugeasTree.AUGEAS_DATA_PATH + vhostFile + "/<VirtualHost");
                    ((ApacheAugeasNode)vhost).setParentNode(tree.getRootNode());
                    
                    if (!willPersist) {
                        //this also means that there was no include
                        //that would load the file, so we have to
                        //add the include directive to the main conf.
                        List<AugeasNode> includes = tree.matchRelative(tree.getRootNode(), "Include");
                        AugeasNode include = tree.createNode(tree.getRootNode(), "Include", null, includes.size() + 1);
                        tree.createNode(include, "param", vhostFile, 0);
                    }
                }
            }
            
            if (vhost == null) {
                report.setStatus(CreateResourceStatus.FAILURE);
            } else {
                try {
                    for(int i = 0; i < vhostDefs.length; ++i) {
                        tree.createNode(vhost, "param", vhostDefs[i], i + 1);
                    }
                    ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);                    
                    mapping.updateAugeas(vhost, vhostResourceConfig, vhostResourceConfigDef);
                    
                    tree.save();
                    report.setStatus(CreateResourceStatus.SUCCESS);
                    
                    finishChildResourceCreate(report);
                } catch (Exception e) {
                    report.setStatus(CreateResourceStatus.FAILURE);
                    report.setException(e);
                }
            }
        }
        return report;
    }

    /**
     * Returns an SNMP session that can be used to communicate with this server's SNMP agent.
     *
     * @return an SNMP session that can be used to communicate with this server's SNMP agent
     *
     * @throws Exception on failure to initialize the SNMP session
     */
    @NotNull
    public SNMPSession getSNMPSession() throws Exception {
        return ApacheServerComponent.getSNMPSession(this.snmpClient, this.resourceContext.getPluginConfiguration());
    }

    @NotNull
    public static SNMPSession getSNMPSession(SNMPClient snmpClient, Configuration pluginConfig) throws Exception {
        SNMPSession snmpSession;
        try {
            String host = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST).getStringValue();
            String portString = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT).getStringValue();
            int port = Integer.valueOf(portString);
            String community = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_COMMUNITY).getStringValue();
            snmpSession = snmpClient.getSession(host, port, community, SNMPClient.SNMPVersion.V2C);
        } catch (SNMPException e) {
            throw new Exception("Error getting SNMP session: " + e.getMessage(), e);
        }

        return snmpSession;
    }

    /**
     * Return the absolute path of this Apache server's server root (e.g. "C:\Program Files\Apache Group\Apache2").
     *
     * @return the absolute path of this Apache server's server root (e.g. "C:\Program Files\Apache Group\Apache2")
     */
    @NotNull
    public File getServerRoot() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
        return new File(serverRoot);
    }

    /**
     * Return the absolute path of this Apache server's executable (e.g. "C:\Program Files\Apache
     * Group\Apache2\bin\Apache.exe").
     *
     * @return the absolute path of this Apache server's executable (e.g. "C:\Program Files\Apache
     *         Group\Apache2\bin\Apache.exe")
     */
    @NotNull
    public File getExecutablePath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String executablePath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, null);
        File executableFile;
        if (executablePath != null) {
            executableFile = resolvePathRelativeToServerRoot(executablePath);
        } else {
            String serverRoot=null;
          
                ApacheDirectiveTree tree = loadParser();
                List<ApacheDirective> directives = tree.search("/ServerRoot");
                if (!directives.isEmpty())
                   if (!directives.get(0).getValues().isEmpty())    
                     serverRoot = directives.get(0).getValues().get(0);
                
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) // UNIX
            {
                // Try some combinations in turn
                executableFile = new File(serverRoot, "bin/httpd");
                if (!executableFile.exists()) {
                    executableFile = new File(serverRoot, "bin/apache2");
                }
                if (!executableFile.exists()) {
                    executableFile = new File(serverRoot, "bin/apache");
                }
            } else // Windows
            {
                executableFile = new File(serverRoot, "bin/Apache.exe");
            }
        }

        return executableFile;
    }

    /**
     * @return The url the server is pinged for availability or null if the url is not set.
     */
    public @Nullable String getServerUrl() {
        return resourceContext.getPluginConfiguration().getSimpleValue(PLUGIN_CONFIG_PROP_URL, null);
    }
    
    /**
     * Returns the httpd.conf file
     * @return A File object that represents the httpd.conf file or null in case of error
     */
    public File getHttpdConfFile() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertySimple prop = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_HTTPD_CONF);
        if (prop == null || prop.getStringValue() == null)
            return null;
        return resolvePathRelativeToServerRoot(pluginConfig, prop.getStringValue());
    }

    /**
     * Return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     * Group\Apache2\bin\Apache.exe").
     *
     * On Unix we need to try various locations, as some unixes have bin/ conf/ .. all within one root
     * and on others those are separated.
     *
     * @return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     *         Group\Apache2\bin\Apache.exe")
     */
    @NotNull
    public File getControlScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String controlScriptPath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH, null);
        File controlScriptFile = null;
        if (controlScriptPath != null) {
            controlScriptFile = resolvePathRelativeToServerRoot(controlScriptPath);
        } else {
            boolean found = false;
            // First try server root as base
            String serverRoot = null;
            try {
                ApacheDirectiveTree tree = loadParser();
                List<ApacheDirective> directives = tree.search("/ServerRoot");
                if (!directives.isEmpty())
                    if (!directives.get(0).getValues().isEmpty())
                        serverRoot = directives.get(0).getValues().get(0);

            } catch (Exception e) {
                log.error("Could not load configuration parser.", e);
            }
            if (serverRoot != null) {
                for (String path : CONTROL_SCRIPT_PATHS) {
                    controlScriptFile = new File(serverRoot, path);
                    if (controlScriptFile.exists()) {
                        found = true;
                        break;
                    }
                }
            }
            
            //only try harder on the control script path on OSes with UNIX file system layout
            if (!found && resourceContext.getSystemInformation().getOperatingSystemType() != OperatingSystemType.WINDOWS) {
                String executablePath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, null);
                if (executablePath != null) {
                    // this is now something like /usr/sbin/httpd .. trim off the last 2 parts
                    int i = executablePath.lastIndexOf(File.separatorChar);
                    
                    if (i >= 0) {
                        executablePath = executablePath.substring(0, i);
                        i = executablePath.lastIndexOf(File.separatorChar);
                    }
                    
                    if (i >= 0) {
                        executablePath = executablePath.substring(0, i);
                        for (String path : CONTROL_SCRIPT_PATHS) {
                            controlScriptFile = new File(executablePath, path);
                            if (controlScriptFile.exists()) {
                                found = true;
                                break;
                            }
                        }
                    }                    
                }
            }
            
            if (!found) {
                controlScriptFile = getExecutablePath(); // fall back to the httpd binary
            }
        }

        return controlScriptFile;
    }

    @NotNull
    public ConfigurationTimestamp getConfigurationTimestamp() {
        AugeasConfigurationApache config = new AugeasConfigurationApache(resourceContext.getTemporaryDirectory().getAbsolutePath(),
                resourceContext.getPluginConfiguration()); 
        return new ConfigurationTimestamp(config.getAllConfigurationFiles());
    }
    
    /**
     * This method is supposed to be called from {@link #updateResourceConfiguration(ConfigurationUpdateReport)}
     * of this resource and any child resources.
     * 
     * Based on the plugin configuration of this resource, the Apache instance is either restarted or left as is.
     * 
     * @param report the report is updated with the error message and status is set to failure if the restart fails.
     */
    public void finishConfigurationUpdate(ConfigurationUpdateReport report) {
        try {
            conditionalRestart();
        } catch (Exception e) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessageFromThrowable(e);
        }
    }
    
    /**
     * This method is akin to {@link #finishConfigurationUpdate(ConfigurationUpdateReport)} but should
     * be used in the {@link #createResource(CreateResourceReport)} method.
     * 
     * @param report the report is updated with the error message and status is set to failure if the restart fails.
     */
    public void finishChildResourceCreate(CreateResourceReport report) {
        try {
            conditionalRestart();
        } catch (Exception e) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setException(e);
        }
    }
    
    /**
     * Conditionally restarts the server based on the settings in the plugin configuration of the server.
     * 
     * @throws Exception if the restart fails.
     */
    public void conditionalRestart() throws Exception {
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        boolean restart = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_RESTART_AFTER_CONFIG_UPDATE).getBooleanValue();
        if (restart) {
            operationsDelegate.invokeOperation("graceful_restart", new Configuration());
        }
    }
    
    /**
     * This method checks whether the supplied node that has been deleted from the tree didn't leave
     * the file it was contained in empty.
     * If the file is empty after deleting the node, the file is automatically deleted.
     * @param tree TODO
     * @param deletedNode the node that has been deleted from the tree.
     */
    public void deleteEmptyFile(AugeasTree tree, AugeasNode deletedNode) {
        File file = tree.getFile(deletedNode);
        List<AugeasNode> fileContents = tree.match(file.getAbsolutePath() + AugeasTree.PATH_SEPARATOR + "*");
        
        if (fileContents.size() == 0) {
            file.delete();
        }
    }
    
    // TODO: Move this method to a helper class.
    static void addSnmpMetricValueToReport(MeasurementReport report, MeasurementScheduleRequest schedule,
        SNMPValue snmpValue, boolean valueIsTimestamp) throws SNMPException {
        switch (schedule.getDataType()) {
        case MEASUREMENT: {
            MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, (double) snmpValue.toLong());
            report.addData(metric);
            break;
        }

        case TRAIT: {
            String stringValue;
            if (valueIsTimestamp) {
                stringValue = new Date(snmpValue.toLong()).toString();
            } else {
                stringValue = snmpValue.toString();
                if (stringValue.startsWith(SNMPConstants.TCP_PROTO_ID + ".")) {
                    // looks like a port - strip off the leading "TCP protocol id" (i.e. "1.3.6.1.2.1.6.")...
                    stringValue = stringValue.substring(stringValue.lastIndexOf('.') + 1);
                }
            }

            MeasurementDataTrait trait = new MeasurementDataTrait(schedule, stringValue);
            report.addData(trait);
            break;
        }

        default: {
            throw new IllegalStateException("SNMP metric request has unsupported data type: " + schedule.getDataType());
        }
        }
    }

    @NotNull
    private File resolvePathRelativeToServerRoot(@NotNull String path) {
        return resolvePathRelativeToServerRoot(this.resourceContext.getPluginConfiguration(), path);
    }

    //TODO this needs to go...
    @NotNull
    static File resolvePathRelativeToServerRoot(Configuration pluginConfig, @NotNull String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
            file = new File(serverRoot, path);
        }

        return file;
    }

    @NotNull
    static String getRequiredPropertyValue(@NotNull Configuration config, @NotNull String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    private void startEventPollers() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        Boolean enabled = Boolean.valueOf(pluginConfig
            .getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_EVENTS_ENABLED, null));
        if (enabled) {
            File errorLogFile = resolvePathRelativeToServerRoot(pluginConfig.getSimpleValue(
                PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH, DEFAULT_ERROR_LOG_PATH));
            ApacheErrorLogEntryProcessor processor = new ApacheErrorLogEntryProcessor(ERROR_LOG_ENTRY_EVENT_TYPE,
                errorLogFile);
            String includesPatternString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_INCLUDES_PATTERN,
                null);
            if (includesPatternString != null) {
                try {
                    Pattern includesPattern = Pattern.compile(includesPatternString);
                    processor.setIncludesPattern(includesPattern);
                } catch (PatternSyntaxException e) {
                    throw new InvalidPluginConfigurationException("Includes pattern [" + includesPatternString
                        + "] is not a valid regular expression.");
                }
            }
            String minimumSeverityString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_MINIMUM_SEVERITY,
                null);
            if (minimumSeverityString != null) {
                EventSeverity minimumSeverity = EventSeverity.valueOf(minimumSeverityString.toUpperCase());
                processor.setMinimumSeverity(minimumSeverity);
            }
            EventPoller poller = new LogFileEventPoller(this.eventContext, ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile,
                processor);
            this.eventContext.registerEventPoller(poller, 60, errorLogFile.getPath());
        }
    }

    private void stopEventPollers() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        File errorLogFile = resolvePathRelativeToServerRoot(pluginConfig.getSimpleValue(
            PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH, DEFAULT_ERROR_LOG_PATH));
        this.eventContext.unregisterEventPoller(ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile.getPath());
    }

    public HttpdAddressUtility getAddressUtility() {
        String version = resourceContext.getVersion();
        return HttpdAddressUtility.get(version);
    }
    
    private String getNewVhostFileName(HttpdAddressUtility.Address address, String mask) {
        String filename = address.host + "_" + address.port;
        String fullPath = mask.replace("*", filename);
        
        File file = getFileRelativeToServerRoot(fullPath);
        
        int i = 1;
        while (file.exists()) {
            filename = address.host + "_" + address.port + "-" + (i++);
            fullPath = mask.replace("*", filename);
            file = getFileRelativeToServerRoot(fullPath);
        }
        return file.getAbsolutePath();
    }
    
    private File getFileRelativeToServerRoot(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(getServerRoot(), path);
        }   
    }
    
    public ApacheDirectiveTree loadParser(){
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree,getServerRoot().getAbsolutePath());
        ApacheConfigReader.buildTree(getHttpdConfFile().getAbsolutePath(), parser);
        return tree;       
    }
    
    public boolean isAugeasEnabled() {
        
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertySimple prop = pluginConfig.getSimple(AUGEAS_ENABLED);
        if (prop == null || prop.getStringValue() == null)
            {
            return false;
            }
        
        String val = prop.getStringValue();
        
        if (val.equals("yes")){
            try {                         
               Augeas ag = new Augeas();                        
            }catch(Exception e){
                log.error("Augeas is enabled in configuration but was not found on the system.");
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);                
            }
            String version = resourceContext.getVersion();
            
            if (!version.startsWith("2.")) {
                log.error(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            }
            return true;
        }else{
            return false;                
        }
    }
}
