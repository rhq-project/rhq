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

package org.rhq.plugins.jbossas5;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;

/**
 * A ResourceComponent for managing a JBoss Web connector.
 *
 * @author Ian Springer
 */
public class ConnectorComponent extends ManagedComponentComponent
{
    private static final Log LOG = LogFactory.getLog(ConnectorComponent.class);

    private static final String THREAD_POOL_METRIC_PREFIX = "ThreadPool" + PREFIX_DELIMITER;

    static final String PROTOCOL_PROPERTY = "protocol";
    static final String HOST_PROPERTY = "host";
    static final String ADDRESS_PROPERTY = "address";
    static final String PORT_PROPERTY = "port";

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
            throws Exception
    {
        Set<MeasurementScheduleRequest> remainingRequests = new LinkedHashSet();
        String webThreadPoolComponentName = getWebThreadPoolComponentName(getResourceContext().getPluginConfiguration());
        ComponentType webThreadPoolComponentType = MoreKnownComponentTypes.MBean.WebThreadPool.getType();
        ManagementView managementView = getConnection().getManagementView();
        ManagedComponent webThreadPoolComponent = managementView.getComponent(webThreadPoolComponentName,
                webThreadPoolComponentType);
        for (MeasurementScheduleRequest request : requests)
        {
            String metricName = request.getName();
            try
            {
                if (metricName.startsWith(THREAD_POOL_METRIC_PREFIX))
                {
                    Object value = getSimpleValue(webThreadPoolComponent, request);
                    addValueToMeasurementReport(report, request, value);
                }
                else
                {
                    remainingRequests.add(request);
                }
            }
            catch (Exception e)
            {
                // Don't let one bad apple spoil the barrel.
                LOG.error("Failed to collect metric '" + metricName + "' for " + getResourceContext().getResourceType()
                        + " Resource with key " + getResourceContext().getResourceKey() + ".", e);
            }
        }
        super.getValues(report, remainingRequests);
    }

    private String getWebThreadPoolComponentName(Configuration pluginConfig) {
        StringBuilder webThreadPoolComponentNameBuilder = new StringBuilder("jboss.web:name=") //
            .append(pluginConfig.getSimpleValue(PROTOCOL_PROPERTY)) //
            .append("-");
        if (pluginConfig.getSimpleValue(HOST_PROPERTY) != null) {
            webThreadPoolComponentNameBuilder.append(pluginConfig.getSimpleValue(HOST_PROPERTY)) //
                .append("%2F");
        }
        webThreadPoolComponentNameBuilder.append(pluginConfig.getSimpleValue(ADDRESS_PROPERTY)) //
            .append("-") //
            .append(pluginConfig.getSimpleValue(PORT_PROPERTY)) //
            .append(",type=ThreadPool");
        return webThreadPoolComponentNameBuilder.toString();
    }
}
