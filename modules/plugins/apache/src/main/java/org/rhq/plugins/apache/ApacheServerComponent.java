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
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
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
import org.rhq.plugins.apache.parser.ApacheConfigWriter;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.Glob;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.WWWUtils;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.plugins.www.snmp.SNMPClient;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;

/**
 * The resource component for Apache 2.x servers.
 *
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheServerComponent implements ApacheConfigurationBase<PlatformComponent>,MeasurementFacet, OperationFacet,
    ConfigurationFacet, CreateChildResourceFacet {

    private final Log log = LogFactory.getLog(this.getClass());
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
    public static final String DEFAULT_ERROR_LOG_PATH = "logs" + File.separator
        + ((File.separatorChar == '/') ? "error_log" : "error.log");
    private static final String ERROR_LOG_ENTRY_EVENT_TYPE = "errorLogEntry";
    
    private ResourceContext<PlatformComponent> resourceContext;
    private EventContext eventContext;
    private SNMPClient snmpClient;
    private URL url;
    private ApacheBinaryInfo binaryInfo;
    private long availPingTime = -1;
    private ApacheServerConfiguration config;
    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private ApacheServerOperationsDelegate operationsDelegate;

    public void start(ResourceContext<PlatformComponent> resourceContext) throws Exception {
        log.info("Initializing server component for server [" + resourceContext.getResourceKey() + "]...");
        this.resourceContext = resourceContext;
        this.eventContext = resourceContext.getEventContext();
        this.snmpClient = new SNMPClient();
        this.config = new ApacheServerConfiguration(resourceContext);
        
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

            this.url = config.getUrl();

            if (!configured && url ==null) {
                throw new InvalidPluginConfigurationException(
                    "Neither SNMP nor an URL for checking availability has been configured");
            }

            File executablePath = config.getExecutablePath();
            try {
                this.binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(), this.resourceContext
                    .getSystemInformation());
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("'" + executablePath
                    + "' is not a valid Apache executable (" + e + ").");
            }

            this.operationsDelegate = new ApacheServerOperationsDelegate(this, this.resourceContext
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
        try {
            if (this.url != null) {
                long t1 = System.currentTimeMillis();
                available = WWWUtils.isAvailable(this.url);
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
        try {
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();

            ApacheDirectiveTree tree = loadParser();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            return mapping.updateConfiguration(tree.getRootNode(), resourceConfigDef);
        } catch (Exception e) {
            log.error("Failed to load Apache configuration.", e);
            throw e;
        }
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ApacheDirectiveTree tree = null;
        try {
            tree = loadParser();
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);

            mapping.updateApache(tree.getRootNode(), report.getConfiguration(), resourceConfigDef);
            saveParser(tree);

            log.info("Apache configuration was updated");
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            
            conditionalRestart();
        } catch (Exception e) {
            if (tree != null)
                log.error("Augeas failed to save configuration ");
            else
                log.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessageFromThrowable(e);
        }
   }
  
    public CreateResourceReport createResource(CreateResourceReport report) {
        
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
                                    
            ApacheDirectiveTree tree = null;
            
            String[] vhostDefs = vhostDef.split(" ");
            HttpdAddressUtility.Address addr;    
            
            try{
            tree = loadParser();
            addr = config.getAddressUtility().getVirtualHostSampleAddress(tree, vhostDefs[0], serverName);
            } catch (Exception e) {
              report.setStatus(CreateResourceStatus.FAILURE);
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
            vhostPluginConfig.put(new PropertySimple(ApacheServerConfiguration.PLUGIN_CONFIG_PROP_URL, url));
                        
            Configuration pluginConfig = resourceContext.getPluginConfiguration();
            String creationType = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_VHOST_CREATION_POLICY,
                PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE);

            ApacheDirective vhost = null;
            String vhostFile = config.getHttpdConfFile().getAbsolutePath();
            
            if (PLUGIN_CONFIG_VHOST_IN_SINGLE_FILE_PROP_VALUE.equals(creationType)) {
                vhost = tree.createNode(tree.getRootNode(), "<VirtualHost");
            } else if (PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE.equals(creationType)) {
                String mask = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_VHOST_FILES_MASK, null);
                if (mask == null) {
                    report.setErrorMessage("No virtual host file mask configured.");
                } else {
                    vhostFile = getNewVhostFileName(addr, mask);
                    File vhostFileFile = new File(vhostFile);
                                                                            
                    try {
                        vhostFileFile.createNewFile();
                    } catch (IOException e) {
                        log.error("Failed to create a new vhost file: " + vhostFile, e);
                    }
                 
                    //check if the the file is already Includede                    
                    boolean isIncluded = false;
                    for(String glob : tree.getGlobs()) {
                        if (Glob.matches(config.getServerRoot(), glob, vhostFileFile)) {
                           isIncluded=true;
                           break;
                        }
                    }

                    if (!isIncluded){
                        ApacheDirective include = tree.createNode(tree.getRootNode(), "Include");
                        include.addValue(vhostFile);
                        }
                    
                    vhost = tree.createNode(tree.getRootNode(),"<VirtualHost");
                    vhost.setFile(vhostFile);                    
                }
            }
            
            if (vhost == null) {
                report.setStatus(CreateResourceStatus.FAILURE);
            } else {
                try {
                    List<String> params = new ArrayList<String>();
                    for(int i = 0; i < vhostDefs.length; ++i) {
                        params.add(vhostDefs[i]);
                    }
                    vhost.setValues(params);
                    
                    ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);                    
                    mapping.updateApache(vhost, vhostResourceConfig, vhostResourceConfigDef);
                    
                    saveParser(tree);
                    report.setStatus(CreateResourceStatus.SUCCESS);
                    
                    conditionalRestart();
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
            File errorLogFile = config.resolvePathRelativeToServerRoot(pluginConfig,pluginConfig.getSimpleValue(
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
        File errorLogFile = config.resolvePathRelativeToServerRoot(pluginConfig,pluginConfig.getSimpleValue(
            PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH, DEFAULT_ERROR_LOG_PATH));
        this.eventContext.unregisterEventPoller(ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile.getPath());
    }

    
    private String getNewVhostFileName(HttpdAddressUtility.Address address, String mask) {
        String filename = address.host + "_" + address.port;
        String fullPath = mask.replace("*", filename);
        
        File file = config.getFileRelativeToServerRoot(fullPath);
        
        int i = 1;
        while (file.exists()) {
            filename = address.host + "_" + address.port + "-" + (i++);
            fullPath = mask.replace("*", filename);
            file = config.getFileRelativeToServerRoot(fullPath);
        }
        return file.getAbsolutePath();
    }
    
     
    public boolean saveParser(ApacheDirectiveTree tree){
      ApacheConfigWriter writer = new ApacheConfigWriter(tree);
      return writer.save(tree.getRootNode());     
    }

    @Override
    public ApacheDirective getNode(ApacheDirectiveTree tree) {
        return tree.getRootNode();
    }

    @Override
    public ApacheDirectiveTree loadParser() {
        return config.loadParser();
    }
    
    public ApacheServerConfiguration getServerConfiguration(){
        return config;
    }
}
