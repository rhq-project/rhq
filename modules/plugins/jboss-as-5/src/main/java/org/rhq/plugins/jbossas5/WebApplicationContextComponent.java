/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.RegularExpressionNameMatcher;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

/**
 * A Resource component for web application contexts (e.g. "//localhost/jmx-console").
 *
 * @author Ian Springer
 */
public class WebApplicationContextComponent extends ManagedComponentComponent {
    public static final String VIRTUAL_HOST_PROPERTY = "virtualHost";
    public static final String CONTEXT_PATH_PROPERTY = "contextPath";

    private static final String RESPONSE_TIME_METRIC = "responseTime";
    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = "responseTimeLogFile";

    private static final String VIRTUAL_HOST_TRAIT = "virtualHost";

    private static final String CLUSTERED_TRAIT = "clustered";
    private static final String DISTRIBUTABLE_MANAGED_PROPERTY = "Distributable";

    // A regex for the names of all MBean:Servlet components for a WAR.
    private static final String SERVLET_COMPONENT_NAMES_REGEX_TEMPLATE = "jboss.web:J2EEApplication=none,J2EEServer=none,"
        + "WebModule=//%" + VIRTUAL_HOST_PROPERTY + "%" + "%" + CONTEXT_PATH_PROPERTY + "%,j2eeType=Servlet,name=[^,]+";

    private static final String SERVLET_METRIC_PREFIX = "Servlet.";

    private static final String SERVLET_MAXIMUM_RESPONSE_TIME_METRIC = "Servlet.maximumResponseTime";
    private static final String SERVLET_MINIMUM_RESPONSE_TIME_METRIC = "Servlet.minimumResponseTime";
    private static final String SERVLET_AVERAGE_RESPONSE_TIME_METRIC = "Servlet.averageResponseTime";
    private static final String SERVLET_TOTAL_RESPONSE_TIME_METRIC = "Servlet.totalResponseTime";
    private static final String SERVLET_REQUEST_COUNT_METRIC = "Servlet.requestCount";
    private static final String SERVLET_ERROR_COUNT_METRIC = "Servlet.errorCount";

    private final Log log = LogFactory.getLog(this.getClass());

    private String servletComponentNamesRegex;
    private ResponseTimeLogParser logParser;

    private Boolean clustered;

    @Override
    public void start(ResourceContext<ProfileServiceComponent<?>> resourceContext) throws Exception {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.servletComponentNamesRegex = ResourceComponentUtils.replacePropertyExpressionsInTemplate(
            SERVLET_COMPONENT_NAMES_REGEX_TEMPLATE, pluginConfig);
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile != null) {
            this.logParser = new ResponseTimeLogParser(logFile);
            this.logParser.setExcludes(responseTimeConfig.getExcludes());
            this.logParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        ProfileServiceComponent warComponent = getResourceContext().getParentResourceComponent();
        ManagementView managementView = warComponent.getConnection().getManagementView();

        ManagedComponent component = getManagedComponent();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try {
                if (metricName.startsWith(SERVLET_METRIC_PREFIX)) {
                    Double value = getServletMetric(managementView, metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(request, value);
                    report.addData(metric);
                } else if (metricName.equals(VIRTUAL_HOST_TRAIT)) {
                    Configuration pluginConfig = getResourceContext().getPluginConfiguration();
                    String virtualHost = pluginConfig.getSimple(VIRTUAL_HOST_PROPERTY).getStringValue();
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, virtualHost);
                    report.addData(trait);
                } else if (metricName.equals(RESPONSE_TIME_METRIC)) {
                    if (this.logParser != null) {
                        try {
                            CallTimeData callTimeData = new CallTimeData(request);
                            this.logParser.parseLog(callTimeData);
                            report.addData(callTimeData);
                        } catch (Exception e) {
                            log.error("Failed to retrieve HTTP call-time data.", e);
                        }
                    } else {
                        log.error("The '" + RESPONSE_TIME_METRIC + "' metric is enabled for WAR resource '"
                            + getComponentName() + "', but no value is defined for the '"
                            + RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                        // TODO: Communicate this error back to the server for display in the GUI.
                    }
                } else if (metricName.equals(CLUSTERED_TRAIT)) {
                    if(clustered == null){
                        retrieveClusteredProperty();
                    }

                    if (clustered != null) {
                        MeasurementDataTrait trait = new MeasurementDataTrait(request, clustered.toString());
                        report.addData(trait);
                    }
                } else {
                    String value = null;

                    /*
                     * Bug 737996 - Working around the property names inconsistency in the clustered web app contexts.
                     * If clustered we need to capitalize the first letter of the metric name to workaround JBPAPP-7172.
                     * If null is returned then we can assume that JBPAPP-7172 does not apply here and use the expected 
                     * metric name.
                     */
                    if (clustered == null) {
                        retrieveClusteredProperty();
                    }

                    if (clustered != null && clustered && !"runState".equals(metricName)) {
                        value = getMeasurement(component, toUpperCaseFirstLetter(metricName));
                    }

                    // value should be null unless app is clustered and JBPAPP-7172 applies
                    if (value == null) {
                        value = getMeasurement(component, metricName);
                    }

                    addValueToMeasurementReport(report, request, value);
                }
            } catch (Exception e) {
                // Don't let one bad apple spoil the barrel.
                log.error("Failed to collect metric '" + metricName + "' for " + getResourceContext().getResourceType()
                    + " Resource with key " + getResourceContext().getResourceKey() + ".", e);
            }
        }
    }

