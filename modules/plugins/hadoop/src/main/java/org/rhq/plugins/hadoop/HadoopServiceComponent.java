/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.log.Log4JLogEntryProcessor;
import org.rhq.core.pluginapi.event.log.LogFileEventPoller;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

public class HadoopServiceComponent extends JMXServerComponent<ResourceComponent<?>> implements
    JMXComponent<ResourceComponent<?>>, MeasurementFacet, OperationFacet, ConfigurationFacet {

    private static final Log LOG = LogFactory.getLog(HadoopServiceComponent.class);
    
    private static final String LOG_EVENT_TYPE = "logEntry";
    private static final String LOG_POLLING_INTERVAL_PROPERTY = "logPollingInterval";
    
    private HadoopServerConfigurationDelegate configurationDelegate;
    
    private HadoopOperationsDelegate operationsDelegate;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void start(ResourceContext context) throws Exception {
        super.start(context);
        configurationDelegate = new HadoopServerConfigurationDelegate(context);
        this.operationsDelegate = new HadoopOperationsDelegate(context);

        EventContext events = context.getEventContext();
        if (events != null) {
            File logFile = determineLogFile();
            int interval = Integer.parseInt(context.getPluginConfiguration().getSimpleValue(LOG_POLLING_INTERVAL_PROPERTY, "60"));                        
            events.registerEventPoller(new LogFileEventPoller(events, LOG_EVENT_TYPE, logFile, new Log4JLogEntryProcessor(LOG_EVENT_TYPE, logFile)), interval);
        }
    }
    
    @Override
    public void stop() {
        EventContext events = getResourceContext().getEventContext();
        if (events != null) {
            events.unregisterEventPoller(LOG_EVENT_TYPE);
        }
        super.stop();
    }
    
    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        return getResourceContext().getNativeProcess().isRunning() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public EmsConnection getEmsConnection() {
        EmsConnection conn = super.getEmsConnection();
        if (LOG.isTraceEnabled()) {
            LOG.trace("EmsConnection is " + conn.toString());
        }
        return conn;

    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();
            int delimIndex = name.lastIndexOf(':');
            String beanName = name.substring(0, delimIndex);
            String attributeName = name.substring(delimIndex + 1);
            try {
                EmsConnection emsConnection = getEmsConnection();
                EmsBean bean = emsConnection.getBean(beanName);
                if (bean != null) {
                    bean.refreshAttributes();
                    EmsAttribute attribute = bean.getAttribute(attributeName);
                    if (attribute != null) {
                        Object valueObject = attribute.refresh();
                        if (valueObject instanceof Number) {
                            Number value = (Number) valueObject;
                            report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
                        } else {
                            report.addData(new MeasurementDataTrait(request, valueObject.toString()));
                        }
                    } else {
                        LOG.warn("Attribute " + attributeName + " not found");
                    }
                } else {
                    LOG.warn("MBean " + beanName + " not found");
                }
            } catch (Exception e) {
                LOG.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        return configurationDelegate.loadConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {    
        try {
            Configuration updatedConfiguration = report.getConfiguration();
            configurationDelegate.updateConfiguration(updatedConfiguration);
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            report.setErrorMessageFromThrowable(e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
    }

    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        HadoopSupportedOperations operation = HadoopSupportedOperations.valueOf(name.toUpperCase());
        return operationsDelegate.invoke(operation, params);
    }
    
    private File determineLogFile() {
        String username = getResourceContext().getNativeProcess().getCredentialsName().getUser();
        String hostname = getResourceContext().getSystemInformation().getHostname();
        
        String serverType = getServerType();
        
        String name = "hadoop-" + username + "-" + serverType + "-" + hostname + ".log";
                        
        return new File(new File(getHomeDir(), "logs"), name);
    }
    
    private String getServerType() {
        String mainClass = getResourceContext().getPluginConfiguration().getSimpleValue("_mainClass");
        int dot = mainClass.lastIndexOf('.');
        String className = mainClass.substring(dot + 1);
        
        return className.toLowerCase();
    }
    
    private File getHomeDir() {
        File homeDir =
            new File(getResourceContext().getPluginConfiguration().getSimpleValue(HadoopServiceDiscovery.HOME_DIR_PROPERTY));

        if (!homeDir.exists()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") no longer exists.");
        }

        if (!homeDir.isDirectory()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not a directory.");
        }

        if (!homeDir.canRead()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not readable.");
        }

        return homeDir;
    }    
}
