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
package org.rhq.plugins.jbossas5;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.management.ObjectName;

import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.jbossas5.util.RegularExpressionNameMatcher;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ComponentType;

/**
 * @author Ian Springer
 */
public class WarMeasurementFacetDelegate implements MeasurementFacet
{
    // A regex for the names of all MBean:Servlet components for a WAR.
    private static final String SERVLET_COMPONENT_NAMES_REGEX_TEMPLATE =
            "jboss.web:J2EEApplication=none,J2EEServer=none,"
          + "WebModule=//%" + AbstractWarDiscoveryComponent.VIRTUAL_HOST_PROPERTY + "%"
          + "%" + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%,j2eeType=Servlet,name=[^,]+";

    // A regex for the names of all MBean:WebApplicationManager components for a WAR
    // (one component per vhost that WAR is deployed to).
    private static final String WEB_APPLICATION_MANAGER_COMPONENT_NAMES_REGEX_TEMPLATE =
            "jboss.web:host=[^,]+,path=%" + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%,"
          + "type=Manager";

    // The name of the MBean:WebApplicationManager component for a WAR.
    private static final String WEB_APPLICATION_MANAGER_COMPONENT_NAME_TEMPLATE =
            "jboss.web:host=%" + AbstractWarDiscoveryComponent.VIRTUAL_HOST_PROPERTY + "%,"
          + "path=%" + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%,type=Manager";

    private static final String SERVLET_METRIC_PREFIX = "Servlet.";
    private static final String WEB_APPLICATION_MANAGER_METRIC_PREFIX = "WebApplicationManager.";

    private static final String SERVLET_MAXIMUM_RESPONSE_TIME_METRIC = "Servlet.maximumResponseTime";
    private static final String SERVLET_MINIMUM_RESPONSE_TIME_METRIC = "Servlet.minimumResponseTime";
    private static final String SERVLET_AVERAGE_RESPONSE_TIME_METRIC = "Servlet.averageResponseTime";
    private static final String SERVLET_TOTAL_RESPONSE_TIME_METRIC = "Servlet.totalResponseTime";
    private static final String SERVLET_REQUEST_COUNT_METRIC = "Servlet.requestCount";
    private static final String SERVLET_ERROR_COUNT_METRIC = "Servlet.errorCount";

    private static final String CONTEXT_ROOT_TRAIT = "contextRoot";
    private static final String VIRTUAL_HOSTS_TRAIT = "virtualHosts";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext<ApplicationServerComponent> resourceContext;
    private String servletComponentNamesRegex;
    private String webApplicationManagerComponentNamesRegex;
    private String webApplicationManagerComponentName;

