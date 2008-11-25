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
import java.net.MalformedURLException;
import java.net.URL;
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
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.event.log.LogFileEventPoller;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.www.snmp.SNMPClient;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;
import org.rhq.plugins.www.util.WWWUtils;

/**
 * The resource component for Apache 1.3/2.x servers.
 *
 * @author Ian Springer
 */
public class ApacheServerComponent implements ResourceComponent, MeasurementFacet, OperationFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    public static final String PLUGIN_CONFIG_PROP_SERVER_ROOT = "serverRoot";
    public static final String PLUGIN_CONFIG_PROP_EXECUTABLE_PATH = "executablePath";
    public static final String PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH = "controlScriptPath";
    public static final String PLUGIN_CONFIG_PROP_URL = "url";

    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST = "snmpAgentHost";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT = "snmpAgentPort";
    public static final String PLUGIN_CONFIG_PROP_SNMP_AGENT_COMMUNITY = "snmpAgentCommunity";

    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH = "errorLogFilePath";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_EVENTS_ENABLED = "errorLogEventsEnabled";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_MINIMUM_SEVERITY = "errorLogMinimumSeverity";
    public static final String PLUGIN_CONFIG_PROP_ERROR_LOG_INCLUDES_PATTERN = "errorLogIncludesPattern";

    public static final String SERVER_BUILT_TRAIT = "serverBuilt";

    public static final String DEFAULT_EXECUTABLE_PATH = "bin" + File.separator
        + ((File.separatorChar == '/') ? "httpd" : "Apache.exe");

    public static final String DEFAULT_ERROR_LOG_PATH = "logs" + File.separator
        + ((File.separatorChar == '/') ? "error_log" : "error.log");

    private static final String ERROR_LOG_ENTRY_EVENT_TYPE = "errorLogEntry";

    private ResourceContext resourceContext;
    private EventContext eventContext;
    private SNMPClient snmpClient;
    private URL url;
    private ApacheBinaryInfo binaryInfo;

    /**
     * Delegate instance for handling all calls to invoke operations on this component.
     */
    private ApacheServerOperationsDelegate operationsDelegate;

    public void start(ResourceContext resourceContext) throws Exception {
        log.info("Initializing server component for server [" + resourceContext.getResourceKey() + "]...");
        this.resourceContext = resourceContext;
        this.eventContext = resourceContext.getEventContext();
        this.snmpClient = new SNMPClient();
        boolean configured = false;

        SNMPSession snmpSession = getSNMPSession();
        if (!snmpSession.ping()) {
            log.warn(
                "Failed to connect to SNMP agent at "
                    + snmpSession
                    + ". Make sure 1) the managed Apache server has been instrumented with the JON SNMP module, 2) the Apache server is running, and 3) the SNMP agent host, port, and community are set correctly in this resource's connection properties.");
        }
        else
            configured = true;

        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String url = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_URL, null);
        if (url != null) {
            try {
                this.url = new URL(url);
                if (this.url.getPort() == 0) {
                    log.error(
                        "The 'url' connection property is invalid - 0 is not a valid port; please change the value to the " +
                        "port the \"main\" Apache server is listening on. NOTE: If the 'url' property was set this way " +
                        "after autodiscovery, you most likely did not include the port in the ServerName directive for " +
                        "the \"main\" Apache server in httpd.conf.");
                }
                else
                    configured = true;
            } catch (MalformedURLException e) {
                throw new InvalidPluginConfigurationException("Value of '" + PLUGIN_CONFIG_PROP_URL
                    + "' connection property ('" + url + "') is not a valid URL.");
            }
        }

        if (!configured) {
            throw new InvalidPluginConfigurationException("Neither SNMP nor an URL for checking availability has been configured");
        }

        File executablePath = getExecutablePath();
        try {
            this.binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(), this.resourceContext
                .getSystemInformation());
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("'" + executablePath + "' is not a valid Apache executable ("
                + e + ").");
        }

        this.operationsDelegate = new ApacheServerOperationsDelegate(this, this.resourceContext.getSystemInformation());

        startEventPollers();
    }

    public void stop() {
        stopEventPollers();
    }

    public AvailabilityType getAvailability() {
        // TODO: If URL is not set, rather than falling back to pinging the SNMP agent,
        //       try to find a pid file under the server root, and then check if the
        //       process is running.
        boolean available;
        try {
            available = (this.url != null) ? WWWUtils.isAvailable(this.url) : getSNMPSession().ping();
        } catch (Exception e) {
            available = false;
        }

        return (available) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
        SNMPSession snmpSession = getSNMPSession();
        if (!snmpSession.ping()) {
            throw new Exception("Failed to connect to SNMP agent at " + snmpSession
                + " - aborting metric collection...");
        }

        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            if (metricName.equals(SERVER_BUILT_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(schedule, this.binaryInfo.getBuilt());
                report.addData(trait);
            } else {
                // Assume anything else is an SNMP metric.
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

    public void startOperationFacet(OperationContext context) {
    }

    @Nullable
    public OperationResult invokeOperation(@NotNull
    String name, @NotNull
    Configuration params) throws Exception {
        log.info("Invoking operation [" + name + "] on server [" + this.resourceContext.getResourceKey() + "]...");
        return this.operationsDelegate.invokeOperation(name, params);
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
            String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) // UNIX
            {
                if (this.binaryInfo.getVersion().startsWith("2.")) {
                    executableFile = new File(serverRoot, "bin/apache2");
                } else {
                    executableFile = new File(serverRoot, "bin/httpd");
                }
            } else // Windows
            {
                executableFile = new File(serverRoot, "bin/Apache.exe");
            }
        }

        return executableFile;
    }

    /**
     * Return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     * Group\Apache2\bin\Apache.exe").
     *
     * @return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     *         Group\Apache2\bin\Apache.exe")
     */
    @NotNull
    public File getControlScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String controlScriptPath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH, null);
        File controlScriptFile;
        if (controlScriptPath != null) {
            controlScriptFile = resolvePathRelativeToServerRoot(controlScriptPath);
        } else {
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) // UNIX
            {
                String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
                if (this.binaryInfo.getVersion().startsWith("2.")) {
                    controlScriptFile = new File(serverRoot, "bin/apache2ctl");
                } else {
                    controlScriptFile = new File(serverRoot, "bin/apachectl");
                }
            } else // Windows
            {
                controlScriptFile = getExecutablePath();
            }
        }

        return controlScriptFile;
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

    @NotNull
    static File resolvePathRelativeToServerRoot(Configuration pluginConfig, @NotNull
    String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
            file = new File(serverRoot, path);
        }

        return file;
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
        Boolean enabled = Boolean.valueOf(pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_EVENTS_ENABLED, null));
        if (enabled) {
            File errorLogFile = resolvePathRelativeToServerRoot(pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH, DEFAULT_ERROR_LOG_PATH));
            ApacheErrorLogEntryProcessor processor = new ApacheErrorLogEntryProcessor(ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile);
            String includesPatternString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_INCLUDES_PATTERN, null);
            if (includesPatternString != null) {
                try {
                    Pattern includesPattern = Pattern.compile(includesPatternString);
                    processor.setIncludesPattern(includesPattern);
                } catch (PatternSyntaxException e) {
                    throw new InvalidPluginConfigurationException("Includes pattern [" + includesPatternString + "] is not a valid regular expression.");
                }
            }
            String minimumSeverityString = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_MINIMUM_SEVERITY, null);
            if (minimumSeverityString != null) {
                EventSeverity minimumSeverity = EventSeverity.valueOf(minimumSeverityString.toUpperCase());
                processor.setMinimumSeverity(minimumSeverity);
            }
            EventPoller poller = new LogFileEventPoller(this.eventContext, ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile, processor);
            this.eventContext.registerEventPoller(poller, 60, errorLogFile.getPath());
        }
    }

    private void stopEventPollers() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        File errorLogFile = resolvePathRelativeToServerRoot(pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_ERROR_LOG_FILE_PATH, DEFAULT_ERROR_LOG_PATH));
        this.eventContext.unregisterEventPoller(ERROR_LOG_ENTRY_EVENT_TYPE, errorLogFile.getPath());
    }
}
