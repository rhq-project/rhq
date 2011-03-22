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
package org.rhq.plugins.agent;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.management.MBeanException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsInvocationException;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;
import org.rhq.enterprise.agent.AgentManagement;
import org.rhq.enterprise.agent.AgentManagementMBean;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * The component that represents the Agent itself.
 *
 * @author John Mazzitelli
 */
public class AgentServerComponent extends JMXServerComponent implements JMXComponent, MeasurementFacet, OperationFacet,
    ConfigurationFacet, SupportFacet {
    private static Log log = LogFactory.getLog(AgentServerComponent.class);

    /**
     * This is a substring that starts all trait measurement property names.
     */
    private static final String TRAIT_INDICATOR = "Trait.";

    private static final String SIGAR_VERSION_METRIC_SUFFIX = "SigarVersion";
    private static final String REASON_FOR_LAST_RESTART_METRIC_SUFFIX = "ReasonForLastRestart";
    private static final String AGENT_HOME_DIRECTORY_METRIC_SUFFIX = "AgentHomeDirectory";

    private String sigarVersion;

    // needed for log tracking
    private LogFileEventResourceComponentHelper logFileEventDelegate;

    @Override
    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
        this.sigarVersion = SystemInfoFactory.getNativeSystemInfoVersion();
        this.logFileEventDelegate = new LogFileEventResourceComponentHelper(resourceContext);
        this.logFileEventDelegate.startLogFileEventPollers();
    }

    @Override
    public void stop() {
        this.logFileEventDelegate.stopLogFileEventPollers();
        super.stop();
    }

    @Override
    public AvailabilityType getAvailability() {
        // this one is simple - if this plugin is running, then we automatically know the agent is up!
        return AvailabilityType.UP;
    }

    protected EmsBean getAgentBean() {
        return getEmsConnection().getBean(AgentManagement.singletonObjectName.getCanonicalName());
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        for (MeasurementScheduleRequest metric_request : metrics) {
            String metric_name = metric_request.getName();

            if (metric_name.startsWith(TRAIT_INDICATOR)) {
                try {
                    if (metric_name.endsWith(SIGAR_VERSION_METRIC_SUFFIX)) {
                        report.addData(new MeasurementDataTrait(metric_request, this.sigarVersion));
                    } else if (metric_name.endsWith(REASON_FOR_LAST_RESTART_METRIC_SUFFIX)) {
                        Object reason = getAgentBean().getAttribute(REASON_FOR_LAST_RESTART_METRIC_SUFFIX).refresh();
                        report.addData(new MeasurementDataTrait(metric_request, reason.toString()));
                    } else if (metric_name.endsWith(AGENT_HOME_DIRECTORY_METRIC_SUFFIX)) {
                        Object reason = getAgentBean().getAttribute(AGENT_HOME_DIRECTORY_METRIC_SUFFIX).refresh();
                        report.addData(new MeasurementDataTrait(metric_request, reason.toString()));
                    } else {
                        log.error("Being asked to collect an unknown trait measurement: " + metric_name);
                    }
                } catch (Exception skip) {
                    log.debug(skip);
                }
            } else {
                // I know that all current metrics are either long or int - so they can be cast to Number
                Number metric_value;
                try {
                    metric_value = (Number) getAgentBean().getAttribute(metric_name).refresh(); // This is always local, just update individual metrics
                    report.addData(new MeasurementDataNumeric(metric_request, metric_value.doubleValue()));
                } catch (Exception e) {
                    log.error("Failed to obtain metric [" + metric_name + "]. Cause: " + e);
                }
            }
        }

        return;
    }

    public OperationResult invokeOperation(String name, Configuration params) {
        OperationResult result = null;

        // I know all operation names have identical MBean operations on the agent management MBean
        // I also know about all operations that have void and non-void parameters
        try {
            if ((params == null) || (params.getProperties().size() == 0)) {
                result = (OperationResult) getAgentBean().getOperation(name).invoke();
            } else {
                if (name.equals("retrievePluginInfo")) {
                    String pluginToUpdate = params.getSimple("pluginName").getStringValue();
                    result = (OperationResult) getAgentBean().getOperation(name).invoke(pluginToUpdate);
                } else if (name.equals("executeAvailabilityScan")) {
                    Boolean changesOnly = params.getSimple("changesOnly").getBooleanValue();
                    result = (OperationResult) getAgentBean().getOperation(name).invoke(changesOnly);
                } else if (name.equals("retrieveCurrentDateTime")) {
                    String timeZone = params.getSimple("timeZone").getStringValue();
                    result = new OperationResult();
                    result.getComplexResults().put(
                        new PropertySimple("dateTime", getAgentBean().getOperation(name).invoke(timeZone)));
                } else if (name.equals("setDebugMode")) {
                    Boolean enabled = params.getSimple("enabled").getBooleanValue();
                    Boolean traceMessaging = params.getSimple("traceMessaging").getBooleanValue();
                    result = new OperationResult();
                    getAgentBean().getOperation(name).invoke(enabled, traceMessaging);
                } else if (name.equals("executePromptCommand")) {
                    String command = params.getSimple("command").getStringValue();
                    result = new OperationResult();
                    try {
                        Object output = getAgentBean().getOperation(name).invoke(command);
                        result.getComplexResults().put(new PropertySimple("output", output));
                    } catch (EmsInvocationException eie) {
                        if (eie.getCause() instanceof MBeanException
                            && eie.getCause().getCause() instanceof ExecutionException) {
                            // the prompt command threw the exception - in this case:
                            // the message is the prompt output and the cause is the actual prompt exception
                            ExecutionException ee = (ExecutionException) eie.getCause().getCause();
                            String output = ee.getMessage();
                            CharArrayWriter caw = new CharArrayWriter();
                            ee.getCause().printStackTrace(new PrintWriter(caw));
                            String error = caw.toString();
                            result.getComplexResults().put(new PropertySimple("output", output));
                            result.getComplexResults().put(new PropertySimple("error", error));
                        } else {
                            throw eie;
                        }
                    }
                } else if (name.equals("switchToServer")) {
                    String server = params.getSimpleValue("server", null);
                    getAgentBean().getOperation(name).invoke(server);
                } else {
                    // this should really never happen
                    throw new IllegalArgumentException("Operation [" + name + "] does not support params");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
        }
        return result;
    }

    public Configuration loadResourceConfiguration() {
        // the agent preferences are simple name/value pairs; just create a Configuration
        // with a bunch of simple properties, one simple property for each agent preference
        AgentManagementMBean mbean = AgentDiscoveryComponent.getAgentManagementMBean();
        Properties agent_prefs = mbean.getAgentConfiguration();
        Configuration config = new Configuration();

        for (Map.Entry<Object, Object> pref : agent_prefs.entrySet()) {
            config.put(new PropertySimple(pref.getKey().toString(), pref.getValue()));
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport request) {
        Configuration configuration = request.getConfiguration();
        Collection<String> config_names = configuration.getNames();
        Properties prefsToMerge = new Properties();
        List<String> prefsToRemove = new ArrayList<String>();

        for (String config_name : config_names) {
            String config_value = configuration.getSimple(config_name).getStringValue();

            // skip props w/ null values, since java.util.Properties does not allow null values.
            if (config_value != null) {
                prefsToMerge.setProperty(config_name, config_value);
            } else {
                prefsToRemove.add(config_name);
            }
        }

        try {
            AgentManagementMBean mbean = AgentDiscoveryComponent.getAgentManagementMBean();
            mbean.mergeIntoAgentConfiguration(prefsToMerge);
            mbean.removeFromAgentConfiguration(prefsToRemove);
            request.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }

    public SnapshotReportResults getSnapshotReport(SnapshotReportRequest request) throws Exception {
        AgentManagementMBean mbean = AgentDiscoveryComponent.getAgentManagementMBean();
        ResourceContext resourceContext = getResourceContext();
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        String installDir = mbean.getAgentHomeDirectory();
        Properties config = mbean.getAgentConfiguration();
        String tmpDir = resourceContext.getTemporaryDirectory().getAbsolutePath();
        String name = request.getName();
        String description = request.getDescription();
        AgentSnapshotReport report = new AgentSnapshotReport(name, description, pluginConfig, installDir, config,
            tmpDir);
        File reportFile = report.generate();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(reportFile));
        SnapshotReportResults results = new SnapshotReportResults(inputStream);
        return results;
    }

}