    /**
     * @deprecated The clustered property should be retrieved by the individual component in the
     * specific use case that requires it. Leaving this method for backwards compatibility
     * with plugins that use the AS5 plugin and also use reflection.
     */
    @Deprecated
    public boolean isClustered() {
        if (clustered == null) {
            return false;
        }

        return clustered;
    }

    private void retrieveClusteredProperty() {
        try {
            ManagedProperty distributableProp = getManagedComponent().getProperty(DISTRIBUTABLE_MANAGED_PROPERTY);
            if (distributableProp != null) {
                Boolean distributable = (Boolean) getInnerValue(distributableProp.getValue());
                clustered = distributable != null && distributable.booleanValue();
            }
        } catch (Exception e) {
            log.warn("Failed to determine whether the web app context " + this.getResourceContext().getResourceKey()
                + " is clustered or not.", e);
        }
    }

    private Double getServletMetric(ManagementView managementView, String metricName) throws Exception {
        ComponentType servletComponentType = MoreKnownComponentTypes.MBean.Servlet.getType();
        //Set<ManagedComponent> servletComponents = managementView.getMatchingComponents(this.servletComponentNamesRegex,
        //        servletComponentType, new RegularExpressionNameMatcher());
        Set<ManagedComponent> servletComponents = ManagedComponentUtils.getManagedComponents(managementView,
            servletComponentType, this.servletComponentNamesRegex, new RegularExpressionNameMatcher());

        long min = Long.MAX_VALUE;
        long max = 0;
        long processingTime = 0;
        int requestCount = 0;
        int errorCount = 0;
        for (ManagedComponent servletComponent : servletComponents) {
            if (metricName.equals(SERVLET_MINIMUM_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long) ManagedComponentUtils.getSimplePropertyValue(servletComponent, "minTime");
                if (longValue < min)
                    min = longValue;
            } else if (metricName.equals(SERVLET_MAXIMUM_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long) ManagedComponentUtils.getSimplePropertyValue(servletComponent, "maxTime");
                if (longValue > max)
                    max = longValue;
            } else if (metricName.equals(SERVLET_AVERAGE_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long) ManagedComponentUtils
                    .getSimplePropertyValue(servletComponent, "processingTime");
                processingTime += longValue;
                Integer intValue = (Integer) ManagedComponentUtils.getSimplePropertyValue(servletComponent,
                    "requestCount");
                requestCount += intValue;
            } else if (metricName.equals(SERVLET_REQUEST_COUNT_METRIC)) {
                Integer intValue = (Integer) ManagedComponentUtils.getSimplePropertyValue(servletComponent,
                    "requestCount");
                requestCount += intValue;
            } else if (metricName.equals(SERVLET_ERROR_COUNT_METRIC)) {
                Integer intValue = (Integer) ManagedComponentUtils.getSimplePropertyValue(servletComponent,
                    "errorCount");
                errorCount += intValue;
            } else if (metricName.equals(SERVLET_TOTAL_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long) ManagedComponentUtils
                    .getSimplePropertyValue(servletComponent, "processingTime");
                processingTime += longValue;
            }
        }

        Double result;
        if (metricName.equals(SERVLET_AVERAGE_RESPONSE_TIME_METRIC)) {
            result = (requestCount > 0) ? ((double) processingTime / (double) requestCount) : Double.NaN;
        } else if (metricName.equals(SERVLET_MINIMUM_RESPONSE_TIME_METRIC)) {
            result = (min != Long.MAX_VALUE) ? (double) min : Double.NaN;
        } else if (metricName.equals(SERVLET_MAXIMUM_RESPONSE_TIME_METRIC)) {
            result = (max != 0) ? (double) max : Double.NaN;
        } else if (metricName.equals(SERVLET_ERROR_COUNT_METRIC)) {
            result = (double) errorCount;
        } else if (metricName.equals(SERVLET_REQUEST_COUNT_METRIC)) {
            result = (double) requestCount;
        } else if (metricName.equals(SERVLET_TOTAL_RESPONSE_TIME_METRIC)) {
            result = (double) processingTime;
        } else {
            // fallback
            result = Double.NaN;
        }

        return result;
    }

    private static String toUpperCaseFirstLetter(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        char first = name.charAt(0);

        return Character.toUpperCase(first) + name.substring(1);
    }
}
