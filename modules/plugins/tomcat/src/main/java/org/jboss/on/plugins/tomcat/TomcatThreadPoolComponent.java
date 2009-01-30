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
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * JON plugin component for representing Tomcat connectors. Much of the functionality is left to the super class,
 * however the metrics required special handling.
 *
 * @author Jay Shaughnessy
 * @author Jason Dobies
 */
public class TomcatThreadPoolComponent extends MBeanResourceComponent<TomcatConnectorComponent> {
    // Constants  --------------------------------------------

    public static final String NAME_ADDRESS = "name";

    /**
     * A Dash character is needed to separate the parts of the GlobalRequestProcessorName
     */
    public static final String PROPERTY_DASH = "-";

    private final Log log = LogFactory.getLog(this.getClass());

    // MBeanResourceComponent Overridden Methods  --------------------------------------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        TomcatConnectorComponent parentComponent = super.resourceContext.getParentResourceComponent();
        parentComponent.getEmsConnection(); // first make sure the connection is loaded

        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();
            //            name = getAttributeName(name);

            String attributeName = name.substring(name.lastIndexOf(':') + 1);

            try {
                EmsAttribute attribute = bean.getAttribute(attributeName);

                Object valueObject = attribute.refresh();

                if (attribute.isNumericType()) {
                    Number value = (Number) valueObject;
                    report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
                } else {
                    report.addData(new MeasurementDataTrait(request, valueObject.toString()));
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }
}