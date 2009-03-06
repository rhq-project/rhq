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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Plugin component for representing Tomcat connectors. Much of the functionality is left to the super class,
 * however the metrics required special handling.
 *
 * @author Jay Shaughnessy
 * @author Jason Dobies
 */
public class TomcatConnectorComponent extends MBeanResourceComponent<TomcatServerComponent> {
    /**
     * Plugin property name for the address the connector is bound to.
     */
    public static final String PLUGIN_CONFIG_ADDRESS = "address";
    /**
     * Plugin property name for the port the connector is listening on.
     */
    public static final String PLUGIN_CONFIG_PORT = "port";
    /**
     * Plugin property name for the schema the connector is processing. Possible values are http (also for https), jk
     */
    public static final String PLUGIN_CONFIG_PROTOCOL = "protocol";
    /**
     * Plugin property name for the schema the connector is processing. Possible values are http (also for https), jk
     */
    public static final String PLUGIN_CONFIG_SCHEME = "scheme";

    public static final String UNKNOWN = "?";

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void start(ResourceContext<TomcatServerComponent> context) {
        if (UNKNOWN.equals(context.getPluginConfiguration().getSimple(PLUGIN_CONFIG_SCHEME).getStringValue())) {
            throw new InvalidPluginConfigurationException(
                "The connector is not listening for requests on the configured port. This is most likely due to the configured port being in use at Tomcat startup. In some cases (AJP connectors) Tomcat will assign an open port. This happens most often when there are multiple Tomcat servers running on the same platform. Check your Tomcat configurations for conflicts.");
        }

        super.start(context);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        TomcatServerComponent parentComponent = getResourceContext().getParentResourceComponent();
        parentComponent.getEmsConnection(); // reload the EMS connection

        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            name = getAttributeName(name);

            String beanName = name.substring(0, name.lastIndexOf(':'));
            String attributeName = name.substring(name.lastIndexOf(':') + 1);

            try {
                // Bean is cached by EMS, so no problem with getting the bean from the connection on each call
                EmsBean eBean = loadBean(beanName);
                if (eBean == null) {
                    log.warn("Bean " + beanName + " not found, skipping ...");
                    continue;
                }

                EmsAttribute attribute = eBean.getAttribute(attributeName);

                Object valueObject = attribute.refresh();
                Number value = (Number) valueObject;

                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    /**
     * Get the real name of the passed property for a concrete connector. The actual object name will begin with:
     * Catalina:name=xxx-y.y.y.y-ppppp with xxx being the scheme, y.y.y.y the address and ppppp the port. We need to
     * substitute in the address and port of this particular connector before the value can be read. In the plugin
     * descriptor, these are written as {scheme}, {address} and {port} respectively, so we can replace on those.
     */
    @Override
    protected String getAttributeName(String property) {
        String theProperty = property;

        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String address = pluginConfiguration.getSimple(PLUGIN_CONFIG_ADDRESS).getStringValue();
        String port = pluginConfiguration.getSimple(PLUGIN_CONFIG_PORT).getStringValue();
        String scheme = pluginConfiguration.getSimple(PLUGIN_CONFIG_SCHEME).getStringValue();

        theProperty = theProperty.replace("%address%", address);
        theProperty = theProperty.replace("%port%", port);
        theProperty = theProperty.replace("%scheme%", scheme);

        if (log.isDebugEnabled())
            log.debug("Finding metrics for: " + theProperty);

        return theProperty;
    }
}