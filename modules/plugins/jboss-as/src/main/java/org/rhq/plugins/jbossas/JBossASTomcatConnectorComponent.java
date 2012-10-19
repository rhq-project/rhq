 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.jbossas;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * RHQ plugin component for representing Tomcat connectors. Much of the functionality is left to the super class,
 * however the metrics required special handling.
 *
 * @author Jason Dobies
 */
public class JBossASTomcatConnectorComponent extends MBeanResourceComponent<JBossASTomcatServerComponent> {
    // Constants  --------------------------------------------

    /**
     * Plugin property name for the address the connector is bound to.
     */
    public static final String PROPERTY_ADDRESS = "address";

    /**
     * Plugin property name for the port the connector is listening on.
     */
    public static final String PROPERTY_PORT = "port";

    /**
     * Plugin property name for the schema the connector is processing. Possible values are http (also for https), jk
     * (JBoss 3.2.x, 4.0.x), and ajp (JBoss 4.2.x and EAP)
     */
    public static final String PROPERTY_SCHEMA = "schema";

    /**
     * A Dash character is needed to separate the parts of the GlobalRequestProcessorName
     */
    public static final String PROPERTY_DASH = "-";

    private final Log log = LogFactory.getLog(this.getClass());

    // MBeanResourceComponent Overridden Methods  --------------------------------------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        JBossASTomcatServerComponent parentTomcatComponent = getResourceContext().getParentResourceComponent();
        parentTomcatComponent.getEmsConnection(); // reload the EMS connection

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
     * jboss.web:name=xxx-y.y.y.y-ppppp with xxx being the schema, y.y.y.y the address and ppppp the port. We need to
     * substitute in the address and port of this particular connector before the value can be read. In the plugin
     * descriptor, these are written as {schema}, {address} and {port} respectively, so we can replace on those. Worse:
     * if its an 'old' embedded tomcat (JBoss 3.2, 4.0) it does not have the address in the object name.
     */
    @Override
    protected String getAttributeName(String property) {
        String theProperty = property;

        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String address = pluginConfiguration.getSimpleValue(PROPERTY_ADDRESS, "");
        String port = pluginConfiguration.getSimpleValue(PROPERTY_PORT, "");
        String schema = pluginConfiguration.getSimpleValue(PROPERTY_SCHEMA, "");
        String dash = pluginConfiguration.getSimpleValue(PROPERTY_DASH, "");

        theProperty = theProperty.replace("%address%", address);
        theProperty = theProperty.replace("%port%", port);
        theProperty = theProperty.replace("%schema%", schema);
        theProperty = theProperty.replace("%dash%", dash);

        if (log.isDebugEnabled()) {
            log.debug("Finding metrics for [" + theProperty + "]...");
        }

        return theProperty;
    }
}