    public WarMeasurementFacetDelegate(ResourceContext<ApplicationServerComponent> resourceContext)
    {
        this.resourceContext = resourceContext;
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();

        this.servletComponentNamesRegex = SERVLET_COMPONENT_NAMES_REGEX_TEMPLATE;
        this.servletComponentNamesRegex = replacePluginConfigProperty(this.servletComponentNamesRegex, pluginConfig,
                AbstractWarDiscoveryComponent.VIRTUAL_HOST_PROPERTY);
        this.servletComponentNamesRegex = replacePluginConfigProperty(this.servletComponentNamesRegex, pluginConfig,
                AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY);

        this.webApplicationManagerComponentNamesRegex = WEB_APPLICATION_MANAGER_COMPONENT_NAMES_REGEX_TEMPLATE;
        this.webApplicationManagerComponentNamesRegex = replacePluginConfigProperty(this.webApplicationManagerComponentNamesRegex, pluginConfig,
                AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY);

        this.webApplicationManagerComponentName = WEB_APPLICATION_MANAGER_COMPONENT_NAME_TEMPLATE;
        this.webApplicationManagerComponentName = replacePluginConfigProperty(this.webApplicationManagerComponentName, pluginConfig,
                AbstractWarDiscoveryComponent.VIRTUAL_HOST_PROPERTY);
        this.webApplicationManagerComponentName = replacePluginConfigProperty(this.webApplicationManagerComponentName, pluginConfig,
                AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY);
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
            throws Exception
    {
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try
            {
                if (metricName.startsWith(SERVLET_METRIC_PREFIX)) {
                    Double value = getServletMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(request, value);
                    report.addData(metric);
                } else if (metricName.startsWith(WEB_APPLICATION_MANAGER_METRIC_PREFIX)) {
                    Double value = getSessionMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(request, value);
                    report.addData(metric);
                } else if (metricName.equals(CONTEXT_ROOT_TRAIT)) {
                    Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
                    String contextPath =
                            pluginConfig.getSimple(AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY).getStringValue();
                    String contextRoot = (contextPath.equals("/")) ? "/" : contextPath.substring(1);
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, contextRoot);
                    report.addData(trait);
                } else if (metricName.equals(VIRTUAL_HOSTS_TRAIT)) {
                    Set<String> vhosts = getVirtualHosts();
                    String value = "";
                    for (Iterator<String> iterator = vhosts.iterator(); iterator.hasNext();)
                    {
                        String vhost = iterator.next();
                        value += vhost;
                        if (iterator.hasNext())
                            value += ", ";
                    }
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, value);
                    report.addData(trait);
                }
            }
            catch (Exception e)
            {
                // Don't let one bad apple spoil the bunch.
                log.error("Failed to collect metric '" + metricName + "' for " + this.resourceContext.getResourceType()
                        + " Resource with key " + this.resourceContext.getResourceKey() + ".", e);
            }
        }
    }

    private String replacePluginConfigProperty(String regex, Configuration pluginConfig, String propName)
    {
        String propValue = pluginConfig.getSimple(propName).getStringValue();
        return regex.replaceAll("%" + propName + "%", propValue);
    }
    
    private ManagementView getManagementView()
    {
        ApplicationServerComponent jbasComponent = this.resourceContext.getParentResourceComponent();
        return jbasComponent.getConnection().getManagementView();
    }

    private Double getServletMetric(String metricName) throws Exception
    {
        ManagementView managementView = getManagementView();
        ComponentType servletComponentType = MoreKnownComponentTypes.MBean.Servlet.getType();
        Set<ManagedComponent> servletComponents =
                managementView.getMatchingComponents(this.servletComponentNamesRegex,
                        servletComponentType, new RegularExpressionNameMatcher());

        long min = Long.MAX_VALUE;
        long max = 0;
        long processingTime = 0;
        int requestCount = 0;
        int errorCount = 0;
        for (ManagedComponent servletComponent : servletComponents) {
            if (metricName.equals(SERVLET_MINIMUM_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "minTime");
                if (longValue < min)
                    min = longValue;
            } else if (metricName.equals(SERVLET_MAXIMUM_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "maxTime");
                if (longValue > max)
                    max = longValue;
            } else if (metricName.equals(SERVLET_AVERAGE_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "processingTime");
                processingTime += longValue;
                Integer intValue = (Integer)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "requestCount");
                requestCount += intValue;
            } else if (metricName.equals(SERVLET_REQUEST_COUNT_METRIC)) {
                Integer intValue = (Integer)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "requestCount");
                requestCount += intValue;
            } else if (metricName.equals(SERVLET_ERROR_COUNT_METRIC)) {
                Integer intValue = (Integer)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "errorCount");
                errorCount += intValue;
            } else if (metricName.equals(SERVLET_TOTAL_RESPONSE_TIME_METRIC)) {
                Long longValue = (Long)ManagedComponentUtils.getSimplePropertyValue(servletComponent, "processingTime");
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

    private Double getSessionMetric(String metricName) throws Exception
    {
        ManagementView managementView = getManagementView();
        ComponentType webApplicationManagerComponentType = MoreKnownComponentTypes.MBean.WebApplicationManager.getType();
        ManagedComponent webApplicationManagerComponent =
                managementView.getComponent(this.webApplicationManagerComponentName,
                        webApplicationManagerComponentType);
        if (webApplicationManagerComponent == null)
            throw new IllegalStateException("Cound not find " + webApplicationManagerComponentType
                    + " ManagedComponent for WAR " + this.resourceContext.getResourceKey() + ".");
        String propertyName = metricName.substring(WEB_APPLICATION_MANAGER_METRIC_PREFIX.length());
        Integer intValue = (Integer)ManagedComponentUtils.getSimplePropertyValue(webApplicationManagerComponent,
                propertyName);
        return (intValue != null) ? Double.valueOf(intValue) : Double.NaN;
    }

    private Set<String> getVirtualHosts() throws Exception
    {
        Set<String> virtualHosts = new HashSet();
        ManagementView managementView = getManagementView();
        ComponentType servletComponentType = MoreKnownComponentTypes.MBean.WebApplicationManager.getType();
        Set<ManagedComponent> webApplicationManagerComponents =
                managementView.getMatchingComponents(this.webApplicationManagerComponentNamesRegex,
                        servletComponentType, new RegularExpressionNameMatcher());
        for (ManagedComponent webApplicationManagerComponent : webApplicationManagerComponents) {
            ObjectName objectName = new ObjectName(webApplicationManagerComponent.getName());
            String host = objectName.getKeyProperty("host");
            virtualHosts.add(host);
        }
        return virtualHosts;
    }
}
