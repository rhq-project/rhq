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
package org.jboss.on.plugins.tomcat;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * A resource component for managing a web application (WAR) deployed to a Tomcat server.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class TomcatWarComponent extends MBeanResourceComponent<TomcatServerComponent> implements OperationFacet {

    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = "responseTimeLogFile";
    public static final String RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP = "responseTimeUrlExcludes";
    public static final String RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP = "responseTimeUrlTransforms";

    private static final String METRIC_PREFIX_APPLICATION = "Application.";
    private static final String METRIC_PREFIX_SERVLET = "Servlet.";
    private static final String METRIC_PREFIX_SESSION = "Session.";
    private static final String METRIC_PREFIX_VHOST = "VHost.";

    private static final String METRIC_RESPONSE_TIME = "ResponseTime";

    private static final String METRIC_MAX_SERVLET_TIME = METRIC_PREFIX_SERVLET + "MaxResponseTime";
    private static final String METRIC_MIN_SERVLET_TIME = METRIC_PREFIX_SERVLET + "MinResponseTime";
    private static final String METRIC_AVG_SERVLET_TIME = METRIC_PREFIX_SERVLET + "AvgResponseTime";
    private static final String METRIC_NUM_SERVLET_REQUESTS = METRIC_PREFIX_SERVLET + "NumRequests";
    private static final String METRIC_NUM_SERVLET_ERRORS = METRIC_PREFIX_SERVLET + "NumErrors";
    private static final String METRIC_TOTAL_TIME = METRIC_PREFIX_SERVLET + "TotalTime";

    private static final String TRAIT_EXPLODED = METRIC_PREFIX_APPLICATION + "exploded";
    private static final String TRAIT_VHOST_NAMES = METRIC_PREFIX_VHOST + "name";

    // Uppercase variables are filled in prior to searching, lowercase are matched by the returned beans
    private static final String QUERY_TEMPLATE_SERVLET = "Catalina:j2eeType=Servlet,J2EEApplication=none,J2EEServer=none,WebModule=%WEBMODULE%,name=%name%";
    private static final String QUERY_TEMPLATE_SESSION = "Catalina:type=Manager,host=%HOST%,path=%PATH%";
    private static final String QUERY_TEMPLATE_HOST = "Catalina:type=Manager,path=%PATH%,host=%host%";

    protected static final String PROPERTY_NAME = "name";
    protected static final String PROPERTY_CONTEXT_ROOT = "contextRoot";
    protected static final String PROPERTY_FILENAME = "filename";
    protected static final String PROPERTY_VHOST = "vHost";

    private final Log log = LogFactory.getLog(this.getClass());

    private EmsBean webModuleMBean;
    private ResponseTimeLogParser logParser;

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType availability;

        if (null == this.webModuleMBean) {
            this.webModuleMBean = getWebModuleMBean();
        }

        if (null != this.webModuleMBean) {
            int state;

            try {
                // check to see if the mbean is truly active
                state = (Integer) this.webModuleMBean.getAttribute("state").refresh();
            } catch (Exception e) {
                // if not active an exception may be thrown
                state = WarMBeanState.STOPPED;
            }

            availability = (WarMBeanState.STARTED == state) ? AvailabilityType.UP : AvailabilityType.DOWN;

            if (AvailabilityType.DOWN == availability) {
                // if availability is down then ensure we use a new mbean on the next try, in case we have
                // a totally new EMS connection.
                this.webModuleMBean = null;
            }
        } else {
            availability = AvailabilityType.DOWN;
        }

        return availability;
    }

    @Override
    public void start(ResourceContext resourceContext) {
        super.start(resourceContext);
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        this.webModuleMBean = getWebModuleMBean();
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile != null) {
            this.logParser = new ResponseTimeLogParser(logFile);
            this.logParser.setExcludes(responseTimeConfig.getExcludes());
            this.logParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {

        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            try {
                if (metricName.equals(METRIC_RESPONSE_TIME)) {
                    if (this.logParser != null) {
                        try {
                            CallTimeData callTimeData = new CallTimeData(schedule);
                            this.logParser.parseLog(callTimeData);
                            report.addData(callTimeData);
                        } catch (Exception e) {
                            log.error("Failed to retrieve HTTP call-time data.", e);
                        }
                    } else {
                        log.error("The '" + METRIC_RESPONSE_TIME + "' metric is enabled for WAR resource '" + getApplicationName() + "', but no value is defined for the '"
                            + RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                        // TODO: Communicate this error back to the server for display in the GUI.
                    }
                } else if (metricName.startsWith(METRIC_PREFIX_SERVLET)) {
                    Double value = getServletMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                    report.addData(metric);
                } else if (metricName.startsWith(METRIC_PREFIX_SESSION)) {
                    Double value = getSessionMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                    report.addData(metric);
                } else if (metricName.startsWith(METRIC_PREFIX_VHOST)) {
                    if (metricName.equals(TRAIT_VHOST_NAMES)) {
                        List<EmsBean> beans = getVHosts();
                        String value = "";
                        Iterator<EmsBean> iter = beans.iterator();
                        while (iter.hasNext()) {
                            EmsBean eBean = iter.next();
                            value += eBean.getBeanName().getKeyProperty("host");
                            if (iter.hasNext())
                                value += ",";
                        }
                        MeasurementDataTrait trait = new MeasurementDataTrait(schedule, value);
                        report.addData(trait);
                    }
                } else if (metricName.startsWith(METRIC_PREFIX_APPLICATION)) {
                    if (metricName.equals(TRAIT_EXPLODED)) {
                        Configuration pluginConfig = super.resourceContext.getPluginConfiguration();
                        String filename = pluginConfig.getSimpleValue(PROPERTY_FILENAME, null);
                        boolean exploded = new File(filename).isDirectory();
                        MeasurementDataTrait trait = new MeasurementDataTrait(schedule, (exploded) ? "yes" : "no");
                        report.addData(trait);
                    }
                } else {
                    log.warn("Unexpected Tomcat WAR metric schedule: " + metricName);
                }
            } catch (Exception e) {
                log.debug("Failed to gather Tomcat WAR metric: " + metricName + ", " + e);
            }
        }
    }

    private Double getSessionMetric(String metricName) {
        EmsConnection jmxConnection = getEmsConnection();
        String servletMBeanNames = QUERY_TEMPLATE_SESSION;
        Configuration config = this.resourceContext.getPluginConfiguration();
        servletMBeanNames = servletMBeanNames.replace("%PATH%", config.getSimpleValue(PROPERTY_CONTEXT_ROOT, ""));
        servletMBeanNames = servletMBeanNames.replace("%HOST%", config.getSimpleValue(PROPERTY_VHOST, ""));
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());
        String property = metricName.substring(METRIC_PREFIX_SESSION.length());
        Double ret = Double.NaN;

        if (mBeans.size() > 0) { // TODO flag error if != 1 ?
            EmsBean eBean = mBeans.get(0);
            eBean.refreshAttributes();
            EmsAttribute att = eBean.getAttribute(property);
            if (att != null) {
                Integer i = (Integer) att.getValue();
                ret = Double.valueOf(i);
            }

        }
        return ret;
    }

    private Double getServletMetric(String metricName) {

        EmsConnection jmxConnection = getEmsConnection();

        String servletMBeanNames = QUERY_TEMPLATE_SERVLET;
        Configuration config = this.resourceContext.getPluginConfiguration();
        servletMBeanNames = servletMBeanNames.replace("%WEBMODULE%", config.getSimpleValue(PROPERTY_NAME, ""));
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());

        long min = Long.MAX_VALUE;
        long max = 0;
        long processingTime = 0;
        int requestCount = 0;
        int errorCount = 0;
        Double result;

        for (EmsBean mBean : mBeans) {
            mBean.refreshAttributes();
            if (metricName.equals(METRIC_MIN_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("minTime");
                Long l = (Long) att.getValue();
                if (l < min)
                    min = l;
            } else if (metricName.equals(METRIC_MAX_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("maxTime");
                Long l = (Long) att.getValue();
                if (l > max)
                    max = l;
            } else if (metricName.equals(METRIC_AVG_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
                att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(METRIC_NUM_SERVLET_REQUESTS)) {
                EmsAttribute att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(METRIC_NUM_SERVLET_ERRORS)) {
                EmsAttribute att = mBean.getAttribute("errorCount");
                Integer i = (Integer) att.getValue();
                errorCount += i;
            } else if (metricName.equals(METRIC_TOTAL_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
            }
        }
        if (metricName.equals(METRIC_AVG_SERVLET_TIME)) {
            result = (requestCount > 0) ? ((double) processingTime / (double) requestCount) : Double.NaN;
        } else if (metricName.equals(METRIC_MIN_SERVLET_TIME)) {
            result = (min != Long.MAX_VALUE) ? (double) min : Double.NaN;
        } else if (metricName.equals(METRIC_MAX_SERVLET_TIME)) {
            result = (max != 0) ? (double) max : Double.NaN;
        } else if (metricName.equals(METRIC_NUM_SERVLET_ERRORS)) {
            result = (double) errorCount;
        } else if (metricName.equals(METRIC_NUM_SERVLET_REQUESTS)) {
            result = (double) requestCount;
        } else if (metricName.equals(METRIC_TOTAL_TIME)) {
            result = (double) processingTime;
        } else {
            // fallback
            result = Double.NaN;
        }

        return result;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        WarOperation operation = getOperation(name);
        if (null == this.webModuleMBean) {
            throw new IllegalStateException("Could not find MBean for WAR '" + getApplicationName() + "'.");
        }

        EmsOperation mbeanOperation = this.webModuleMBean.getOperation(name);
        if (mbeanOperation == null) {
            throw new IllegalStateException("Operation [" + name + "] not found on bean [" + this.webModuleMBean.getBeanName() + "]");
        }

        // NOTE: None of the supported operations have any parameters or return values, which makes our job easier.
        Object[] paramValues = new Object[0];
        mbeanOperation.invoke(paramValues);
        int state = (Integer) this.webModuleMBean.getAttribute("state").refresh();
        int expectedState = getExpectedPostExecutionState(operation);
        if (state != expectedState) {
            throw new Exception("Failed to " + name + " webapp (value of the 'state' attribute of MBean '" + this.webModuleMBean.getBeanName() + "' is " + state + ", not " + expectedState + ").");
        }

        return new OperationResult();
    }

    private static int getExpectedPostExecutionState(WarOperation operation) {
        int expectedState;
        switch (operation) {
        case START:
        case RELOAD: {
            expectedState = WarMBeanState.STARTED;
            break;
        }

        case STOP: {
            expectedState = WarMBeanState.STOPPED;
            break;
        }

        default: {
            throw new IllegalStateException("Unsupported operation: " + operation); // will never happen
        }
        }

        return expectedState;
    }

    private WarOperation getOperation(String name) {
        try {
            return WarOperation.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation name: " + name);
        }
    }

    /**
     * Returns the Catalina WebModule MBean associated with this WAR (e.g.
     * Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=//localhost/jmx-console), or null if the
     * WAR has no corresponding WebModuleMBean.
     *
     * <p/>This will return null only in the rare case that a deployed WAR has no context root associated with it. An
     * example of this is ROOT.war in the RHQ Server. rhq.ear maps rhq-portal.war to "/" and overrides ROOT.war's
     * association with "/".
     *
     * @return the Catalina WebModule MBean associated with this WAR
     */
    @Nullable
    private EmsBean getWebModuleMBean() {
        String webModuleMBeanName = getWebModuleMBeanName();
        EmsBean result = null;

        if (webModuleMBeanName != null) {
            ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(webModuleMBeanName);
            List<EmsBean> mBeans = getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());
            // There should only be one mBean for this match.
            if (mBeans.size() == 1) {
                result = mBeans.get(0);
            }
        }

        return result;
    }

    @Nullable
    private String getWebModuleMBeanName() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String name = pluginConfig.getSimpleValue(PROPERTY_NAME, null);
        String webModuleMBeanName = "Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=" + name;
        return webModuleMBeanName;
    }

    private enum WarOperation {
        START, STOP, RELOAD
    }

    private interface WarMBeanState {
        int STOPPED = 0;
        int STARTED = 1;
    }

    private List<EmsBean> getVHosts() {
        EmsConnection emsConnection = getEmsConnection();
        String query = QUERY_TEMPLATE_HOST;
        query = query.replace("%PATH%", this.resourceContext.getPluginConfiguration().getSimpleValue(PROPERTY_CONTEXT_ROOT, ""));
        ObjectNameQueryUtility queryUtil = new ObjectNameQueryUtility(query);
        List<EmsBean> mBeans = emsConnection.queryBeans(queryUtil.getTranslatedQuery());
        return mBeans;
    }

    /**
     * Returns the name of the application.
     *
     * @return application name
     */
    private String getApplicationName() {
        String resourceKey = resourceContext.getResourceKey();
        String appName = resourceKey.substring(resourceKey.lastIndexOf('=') + 1);
        return appName;
    }

}