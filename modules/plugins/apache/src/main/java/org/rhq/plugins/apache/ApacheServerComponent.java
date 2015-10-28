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
package org.rhq.plugins.apache;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.augeas.Augeas;
import net.augeas.AugeasException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.util.Glob;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
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
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.apache.augeas.ApacheAugeasNode;
import org.rhq.plugins.apache.augeas.AugeasConfigurationApache;
import org.rhq.plugins.apache.augeas.AugeasTreeBuilderApache;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.ConfigurationTimestamp;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.PluginUtility;
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
 * @author Maxime Beck (Remplacement of the SNMP Module with mod_bmx)
 */
public class ApacheServerComponent implements AugeasRHQComponent, ResourceComponent<PlatformComponent>,
    MeasurementFacet, OperationFacet, ConfigurationFacet, CreateChildResourceFacet {

    private static final Log LOG = LogFactory.getLog(ApacheServerComponent.class);

    public static final String CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE = "Configuration and child resource creation/deletion support for Apache is optional. "
        + "If you switched it on by enabling Augeas support in the connection settings of the Apache server resource and still get this message, "
        + "it means that either your Apache version is not supported (only Apache 2.x is supported) or Augeas is not available on your platform."
        + " Please refer to your agent's log for the precise exception that is causing this behavior. It will logged at error level only once "
        + "per plugin container lifetime";

    public static final String PLUGIN_CONFIG_PROP_SERVER_ROOT = "serverRoot";
    public static final String PLUGIN_CONFIG_PROP_EXECUTABLE_PATH = "executablePath";
    public static final String PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH = "controlScriptPath";
    public static final String PLUGIN_CONFIG_PROP_URL = "url";
    public static final String PLUGIN_CONFIG_PROP_BMX_URL = "bmxUrl";
    public static final String PLUGIN_CONFIG_PROP_HTTPD_CONF = "configFile";
    public static final String AUGEAS_HTTP_MODULE_NAME = "Httpd";

    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST = "snmpAgentHost";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT = "snmpAgentPort";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_COMMUNITY = "snmpAgentCommunity";
    public static final String PLUGIN_CONFIG_PROP_SNMP_REQUEST_TIMEOUT = "snmpRequestTimeout";
    public static final String PLUGIN_CONFIG_PROP_SNMP_REQUEST_RETRIES = "snmpRequestRetries";

    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH = "errorLogFilePath";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_EVENTS_ENABLED = "errorLogEventsEnabled";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_MINIMUM_SEVERITY = "errorLogMinimumSeverity";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_INCLUDES_PATTERN = "errorLogIncludesPattern";
    public static final String PLUGIN_CONFIG_PROP_VHOST_FILES_MASK = "vhostFilesMask";
    public static final String PLUGIN_CONFIG_PROP_VHOST_CREATION_POLICY = "vhostCreationPolicy";

    public static final String PLUGIN_CONFIG_PROP_RESTART_AFTER_CONFIG_UPDATE = "restartAfterConfigurationUpdate";

    public static final String PLUGIN_CONFIG_VHOST_IN_SINGLE_FILE_PROP_VALUE = "single-file";
    public static final String PLUGIN_CONFIG_VHOST_PER_FILE_PROP_VALUE = "vhost-per-file";

    public static final String PLUGIN_CONFIG_CUSTOM_MODULE_NAMES = "customModuleNames";
    public static final String PLUGIN_CONFIG_MODULE_MAPPING = "moduleMapping";
    public static final String PLUGIN_CONFIG_MODULE_NAME = "moduleName";
    public static final String PLUGIN_CONFIG_MODULE_SOURCE_FILE = "moduleSourceFile";

    private static final long DEFAULT_SNMP_REQUEST_TIMEOUT = 2000L;
    private static final int DEFAULT_SNMP_REQUEST_RETRIES = 1;

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

    private String bmxUrl;
    private boolean useBMX = false;
    static Pattern typePattern = Pattern.compile(".*Type=([\\w-]+),.*");

    private ResourceContext<PlatformComponent> resourceContext;
    private EventContext eventContext;
    private SNMPClient snmpClient;
    private URL url;
    private ApacheBinaryInfo binaryInfo;
    private long availPingTime = -1;
    private boolean augeasErrorLogged;

    private Map<String, String> moduleNames;
    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private ApacheServerOperationsDelegate operationsDelegate;

    private AvailabilityType lastKnownAvailability;

    public void start(ResourceContext<PlatformComponent> resourceContext) throws Exception {
        LOG.info("Initializing Resource component for Apache Server [" + resourceContext.getResourceKey() + "]...");

        this.resourceContext = resourceContext;
        this.eventContext = resourceContext.getEventContext();
        this.snmpClient = new SNMPClient();

        boolean configured = false;

        try {

            SNMPSession snmpSession = getSNMPSession();
            if (!snmpSession.ping()) {
                LOG.warn("Failed to connect to SNMP agent at "
                    + snmpSession
                    + "\n"
                    + ". Make sure\n1) the managed Apache server has been instrumented with the JON SNMP module,\n"
                    + "2) the Apache server is running, and\n"
                    + "3) the SNMP agent host, port, and community are set correctly in this resource's connection properties.\n"
                    + "The agent might not be able to record metrics from apache httpd without SNMP");
            } else {
                configured = true;
            }

            Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
            String url = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_URL, null);
            if (url != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking url " + bmxUrl);
                }
                try {
                    this.url = new URL(url);
                    if (this.url.getPort() == 0) {
                        LOG.error("The 'URL' connection property is invalid - 0 is not a valid port; please change the value to the "
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

            bmxUrl = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_BMX_URL, null);
            if (bmxUrl != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking BMX url " + bmxUrl);
                }
                try {
                    URL uurl = new URL(bmxUrl);
                    if (uurl.getPort() == 0) {
                        LOG.error("The 'BMX Handler' connection property is invalid - 0 is not a valid port; please change the value to the "
                            + "port the Apache server is listening on.");
                    } else {
                        if (this.url == null) {
                            this.url = uurl;
                            configured = true;
                        }
                    }
                } catch (MalformedURLException e) {
                    throw new InvalidPluginConfigurationException("Value of '" + PLUGIN_CONFIG_PROP_BMX_URL
                        + "' connection property ('" + bmxUrl + "') is not a valid URL.");
                }
            }
            if (bmxUrl != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Testing BMX connection on " + bmxUrl);
                }
                try {
                    /* Check the BMX URL and use it if available */
                    URL uurl = new URL(bmxUrl);
                    HttpURLConnection conn = (HttpURLConnection) uurl.openConnection();
                    conn.connect();
                    if (conn.getResponseCode() == 200) {
                        useBMX = true;
                        LOG.info("BMX will be used to check availability");
                    }
                    conn.disconnect();
                } catch (Exception ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("BMX connection fails on " + bmxUrl + " with " + ex);
                    }
                }
            }


            if (!configured) {
                throw new InvalidPluginConfigurationException(
                    "Neither SNMP, BMX nor an URL for checking availability has been configured");
            }

            File executablePath = getExecutablePath();
            try {
                this.binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(),
                    this.resourceContext.getSystemInformation());
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("'" + executablePath
                    + "' is not a valid Apache executable (" + e + ").");
            }

            this.operationsDelegate = new ApacheServerOperationsDelegate(this, pluginConfig,
                this.resourceContext.getSystemInformation());

            //init the module names with the defaults
            moduleNames = new HashMap<String, String>(ApacheServerDiscoveryComponent.getDefaultModuleNames(binaryInfo
                .getVersion()));

            //and add the user-provided overrides/additions
            PropertyList list = resourceContext.getPluginConfiguration().getList(PLUGIN_CONFIG_CUSTOM_MODULE_NAMES);

            if (list != null) {
                for (Property p : list.getList()) {
                    PropertyMap map = (PropertyMap) p;
                    String sourceFile = map.getSimpleValue(PLUGIN_CONFIG_MODULE_SOURCE_FILE, null);
                    String moduleName = map.getSimpleValue(PLUGIN_CONFIG_MODULE_NAME, null);

                    if (sourceFile == null || moduleName == null) {
                        LOG.info("A corrupted module name mapping found (" + sourceFile + " = " + moduleName
                            + "). Check your module mappings in the plugin configuration for the server: "
                            + resourceContext.getResourceKey());
                        continue;
                    }

                    moduleNames.put(sourceFile, moduleName);
                }
            }

            startEventPollers();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing Resource component for Apache Server failed: " + e);
            }
            if (this.snmpClient != null) {
                this.snmpClient.close();
            }
            throw e;
        }

        this.lastKnownAvailability = UP;
    }

    public void stop() {
        this.url = null;
        stopEventPollers();
        if (this.snmpClient != null) {
            this.snmpClient.close();
        }
        this.lastKnownAvailability = null;
    }

    public String getBMXUrl() {
        return bmxUrl;
    }

    public boolean getUseBMX() {
        return useBMX;
    }

    public AvailabilityType getAvailability() {
        lastKnownAvailability = getAvailabilityInternal();
        return lastKnownAvailability;
    }

    private AvailabilityType getAvailabilityInternal() {
        // TODO: If URL is not set, rather than falling back to pinging the SNMP agent,
        //       try to find a pid file under the server root, and then check if the
        //       process is running.
        boolean available;
        try {
            if (this.url != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to ping the server for availability: " + this.url);
                }
                long t1 = System.currentTimeMillis();
                int timeout = PluginUtility.getAvailabilityFacetTimeout();
                AvailabilityResult availabilityResult = WWWUtils.checkAvailability(this.url, timeout);
                if (availabilityResult.getAvailabilityType() == UP) {
                    available = true;
                } else {
                    available = false;
                    if (lastKnownAvailability == UP) {
                        switch (availabilityResult.getErrorType()) {
                        case CANNOT_CONNECT:
                            LOG.warn("Could not connect to Apache server " + resourceContext.getResourceDetails()
                                + ", availability will be reported as " + DOWN.name());
                            break;
                        case CONNECTION_TIMEOUT:
                            LOG.warn("Connection to Apache server " + resourceContext.getResourceDetails()
                                + " timed out, availability will be reported as " + DOWN.name());
                            break;
                        default:
                        }
                    }
                }
                availPingTime = System.currentTimeMillis() - t1;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to ping the server for availability through SNMP "
                        + getSNMPAddressString(resourceContext.getPluginConfiguration()));
                }
                available = getSNMPSession().ping();
                availPingTime = -1;
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while checking availability.", e);
            }
            available = false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Availability determined: " + (available ? UP : DOWN));
        }

        return (available) ? UP : DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
        if (useBMX)
            getBMXValues(report, schedules);
        else
            getSNMPValues(report, schedules);
    }
    private void getSNMPValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
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
                        LOG.error("No values found for MIB name [" + mibName + "].");
                        continue;
                    }

                    SNMPValue snmpValue = snmpValues.get(0);
                    boolean valueIsTimestamp = isValueTimestamp(mibName);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Collected SNMP metric [" + mibName + "], value = " + snmpValue);
                    }

                    addSnmpMetricValueToReport(report, schedule, snmpValue, valueIsTimestamp);
                } catch (SNMPException e) {
                    LOG.error("An error occurred while attempting to collect an SNMP metric.", e);
                }
            }
        }
    }
    private void getBMXValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
         Map<String,String> values = parseBMXInput(null, this.getBMXUrl());
         if (LOG.isDebugEnabled()) {
                 LOG.debug("BMX map: " + values);
         }

         for (MeasurementScheduleRequest schedule : schedules) {
             String metricName = convertStringToBMX(schedule.getName());
             if (LOG.isDebugEnabled()) {
                 LOG.debug("Collecting BMX metric [" + metricName + "]");
             }
             if (metricName.equals(SERVER_BUILT_TRAIT)) {
                 MeasurementDataTrait trait = new MeasurementDataTrait(schedule, this.binaryInfo.getBuilt());
                 report.addData(trait);
             } else if (metricName.equals("rhq_avail_ping_time")) {
                 if (availPingTime == -1)
                     continue; // Skip if we have no data
                 MeasurementDataNumeric num = new MeasurementDataNumeric(schedule, (double) availPingTime);
                 report.addData(num);
             } else if (values.containsKey(metricName)) {
                 if (schedule.getDataType()== DataType.TRAIT) {
                    String val = values.get(metricName);
                    MeasurementDataTrait mdt = new MeasurementDataTrait(schedule,val);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Collected BMX metric [" + metricName + "], value = " + val);
                    }
                    report.addData(mdt);
                 } else {
                    String s = values.get(metricName);
                    if (s.endsWith("u"))
                        s = s.substring(0,s.length()-1);
                    Double val = Double.valueOf(s);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Collected BMX metric [" + metricName + "], value = " + val);
                    }
                    MeasurementDataNumeric mdn = new MeasurementDataNumeric(schedule,val);
                    report.addData(mdn);
                 }
             } else {
                 LOG.warn("BMX metric [" + metricName + "] not found");
             }
         }
    }

    /* Convert the snmp name into the BMX one */
    public static String convertStringToBMX(String string) {
        if (string.equals("wwwServiceName"))
            return "global:ServerName";
        if (string.equals("wwwServiceStartTime"))
            return "global:RestartTime";
        if (string.equals("applInboundAssociations"))
            return "global:BusyWorkers";
        if (string.startsWith("wwwSummary")) {
            return "forever:" + string.substring(10);
        } else if (string.startsWith("wwwRequest")) {
            int index =  string.indexOf('.');
            return "restart:" + string.substring(10, index) +  string.substring(index+1);
        } else if (string.startsWith("wwwResponse")) {
            int index =  string.indexOf('.');
            return "restart:" + string.substring(11, index) +  string.substring(index+1);
        }
        return string;
    }
    
    public static Map<String, String> parseBMXInput(String vHost, String bmxUrl) throws Exception {
        Map<String,String> ret = new HashMap<String, String>();
    	// TODO do some clever caching of data here, so that we won't hammer mod_bmx
        URL url = new URL(bmxUrl);
        URLConnection conn = url.openConnection();
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;

        try {
            while ((line = reader.readLine())!=null) {

                if (!line.startsWith("Name: mod_bmx_"))
                    continue;

                // Skip over sample data - this is no real module
                if (line.contains("mod_bmx_example"))
                    continue;

                // Now we have a modules output

                // check for the status module
                if (line.contains("mod_bmx_status")) {
                    slurpSection(ret,reader,"global");
                    continue;
                }


                // If the section does not match our vhost, ignore it.
                // RHQ will do 3 kinds of vHost:
                // null = Guessing Host=_GLOBAL_
                // MainServer = ignore the Host and use Port=_ANY_
                // |*:6666 = ignore the Host and use Port=6666
                // neo4|*:7777 = Use the Host and ignore the Port.
                if (vHost == null) {
                    if (!line.contains("Host=_GLOBAL_,"))
                        continue;
                } else if (vHost.startsWith("|")) {
                    if (line.contains("Host=_GLOBAL_,"))
                        continue;
                    String port = vHost.substring(vHost.indexOf(':')+1);
                    if (!line.endsWith("Port=" + port))
                        continue;
                } else if (vHost.equals("MainServer")) {
                    if (!line.contains("Type=forever") && !line.contains("Host=_GLOBAL_,") && !line.endsWith("Port=_ANY_"))
                        continue;
                } else {
                    if (line.contains("Host=_GLOBAL_,"))
                        continue;
                    String host = vHost.substring(0,vHost.indexOf('|'));
                    if (!line.contains("Host=" + host + ","))
                        continue;
                }

                // Now some global data
                Matcher m = typePattern.matcher(line);

                if (m.matches()) {
                    String type = m.group(1);
                    if (type.contains("-"))
                        type= type.substring(type.indexOf("-")+1);

                    slurpSection(ret, reader, type);
                }
            }
        } catch (Exception e) {
                 LOG.warn("parseBMXInput failed" + e);
                 throw e;
        } finally {
            try {
                 in.close();
            } catch (Exception e) {
                 // Ignore it.
            }
        }

        return ret;
    }
    
    private static void slurpSection(Map<String, String> ret, BufferedReader reader, String type) throws IOException {
        String line;
        while (!(line = reader.readLine()).equals("")) {
            int pos = line.indexOf(":");
            String key = line.substring(0,pos);
            String val = line.substring(pos+2);
            ret.put(type + ":" + key , val);
        }
    }    

    private boolean isValueTimestamp(String mibName) {
        return (mibName.equals("wwwServiceStartTime"));
    }

    @Nullable
    public OperationResult invokeOperation(@NotNull
    String name, @NotNull
    Configuration params) throws Exception {
        LOG.info("Invoking operation [" + name + "] on server [" + this.resourceContext.getResourceKey() + "]...");
        return this.operationsDelegate.invokeOperation(name, params);
    }

    public Configuration loadResourceConfiguration() throws Exception {

        // BZ 858813 - treat Augeas disabled as configuration disabled and just return null, otherwise
        // we spam the log.
        if (!isAugeasEnabled()) {
            LOG.debug(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            return null;
        }

        AugeasComponent comp = getAugeas();
        try {
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();

            AugeasTree tree = comp.getAugeasTree(AUGEAS_HTTP_MODULE_NAME);
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            return mapping.updateConfiguration(tree.getRootNode(), resourceConfigDef);
        } catch (Exception e) {
            LOG.error("Failed to load Apache configuration.", e);
            throw e;
        } finally {
            comp.close();
        }
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        if (!isAugeasEnabled()) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(ApacheServerComponent.CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            return;
        }

        AugeasComponent comp = getAugeas();

        Configuration originalConfig = report.getConfiguration().deepCopy(true);
        AugeasTree tree = null;
        try {
            tree = comp.getAugeasTree(AUGEAS_HTTP_MODULE_NAME);
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);

            mapping.updateAugeas(tree.getRootNode(), report.getConfiguration(), resourceConfigDef);
            tree.save();

            LOG.info("Apache configuration was updated");
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);

            finishConfigurationUpdate(report);
        } catch (Exception e) {
            if (tree != null) {
                LOG.error("Augeas failed to save configuration " + tree.summarizeAugeasError());
                e = new AugeasException("Failed to save configuration: " + tree.summarizeAugeasError() + " ", e);
            } else
                LOG.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessageFromThrowable(e);
            if (!originalConfig.equals(report.getConfiguration())) {
                LOG.error("Configuration has changed");
            } else {
                LOG.error("Configuratio has not changed");
            }
        } finally {
            comp.close();
        }
    }

    public AugeasComponent getAugeas() throws AugeasTreeException {
        return new AugeasComponent() {

            @Override
            public AugeasConfiguration initConfiguration() {
                File tempDir = resourceContext.getDataDirectory();
                if (!tempDir.exists())
                    throw new RuntimeException("Loading of lens failed");
                AugeasConfigurationApache config = new AugeasConfigurationApache(tempDir.getAbsolutePath(),
                    resourceContext.getPluginConfiguration());
                return config;
            }

            @Override
            public AugeasTreeBuilder initTreeBuilder() {
                AugeasTreeBuilderApache builder = new AugeasTreeBuilderApache();
                return builder;
            }

        };
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        if (!isAugeasEnabled()) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            return report;
        }

        if (ApacheVirtualHostServiceComponent.RESOURCE_TYPE_NAME.equals(report.getResourceType().getName())) {
            Configuration vhostResourceConfig = report.getResourceConfiguration();
            ConfigurationDefinition vhostResourceConfigDef = report.getResourceType()
                .getResourceConfigurationDefinition();
            Configuration vhostPluginConfig = report.getPluginConfiguration();

            String vhostDef = report.getUserSpecifiedResourceName();
            String serverName = vhostResourceConfig.getSimpleValue(
                ApacheVirtualHostServiceComponent.SERVER_NAME_CONFIG_PROP, null);

            String[] vhostDefs = vhostDef.split(" ");

            HttpdAddressUtility.Address addr;
            try {
                ApacheDirectiveTree parserTree = parseRuntimeConfiguration(true);

                Pattern virtualHostPattern = Pattern.compile(".+:([\\d]+|\\*)");
                Matcher matcher = virtualHostPattern.matcher(vhostDefs[0]);
                if (!matcher.matches())
                    throw new Exception("Wrong format of virtual host resource name. The right format is Address:Port.");

                addr = getAddressUtility().getVirtualHostSampleAddress(parserTree, vhostDefs[0], serverName, false);
            } catch (Exception e) {
                report.setStatus(CreateResourceStatus.FAILURE);
                report.setErrorMessage("Wrong format of virtual host resource name.");
                report.setException(e);
                return report;
            }

            String resourceKey = vhostDef;
            String resourceName;

            if (serverName != null) {
                resourceKey = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(serverName, Arrays.asList(vhostDefs));
                resourceName = resourceKey;
            } else {
                resourceName = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(addr.host + ":" + addr.port, Arrays.asList(vhostDefs));
            }

            report.setResourceKey(resourceKey);
            report.setResourceName(resourceName);

            AugeasComponent comp = getAugeas();
            //determine the resource name

            AugeasTree tree;
            try {

                tree = comp.getAugeasTree(AUGEAS_HTTP_MODULE_NAME);
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

                String vhostFile = comp.getConfiguration().getModules().get(0).getConfigFiles().get(0);

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
                        AugeasConfigurationApache config = (AugeasConfigurationApache) comp.getConfiguration();
                        AugeasModuleConfig moduleConfig = config.getModuleByName(config.getAugeasModuleName());
                        boolean willPersist = false;
                        for (String glob : moduleConfig.getIncludedGlobs()) {
                            if (Glob.matches(getServerRoot(), glob, vhostFileFile)) {
                                willPersist = true;
                                break;
                            }
                        }

                        if (!willPersist) {
                            //the file wouldn't be loaded by augeas
                            moduleConfig.addIncludedGlob(vhostFile);
                            //this also means that there was no include
                            //that would load the file, so we have to
                            //add the include directive to the main conf.
                            List<AugeasNode> includes = tree.matchRelative(tree.getRootNode(), "Include");
                            AugeasNode include = tree.createNode(tree.getRootNode(), "Include", null,
                                includes.size() + 1);
                            tree.createNode(include, "param", vhostFile, 0);
                            tree.save();
                        }

                        try {
                            vhostFileFile.createNewFile();
                        } catch (IOException e) {
                            LOG.error("Failed to create a new vhost file: " + vhostFile, e);
                        }

                        comp.close();
                        comp = getAugeas();
                        tree = comp.getAugeasTree(moduleConfig.getModuletName());

                        vhost = tree.createNode(AugeasTree.AUGEAS_DATA_PATH + vhostFile + "/<VirtualHost");
                        ((ApacheAugeasNode) vhost).setParentNode(tree.getRootNode());

                    }
                }

                if (vhost == null) {
                    report.setStatus(CreateResourceStatus.FAILURE);
                } else {
                    try {
                        for (int i = 0; i < vhostDefs.length; ++i) {
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
            } finally {
                if (comp != null)
                    comp.close();
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
            String timeoutString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_SNMP_REQUEST_TIMEOUT, null);
            long timeout = (timeoutString != null) ? Long.parseLong(timeoutString) : DEFAULT_SNMP_REQUEST_TIMEOUT;
            String retriesString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_SNMP_REQUEST_RETRIES, null);
            int retries = (retriesString != null) ? Integer.parseInt(retriesString) : DEFAULT_SNMP_REQUEST_RETRIES;
            snmpSession = snmpClient.getSession(host, port, community, SNMPClient.SNMPVersion.V2C, timeout, retries);
        } catch (SNMPException e) {
            throw new Exception("Error getting SNMP session: " + e.getMessage(), e);
        }

        return snmpSession;
    }

    private static String getSNMPAddressString(Configuration pluginConfig) {
        String host = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST).getStringValue();
        String portString = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT).getStringValue();
        String community = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_SNMP_AGENT_COMMUNITY).getStringValue();

        return host + ":" + portString + "/" + community;
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
            String serverRoot = null;

            ApacheDirectiveTree tree = parseRuntimeConfiguration(true);
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
    public @Nullable
    String getServerUrl() {
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
                ApacheDirectiveTree tree = parseRuntimeConfiguration(true);
                List<ApacheDirective> directives = tree.search("/ServerRoot");
                if (!directives.isEmpty())
                    if (!directives.get(0).getValues().isEmpty())
                        serverRoot = directives.get(0).getValues().get(0);

            } catch (Exception e) {
                LOG.error("Could not load configuration parser.", e);
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
            if (!found
                && resourceContext.getSystemInformation().getOperatingSystemType() != OperatingSystemType.WINDOWS) {
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
        AugeasConfigurationApache config = new AugeasConfigurationApache(resourceContext.getTemporaryDirectory()
            .getAbsolutePath(), resourceContext.getPluginConfiguration());
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

    public Map<String, String> getModuleNames() {
        return moduleNames;
    }

    public ProcessInfo getCurrentProcessInfo() {
        return resourceContext.getNativeProcess();
    }

    public ApacheBinaryInfo getCurrentBinaryInfo() {
        return binaryInfo;
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
    private File resolvePathRelativeToServerRoot(@NotNull
    String path) {
        return resolvePathRelativeToServerRoot(this.resourceContext.getPluginConfiguration(), path);
    }

    //TODO this needs to go...
    @NotNull
    static File resolvePathRelativeToServerRoot(Configuration pluginConfig, @NotNull
    String path) {
        File file = new File(path);
        if (!FileUtil.isAbsolutePath(path)) {
            String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
            file = new File(serverRoot, path);
        }

        // BZ 903402 - get the real absolute path - under most conditions, it's the same thing, but if on windows
        //             the drive letter might not have been specified - this makes sure the drive letter is specified.
        return file.getAbsoluteFile();
    }

    @NotNull
    static String getRequiredPropertyValue(@NotNull
    Configuration config, @NotNull
    String propName) {
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
        String version = getVersion();
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

    public ApacheDirectiveTree parseFullConfiguration() {
        String httpdConfPath = getHttpdConfFile().getAbsolutePath();
        return ApacheServerDiscoveryComponent.parseFullConfiguration(httpdConfPath, binaryInfo.getRoot());
    }

    public ApacheDirectiveTree parseRuntimeConfiguration(boolean suppressUnknownModuleWarnings) {
        String httpdConfPath = getHttpdConfFile().getAbsolutePath();
        ProcessInfo processInfo = resourceContext.getNativeProcess();

        return ApacheServerDiscoveryComponent.parseRuntimeConfiguration(httpdConfPath, processInfo, binaryInfo,
            getModuleNames(), suppressUnknownModuleWarnings);
    }

    public boolean isAugeasEnabled() {

        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertySimple prop = pluginConfig.getSimple(AUGEAS_ENABLED);
        if (prop == null || prop.getStringValue() == null) {
            return false;
        }

        String val = prop.getStringValue();

        if (val.equals("yes")) {
            Augeas ag = null;
            try {
                ag = new Augeas();
            } catch (Exception e) {
                logAugeasError(e);
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            } catch (NoClassDefFoundError e) {
                logAugeasError(e);
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            } catch (UnsatisfiedLinkError e) {
                logAugeasError(e);
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            } finally {
                if (ag != null) {
                    try {
                        ag.close();
                    } catch (Exception e) {
                    }
                    ag = null;
                }
            }
            String version = getVersion();

            if (!version.startsWith("2.")) {
                if (!augeasErrorLogged) {
                    augeasErrorLogged = true;
                    LOG.error("Augeas is only supported with Apache version 2.x but version '" + version
                        + "' was detected.");
                }
                throw new RuntimeException(CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
            }
            return true;
        } else {
            return false;
        }
    }

    private void logAugeasError(Throwable cause) {
        if (!augeasErrorLogged) {
            LOG.error("Augeas is enabled in configuration but was not found on the system.", cause);
            augeasErrorLogged = true;
        }
    }

    private String getVersion() {
        String ret = resourceContext.getVersion();
        if (ret == null) {
            //strange, but this happens sometimes when
            //the resource is synced with the server for the first
            //time after data purge on the agent side

            //let's determine the version from the binary info
            ret = binaryInfo.getVersion();
        }

        return ret;
    }

    ResourceContext getResourceContext() {
        return this.resourceContext;
    }

}
