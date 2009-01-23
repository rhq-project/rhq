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

import java.util.Arrays;
import java.util.Set;

import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Handle generic information about an application cache
 * 
 * @author Jay Shaughnessy
 * @author Heiko W. Rupp
 *
 */
public class TomcatCacheComponent extends MBeanResourceComponent<JMXComponent> implements MeasurementFacet {

    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_PATH = "path";

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        //        JBossASTomcatServerComponent parentTomcatComponent = (JBossASTomcatServerComponent) super.resourceContext
        //            .getParentResourceComponent();
        //        EmsConnection connection = parentTomcatComponent.getEmsConnection();
        boolean isreg = bean.isRegistered();
        return isreg ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        TomcatServerComponent parentComponent = (TomcatServerComponent) super.resourceContext.getParentResourceComponent();
        parentComponent.getEmsConnection(); // first make sure the connection is loaded

        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();
            //            name = getAttributeName(name);

            String attributeName = name.substring(name.lastIndexOf(':') + 1);

            try {
                EmsAttribute attribute = bean.getAttribute(attributeName);

                Object valueObject = attribute.refresh();

                if (attributeName.equals("aliases")) {
                    String[] vals = (String[]) valueObject;
                    MeasurementDataTrait mdt = new MeasurementDataTrait(request, Arrays.toString(vals));
                    report.addData(mdt);
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }

    }

}
