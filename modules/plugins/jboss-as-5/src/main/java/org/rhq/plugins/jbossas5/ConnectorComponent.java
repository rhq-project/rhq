/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

/**
 * A ResourceComponent for managing a JBoss Web connector.
 *
 * @author Ian Springer
 */
public class ConnectorComponent extends ManagedComponentComponent {
    static final String PROTOCOL_PROPERTY = "protocol";
    static final String ADDRESS_PROPERTY = "address";
    static final String PORT_PROPERTY = "port";

    private static final String THREAD_POOL_METRIC_PREFIX = "ThreadPool" + PREFIX_DELIMITER;

    // A regex for the name of a particular MBean:WebThreadPool component, 
    // e.g. "jboss.web:name=http-127.0.0.1-8080,type=ThreadPool"
    private static final String WEB_THREAD_POOL_COMPONENT_NAME_TEMPLATE =
            "jboss.web:name=%" + PROTOCOL_PROPERTY + "%-%" + ADDRESS_PROPERTY + "%-%" + PORT_PROPERTY + "%,"
          + "type=ThreadPool";              

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
            throws Exception
    {
        Set<MeasurementScheduleRequest> remainingRequests = new LinkedHashSet();
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String webThreadPoolComponentName =
                ResourceComponentUtils.replacePropertyExpressionsInTemplate(WEB_THREAD_POOL_COMPONENT_NAME_TEMPLATE,
                        pluginConfig);
        ComponentType webThreadPoolComponentType = MoreKnownComponentTypes.MBean.WebThreadPool.getType();
        ManagementView managementView = getConnection().getManagementView();
        ManagedComponent webThreadPoolComponent = managementView.getComponent(webThreadPoolComponentName,
                webThreadPoolComponentType);
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try
            {
                if (metricName.startsWith(THREAD_POOL_METRIC_PREFIX)) {
                    Object value = getSimpleValue(webThreadPoolComponent, request);
                    addValueToMeasurementReport(report, request, value);
                } else  {
                    remainingRequests.add(request);
                }
            }
            catch (Exception e)
            {
                // Don't let one bad apple spoil the barrel.
                log.error("Failed to collect metric '" + metricName + "' for " + getResourceContext().getResourceType()
                        + " Resource with key " + getResourceContext().getResourceKey() + ".", e);
            }
        }
        super.getValues(report, remainingRequests);
    }
}